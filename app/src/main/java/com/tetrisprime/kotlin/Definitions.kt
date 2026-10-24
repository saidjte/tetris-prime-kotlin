package com.tetrisprime.kotlin

import android.content.Context
import kotlin.random.Random

/**
 * Mission definitions — loads the 101 missions from tetris-content.json
 * (mirrors the original WebView app), with a programmatic fallback of
 * 100 generated missions when content isn't available.
 */
object MissionDefinitions {

    /** Cache of missions loaded from JSON (null = not yet loaded). */
    @Volatile private var cachedJsonMissions: List<MissionGoal>? = null

    /** Load all missions. If ctx is provided, attempts to load from
     *  tetris-content.json first; falls back to 100 generated missions. */
    fun generateAll(ctx: Context? = null): List<MissionGoal> {
        // Try JSON first
        if (ctx != null) {
            cachedJsonMissions?.let { return it }
            try {
                val jsonMissions = ContentLoader.getMissions(ctx).map { m ->
                    MissionGoal(
                        id = m.id,
                        kind = MissionKind.valueOf(m.kind.uppercase()),
                        n = m.n,
                        level = m.level,
                        title = m.title["ar"] ?: m.title["en"],
                        desc = m.desc["ar"] ?: m.desc["en"]
                    )
                }
                if (jsonMissions.isNotEmpty()) {
                    cachedJsonMissions = jsonMissions
                    return jsonMissions
                }
            } catch (e: Exception) {
                android.util.Log.w("MissionDefinitions", "JSON load failed, using generated: ${e.message}")
            }
        }
        // Fallback: generate 100 missions
        return generateFallback()
    }

    /** Generate 100 missions programmatically (fallback when JSON unavailable). */
    private fun generateFallback(): List<MissionGoal> {
        val missions = mutableListOf<MissionGoal>()
        var id = 1

        // 1-12: Lines (levels 1-4)
        for (i in 1..12) {
            val level = ((i - 1) / 3) + 1
            val lines = i * 5
            missions.add(MissionGoal(id = id++, kind = MissionKind.LINES, n = lines, level = level))
        }

        // 13-22: Score
        for (i in 13..22) {
            val level = ((i - 13) / 3) + 1
            val scoreGoal = (i - 12) * 5000
            missions.add(MissionGoal(id = id++, kind = MissionKind.SCORE, n = scoreGoal, level = level))
        }

        // 23-30: Tetrises
        for (i in 23..30) {
            val level = ((i - 23) / 2) + 2
            val count = i - 22
            missions.add(MissionGoal(id = id++, kind = MissionKind.TETRISES, n = count, level = level))
        }

        // 31-38: T-Spins
        for (i in 31..38) {
            val level = ((i - 31) / 2) + 3
            val count = i - 30
            missions.add(MissionGoal(id = id++, kind = MissionKind.TSPINS, n = count, level = level))
        }

        // 39-46: Combo
        for (i in 39..46) {
            val level = ((i - 39) / 2) + 4
            val combo = i - 37
            missions.add(MissionGoal(id = id++, kind = MissionKind.COMBO, n = combo, level = level))
        }

        // 47-49: Perfect Clear
        for (i in 47..49) {
            missions.add(MissionGoal(id = id++, kind = MissionKind.PERFECT_CLEAR, n = i - 46, level = 5))
        }

        // 50-51: Score (higher)
        missions.add(MissionGoal(id = id++, kind = MissionKind.SCORE, n = 50000, level = 6))
        missions.add(MissionGoal(id = id++, kind = MissionKind.SCORE, n = 75000, level = 7))

        // 52-63: Survive (with garbage)
        for (i in 52..63) {
            val level = ((i - 52) / 2) + 5
            val seconds = (i - 51) * 15
            val garbage = if (i % 3 == 0) "light" else "heavy"
            missions.add(MissionGoal(id = id++, kind = MissionKind.SURVIVE, n = seconds, level = level, garbage = garbage))
        }

        // 64-75: Level
        for (i in 64..75) {
            val levelGoal = ((i - 64) / 2) + 5
            val garbage = if (i % 2 == 0) "light" else null
            missions.add(MissionGoal(id = id++, kind = MissionKind.LEVEL, n = levelGoal, level = 1, garbage = garbage))
        }

        // 76-87: Score (with garbage)
        for (i in 76..87) {
            val level = ((i - 76) / 2) + 8
            val scoreGoal = (i - 75) * 10000
            val garbage = if (i % 2 == 0) "heavy" else "light"
            missions.add(MissionGoal(id = id++, kind = MissionKind.SCORE, n = scoreGoal, level = level, garbage = garbage))
        }

        // 88-94: Tetrises/T-Spins (with garbage)
        for (i in 88..94) {
            val level = 10 + (i - 88)
            val count = i - 87
            val kind = if (i % 2 == 0) MissionKind.TETRISES else MissionKind.TSPINS
            missions.add(MissionGoal(id = id++, kind = kind, n = count, level = level, garbage = "light"))
        }

        // 95-96: Combo/Survive (high level)
        missions.add(MissionGoal(id = id++, kind = MissionKind.COMBO, n = 10, level = 12))
        missions.add(MissionGoal(id = id++, kind = MissionKind.SURVIVE, n = 180, level = 12, garbage = "heavy"))

        // 97-100: Mixed high-level
        missions.add(MissionGoal(id = id++, kind = MissionKind.SCORE, n = 200000, level = 15))
        missions.add(MissionGoal(id = id++, kind = MissionKind.TETRISES, n = 15, level = 15, garbage = "heavy"))
        missions.add(MissionGoal(id = id++, kind = MissionKind.TSPINS, n = 10, level = 18, garbage = "heavy"))
        missions.add(MissionGoal(id = id++, kind = MissionKind.PERFECT_CLEAR, n = 5, level = 20))

        return missions
    }
}

/**
 * Adventure stage definitions — loads the 40 stages across 7 worlds from
 * tetris-content.json, with a programmatic fallback of 36 stages (6 worlds × 6).
 */
object AdventureDefinitions {

    /** Token types collectible in adventure mode. */
    enum class TokenType(val glyph: String) {
        STAR("⭐"),
        DIAMOND("🔶"),
        HEART("❤️"),
        BOLT("⚡"),
        GEM("💎"),
        CROWN("👑"),
        FLOWER("🌸"),
        SHIELD("🛡️")
    }

    /** Adventure stage definition. */
    data class Stage(
        val id: Int,
        val world: Int,
        val kind: MissionKind,
        val n: Int,
        val level: Int? = null,
        val garbage: String? = null,
        val tokens: List<TokenType>? = null,
        val title: String? = null,
        val desc: String? = null
    )

    /** World metadata (7 worlds from tetris-content.json). */
    data class World(
        val id: Int,
        val name: String,
        val accent: String,
        val stageCount: Int
    )

    @Volatile private var cachedJsonStages: List<Stage>? = null
    @Volatile private var cachedJsonWorlds: List<World>? = null

    /** Load all stages. If ctx is provided, attempts JSON first; falls back to generated. */
    fun generateAll(ctx: Context? = null): List<Stage> {
        if (ctx != null) {
            cachedJsonStages?.let { return it }
            try {
                val worlds = ContentLoader.getWorlds(ctx)
                val stages = mutableListOf<Stage>()
                var globalId = 1
                for (world in worlds) {
                    for (stage in world.stages) {
                        val tokens = if (stage.kind == "tokens") {
                            // Pick tokens based on stage id (deterministic)
                            listOf(TokenType.values()[(stage.id - 1) % TokenType.values().size])
                        } else null
                        stages.add(Stage(
                            id = globalId++,
                            world = world.id,
                            kind = MissionKind.valueOf(stage.kind.uppercase()),
                            n = stage.n,
                            level = stage.level,
                            tokens = tokens,
                            title = stage.title["ar"] ?: stage.title["en"],
                            desc = stage.desc["ar"] ?: stage.desc["en"]
                        ))
                    }
                }
                if (stages.isNotEmpty()) {
                    cachedJsonStages = stages
                    // Also cache worlds
                    cachedJsonWorlds = worlds.map { w ->
                        World(
                            id = w.id,
                            name = w.name["ar"] ?: w.name["en"] ?: "World ${w.id}",
                            accent = w.accent,
                            stageCount = w.stages.size
                        )
                    }
                    return stages
                }
            } catch (e: Exception) {
                android.util.Log.w("AdventureDefinitions", "JSON load failed, using generated: ${e.message}")
            }
        }
        return generateFallback()
    }

    /** Get world metadata. Loads from JSON if available, else generates defaults. */
    fun getWorlds(ctx: Context? = null): List<World> {
        if (ctx != null) {
            cachedJsonWorlds?.let { return it }
            // Trigger load via generateAll
            generateAll(ctx)
            cachedJsonWorlds?.let { return it }
        }
        // Fallback worlds
        return listOf(
            World(1, "الواحة", "#4ADE80", 7),
            World(2, "المدينة العتيقة", "#FBBF24", 6),
            World(3, "الجبل", "#22D3EE", 6),
            World(4, "الصحراء", "#F59E0B", 6),
            World(5, "القصر", "#F472B6", 6),
            World(6, "السماء", "#A78BFA", 6),
            World(7, "الأرض", "#C42D16", 3)
        )
    }

    /** Generate 36 stages programmatically (6 worlds × 6). */
    private fun generateFallback(): List<Stage> {
        val stages = mutableListOf<Stage>()
        var id = 1
        val worlds = 6

        for (world in 1..worlds) {
            stages.add(Stage(id = id++, world = world, kind = MissionKind.LINES, n = 10 + world * 2, level = world))
            val tokens = listOf(TokenType.values()[(world - 1) % TokenType.values().size])
            stages.add(Stage(id = id++, world = world, kind = MissionKind.LINES, n = 5, level = world, tokens = tokens))
            stages.add(Stage(id = id++, world = world, kind = MissionKind.SCORE, n = world * 10000, level = world + 1))
            val kind = if (world % 2 == 0) MissionKind.TETRISES else MissionKind.TSPINS
            stages.add(Stage(id = id++, world = world, kind = kind, n = 2 + world, level = world + 1))
            val kind5 = if (world <= 3) MissionKind.COMBO else MissionKind.PERFECT_CLEAR
            val n5 = if (kind5 == MissionKind.COMBO) 3 + world else world - 2
            stages.add(Stage(id = id++, world = world, kind = kind5, n = n5, level = world + 2))
            val kind6 = if (world % 2 == 0) MissionKind.SURVIVE else MissionKind.LEVEL
            val n6 = if (kind6 == MissionKind.SURVIVE) 30 + world * 10 else world + 3
            stages.add(Stage(id = id++, world = world, kind = kind6, n = n6, level = world + 2, garbage = "light"))
        }

        return stages
    }
}

