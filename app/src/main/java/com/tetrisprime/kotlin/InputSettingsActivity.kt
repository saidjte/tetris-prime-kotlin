package com.tetrisprime.kotlin

import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * Phase 7 — customize the 8 touch buttons (position via drag, size via slider,
 * visibility per button, reset to default).
 *
 * The user drags buttons around an empty preview area to set their normalized
 * positions. Sizes are tweaked via sliders. Visibility toggles hide individual
 * buttons (e.g., for users who rely on gestures).
 */
class InputSettingsActivity : AppCompatActivity() {

    private lateinit var preview: FrameLayout
    private lateinit var layout: MutableList<ButtonLayout>
    private var selectedButton: InputButton? = null

    // UI for editing the selected button
    private lateinit var sizeLabel: TextView
    private lateinit var sizeSlider: SeekBar
    private lateinit var visibilitySwitch: Switch
    private lateinit var selectedLabel: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        layout = LayoutStore.load(this).toMutableList()

        val scroll = android.widget.ScrollView(this)
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 60, 32, 60)
            setBackgroundColor(0xFF0B081E.toInt())
        }

        container.addView(header("🎮 تخصيص الأزرار"))

        // Preview area (16:9-ish)
        preview = FrameLayout(this).apply {
            setBackgroundColor(0xFF1E1B4B.toInt())
            val d = android.graphics.drawable.GradientDrawable()
            d.setColor(0xFF1E1B4B.toInt())
            d.cornerRadius = 16f
            d.setStroke(2, 0xFFA78BFA.toInt())
            background = d
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                900
            ).apply { bottomMargin = 16 }
        }
        container.addView(preview)

        // Hint
        val hint = TextView(this).apply {
            text = "💡 اسحب أي زر لتغيير موضعه. اختر زراً من القائمة لضبط حجمه."
            textSize = 12f
            setTextColor(0xFF94A3B8.toInt())
            setPadding(0, 0, 0, 16)
        }
        container.addView(hint)

        // Selected button editor
        container.addView(buildEditorCard())

        // Button selector chips
        container.addView(buildButtonSelector())

        // Bottom actions
        container.addView(buildBottomActions())

        scroll.addView(container)
        setContentView(scroll)

        // Render initial layout
        preview.post { renderPreview() }
    }

    private fun header(text: String): TextView = TextView(this).apply {
        this.text = text
        textSize = 24f
        setTextColor(0xFFF3F1FF.toInt())
        gravity = Gravity.CENTER
        setPadding(0, 0, 0, 24)
    }

    private fun buildEditorCard(): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
            val d = android.graphics.drawable.GradientDrawable()
            d.setColor(0xFF312E81.toInt())
            d.cornerRadius = 16f
            background = d
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 16 }
        }

        selectedLabel = TextView(this).apply {
            text = "اختر زراً لتعديله"
            textSize = 15f
            setTextColor(0xFFF3F1FF.toInt())
            setPadding(0, 0, 0, 12)
        }
        card.addView(selectedLabel)

        // Size slider
        val sizeRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        sizeLabel = TextView(this).apply {
            text = "الحجم: 14%"
            textSize = 13f
            setTextColor(0xFF94A3B8.toInt())
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        sizeSlider = SeekBar(this).apply {
            max = 35  // 5%..40%
            progress = 14
            isEnabled = false
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) {
                    val sz = (p + 5) / 100f
                    sizeLabel.text = "الحجم: ${p + 5}%"
                    val btn = selectedButton ?: return
                    val idx = layout.indexOfFirst { it.buttonId == btn }
                    if (idx >= 0) {
                        layout[idx] = layout[idx].copy(sizeN = sz)
                        LayoutStore.save(this@InputSettingsActivity, layout)
                        renderPreview()
                    }
                }
                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            })
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.5f)
        }
        sizeRow.addView(sizeLabel)
        sizeRow.addView(sizeSlider)
        card.addView(sizeRow)

        // Visibility switch
        visibilitySwitch = Switch(this).apply {
            text = "إظهار الزر"
            isEnabled = false
            setOnCheckedChangeListener { _, checked ->
                val btn = selectedButton ?: return@setOnCheckedChangeListener
                val idx = layout.indexOfFirst { it.buttonId == btn }
                if (idx >= 0) {
                    layout[idx] = layout[idx].copy(visible = checked)
                    LayoutStore.save(this@InputSettingsActivity, layout)
                    renderPreview()
                }
            }
        }
        card.addView(visibilitySwitch)

        return card
    }

    private fun buildButtonSelector(): LinearLayout {
        val grid = android.widget.GridLayout(this).apply {
            columnCount = 4
            useDefaultMargins = true
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 16 }
        }
        for (b in InputButton.values()) {
            val chip = TextView(this).apply {
                text = b.defaultIcon
                textSize = 18f
                setTextColor(0xFFF3F1FF.toInt())
                gravity = Gravity.CENTER
                setPadding(16, 16, 16, 16)
                val d = android.graphics.drawable.GradientDrawable()
                d.setColor(0xFF1E1B4B.toInt())
                d.cornerRadius = 12f
                background = d
                setOnClickListener { selectButton(b) }
                layoutParams = android.widget.GridLayout.LayoutParams().apply {
                    width = 0
                    height = android.widget.GridLayout.LayoutParams.WRAP_CONTENT
                    columnSpec = android.widget.GridLayout.spec(android.widget.GridLayout.UNDEFINED, 1, 1f)
                }
            }
            grid.addView(chip)
        }
        return grid
    }

    private fun buildBottomActions(): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        val resetBtn = Button(this).apply {
            text = "استعادة الافتراضي"
            setBackgroundColor(0xFF1E1B4B.toInt())
            setTextColor(0xFFF3F1FF.toInt())
            setOnClickListener {
                LayoutStore.reset(this@InputSettingsActivity)
                layout = DefaultLayouts.RIGHT_HANDED.toMutableList()
                renderPreview()
            }
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = 8 }
        }
        val doneBtn = Button(this).apply {
            text = "تم"
            setBackgroundColor(0xFF8B5CF6.toInt())
            setTextColor(0xFFFFFFFF.toInt())
            setOnClickListener {
                LayoutStore.save(this@InputSettingsActivity, layout)
                finish()
            }
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        row.addView(resetBtn)
        row.addView(doneBtn)
        return row
    }

    private fun selectButton(b: InputButton) {
        selectedButton = b
        val item = layout.find { it.buttonId == b }
        selectedLabel.text = "${b.defaultIcon} ${b.label}"
        sizeSlider.isEnabled = item != null
        visibilitySwitch.isEnabled = item != null
        if (item != null) {
            sizeSlider.progress = ((item.sizeN * 100).toInt() - 5).coerceIn(0, 35)
            sizeLabel.text = "الحجم: ${(item.sizeN * 100).toInt()}%"
            visibilitySwitch.isChecked = item.visible
        }
    }

    private fun renderPreview() {
        preview.removeAllViews()
        val pw = preview.width.toFloat()
        val ph = preview.height.toFloat()
        if (pw <= 0f) return
        for (item in layout) {
            if (!item.visible) continue
            val v = TextView(this).apply {
                text = item.buttonId.defaultIcon
                textSize = 22f
                setTextColor(0xFFFFFFFF.toInt())
                gravity = Gravity.CENTER
                val d = android.graphics.drawable.GradientDrawable()
                d.setColor(0xCC8B5CF6.toInt())
                d.cornerRadius = 24f
                background = d
            }
            val sizePx = (item.sizeN * pw).toInt().coerceIn(60, 240)
            val cx = (item.xN * pw).toInt()
            val cy = (item.yN * ph).toInt()
            val lp = FrameLayout.LayoutParams(sizePx, sizePx).apply {
                leftMargin = (cx - sizePx / 2).coerceAtLeast(0)
                topMargin = (cy - sizePx / 2).coerceAtLeast(0)
            }
            v.layoutParams = lp

            // Drag-to-move
            var dragStartX = 0f
            var dragStartY = 0f
            var origLeft = 0
            var origTop = 0
            v.setOnTouchListener { view, e ->
                when (e.action) {
                    MotionEvent.ACTION_DOWN -> {
                        dragStartX = e.rawX
                        dragStartY = e.rawY
                        origLeft = view.left
                        origTop = view.top
                        selectButton(item.buttonId)
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = e.rawX - dragStartX
                        val dy = e.rawY - dragStartY
                        val newLeft = (origLeft + dx).toInt().coerceIn(0, (pw - view.width).toInt())
                        val newTop  = (origTop + dy).toInt().coerceIn(0, (ph - view.height).toInt())
                        view.x = newLeft.toFloat()
                        view.y = newTop.toFloat()

                        // Update normalized position in real time
                        val idx = layout.indexOfFirst { it.buttonId == item.buttonId }
                        if (idx >= 0) {
                            val newXn = (newLeft + view.width / 2) / pw
                            val newYn = (newTop + view.height / 2) / ph
                            layout[idx] = layout[idx].copy(xN = newXn, yN = newYn)
                        }
                    }
                    MotionEvent.ACTION_UP -> {
                        LayoutStore.save(this@InputSettingsActivity, layout)
                    }
                }
                true
            }
            preview.addView(v)
        }
    }
}
