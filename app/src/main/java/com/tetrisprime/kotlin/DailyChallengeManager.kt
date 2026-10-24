package com.tetrisprime.kotlin

import android.content.Context

/**
 * Phase 8.1 — Daily challenge system.
 *
 * - Goal: FNV-1a hash of "goal/YYYY-MM-DD" → 9000 + (hash % 25) × 250
 * - Seeded piece sequence: FNV-1a hash of "pieces/YYYY-MM-DD" seeds SeededRandom
 * - Streak: increments each consecutive day a daily challenge is won; resets to 0
 *   if a day is skipped or the challenge is failed.
 */
object DailyChallengeManager {

    private const val PREFS = "tetris_prime_daily"
    private const val KEY_LAST_PLAYED = "last_played_date"
    private const val KEY_STREAK = "streak"
    private const val KEY_LAST_WON = "last_won_date"
    private const val KEY_TOTAL_WON = "total_won"
    private const val KEY_BEST_SCORE = "best_score"

    data class DailyState(
        val today: String,            // YYYY-MM-DD
        val goal: Int,
        val streak: Int,
        val lastWonDate: String?,     // YYYY-MM-DD of last win
        val totalWon: Int,
        val bestScore: Long,
        val alreadyWonToday: Boolean,
        val seededRandom: SeededRandom
    )

    private fun prefs(ctx: Context) =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Get today's date string in YYYY-MM-DD format. */
    fun todayString(): String = java.time.LocalDate.now().toString()

    /** Get the daily goal for a given date (or today if null). */
    fun goalFor(dateStr: String? = null): Int =
        Fnv1aHash.dailyGoal(dateStr ?: todayString())

    /** Build a SeededRandom for the given date's piece sequence. */
    fun seededRandomFor(dateStr: String? = null): SeededRandom {
        val seed = Fnv1aHash.hash("pieces/${dateStr ?: todayString()}")
        return SeededRandom(seed)
    }

    /** Load the current daily challenge state. */
    fun load(ctx: Context): DailyState {
        val p = prefs(ctx)
        val today = todayString()
        val lastWon = p.getString(KEY_LAST_WON, null)
        return DailyState(
            today = today,
            goal = goalFor(today),
            streak = p.getInt(KEY_STREAK, 0),
            lastWonDate = lastWon,
            totalWon = p.getInt(KEY_TOTAL_WON, 0),
            bestScore = p.getLong(KEY_BEST_SCORE, 0L),
            alreadyWonToday = lastWon == today,
            seededRandom = seededRandomFor(today)
        )
    }

    /**
     * Record a daily-challenge win.
     * Updates streak, last-won date, total-won count, and best score.
     * Returns the new streak.
     */
    fun recordWin(ctx: Context, score: Long): Int {
        val p = prefs(ctx)
        val today = todayString()
        val lastWon = p.getString(KEY_LAST_WON, null)
        val prevStreak = p.getInt(KEY_STREAK, 0)

        // Compute new streak: if lastWon is yesterday, increment; if today, no change;
        // otherwise reset to 1
        val newStreak = when {
            lastWon == null -> 1
            lastWon == today -> prevStreak  // already won today — no double count
            isYesterday(lastWon, today) -> prevStreak + 1
            else -> 1  // streak broken — start fresh
        }

        val prevBest = p.getLong(KEY_BEST_SCORE, 0L)
        p.edit()
            .putString(KEY_LAST_WON, today)
            .putInt(KEY_STREAK, newStreak)
            .putInt(KEY_TOTAL_WON, p.getInt(KEY_TOTAL_WON, 0) + if (lastWon != today) 1 else 0)
            .putLong(KEY_BEST_SCORE, maxOf(prevBest, score))
            .apply()

        return newStreak
    }

    /**
     * Mark that the user played (but did not win) today.
     * Used for "last played" tracking — does not break an existing streak
     * (only skipping a full day breaks it).
     */
    fun recordPlay(ctx: Context) {
        prefs(ctx).edit().putString(KEY_LAST_PLAYED, todayString()).apply()
    }

    /** True if `yesterday` is the calendar day before `today`. */
    private fun isYesterday(yesterday: String, today: String): Boolean {
        return try {
            val yd = java.time.LocalDate.parse(yesterday)
            val td = java.time.LocalDate.parse(today)
            yd.plusDays(1) == td
        } catch (e: Exception) { false }
    }

    /** Format the streak for display. */
    fun streakLabel(streak: Int): String = when (streak) {
        0    -> "ابدأ سلسلتك اليوم!"
        1    -> "أول فوز! 🔥"
        in 2..6  -> "سلسلة $streak أيام 🔥"
        in 7..13 -> "سلسلة $streak يوم! ⚡"
        in 14..29 -> "سلسلة $streak يوم! 🌟"
        in 30..99 -> "أسطورة! $streak يوم متتالي 👑"
        else -> "أسطورة حية! $streak يوم 👑💎"
    }
}
