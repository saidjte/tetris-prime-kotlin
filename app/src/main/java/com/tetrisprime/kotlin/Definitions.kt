package com.tetrisprime.kotlin

import kotlin.random.Random

/**
 * Mission definitions — 100 missions with progressive difficulty.
 * Matches the original game's mission system.
 */
object MissionDefinitions {

    /** Generate all 100 missions. */
    fun generateAll(): List<MissionGoal> {
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
 * Adventure stage definitions — 36 stages across 6 worlds.
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
        val tokens: List<TokenType>? = null
    )

    /** Generate all 36 stages (6 worlds × 6 stages). */
    fun generateAll(): List<Stage> {
        val stages = mutableListOf<Stage>()
        var id = 1
        val worlds = 6

        for (world in 1..worlds) {
            // Stage 1: Lines
            stages.add(Stage(id = id++, world = world, kind = MissionKind.LINES, n = 10 + world * 2, level = world))
            // Stage 2: Tokens
            val tokens = listOf(TokenType.values()[(world - 1) % TokenType.values().size])
            stages.add(Stage(id = id++, world = world, kind = MissionKind.LINES, n = 5, level = world, tokens = tokens))
            // Stage 3: Score
            stages.add(Stage(id = id++, world = world, kind = MissionKind.SCORE, n = world * 10000, level = world + 1))
            // Stage 4: Tetrises or T-Spins
            val kind = if (world % 2 == 0) MissionKind.TETRISES else MissionKind.TSPINS
            stages.add(Stage(id = id++, world = world, kind = kind, n = 2 + world, level = world + 1))
            // Stage 5: Combo or Perfect Clear
            val kind5 = if (world <= 3) MissionKind.COMBO else MissionKind.PERFECT_CLEAR
            val n5 = if (kind5 == MissionKind.COMBO) 3 + world else world - 2
            stages.add(Stage(id = id++, world = world, kind = kind5, n = n5, level = world + 2))
            // Stage 6: Survive or Level (with garbage)
            val kind6 = if (world % 2 == 0) MissionKind.SURVIVE else MissionKind.LEVEL
            val n6 = if (kind6 == MissionKind.SURVIVE) 30 + world * 10 else world + 3
            stages.add(Stage(id = id++, world = world, kind = kind6, n = n6, level = world + 2, garbage = "light"))
        }

        return stages
    }
}
