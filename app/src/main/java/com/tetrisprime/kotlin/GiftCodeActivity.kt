package com.tetrisprime.kotlin

import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/**
 * Phase 8.8 — Gift code redemption screen.
 *
 * User enters a code, sees the result (success with reward, or failure reason).
 * Shows count of codes redeemed so far.
 */
class GiftCodeActivity : AppCompatActivity() {

    private lateinit var resultLabel: TextView
    private lateinit var redeemedCountLabel: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 80, 40, 40)
            setBackgroundColor(0xFF0B081E.toInt())
            gravity = Gravity.CENTER_VERTICAL
        }

        // Icon
        container.addView(TextView(this).apply {
            text = "🎁"
            textSize = 64f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 16)
        })

        // Title
        container.addView(TextView(this).apply {
            text = "أكواد الهدية"
            textSize = 24f
            setTextColor(0xFFF3F1FF.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 8)
        })

        // Subtitle
        container.addView(TextView(this).apply {
            text = "أدخل رمزاً لاستلام المكافأة"
            textSize = 13f
            setTextColor(0xFF94A3B8.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 24)
        })

        // Input
        val input = EditText(this).apply {
            hint = "مثال: WELCOME200"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
            setTextColor(0xFFF3F1FF.toInt())
            setHintTextColor(0xFF64748B.toInt())
            setPadding(24, 16, 24, 16)
            val d = android.graphics.drawable.GradientDrawable()
            d.setColor(0xFF1E1B4B.toInt())
            d.cornerRadius = 12f
            d.setStroke(2, 0xFF8B5CF6.toInt())
            background = d
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 16 }
        }
        container.addView(input)

        // Redeem button
        val redeemBtn = Button(this).apply {
            text = "استرداد"
            setBackgroundColor(0xFF8B5CF6.toInt())
            setTextColor(0xFFFFFFFF.toInt())
            setOnClickListener {
                val code = input.text.toString()
                if (code.isBlank()) {
                    Toast.makeText(this@GiftCodeActivity, "أدخل رمزاً", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val result = GiftCodes.redeem(this@GiftCodeActivity, code)
                when (result) {
                    is GiftCodes.RedeemResult.Success -> {
                        val rewardText = GiftCodes.rewardLabel(result.reward)
                        resultLabel.text = "✅ ${result.code.description}\nالمكافأة: $rewardText"
                        resultLabel.setTextColor(0xFF34D399.toInt())
                        input.text.clear()
                        refreshRedeemedCount()
                    }
                    is GiftCodes.RedeemResult.Failure -> {
                        resultLabel.text = "❌ ${result.reason}"
                        resultLabel.setTextColor(0xFFEF4444.toInt())
                    }
                }
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 16 }
        }
        container.addView(redeemBtn)

        // Result label
        resultLabel = TextView(this).apply {
            textSize = 14f
            setTextColor(0xFFF3F1FF.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 8, 0, 24)
            minLines = 2
        }
        container.addView(resultLabel)

        // Redeemed count
        redeemedCountLabel = TextView(this).apply {
            textSize = 13f
            setTextColor(0xFF94A3B8.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 24, 0, 24)
        }
        container.addView(redeemedCountLabel)
        refreshRedeemedCount()

        // Hint with sample codes
        container.addView(TextView(this).apply {
            text = "💡 رموز تجريبية: WELCOME200, PRIME1000, PHOENIXFX"
            textSize = 11f
            setTextColor(0xFF64748B.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 24, 0, 0)
        })

        // Done button
        val doneBtn = Button(this).apply {
            text = "تم"
            setBackgroundColor(0xFF1E1B4B.toInt())
            setTextColor(0xFFF3F1FF.toInt())
            setOnClickListener { finish() }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 32 }
        }
        container.addView(doneBtn)

        setContentView(container)
    }

    private fun refreshRedeemedCount() {
        val count = GiftCodes.redeemedCount(this)
        redeemedCountLabel.text = "الرموز المستخدمة: $count"
    }
}
