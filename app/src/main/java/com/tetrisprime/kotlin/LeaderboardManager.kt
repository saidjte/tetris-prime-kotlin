package com.tetrisprime.kotlin

import android.content.Context

/**
 * Per-mode leaderboard: top 10 entries.
 * For SPRINT/CHEESE, lower time is better; for others, higher score is better.
 */
data class LeaderboardEntry(
    val score: Long,        // For score-based: the score. For time-based: the time in ms.
    val lines: Int,
    val level: Int,
    val timestampMs: Long
)

object LeaderboardManager {

    private const val PREFS = "tetris_prime_leaderboard"
    private const val MAX_ENTRIES = 10

    private fun prefs(ctx: Context) =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun key(mode: GameMode) = "lb_${mode.name}"

    /** Returns true if this mode is time-based (lower = better). */
    fun isTimeBased(mode: GameMode): Boolean =
        mode == GameMode.SPRINT || mode == GameMode.CHEESE

    /** Get the top entries for a mode (sorted best-first). */
    fun getTop(ctx: Context, mode: GameMode): List<LeaderboardEntry> {
        val raw = prefs(ctx).getString(key(mode), "") ?: ""
        if (raw.isBlank()) return emptyList()
        return raw.split("\n").mapNotNull { line ->
            val parts = line.split(",")
            if (parts.size != 4) return@mapNotNull null
            try {
                LeaderboardEntry(
                    score = parts[0].toLong(),
                    lines = parts[1].toInt(),
                    level = parts[2].toInt(),
                    timestampMs = parts[3].toLong()
                )
            } catch (e: Exception) { null }
        }.let { list ->
            if (isTimeBased(mode)) list.sortedBy { it.score }
            else list.sortedByDescending { it.score }
        }.take(MAX_ENTRIES)
    }

    /**
     * Submit a new entry. Returns the new leaderboard after insertion
     * (or unchanged if not good enough).
     */
    fun submit(ctx: Context, mode: GameMode, entry: LeaderboardEntry): List<LeaderboardEntry> {
        val cur = getTop(ctx, mode).toMutableList()
        cur.add(entry)
        val sorted = if (isTimeBased(mode)) cur.sortedBy { it.score }
                     else cur.sortedByDescending { it.score }
        val top = sorted.take(MAX_ENTRIES)
        // Persist
        val raw = top.joinToString("\n") { "${it.score},${it.lines},${it.level},${it.timestampMs}" }
        prefs(ctx).edit().putString(key(mode), raw).apply()
        return top
    }

    /** True if the entry would make the top 10. */
    fun wouldQualify(ctx: Context, mode: GameMode, entry: LeaderboardEntry): Boolean {
        val cur = getTop(ctx, mode)
        if (cur.size < MAX_ENTRIES) return true
        val worst = cur.last()
        return if (isTimeBased(mode)) entry.score < worst.score
               else entry.score > worst.score
    }

    /** Best entry for a mode (or null). */
    fun best(ctx: Context, mode: GameMode): LeaderboardEntry? =
        getTop(ctx, mode).firstOrNull()
}
