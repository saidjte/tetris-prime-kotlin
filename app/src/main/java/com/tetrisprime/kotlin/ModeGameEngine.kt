package com.tetrisprime.kotlin

import kotlin.math.max

/**
 * Extended game engine with mode-specific logic.
 * Handles win/lose conditions for each game mode.
 */
class ModeGameEngine(
    val mode: GameMode = GameMode.CLASSIC,
    private val seededRandom: SeededRandom? = null,
    private val missionGoal: MissionGoal? = null,
    private val dailyDateStr: String? = null
) : GameEngine(mode, seededRandom) {

    // Mode-specific state
    private var elapsedTimeMs = 0L
    private var ultra60Announced = false
    private var ultra30Announced = false
    private var hurryAnnounced = false

    // Callbacks for UI events
    var onWin: (() -> Unit)? = null
    var onTimeUp: (() -> Unit)? = null
    var onUltra60: (() -> Unit)? = null
    var onUltra30: (() -> Unit)? = null
    var onHurry: (() -> Unit)? = null
    var onCheeseProgress: ((remaining: Int) -> Unit)? = null
    var onMissionProgress: ((current: Int, goal: Int) -> Unit)? = null

    /** Get the game result when game ends. */
    fun getGameResult(): GameResult {
        val isWin = when (mode) {
            GameMode.SPRINT -> lines >= ModeConfig.SPRINT_GOAL_LINES
            GameMode.ULTRA -> true // ultra always "wins" (score-based)
            GameMode.CHEESE -> board.countCheeseCells() == 0
            GameMode.MISSION -> isMissionComplete()
            GameMode.ADVENTURE -> isMissionComplete()
            GameMode.BATTLE -> false // set externally
            GameMode.DAILY -> score >= getDailyGoal()
            else -> false
        }

        val reason = when {
            mode == GameMode.ULTRA && elapsedTimeMs >= ModeConfig.ULTRA_DURATION_MS -> GameOverReason.TIME
            isWin && mode == GameMode.BATTLE -> GameOverReason.BATTLE_WIN
            !isWin && mode == GameMode.BATTLE -> GameOverReason.BATTLE_LOSS
            isWin -> GameOverReason.GOAL
            else -> GameOverReason.TOPOUT
        }

        return GameResult(
            mode = mode,
            score = score,
            lines = lines,
            timeMs = elapsedTimeMs,
            isWin = isWin,
            isNewRecord = false, // set by caller
            overReason = reason
        )
    }

    /** Get the daily challenge goal. */
    fun getDailyGoal(): Int {
        return if (dailyDateStr != null) {
            Fnv1aHash.dailyGoal(dailyDateStr)
        } else {
            ModeConfig.DAILY_GOAL_MIN
        }
    }

    /** Check if mission/adventure is complete. */
    private fun isMissionComplete(): Boolean {
        val goal = missionGoal ?: return false
        return when (goal.kind) {
            MissionKind.LINES -> lines >= goal.n
            MissionKind.SCORE -> score >= goal.n
            MissionKind.TETRISES -> tetrises >= goal.n
            MissionKind.TSPINS -> tspins >= goal.n
            MissionKind.COMBO -> maxCombo >= goal.n
            MissionKind.PERFECT_CLEAR -> perfectClears >= goal.n
            MissionKind.SURVIVE -> elapsedTimeMs >= goal.n * 1000L
            MissionKind.LEVEL -> level >= goal.n
        }
    }

    /** Update with mode-specific logic. */
    fun updateMode(deltaMs: Long) {
        if (isGameOver || isPaused || status != GameStatus.PLAYING) return

        elapsedTimeMs += deltaMs

        // Ultra mode: time-based
        if (mode == GameMode.ULTRA) {
            // Announcements
            val remaining = ModeConfig.ULTRA_DURATION_MS - elapsedTimeMs
            if (!ultra60Announced && remaining <= ModeConfig.ULTRA_60S_ANNOUNCE) {
                ultra60Announced = true
                onUltra60?.invoke()
            }
            if (!ultra30Announced && remaining <= ModeConfig.ULTRA_30S_ANNOUNCE) {
                ultra30Announced = true
                onUltra30?.invoke()
            }
            if (!hurryAnnounced && remaining <= ModeConfig.ULTRA_HURRY_ANNOUNCE) {
                hurryAnnounced = true
                onHurry?.invoke()
            }
            // Time up
            if (elapsedTimeMs >= ModeConfig.ULTRA_DURATION_MS) {
                isGameOver = true
                status = GameStatus.OVER
                onTimeUp?.invoke()
                return
            }
        }

        // Sprint mode: win at 40 lines
        if (mode == GameMode.SPRINT && lines >= ModeConfig.SPRINT_GOAL_LINES) {
            isGameOver = true
            status = GameStatus.OVER
            onWin?.invoke()
            return
        }

        // Cheese mode: win when all cheese cleared
        if (mode == GameMode.CHEESE) {
            val remaining = board.countCheeseCells()
            onCheeseProgress?.invoke(remaining)
            if (remaining == 0) {
                isGameOver = true
                status = GameStatus.OVER
                onWin?.invoke()
                return
            }
        }

        // Mission/Adventure mode: check goal
        if (mode == GameMode.MISSION || mode == GameMode.ADVENTURE) {
            missionGoal?.let { goal ->
                val current = when (goal.kind) {
                    MissionKind.LINES -> lines
                    MissionKind.SCORE -> score.toInt()
                    MissionKind.TETRISES -> tetrises
                    MissionKind.TSPINS -> tspins
                    MissionKind.COMBO -> maxCombo
                    MissionKind.PERFECT_CLEAR -> perfectClears
                    MissionKind.SURVIVE -> (elapsedTimeMs / 1000).toInt()
                    MissionKind.LEVEL -> level
                }
                onMissionProgress?.invoke(current, goal.n)
                if (current >= goal.n) {
                    isGameOver = true
                    status = GameStatus.OVER
                    onWin?.invoke()
                    return
                }
            }
        }

        // Daily mode: win at goal score
        if (mode == GameMode.DAILY && score >= getDailyGoal()) {
            isGameOver = true
            status = GameStatus.OVER
            onWin?.invoke()
            return
        }

        // Call parent update for gravity/lock
        update(deltaMs)
    }

    /** Get elapsed time in seconds. */
    fun getElapsedSeconds(): Int = (elapsedTimeMs / 1000).toInt()

    /** Get remaining time for ultra mode. */
    fun getUltraRemainingMs(): Long {
        return if (mode == GameMode.ULTRA) {
            max(0, ModeConfig.ULTRA_DURATION_MS - elapsedTimeMs)
        } else 0
    }
}

/** Mission/adventure goal definition. */
data class MissionGoal(
    val id: Int,
    val kind: MissionKind,
    val n: Int,
    val level: Int? = null,
    val garbage: String? = null, // "off", "light", "heavy"
    val title: String? = null,
    val desc: String? = null
)

/** Mission kinds. */
enum class MissionKind {
    LINES,
    SCORE,
    TETRISES,
    TSPINS,
    COMBO,
    PERFECT_CLEAR,
    SURVIVE,
    LEVEL
}
