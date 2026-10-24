package com.tetrisprime.kotlin

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * A single particle in the particle system.
 * Phase 11: all fields are mutable to support object pooling.
 */
data class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var life: Float,       // remaining life in ms
    var maxLife: Float,    // total life in ms
    var color: Int,
    var size: Float,
    var rot: Float = 0f,
    var spin: Float = 0f,
    var seed: Float = 0f,
    var kind: Int = 0,     // particle render type
    var payload: Int = 0   // extra data (e.g., burst count for fireworks)
)

/**
 * A bloom (radial gradient flash) effect.
 */
data class Bloom(
    var x: Float,
    var y: Float,
    var life: Float,
    var maxLife: Float,
    val color: Int,
    var delay: Float = 0f,
    var size: Float = 200f
)

/**
 * A floating text effect (celebratory phrases).
 */
data class FloatingText(
    var x: Float,
    var y: Float,
    val text: String,
    val color: Int,
    var life: Float,
    var maxLife: Float,
    var scale: Float = 1f,
    var lane: Int = 0
)

/**
 * Particle render types (matching original game's kinds).
 */
object ParticleKind {
    const val DEFAULT = 0
    const val STAR = 1
    const val BUBBLE = 2
    const val ELECTRIC = 3
    const val FIREWORK = 4
    const val NEON_STREAK = 5
    const val CRYSTAL_SHARD = 6
    const val PHOENIX_FLAME = 7
    const val PARTY_CONFETTI = 8
    const val GALAXY_SWIRL = 9
    const val AURORA_WAVE = 10
    const val LIGHTNING_BOLT = 11
}

/**
 * Complete particle system for game effects.
 * Handles particles, bloom, confetti, and floating text.
 *
 * Supports 22 FX packs with 6 quality levels.
 */
class ParticleSystem {

    private val particles = mutableListOf<Particle>()

    // Phase 11: object pool — recycles dead Particle instances to avoid GC churn.
    private val particlePool = ArrayDeque<Particle>()
    private const val MAX_POOL_SIZE = 128

    /**
     * Obtain a particle (from the pool if available, otherwise new), add it
     * to the active list, and return it. Signature mirrors the Particle
     * constructor so every emitter call site stays unchanged.
     */
    private fun spawnParticle(
        x: Float, y: Float, vx: Float, vy: Float,
        life: Float, maxLife: Float, color: Int, size: Float,
        rot: Float = 0f, spin: Float = 0f, seed: Float = 0f,
        kind: Int = 0, payload: Int = 0
    ): Particle {
        val recycled = particlePool.removeFirstOrNull()
        val p: Particle
        if (recycled != null) {
            recycled.x = x; recycled.y = y; recycled.vx = vx; recycled.vy = vy
            recycled.life = life; recycled.maxLife = maxLife; recycled.color = color
            recycled.size = size; recycled.rot = rot; recycled.spin = spin
            recycled.seed = seed; recycled.kind = kind; recycled.payload = payload
            p = recycled
        } else {
            p = Particle(x, y, vx, vy, life, maxLife, color, size, rot, spin, seed, kind, payload)
        }
        particles.add(p)
        return p
    }

    /** Return a dead particle to the pool (bounded). */
    private fun recycle(p: Particle) {
        if (particlePool.size < MAX_POOL_SIZE) {
            particlePool.addLast(p)
        }
    }
    private val blooms = mutableListOf<Bloom>()
    private val floatingTexts = mutableListOf<FloatingText>()
    private var confettiCount = 0

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val random = Random.Default

    // FX settings
    private var fxLevel = "high"
    private var fxMul: Float = 1.0f
    private var maxParticles = 48

    // Phase 11: performance profile cap (set via applyPerformanceCap).
    @Volatile private var particleCapOverride: Int? = null

    /** Effective particle cap = FX level cap intersected with profile cap. */
    private fun effectiveCap(): Int {
        val cap = particleCapOverride
        return if (cap != null && cap < maxParticles) cap else maxParticles
    }

    // Current FX pack colors
    private var packColors = listOf("#8B5CF6", "#EC4899", "#22D3EE", "#FBBF24", "#34D399")

    /** Set the FX quality level. */
    fun setFxLevel(level: String) {
        fxLevel = level
        fxMul = when (level) {
            "off" -> 0f
            "low" -> 0.3f
            "medium" -> 0.5f
            "high" -> 1.0f
            "ultra" -> 1.5f
            "legendary" -> 2.2f
            else -> 1.0f
        }
        maxParticles = (48 * (0.5f + 0.5f * fxMul)).toInt()
    }

    /** Set the current FX pack colors. */
    fun setPackColors(colors: List<String>) {
        packColors = colors
    }

    /** Phase 11: intersect particle cap with the active performance profile. */
    fun applyPerformanceCap(profileMaxParticles: Int) {
        particleCapOverride = profileMaxParticles
    }

    /** Get a random color from the current pack. */
    private fun randomColor(): Int {
        val hex = packColors[random.nextInt(packColors.size)]
        return Color.parseColor(hex)
    }

    /** Check if FX is enabled. */
    fun isEnabled(): Boolean = fxMul > 0f

    // === Event handlers ===

    /** Trigger a line clear effect at the given position. */
    fun onClear(linesCleared: Int, x: Float, y: Float, packId: String = "classic") {
        if (!isEnabled()) return

        val available = effectiveCap() - particles.size
        if (available <= 0) return

        when (packId) {
            "burst", "classic" -> emitBurst(linesCleared, x, y, available)
            "stars" -> emitStars(linesCleared, x, y, available)
            "bubbles" -> emitBubbles(linesCleared, x, y, available)
            "electric" -> emitElectric(linesCleared, x, y, available)
            "fireworks" -> emitFireworks(linesCleared, x, y, available)
            "neonrain" -> emitNeonRain(linesCleared, x, y, available)
            "crystal" -> emitCrystal(linesCleared, x, y, available)
            "phoenix" -> emitPhoenix(linesCleared, x, y, available)
            "party" -> emitParty(linesCleared, x, y, available)
            "galaxy" -> emitGalaxy(linesCleared, x, y, available)
            "aurora" -> emitAurora(linesCleared, x, y, available)
            "supernova" -> emitSupernova(linesCleared, x, y, available)
            "tsunami" -> emitTsunami(linesCleared, x, y, available)
            "inferno" -> emitInferno(linesCleared, x, y, available)
            "blizzard" -> emitBlizzard(linesCleared, x, y, available)
            "volcano" -> emitVolcano(linesCleared, x, y, available)
            "earthquake" -> emitEarthquake(linesCleared, x, y, available)
            "tornado" -> emitTornado(linesCleared, x, y, available)
            "lightning" -> emitLightning(linesCleared, x, y, available)
            "rainbow" -> emitRainbow(linesCleared, x, y, available)
            "cosmic" -> emitCosmic(linesCleared, x, y, available)
            else -> emitBurst(linesCleared, x, y, available)
        }
    }

    /** Trigger a bloom (radial flash) effect. */
    fun onBloom(color: Int, x: Float, y: Float) {
        if (!isEnabled()) return
        blooms.add(Bloom(
            x = x, y = y,
            life = 400f, maxLife = 400f,
            color = color,
            size = 200f * (0.7f + 0.3f * fxMul)
        ))
    }

    /** Trigger confetti. */
    fun onConfetti() {
        if (!isEnabled()) return
        confettiCount = (28 * fxMul).toInt()
    }

    /** Trigger a floating text effect. */
    fun onFloatText(text: String, color: Int, lane: Int = 0) {
        floatingTexts.add(FloatingText(
            x = 0f, y = 0f,
            text = text,
            color = color,
            life = 1400f, maxLife = 1400f,
            scale = 1f,
            lane = lane
        ))
    }

    // === Particle emitters ===

    private fun emitBurst(lines: Int, x: Float, y: Float, available: Int) {
        val count = minOf(((8 + 6 * lines) * fxMul).toInt(), available)
        for (i in 0 until count) {
            val angle = random.nextFloat() * 2f * Math.PI.toFloat()
            val speed = (260 + 300 * random.nextFloat()) * fxMul
            particles.add(spawnParticle(
                x = x + (random.nextFloat() - 0.5f) * 20,
                y = y + (random.nextFloat() - 0.5f) * 14,
                vx = cos(angle) * speed,
                vy = sin(angle) * speed - 60,
                life = (350 + 350 * random.nextFloat()) * fxMul,
                maxLife = 700f * fxMul,
                color = randomColor(),
                size = (2 + 3 * random.nextFloat()) * fxMul,
                seed = random.nextFloat() * 1000
            ))
        }
    }

    private fun emitStars(lines: Int, x: Float, y: Float, available: Int) {
        val count = minOf(((12 + 8 * lines) * fxMul).toInt(), available)
        for (i in 0 until count) {
            particles.add(spawnParticle(
                x = x + (random.nextFloat() - 0.5f) * 80,
                y = y + (random.nextFloat() - 0.5f) * 30,
                vx = (random.nextFloat() - 0.5f) * 120 * fxMul,
                vy = (-120 - 200 * random.nextFloat()) * fxMul,
                life = (600 + 500 * random.nextFloat()) * fxMul,
                maxLife = 1100f * fxMul,
                color = randomColor(),
                size = (5 + 4 * random.nextFloat()) * fxMul,
                rot = random.nextFloat() * Math.PI.toFloat(),
                spin = (random.nextFloat() - 0.5f) * 8,
                kind = ParticleKind.STAR
            ))
        }
    }

    private fun emitBubbles(lines: Int, x: Float, y: Float, available: Int) {
        val count = minOf(((10 + 8 * lines) * fxMul).toInt(), available)
        for (i in 0 until count) {
            particles.add(spawnParticle(
                x = x + (random.nextFloat() - 0.5f) * 60,
                y = y + (random.nextFloat() - 0.5f) * 24,
                vx = (random.nextFloat() - 0.5f) * 60 * fxMul,
                vy = (-40 - 120 * random.nextFloat()) * fxMul,
                life = (700 + 500 * random.nextFloat()) * fxMul,
                maxLife = 1200f * fxMul,
                color = randomColor(),
                size = (3 + 5 * random.nextFloat()) * fxMul,
                kind = ParticleKind.BUBBLE
            ))
        }
    }

    private fun emitElectric(lines: Int, x: Float, y: Float, available: Int) {
        val count = minOf(((7 + 5 * lines) * fxMul).toInt(), available)
        for (i in 0 until count) {
            particles.add(spawnParticle(
                x = x + (random.nextFloat() - 0.5f) * 30,
                y = y + (random.nextFloat() - 0.5f) * 16,
                vx = (random.nextFloat() - 0.5f) * 760 * fxMul,
                vy = (-60 - 280 * random.nextFloat()) * fxMul,
                life = (250 + 250 * random.nextFloat()) * fxMul,
                maxLife = 500f * fxMul,
                color = randomColor(),
                size = (10 + 12 * random.nextFloat()) * fxMul,
                kind = ParticleKind.ELECTRIC
            ))
        }
    }

    private fun emitFireworks(lines: Int, x: Float, y: Float, available: Int) {
        val count = minOf(((3 + lines / 2) * fxMul).toInt(), available)
        for (i in 0 until count) {
            particles.add(spawnParticle(
                x = x + (random.nextFloat() - 0.5f) * 50,
                y = y + (random.nextFloat() - 0.5f) * 18,
                vx = (random.nextFloat() - 0.5f) * 90 * fxMul,
                vy = (-420 - 260 * random.nextFloat()) * fxMul,
                life = (420 + 180 * random.nextFloat()) * fxMul,
                maxLife = 600f * fxMul,
                color = randomColor(),
                size = (3.4f + 1.4f * random.nextFloat()) * fxMul,
                kind = ParticleKind.FIREWORK,
                payload = 14 + random.nextInt(7)
            ))
        }
    }

    private fun emitNeonRain(lines: Int, x: Float, y: Float, available: Int) {
        val count = minOf(((12 + 5 * lines) * fxMul).toInt(), available)
        for (i in 0 until count) {
            particles.add(spawnParticle(
                x = x + (random.nextFloat() - 0.5f) * 80,
                y = y - 30 + 40 * random.nextFloat(),
                vx = (random.nextFloat() - 0.5f) * 40 * fxMul,
                vy = (300 + 340 * random.nextFloat()) * fxMul,
                life = (420 + 320 * random.nextFloat()) * fxMul,
                maxLife = 740f * fxMul,
                color = randomColor(),
                size = (8 + 10 * random.nextFloat()) * fxMul,
                kind = ParticleKind.NEON_STREAK
            ))
        }
    }

    private fun emitCrystal(lines: Int, x: Float, y: Float, available: Int) {
        val count = minOf(((10 + 5 * lines) * fxMul).toInt(), available)
        for (i in 0 until count) {
            val angle = -Math.PI.toFloat() / 2 + (random.nextFloat() - 0.5f) * 2.4f
            val speed = (90 + 220 * random.nextFloat()) * fxMul
            particles.add(spawnParticle(
                x = x + (random.nextFloat() - 0.5f) * 30,
                y = y + (random.nextFloat() - 0.5f) * 18,
                vx = cos(angle) * speed,
                vy = sin(angle) * speed - 40,
                life = (700 + 550 * random.nextFloat()) * fxMul,
                maxLife = 1250f * fxMul,
                color = randomColor(),
                size = (3.5f + 4.5f * random.nextFloat()) * fxMul,
                rot = random.nextFloat() * Math.PI.toFloat(),
                spin = (random.nextFloat() - 0.5f) * 5,
                kind = ParticleKind.CRYSTAL_SHARD
            ))
        }
    }

    private fun emitPhoenix(lines: Int, x: Float, y: Float, available: Int) {
        val count = minOf(((9 + 5 * lines) * fxMul).toInt(), available)
        for (i in 0 until count) {
            particles.add(spawnParticle(
                x = x + (random.nextFloat() - 0.5f) * 55,
                y = y + (random.nextFloat() - 0.5f) * 16,
                vx = (random.nextFloat() - 0.5f) * 70 * fxMul,
                vy = (-90 - 160 * random.nextFloat()) * fxMul,
                life = (700 + 500 * random.nextFloat()) * fxMul,
                maxLife = 1200f * fxMul,
                color = randomColor(),
                size = (3.5f + 3.5f * random.nextFloat()) * fxMul,
                kind = ParticleKind.PHOENIX_FLAME
            ))
        }
    }

    private fun emitParty(lines: Int, x: Float, y: Float, available: Int) {
        val count = minOf(((14 + 6 * lines) * fxMul).toInt(), available)
        for (i in 0 until count) {
            val angle = -Math.PI.toFloat() / 2 + (random.nextFloat() - 0.5f) * 2.6f
            val speed = (180 + 320 * random.nextFloat()) * fxMul
            particles.add(spawnParticle(
                x = x + (random.nextFloat() - 0.5f) * 26,
                y = y + (random.nextFloat() - 0.5f) * 12,
                vx = cos(angle) * speed,
                vy = sin(angle) * speed - 60,
                life = (800 + 600 * random.nextFloat()) * fxMul,
                maxLife = 1400f * fxMul,
                color = randomColor(),
                size = (4 + 3.5f * random.nextFloat()) * fxMul,
                rot = random.nextFloat() * Math.PI.toFloat(),
                spin = (random.nextFloat() - 0.5f) * 14,
                kind = ParticleKind.PARTY_CONFETTI
            ))
        }
    }

    private fun emitGalaxy(lines: Int, x: Float, y: Float, available: Int) {
        val count = minOf(((12 + 5 * lines) * fxMul).toInt(), available)
        for (i in 0 until count) {
            val angle = random.nextFloat() * 2f * Math.PI.toFloat()
            val speed = (110 + 170 * random.nextFloat()) * fxMul
            particles.add(spawnParticle(
                x = x + cos(angle) * (4 + 22 * random.nextFloat()),
                y = y + sin(angle) * (3 + 14 * random.nextFloat()),
                vx = -sin(angle) * speed,
                vy = cos(angle) * speed * 0.55f - 30,
                life = (750 + 550 * random.nextFloat()) * fxMul,
                maxLife = 1300f * fxMul,
                color = randomColor(),
                size = (2 + 2.6f * random.nextFloat()) * fxMul,
                kind = ParticleKind.GALAXY_SWIRL
            ))
        }
    }

    private fun emitAurora(lines: Int, x: Float, y: Float, available: Int) {
        val count = minOf(((6 + 3 * lines) * fxMul).toInt(), available)
        for (i in 0 until count) {
            particles.add(spawnParticle(
                x = x + (random.nextFloat() - 0.5f) * 80,
                y = y + (random.nextFloat() - 0.5f) * 26,
                vx = (random.nextFloat() - 0.5f) * 30 * fxMul,
                vy = (-28 - 46 * random.nextFloat()) * fxMul,
                life = (1000 + 600 * random.nextFloat()) * fxMul,
                maxLife = 1600f * fxMul,
                color = randomColor(),
                size = (10 + 9 * random.nextFloat()) * fxMul,
                kind = ParticleKind.AURORA_WAVE
            ))
        }
    }

    private fun emitSupernova(lines: Int, x: Float, y: Float, available: Int) {
        val count = minOf(((20 + 8 * lines) * fxMul).toInt(), available)
        for (i in 0 until count) {
            val angle = random.nextFloat() * 2f * Math.PI.toFloat()
            val speed = (300 + 400 * random.nextFloat()) * fxMul
            particles.add(spawnParticle(
                x = x, y = y,
                vx = cos(angle) * speed,
                vy = sin(angle) * speed,
                life = (500 + 400 * random.nextFloat()) * fxMul,
                maxLife = 900f * fxMul,
                color = randomColor(),
                size = (3 + 5 * random.nextFloat()) * fxMul,
                seed = random.nextFloat() * 1000
            ))
        }
    }

    private fun emitTsunami(lines: Int, x: Float, y: Float, available: Int) {
        val count = minOf(((15 + 6 * lines) * fxMul).toInt(), available)
        for (i in 0 until count) {
            particles.add(spawnParticle(
                x = x + (random.nextFloat() - 0.5f) * 60,
                y = y + (random.nextFloat() - 0.5f) * 20,
                vx = ((if (random.nextBoolean()) 1 else -1) * (200 + 200 * random.nextFloat())) * fxMul,
                vy = (-50 - 100 * random.nextFloat()) * fxMul,
                life = (600 + 400 * random.nextFloat()) * fxMul,
                maxLife = 1000f * fxMul,
                color = randomColor(),
                size = (4 + 6 * random.nextFloat()) * fxMul,
                kind = ParticleKind.BUBBLE
            ))
        }
    }

    private fun emitInferno(lines: Int, x: Float, y: Float, available: Int) {
        val count = minOf(((18 + 7 * lines) * fxMul).toInt(), available)
        for (i in 0 until count) {
            particles.add(spawnParticle(
                x = x + (random.nextFloat() - 0.5f) * 40,
                y = y + (random.nextFloat() - 0.5f) * 16,
                vx = (random.nextFloat() - 0.5f) * 100 * fxMul,
                vy = (-150 - 250 * random.nextFloat()) * fxMul,
                life = (700 + 500 * random.nextFloat()) * fxMul,
                maxLife = 1200f * fxMul,
                color = randomColor(),
                size = (5 + 7 * random.nextFloat()) * fxMul,
                kind = ParticleKind.PHOENIX_FLAME
            ))
        }
    }

    private fun emitBlizzard(lines: Int, x: Float, y: Float, available: Int) {
        val count = minOf(((16 + 6 * lines) * fxMul).toInt(), available)
        for (i in 0 until count) {
            particles.add(spawnParticle(
                x = x + (random.nextFloat() - 0.5f) * 70,
                y = y - 20 + 30 * random.nextFloat(),
                vx = (random.nextFloat() - 0.5f) * 60 * fxMul,
                vy = (100 + 200 * random.nextFloat()) * fxMul,
                life = (800 + 500 * random.nextFloat()) * fxMul,
                maxLife = 1300f * fxMul,
                color = randomColor(),
                size = (3 + 4 * random.nextFloat()) * fxMul,
                rot = random.nextFloat() * Math.PI.toFloat(),
                spin = (random.nextFloat() - 0.5f) * 6,
                kind = ParticleKind.STAR
            ))
        }
    }

    private fun emitVolcano(lines: Int, x: Float, y: Float, available: Int) {
        val count = minOf(((14 + 6 * lines) * fxMul).toInt(), available)
        for (i in 0 until count) {
            val angle = -Math.PI.toFloat() / 2 + (random.nextFloat() - 0.5f) * 1.5f
            val speed = (300 + 400 * random.nextFloat()) * fxMul
            particles.add(spawnParticle(
                x = x + (random.nextFloat() - 0.5f) * 30,
                y = y,
                vx = cos(angle) * speed,
                vy = sin(angle) * speed,
                life = (600 + 400 * random.nextFloat()) * fxMul,
                maxLife = 1000f * fxMul,
                color = randomColor(),
                size = (4 + 6 * random.nextFloat()) * fxMul,
                kind = ParticleKind.PHOENIX_FLAME
            ))
        }
    }

    private fun emitEarthquake(lines: Int, x: Float, y: Float, available: Int) {
        val count = minOf(((12 + 5 * lines) * fxMul).toInt(), available)
        for (i in 0 until count) {
            particles.add(spawnParticle(
                x = x + (random.nextFloat() - 0.5f) * 50,
                y = y + (random.nextFloat() - 0.5f) * 20,
                vx = (random.nextFloat() - 0.5f) * 200 * fxMul,
                vy = (-80 - 150 * random.nextFloat()) * fxMul,
                life = (500 + 400 * random.nextFloat()) * fxMul,
                maxLife = 900f * fxMul,
                color = randomColor(),
                size = (4 + 5 * random.nextFloat()) * fxMul
            ))
        }
    }

    private fun emitTornado(lines: Int, x: Float, y: Float, available: Int) {
        val count = minOf(((14 + 5 * lines) * fxMul).toInt(), available)
        for (i in 0 until count) {
            val angle = random.nextFloat() * 2f * Math.PI.toFloat()
            val radius = 20 + 30 * random.nextFloat()
            particles.add(spawnParticle(
                x = x + cos(angle) * radius,
                y = y + sin(angle) * radius * 0.5f,
                vx = -sin(angle) * 150 * fxMul,
                vy = cos(angle) * 150 * fxMul * 0.5f - 50,
                life = (600 + 400 * random.nextFloat()) * fxMul,
                maxLife = 1000f * fxMul,
                color = randomColor(),
                size = (3 + 4 * random.nextFloat()) * fxMul,
                kind = ParticleKind.GALAXY_SWIRL
            ))
        }
    }

    private fun emitLightning(lines: Int, x: Float, y: Float, available: Int) {
        val count = minOf(((10 + 4 * lines) * fxMul).toInt(), available)
        for (i in 0 until count) {
            val angle = random.nextFloat() * 2f * Math.PI.toFloat()
            val speed = (400 + 300 * random.nextFloat()) * fxMul
            particles.add(spawnParticle(
                x = x, y = y,
                vx = cos(angle) * speed,
                vy = sin(angle) * speed,
                life = (200 + 200 * random.nextFloat()) * fxMul,
                maxLife = 400f * fxMul,
                color = randomColor(),
                size = (8 + 10 * random.nextFloat()) * fxMul,
                kind = ParticleKind.LIGHTNING_BOLT
            ))
        }
    }

    private fun emitRainbow(lines: Int, x: Float, y: Float, available: Int) {
        val rainbowColors = listOf("#EF4444", "#F97316", "#FBBF24", "#34D399", "#3B82F6", "#A78BFA", "#EC4899")
        val count = minOf(((18 + 7 * lines) * fxMul).toInt(), available)
        for (i in 0 until count) {
            val angle = random.nextFloat() * 2f * Math.PI.toFloat()
            val speed = (200 + 300 * random.nextFloat()) * fxMul
            particles.add(spawnParticle(
                x = x, y = y,
                vx = cos(angle) * speed,
                vy = sin(angle) * speed - 60,
                life = (600 + 400 * random.nextFloat()) * fxMul,
                maxLife = 1000f * fxMul,
                color = Color.parseColor(rainbowColors[i % rainbowColors.size]),
                size = (4 + 5 * random.nextFloat()) * fxMul
            ))
        }
    }

    private fun emitCosmic(lines: Int, x: Float, y: Float, available: Int) {
        val count = minOf(((20 + 8 * lines) * fxMul).toInt(), available)
        for (i in 0 until count) {
            val angle = random.nextFloat() * 2f * Math.PI.toFloat()
            val speed = (100 + 200 * random.nextFloat()) * fxMul
            particles.add(spawnParticle(
                x = x + cos(angle) * 10,
                y = y + sin(angle) * 10,
                vx = cos(angle) * speed,
                vy = sin(angle) * speed * 0.5f - 40,
                life = (800 + 600 * random.nextFloat()) * fxMul,
                maxLife = 1400f * fxMul,
                color = randomColor(),
                size = (3 + 4 * random.nextFloat()) * fxMul,
                kind = ParticleKind.GALAXY_SWIRL
            ))
        }
    }

    // === Update + Render ===

    /** Update all particles. deltaMs in milliseconds. */
    fun update(deltaMs: Float) {
        val dt = deltaMs / 1000f

        // Update particles
        val pit = particles.iterator()
        while (pit.hasNext()) {
            val p = pit.next()
            p.x += p.vx * dt
            p.y += p.vy * dt
            p.vy += 200 * dt // gravity
            p.rot += p.spin * dt
            p.life -= deltaMs
            if (p.life <= 0) {
                // Firework: explode payload
                if (p.kind == ParticleKind.FIREWORK && p.payload > 0) {
                    for (i in 0 until p.payload) {
                        if (particles.size >= effectiveCap()) break
                        val angle = random.nextFloat() * 2f * Math.PI.toFloat()
                        val speed = 100 + 150 * random.nextFloat()
                        spawnParticle(
                            x = p.x, y = p.y,
                            vx = cos(angle) * speed,
                            vy = sin(angle) * speed,
                            life = 400 + 200 * random.nextFloat(),
                            maxLife = 600f,
                            color = p.color,
                            size = 2 + 2 * random.nextFloat()
                        )
                    }
                }
                recycle(p)
                pit.remove()
            }
        }

        // Cap particles
        val cap = effectiveCap()
        if (particles.size > cap) {
            particles.subList(0, particles.size - cap).clear()
        }

        // Update blooms
        val bit = blooms.iterator()
        while (bit.hasNext()) {
            val b = bit.next()
            if (b.delay > 0) {
                b.delay -= deltaMs
            } else {
                b.life -= deltaMs
            }
            if (b.life <= 0) bit.remove()
        }

        // Update floating texts
        val fit = floatingTexts.iterator()
        while (fit.hasNext()) {
            val ft = fit.next()
            ft.life -= deltaMs
            ft.y -= 30 * dt // float upward
            if (ft.life <= 0) fit.remove()
        }

        // Update confetti
        if (confettiCount > 0) {
            // Confetti handled in render
        }
    }

    /** Render all effects to canvas. */
    fun render(canvas: Canvas, boardLeft: Float, boardTop: Float, boardWidth: Float, boardHeight: Float, cellSize: Float) {
        if (!isEnabled()) return

        // Render blooms (radial gradient flashes)
        for (b in blooms) {
            if (b.delay > 0) continue
            val progress = 1f - (b.life / b.maxLife)
            val alpha = (255 * (1f - progress) * 0.75f).toInt()
            val scale = 0.55f + progress * 0.65f
            val radius = b.size * scale
            paint.color = Color.argb(alpha, Color.red(b.color), Color.green(b.color), Color.blue(b.color))
            paint.alpha = alpha
            canvas.drawCircle(b.x, b.y, radius, paint)
        }

        // Render particles
        for (p in particles) {
            val alpha = ((p.life / p.maxLife) * 255).toInt().coerceIn(0, 255)
            paint.color = p.color
            paint.alpha = alpha

            when (p.kind) {
                ParticleKind.STAR -> {
                    paint.style = Paint.Style.FILL
                    drawStar(canvas, p.x, p.y, p.size, p.rot)
                }
                ParticleKind.BUBBLE -> {
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = 2f
                    canvas.drawCircle(p.x, p.y, p.size, paint)
                    paint.style = Paint.Style.FILL
                }
                ParticleKind.ELECTRIC -> {
                    paint.style = Paint.Style.FILL
                    canvas.drawRect(p.x - p.size / 2, p.y - p.size / 2, p.x + p.size / 2, p.y + p.size / 2, paint)
                }
                ParticleKind.CRYSTAL_SHARD -> {
                    paint.style = Paint.Style.FILL
                    drawShard(canvas, p.x, p.y, p.size, p.rot)
                }
                ParticleKind.PHOENIX_FLAME -> {
                    paint.style = Paint.Style.FILL
                    canvas.drawCircle(p.x, p.y, p.size, paint)
                }
                ParticleKind.PARTY_CONFETTI -> {
                    paint.style = Paint.Style.FILL
                    canvas.save()
                    canvas.rotate(p.rot * 57.3f, p.x, p.y)
                    canvas.drawRect(p.x - p.size / 2, p.y - p.size / 4, p.x + p.size / 2, p.y + p.size / 4, paint)
                    canvas.restore()
                }
                ParticleKind.GALAXY_SWIRL -> {
                    paint.style = Paint.Style.FILL
                    canvas.drawCircle(p.x, p.y, p.size, paint)
                }
                ParticleKind.AURORA_WAVE -> {
                    paint.style = Paint.Style.FILL
                    paint.alpha = alpha / 2
                    canvas.drawCircle(p.x, p.y, p.size, paint)
                }
                ParticleKind.LIGHTNING_BOLT -> {
                    paint.style = Paint.Style.FILL
                    canvas.drawRect(p.x - p.size / 4, p.y - p.size, p.x + p.size / 4, p.y + p.size, paint)
                }
                else -> {
                    paint.style = Paint.Style.FILL
                    canvas.drawCircle(p.x, p.y, p.size, paint)
                }
            }
        }
        paint.alpha = 255

        // Render floating texts
        for (ft in floatingTexts) {
            val alpha = ((ft.life / ft.maxLife) * 255).toInt().coerceIn(0, 255)
            val progress = 1f - (ft.life / ft.maxLife)
            val scale = if (progress < 0.2f) {
                0.2f + progress * 5f * 0.8f // pop-in
            } else {
                1f
            }
            paint.color = ft.color
            paint.alpha = alpha
            paint.textSize = 42f * scale
            paint.textAlign = Paint.Align.CENTER
            paint.isFakeBoldText = true
            canvas.drawText(ft.text, boardLeft + boardWidth / 2, boardTop + 100 + ft.lane * 60, paint)
        }
        paint.alpha = 255

        // Render confetti
        if (confettiCount > 0) {
            val confettiColors = listOf(
                0xFF8B5CF6.toInt(), 0xFFEC4899.toInt(), 0xFF22D3EE.toInt(),
                0xFFFBBF24.toInt(), 0xFF34D399.toInt(), 0xFFE879F9.toInt()
            )
            for (i in 0 until confettiCount) {
                val cx = boardLeft + (boardWidth * (i * 37 % 100) / 100f)
                val cy = boardTop + (boardHeight * (i * 53 % 100) / 100f)
                paint.color = confettiColors[i % confettiColors.size]
                paint.style = Paint.Style.FILL
                canvas.drawRect(cx, cy, cx + 7, cy + 4, paint)
            }
        }
    }

    private fun drawStar(canvas: Canvas, x: Float, y: Float, size: Float, rot: Float) {
        val path = android.graphics.Path()
        for (i in 0..9) {
            val angle = rot + i * Math.PI.toFloat() / 5 - Math.PI.toFloat() / 2
            val r = if (i % 2 == 0) size else size * 0.45f
            val px = x + cos(angle) * r
            val py = y + sin(angle) * r
            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        path.close()
        canvas.drawPath(path, paint)
    }

    private fun drawShard(canvas: Canvas, x: Float, y: Float, size: Float, rot: Float) {
        val path = android.graphics.Path()
        val s = size
        path.moveTo(x, y - s)
        path.lineTo(x + s * 0.6f, y)
        path.lineTo(x, y + s)
        path.lineTo(x - s * 0.6f, y)
        path.close()
        canvas.save()
        canvas.rotate(rot * 57.3f, x, y)
        canvas.drawPath(path, paint)
        canvas.restore()
    }

    /** Clear all effects. */
    fun clear() {
        particles.clear()
        blooms.clear()
        floatingTexts.clear()
        confettiCount = 0
    }

    /** Get particle count. */
    fun particleCount(): Int = particles.size
}
