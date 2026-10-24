package com.tetrisprime.kotlin

import android.content.Context

/**
 * 21 player metrics persisted across sessions.
 * Used by Stats screen, achievements, leaderboard tie-breakers.
 */
data class PlayerMetrics(
    // Aggregate play stats
    val gamesPlayed: Int = 0,            // 1
    val totalTimeSec: Long = 0L,         // 2
    val totalPieces: Long = 0L,          // 3
    val totalLines: Long = 0L,           // 4
    val totalScore: Long = 0L,           // 5
    // Clear-type counts
    val singleClears: Long = 0L,         // 6
    val doubleClears: Long = 0L,         // 7
    val tripleClears: Long = 0L,         // 8
    val tetrisClears: Long = 0L,         // 9
    val tspinMiniCount: Long = 0L,       // 10
    val tspinFullCount: Long = 0L,       // 11
    val perfectClears: Long = 0L,        // 12
    // Combo / chain
    val maxCombo: Int = 0,               // 13
    val maxB2bChain: Int = 0,            // 14
    // Records
    val bestScore: Long = 0L,            // 15
    val bestSprintMs: Long = 0L,         // 16 — 0 means not completed
    val bestUltraScore: Long = 0L,       // 17
    val bestComboInRun: Int = 0,         // 18
    // Mode-specific completions
    val cheeseCompleted: Int = 0,        // 19
    val missionsCompleted: Int = 0,      // 20
    val dailyChallengesDone: Int = 0     // 21
)

/**
 * Persistent stats store.
 */
object StatsManager {

    private const val PREFS = "tetris_prime_stats"

    private fun prefs(ctx: Context) =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load(ctx: Context): PlayerMetrics {
        val p = prefs(ctx)
        return PlayerMetrics(
            gamesPlayed = p.getInt("gamesPlayed", 0),
            totalTimeSec = p.getLong("totalTimeSec", 0L),
            totalPieces = p.getLong("totalPieces", 0L),
            totalLines = p.getLong("totalLines", 0L),
            totalScore = p.getLong("totalScore", 0L),
            singleClears = p.getLong("singleClears", 0L),
            doubleClears = p.getLong("doubleClears", 0L),
            tripleClears = p.getLong("tripleClears", 0L),
            tetrisClears = p.getLong("tetrisClears", 0L),
            tspinMiniCount = p.getLong("tspinMiniCount", 0L),
            tspinFullCount = p.getLong("tspinFullCount", 0L),
            perfectClears = p.getLong("perfectClears", 0L),
            maxCombo = p.getInt("maxCombo", 0),
            maxB2bChain = p.getInt("maxB2bChain", 0),
            bestScore = p.getLong("bestScore", 0L),
            bestSprintMs = p.getLong("bestSprintMs", 0L),
            bestUltraScore = p.getLong("bestUltraScore", 0L),
            bestComboInRun = p.getInt("bestComboInRun", 0),
            cheeseCompleted = p.getInt("cheeseCompleted", 0),
            missionsCompleted = p.getInt("missionsCompleted", 0),
            dailyChallengesDone = p.getInt("dailyChallengesDone", 0)
        )
    }

    /** Save a full PlayerMetrics snapshot. */
    fun save(ctx: Context, m: PlayerMetrics) {
        prefs(ctx).edit().apply {
            putInt("gamesPlayed", m.gamesPlayed)
            putLong("totalTimeSec", m.totalTimeSec)
            putLong("totalPieces", m.totalPieces)
            putLong("totalLines", m.totalLines)
            putLong("totalScore", m.totalScore)
            putLong("singleClears", m.singleClears)
            putLong("doubleClears", m.doubleClears)
            putLong("tripleClears", m.tripleClears)
            putLong("tetrisClears", m.tetrisClears)
            putLong("tspinMiniCount", m.tspinMiniCount)
            putLong("tspinFullCount", m.tspinFullCount)
            putLong("perfectClears", m.perfectClears)
            putInt("maxCombo", m.maxCombo)
            putInt("maxB2bChain", m.maxB2bChain)
            putLong("bestScore", m.bestScore)
            putLong("bestSprintMs", m.bestSprintMs)
            putLong("bestUltraScore", m.bestUltraScore)
            putInt("bestComboInRun", m.bestComboInRun)
            putInt("cheeseCompleted", m.cheeseCompleted)
            putInt("missionsCompleted", m.missionsCompleted)
            putInt("dailyChallengesDone", m.dailyChallengesDone)
        }.apply()
    }

    /**
     * Apply a delta from a finished game session.
     * Returns the updated snapshot.
     */
    fun applyGameResult(ctx: Context, result: GameResult, runtimeMs: Long): PlayerMetrics {
        val cur = load(ctx)
        val next = cur.copy(
            gamesPlayed = cur.gamesPlayed + 1,
            totalTimeSec = cur.totalTimeSec + (runtimeMs / 1000),
            totalPieces = cur.totalPieces + (result.piecesPlaced ?: 0).toLong(),
            totalLines = cur.totalLines + result.lines.toLong(),
            totalScore = cur.totalScore + result.score,
            singleClears = cur.singleClears + (result.singleClears ?: 0).toLong(),
            doubleClears = cur.doubleClears + (result.doubleClears ?: 0).toLong(),
            tripleClears = cur.tripleClears + (result.tripleClears ?: 0).toLong(),
            tetrisClears = cur.tetrisClears + (result.tetrises ?: 0).toLong(),
            tspinMiniCount = cur.tspinMiniCount + (result.tspinMini ?: 0).toLong(),
            tspinFullCount = cur.tspinFullCount + (result.tspinFull ?: 0).toLong(),
            perfectClears = cur.perfectClears + (result.perfectClears ?: 0).toLong(),
            maxCombo = maxOf(cur.maxCombo, result.maxCombo ?: 0),
            maxB2bChain = maxOf(cur.maxB2bChain, result.maxB2b ?: 0),
            bestScore = maxOf(cur.bestScore, result.score),
            bestSprintMs = when {
                result.mode == GameMode.SPRINT && result.isWin ->
                    if (cur.bestSprintMs == 0L) result.timeMs
                    else minOf(cur.bestSprintMs, result.timeMs)
                else -> cur.bestSprintMs
            },
            bestUltraScore = if (result.mode == GameMode.ULTRA)
                maxOf(cur.bestUltraScore, result.score) else cur.bestUltraScore,
            bestComboInRun = maxOf(cur.bestComboInRun, result.maxCombo ?: 0),
            cheeseCompleted = cur.cheeseCompleted + (if (result.mode == GameMode.CHEESE && result.isWin) 1 else 0),
            missionsCompleted = cur.missionsCompleted + (if (result.mode == GameMode.MISSION && result.isWin) 1 else 0),
            dailyChallengesDone = cur.dailyChallengesDone + (if (result.mode == GameMode.DAILY && result.isWin) 1 else 0)
        )
        save(ctx, next)
        return next
    }
}
