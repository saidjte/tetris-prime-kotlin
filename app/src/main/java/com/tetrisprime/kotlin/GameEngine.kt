package com.tetrisprime.kotlin

import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Core game state and logic — complete implementation matching the original.
 *
 * Features:
 * - SRS rotation with wall kicks
 * - 7-bag randomizer (regular + seeded for daily)
 * - Official Tetris guideline gravity formula
 * - Lock delay (500ms) with max 15 resets
 * - T-Spin detection (3-corner rule)
 * - Perfect Clear detection
 * - Back-to-Back (1.5×) bonus
 * - Combo system
 * - Full scoring tables (regular + T-Spin + Perfect Clear)
 * - Hold piece (one swap per drop)
 * - Ghost piece
 * - Soft/Hard drop
 */
class GameEngine(
    private val mode: GameMode = GameMode.CLASSIC,
    private val seededRandom: SeededRandom? = null // null = regular random
) {
    val board = Board()
    private val bag: BagRandomizer = if (seededRandom != null) {
        // Use seeded bag — but BagRandomizer takes kotlin.random.Random
        // We'll use a wrapper
        BagRandomizer(SeededKotlinRandom(seededRandom))
    } else {
        BagRandomizer()
    }

    var currentPiece: Piece? = null
        private set
    var nextPieces: MutableList<PieceType> = mutableListOf()
        private set
    var holdPiece: PieceType? = null
        private set
    var holdUsed = false
        private set

    var score = 0L
        private set
    var lines = 0
        private set
    var level = 1
        private set
    var combo = -1
        private set
    var b2bActive = false
        private set

    // T-Spin and Perfect Clear tracking
    var lastClearWasTSpin = TSpinType.NONE
        private set
    var lastClearWasDifficult = false
        private set
    var lastClearWasPerfect = false
        private set
    var lastActionWasRotate = false
        private set

    // Lock delay system
    private var lockAccumulator = 0L
    private var lockResets = 0
    private var isGrounded = false

    // Game state
    var isGameOver = false
        private set
    var isPaused = false
    var status = GameStatus.COUNTDOWN
        private set
    var countdown = 3
        private set

    // Stats
    var tetrises = 0
        private set
    var tspins = 0
        private set
    var perfectClears = 0
        private set
    var maxCombo = 0
        private set
    var maxB2b = 0
        private set
    var piecesPlaced = 0
        private set

    private var dropAccumulator = 0L
    private var softDropping = false

    /** Official Tetris guideline gravity formula.
     * Returns ms per cell drop. Level capped at 20. */
    val dropIntervalMs: Long
        get() {
            val cappedLevel = min(level, 20)
            return max(30L, (1000.0 * (0.8 - (cappedLevel - 1) * 0.007).pow(cappedLevel - 1)).roundToInt().toLong())
        }

    /** Soft drop gravity (max(15, floor(gravity/20))). */
    val softDropIntervalMs: Long
        get() = max(15L, dropIntervalMs / 20)

    /** Lock delay in ms. */
    val lockDelayMs: Long = 500L

    /** Max lock resets before forced lock. */
    val maxLockResets: Int = 15

    /** Initialize / restart the game. */
    fun start() {
        // Clear board
        for (y in 0 until board.height) {
            for (x in 0 until board.width) {
                board[x, y] = null
            }
        }

        score = 0
        lines = 0
        level = if (mode == GameMode.SPRINT) 1 else (mode.fixedLevel ?: 1)
        combo = -1
        b2bActive = false
        isGameOver = false
        isPaused = false
        holdPiece = null
        holdUsed = false
        dropAccumulator = 0
        lockAccumulator = 0
        lockResets = 0
        isGrounded = false
        lastActionWasRotate = false
        tetrises = 0
        tspins = 0
        perfectClears = 0
        maxCombo = 0
        maxB2b = 0
        piecesPlaced = 0

        // Cheese mode: fill bottom rows
        if (mode == GameMode.CHEESE) {
            val rng = seededRandom?.let { SeededKotlinRandom(it) } ?: kotlin.random.Random.Default
            board.fillCheese(8, rng)
        }

        nextPieces.clear()
        repeat(5) { nextPieces.add(bag.next()) }

        status = GameStatus.COUNTDOWN
        countdown = 3
        spawnPiece()
    }

    /** Spawn the next piece from the queue. */
    private fun spawnPiece() {
        val type = nextPieces.removeAt(0)
        nextPieces.add(bag.next())
        // Spawn position: centered, at top
        val piece = when (type) {
            PieceType.I -> Piece(type, x = 3, y = 1) // I-piece spawns lower
            else -> Piece(type, x = 4, y = 0)
        }
        if (board.isGameOver(piece)) {
            isGameOver = true
            status = GameStatus.OVER
        } else {
            currentPiece = piece
            holdUsed = false
            isGrounded = false
            lockAccumulator = 0
            lockResets = 0
            lastActionWasRotate = false
        }
    }

    /** Move piece left/right. Returns true if moved. */
    fun move(dx: Int): Boolean {
        val p = currentPiece ?: return false
        val moved = p.moved(dx, 0)
        return if (board.isValid(moved)) {
            currentPiece = moved
            lastActionWasRotate = false
            if (isGrounded) resetLockDelay()
            true
        } else false
    }

    /** Rotate piece with SRS wall kicks. Returns true if rotated. */
    fun rotate(dir: Int): Boolean {
        val p = currentPiece ?: return false
        // O-piece doesn't rotate
        if (p.type == PieceType.O) return false

        val oldRot = p.rotation
        val newRot = (oldRot + dir + 4) % 4
        val kicks = Piece.WALL_KICKS[oldRot to newRot] ?: listOf(0 to 0)
        for ((kx, ky) in kicks) {
            val test = Piece(p.type, p.x + kx, p.y - ky, newRot)
            if (board.isValid(test)) {
                currentPiece = test
                lastActionWasRotate = true
                if (isGrounded) resetLockDelay()
                return true
            }
        }
        return false
    }

    /** Soft drop (move down one cell). Returns true if moved, false if locked. */
    fun softDrop(): Boolean {
        val p = currentPiece ?: return false
        val moved = p.moved(0, 1)
        return if (board.isValid(moved)) {
            currentPiece = moved
            score += 1L
            dropAccumulator = 0
            true
        } else {
            // Piece can't move down — it's grounded
            isGrounded = true
            false
        }
    }

    /** Set soft drop state (hold). */
    fun setSoftDropping(dropping: Boolean) {
        softDropping = dropping
        if (dropping) dropAccumulator = 0
    }

    /** Hard drop — instantly drop and lock. */
    fun hardDrop() {
        val p = currentPiece ?: return
        val ghost = board.ghostDrop(p)
        val dropDistance = ghost.y - p.y
        score += dropDistance * 2L
        currentPiece = ghost
        lockPiece()
    }

    /** Hold the current piece (swap with held piece). */
    fun hold() {
        if (holdUsed) return
        val p = currentPiece ?: return
        val currentType = p.type
        if (holdPiece == null) {
            holdPiece = currentType
            currentPiece = null
            spawnPiece()
        } else {
            val heldType = holdPiece!!
            holdPiece = currentType
            currentPiece = null
            // Spawn the held piece
            val piece = when (heldType) {
                PieceType.I -> Piece(heldType, x = 3, y = 1)
                else -> Piece(heldType, x = 4, y = 0)
            }
            if (board.isGameOver(piece)) {
                isGameOver = true
                status = GameStatus.OVER
            } else {
                currentPiece = piece
            }
        }
        holdUsed = true
        dropAccumulator = 0
        lockAccumulator = 0
        lockResets = 0
        isGrounded = false
        lastActionWasRotate = false
    }

    /** Lock the current piece and process line clears. */
    private fun lockPiece() {
        val p = currentPiece ?: return

        // Check T-Spin BEFORE locking (need board state)
        val tspin = board.checkTSpin(p)

        board.lock(p)
        piecesPlaced++

        val clearResult = board.clearLines()
        val cleared = clearResult.linesCleared

        // Check Perfect Clear (board empty after clear)
        val perfectClear = cleared > 0 && board.isEmpty()

        // Calculate score
        val isDifficult = cleared > 0 && (cleared == 4 || tspin != TSpinType.NONE)
        val b2bEligible = isDifficult && b2bActive

        // Base score
        val baseScore: Int = if (tspin != TSpinType.NONE) {
            // T-Spin scoring: [0, 800, 1200, 1600] for mini/full × lines
            val tspinLines = min(cleared, 3)
            val tspinBase = TSPIN_SCORES[tspinLines]
            if (tspin == TSpinType.MINI) tspinBase / 2 else tspinBase // Mini gets half
        } else {
            // Regular scoring: [0, 100, 300, 500, 800]
            REGULAR_SCORES[min(cleared, 4)]
        }

        var totalScore = baseScore.toLong() * level

        // Back-to-Back bonus (1.5×)
        if (b2bEligible) {
            totalScore = (totalScore * 1.5).toLong()
        }

        // Combo bonus
        if (cleared > 0) {
            combo++
            if (combo > 0) {
                totalScore += 50L * combo * level
            }
            maxCombo = max(maxCombo, combo)
        } else {
            combo = -1
        }

        // Perfect Clear bonus
        if (perfectClear) {
            totalScore += 2000L * level
            perfectClears++
        }

        // Update B2B state
        if (cleared > 0) {
            if (isDifficult) {
                if (b2bActive) {
                    maxB2b++
                }
                b2bActive = true
            } else {
                b2bActive = false
            }
        }

        // Track stats
        if (cleared == 4) tetrises++
        if (tspin != TSpinType.NONE) tspins++
        lastClearWasTSpin = tspin
        lastClearWasDifficult = isDifficult
        lastClearWasPerfect = perfectClear

        // Update lines and level
        lines += cleared
        if (mode.levelUp) {
            level = lines / 10 + 1
        }

        score += totalScore

        // Spawn next piece
        currentPiece = null
        spawnPiece()
    }

    /** Reset lock delay (called when piece moves/rotates while grounded). */
    private fun resetLockDelay() {
        if (lockResets < maxLockResets) {
            lockAccumulator = 0
            lockResets++
        }
    }

    /** Update game state (called on each frame). deltaMs = milliseconds since last update. */
    fun update(deltaMs: Long) {
        if (isGameOver || isPaused || status != GameStatus.PLAYING || currentPiece == null) return

        // Handle lock delay if grounded
        if (isGrounded) {
            lockAccumulator += deltaMs
            if (lockAccumulator >= lockDelayMs) {
                lockPiece()
                return
            }
            // Check if piece can still move down (might have been rotated into space)
            val p = currentPiece
            if (p != null && board.isValid(p.moved(0, 1))) {
                isGrounded = false
                lockAccumulator = 0
            }
            return
        }

        // Gravity
        val interval = if (softDropping) softDropIntervalMs else dropIntervalMs
        dropAccumulator += deltaMs
        while (dropAccumulator >= interval) {
            dropAccumulator -= interval
            val moved = softDrop()
            if (!moved) {
                // Piece is grounded, will be locked by lock delay
                break
            }
        }
    }

    /** Update countdown. Returns true if countdown finished this update. */
    fun updateCountdown(deltaMs: Long): Boolean {
        if (status != GameStatus.COUNTDOWN) return false
        // Countdown steps every 650ms
        countdownAccumulator += deltaMs
        if (countdownAccumulator >= 650L) {
            countdownAccumulator -= 650L
            countdown--
            if (countdown < 0) {
                status = GameStatus.PLAYING
                return true
            }
        }
        return false
    }

    private var countdownAccumulator = 0L

    /** Pause/unpause the game. */
    fun togglePause() {
        if (!isGameOver && status == GameStatus.PLAYING) {
            isPaused = !isPaused
        }
    }

    /** Get the ghost piece (shadow showing where piece will land). */
    fun getGhostPiece(): Piece? {
        val p = currentPiece ?: return null
        return board.ghostDrop(p)
    }

    /** Get the highest filled row (for danger detection). */
    fun getDangerLevel(): Int = board.highestFilledRow()

    /** Check if in danger zone (board >= 15 rows filled). */
    fun isInDanger(): Boolean = board.highestFilledRow() >= 15

    companion object {
        /** Regular line clear scores: [0, single, double, triple, tetris]. */
        val REGULAR_SCORES = intArrayOf(0, 100, 300, 500, 800)

        /** T-Spin clear scores: [0, single, double, triple]. */
        val TSPIN_SCORES = intArrayOf(0, 800, 1200, 1600)

        /** Perfect Clear bonus multiplier. */
        const val PERFECT_CLEAR_BONUS = 2000

        /** Back-to-Back multiplier. */
        const val B2B_MULTIPLIER = 1.5

        /** Combo bonus per combo × level. */
        const val COMBO_BONUS = 50
    }
}

/** Game modes matching the original. */
enum class GameMode(
    val fixedLevel: Int? = null,
    val levelUp: Boolean = false,
    val timed: Boolean = false,
    val countdownTimer: Boolean = false,
    val scoreBased: Boolean = false,
    val timeRecord: Boolean = false,
    val cheeseStart: Boolean = false
) {
    CLASSIC(levelUp = true, scoreBased = true),
    SPRINT(fixedLevel = 1, timed = true, timeRecord = true),
    ULTRA(fixedLevel = null, levelUp = true, timed = true, countdownTimer = true, scoreBased = true),
    ZEN(fixedLevel = 2, scoreBased = true),
    CHEESE(fixedLevel = 3, timed = true, timeRecord = true, cheeseStart = true),
    MISSION(levelUp = true, scoreBased = true),
    DAILY(fixedLevel = 4, scoreBased = true),
    BATTLE(levelUp = true, scoreBased = true),
    ADVENTURE(levelUp = true, scoreBased = true)
}

/** Game status. */
enum class GameStatus {
    COUNTDOWN,
    PLAYING,
    PAUSED,
    OVER
}

/**
 * Wrapper to use SeededRandom as kotlin.random.Random.
 */
class SeededKotlinRandom(private val seeded: SeededRandom) : kotlin.random.Random() {
    override fun nextBits(bitCount: Int): Int {
        return seeded.nextInt(1 shl bitCount)
    }

    override fun nextInt(until: Int): Int = seeded.nextInt(until)
    override fun nextFloat(): Float = seeded.nextFloat()
}
