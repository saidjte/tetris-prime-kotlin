package com.tetrisprime.kotlin

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

/**
 * Store screen — three categories: FX packs, Themes, Voice packs.
 * Currency shown at top. Buy / Equip actions per item.
 */
class StoreActivity : AppCompatActivity() {

    private enum class Tab(val label: String) { FX("الجسيمات"), THEME("الثيمات"), VOICE("المعلقون") }

    private var currentTab = Tab.FX
    private lateinit var coinLabel: TextView
    private lateinit var listContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 60, 32, 32)
            setBackgroundColor(0xFF0B081E.toInt())
        }

        // Header row: title + coins
        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, 16)
        }
        val title = TextView(this).apply {
            text = "🛒 المتجر"
            textSize = 22f
            setTextColor(0xFFF3F1FF.toInt())
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        coinLabel = TextView(this).apply {
            textSize = 16f
            setTextColor(0xFFFBBF24.toInt())
        }
        headerRow.addView(title)
        headerRow.addView(coinLabel)
        container.addView(headerRow)

        // Phase 8.8: gift codes link
        val giftBtn = android.widget.TextView(this).apply {
            text = "🎁 استرداد كود هدية"
            textSize = 13f
            setTextColor(0xFFFBBF24.toInt())
            setPadding(0, 0, 0, 16)
            setOnClickListener {
                startActivity(android.content.Intent(this@StoreActivity, GiftCodeActivity::class.java))
            }
        }
        container.addView(giftBtn)

        // Tab bar
        val tabRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, 24)
        }
        for (t in Tab.values()) {
            val btn = TextView(this).apply {
                text = t.label
                textSize = 14f
                setTextColor(0xFFF3F1FF.toInt())
                setPadding(24, 12, 24, 12)
                val d = android.graphics.drawable.GradientDrawable()
                d.setColor(if (t == currentTab) 0xFF8B5CF6.toInt() else 0xFF1E1B4B.toInt())
                d.cornerRadius = 16f
                background = d
                setOnClickListener {
                    currentTab = t
                    recreate()
                }
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginEnd = 8
                }
            }
            tabRow.addView(btn)
        }
        container.addView(tabRow)

        // Scrollable list
        val scroll = ScrollView(this)
        listContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        scroll.addView(listContainer)
        container.addView(scroll)

        setContentView(container)
        refresh()
    }

    private fun refresh() {
        coinLabel.text = "💰 ${StoreManager.getCoins(this)}"
        listContainer.removeAllViews()
        when (currentTab) {
            Tab.FX -> renderFxPacks()
            Tab.THEME -> renderThemes()
            Tab.VOICE -> renderVoicePacks()
        }
    }

    private fun renderFxPacks() {
        val owned = StoreManager.getOwned(this, "fx")
        val equipped = StoreManager.getEquipped(this, "fx")
        for (pack in FxPackDefinitions.ALL_PACKS) {
            val isOwned = owned.contains(pack.id)
            val isEquipped = equipped == pack.id
            listContainer.addView(itemCard(
                title = pack.id,
                subtitle = "${pack.colors.size} ألوان",
                accent = pack.accent,
                price = pack.price,
                isOwned = isOwned,
                isEquipped = isEquipped,
                onBuy = {
                    if (StoreManager.buy(this, "fx", pack.id, pack.price)) {
                        toast("تم شراء ${pack.id}")
                        refresh()
                    } else toast("عملات غير كافية")
                },
                onEquip = {
                    StoreManager.setEquipped(this, "fx", pack.id)
                    toast("تم تفعيل ${pack.id}")
                    refresh()
                }
            ))
        }
    }

    private fun renderThemes() {
        val owned = StoreManager.getOwned(this, "theme")
        val equipped = StoreManager.getEquipped(this, "theme")
        for (theme in ThemeDefinitions.ALL_THEMES) {
            val isOwned = owned.contains(theme.id)
            val isEquipped = equipped == theme.id
            listContainer.addView(itemCard(
                title = theme.id,
                subtitle = "ثيم ${theme.id}",
                accent = theme.accent,
                price = theme.price,
                isOwned = isOwned,
                isEquipped = isEquipped,
                onBuy = {
                    if (StoreManager.buy(this, "theme", theme.id, theme.price)) {
                        toast("تم شراء ${theme.id}")
                        refresh()
                    } else toast("عملات غير كافية")
                },
                onEquip = {
                    StoreManager.setEquipped(this, "theme", theme.id)
                    toast("تم تفعيل ${theme.id}")
                    refresh()
                }
            ))
        }
    }

    private fun renderVoicePacks() {
        val owned = StoreManager.getOwned(this, "voice")
        val equipped = StoreManager.getEquipped(this, "voice")
        // Voice pack metadata with friendly prices
        val voiceMeta = mapOf(
            "raffina" to 0, "giant" to 700, "chipmunk" to 700, "cyber" to 900,
            "stadium" to 1000, "echo" to 600, "radio" to 800, "twin" to 800,
            "alien" to 1100, "calm" to 1200, "whisper" to 1200, "hero" to 1500,
            "sage" to 1500
        )
        for (id in VoiceManager.voicePacks.keys) {
            val cfg = VoiceManager.voicePacks[id] ?: continue
            val price = voiceMeta[id] ?: 1000
            val isOwned = owned.contains(id)
            val isEquipped = equipped == id
            listContainer.addView(itemCard(
                title = id,
                subtitle = "pitch=${cfg.pitch}, reverb=${cfg.reverb}",
                accent = "#A78BFA",
                price = price,
                isOwned = isOwned,
                isEquipped = isEquipped,
                onBuy = {
                    if (StoreManager.buy(this, "voice", id, price)) {
                        toast("تم شراء $id")
                        refresh()
                    } else toast("عملات غير كافية")
                },
                onEquip = {
                    StoreManager.setEquipped(this, "voice", id)
                    toast("تم تفعيل $id")
                    refresh()
                }
            ))
        }
    }

    private fun itemCard(
        title: String,
        subtitle: String,
        accent: String,
        price: Int,
        isOwned: Boolean,
        isEquipped: Boolean,
        onBuy: () -> Unit,
        onEquip: () -> Unit
    ): LinearLayout {
        val accentColor = java.lang.Long.parseLong(accent.removePrefix("#"), 16).toInt()
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(20, 20, 20, 20)
            gravity = Gravity.CENTER_VERTICAL
            val d = android.graphics.drawable.GradientDrawable()
            d.setColor(0xFF1E1B4B.toInt())
            d.cornerRadius = 16f
            d.setStroke(if (isEquipped) 3 else 1, accentColor)
            background = d
        }
        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = 12 }
        layout.layoutParams = lp

        // Accent color preview
        val swatch = View(this).apply {
            val d = android.graphics.drawable.GradientDrawable()
            d.setColor(accentColor)
            d.cornerRadius = 8f
            background = d
            layoutParams = LinearLayout.LayoutParams(48, 48).apply { marginEnd = 16 }
        }
        layout.addView(swatch)

        // Title/subtitle column
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val titleView = TextView(this).apply {
            text = title
            textSize = 15f
            setTextColor(0xFFF3F1FF.toInt())
        }
        val subView = TextView(this).apply {
            text = subtitle
            textSize = 12f
            setTextColor(0xFF94A3B8.toInt())
        }
        col.addView(titleView)
        col.addView(subView)
        layout.addView(col)

        // Action button
        val btn = Button(this).apply {
            textSize = 12f
            when {
                isEquipped -> { text = "مفعّل"; isEnabled = false }
                isOwned -> { text = "تفعيل"; setOnClickListener { onEquip() } }
                else -> { text = "💰 $price"; setOnClickListener { onBuy() } }
            }
            val d = android.graphics.drawable.GradientDrawable()
            d.setColor(if (isEquipped) 0xFF34D399.toInt() else if (isOwned) 0xFF8B5CF6.toInt() else 0xFFF59E0B.toInt())
            d.cornerRadius = 12f
            background = d
            setTextColor(0xFFFFFFFF.toInt())
        }
        layout.addView(btn)

        return layout
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
