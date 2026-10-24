package com.tetrisprime.kotlin

import android.content.Context
import org.json.JSONObject
import org.json.JSONArray

/**
 * Phase 0+1 — Loads tetris-content.json from assets.
 *
 * This is the canonical content source — mirrors the original WebView app's
 * /assets/www/tetris-content.json (303KB) exactly. All 101 missions, 7
 * adventure worlds (40 stages), 17 achievements, and daily config are
 * defined there with 16-language translations.
 *
 * The original app fetches this same file at runtime. Kotlin reads it once
 * at first access and caches the parsed objects.
 */
object ContentLoader {

    /** A mission: kind + numeric goal. Title/desc are i18n-keyed by kind. */
    data class Mission(
        val id: Int,
        val kind: String,           // lines|score|tetrises|tspins|combo|pc|survive|level
        val n: Int,                 // goal value
        val level: Int? = null,     // some stages pin the level
        val title: Map<String, String>,
        val desc: Map<String, String>
    )

    /** A stage in an adventure world. */
    data class Stage(
        val id: Int,
        val kind: String,           // lines|score|tetrises|tspins|combo|pc|survive|level|tokens
        val n: Int,
        val level: Int? = null,
        val title: Map<String, String>,
        val desc: Map<String, String>
    )

    /** A world in adventure mode. */
    data class World(
        val id: Int,
        val name: Map<String, String>,
        val accent: String,
        val stages: List<Stage>
    )

    /** Daily challenge configuration. */
    data class DailyConfig(
        val reward: Int,
        val goalMin: Int,
        val goalMax: Int,
        val level: Int
    )

    /** Achievement definition with metric + goal + binary flag. */
    data class AchievementDef(
        val id: String,
        val icon: String,
        val titleKey: String,       // e.g. "ach.firstGame"
        val descKey: String,        // e.g. "ach.firstGameDesc"
        val goal: Int,
        val valuePath: String,      // e.g. "stats.games" or "records.sprint<60000"
        val binary: Boolean
    )

    @Volatile private var loaded = false
    private var missionsCache: List<Mission> = emptyList()
    private var worldsCache: List<World> = emptyList()
    private var dailyCache = DailyConfig(150, 9000, 15000, 4)
    private var achievementsCache: List<AchievementDef> = emptyList()

    /** Lazy load + cache. Thread-safe via @Volatile + synchronized. */
    fun ensureLoaded(context: Context) {
        if (loaded) return
        synchronized(this) {
            if (loaded) return
            try {
                val json = context.assets.open("www/tetris-content.json").bufferedReader().use { it.readText() }
                val root = JSONObject(json)

                // Missions
                val missionsArr = root.optJSONArray("missions") ?: JSONArray()
                val missions = mutableListOf<Mission>()
                for (i in 0 until missionsArr.length()) {
                    val m = missionsArr.getJSONObject(i)
                    missions.add(Mission(
                        id = i + 1,
                        kind = m.getString("kind"),
                        n = m.getInt("n"),
                        level = m.optInt("level", -1).let { if (it == -1) null else it },
                        title = readI18n(m.optJSONObject("title")),
                        desc = readI18n(m.optJSONObject("desc"))
                    ))
                }
                missionsCache = missions

                // Adventure worlds
                val advObj = root.optJSONObject("adventure")
                val worldsArr = advObj?.optJSONArray("worlds") ?: JSONArray()
                val worlds = mutableListOf<World>()
                for (wi in 0 until worldsArr.length()) {
                    val w = worldsArr.getJSONObject(wi)
                    val stagesArr = w.optJSONArray("stages") ?: JSONArray()
                    val stages = mutableListOf<Stage>()
                    for (si in 0 until stagesArr.length()) {
                        val s = stagesArr.getJSONObject(si)
                        stages.add(Stage(
                            id = si + 1,
                            kind = s.getString("kind"),
                            n = s.getInt("n"),
                            level = s.optInt("level", -1).let { if (it == -1) null else it },
                            title = readI18n(s.optJSONObject("title")),
                            desc = readI18n(s.optJSONObject("desc"))
                        ))
                    }
                    worlds.add(World(
                        id = wi + 1,
                        name = readI18n(w.optJSONObject("name")),
                        accent = w.optString("accent", "#8B5CF6"),
                        stages = stages
                    ))
                }
                worldsCache = worlds

                // Daily
                val dailyObj = root.optJSONObject("daily")
                if (dailyObj != null) {
                    dailyCache = DailyConfig(
                        reward = dailyObj.optInt("reward", 150),
                        goalMin = dailyObj.optInt("goalMin", 9000),
                        goalMax = dailyObj.optInt("goalMax", 15000),
                        level = dailyObj.optInt("level", 4)
                    )
                }

                // Achievements (from JSON — only 4 there; rest are JS-defined)
                val achArr = root.optJSONArray("achievements") ?: JSONArray()
                val achs = mutableListOf<AchievementDef>()
                for (i in 0 until achArr.length()) {
                    val a = achArr.getJSONObject(i)
                    achs.add(AchievementDef(
                        id = a.getString("id"),
                        icon = a.optString("icon", "🏆"),
                        titleKey = "ach.${a.getString("id").removePrefix("ach_")}",
                        descKey = "ach.${a.getString("id").removePrefix("ach_")}Desc",
                        goal = a.getInt("goal"),
                        valuePath = a.getString("metric"),
                        binary = false
                    ))
                }
                // The JS bundle defines 17 achievements (these are JS-side, mirror them here)
                achievementsCache = achs + JS_ACHIEVEMENTS
                loaded = true
            } catch (e: Exception) {
                // Fallback: empty content; app still runs but with no missions/achievements
                android.util.Log.e("ContentLoader", "Failed to load content: ${e.message}")
                loaded = true
            }
        }
    }

    private fun readI18n(obj: JSONObject?): Map<String, String> {
        if (obj == null) return emptyMap()
        val out = mutableMapOf<String, String>()
        for (key in obj.keys()) {
            out[key] = obj.getString(key)
        }
        return out
    }

    fun getMissions(context: Context): List<Mission> {
        ensureLoaded(context); return missionsCache
    }
    fun getWorlds(context: Context): List<World> {
        ensureLoaded(context); return worldsCache
    }
    fun getDailyConfig(context: Context): DailyConfig {
        ensureLoaded(context); return dailyCache
    }
    fun getAchievements(context: Context): List<AchievementDef> {
        ensureLoaded(context); return achievementsCache
    }

    /** The 17 JS-defined achievements (extracted from the minified bundle). */
    private val JS_ACHIEVEMENTS = listOf(
        AchievementDef("firstGame", "🎮", "ach.firstGame", "ach.firstGameDesc", 1, "stats.games", true),
        AchievementDef("games50", "🕹️", "ach.games50", "ach.games50Desc", 50, "stats.games", false),
        AchievementDef("lines100", "📏", "ach.lines100", "ach.lines100Desc", 100, "stats.totalLines", false),
        AchievementDef("lines1000", "🧱", "ach.lines1000", "ach.lines1000Desc", 1000, "stats.totalLines", false),
        AchievementDef("lines5000", "🏔️", "ach.lines5000", "ach.lines5000Desc", 5000, "stats.totalLines", false),
        AchievementDef("tetris1", "💥", "ach.tetris1", "ach.tetris1Desc", 1, "stats.tetrises", true),
        AchievementDef("tetris25", "⚡", "ach.tetris25", "ach.tetris25Desc", 25, "stats.tetrises", false),
        AchievementDef("tspin1", "🌀", "ach.tspin1", "ach.tspin1Desc", 1, "stats.tspins", true),
        AchievementDef("tspin15", "🎯", "ach.tspin15", "ach.tspin15Desc", 15, "stats.tspins", false),
        AchievementDef("combo5", "🔗", "ach.combo5", "ach.combo5Desc", 5, "stats.maxCombo", false),
        AchievementDef("pc1", "✨", "ach.pc1", "ach.pc1Desc", 1, "stats.perfectClears", true),
        AchievementDef("sprintSub60", "🏃", "ach.sprintSub60", "ach.sprintSub60Desc", 1, "records.sprint<60000", true),
        AchievementDef("classic100k", "💯", "ach.classic100k", "ach.classic100kDesc", 1, "records.classic>=100000", true),
        AchievementDef("battle1", "🤖", "ach.battle1", "ach.battle1Desc", 1, "stats.battleWins", true),
        AchievementDef("daily1", "📅", "ach.daily1", "ach.daily1Desc", 1, "stats.dailyWins", true),
        AchievementDef("streak3", "🔥", "ach.streak3", "ach.streak3Desc", 3, "stats.bestDailyStreak", false),
        AchievementDef("mission10", "🗺️", "ach.mission10", "ach.mission10Desc", 1, "missionUnlocked>10", true),
        AchievementDef("themes5", "🛍️", "ach.themes5", "ach.themes5Desc", 5, "ownedThemes", false)
    )
}
