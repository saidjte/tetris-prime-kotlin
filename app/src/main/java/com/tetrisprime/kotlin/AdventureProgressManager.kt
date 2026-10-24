package com.tetrisprime.kotlin

import android.content.Context

/**
 * Phase 8.3 — Adventure progress tracking.
 *
 * 36 stages across 6 worlds. Each world unlocks when the previous one's
 * final (6th) stage is completed. Tokens collected per stage are persisted
 * so the player can see what they've gathered.
 */
object AdventureProgressManager {

    private const val PREFS = "tetris_prime_adventure"
    private const val KEY_COMPLETED = "completed_set"
    private const val KEY_TOKENS = "collected_tokens"
    private const val KEY_HIGHEST_UNLOCKED = "highest_unlocked_stage"

    /** World names (6 worlds). */
    val WORLD_NAMES = listOf(
        "وادي البداية",
        "غابة الكريستال",
        "جبال النار",
        "محيط الظلام",
        "سماء الرعد",
        "قصر الأبدي"
    )

    /** World accent colors. */
    val WORLD_COLORS = listOf(
        0xFF34D399.toInt(),  // green
        0xFF22D3EE.toInt(),  // cyan
        0xFFF97316.toInt(),  // orange
        0xFF8B5CF6.toInt(),  // purple
        0xFFFBBF24.toInt(),  // yellow
        0xFFFB7185.toInt()   // rose
    )

    data class StageState(
        val stage: AdventureDefinitions.Stage,
        val index: Int,
        val isUnlocked: Boolean,
        val isCompleted: Boolean,
        val collectedTokens: Set<AdventureDefinitions.TokenType>
    )

    data class WorldState(
        val world: Int,                  // 1..6
        val name: String,
        val accentColor: Int,
        val stages: List<StageState>,
        val isUnlocked: Boolean,
        val completedCount: Int
    )

    private fun prefs(ctx: Context) =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun loadCompleted(ctx: Context): Set<Int> {
        val raw = prefs(ctx).getStringSet(KEY_COMPLETED, emptySet()) ?: emptySet()
        return raw.mapNotNull { it.toIntOrNull() }.toSet()
    }

    private fun loadTokens(ctx: Context): Map<Int, Set<String>> {
        val raw = prefs(ctx).getString(KEY_TOKENS, "") ?: ""
        if (raw.isBlank()) return emptyMap()
        return raw.split(";").mapNotNull { entry ->
            val parts = entry.split(":")
            if (parts.size != 2) return@mapNotNull null
            val stageIdx = parts[0].toIntOrNull() ?: return@mapNotNull null
            val tokens = if (parts[1].isBlank()) emptySet()
                         else parts[1].split(",").toSet()
            stageIdx to tokens
        }.toMap()
    }

    private fun loadHighestUnlocked(ctx: Context): Int =
        prefs(ctx).getInt(KEY_HIGHEST_UNLOCKED, 0)  // stage 0 always unlocked

    /** Get all 36 stages with state. */
    fun getAllStages(ctx: Context): List<StageState> {
        val all = AdventureDefinitions.generateAll()
        val completed = loadCompleted(ctx)
        val tokens = loadTokens(ctx)
        val highest = loadHighestUnlocked(ctx)
        return all.mapIndexed { idx, s ->
            StageState(
                stage = s,
                index = idx,
                isUnlocked = idx <= highest,
                isCompleted = completed.contains(idx),
                collectedTokens = (tokens[idx] ?: emptySet()).mapNotNull { name ->
                    try { AdventureDefinitions.TokenType.valueOf(name) } catch (e: Exception) { null }
                }.toSet()
            )
        }
    }

    /** Group stages by world (6 worlds × 6 stages each). */
    fun getWorlds(ctx: Context): List<WorldState> {
        val stages = getAllStages(ctx)
        return (1..6).map { world ->
            val worldStages = stages.filter { it.stage.world == world }
            val isUnlocked = worldStages.first().isUnlocked
            WorldState(
                world = world,
                name = WORLD_NAMES[world - 1],
                accentColor = WORLD_COLORS[world - 1],
                stages = worldStages,
                isUnlocked = isUnlocked,
                completedCount = worldStages.count { it.isCompleted }
            )
        }
    }

    /** Get a specific stage by index. */
    fun getStage(ctx: Context, index: Int): StageState? =
        getAllStages(ctx).getOrNull(index)

    /** Mark a stage completed + record tokens. Unlocks next stage. */
    fun completeStage(ctx: Context, index: Int, tokens: Set<AdventureDefinitions.TokenType>): StageState? {
        val all = getAllStages(ctx)
        if (index !in all.indices) return null
        val state = all[index]
        if (!state.isUnlocked) return null

        val completed = loadCompleted(ctx).toMutableSet()
        completed.add(index)

        val tokensMap = loadTokens(ctx).toMutableMap()
        tokensMap[index] = tokens.map { it.name }.toSet()

        val highest = loadHighestUnlocked(ctx)
        val newHighest = if (index == highest && index + 1 < all.size) index + 1 else highest

        prefs(ctx).edit()
            .putStringSet(KEY_COMPLETED, completed.map { it.toString() }.toSet())
            .putString(KEY_TOKENS, tokensMap.entries.joinToString(";") { "${it.key}:${it.value.joinToString(",")}" })
            .putInt(KEY_HIGHEST_UNLOCKED, newHighest)
            .apply()

        return getStage(ctx, index)
    }

    /** Total completed stages (out of 36). */
    fun completedCount(ctx: Context): Int = loadCompleted(ctx).size

    /** Count of a specific token type collected across all stages. */
    fun countToken(ctx: Context, type: AdventureDefinitions.TokenType): Int {
        val tokens = loadTokens(ctx)
        return tokens.values.count { it.contains(type.name) }
    }

    const val TOTAL_STAGES = 36
    const val STAGES_PER_WORLD = 6
    const val TOTAL_WORLDS = 6
}
