package com.tetrisprime.kotlin

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View

/**
 * Phase 11 — custom SurfaceView that renders the Tetris board, pieces and FX.
 *
 * Rendering happens on a dedicated thread at the FPS target set by
 * PerformanceManager (32/46/60/72 depending on profile), instead of the
 * old View-invalidate model. This keeps particle-heavy scenes smooth and
 * removes the dependency on the UI thread for frame pacing.
 *
 * Public API is identical to the previous View-based version:
 *   setEngine, setFxPack, setTheme, setBackground, setFxLevel, update, invalidate
 */
class GameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : SurfaceView(context, attrs, defStyleAttr), SurfaceHolder.Callback, Runnable {

    private var engine = GameEngine()
    val particleSystem = ParticleSystem()
    val backgroundRenderer = BackgroundRenderer()
    private var currentTheme = "classic"
    private var currentBg = "synthwave"
    private var cellSize = 0f
    private var boardOffsetX = 0f
    private var boardOffsetY = 0f

    // Render-thread state
    @Volatile private var running = false
    private var renderThread: Thread? = null
    @Volatile var fpsLabel: String = ""
    private var lastFrameTs = 0L  // Phase 11: real delta for animation pacing

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(30, 255, 255, 255)
        strokeWidth = 1f
        style = Paint.Style.STROKE
    }

    private val blockPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val ghostPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    private val bgPaint = Paint().apply {
        color = Color.argb(180, 11, 8, 30)
    }

    private val dangerPaint = Paint().apply {
        color = Color.argb(40, 239, 68, 68)
    }

    private val hudPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(160, 148, 163, 184)
        textSize = 22f
    }

    /** Default piece colors (fallback). */
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

    /** Get piece color from current theme. */
    private fun getPieceColor(type: Char): Int {
        val themeColor = ThemeDefinitions.getPieceColor(currentTheme, type)
        return try { Color.parseColor(themeColor) } catch (e: Exception) {
            defaultPieceColors[type] ?: Color.WHITE
        }
    }

    val gameEngine: GameEngine get() = engine

    fun setEngine(newEngine: GameEngine) {
        engine = newEngine
        markDirty()
    }

    /** Set the active FX pack. */
    fun setFxPack(packId: String) {
        val pack = FxPackDefinitions.getPack(packId)
        particleSystem.setPackColors(pack.colors)
    }

    /** Set the active theme. */
    fun setTheme(themeId: String) {
        currentTheme = themeId
        markDirty()
    }

    /** Set the active background. */
    fun setBackground(bgId: String) {
        currentBg = bgId
        backgroundRenderer.setBackground(bgId)
    }

    /** Set the FX quality level. */
    fun setFxLevel(level: String) {
        particleSystem.setFxLevel(level)
    }

    /** Phase 11: listener handle so we can detach on destroy (avoid leak). */
    private var performanceListener: ((PerformanceManager.Profile) -> Unit)? = null

    init {
        holder.addCallback(this)
        // Focus so touch/key events are delivered
        isFocusable = true
        isFocusableInTouchMode = true
        engine.start()
        // Phase 11: cap particle count by the active performance profile
        particleSystem.applyPerformanceCap(PerformanceManager.activeProfile.maxParticles)
        val listener: (PerformanceManager.Profile) -> Unit = { profile ->
            particleSystem.applyPerformanceCap(profile.maxParticles)
        }
        performanceListener = listener
        PerformanceManager.addListener(listener)
    }

    /** Detach from PerformanceManager (call from Activity.onDestroy). */
    fun releasePerformance() {
        performanceListener?.let { PerformanceManager.removeListener(it) }
        performanceListener = null
    }

    // ---- SurfaceHolder.Callback ----

    override fun surfaceCreated(holder: SurfaceHolder) {
        // Start the render thread
        running = true
        renderThread = Thread(this, "GameViewRender").also { it.start() }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        computeLayout(width.toFloat(), height.toFloat())
        markDirty()
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        running = false
        try {
            renderThread?.join(500)
        } catch (e: InterruptedException) { /* ignore */ }
        renderThread = null
    }

    // ---- Render thread ----

    override fun run() {
        while (running) {
            val frameStart = System.currentTimeMillis()
            // Phase 11: real delta so animations pace correctly at any FPS target
            var delta = if (lastFrameTs == 0L) 16f else (frameStart - lastFrameTs).toFloat()
            lastFrameTs = frameStart
            if (delta > 100f) delta = 100f  // clamp after long pauses

            // Draw a frame
            val canvas = try { holder.lockCanvas() } catch (e: Exception) { null }
            if (canvas != null) {
                try {
                    backgroundRenderer.update(delta)
                    particleSystem.update(delta)
                    doDraw(canvas)
                } catch (e: Exception) {
                    // Never let a render crash kill the thread
                } finally {
                    try { holder.unlockCanvasAndPost(canvas) } catch (e: Exception) { }
                }
            }

            // Record frame time for auto performance detection
            val frameMs = System.currentTimeMillis() - frameStart
            PerformanceManager.recordFrameTime(frameMs)
            fpsLabel = PerformanceManager.hudLabel()

            // Pace to the profile's target FPS
            val target = PerformanceManager.frameTimeMsOf(PerformanceManager.activeProfile)
            val sleepMs = target - frameMs
            if (sleepMs > 2) {
                try { Thread.sleep(sleepMs - 2) } catch (e: InterruptedException) { running = false }
            }
        }
    }

    /** Mark the view as needing a redraw (compat: render loop is continuous). */
    fun markDirty() { /* continuous rendering — kept for API compatibility */ }

    override fun invalidate() {
        super.invalidate()
        markDirty()
    }

    // ---- Layout ----

    private fun computeLayout(w: Float, h: Float) {
        val boardWidth = engine.board.width
        val visibleHeight = engine.board.visibleHeight
        if (w <= 0f || h <= 0f) return
        cellSize = Math.min(w / boardWidth, h / visibleHeight)
        boardOffsetX = (w - cellSize * boardWidth) / 2
        boardOffsetY = (h - cellSize * visibleHeight) / 2
    }

    // ---- Drawing ----

    private fun doDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()

        // Background layer (skipped on eco profile for perf)
        if (PerformanceManager.activeProfile.backgroundLayers) {
            backgroundRenderer.render(canvas, w, h)
        } else {
            canvas.drawColor(0xFF0B081E.toInt())
        }

        // Particles
        particleSystem.render(canvas, boardOffsetX, boardOffsetY,
            cellSize * engine.board.width, cellSize * engine.board.visibleHeight, cellSize)

        val board = engine.board
        val visHeight = board.visibleHeight

        // Danger zone tint
        if (engine.isInDanger()) {
            canvas.drawRect(
                boardOffsetX, boardOffsetY,
                boardOffsetX + cellSize * board.width,
                boardOffsetY + cellSize * visHeight,
                dangerPaint
            )
        }

        // Board background
        canvas.drawRect(
            boardOffsetX, boardOffsetY,
            boardOffsetX + cellSize * board.width,
            boardOffsetY + cellSize * visHeight,
            bgPaint
        )

        // Grid lines
        for (x in 0..board.width) {
            canvas.drawLine(
                boardOffsetX + x * cellSize, boardOffsetY,
                boardOffsetX + x * cellSize, boardOffsetY + cellSize * visHeight,
                gridPaint
            )
        }
        for (y in 0..visHeight) {
            canvas.drawLine(
                boardOffsetX, boardOffsetY + y * cellSize,
                boardOffsetX + cellSize * board.width, boardOffsetY + y * cellSize,
                gridPaint
            )
        }

        // Locked blocks (only visible rows)
        // Row y in board maps to row (y-2) on screen
        for (boardY in 2 until board.height) {
            val screenY = boardY - 2
            for (x in 0 until board.width) {
                board[x, boardY]?.let { type ->
                    val color = getPieceColor(type)
                    drawBlock(canvas, x, screenY, color)
                }
            }
        }

        // Ghost piece
        engine.getGhostPiece()?.let { ghost ->
            val color = getPieceColor(ghost.type.char)
            ghostPaint.color = Color.argb(100, Color.red(color), Color.green(color), Color.blue(color))
            ghost.cells().forEach { (x, y) ->
                if (y >= 2) {
                    drawGhostBlock(canvas, x, y - 2, ghostPaint)
                }
            }
        }

        // Current piece
        engine.currentPiece?.let { piece ->
            val color = getPieceColor(piece.type.char)
            piece.cells().forEach { (x, y) ->
                if (y >= 2) {
                    drawBlock(canvas, x, y - 2, color)
                }
            }
        }

        // FPS HUD (small, top-left corner)
        canvas.drawText(fpsLabel, 16f, 30f, hudPaint)
    }

    private fun drawBlock(canvas: Canvas, x: Int, y: Int, color: Int) {
        blockPaint.color = color
        val left = boardOffsetX + x * cellSize + 1
        val top = boardOffsetY + y * cellSize + 1
        val right = boardOffsetX + (x + 1) * cellSize - 1
        val bottom = boardOffsetY + (y + 1) * cellSize - 1
        canvas.drawRect(left, top, right, bottom, blockPaint)

        // Inner highlight
        blockPaint.color = Color.argb(60, 255, 255, 255)
        canvas.drawRect(left, top, right, top + (bottom - top) * 0.2f, blockPaint)
    }

    private fun drawGhostBlock(canvas: Canvas, x: Int, y: Int, paint: Paint) {
        val left = boardOffsetX + x * cellSize + 2
        val top = boardOffsetY + y * cellSize + 2
        val right = boardOffsetX + (x + 1) * cellSize - 2
        val bottom = boardOffsetY + (y + 1) * cellSize - 2
        canvas.drawRect(left, top, right, bottom, paint)
    }

    /** Compatibility shim — engine ticking stays in GameActivity's loop. */
    fun update(deltaMs: Long) {
        markDirty()
    }

    /** View sizing callback (used before the surface exists). */
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        computeLayout(w.toFloat(), h.toFloat())
    }
}
