package com.tetrisprime.kotlin

import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * Achievements screen — 18 achievements with progress bars.
 */
class AchievementsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val metrics = StatsManager.load(this)
        val (unlocked, _) = AchievementDefinitions.updateUnlocked(this, metrics)

        val scroll = ScrollView(this)
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 60, 40, 80)
            setBackgroundColor(0xFF0B081E.toInt())
        }

        container.addView(header("🏆 الإنجازات"))

        // Summary
        val summary = TextView(this).apply {
            text = "مفتوح: ${unlocked.size} / ${AchievementDefinitions.ALL.size}"
            textSize = 16f
            setTextColor(0xFF22D3EE.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 32)
        }
        container.addView(summary)

        for (ach in AchievementDefinitions.ALL) {
            val cur = ach.currentValue(metrics)
            val isUnlocked = unlocked.contains(ach.id)
            container.addView(achievementCard(ach, cur, isUnlocked))
        }

        scroll.addView(container)
        setContentView(scroll)
    }

    private fun header(text: String): TextView = TextView(this).apply {
        this.text = text
        textSize = 24f
        setTextColor(0xFFF3F1FF.toInt())
        gravity = Gravity.CENTER
        setPadding(0, 0, 0, 16)
    }

    private fun achievementCard(ach: Achievement, current: Long, unlocked: Boolean): LinearLayout {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 20, 20, 20)
            val d = android.graphics.drawable.GradientDrawable()
            d.setColor(if (unlocked) 0xFF312E81.toInt() else 0xFF1E293B.toInt())
            d.cornerRadius = 16f
            if (unlocked) d.setStroke(2, 0xFFFBBF24.toInt())
            background = d
        }
        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = 16 }
        layout.layoutParams = lp

        // Title row
        val titleRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val icon = TextView(this).apply {
            text = if (unlocked) ach.icon else "🔒"
            textSize = 28f
            setPadding(0, 0, 16, 0)
        }
        val title = TextView(this).apply {
            text = ach.title
            textSize = 16f
            setTextColor(if (unlocked) 0xFFFBBF24.toInt() else 0xFFCBD5E1.toInt())
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        titleRow.addView(icon)
        titleRow.addView(title)
        layout.addView(titleRow)

        // Description
        val desc = TextView(this).apply {
            text = ach.description
            textSize = 13f
            setTextColor(0xFF94A3B8.toInt())
            setPadding(0, 8, 0, 8)
        }
        layout.addView(desc)

        // Progress
        val progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            progress = if (ach.goal > 0) ((current.toFloat() / ach.goal.toFloat()) * 100).toInt().coerceIn(0, 100) else 100
        }
        layout.addView(progress)

        // Progress label
        val label = TextView(this).apply {
            text = "$current / ${ach.goal}"
            textSize = 12f
            setTextColor(if (unlocked) 0xFF34D399.toInt() else 0xFF64748B.toInt())
            setPadding(0, 4, 0, 0)
        }
        layout.addView(label)

        return layout
    }
}
