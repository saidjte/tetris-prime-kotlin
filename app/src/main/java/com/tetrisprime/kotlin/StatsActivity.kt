package com.tetrisprime.kotlin

import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * Statistics screen — displays 21 player metrics in grouped cards.
 */
class StatsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val m = StatsManager.load(this)

        val scroll = ScrollView(this)
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 60, 40, 80)
            setBackgroundColor(0xFF0B081E.toInt())
        }

        container.addView(header("📊 الإحصائيات"))

        // Section: Aggregate
        container.addView(card("إجمالي اللعب").apply {
            addView(row("ألعاب", "${m.gamesPlayed}"))
            addView(row("الوقت الكلي", formatTime(m.totalTimeSec)))
            addView(row("القطع الموضوعة", "${m.totalPieces}"))
            addView(row("الخطوط الممحوة", "${m.totalLines}"))
            addView(row("إجمالي النقاط", "${m.totalScore}"))
        })

        // Section: Clear types
        container.addView(card("أنواع المسح").apply {
            addView(row("مفرد (1)", "${m.singleClears}"))
            addView(row("مزدوج (2)", "${m.doubleClears}"))
            addView(row("ثلاثي (3)", "${m.tripleClears}"))
            addView(row("تيتريس (4)", "${m.tetrisClears}"))
            addView(row("T-Spin Mini", "${m.tspinMiniCount}"))
            addView(row("T-Spin Full", "${m.tspinFullCount}"))
            addView(row("Perfect Clear", "${m.perfectClears}"))
        })

        // Section: Records
        container.addView(card("الأرقام القياسية").apply {
            addView(row("أعلى نتيجة", "${m.bestScore}"))
            addView(row("أفضل سبرينت", if (m.bestSprintMs > 0) formatMs(m.bestSprintMs) else "—"))
            addView(row("أفضل ألترا", "${m.bestUltraScore}"))
            addView(row("أطول كومبو", "${m.maxCombo}"))
            addView(row("أطول B2B", "${m.maxB2bChain}"))
        })

        // Section: Completions
        container.addView(card("الإنجازات بالأنماط").apply {
            addView(row("جبن مكتمل", "${m.cheeseCompleted}"))
            addView(row("مهمات مكتملة", "${m.missionsCompleted}"))
            addView(row("تحديات يومية", "${m.dailyChallengesDone}"))
            addView(row("أعلى كومبو في جولة", "${m.bestComboInRun}"))
        })

        scroll.addView(container)
        setContentView(scroll)
    }

    // ---- UI helpers ----

    private fun header(text: String): TextView = TextView(this).apply {
        this.text = text
        textSize = 24f
        setTextColor(0xFFF3F1FF.toInt())
        gravity = Gravity.CENTER
        setPadding(0, 0, 0, 32)
    }

    private fun card(title: String): LinearLayout {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
            val d = android.graphics.drawable.GradientDrawable()
            d.setColor(0xFF1E1B4B.toInt())
            d.cornerRadius = 20f
            background = d
        }
        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = 24 }
        layout.layoutParams = lp

        val titleView = TextView(this).apply {
            this.text = title
            textSize = 16f
            setTextColor(0xFFA78BFA.toInt())
            setPadding(0, 0, 0, 16)
        }
        layout.addView(titleView)
        return layout
    }

    private fun row(label: String, value: String): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 8, 0, 8)
        }
        val l = TextView(this).apply {
            text = label
            textSize = 14f
            setTextColor(0xFFF3F1FF.toInt())
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val v = TextView(this).apply {
            text = value
            textSize = 14f
            setTextColor(0xFF22D3EE.toInt())
            gravity = Gravity.END
        }
        row.addView(l)
        row.addView(v)
        return row
    }

    private fun formatTime(sec: Long): String {
        val h = sec / 3600
        val mn = (sec % 3600) / 60
        val s = sec % 60
        return if (h > 0) String.format("%dس %dد %dث", h, mn, s)
               else String.format("%dد %dث", mn, s)
    }

    private fun formatMs(ms: Long): String {
        val s = ms / 1000.0
        return String.format("%.2f ث", s)
    }
}
