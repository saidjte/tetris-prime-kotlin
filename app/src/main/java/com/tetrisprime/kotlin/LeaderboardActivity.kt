package com.tetrisprime.kotlin

import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * Leaderboard screen — top 10 entries per mode.
 * Mode picker at top, table below.
 */
class LeaderboardActivity : AppCompatActivity() {

    private var selectedMode: GameMode = GameMode.CLASSIC
    private lateinit var listContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 60, 32, 32)
            setBackgroundColor(0xFF0B081E.toInt())
        }

        // Title
        val title = TextView(this).apply {
            text = "🥇 لوحة الصدارة"
            textSize = 22f
            setTextColor(0xFFF3F1FF.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 16)
        }
        container.addView(title)

        // Mode picker (horizontal scroll)
        val modeScroll = android.widget.HorizontalScrollView(this)
        val modeRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, 16)
        }
        for (mode in ModeConfig.ALL_MODES) {
            val btn = TextView(this).apply {
                text = modeName(mode)
                textSize = 13f
                setTextColor(if (mode == selectedMode) 0xFFFFFFFF.toInt() else 0xFFF3F1FF.toInt())
                setPadding(20, 10, 20, 10)
                val d = android.graphics.drawable.GradientDrawable()
                d.setColor(if (mode == selectedMode) 0xFF8B5CF6.toInt() else 0xFF1E1B4B.toInt())
                d.cornerRadius = 14f
                background = d
                setOnClickListener {
                    selectedMode = mode
                    refresh()
                }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { marginEnd = 8 }
            }
            modeRow.addView(btn)
        }
        modeScroll.addView(modeRow)
        container.addView(modeScroll)

        // List
        val scroll = ScrollView(this)
        listContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        scroll.addView(listContainer)
        container.addView(scroll)

        setContentView(container)
        refresh()
    }

    private fun refresh() {
        listContainer.removeAllViews()
        val entries = LeaderboardManager.getTop(this, selectedMode)
        val timeBased = LeaderboardManager.isTimeBased(selectedMode)

        // Header row
        listContainer.addView(headerRow())

        if (entries.isEmpty()) {
            val empty = TextView(this).apply {
                text = "لا توجد نتائج بعد — العب لِتُسجّل رقماً!"
                textSize = 14f
                setTextColor(0xFF64748B.toInt())
                gravity = Gravity.CENTER
                setPadding(0, 48, 0, 0)
            }
            listContainer.addView(empty)
            return
        }

        for ((i, e) in entries.withIndex()) {
            val rankText = when (i) { 0 -> "🥇"; 1 -> "🥈"; 2 -> "🥉"; else -> "${i + 1}" }
            val scoreText = if (timeBased) formatMs(e.score) else "${e.score}"
            listContainer.addView(dataRow(rankText, scoreText, "${e.lines} خط", "مستوى ${e.level}"))
        }
    }

    private fun headerRow(): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(12, 12, 12, 12)
            val d = android.graphics.drawable.GradientDrawable()
            d.setColor(0xFF312E81.toInt())
            d.cornerRadius = 12f
            background = d
        }
        row.addView(cell("#", 0.5f, 0xFFA78BFA.toInt(), 13f, true))
        row.addView(cell("النتيجة", 2f, 0xFFA78BFA.toInt(), 13f, true))
        row.addView(cell("خطوط", 1f, 0xFFA78BFA.toInt(), 13f, true))
        row.addView(cell("مستوى", 1f, 0xFFA78BFA.toInt(), 13f, true))
        return row
    }

    private fun dataRow(rank: String, score: String, lines: String, level: String): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(12, 14, 12, 14)
            val d = android.graphics.drawable.GradientDrawable()
            d.setColor(0xFF1E1B4B.toInt())
            d.cornerRadius = 10f
            background = d
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 6 }
        }
        row.addView(cell(rank, 0.5f, 0xFFFBBF24.toInt(), 14f, false))
        row.addView(cell(score, 2f, 0xFFF3F1FF.toInt(), 14f, false))
        row.addView(cell(lines, 1f, 0xFF22D3EE.toInt(), 14f, false))
        row.addView(cell(level, 1f, 0xFF94A3B8.toInt(), 14f, false))
        return row
    }

    private fun cell(text: String, weight: Float, color: Int, size: Float, bold: Boolean): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = size
            setTextColor(color)
            gravity = Gravity.CENTER
            if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, weight)
        }
    }

    private fun formatMs(ms: Long): String {
        val s = ms / 1000.0
        return String.format("%.2f ث", s)
    }

    private fun modeName(mode: GameMode): String = when (mode) {
        GameMode.CLASSIC -> "كلاسيكي"
        GameMode.SPRINT -> "سبرينت"
        GameMode.ULTRA -> "ألترا"
        GameMode.ZEN -> "زن"
        GameMode.CHEESE -> "جبن"
        GameMode.MISSION -> "مهمات"
        GameMode.ADVENTURE -> "مغامرة"
        GameMode.BATTLE -> "معركة"
        GameMode.DAILY -> "يومي"
    }
}
