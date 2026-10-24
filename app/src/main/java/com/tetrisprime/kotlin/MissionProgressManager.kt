package com.tetrisprime.kotlin

import android.content.Context

/**
 * Phase 8.2 — Mission progress tracking.
 *
 * 100 missions across 8 types (LINES, SCORE, TETRISES, TSPINS, COMBO,
 * PERFECT_CLEAR, SURVIVE, LEVEL).
 *
 * Progressive unlock: missions unlock sequentially. Mission N is unlocked
 * only if mission N-1 has been completed.
 */
object MissionProgressManager {

    private const val PREFS = "tetris_prime_missions"
    private const val KEY_COMPLETED = "completed_set"
    private const val KEY_HIGHEST_UNLOCKED = "highest_unlocked"

    data class MissionState(
        val mission: MissionGoal,
        val index: Int,          // 0-based index in the 100-mission list
        val isUnlocked: Boolean,
        val isCompleted: Boolean
    )

    private fun prefs(ctx: Context) =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Get the set of completed mission IDs (as a set of index strings). */
    private fun loadCompleted(ctx: Context): Set<Int> {
        val raw = prefs(ctx).getStringSet(KEY_COMPLETED, emptySet()) ?: emptySet()
        return raw.mapNotNull { it.toIntOrNull() }.toSet()
    }

    /** Get the highest unlocked mission index (0-based). Default = 0 (first mission unlocked). */
    private fun loadHighestUnlocked(ctx: Context): Int =
        prefs(ctx).getInt(KEY_HIGHEST_UNLOCKED, 0)

    /** Get all 101 missions with their unlock/complete state. */
    fun getAllMissions(ctx: Context): List<MissionState> {
        val all = MissionDefinitions.generateAll(ctx)
        val completed = loadCompleted(ctx)
        val highest = loadHighestUnlocked(ctx)
        return all.mapIndexed { idx, m ->
            MissionState(
                mission = m,
                index = idx,
                isUnlocked = idx <= highest,
                isCompleted = completed.contains(idx)
            )
        }
    }

    /** Get the next mission to play (first unlocked-but-not-completed mission). */
    fun getNextMission(ctx: Context): MissionState? =
        getAllMissions(ctx).firstOrNull { it.isUnlocked && !it.isCompleted }

    /** Get a specific mission by index. */
    fun getMission(ctx: Context, index: Int): MissionState? =
        getAllMissions(ctx).getOrNull(index)

    /** Mark a mission as completed and unlock the next one. Returns the new state. */
    fun completeMission(ctx: Context, index: Int): MissionState? {
        val all = getAllMissions(ctx)
        if (index !in all.indices) return null
        val state = all[index]
        if (!state.isUnlocked) return null

        val completed = loadCompleted(ctx).toMutableSet()
        val wasAlreadyComplete = completed.contains(index)
        completed.add(index)

        val highestUnlocked = loadHighestUnlocked(ctx)
        // Unlock next mission only if this was the highest completed
        val newHighest = if (index == highestUnlocked && index + 1 < all.size) {
            index + 1
        } else {
            highestUnlocked
        }

        prefs(ctx).edit()
            .putStringSet(KEY_COMPLETED, completed.map { it.toString() }.toSet())
            .putInt(KEY_HIGHEST_UNLOCKED, newHighest)
            .apply()

        return getMission(ctx, index)
    }

    /** Total number of completed missions. */
    fun completedCount(ctx: Context): Int = loadCompleted(ctx).size

    /** Total missions = 100. */
    const val TOTAL = 100

    /** Friendly name for a mission kind. */
    fun kindLabel(kind: MissionKind): String = when (kind) {
        MissionKind.LINES         -> "خطوط"
        MissionKind.SCORE         -> "نقاط"
        MissionKind.TETRISES      -> "تيتريس"
        MissionKind.TSPINS        -> "T-Spin"
        MissionKind.COMBO         -> "كومبو"
        MissionKind.PERFECT_CLEAR -> "مسح كامل"
        MissionKind.SURVIVE       -> "صمود"
        MissionKind.LEVEL         -> "مستوى"
        MissionKind.TOKENS        -> "رموز"
    }

    /** Human-readable mission description. */
    fun describe(mission: MissionGoal): String {
        val kindStr = kindLabel(mission.kind)
        return when (mission.kind) {
            MissionKind.LINES         -> "امسح ${mission.n} خطاً"
            MissionKind.SCORE         -> "حقق ${mission.n} نقطة"
            MissionKind.TETRISES      -> "حقق ${mission.n} تيتريس"
            MissionKind.TSPINS        -> "حقق ${mission.n} T-Spin"
            MissionKind.COMBO         -> "كومبو ${mission.n}×"
            MissionKind.PERFECT_CLEAR -> "حقق ${mission.n} مسح كامل"
            MissionKind.SURVIVE       -> "اصمد ${mission.n} ثانية"
            MissionKind.LEVEL         -> "بلوغ المستوى ${mission.n}"
            MissionKind.TOKENS        -> "اجمع ${mission.n} رموز"
        }
    }
}
