package com.tetrisprime.kotlin

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/**
 * Phase 11.1 — performance profile picker.
 *
 * Options: auto / ultra (72fps) / high (60fps) / balanced (46fps) / eco (32fps).
 * Each row shows target FPS, particle cap, and render scale. The current
 * profile is highlighted; selecting one persists it and applies immediately.
 * A live FPS readout is shown when auto mode is active.
 */
class PerformanceProfileActivity : AppCompatActivity() {

    private lateinit var liveFpsLabel: TextView
    private lateinit var rowsContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PerformanceManager.init(this)

        val scroll = ScrollView(this)
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 60, 32, 80)
            setBackgroundColor(0xFF0B081E.toInt())
        }

        // Header
        container.addView(TextView(this).apply {
            text = "⚡ الأداء"
            textSize = 24f
            setTextColor(0xFFF3F1FF.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 8)
        })

        // Live FPS readout
        liveFpsLabel = TextView(this).apply {
            textSize = 14f
            setTextColor(0xFF34D399.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 16)
        }
        container.addView(liveFpsLabel)

        // Profile rows
        rowsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        container.addView(rowsContainer)
        renderRows()

        // Description
        container.addView(TextView(this).apply {
            text = "الوضع التلقائي يخفض المستوى عند تأخر الإطارات ويرفعه بعد استقرار الأداء."
            textSize = 12f
            setTextColor(0xFF94A3B8.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 24, 0, 0)
        })

        scroll.addView(container)
        setContentView(scroll)
    }

    private fun renderRows() {
        rowsContainer.removeAllViews()
        val settings = SettingsManager.load(this)
        val current = settings.performance

        for (selection in PerformanceManager.SELECTIONS) {
            rowsContainer.addView(profileRow(selection, current))
        }
    }

    private fun profileRow(selection: String, current: String): View {
        val isCurrent = selection == current
        val isActive = when {
            PerformanceManager.autoDetect -> selection == "auto"
            else -> selection == PerformanceManager.activeProfile.name.lowercase()
        }

        val label: String
        val sub: String
        when (selection) {
            "auto" -> {
                label = "🌐 تلقائي"
                sub = "كشف تلقائي بناءً على زمن الإطار — الحالي: ${PerformanceManager.activeProfile.label}"
            }
            "ultra" -> {
                val p = PerformanceManager.Profile.ULTRA
                label = "🚀 ألترا"
                sub = "${p.targetFps} إطار/ث · ${p.maxParticles} جسيم · ${p.renderScale}× رسم"
            }
            "high" -> {
                val p = PerformanceManager.Profile.HIGH
                label = "✨ عالي"
                sub = "${p.targetFps} إطار/ث · ${p.maxParticles} جسيم · ${p.renderScale}× رسم"
            }
            "balanced" -> {
                val p = PerformanceManager.Profile.BALANCED
                label = "⚖️ متوازن"
                sub = "${p.targetFps} إطار/ث · ${p.maxParticles} جسيم · ${p.renderScale}× رسم"
            }
            "eco" -> {
                val p = PerformanceManager.Profile.ECO
                label = "🔋 موفّر"
                sub = "${p.targetFps} إطار/ث · ${p.maxParticles} جسيم · ${p.renderScale}× رسم"
            }
            else -> {
                label = selection
                sub = ""
            }
        }

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(20, 20, 20, 20)
            gravity = Gravity.CENTER_VERTICAL
            val d = android.graphics.drawable.GradientDrawable()
            d.setColor(if (isCurrent) 0xFF312E81.toInt() else 0xFF1E1B4B.toInt())
            d.cornerRadius = 14f
            if (isCurrent) d.setStroke(2, 0xFF8B5CF6.toInt())
            background = d
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 10 }
        }

        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        col.addView(TextView(this).apply {
            text = label
            textSize = 16f
            setTextColor(if (isActive) 0xFF34D399.toInt() else 0xFFF3F1FF.toInt())
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        })
        col.addView(TextView(this).apply {
            text = sub
            textSize = 12f
            setTextColor(0xFF94A3B8.toInt())
            setPadding(0, 4, 0, 0)
        })
        row.addView(col)

        if (isCurrent) {
            row.addView(TextView(this).apply {
                text = "✓"
                textSize = 18f
                setTextColor(0xFF34D399.toInt())
            })
        }

        row.setOnClickListener {
            val settings = SettingsManager.load(this)
            SettingsManager.save(this, settings.copy(performance = selection))
            PerformanceManager.applySelection(selection)
            Toast.makeText(this, "تم التطبيق: $selection", Toast.LENGTH_SHORT).show()
            renderRows()
        }

        return row
    }

    private var fpsTicker: Runnable? = null

    override fun onResume() {
        super.onResume()
        // Live FPS readout ticker
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        val ticker = object : Runnable {
            override fun run() {
                liveFpsLabel.text = "القياس الحالي: ${PerformanceManager.hudLabel()}"
                handler.postDelayed(this, 500)
            }
        }
        fpsTicker = ticker
        handler.post(ticker)
    }

    override fun onPause() {
        super.onPause()
        fpsTicker?.let { t ->
            android.os.Handler(android.os.Looper.getMainLooper()).removeCallbacks(t)
        }
        fpsTicker = null
    }
}
