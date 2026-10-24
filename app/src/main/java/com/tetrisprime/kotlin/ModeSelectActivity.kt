package com.tetrisprime.kotlin

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * Mode selection screen — shows all game modes with lock/unlock status,
 * plus a navigation rail to other Phase 6 UI screens.
 */
class ModeSelectActivity : AppCompatActivity() {

    private lateinit var stats: PlayerStats

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Phase 9: initialize translation system and apply RTL/LTR
        TranslationManager.init(this)
        applyLayoutDirection()

        // Load stats (in real app, from SharedPreferences/Room)
        stats = loadStats()

        // Build the UI programmatically
        val scrollView = android.widget.ScrollView(this)
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 64, 32, 32)
            setBackgroundColor(0xFF0B081E.toInt())
        }

        // Title
        val title = TextView(this).apply {
            text = TranslationManager.t(TranslationManager.Keys.MODE_SELECT_TITLE)
            textSize = 24f
            setTextColor(0xFFF3F1FF.toInt())
            gravity = android.view.Gravity.CENTER
            setPadding(0, 0, 0, 32)
        }
        container.addView(title)

        // Mode buttons grid
        val grid = GridLayout(this).apply {
            columnCount = 2
            rowCount = 5
            useDefaultMargins = true
        }

        for (mode in ModeConfig.ALL_MODES) {
            val modeView = createModeButton(mode)
            grid.addView(modeView)
        }

        container.addView(grid)

        // Navigation rail (Phase 6 screens)
        container.addView(buildNavRail())

        // Stats display — Phase 4: richer header with games/lines/coins/best
        val coins = StoreManager.getCoins(this)
        val bestClassic = LeaderboardManager.getTop(this, GameMode.CLASSIC).firstOrNull()?.score ?: 0L
        val statsGrid = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 32, 0, 16)
            gravity = Gravity.CENTER_VERTICAL
        }
        val statsItems = listOf(
            "🎮 ${stats.games}" to 0xFFA78BFA.toInt(),
            "📏 ${stats.totalLines}" to 0xFF34D399.toInt(),
            "🏆 ${if (bestClassic > 0) bestClassic else "—"}" to 0xFFFBBF24.toInt(),
            "🪙 $coins" to 0xFFF97316.toInt()
        )
        for ((label, color) in statsItems) {
            val tv = TextView(this).apply {
                text = label
                textSize = 13f
                setTextColor(color)
                setPadding(12, 8, 12, 8)
                gravity = Gravity.CENTER
                val d = android.graphics.drawable.GradientDrawable()
                d.setColor(0xFF1E1B4B.toInt())
                d.cornerRadius = 12f
                d.setStroke(1, color and 0x44FFFFFF.toInt())
                background = d
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            statsGrid.addView(tv)
        }
        container.addView(statsGrid)

        // Phase 8.1 + Phase 4: Daily challenge streak banner (richer)
        val daily = DailyChallengeManager.load(this)
        val dailyBanner = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(20, 16, 20, 16)
            gravity = Gravity.CENTER_VERTICAL
            val d = android.graphics.drawable.GradientDrawable()
            val baseColor = if (daily.alreadyWonToday) 0xFF065F46.toInt() else 0xFF7C2D12.toInt()
            d.setColor(baseColor)
            d.cornerRadius = 16f
            d.setStroke(1, if (daily.alreadyWonToday) 0xFF34D399.toInt() else 0xFFFBBF24.toInt())
            background = d
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.topMargin = 8
            layoutParams = lp
        }
        val dailyEmoji = TextView(this).apply {
            text = if (daily.alreadyWonToday) "✅" else "🔥"
            textSize = 24f
            setPadding(0, 0, 12, 0)
        }
        val dailyInfo = TextView(this).apply {
            text = if (daily.alreadyWonToday) {
                "أكملت تحدي اليوم! • ${DailyChallengeManager.streakLabel(daily.streak)} • +${daily.reward} 🪙"
            } else {
                "تحدي اليوم: ${daily.goal} نقطة • ${DailyChallengeManager.streakLabel(daily.streak)}"
            }
            textSize = 13f
            setTextColor(0xFFF3F1FF.toInt())
            maxLines = 2
        }
        dailyBanner.addView(dailyEmoji)
        dailyBanner.addView(dailyInfo)
        container.addView(dailyBanner)

        scrollView.addView(container)
        setContentView(scrollView)

        // Phase 0: show banner ad on main screen
        AdMobManager.showBanner(this)
    }

    private var lastBackPress = 0L

    /**
     * Phase 0: back-press confirmation dialog (mirrors original exit.confirm).
     * First press shows a Toast, second press within 2s exits via ExitBridge.
     */
    override fun onBackPressed() {
        val now = System.currentTimeMillis()
        if (now - lastBackPress < 2000) {
            AdMobManager.hideBanner()
            ExitBridge.exitApp(this)
        } else {
            lastBackPress = now
            android.widget.Toast.makeText(this, "اضغط مرة أخرى للخروج", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        AdMobManager.hideBanner()
    }

    /**
     * Build a grid of buttons that navigate to other Phase 6/8 UI screens.
     */
    private fun buildNavRail(): View {
        val grid = GridLayout(this).apply {
            columnCount = 3
            rowCount = 3
            useDefaultMargins = true
            setPadding(0, 32, 0, 0)
        }
        val items = listOf(
            NavItem("⚙️ الإعدادات") { startActivity(Intent(this, SettingsActivity::class.java)) },
            NavItem("📊 الإحصائيات") { startActivity(Intent(this, StatsActivity::class.java)) },
            NavItem("🏆 الإنجازات") { startActivity(Intent(this, AchievementsActivity::class.java)) },
            NavItem("🛒 المتجر") { startActivity(Intent(this, StoreActivity::class.java)) },
            NavItem("📚 الشرح") { startActivity(Intent(this, TutorialActivity::class.java)) },
            NavItem("🥇 الصدارة") { startActivity(Intent(this, LeaderboardActivity::class.java)) },
            NavItem("🎯 المهمات") { startActivity(Intent(this, MissionsActivity::class.java)) },
            NavItem("🗺️ المغامرة") { startActivity(Intent(this, AdventureActivity::class.java)) },
            NavItem("🎁 هدية") { startActivity(Intent(this, GiftCodeActivity::class.java)) }
        )
        for (item in items) {
            grid.addView(navButton(item))
        }
        return grid
    }

    private data class NavItem(val label: String, val action: () -> Unit)

    private fun navButton(item: NavItem): View {
        val tv = TextView(this).apply {
            text = item.label
            textSize = 13f
            setTextColor(0xFFF3F1FF.toInt())
            gravity = Gravity.CENTER
            setPadding(16, 24, 16, 24)
            val d = android.graphics.drawable.GradientDrawable()
            d.setColor(0xFF312E81.toInt())
            d.cornerRadius = 16f
            background = d
            setOnClickListener { item.action() }
        }
        val lp = GridLayout.LayoutParams().apply {
            width = 0
            height = GridLayout.LayoutParams.WRAP_CONTENT
            columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1, 1f)
        }
        tv.layoutParams = lp
        return tv
    }

    private fun createModeButton(mode: GameMode): View {
        val unlocked = ModeConfig.isUnlocked(mode, stats)
        val modeName = getModeName(mode)
        val modeDesc = getModeDescription(mode)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
            minimumHeight = 200
        }

        val bgColor = if (unlocked) 0xFF1E1B4B.toInt() else 0xFF1E293B.toInt()
        val accentColor = getModeAccent(mode)
        val drawable = android.graphics.drawable.GradientDrawable()
        drawable.setColor(bgColor)
        drawable.cornerRadius = 24f
        drawable.setStroke(2, accentColor)
        layout.background = drawable

        // Mode icon + name
        val nameText = TextView(this).apply {
            text = if (unlocked) "${getModeIcon(mode)} $modeName" else "🔒 $modeName"
            textSize = 16f
            setTextColor(if (unlocked) 0xFFF3F1FF.toInt() else 0xFF64748B.toInt())
            setPadding(0, 0, 0, 8)
        }
        layout.addView(nameText)

        // Description
        val descText = TextView(this).apply {
            text = modeDesc
            textSize = 12f
            setTextColor(if (unlocked) 0xFF94A3B8.toInt() else 0xFF475569.toInt())
            maxLines = 2
        }
        layout.addView(descText)

        // Unlock requirement (if locked)
        if (!unlocked) {
            val unlockDesc = ModeConfig.getUnlockDescription(mode)
            if (unlockDesc != null) {
                val lockText = TextView(this).apply {
                    text = "🔓 $unlockDesc"
                    textSize = 11f
                    setTextColor(0xFFFBBF24.toInt())
                    setPadding(0, 8, 0, 0)
                }
                layout.addView(lockText)
            }
        }

        val params = GridLayout.LayoutParams().apply {
            width = 0
            height = GridLayout.LayoutParams.WRAP_CONTENT
            columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1, 1f)
        }
        layout.layoutParams = params

        if (unlocked) {
            layout.setOnClickListener {
                startGame(mode)
            }
        }

        return layout
    }

    private fun startGame(mode: GameMode) {
        // Phase 4: Battle mode shows difficulty picker before launching
        if (mode == GameMode.BATTLE) {
            showBattleDifficultyDialog()
            return
        }
        launchGame(mode, battleDifficulty = BattleDifficulty.NORMAL)
    }

    /** Phase 4 — Battle difficulty selection dialog (mirrors original battle.difficulty). */
    private fun showBattleDifficultyDialog() {
        val options = BattleDifficulty.values()
        val labels = options.map { diff ->
            when (diff) {
                BattleDifficulty.EASY -> "🟢 سهل — الخصم بطيء وضعيف"
                BattleDifficulty.NORMAL -> "🟡 عادي — توازن بين السرعة والهجوم"
                BattleDifficulty.HARD -> "🔴 صعب — خصم سريع يرسل tetris bonuses"
            }
        }.toTypedArray()
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("⚔️ صعوبة المواجهة")
            .setItems(labels) { _, which ->
                launchGame(GameMode.BATTLE, battleDifficulty = options[which])
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    private fun launchGame(mode: GameMode, battleDifficulty: BattleDifficulty = BattleDifficulty.NORMAL) {
        val intent = Intent(this, GameActivity::class.java)
        intent.putExtra("mode", mode.name)
        // Add mission/adventure/daily params as needed
        when (mode) {
            GameMode.MISSION -> {
                // Phase 8.2: pick the next uncompleted mission automatically
                val next = MissionProgressManager.getNextMission(this)
                if (next != null) {
                    intent.putExtra("mission_index", next.index)
                    intent.putExtra("mission_kind", next.mission.kind.name)
                    intent.putExtra("mission_n", next.mission.n)
                    intent.putExtra("mission_level", next.mission.level ?: 1)
                } else {
                    // All 100 missions complete — fall back to first
                    val missions = MissionDefinitions.generateAll()
                    intent.putExtra("mission_index", 0)
                    intent.putExtra("mission_kind", missions[0].kind.name)
                    intent.putExtra("mission_n", missions[0].n)
                    intent.putExtra("mission_level", missions[0].level ?: 1)
                }
            }
            GameMode.ADVENTURE -> {
                // Phase 8.3: pick the next uncompleted stage automatically
                val stages = AdventureProgressManager.getAllStages(this)
                val next = stages.firstOrNull { it.isUnlocked && !it.isCompleted }
                if (next != null) {
                    intent.putExtra("adventure_index", next.index)
                    intent.putExtra("mission_index", next.index)
                    intent.putExtra("mission_kind", next.stage.kind.name)
                    intent.putExtra("mission_n", next.stage.n)
                    intent.putExtra("mission_level", next.stage.level ?: 1)
                } else {
                    // All 36 stages complete — fall back to first
                    val all = AdventureDefinitions.generateAll()
                    intent.putExtra("adventure_index", 0)
                    intent.putExtra("mission_index", 0)
                    intent.putExtra("mission_kind", all[0].kind.name)
                    intent.putExtra("mission_n", all[0].n)
                    intent.putExtra("mission_level", all[0].level ?: 1)
                }
            }
            GameMode.DAILY -> {
                val today = java.time.LocalDate.now().toString()
                intent.putExtra("daily_date", today)
            }
            GameMode.BATTLE -> {
                intent.putExtra("battle_difficulty", battleDifficulty.name)
            }
            else -> {}
        }
        startActivity(intent)
    }

    private fun getModeName(mode: GameMode): String = when (mode) {
        GameMode.CLASSIC -> "كلاسيكي"
        GameMode.SPRINT -> "سبرينت ٤٠"
        GameMode.ULTRA -> "ألترا"
        GameMode.ZEN -> "زن"
        GameMode.CHEESE -> "جبن"
        GameMode.MISSION -> "مهمات"
        GameMode.ADVENTURE -> "مغامرة"
        GameMode.BATTLE -> "معركة"
        GameMode.DAILY -> "يومي"
    }

    private fun getModeDescription(mode: GameMode): String = when (mode) {
        GameMode.CLASSIC -> "ماراثون بلا نهاية بتسارع تدريجي"
        GameMode.SPRINT -> "امسح ٤٠ خطًا بأسرع وقت"
        GameMode.ULTRA -> "دقيقتان لتحقيق أعلى نتيجة"
        GameMode.ZEN -> "استرخاء بلا تسارع"
        GameMode.CHEESE -> "امسح خلايا الجبن"
        GameMode.MISSION -> "١٠٠ مهمة متدرجة"
        GameMode.ADVENTURE -> "٣٦ مرحلة عبر ٦ عوالم"
        GameMode.BATTLE -> "مواجهة الذكاء الاصطناعي"
        GameMode.DAILY -> "تحدي يومي محدد"
    }

    private fun getModeIcon(mode: GameMode): String = when (mode) {
        GameMode.CLASSIC -> "🎮"
        GameMode.SPRINT -> "🏃"
        GameMode.ULTRA -> "⚡"
        GameMode.ZEN -> "🧘"
        GameMode.CHEESE -> "🧀"
        GameMode.MISSION -> "🎯"
        GameMode.ADVENTURE -> "🗺️"
        GameMode.BATTLE -> "🤖"
        GameMode.DAILY -> "📅"
    }

    private fun getModeAccent(mode: GameMode): Int = when (mode) {
        GameMode.CLASSIC -> 0xFF8B5CF6.toInt()
        GameMode.SPRINT -> 0xFF22D3EE.toInt()
        GameMode.ULTRA -> 0xFFFBBF24.toInt()
        GameMode.ZEN -> 0xFF34D399.toInt()
        GameMode.CHEESE -> 0xFFFDE68A.toInt()
        GameMode.MISSION -> 0xFFA78BFA.toInt()
        GameMode.ADVENTURE -> 0xFFFB7185.toInt()
        GameMode.BATTLE -> 0xFFF472B6.toInt()
        GameMode.DAILY -> 0xFFF97316.toInt()
    }

    private fun loadStats(): PlayerStats {
        val prefs = getSharedPreferences("tetris_prime", MODE_PRIVATE)
        return PlayerStats(
            games = prefs.getInt("games", 0),
            totalLines = prefs.getInt("totalLines", 0),
            totalPieces = prefs.getInt("totalPieces", 0),
            tetrises = prefs.getInt("tetrises", 0),
            tspins = prefs.getInt("tspins", 0),
            perfectClears = prefs.getInt("perfectClears", 0),
            maxCombo = prefs.getInt("maxCombo", 0),
            maxB2b = prefs.getInt("maxB2b", 0)
        )
    }

    /**
     * Phase 9 — apply RTL/LTR layout direction based on current language.
     * ar/fa/ur → RTL, everything else → LTR.
     */
    private fun applyLayoutDirection() {
        val isRtl = TranslationManager.isRtl(TranslationManager.currentLang)
        window.decorView.layoutDirection = if (isRtl)
            android.view.View.LAYOUT_DIRECTION_RTL
        else
            android.view.View.LAYOUT_DIRECTION_LTR
    }
}
