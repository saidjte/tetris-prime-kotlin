package com.tetrisprime.kotlin

/**
 * Mode configuration and unlock requirements.
 * Matches the original game's mode system.
 */
object ModeConfig {

    /** Modes shown in main menu carousel. */
    val MENU_MODES = listOf(GameMode.CLASSIC, GameMode.SPRINT, GameMode.ULTRA, GameMode.ZEN, GameMode.CHEESE)

    /** All selectable modes. */
    val ALL_MODES = listOf(
        GameMode.CLASSIC, GameMode.SPRINT, GameMode.ULTRA, GameMode.ZEN,
        GameMode.CHEESE, GameMode.MISSION, GameMode.ADVENTURE, GameMode.BATTLE, GameMode.DAILY
    )

    /** Unlock requirements per mode.
     *  null = unlocked by default.
     *  Pair(metric, threshold) = requires metric >= threshold. */
    val UNLOCK_REQUIREMENTS: Map<GameMode, Pair<String, Int>?> = mapOf(
        GameMode.CLASSIC to null,
        GameMode.SPRINT to ("games" to 5),
        GameMode.ZEN to ("games" to 10),
        GameMode.ULTRA to ("totalLines" to 250),
        GameMode.CHEESE to ("totalLines" to 500),
        GameMode.MISSION to null,
        GameMode.DAILY to null,
        GameMode.BATTLE to null,
    )

    /** Check if a mode is unlocked given current stats. */
    fun isUnlocked(mode: GameMode, stats: PlayerStats): Boolean {
        val req = UNLOCK_REQUIREMENTS[mode] ?: return true
        val (metric, threshold) = req
        val currentValue = when (metric) {
            "games" -> stats.games
            "totalLines" -> stats.totalLines
            else -> 0
        }
        return currentValue >= threshold
    }

    /** Get unlock requirement description for a mode. */
    fun getUnlockDescription(mode: GameMode): String? {
        val req = UNLOCK_REQUIREMENTS[mode] ?: return null
        val (metric, threshold) = req
        return when (metric) {
            "games" -> "العب $threshold ألعاب"
            "totalLines" -> "امسح $threshold خط"
            else -> null
        }
    }

    /** Sprint goal: 40 lines. */
    const val SPRINT_GOAL_LINES = 40

    /** Ultra duration: 120 seconds. */
    const val ULTRA_DURATION_MS = 120_000L

    /** Ultra time announcements. */
    const val ULTRA_60S_ANNOUNCE = 60_000L
    const val ULTRA_30S_ANNOUNCE = 30_000L
    const val ULTRA_HURRY_ANNOUNCE = 10_000L

    /** Cheese initial rows. */
    const val CHEESE_INITIAL_ROWS = 8

    /** Daily goal range. */
    const val DAILY_GOAL_MIN = 9000
    const val DAILY_GOAL_MAX = 15250
}

/** Player statistics used for mode unlocking. */
data class PlayerStats(
    val games: Int = 0,
    val totalLines: Int = 0,
    val totalPieces: Int = 0,
    val tetrises: Int = 0,
    val tspins: Int = 0,
    val perfectClears: Int = 0,
    val maxCombo: Int = 0,
    val maxB2b: Int = 0
)

/** Result of a game session. */
data class GameResult(
    val mode: GameMode,
    val score: Long,
    val lines: Int,
    val timeMs: Long,
    val isWin: Boolean,
    val isNewRecord: Boolean,
    val overReason: GameOverReason,
    // Optional detailed stats (populated by GameEngine for StatsManager)
    val piecesPlaced: Int? = null,
    val singleClears: Int? = null,
    val doubleClears: Int? = null,
    val tripleClears: Int? = null,
    val tetrises: Int? = null,
    val tspinMini: Int? = null,
    val tspinFull: Int? = null,
    val perfectClears: Int? = null,
    val maxCombo: Int? = null,
    val maxB2b: Int? = null
)

/** Game over reasons. */
enum class GameOverReason {
    TOPOUT,      // board overflow
    GOAL,        // sprint/cheese/mission/adventure objective met
    TIME,        // ultra timer expired
    BATTLE_LOSS, // AI defeated player
    BATTLE_WIN   // player defeated AI
}
