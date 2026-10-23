package com.tetrisprime.kotlin

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import kotlin.math.sin
import kotlin.random.Random

/**
 * Background definitions — 9 animated backgrounds.
 */
object BackgroundDefinitions {

    data class Background(
        val id: String,
        val gradientColors: List<Int>, // top → bottom
        val layerType: LayerType,
        val veilAlpha: Int // 0-255 darkening overlay
    )

    enum class LayerType {
        NONE, SUN_GRID, TWINKLE, AURORA, DUNES, BUBBLES, FIREFLIES, MOSAIC
    }

    val ALL_BACKGROUNDS: List<Background> = listOf(
        Background("synthwave", listOf(0xFF1A0B2E.toInt(), 0xFF16213E.toInt(), 0xFF0F3460.toInt()), LayerType.SUN_GRID, 26),
        Background("midnight", listOf(0xFF0F172A.toInt(), 0xFF1E293B.toInt(), 0xFF0F172A.toInt()), LayerType.TWINKLE, 20),
        Background("starfield", listOf(0xFF0B0B2A.toInt(), 0xFF1A1A3E.toInt(), 0xFF0B0B2A.toInt()), LayerType.TWINKLE, 31),
        Background("aurora", listOf(0xFF064E3B.toInt(), 0xFF065F46.toInt(), 0xFF064E3B.toInt()), LayerType.AURORA, 36),
        Background("sunset", listOf(0xFF7C2D12.toInt(), 0xFF9A3412.toInt(), 0xFFC2410C.toInt()), LayerType.DUNES, 41),
        Background("ocean", listOf(0xFF0C4A6E.toInt(), 0xFF075985.toInt(), 0xFF0369A1.toInt()), LayerType.BUBBLES, 33),
        Background("forest", listOf(0xFF052E16.toInt(), 0xFF14532D.toInt(), 0xFF166534.toInt()), LayerType.FIREFLIES, 36),
        Background("zellige", listOf(0xFF060913.toInt(), 0xFF0D1326.toInt(), 0xFF131B33.toInt()), LayerType.MOSAIC, 31),
        Background("mono", listOf(0xFF050505.toInt(), 0xFF0A0A0C.toInt(), 0xFF050505.toInt()), LayerType.NONE, 13)
    )

    fun getBackground(id: String): Background = ALL_BACKGROUNDS.find { it.id == id } ?: ALL_BACKGROUNDS[0]
}

/**
 * Renders animated backgrounds on the game canvas.
 */
class BackgroundRenderer {

    private var background: BackgroundDefinitions.Background = BackgroundDefinitions.getBackground("synthwave")
    private var time = 0f
    private val random = Random.Default
    private val stars = mutableListOf<Star>()
    private val fireflies = mutableListOf<Firefly>()
    private val bubbles = mutableListOf<Bubble>()

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val veilPaint = Paint()

    data class Star(var x: Float, var y: Float, val size: Float, val twinkleOffset: Float)
    data class Firefly(var x: Float, var y: Float, var vx: Float, var vy: Float, val phase: Float)
    data class Bubble(var x: Float, var y: Float, var vy: Float, val size: Float)

    init {
        // Pre-generate stars
        for (i in 0..80) {
            stars.add(Star(
                random.nextFloat(),
                random.nextFloat(),
                1f + random.nextFloat() * 2f,
                random.nextFloat() * 6.28f
            ))
        }
        // Pre-generate fireflies
        for (i in 0..15) {
            fireflies.add(Firefly(
                random.nextFloat(),
                random.nextFloat(),
                (random.nextFloat() - 0.5f) * 0.02f,
                (random.nextFloat() - 0.5f) * 0.02f,
                random.nextFloat() * 6.28f
            ))
        }
        // Pre-generate bubbles
        for (i in 0..12) {
            bubbles.add(Bubble(
                random.nextFloat(),
                random.nextFloat(),
                0.01f + random.nextFloat() * 0.03f,
                2f + random.nextFloat() * 5f
            ))
        }
    }

    /** Set the active background. */
    fun setBackground(id: String) {
        background = BackgroundDefinitions.getBackground(id)
    }

    /** Update animation. */
    fun update(deltaMs: Float) {
        time += deltaMs / 1000f
    }

    /** Render the background to canvas. */
    fun render(canvas: Canvas, width: Float, height: Float) {
        // Draw gradient background
        val colors = IntArray(background.gradientColors.size) { background.gradientColors[it] }
        val positions = floatArrayOf(0f, 0.55f, 1f)
        paint.shader = LinearGradient(0f, 0f, 0f, height, colors, positions, Shader.TileMode.CLAMP)
        paint.alpha = 255
        canvas.drawRect(0f, 0f, width, height, paint)
        paint.shader = null

        // Draw animated layer
        when (background.layerType) {
            BackgroundDefinitions.LayerType.SUN_GRID -> renderSunGrid(canvas, width, height)
            BackgroundDefinitions.LayerType.TWINKLE -> renderTwinkle(canvas, width, height)
            BackgroundDefinitions.LayerType.AURORA -> renderAurora(canvas, width, height)
            BackgroundDefinitions.LayerType.DUNES -> renderDunes(canvas, width, height)
            BackgroundDefinitions.LayerType.BUBBLES -> renderBubbles(canvas, width, height)
            BackgroundDefinitions.LayerType.FIREFLIES -> renderFireflies(canvas, width, height)
            BackgroundDefinitions.LayerType.MOSAIC -> renderMosaic(canvas, width, height)
            BackgroundDefinitions.LayerType.NONE -> {}
        }

        // Draw veil (darkening overlay)
        veilPaint.color = Color.argb(background.veilAlpha, 0, 0, 0)
        canvas.drawRect(0f, 0f, width, height, veilPaint)
    }

    private fun renderSunGrid(canvas: Canvas, w: Float, h: Float) {
        // Perspective grid lines
        paint.color = Color.argb(60, 236, 72, 153) // pink
        paint.strokeWidth = 1f
        paint.style = Paint.Style.STROKE

        val horizon = h * 0.5f
        val vanishX = w / 2f

        // Horizontal lines (perspective)
        for (i in 1..10) {
            val t = i / 10f
            val y = horizon + (h - horizon) * t * t
            canvas.drawLine(0f, y, w, y, paint)
        }

        // Vertical lines (radiating from vanishing point)
        for (i in -5..5) {
            val x = vanishX + i * w * 0.15f
            canvas.drawLine(vanishX, horizon, x, h, paint)
        }

        // Sun (semi-circle above horizon)
        paint.style = Paint.Style.FILL
        paint.color = Color.argb(100, 251, 191, 36)
        val sunRadius = w * 0.12f
        canvas.drawCircle(vanishX, horizon, sunRadius, paint)

        paint.style = Paint.Style.FILL
    }

    private fun renderTwinkle(canvas: Canvas, w: Float, h: Float) {
        paint.style = Paint.Style.FILL
        for (star in stars) {
            val twinkle = 0.5f + 0.5f * sin(time * 2f + star.twinkleOffset)
            val alpha = (twinkle * 200).toInt()
            paint.color = Color.argb(alpha, 255, 255, 255)
            val sx = star.x * w
            val sy = star.y * h
            canvas.drawCircle(sx, sy, star.size, paint)
        }
    }

    private fun renderAurora(canvas: Canvas, w: Float, h: Float) {
        paint.style = Paint.Style.FILL
        val bands = 3
        val bandColors = listOf(
            Color.argb(40, 52, 211, 153),
            Color.argb(30, 34, 211, 238),
            Color.argb(25, 167, 139, 250)
        )
        for (b in 0 until bands) {
            val baseY = h * (0.3f + b * 0.12f)
            paint.color = bandColors[b % bandColors.size]
            for (x in 0..w.toInt() step 4) {
                val wave = sin(time * 0.5f + x * 0.01f + b) * 30f
                canvas.drawCircle(x.toFloat(), baseY + wave, 40f, paint)
            }
        }
    }

    private fun renderDunes(canvas: Canvas, w: Float, h: Float) {
        paint.style = Paint.Style.FILL
        paint.color = Color.argb(50, 217, 119, 6)
        for (i in 0..3) {
            val baseY = h * (0.6f + i * 0.1f)
            val path = android.graphics.Path()
            path.moveTo(0f, h)
            for (x in 0..w.toInt() step 8) {
                val y = baseY + sin(time * 0.3f + x * 0.005f + i) * 20f
                path.lineTo(x.toFloat(), y)
            }
            path.lineTo(w, h)
            path.close()
            canvas.drawPath(path, paint)
        }
    }

    private fun renderBubbles(canvas: Canvas, w: Float, h: Float) {
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.5f
        for (bubble in bubbles) {
            bubble.y -= bubble.vy * 0.01f
            if (bubble.y < 0) bubble.y = 1f

            val alpha = (sin(time + bubble.x * 10) * 50 + 80).toInt().coerceIn(0, 150)
            paint.color = Color.argb(alpha, 125, 211, 252)
            canvas.drawCircle(bubble.x * w, bubble.y * h, bubble.size, paint)
        }
        paint.style = Paint.Style.FILL
    }

    private fun renderFireflies(canvas: Canvas, w: Float, h: Float) {
        paint.style = Paint.Style.FILL
        for (ff in fireflies) {
            ff.x += ff.vx * 0.5f
            ff.y += ff.vy * 0.5f
            if (ff.x < 0 || ff.x > 1) ff.vx *= -1
            if (ff.y < 0 || ff.y > 1) ff.vy *= -1

            val glow = 0.5f + 0.5f * sin(time * 3f + ff.phase)
            val alpha = (glow * 200).toInt()
            paint.color = Color.argb(alpha, 190, 255, 100)
            canvas.drawCircle(ff.x * w, ff.y * h, 2f + glow * 2f, paint)
        }
    }

    private fun renderMosaic(canvas: Canvas, w: Float, h: Float) {
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 0.5f
        paint.color = Color.argb(30, 16, 185, 129)
        val tileSize = 40f
        for (y in 0..h.toInt() step tileSize.toInt()) {
            for (x in 0..w.toInt() step tileSize.toInt()) {
                val offset: Float = if ((x / tileSize.toInt() + y / tileSize.toInt()) % 2 == 0) 0f else tileSize / 2f
                canvas.drawRect(
                    x.toFloat() + offset, y.toFloat(),
                    x.toFloat() + offset + tileSize, y.toFloat() + tileSize,
                    paint
                )
            }
        }
        paint.style = Paint.Style.FILL
    }
}
