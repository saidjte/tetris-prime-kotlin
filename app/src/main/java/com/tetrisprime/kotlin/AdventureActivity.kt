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
 * Phase 8.3 — Adventure mode screen.
 *
 * Shows 6 worlds, each containing 6 stages (36 total).
 * Tap an unlocked stage to launch it.
 */
class AdventureActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val worlds = AdventureProgressManager.getWorlds(this)
        val completed = AdventureProgressManager.completedCount(this)

        val scroll = ScrollView(this)
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 60, 32, 80)
            setBackgroundColor(0xFF0B081E.toInt())
        }

        // Header
        container.addView(TextView(this).apply {
            text = "🗺️ المغامرة"
            textSize = 24f
            setTextColor(0xFFF3F1FF.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 8)
        })

        // Progress
        container.addView(TextView(this).apply {
            text = "مراحل مكتملة: $completed / ${AdventureProgressManager.TOTAL_STAGES}"
            textSize = 14f
            setTextColor(0xFF22D3EE.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 24)
        })

        // Token summary
        container.addView(tokenSummary())

        // Each world as a card
        for (world in worlds) {
            container.addView(worldCard(world))
        }

        scroll.addView(container)
        setContentView(scroll)
    }

    private fun tokenSummary(): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 24)
        }
        for (token in AdventureDefinitions.TokenType.values()) {
            val count = AdventureProgressManager.countToken(this, token)
            val tv = TextView(this).apply {
                text = "${token.glyph} $count"
                textSize = 13f
                setTextColor(0xFFF3F1FF.toInt())
                setPadding(12, 8, 12, 8)
                val d = android.graphics.drawable.GradientDrawable()
                d.setColor(0xFF312E81.toInt())
                d.cornerRadius = 12f
                background = d
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { marginEnd = 6 }
            }
            row.addView(tv)
        }
        return row
    }

    private fun worldCard(world: AdventureProgressManager.WorldState): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 20, 20, 20)
            val d = android.graphics.drawable.GradientDrawable()
            d.setColor(0xFF1E1B4B.toInt())
            d.cornerRadius = 20f
            if (world.isUnlocked) d.setStroke(2, world.accentColor)
            background = d
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 16 }
        }

        // World header
        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val titleText = TextView(this).apply {
            text = "${world.name}"
            textSize = 17f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(if (world.isUnlocked) world.accentColor else 0xFF64748B.toInt())
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val progressText = TextView(this).apply {
            text = "${world.completedCount}/${AdventureProgressManager.STAGES_PER_WORLD}"
            textSize = 13f
            setTextColor(0xFF94A3B8.toInt())
        }
        headerRow.addView(titleText)
        headerRow.addView(progressText)
        card.addView(headerRow)

        if (!world.isUnlocked) {
            card.addView(TextView(this).apply {
                text = "🔒 أكمل العالم السابق لفتح هذا العالم"
                textSize = 12f
                setTextColor(0xFFFBBF24.toInt())
                setPadding(0, 12, 0, 0)
            })
            return card
        }

        // Stages row
        val stagesRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 16, 0, 0)
        }
        for (stage in world.stages) {
            stagesRow.addView(stageCell(stage, world.accentColor))
        }
        card.addView(stagesRow)

        return card
    }

    private fun stageCell(stage: AdventureProgressManager.StageState, accent: Int): View {
        val cell = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(8, 12, 8, 12)
            gravity = Gravity.CENTER
            minimumWidth = 130
            minimumHeight = 110
            val d = android.graphics.drawable.GradientDrawable()
            when {
                stage.isCompleted -> {
                    d.setColor(0xFF065F46.toInt())
                    d.setStroke(2, 0xFF34D399.toInt())
                }
                stage.isUnlocked -> {
                    d.setColor(accent and 0x33FFFFFF.toInt())  // translucent accent
                    d.setStroke(2, accent)
                }
                else -> {
                    d.setColor(0xFF1E293B.toInt())
                }
            }
            d.cornerRadius = 12f
            background = d
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = 6 }
        }

        // Stage number
        val num = TextView(this).apply {
            text = "${stage.index + 1}"
            textSize = 14f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(when {
                stage.isCompleted -> 0xFF34D399.toInt()
                stage.isUnlocked -> 0xFFF3F1FF.toInt()
                else -> 0xFF64748B.toInt()
            })
        }
        cell.addView(num)

        // Token glyph if any
        val tokenText = TextView(this).apply {
            text = stage.stage.tokens?.joinToString(" ") { it.glyph } ?: ""
            textSize = 14f
            setPadding(0, 4, 0, 0)
        }
        cell.addView(tokenText)

        // Goal text
        val goal = TextView(this).apply {
            text = MissionProgressManager.describe(
                MissionGoal(
                    id = stage.stage.id,
                    kind = stage.stage.kind,
                    n = stage.stage.n,
                    level = stage.stage.level
                )
            )
            textSize = 10f
            setTextColor(0xFF94A3B8.toInt())
            setPadding(0, 4, 0, 0)
            maxLines = 2
        }
        cell.addView(goal)

        if (stage.isUnlocked && !stage.isCompleted) {
            cell.setOnClickListener { launchStage(stage) }
        }

        return cell
    }

    private fun launchStage(stage: AdventureProgressManager.StageState) {
        val intent = Intent(this, GameActivity::class.java)
        intent.putExtra("mode", GameMode.ADVENTURE.name)
        intent.putExtra("mission_index", stage.index)
        intent.putExtra("mission_kind", stage.stage.kind.name)
        intent.putExtra("mission_n", stage.stage.n)
        intent.putExtra("mission_level", stage.stage.level ?: 1)
        intent.putExtra("adventure_index", stage.index)
        startActivity(intent)
    }
}
