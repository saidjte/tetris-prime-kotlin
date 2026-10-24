package com.tetrisprime.kotlin

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.media.AudioAttributes
import android.media.SoundPool
import java.io.File

/**
 * Phase 2 — SFX manager loading 22 real sound effects from
 * assets/www/sfx/ (mirrors the original WebView app's audio assets).
 *
 * File names (NO `sfx_` prefix — they are exactly:
 *   move, rotate, soft, hard, lock, hold, click, save,
 *   clear1, clear2, clear3, tetris,
 *   tspin, tspin2, tspin3,
 *   levelup, danger, record, tick, go, over
 *
 * These are loaded once into a SoundPool for low-latency playback.
 * All 22 are < 30KB each → total memory footprint < 600KB.
 */
class SfxManager(private val context: Context) {

    private val soundPool: SoundPool
    private val soundMap = mutableMapOf<String, Int>()
    private var loaded = false
    private var volume = 0.8f
    private var enabled = true

    /** The 22 SFX file names (without .mp3 extension). */
    val sfxNames = listOf(
        // Movement
        "move", "rotate", "soft", "hard", "lock", "hold",
        // UI
        "click", "save",
        // Line clears
        "clear1", "clear2", "clear3", "tetris",
        // T-Spin
        "tspin", "tspin2", "tspin3",
        // Events
        "levelup", "danger", "record", "tick", "go", "over"
    )

    init {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(8)
            .setAudioAttributes(audioAttributes)
            .build()
    }

    /**
     * Load all SFX from assets/www/sfx/.
     * Called from AudioEngine.init().
     */
    fun loadFromAssets() {
        if (loaded) return
        for (name in sfxNames) {
            try {
                val afd = context.assets.openFd("www/sfx/$name.mp3")
                val soundId = soundPool.load(afd, 1)
                soundMap[name] = soundId
                afd.close()
            } catch (e: Exception) {
                // File missing — skip silently
                android.util.Log.w("SfxManager", "Missing SFX: $name (${e.message})")
            }
        }
        loaded = true
    }

    /** Legacy signature — kept for backward compat (no-op now). */
    fun loadFromResources() { loadFromAssets() }

    /** Play a sound effect by name (e.g. "move", "clear1", "tetris"). */
    fun play(name: String) {
        if (!enabled || !loaded) return
        val soundId = soundMap[name] ?: return
        soundPool.play(soundId, volume, volume, 1, 0, 1.0f)
    }

    fun setVolume(vol: Float) { volume = vol.coerceIn(0f, 1f) }
    fun setEnabled(enabled: Boolean) { this.enabled = enabled }

    fun release() {
        soundPool.release()
        soundMap.clear()
        loaded = false
    }
}
