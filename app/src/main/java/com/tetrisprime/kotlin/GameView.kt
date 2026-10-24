package com.tetrisprime.kotlin

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import kotlin.math.min

/**
 * Custom View that renders the Tetris board with rich visuals:
 *  - Gradient + glow + rounded blocks
 *  - Ghost piece (dashed, pulsing)
 *  - Animated line-clear flash
 *  - Lock-flash on newly-placed piece
 *  - Hard-drop trail
 *  - Next (×3) and Hold side panels rendered inside the view
 *  - Countdown overlay (3 / 2 / 1 / GO!) with scale+fade
 *  - Danger zone pulsing overlay
 *  - Auto-wires ParticleSystem.onClear / onFloatText on game events
 */
class GameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var engine = GameEngine()
    val particleSystem = ParticleSystem()
    val backgroundRenderer = BackgroundRenderer()
    private var currentTheme = "classic"
    private var currentBg = "synthwave"

    // Layout metrics (computed in onSizeChanged)
    private var cellSize = 0f
    private var boardLeft = 0f
    private var boardTop = 0f
    private var boardW = 0f
    private var boardH = 0f
    private var panelCellSize = 0f
    private var holdLeft = 0f
    private var holdTop = 0f
    private var nextLeft = 0f
    private var nextTop = 0f

    // ----- Paints (reused across frames; never allocate in onDraw) -----
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(28, 255, 255, 255)
        strokeWidth = 1f
        style = Paint.Style.STROKE
    }
    private val blockFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val blockShinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val blockEdgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
    }
    private val ghostPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.5f
        pathEffect = android.graphics.DashPathEffect(floatArrayOf(6f, 5f), 0f)
    }
    private val boardBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val panelBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.argb(60, 20, 16, 50)
    }
    private val panelLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(180, 200, 200, 230)
        textSize = 22f
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }
    private val dangerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.argb(0, 239, 68, 68)
    }
    private val flashPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.WHITE
    }
    private val trailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 6f
    }
    private val countdownPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }

    /** Default piece colors (fallback if theme lookup fails). */
    private val defaultPieceColors = mapOf(
        'I' to Color.parseColor("#22D3EE"),
        'O' to Color.parseColor("#FBBF24"),
        'T' to Color.parseColor("#A78BFA"),
        'S' to Color.parseColor("#34D399"),
        'Z' to Color.parseColor("#FB7185"),
        'J' to Color.parseColor("#60A5FA"),
        'L' to Color.parseColor("#FB923C"),
        'G' to Color.parseColor("#6B7280"),
        'C' to Color.parseColor("#FDE68A")
    )

    private fun getPieceColor(type: Char): Int {
        val themeColor = ThemeDefinitions.getPieceColor(currentTheme, type)
        return try { Color.parseColor(themeColor) } catch (e: Exception) {
            defaultPieceColors[type] ?: Color.WHITE
        }
    }

    val gameEngine: GameEngine get() = engine

    fun setEngine(newEngine: GameEngine) {
        engine = newEngine
        // Reset diff tracking so we don't fire spurious events on engine swap
        prevPiecesPlaced = engine.piecesPlaced
        prevLines = engine.lines
        prevScore = engine.score
        prevCombo = engine.combo
        prevLevel = engine.level
        prevTetrises = engine.tetrises
        prevPerfectClears = engine.perfectClears
        prevTSpins = engine.tspins
        prevLastClearTSpin = engine.lastClearWasTSpin
        prevLastClearDifficult = engine.lastClearWasDifficult
        prevLastClearPerfect = engine.lastClearWasPerfect
        prevBoardSnapshot = snapshotBoard()
        prevCurrentPieceType = null
        prevCurrentPieceCells = emptyList()
        engine.lastEventWasHardDrop = false
        // Clear transient animations
        lineClearAnim = null
        lockFlash = null
        hardDropTrail = null
        invalidate()
    }

    fun setFxPack(packId: String) {
        val pack = FxPackDefinitions.getPack(packId)
        particleSystem.setPackColors(pack.colors)
    }

    fun setTheme(themeId: String) {
        currentTheme = themeId
        invalidate()
    }

    fun setBackground(bgId: String) {
        currentBg = bgId
        backgroundRenderer.setBackground(bgId)
    }

    fun setFxLevel(level: String) {
        particleSystem.setFxLevel(level)
    }

    // ----- Performance hook (Phase 11) -----
    private var performanceListener: ((PerformanceManager.Profile) -> Unit)? = null
    init {
        engine.start()
        particleSystem.applyPerformanceCap(PerformanceManager.activeProfile.maxParticles)
        val listener: (PerformanceManager.Profile) -> Unit = { profile ->
            particleSystem.applyPerformanceCap(profile.maxParticles)
        }
        performanceListener = listener
        PerformanceManager.addListener(listener)
    }
    fun releasePerformance() {
        performanceListener?.let { PerformanceManager.removeListener(it) }
    }

    // ====================================================================
    // Animation state — transient visual effects layered on top of engine
    // ====================================================================

    /** Lines currently being flashed (before they collapse). y is board-row. */
    private data class LineClearAnim(val rows: List<Int>, var ageMs: Float)
    private var lineClearAnim: LineClearAnim? = null

    /** Lock-flash on the just-placed piece cells. */
    private data class LockFlash(val cells: List<Pair<Int, Int>>, val color: Int, var ageMs: Float)
    private var lockFlash: LockFlash? = null

    /** Hard-drop trail. */
    private data class HardDropTrail(val col: Int, val fromY: Int, val toY: Int, val color: Int, var ageMs: Float)
    private var hardDropTrail: HardDropTrail? = null

    /** Countdown overlay state (driven by engine.countdown). */
    private var lastCountdownValue: Int = -1
    private var countdownAgeMs: Float = 0f

    /** Game-over overlay fade-in. */
    private var gameOverAgeMs: Float = 0f

    /** Phase 5 — announcer text (transient on-screen caption).
     *  Set by the activity when audio/voice events fire, to mirror the
     *  original app's on-screen announcer for accessibility (captions). */
    private var announcerText: String? = null
    private var announcerAgeMs: Float = 0f
    private val announcerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
        textSize = 18f
    }
    private val announcerBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.argb(180, 11, 8, 30)
    }

    /** Called from the activity to display a transient announcer caption. */
    fun showAnnouncer(text: String?) {
        announcerText = text
        announcerAgeMs = 0f
    }

    // ====================================================================
    // Diff tracking — used to detect engine events without modifying engine
    // ====================================================================
    private var prevPiecesPlaced = 0
    private var prevLines = 0
    private var prevScore = 0L
    private var prevCombo = -1
    private var prevLevel = 1
    private var prevTetrises = 0
    private var prevPerfectClears = 0
    private var prevTSpins = 0
    private var prevLastClearTSpin: TSpinType = TSpinType.NONE
    private var prevLastClearDifficult = false
    private var prevLastClearPerfect = false
    private var prevBoardSnapshot: Array<CharArray?> = arrayOfNulls(0)
    private var prevCurrentPieceType: Char? = null

    /** Cached piece-cell list of the previous frame's current piece — used for lock-flash placement. */
    private var prevCurrentPieceCells: List<Pair<Int, Int>> = emptyList()

    private fun snapshotBoard(): Array<CharArray?> {
        val b = engine.board
        val out = arrayOfNulls<CharArray>(b.height)
        for (y in 0 until b.height) {
            val row = CharArray(b.width)
            for (x in 0 until b.width) {
                row[x] = b[x, y] ?: '.'
            }
            out[y] = row
        }
        return out
    }

    /** Called from update() — detects engine events and queues animations. */
    private fun detectEvents(deltaMs: Long) {
        val eng = engine

        // ----- Lock detection: piecesPlaced incremented -----
        if (eng.piecesPlaced > prevPiecesPlaced) {
            // The piece that just locked = prevCurrentPieceCells, painted with prev color
            val type = prevCurrentPieceType
            if (type != null && prevCurrentPieceCells.isNotEmpty()) {
                val color = getPieceColor(type)
                lockFlash = LockFlash(prevCurrentPieceCells, color, 0f)
            }
            prevPiecesPlaced = eng.piecesPlaced
        }

        // ----- Hard-drop trail detection: engine sets lastEventWasHardDrop on hardDrop() -----
        if (eng.lastEventWasHardDrop && prevCurrentPieceCells.isNotEmpty()) {
            val maxCell = prevCurrentPieceCells.maxByOrNull { it.second } ?: prevCurrentPieceCells.first()
            val type = prevCurrentPieceType ?: '?'
            hardDropTrail = HardDropTrail(
                col = maxCell.first,
                fromY = 0,
                toY = maxCell.second,
                color = getPieceColor(type),
                ageMs = 0f
            )
            eng.lastEventWasHardDrop = false  // consume
        }

        // ----- Line-clear detection -----
        if (eng.lines > prevLines) {
            val linesCleared = eng.lines - prevLines
            // Find which rows in prevBoardSnapshot were "full" (all non-dot) — those are the cleared rows.
            val clearedRows = mutableListOf<Int>()
            prevBoardSnapshot.forEachIndexed rowLoop@{ y, row ->
                if (row != null && row.all { it != '.' }) {
                    clearedRows.add(y)
                }
            }
            if (clearedRows.isNotEmpty()) {
                lineClearAnim = LineClearAnim(clearedRows, 0f)
                // Trigger particle burst at center of cleared region
                if (cellSize > 0f) {
                    val centerY = clearedRows.average().toFloat()
                    val cy = boardTop + (centerY - 2) * cellSize + cellSize / 2f
                    val cx = boardLeft + boardW / 2f
                    particleSystem.onClear(linesCleared, cx, cy, currentTheme)
                }
            }

            // Floating-text popups based on what the engine says about the clear
            val popupColor = Color.parseColor("#FDE047")
            val popupColorTSpin = Color.parseColor("#A78BFA")
            val popupColorTetris = Color.parseColor("#22D3EE")
            val popupColorPC = Color.parseColor("#34D399")

            when {
                eng.lastClearWasPerfect -> {
                    particleSystem.onFloatText("PERFECT CLEAR!", popupColorPC, lane = 1)
                    particleSystem.onConfetti()
                }
                eng.lastClearWasTSpin != TSpinType.NONE && linesCleared >= 1 -> {
                    val label = when (linesCleared) {
                        1 -> "T-SPIN SINGLE"
                        2 -> "T-SPIN DOUBLE"
                        3 -> "T-SPIN TRIPLE"
                        else -> "T-SPIN!"
                    }
                    particleSystem.onFloatText(label, popupColorTSpin, lane = 1)
                    if (eng.b2bActive) particleSystem.onFloatText("BACK-TO-BACK ×1.5", popupColorTSpin, lane = 2)
                }
                linesCleared == 4 -> {
                    particleSystem.onFloatText("TETRIS!", popupColorTetris, lane = 1)
                    if (eng.b2bActive) particleSystem.onFloatText("BACK-TO-BACK ×1.5", popupColorTetris, lane = 2)
                }
                linesCleared == 3 -> {
                    particleSystem.onFloatText("TRIPLE", popupColor, lane = 1)
                }
            }

            if (eng.combo >= 1) {
                particleSystem.onFloatText("COMBO ×${eng.combo + 1}", Color.parseColor("#FB923C"), lane = 0)
            }
            prevLines = eng.lines
        }

        // ----- Level-up detection -----
        if (eng.level > prevLevel) {
            particleSystem.onFloatText("LEVEL ${eng.level}", Color.parseColor("#22D3EE"), lane = 0)
            particleSystem.onConfetti()
            prevLevel = eng.level
        }

        // ----- Track current-piece info for next frame's lock detection -----
        eng.currentPiece?.let { piece ->
            val cells = piece.cells()
            prevCurrentPieceCells = cells.map { (x, y) -> x to y }
            prevCurrentPieceType = piece.type.char
        } ?: run {
            // Piece is null right after lock, before spawn — keep prev cells for lock-flash
            // but clear after a frame so we don't mis-attribute next lock.
            if (eng.piecesPlaced == prevPiecesPlaced) {
                // No new lock yet; keep prev cells for trail/flash detection
            }
        }

        // ----- Sync remaining diffs -----
        prevScore = eng.score
        prevCombo = eng.combo
        prevTetrises = eng.tetrises
        prevPerfectClears = eng.perfectClears
        prevTSpins = eng.tspins
        prevLastClearTSpin = eng.lastClearWasTSpin
        prevLastClearDifficult = eng.lastClearWasDifficult
        prevLastClearPerfect = eng.lastClearWasPerfect
        prevBoardSnapshot = snapshotBoard()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val b = engine.board
        // Reserve ~28% of width for side panels (14% each side)
        val sideReserve = if (w > h * 0.9f) w * 0.14f else 0f
        val availW = w - sideReserve * 2f
        val availH = h.toFloat()
        cellSize = min(availW / b.width, availH / b.visibleHeight)
        boardW = cellSize * b.width
        boardH = cellSize * b.visibleHeight
        boardLeft = (w - boardW) / 2f
        boardTop = (h - boardH) / 2f

        // Side panel positioning (if we have room)
        if (sideReserve > 0f) {
            panelCellSize = sideReserve / 4.5f
            holdLeft = boardLeft - sideReserve + (sideReserve - panelCellSize * 4) / 2f
            holdTop = boardTop + 50f
            nextLeft = boardLeft + boardW + (sideReserve - panelCellSize * 4) / 2f
            nextTop = boardTop + 50f
        } else {
            panelCellSize = 0f  // disable side panels when narrow
        }

        // Update board-bg shader with new dimensions
        boardBgPaint.shader = LinearGradient(
            0f, boardTop, 0f, boardTop + boardH,
            Color.argb(220, 14, 10, 38),
            Color.argb(220, 8, 6, 26),
            Shader.TileMode.CLAMP
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // 1. Background
        backgroundRenderer.update(16f)
        backgroundRenderer.render(canvas, width.toFloat(), height.toFloat())

        // 2. Particles update (advance physics)
        particleSystem.update(16f)

        val b = engine.board
        val visH = b.visibleHeight

        // 3. Board background panel
        canvas.drawRoundRect(
            boardLeft - 4f, boardTop - 4f,
            boardLeft + boardW + 4f, boardTop + boardH + 4f,
            10f, 10f,
            panelBgPaint
        )
        canvas.drawRect(boardLeft, boardTop, boardLeft + boardW, boardTop + boardH, boardBgPaint)

        // 4. Grid
        for (x in 0..b.width) {
            canvas.drawLine(
                boardLeft + x * cellSize, boardTop,
                boardLeft + x * cellSize, boardTop + boardH,
                gridPaint
            )
        }
        for (y in 0..visH) {
            canvas.drawLine(
                boardLeft, boardTop + y * cellSize,
                boardLeft + boardW, boardTop + y * cellSize,
                gridPaint
            )
        }

        // 5. Locked blocks
        for (boardY in 2 until b.height) {
            val screenY = boardY - 2
            for (x in 0 until b.width) {
                b[x, boardY]?.let { type ->
                    drawBlock(canvas, x, screenY, getPieceColor(type))
                }
            }
        }

        // 6. Line-clear flash overlay (animate BEFORE blocks collapse)
        lineClearAnim?.let { anim ->
            val totalDuration = 220f
            anim.ageMs += 16f
            val t = (anim.ageMs / totalDuration).coerceIn(0f, 1f)
            val alpha = ((1f - t) * 220).toInt()
            flashPaint.color = Color.argb(alpha, 255, 255, 255)
            for (row in anim.rows) {
                val sy = row - 2
                if (sy in 0 until visH) {
                    canvas.drawRect(
                        boardLeft, boardTop + sy * cellSize,
                        boardLeft + boardW, boardTop + (sy + 1) * cellSize,
                        flashPaint
                    )
                }
            }
            if (anim.ageMs >= totalDuration) lineClearAnim = null
        }

        // 7. Hard-drop trail (vertical fading line)
        hardDropTrail?.let { trail ->
            val totalDuration = 280f
            trail.ageMs += 16f
            val t = (trail.ageMs / totalDuration).coerceIn(0f, 1f)
            val alpha = ((1f - t) * 180).toInt()
            trailPaint.color = Color.argb(alpha, Color.red(trail.color), Color.green(trail.color), Color.blue(trail.color))
            trailPaint.strokeWidth = cellSize * 0.6f
            val x = boardLeft + (trail.col + 0.5f) * cellSize
            val yTop = boardTop + (trail.fromY - 2).coerceAtLeast(0) * cellSize
            val yBottom = boardTop + (trail.toY - 2 + 1).coerceAtMost(visH) * cellSize
            canvas.drawLine(x, yTop, x, yBottom, trailPaint)
            if (trail.ageMs >= totalDuration) hardDropTrail = null
        }

        // 8. Ghost piece
        engine.getGhostPiece()?.let { ghost ->
            val color = getPieceColor(ghost.type.char)
            // Pulse alpha slightly
            val pulse = 0.55f + 0.25f * kotlin.math.sin(System.currentTimeMillis() / 250.0).toFloat()
            ghostPaint.color = Color.argb((100 * pulse).toInt(), Color.red(color), Color.green(color), Color.blue(color))
            ghost.cells().forEach { (x, y) ->
                if (y >= 2) drawGhostBlock(canvas, x, y - 2, ghostPaint)
            }
        }

        // 9. Current piece
        engine.currentPiece?.let { piece ->
            val color = getPieceColor(piece.type.char)
            piece.cells().forEach { (x, y) ->
                if (y >= 2) drawBlock(canvas, x, y - 2, color)
            }
        }

        // 10. Lock-flash on just-placed piece
        lockFlash?.let { flash ->
            val totalDuration = 200f
            flash.ageMs += 16f
            val t = (flash.ageMs / totalDuration).coerceIn(0f, 1f)
            val alpha = ((1f - t) * 200).toInt()
            flashPaint.color = Color.argb(alpha, 255, 255, 255)
            for ((x, y) in flash.cells) {
                if (y >= 2) {
                    val sy = y - 2
                    val left = boardLeft + x * cellSize + 1
                    val top = boardTop + sy * cellSize + 1
                    val right = boardLeft + (x + 1) * cellSize - 1
                    val bottom = boardTop + (sy + 1) * cellSize - 1
                    canvas.drawRect(left, top, right, bottom, flashPaint)
                }
            }
            if (flash.ageMs >= totalDuration) lockFlash = null
        }

        // 11. Danger overlay (pulsing red when in danger zone)
        if (engine.isInDanger()) {
            val pulse = 0.4f + 0.3f * kotlin.math.sin(System.currentTimeMillis() / 180.0).toFloat()
            dangerPaint.color = Color.argb((90 * pulse).toInt(), 239, 68, 68)
            canvas.drawRect(boardLeft, boardTop, boardLeft + boardW, boardTop + boardH, dangerPaint)
        }

        // 12. Side panels (Hold + Next ×3)
        if (panelCellSize > 0f) {
            drawHoldPanel(canvas)
            drawNextPanel(canvas)
        }

        // 13. Particles (rendered on top of everything)
        particleSystem.render(canvas, boardLeft, boardTop, boardW, boardH, cellSize)

        // 14. Countdown overlay
        if (engine.status == GameStatus.COUNTDOWN) {
            drawCountdownOverlay(canvas)
        }

        // 15. Game-over overlay (fade-in)
        if (engine.isGameOver) {
            gameOverAgeMs += 16f
            drawGameOverOverlay(canvas)
        } else {
            gameOverAgeMs = 0f
        }

        // 16. Phase 5 — announcer text (transient caption, fades after 2.5s)
        drawAnnouncer(canvas)
    }

    private fun drawAnnouncer(canvas: Canvas) {
        val text = announcerText ?: return
        announcerAgeMs += 16f
        val totalDuration = 2500f
        if (announcerAgeMs >= totalDuration) {
            announcerText = null
            return
        }
        val t = announcerAgeMs / totalDuration
        // Fade out in last 30% of duration
        val alpha = if (t < 0.7f) 255 else ((1f - (t - 0.7f) / 0.3f) * 255).toInt().coerceIn(0, 255)
        // Position: top-center, below the status bar
        val cx = width / 2f
        val cy = height * 0.18f
        val pad = 24f
        val textW = announcerPaint.measureText(text)
        val bgLeft = cx - textW / 2 - pad
        val bgRight = cx + textW / 2 + pad
        val bgTop = cy - announcerPaint.textSize
        val bgBottom = cy + announcerPaint.textSize * 0.4f

        announcerBgPaint.alpha = (alpha * 0.7f).toInt()
        canvas.drawRoundRect(
            android.graphics.RectF(bgLeft, bgTop, bgRight, bgBottom),
            12f, 12f, announcerBgPaint
        )

        announcerPaint.color = Color.argb(alpha, 253, 224, 71)  // amber
        canvas.drawText(text, cx, cy, announcerPaint)
    }

    // ====================================================================
    // Block drawing — gradient + edge + shine for "juicy" look
    // ====================================================================
    private fun drawBlock(canvas: Canvas, x: Int, y: Int, color: Int) {
        val left = boardLeft + x * cellSize + 1
        val top = boardTop + y * cellSize + 1
        val right = boardLeft + (x + 1) * cellSize - 1
        val bottom = boardTop + (y + 1) * cellSize - 1
        val rectF = RectF(left, top, right, bottom)

        // Main fill with subtle vertical gradient (lighter top, darker bottom)
        val r = Color.red(color); val g = Color.green(color); val bl = Color.blue(color)
        blockFillPaint.shader = LinearGradient(
            left, top, left, bottom,
            Color.argb(255, min(255, r + 40), min(255, g + 40), min(255, bl + 40)),
            Color.argb(255, (r * 0.7f).toInt(), (g * 0.7f).toInt(), (bl * 0.7f).toInt()),
            Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(rectF, cellSize * 0.18f, cellSize * 0.18f, blockFillPaint)
        blockFillPaint.shader = null  // reset

        // Edge stroke (theme color, slightly brighter)
        blockEdgePaint.color = Color.argb(200, min(255, r + 80), min(255, g + 80), min(255, bl + 80))
        canvas.drawRoundRect(rectF, cellSize * 0.18f, cellSize * 0.18f, blockEdgePaint)

        // Top shine stripe (glass highlight)
        blockShinePaint.color = Color.argb(90, 255, 255, 255)
        val shineRect = RectF(left + cellSize * 0.12f, top + cellSize * 0.10f,
            right - cellSize * 0.12f, top + cellSize * 0.32f)
        canvas.drawRoundRect(shineRect, cellSize * 0.08f, cellSize * 0.08f, blockShinePaint)
    }

    private fun drawGhostBlock(canvas: Canvas, x: Int, y: Int, paint: Paint) {
        val left = boardLeft + x * cellSize + 3
        val top = boardTop + y * cellSize + 3
        val right = boardLeft + (x + 1) * cellSize - 3
        val bottom = boardTop + (y + 1) * cellSize - 3
        canvas.drawRoundRect(RectF(left, top, right, bottom), cellSize * 0.15f, cellSize * 0.15f, paint)
    }

    // ====================================================================
    // Hold panel — left side
    // ====================================================================
    private fun drawHoldPanel(canvas: Canvas) {
        if (holdLeft <= 0f) return
        // Label
        canvas.drawText("HOLD", holdLeft + panelCellSize * 2, holdTop - 12f, panelLabelPaint)
        // Panel background
        val pw = panelCellSize * 4
        val ph = panelCellSize * 3
        canvas.drawRoundRect(
            RectF(holdLeft - 4f, holdTop - 4f, holdLeft + pw + 4f, holdTop + ph + 4f),
            8f, 8f, panelBgPaint
        )
        engine.holdPiece?.let { type ->
            val color = if (engine.holdUsed) {
                // Grayscale when used (can't swap again this turn)
                Color.argb(255, 120, 120, 140)
            } else getPieceColor(type.char)
            drawMiniPiece(canvas, type, holdLeft, holdTop, panelCellSize, color)
        }
    }

    // ====================================================================
    // Next panel — right side, shows up to 3 pieces
    // ====================================================================
    private fun drawNextPanel(canvas: Canvas) {
        if (nextLeft <= 0f) return
        canvas.drawText("NEXT", nextLeft + panelCellSize * 2, nextTop - 12f, panelLabelPaint)
        val pw = panelCellSize * 4
        val pieceSpacing = panelCellSize * 3.6f
        val ph = pieceSpacing * 3
        canvas.drawRoundRect(
            RectF(nextLeft - 4f, nextTop - 4f, nextLeft + pw + 4f, nextTop + ph + 4f),
            8f, 8f, panelBgPaint
        )
        val nextQueue = engine.nextPieces.take(3)
        nextQueue.forEachIndexed { i, type ->
            val color = getPieceColor(type.char)
            drawMiniPiece(canvas, type, nextLeft, nextTop + i * pieceSpacing, panelCellSize, color)
        }
    }

    private fun drawMiniPiece(canvas: Canvas, type: PieceType, originX: Float, originY: Float, size: Float, color: Int) {
        // Build a temp piece at rotation 0 to get its cells
        val piece = Piece(type, 0, 0, 0)
        val cells = piece.cells()
        // Center the mini-piece in a 4×2 bounding box
        val minX = cells.minOf { it.first }
        val maxX = cells.maxOf { it.first }
        val minY = cells.minOf { it.second }
        val maxY = cells.maxOf { it.second }
        val pieceW = (maxX - minX + 1)
        val pieceH = (maxY - minY + 1)
        val offX = (4 - pieceW) / 2f - minX
        val offY = (2 - pieceH) / 2f - minY
        val r = Color.red(color); val g = Color.green(color); val bl = Color.blue(color)
        blockFillPaint.shader = LinearGradient(
            originX, originY, originX, originY + size * 2,
            Color.argb(255, min(255, r + 40), min(255, g + 40), min(255, bl + 40)),
            Color.argb(255, (r * 0.7f).toInt(), (g * 0.7f).toInt(), (bl * 0.7f).toInt()),
            Shader.TileMode.CLAMP
        )
        blockEdgePaint.color = Color.argb(200, min(255, r + 80), min(255, g + 80), min(255, bl + 80))
        for ((x, y) in cells) {
            val left = originX + (x + offX) * size + 1
            val top = originY + (y + offY) * size + 1
            val right = left + size - 2
            val bottom = top + size - 2
            canvas.drawRoundRect(RectF(left, top, right, bottom), size * 0.18f, size * 0.18f, blockFillPaint)
            canvas.drawRoundRect(RectF(left, top, right, bottom), size * 0.18f, size * 0.18f, blockEdgePaint)
        }
        blockFillPaint.shader = null
    }

    // ====================================================================
    // Countdown overlay — 3 / 2 / 1 / GO!
    // ====================================================================
    private fun drawCountdownOverlay(canvas: Canvas) {
        val v = engine.countdown
        if (v != lastCountdownValue) {
            countdownAgeMs = 0f
            lastCountdownValue = v
        }
        countdownAgeMs += 16f

        val text = when (v) {
            3 -> "3"
            2 -> "2"
            1 -> "1"
            0 -> "GO!"
            else -> return
        }
        // Dim background slightly
        canvas.drawColor(Color.argb(80, 0, 0, 0))

        val cx = width / 2f
        val cy = height / 2f
        val phase = (countdownAgeMs / 800f).coerceIn(0f, 1f)
        val scale = 0.6f + 1.4f * (1f - (1f - phase) * (1f - phase))  // ease-out
        val alpha = ((1f - phase) * 255).toInt().coerceIn(0, 255)
        val baseSize = min(width, height) * 0.35f

        countdownPaint.textSize = baseSize * scale
        countdownPaint.color = when (v) {
            3 -> Color.argb(alpha, 251, 191, 36)   // amber
            2 -> Color.argb(alpha, 251, 146, 60)   // orange
            1 -> Color.argb(alpha, 248, 113, 113)  // red
            else -> Color.argb(alpha, 52, 211, 153) // green GO!
        }
        // Glow shadow
        countdownPaint.setShadowLayer(30f, 0f, 0f, countdownPaint.color)
        canvas.drawText(text, cx, cy + countdownPaint.textSize / 3f, countdownPaint)
        countdownPaint.setShadowLayer(0f, 0f, 0f, 0)
    }

    // ====================================================================
    // Game-over overlay — dim + big text
    // ====================================================================
    private fun drawGameOverOverlay(canvas: Canvas) {
        val t = (gameOverAgeMs / 600f).coerceIn(0f, 1f)
        val alpha = (t * 180).toInt()
        canvas.drawColor(Color.argb(alpha, 5, 3, 15))

        val cx = width / 2f
        val cy = height / 2f
        countdownPaint.textSize = min(width, height) * 0.13f
        countdownPaint.color = Color.argb((t * 255).toInt(), 248, 113, 113)
        countdownPaint.setShadowLayer(20f, 0f, 0f, Color.argb((t * 200).toInt(), 248, 113, 113))
        canvas.drawText("GAME OVER", cx, cy, countdownPaint)
        countdownPaint.setShadowLayer(0f, 0f, 0f, 0)

        // Subtitle
        countdownPaint.textSize = min(width, height) * 0.04f
        countdownPaint.color = Color.argb((t * 200).toInt(), 200, 200, 230)
        canvas.drawText("Score: ${engine.score}", cx, cy + countdownPaint.textSize * 1.8f, countdownPaint)
    }

    fun update(deltaMs: Long) {
        if (engine.status == GameStatus.COUNTDOWN) {
            engine.updateCountdown(deltaMs)
        } else if (engine.status == GameStatus.PLAYING) {
            engine.update(deltaMs)
        }
        detectEvents(deltaMs)
        invalidate()
    }
}
