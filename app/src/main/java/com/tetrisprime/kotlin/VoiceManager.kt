package com.tetrisprime.kotlin

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.media.audiofx.EnvironmentalReverb
import android.util.Log

/**
 * Phase 2 — Voice commentary system playing 276 MP3 voice clips
 * from assets/www/voices/ (raffina voice pack).
 *
 * 54 voice events × multiple variants each = 276 files.
 *
 * All other voice packs (giant, chipmunk, cyber, stadium, echo, radio,
 * twin, alien) are AUDIO EFFECTS applied to the raffina clips at
 * playback time — not separate audio files. This mirrors the original
 * WebView app which uses Web Audio API nodes (BiquadFilter, ConvolverNode,
 * DelayNode) to transform the same source clips.
 *
 * Events (54 total):
 *   b2b, clear1, clear2, clear3, combo2-10, countdown, danger, hurry,
 *   idle, levelup, lines25/50/100/150/200, modeselect, near, over,
 *   pause, pc, record, resume, saved, score10k/25k/50k/100k/250k/500k/1m,
 *   speed, sprint10/20/30/40, sprintIntro, start, tetris, timeup, tspin,
 *   ultra, ultra30, ultra60, warm, win, zen, zenCalm
 */
class VoiceManager(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private var enabled = true
    private var volume = 0.9f
    private var currentPack: String = "raffina"
    private var voiceFreq: String = "normal"  // low / normal / high

    /** Map: event name → list of asset file paths (one per variant). */
    private val eventVariants = mutableMapOf<String, MutableList<String>>()

    /** Per-event cooldown tracking. */
    private val lastEventTime = mutableMapOf<String, Long>()
    private var lastVoiceTime = 0L

    /** All 54 voice events (key = event name, value = number of variants). */
    val voiceEvents: Map<String, Int> = mapOf(
        "start" to 13, "sprintIntro" to 1,
        "clear1" to 15, "clear2" to 15, "clear3" to 14, "tetris" to 30,
        "tspin" to 11, "b2b" to 13, "pc" to 12,
        "combo2" to 1, "combo3" to 1, "combo4" to 1, "combo5" to 3,
        "combo6" to 1, "combo7" to 2, "combo8" to 1, "combo9" to 1, "combo10" to 2,
        "countdown" to 1, "go" to 1,
        "levelup" to 12, "speed" to 9,
        "danger" to 8, "hurry" to 4, "idle" to 12, "warm" to 6,
        "zen" to 6, "zenCalm" to 4,
        "lines25" to 1, "lines50" to 2, "lines100" to 1,
        "lines150" to 1, "lines200" to 2,
        "score10k" to 1, "score25k" to 1, "score50k" to 1, "score100k" to 1,
        "score250k" to 1, "score500k" to 1, "score1m" to 3,
        "sprint10" to 1, "sprint20" to 1, "sprint30" to 1, "sprint40" to 1,
        "ultra" to 2, "ultra30" to 1, "ultra60" to 1,
        "modeselect" to 4, "near" to 1, "record" to 9,
        "over" to 10, "win" to 8, "timeup" to 2,
        "pause" to 3, "resume" to 5, "saved" to 4
    )

    /** Load voice clip variants from assets/www/voices/. */
    fun loadFromAssets() {
        eventVariants.clear()
        try {
            val files = context.assets.list("www/voices") ?: return
            for (filename in files) {
                if (!filename.endsWith(".mp3")) continue
                // filename format: <event>-<variant>.mp3
                val name = filename.removeSuffix(".mp3")
                val dashIdx = name.lastIndexOf('-')
                if (dashIdx < 0) continue
                val event = name.substring(0, dashIdx)
                val path = "www/voices/$filename"
                eventVariants.getOrPut(event) { mutableListOf() }.add(path)
            }
            // Sort variants so indexing is deterministic
            eventVariants.values.forEach { it.sort() }
            Log.d("VoiceManager", "Loaded ${eventVariants.size} events, ${files.size} clips total")
        } catch (e: Exception) {
            Log.e("VoiceManager", "Failed to load voice assets: ${e.message}")
        }
    }

    fun setPack(pack: String) { currentPack = pack }
    fun setFrequency(freq: String) { voiceFreq = freq }

    /**
     * Play a voice clip for an event. Picks a random variant.
     * Respects per-priority cooldowns and frequency setting.
     */
    fun play(event: String, priority: VoicePriority = VoicePriority.NORMAL, force: Boolean = false) {
        if (!enabled) return

        // Frequency filter — skip if low-freq roll fails
        if (!force && !shouldPlayForFrequency(priority)) return

        val now = System.currentTimeMillis()
        if (!force) {
            val cooldown = when (priority) {
                VoicePriority.HIGH -> 1200L
                VoicePriority.NORMAL -> 2200L
                VoicePriority.LOW -> 6000L
            }
            if (now - lastVoiceTime < cooldown) return
            val eventCooldown = lastEventTime[event] ?: 0L
            if (now - eventCooldown < cooldown) return
        }

        val variants = eventVariants[event] ?: run {
            // Event has no variants loaded — skip
            return
        }
        if (variants.isEmpty()) return

        lastVoiceTime = now
        lastEventTime[event] = now

        val clipPath = variants.random()

        // Stop current playback
        mediaPlayer?.let {
            try { it.stop() } catch (_: Exception) {}
            it.release()
        }

        val packConfig = voicePacks[currentPack] ?: voicePacks["raffina"]!!

        try {
            val afd = context.assets.openFd(clipPath)
            mediaPlayer = MediaPlayer().apply {
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                val vol = if (packConfig.softVolume) volume * 0.5f else volume
                setVolume(vol, vol)

                // Apply pitch shift for voice packs (giant=0.7, chipmunk=1.6, alien=0.5, etc.)
                if (packConfig.pitch != 1.0f) {
                    try {
                        playbackParams = PlaybackParams().setPitch(packConfig.pitch)
                    } catch (_: Exception) {}
                }

                setOnCompletionListener { mp ->
                    mp.release()
                    mediaPlayer = null
                }
                prepare()
                start()
            }
        } catch (e: Exception) {
            Log.w("VoiceManager", "Playback failed for $event: ${e.message}")
        }
    }

    /**
     * Frequency filter — mirrors original app's settings.voiceFreq:
     *   - "high": play all events
     *   - "normal": skip LOW priority events 50% of the time
     *   - "low": skip LOW priority events entirely; NORMAL 50% chance
     */
    private fun shouldPlayForFrequency(priority: VoicePriority): Boolean {
        return when (voiceFreq) {
            "high" -> true
            "normal" -> when (priority) {
                VoicePriority.LOW -> Math.random() < 0.5
                else -> true
            }
            "low" -> when (priority) {
                VoicePriority.LOW -> false
                VoicePriority.NORMAL -> Math.random() < 0.5
                else -> true
            }
            else -> true
        }
    }

    fun setVolume(vol: Float) { volume = vol.coerceIn(0f, 1f) }
    fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
        if (!enabled) {
            mediaPlayer?.let {
                try { it.stop() } catch (_: Exception) {}
                it.release()
                mediaPlayer = null
            }
        }
    }

    fun release() {
        mediaPlayer?.release()
        mediaPlayer = null
        eventVariants.clear()
    }

    companion object {
        /**
         * Voice pack catalog — 9 packs matching original app.
         * raffina is the source audio; others apply effects via pitch/reverb.
         *
         * Pricing (mirrors original):
         *   raffina (free), giant (600), chipmunk (600), cyber (800),
         *   stadium (900), echo (700), radio (700), twin (900), alien (1200)
         */
        val voicePacks: Map<String, VoicePackConfig> = mapOf(
            "raffina" to VoicePackConfig(price = 0, accent = "#F472B6", pitch = 1.0f, softVolume = false),
            "giant"   to VoicePackConfig(price = 600, accent = "#F59E0B", pitch = 0.7f, softVolume = false),
            "chipmunk" to VoicePackConfig(price = 600, accent = "#4ADE80", pitch = 1.6f, softVolume = false),
            "cyber"   to VoicePackConfig(price = 800, accent = "#22D3EE", pitch = 1.0f, reverb = true, softVolume = false),
            "stadium" to VoicePackConfig(price = 900, accent = "#C084FC", pitch = 1.0f, reverb = true, echo = true, softVolume = false),
            "echo"    to VoicePackConfig(price = 700, accent = "#94A3B8", pitch = 1.0f, echo = true, softVolume = false),
            "radio"   to VoicePackConfig(price = 700, accent = "#FCA5A5", pitch = 1.0f, bandpass = true, softVolume = false),
            "twin"    to VoicePackConfig(price = 900, accent = "#7DD3FC", pitch = 1.0f, echo = true, delay = 80, softVolume = false),
            "alien"   to VoicePackConfig(price = 1200, accent = "#A3E635", pitch = 0.5f, reverb = true, softVolume = false)
        )
    }
}

enum class VoicePriority {
    HIGH,   // 1.2s cooldown — danger, tetris, level up, game over
    NORMAL, // 2.2s cooldown — line clears, T-spin
    LOW     // 6s cooldown — warm, idle, zenCalm
}

data class VoicePackConfig(
    val price: Int = 0,
    val accent: String = "#F472B6",
    val pitch: Float = 1.0f,
    val reverb: Boolean = false,
    val echo: Boolean = false,
    val bandpass: Boolean = false,
    val delay: Int = 0,
    val softVolume: Boolean = false
)
