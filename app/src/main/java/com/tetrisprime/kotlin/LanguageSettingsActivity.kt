package com.tetrisprime.kotlin

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * Phase 9 — Language picker.
 *
 * Shows all 16 supported languages with their native name, flag emoji,
 * and an RTL badge for ar/fa/ur. Selecting a language persists it to
 * SettingsManager.lang and updates TranslationManager immediately.
 */
class LanguageSettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val settings = SettingsManager.load(this)
        val currentLang = TranslationManager.resolveLanguage(settings.lang)

        val scroll = ScrollView(this)
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 60, 32, 80)
            setBackgroundColor(0xFF0B081E.toInt())
        }

        // Header
        container.addView(TextView(this).apply {
            text = "🌐 " + TranslationManager.t(TranslationManager.Keys.SETTINGS_LANGUAGE)
            textSize = 22f
            setTextColor(0xFFF3F1FF.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 24)
        })

        // Auto option
        container.addView(langRow("auto", "🌐", "Auto", "اتجاه النظام", currentLang))

        // All supported languages
        for (code in TranslationManager.SUPPORTED_LANGUAGES) {
            val nativeName = TranslationManager.NATIVE_NAMES[code] ?: code
            val flag = TranslationManager.FLAGS[code] ?: "🌐"
            val rtlLabel = if (TranslationManager.isRtl(code)) "RTL" else ""
            container.addView(langRow(code, flag, nativeName, rtlLabel, currentLang))
        }

        scroll.addView(container)
        setContentView(scroll)
    }

    private fun langRow(
        code: String,
        flag: String,
        nativeName: String,
        rtlBadge: String,
        currentLang: String
    ): View {
        val isCurrent = (code == "auto" && currentLang == TranslationManager.resolveLanguage("auto")) ||
                        (code != "auto" && code == currentLang)

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

        // Flag emoji
        val flagView = TextView(this).apply {
            text = flag
            textSize = 24f
            setPadding(0, 0, 16, 0)
        }
        row.addView(flagView)

        // Native name + RTL badge
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val nameView = TextView(this).apply {
            text = nativeName
            textSize = 15f
            setTextColor(if (isCurrent) 0xFFF3F1FF.toInt() else 0xFFCBD5E1.toInt())
        }
        col.addView(nameView)
        if (rtlBadge.isNotEmpty()) {
            val badgeView = TextView(this).apply {
                text = rtlBadge
                textSize = 11f
                setTextColor(0xFFA78BFA.toInt())
                setPadding(0, 2, 0, 0)
            }
            col.addView(badgeView)
        }
        row.addView(col)

        // Current indicator
        if (isCurrent) {
            val check = TextView(this).apply {
                text = "✓"
                textSize = 18f
                setTextColor(0xFF34D399.toInt())
            }
            row.addView(check)
        }

        row.setOnClickListener {
            val settings = SettingsManager.load(this)
            SettingsManager.save(this, settings.copy(lang = code))
            TranslationManager.setLanguage(code)
            // Recreate this activity to refresh the "current" highlighting
            recreate()
        }

        return row
    }
}
