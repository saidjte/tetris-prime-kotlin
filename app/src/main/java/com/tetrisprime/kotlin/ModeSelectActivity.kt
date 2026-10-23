package com.tetrisprime.kotlin

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * Mode selection screen — shows all game modes with lock/unlock status.
 */
class ModeSelectActivity : AppCompatActivity() {

    private lateinit var stats: PlayerStats

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
            text = "اختر نمط اللعب"
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

        // Stats display
        val statsText = TextView(this).apply {
            text = "ألعاب: ${stats.games} | خطوط: ${stats.totalLines}"
            textSize = 14f
            setTextColor(0xFF94A3B8.toInt())
            setPadding(0, 48, 0, 0)
            gravity = android.view.Gravity.CENTER
        }
        container.addView(statsText)

        scrollView.addView(container)
        setContentView(scrollView)
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
        setBackground(android.graphics.drawable.GradientDrawable()).apply {
            setColor(bgColor)
            cornerRadius = 24f
            setStroke(2, accentColor)
        }

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
        val intent = Intent(this, GameActivity::class.java)
        intent.putExtra("mode", mode.name)
        // Add mission/adventure/daily params as needed
        when (mode) {
            GameMode.MISSION -> {
                val missions = MissionDefinitions.generateAll()
                intent.putExtra("mission_kind", missions[0].kind.name)
                intent.putExtra("mission_n", missions[0].n)
                intent.putExtra("mission_level", missions[0].level ?: 1)
            }
            GameMode.DAILY -> {
                val today = java.time.LocalDate.now().toString()
                intent.putExtra("daily_date", today)
            }
            GameMode.BATTLE -> {
                intent.putExtra("battle_difficulty", BattleDifficulty.NORMAL.name)
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
}
