package com.tetrisprime.kotlin

import android.content.Context

/**
 * 18 achievements with unlock conditions evaluated against PlayerMetrics.
 */
data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val icon: String,
    val goal: Long,
    val currentValue: (PlayerMetrics) -> Long
)

object AchievementDefinitions {

    val ALL: List<Achievement> = listOf(
        Achievement("first_game", "أول خطوة", "أكمل أول لعبة", "🎮", 1L,
            { m -> m.gamesPlayed.toLong() }),
        Achievement("games_10", "لاعب مبتدئ", "العب 10 ألعاب", "🌱", 10L,
            { m -> m.gamesPlayed.toLong() }),
        Achievement("games_50", "لاعب شغوف", "العب 50 لعبة", "🔥", 50L,
            { m -> m.gamesPlayed.toLong() }),
        Achievement("games_100", "محترف", "العب 100 لعبة", "👑", 100L,
            { m -> m.gamesPlayed.toLong() }),
        Achievement("lines_100", "ممسّح الخطوط", "امسح 100 خط", "🧹", 100L,
            { m -> m.totalLines }),
        Achievement("lines_500", "عامل النظافة", "امسح 500 خط", "🪣", 500L,
            { m -> m.totalLines }),
        Achievement("lines_2000", "إعصار", "امسح 2000 خط", "🌪️", 2000L,
            { m -> m.totalLines }),
        Achievement("tetris_10", "محب التيتريس", "حقق 10 تيتريس", "🎯", 10L,
            { m -> m.tetrisClears }),
        Achievement("tetris_50", "سيد التيتريس", "حقق 50 تيتريس", "🏆", 50L,
            { m -> m.tetrisClears }),
        Achievement("tspin_first", "أول T-Spin", "حقق أول T-Spin", "💫", 1L,
            { m -> m.tspinFullCount + m.tspinMiniCount }),
        Achievement("tspin_20", "محترف T-Spin", "حقق 20 T-Spin", "⭐", 20L,
            { m -> m.tspinFullCount + m.tspinMiniCount }),
        Achievement("pc_first", "أول Perfect Clear", "حقق أول مسح كامل", "✨", 1L,
            { m -> m.perfectClears }),
        Achievement("pc_5", "سيد المسح الكامل", "حقق 5 مسح كامل", "🌟", 5L,
            { m -> m.perfectClears }),
        Achievement("combo_5", "كومبو 5", "حقق كومبو من 5", "⚡", 5L,
            { m -> m.maxCombo.toLong() }),
        Achievement("combo_10", "كومبو 10", "حقق كومبو من 10", "💥", 10L,
            { m -> m.maxCombo.toLong() }),
        Achievement("b2b_5", "سلسلة B2B 5", "حقق سلسلة B2B من 5", "🔗", 5L,
            { m -> m.maxB2bChain.toLong() }),
        Achievement("sprint_done", "سبرينت مكتمل", "أكمل نمط سبرينت 40", "🏃", 1L,
            { m -> if (m.bestSprintMs > 0L) 1L else 0L }),
        Achievement("score_100k", "نقاط 100k", "تجاوز 100,000 نقطة في لعبة", "💯", 100_000L,
            { m -> m.bestScore })
    )

    /** Evaluate which achievements are unlocked given metrics. */
    fun unlocked(m: PlayerMetrics): Set<String> =
        ALL.filter { it.currentValue(m) >= it.goal }.map { it.id }.toSet()

    /** Persist unlocked achievements (extra: anything ever unlocked). */
    fun loadUnlocked(ctx: Context): Set<String> {
        val p = ctx.getSharedPreferences("tetris_prime_ach", Context.MODE_PRIVATE)
        return p.getStringSet("unlocked", emptySet()) ?: emptySet()
    }

    /** Merge newly-unlocked with stored set; persist; return new set + newly-unlocked delta. */
    fun updateUnlocked(ctx: Context, metrics: PlayerMetrics): Pair<Set<String>, Set<String>> {
        val stored = loadUnlocked(ctx).toMutableSet()
        val current = unlocked(metrics)
        val newlyUnlocked = current - stored
        val merged = stored + newlyUnlocked
        ctx.getSharedPreferences("tetris_prime_ach", Context.MODE_PRIVATE)
            .edit().putStringSet("unlocked", merged).apply()
        return merged to newlyUnlocked
    }
}
