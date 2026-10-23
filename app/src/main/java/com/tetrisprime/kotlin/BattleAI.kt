package com.tetrisprime.kotlin

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Battle AI opponent.
 * Plays its own board with configurable difficulty.
 */
class BattleAI(
    private val difficulty: BattleDifficulty = BattleDifficulty.NORMAL,
    private val rng: kotlin.random.Random = kotlin.random.Random.Default
) {
    var board = Board()
        private set
    var currentPiece: Piece? = null
        private set
    private val bag = BagRandomizer(rng)
    private var nextPieces = mutableListOf<PieceType>()
    var isAlive = true
        private set
    var lines = 0
        private set

    // AI plan: where to place the current piece
    private var planX = 0
    private var planRot = 0
    private var hasPlan = false

    // Timing
    private var stepAccumulator = 0L
    private val stepMs: Long = difficulty.stepMs.toLong()
    private val mistakeChance: Double = difficulty.mistakeChance
    private val garbageMul: Double = difficulty.garbageMul
    private val tetrisBonus: Int = difficulty.tetrisBonus
    private val sendEvery: Int = difficulty.sendEvery

    // Garbage queue
    var pendingGarbage = 0
        private set
    private var lineMeter = 0
    private var lastGarbageHole = -1

    init {
        reset()
    }

    fun reset() {
        board = Board()
        isAlive = true
        lines = 0
        hasPlan = false
        stepAccumulator = 0
        pendingGarbage = 0
        lineMeter = 0
        nextPieces.clear()
        repeat(5) { nextPieces.add(bag.next()) }
        spawnPiece()
    }

    private fun spawnPiece() {
        val type = nextPieces.removeAt(0)
        nextPieces.add(bag.next())
        val piece = when (type) {
            PieceType.I -> Piece(type, x = 3, y = 1)
            else -> Piece(type, x = 4, y = 0)
        }
        if (!board.isValid(piece)) {
            isAlive = false
        } else {
            currentPiece = piece
            hasPlan = false
        }
    }

    /** Compute the best placement for the current piece. */
    private fun computePlan() {
        val p = currentPiece ?: return
        var bestScore = Int.MIN_VALUE
        var bestX = p.x
        var bestRot = p.rotation

        // Try all rotations and positions
        for (rot in 0..3) {
            for (x in -2 until board.width) {
                val testPiece = Piece(p.type, x, p.y, rot)
                if (!board.isValid(testPiece)) continue

                // Drop to bottom
                val dropped = board.ghostDrop(testPiece)
                if (!board.isValid(dropped)) continue

                // Simulate lock and evaluate
                val testBoard = board.copy()
                testBoard.lock(dropped)
                val score = evaluateBoard(testBoard, dropped)

                if (score > bestScore) {
                    bestScore = score
                    bestX = x
                    bestRot = rot
                }
            }
        }

        // Apply mistake chance — randomly choose a suboptimal placement
        if (rng.nextDouble() < mistakeChance) {
            // Pick a random valid placement
            val validPlacements = mutableListOf<Triple<Int, Int, Int>>()
            for (rot in 0..3) {
                for (x in -2 until board.width) {
                    val testPiece = Piece(p.type, x, p.y, rot)
                    if (board.isValid(testPiece)) {
                        val dropped = board.ghostDrop(testPiece)
                        if (board.isValid(dropped)) {
                            validPlacements.add(Triple(x, rot, 0))
                        }
                    }
                }
            }
            if (validPlacements.isNotEmpty()) {
                val pick = validPlacements[rng.nextInt(validPlacements.size)]
                bestX = pick.first
                bestRot = pick.second
            }
        }

        planX = bestX
        planRot = bestRot
        hasPlan = true
    }

    /** Evaluate a board state. Higher = better. */
    private fun evaluateBoard(testBoard: Board, piece: Piece): Int {
        var score = 0

        // Count lines that would be cleared
        val linesBefore = countFilledRows(board)
        val linesAfter = countFilledRows(testBoard)
        val clearedRows = linesBefore - linesAfter + countFullRows(testBoard)

        // Reward line clears
        score += clearedRows * 100
        if (clearedRows == 4) score += tetrisBonus * 50 // Tetris bonus

        // Penalize height
        val height = testBoard.highestFilledRow()
        score -= height * 2

        // Penalize holes (gaps below filled cells)
        val holes = countHoles(testBoard)
        score -= holes * 30

        // Penalize bumpiness (height differences between columns)
        val bumpiness = calculateBumpiness(testBoard)
        score -= bumpiness * 5

        return score
    }

    private fun countFilledRows(b: Board): Int {
        var count = 0
        for (y in 0 until b.height) {
            for (x in 0 until b.width) {
                if (b[x, y] != null) {
                    count++
                    break
                }
            }
        }
        return count
    }

    private fun countFullRows(b: Board): Int {
        var count = 0
        for (y in 0 until b.height) {
            var full = true
            for (x in 0 until b.width) {
                if (b[x, y] == null) {
                    full = false
                    break
                }
            }
            if (full) count++
        }
        return count
    }

    private fun countHoles(b: Board): Int {
        var holes = 0
        for (x in 0 until b.width) {
            var foundFilled = false
            for (y in 0 until b.height) {
                if (b[x, y] != null) {
                    foundFilled = true
                } else if (foundFilled) {
                    holes++
                }
            }
        }
        return holes
    }

    private fun calculateBumpiness(b: Board): Int {
        val heights = IntArray(b.width)
        for (x in 0 until b.width) {
            for (y in 0 until b.height) {
                if (b[x, y] != null) {
                    heights[x] = b.height - y
                    break
                }
            }
        }
        var bumpiness = 0
        for (i in 0 until b.width - 1) {
            bumpiness += abs(heights[i] - heights[i + 1])
        }
        return bumpiness
    }

    /** Update AI — called each frame. Returns garbage to send to player (0 if none). */
    fun update(deltaMs: Long): Int {
        if (!isAlive) return 0

        // Apply pending garbage
        if (pendingGarbage > 0) {
            val toApply = min(4, pendingGarbage)
            for (i in 0 until toApply) {
                var hole = rng.nextInt(board.width)
                while (hole == lastGarbageHole) hole = rng.nextInt(board.width)
                lastGarbageHole = hole
                board.addGarbageRow(hole)
            }
            pendingGarbage -= toApply
        }

        stepAccumulator += deltaMs
        if (stepAccumulator < stepMs) return 0
        stepAccumulator -= stepMs

        val p = currentPiece ?: return 0
        if (!hasPlan) computePlan()

        // Move toward plan
        var piece = p
        if (piece.rotation != planRot) {
            val rotated = piece.rotated(1)
            if (board.isValid(rotated)) {
                piece = rotated
            }
        }
        if (piece.x < planX) {
            val moved = piece.moved(1, 0)
            if (board.isValid(moved)) piece = moved
        } else if (piece.x > planX) {
            val moved = piece.moved(-1, 0)
            if (board.isValid(moved)) piece = moved
        }

        // If in position, hard drop
        if (piece.x == planX && piece.rotation == planRot) {
            val dropped = board.ghostDrop(piece)
            board.lock(dropped)
            val result = board.clearLines()
            if (result.linesCleared > 0) {
                lines += result.linesCleared
                lineMeter += result.linesCleared

                // Send garbage to player
                var garbageToSend = 0
                if (sendEvery > 0 && lineMeter >= sendEvery) {
                    garbageToSend = (result.linesCleared * garbageMul).toInt()
                    if (result.linesCleared == 4) garbageToSend += tetrisBonus
                    lineMeter = 0
                }
                currentPiece = null
                spawnPiece()
                return garbageToSend
            }
            currentPiece = null
            spawnPiece()
        } else {
            currentPiece = piece
        }

        return 0
    }

    /** Receive garbage from player. */
    fun receiveGarbage(amount: Int) {
        pendingGarbage += amount
    }
}

/** Battle AI difficulty levels. */
enum class BattleDifficulty(
    val stepMs: Int,
    val mistakeChance: Double,
    val garbageMul: Double,
    val tetrisBonus: Int,
    val sendEvery: Int
) {
    EASY(stepMs = 380, mistakeChance = 0.30, garbageMul = 0.5, tetrisBonus = 0, sendEvery = 0),
    NORMAL(stepMs = 240, mistakeChance = 0.12, garbageMul = 1.0, tetrisBonus = 0, sendEvery = 8),
    HARD(stepMs = 140, mistakeChance = 0.03, garbageMul = 1.0, tetrisBonus = 1, sendEvery = 4)
}
