package com.tetrisprime.kotlin

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * Phase 8.2 — Mission selection screen.
 *
 * Shows all 100 missions in a scrollable grid with three states:
 * - Completed (green checkmark)
 * - Unlocked & available (purple, tappable → launches GameActivity with that mission)
 * - Locked (greyed out)
 */
class MissionsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val all = MissionProgressManager.getAllMissions(this)
        val completedCount = all.count { it.isCompleted }

        val scroll = ScrollView(this)
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 60, 32, 80)
            setBackgroundColor(0xFF0B081E.toInt())
        }

        // Header
        container.addView(TextView(this).apply {
            text = "🎯 المهمات"
            textSize = 24f
            setTextColor(0xFFF3F1FF.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 8)
        })

        // Progress
        container.addView(TextView(this).apply {
            text = "مكتمل: $completedCount / ${MissionProgressManager.TOTAL}"
            textSize = 14f
            setTextColor(0xFF22D3EE.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 24)
        })

        // Grid of missions — 5 columns
        val grid = android.widget.GridLayout(this).apply {
            columnCount = 5
            useDefaultMargins = true
        }

        for (state in all) {
            grid.addView(missionCell(state))
        }
        container.addView(grid)

        scroll.addView(container)
        setContentView(scroll)
    }

    private fun missionCell(state: MissionProgressManager.MissionState): View {
        val cell = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(12, 16, 12, 16)
            gravity = Gravity.CENTER
            minimumHeight = 140
            val d = android.graphics.drawable.GradientDrawable()
            when {
                state.isCompleted -> {
                    d.setColor(0xFF065F46.toInt())
                    d.setStroke(2, 0xFF34D399.toInt())
                }
                state.isUnlocked -> {
                    d.setColor(0xFF312E81.toInt())
                    d.setStroke(2, 0xFF8B5CF6.toInt())
                }
                else -> {
                    d.setColor(0xFF1E293B.toInt())
                }
            }
            d.cornerRadius = 12f
            background = d
        }
        val lp = android.widget.GridLayout.LayoutParams().apply {
            width = 0
            height = android.widget.GridLayout.LayoutParams.WRAP_CONTENT
            columnSpec = android.widget.GridLayout.spec(android.widget.GridLayout.UNDEFINED, 1, 1f)
        }
        cell.layoutParams = lp

        // Mission number
        val num = TextView(this).apply {
            text = "${state.index + 1}"
            textSize = 16f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(when {
                state.isCompleted -> 0xFF34D399.toInt()
                state.isUnlocked -> 0xFFF3F1FF.toInt()
                else -> 0xFF64748B.toInt()
            })
        }
        cell.addView(num)

        // Icon
        val icon = TextView(this).apply {
            text = when {
                state.isCompleted -> "✓"
                state.isUnlocked -> MissionProgressManager.kindLabel(state.mission.kind).take(2)
                else -> "🔒"
            }
            textSize = 18f
            setPadding(0, 8, 0, 0)
            setTextColor(when {
                state.isCompleted -> 0xFF34D399.toInt()
                state.isUnlocked -> 0xFFA78BFA.toInt()
                else -> 0xFF475569.toInt()
            })
        }
        cell.addView(icon)

        // Title (from tetris-content.json if available, else kindLabel)
        val titleText = state.mission.title
            ?: "${MissionProgressManager.kindLabel(state.mission.kind)} ${state.mission.n}"
        val titleView = TextView(this).apply {
            text = titleText
            textSize = 10f
            setTextColor(when {
                state.isCompleted -> 0xFF34D399.toInt()
                state.isUnlocked -> 0xFFF3F1FF.toInt()
                else -> 0xFF475569.toInt()
            })
            setPadding(0, 4, 0, 0)
            maxLines = 2
        }
        cell.addView(titleView)

        // Goal (small)
        val goal = TextView(this).apply {
            text = "${state.mission.n}"
            textSize = 11f
            setTextColor(0xFF94A3B8.toInt())
            setPadding(0, 4, 0, 0)
        }
        cell.addView(goal)

        if (state.isUnlocked && !state.isCompleted) {
            cell.setOnClickListener { launchMission(state) }
        }

        return cell
    }

    private fun launchMission(state: MissionProgressManager.MissionState) {
        val intent = Intent(this, GameActivity::class.java)
        intent.putExtra("mode", GameMode.MISSION.name)
        intent.putExtra("mission_index", state.index)
        intent.putExtra("mission_kind", state.mission.kind.name)
        intent.putExtra("mission_n", state.mission.n)
        intent.putExtra("mission_level", state.mission.level ?: 1)
        startActivity(intent)
    }
}
