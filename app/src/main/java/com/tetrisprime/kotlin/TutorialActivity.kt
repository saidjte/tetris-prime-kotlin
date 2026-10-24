package com.tetrisprime.kotlin

import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * Tutorial screen — 12-step walkthrough with next/prev navigation.
 */
class TutorialActivity : AppCompatActivity() {

    private var stepIndex = 0
    private lateinit var iconView: TextView
    private lateinit var titleView: TextView
    private lateinit var bodyView: TextView
    private lateinit var pageIndicator: TextView
    private lateinit var prevBtn: Button
    private lateinit var nextBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 80, 40, 40)
            setBackgroundColor(0xFF0B081E.toInt())
            gravity = Gravity.CENTER_VERTICAL
        }

        // Icon (large)
        iconView = TextView(this).apply {
            textSize = 80f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 24)
        }
        container.addView(iconView)

        // Title
        titleView = TextView(this).apply {
            textSize = 22f
            setTextColor(0xFFF3F1FF.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 16)
        }
        container.addView(titleView)

        // Body
        bodyView = TextView(this).apply {
            textSize = 16f
            setTextColor(0xFFCBD5E1.toInt())
            gravity = Gravity.CENTER
            setPadding(16, 0, 16, 24)
        }
        container.addView(bodyView)

        // Page indicator
        pageIndicator = TextView(this).apply {
            textSize = 14f
            setTextColor(0xFFA78BFA.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 32)
        }
        container.addView(pageIndicator)

        // Buttons row
        val btnRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        prevBtn = Button(this).apply {
            text = "السابق"
            setBackgroundColor(0xFF1E1B4B.toInt())
            setTextColor(0xFFF3F1FF.toInt())
            setOnClickListener {
                if (stepIndex > 0) { stepIndex--; renderStep() }
            }
        }
        nextBtn = Button(this).apply {
            text = "التالي"
            setBackgroundColor(0xFF8B5CF6.toInt())
            setTextColor(0xFFFFFFFF.toInt())
            setOnClickListener {
                if (stepIndex < TutorialDefinitions.ALL.size - 1) { stepIndex++; renderStep() }
                else finish()
            }
        }
        val btnLp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = 12 }
        prevBtn.layoutParams = btnLp
        nextBtn.layoutParams = btnLp
        btnRow.addView(prevBtn)
        btnRow.addView(nextBtn)
        container.addView(btnRow)

        // Skip button
        val skipBtn = Button(this).apply {
            text = "تخطّي"
            setBackgroundColor(0x00000000)
            setTextColor(0xFF64748B.toInt())
            setOnClickListener { finish() }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = 16
                gravity = Gravity.CENTER
            }
        }
        container.addView(skipBtn)

        setContentView(container)
        renderStep()
    }

    private fun renderStep() {
        val step = TutorialDefinitions.ALL[stepIndex]
        iconView.text = step.icon
        titleView.text = step.title
        bodyView.text = step.body
        pageIndicator.text = "${stepIndex + 1} / ${TutorialDefinitions.ALL.size}"
        prevBtn.isEnabled = stepIndex > 0
        prevBtn.alpha = if (prevBtn.isEnabled) 1f else 0.4f
        nextBtn.text = if (stepIndex == TutorialDefinitions.ALL.size - 1) "إنهاء" else "التالي"
    }
}
