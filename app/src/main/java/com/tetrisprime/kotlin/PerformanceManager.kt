package com.tetrisprime.kotlin

import android.content.Context

/**
 * Phase 11.1 — performance profiles.
 *
 * Four manual profiles plus an "auto" mode that steps the profile down
 * (or up) based on measured frame times:
 *
 *   ultra    : 72 FPS (14ms), 2.0× render scale, 44 particles max
 *   high     : 60 FPS (17ms), 1.5× render scale, 40 particles max
 *   balanced : 46 FPS (22ms), 1.25× render scale, 28 particles max
 *   eco      : 32 FPS (31ms), 1.0× render scale, 18 particles max
 *
 * Auto-detection: keeps a rolling average of the last 60 frame times.
 * If the average exceeds the current profile's frame budget by 25% for
 * more than 3 seconds of continuous frames, the profile steps DOWN one
 * level (eco is the floor). It steps UP again when frames run 25% under
 * the next-higher profile's budget for 10 seconds.
 */
object PerformanceManager {

    enum class Profile(
        val label: String,
        val targetFps: Int,
        val renderScale: Float,
        val maxParticles: Int,
        val backgroundLayers: Boolean   // animate background layers?
    ) {
        ULTRA("ultra", 72, 2.0f, 44, true),
        HIGH("high", 60, 1.5f, 40, true),
        BALANCED("balanced", 46, 1.25f, 28, true),
        ECO("eco", 32, 1.0f, 18, false)
    }

    /** Frame budget per profile (ms). */
    val Profile.frameTimeMs: Long
        get() = (1000L + targetFps - 1) / targetFps  // ceil(1000/fps)

    /** Supported selection values (stored in Settings.performance). */
    val SELECTIONS = listOf("auto", "ultra", "high", "balanced", "eco")

    // ---- Runtime state ----
    @Volatile var activeProfile: Profile = Profile.HIGH
        private set

    @Volatile var autoDetect: Boolean = true
        private set

    // Rolling frame-time window
    private val frameWindow = LongArray(60)
    private var frameWindowIndex = 0
    private var frameWindowFilled = 0

    // Step-down / step-up timers
    private var slowFramesSince = 0L   // ms spent continuously above budget
    private var fastFramesSince = 0L   // ms spent continuously below next-higher budget

    /** Listeners notified when the active profile changes (for HUD). */
    private val listeners = mutableListOf<(Profile) -> Unit>()

    fun addListener(l: (Profile) -> Unit) { listeners.add(l) }
    fun removeListener(l: (Profile) -> Unit) { listeners.remove(l) }

    /** Initialize from saved settings. Call from Activity.onCreate. */
    fun init(ctx: Context) {
        val settings = SettingsManager.load(ctx)
        applySelection(settings.performance)
    }

    /**
     * Apply a profile selection ("auto", "ultra", "high", "balanced", "eco").
     */
    fun applySelection(selection: String) {
        autoDetect = (selection == "auto")
        activeProfile = when (selection) {
            "ultra"    -> Profile.ULTRA
            "high"     -> Profile.HIGH
            "balanced" -> Profile.BALANCED
            "eco"      -> Profile.ECO
            else       -> detectDefaultProfile()
        }
        notifyListeners()
    }

    /** Sensible default for auto mode on first run. */
    private fun detectDefaultProfile(): Profile = Profile.HIGH

    /**
     * Record a rendered frame's duration (ms). Called by the render loop.
     * Only drives auto step-down/up when autoDetect is on.
     */
    fun recordFrameTime(frameMs: Long) {
        // Rolling window
        frameWindow[frameWindowIndex] = frameMs
        frameWindowIndex = (frameWindowIndex + 1) % frameWindow.size
        if (frameWindowFilled < frameWindow.size) frameWindowFilled++

        if (!autoDetect) return

        val budget = activeProfile.frameTimeMs
        if (frameMs > (budget + budget / 4)) {
            // Frame overran budget by >25%
            slowFramesSince += frameMs
            fastFramesSince = 0L
            if (slowFramesSince >= 3000L) {
                stepDown()
                slowFramesSince = 0L
            }
        } else if (frameMs < (budget * 3 / 4)) {
            // Frame well under budget — maybe we can step up
            fastFramesSince += frameMs
            slowFramesSince = 0L
            if (fastFramesSince >= 10_000L) {
                stepUp()
                fastFramesSince = 0L
            }
        } else {
            slowFramesSince = 0L
            fastFramesSince = 0L
        }
    }

    /** Average frame time over the rolling window (ms), or 0 if not enough data. */
    fun averageFrameTimeMs(): Long {
        if (frameWindowFilled == 0) return 0L
        var sum = 0L
        for (i in 0 until frameWindowFilled) sum += frameWindow[i]
        return sum / frameWindowFilled
    }

    /** Measured FPS over the rolling window. */
    fun measuredFps(): Int {
        val avg = averageFrameTimeMs()
        return if (avg > 0) (1000 / avg).toInt() else 0
    }

    private fun stepDown() {
        val next = when (activeProfile) {
            Profile.ULTRA    -> Profile.HIGH
            Profile.HIGH     -> Profile.BALANCED
            Profile.BALANCED -> Profile.ECO
            Profile.ECO      -> Profile.ECO   // floor
        }
        if (next != activeProfile) {
            activeProfile = next
            notifyListeners()
        }
    }

    private fun stepUp() {
        val next = when (activeProfile) {
            Profile.ECO      -> Profile.BALANCED
            Profile.BALANCED -> Profile.HIGH
            Profile.HIGH     -> Profile.ULTRA
            Profile.ULTRA    -> Profile.ULTRA   // ceiling
        }
        if (next != activeProfile) {
            activeProfile = next
            notifyListeners()
        }
    }

    private fun notifyListeners() {
        val p = activeProfile
        listeners.forEach { l -> l(p) }
    }

    /** Human-readable label of the current profile for HUD display. */
    fun hudLabel(): String =
        "${activeProfile.label} · ${measuredFps()}fps"
}
