package com.tetrisprime.kotlin

import android.content.Context

/**
 * Phase 8.7 — Continue system after game over.
 *
 * Three continue options:
 * 1. Watch an ad → free continue (clears top 10 rows, resumes play)
 * 2. Pay coins → continue (cost scales with usage: 100, 200, 400, ...)
 * 3. Clear top 10 rows only (free, but no score protection)
 *
 * Each continue resets the lock delay and unsets isGameOver.
 */
object ContinueSystem {

    private const val PREFS = "tetris_prime_continue"
    private const val KEY_CONTINUES_USED = "continues_used_today"
    private const val KEY_LAST_RESET_DATE = "last_reset_date"
    private const val KEY_FREE_AD_USED = "ad_used_today"

    private const val MAX_DAILY_AD_CONTINUES = 3
    private val COIN_COSTS = intArrayOf(100, 200, 400, 800, 1600)  // exponential scaling

    data class ContinueState(
        val adContinuesRemaining: Int,
        val coinContinueCost: Int,
        val totalContinuesUsed: Int
    )

    private fun prefs(ctx: Context) =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun todayStr() = java.time.LocalDate.now().toString()

    /** Reset daily counters if it's a new day. */
    private fun maybeResetDaily(ctx: Context) {
        val p = prefs(ctx)
        val lastReset = p.getString(KEY_LAST_RESET_DATE, null)
        val today = todayStr()
        if (lastReset != today) {
            p.edit()
                .putString(KEY_LAST_RESET_DATE, today)
                .putInt(KEY_CONTINUES_USED, 0)
                .putBoolean(KEY_FREE_AD_USED, false)
                .apply()
        }
    }

    /** Get the current continue state (resets daily counters if needed). */
    fun getState(ctx: Context): ContinueState {
        maybeResetDaily(ctx)
        val p = prefs(ctx)
        val adUsed = p.getBoolean(KEY_FREE_AD_USED, false)
        val totalUsed = p.getInt(KEY_CONTINUES_USED, 0)
        val adRemaining = if (adUsed) MAX_DAILY_AD_CONTINUES - 1 else MAX_DAILY_AD_CONTINUES
        val cost = COIN_COSTS[minOf(totalUsed, COIN_COSTS.size - 1)]
        return ContinueState(
            adContinuesRemaining = adRemaining,
            coinContinueCost = cost,
            totalContinuesUsed = totalUsed
        )
    }

    /**
     * Apply a continue to the engine. Clears top 10 rows, unsets game over,
     * and returns the engine to PLAYING status.
     *
     * @return true if the continue was applied successfully.
     */
    fun applyContinue(engine: ModeGameEngine): Boolean {
        if (!engine.isGameOver) return false
        // Clear top 10 filled rows to give the player breathing room
        val board = engine.board
        val cleared = board.clearTopRows(10)
        if (cleared == 0) {
            // Even if no rows to clear, still allow continue
        }
        engine.resumeAfterContinue()
        return true
    }

    /** Record that the user used an ad continue today. */
    fun recordAdContinue(ctx: Context) {
        maybeResetDaily(ctx)
        val p = prefs(ctx)
        p.edit()
            .putBoolean(KEY_FREE_AD_USED, true)
            .putInt(KEY_CONTINUES_USED, p.getInt(KEY_CONTINUES_USED, 0) + 1)
            .apply()
    }

    /**
     * Record that the user used a coin continue. Charges coins via StoreManager.
     * Returns true if the charge succeeded.
     */
    fun recordCoinContinue(ctx: Context): Boolean {
        val state = getState(ctx)
        if (!StoreManager.spend(ctx, state.coinContinueCost)) return false
        maybeResetDaily(ctx)
        val p = prefs(ctx)
        p.edit()
            .putInt(KEY_CONTINUES_USED, p.getInt(KEY_CONTINUES_USED, 0) + 1)
            .apply()
        return true
    }

    /** Record that the user used the free "clear top 10" option. */
    fun recordFreeContinue(ctx: Context) {
        maybeResetDaily(ctx)
        val p = prefs(ctx)
        p.edit()
            .putInt(KEY_CONTINUES_USED, p.getInt(KEY_CONTINUES_USED, 0) + 1)
            .apply()
    }
}
