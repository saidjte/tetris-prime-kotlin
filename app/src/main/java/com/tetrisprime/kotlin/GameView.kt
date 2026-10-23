package com.tetrisprime.kotlin

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

/**
 * Custom View that renders the Tetris board and current piece.
 * Renders only the visible playfield (rows 2..height-1).
 */
class GameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var engine = GameEngine()
    private var cellSize = 0f
    private var boardOffsetX = 0f
    private var boardOffsetY = 0f

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

    /** Piece colors. */
    private val pieceColors = mapOf(
        'I' to Color.parseColor("#22D3EE"),
        'O' to Color.parseColor("#FBBF24"),
        'T' to Color.parseColor("#A78BFA"),
        'S' to Color.parseColor("#34D399"),
        'Z' to Color.parseColor("#FB7185"),
        'J' to Color.parseColor("#60A5FA"),
        'L' to Color.parseColor("#FB923C"),
        'G' to Color.parseColor("#6B7280"), // garbage
        'C' to Color.parseColor("#FDE68A")  // cheese
    )

    val gameEngine: GameEngine get() = engine

    fun setEngine(newEngine: GameEngine) {
        engine = newEngine
        invalidate()
    }

    init {
        engine.start()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val boardWidth = engine.board.width
        val visibleHeight = engine.board.visibleHeight
        cellSize = Math.min(w.toFloat() / boardWidth, h.toFloat() / visibleHeight)
        boardOffsetX = (w - cellSize * boardWidth) / 2
        boardOffsetY = (h - cellSize * visibleHeight) / 2
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.parseColor("#0B081E"))

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
                    val color = pieceColors[type] ?: Color.WHITE
                    drawBlock(canvas, x, screenY, color)
                }
            }
        }

        // Ghost piece
        engine.getGhostPiece()?.let { ghost ->
            val color = pieceColors[ghost.type.char] ?: Color.WHITE
            ghostPaint.color = Color.argb(100, Color.red(color), Color.green(color), Color.blue(color))
            ghost.cells().forEach { (x, y) ->
                if (y >= 2) {
                    drawGhostBlock(canvas, x, y - 2, ghostPaint)
                }
            }
        }

        // Current piece
        engine.currentPiece?.let { piece ->
            val color = pieceColors[piece.type.char] ?: Color.WHITE
            piece.cells().forEach { (x, y) ->
                if (y >= 2) {
                    drawBlock(canvas, x, y - 2, color)
                }
            }
        }
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

    fun update(deltaMs: Long) {
        if (engine.status == GameStatus.COUNTDOWN) {
            engine.updateCountdown(deltaMs)
        } else if (engine.status == GameStatus.PLAYING) {
            engine.update(deltaMs)
        }
        invalidate()
    }
}
