package com.tetrisprime.kotlin

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

/**
 * Settings screen — 18 configurable options grouped into sections.
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var settings: Settings
    private val sections = mutableListOf<LinearLayout>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settings = SettingsManager.load(this)
        // Phase 9: init translation system with the user's language
        TranslationManager.init(this)
        // Apply RTL/LTR layout direction based on current language
        applyLayoutDirection()

        val scroll = ScrollView(this)
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 60, 40, 80)
            setBackgroundColor(0xFF0B081E.toInt())
        }

        // Header
        container.addView(header(TranslationManager.t(TranslationManager.Keys.SETTINGS_TITLE)))

        // Section: Audio
        container.addView(section("الصوت").also { s ->
            s.addView(toggle("الموسيقى", settings.music) { settings = settings.copy(music = it); save() })
            s.addView(slider("مستوى الموسيقى", settings.musicVol, 0f..1f) { v -> settings = settings.copy(musicVol = v); save() })
            s.addView(toggle("المؤثرات الصوتية (SFX)", settings.sfx) { settings = settings.copy(sfx = it); save() })
            s.addView(slider("مستوى SFX", settings.sfxVol, 0f..1f) { v -> settings = settings.copy(sfxVol = v); save() })
            s.addView(toggle("المعلق الصوتي", settings.voice) { settings = settings.copy(voice = it); save() })
            s.addView(slider("مستوى المعلق", settings.voiceVol, 0f..1f) { v -> settings = settings.copy(voiceVol = v); save() })
            s.addView(
                dropdown("تكرار المعلق", listOf("sparse", "normal", "dense"), settings.voiceFreq) {
                    settings = settings.copy(voiceFreq = it); save()
                }
            )
            s.addView(toggle("الترجمة النصية", settings.captions) { settings = settings.copy(captions = it); save() })
        })

        // Section: Visuals / FX
        container.addView(section("الجسيمات والتأثيرات").also { s ->
            s.addView(
                dropdown("مستوى FX", listOf("off", "low", "medium", "high", "ultra", "legendary"), settings.fx) {
                    settings = settings.copy(fx = it); save()
                }
            )
        })

        // Section: Gameplay
        container.addView(section("اللعب").also { s ->
            s.addView(toggle("الإيماءات (Swipe)", settings.gestures) { settings = settings.copy(gestures = it); save() })
            s.addView(toggle("عرض 5 قطع قادمة", settings.next5) { settings = settings.copy(next5 = it); save() })
            s.addView(
                dropdown("إظهار القمامة", listOf("off", "withLines", "always"), settings.garbage) {
                    settings = settings.copy(garbage = it); save()
                }
            )
            s.addView(
                dropdown("صعوبة المعركة", listOf("easy", "normal", "hard"), settings.battleLevel) {
                    settings = settings.copy(battleLevel = it); save()
                }
            )
            s.addView(toggle("دعم يد التحكم", settings.gamepad) { settings = settings.copy(gamepad = it); save() })
            s.addView(toggle("رموز القطع", settings.pieceSymbols) { settings = settings.copy(pieceSymbols = it); save() })
            s.addView(toggle("السماح بالدوران المستحيل", settings.allowRotation) { settings = settings.copy(allowRotation = it); save() })
            s.addView(toggle("إخفاء أزرار التحكم", settings.hideCtlButtons) { settings = settings.copy(hideCtlButtons = it); save() })

            // Phase 7: link to button customization screen
            val customizeBtn = Button(this).apply {
                text = "🎮 تخصيص الأزرار"
                setBackgroundColor(0xFF312E81.toInt())
                setTextColor(0xFFF3F1FF.toInt())
                setOnClickListener {
                    startActivity(android.content.Intent(this@SettingsActivity, InputSettingsActivity::class.java))
                }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = 12 }
            }
            s.addView(customizeBtn)
        })

        // Section: Language
        container.addView(section(TranslationManager.t(TranslationManager.Keys.SETTINGS_LANGUAGE)).also { s ->
            // Phase 9: language picker button (opens full 16-language screen)
            val langBtn = Button(this).apply {
                val resolved = TranslationManager.resolveLanguage(settings.lang)
                val nativeName = TranslationManager.NATIVE_NAMES[resolved] ?: resolved
                val flag = TranslationManager.FLAGS[resolved] ?: "🌐"
                val autoLabel = if (settings.lang == "auto") " (تلقائي)" else ""
                text = "$flag $nativeName$autoLabel"
                setBackgroundColor(0xFF312E81.toInt())
                setTextColor(0xFFF3F1FF.toInt())
                setOnClickListener {
                    startActivity(android.content.Intent(this@SettingsActivity, LanguageSettingsActivity::class.java))
                }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = 8 }
            }
            s.addView(langBtn)
        })

        // Done button
        val doneBtn = Button(this).apply {
            text = "تم"
            setBackgroundColor(0xFF8B5CF6.toInt())
            setTextColor(0xFFFFFFFF.toInt())
            setOnClickListener { finish() }
        }
        val doneLp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = 40 }
        container.addView(doneBtn, doneLp)

        scroll.addView(container)
        setContentView(scroll)
    }

    private fun save() = SettingsManager.save(this, settings)

    /**
     * Phase 9 — apply RTL/LTR layout direction based on current language.
     * ar/fa/ur → RTL, everything else → LTR.
     */
    private fun applyLayoutDirection() {
        val isRtl = TranslationManager.isRtl(TranslationManager.currentLang)
        window.decorView.layoutDirection = if (isRtl)
            android.view.View.LAYOUT_DIRECTION_RTL
        else
            android.view.View.LAYOUT_DIRECTION_LTR
    }

    // ---- UI helpers ----

    private fun header(text: String): TextView = TextView(this).apply {
        this.text = text
        textSize = 24f
        setTextColor(0xFFF3F1FF.toInt())
        gravity = Gravity.CENTER
        setPadding(0, 0, 0, 32)
    }

    private fun section(title: String): LinearLayout {
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
        sections.add(layout)
        return layout
    }

    private fun toggle(label: String, initial: Boolean, onChange: (Boolean) -> Unit): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 12, 0, 12)
        }
        val text = TextView(this).apply {
            this.text = label
            textSize = 14f
            setTextColor(0xFFF3F1FF.toInt())
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val sw = Switch(this).apply {
            isChecked = initial
            setOnCheckedChangeListener { _, b -> onChange(b) }
        }
        row.addView(text)
        row.addView(sw)
        return row
    }

    private fun slider(label: String, initial: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit): View {
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 12, 0, 12)
        }
        val labelRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        val text = TextView(this).apply {
            this.text = label
            textSize = 14f
            setTextColor(0xFFF3F1FF.toInt())
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val valueText = TextView(this).apply {
            this.text = "${(initial * 100).toInt()}%"
            textSize = 13f
            setTextColor(0xFF94A3B8.toInt())
        }
        labelRow.addView(text)
        labelRow.addView(valueText)
        col.addView(labelRow)

        val seek = SeekBar(this).apply {
            max = 100
            progress = ((initial - range.start) / (range.endInclusive - range.start) * 100).toInt()
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) {
                    val v = range.start + (p / 100f) * (range.endInclusive - range.start)
                    valueText.text = "${(v * 100).toInt()}%"
                    onChange(v)
                }
                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            })
        }
        col.addView(seek)
        return col
    }

    private fun dropdown(label: String, options: List<String>, initial: String, onChange: (String) -> Unit): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 12, 0, 12)
        }
        val text = TextView(this).apply {
            this.text = label
            textSize = 14f
            setTextColor(0xFFF3F1FF.toInt())
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val spinner = Spinner(this).apply {
            adapter = ArrayAdapter(this@SettingsActivity, android.R.layout.simple_spinner_dropdown_item, options)
            setSelection(options.indexOf(initial).coerceAtLeast(0))
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                    onChange(options[pos])
                }
                override fun onNothingSelected(p: AdapterView<*>?) {}
            }
        }
        row.addView(text)
        row.addView(spinner)
        return row
    }
}
