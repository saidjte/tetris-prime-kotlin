package com.tetrisprime.kotlin

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.PlaybackParams
import java.io.File

/**
 * Voice commentary system.
 * Plays voice clips for game events.
 *
 * 38 voice event types, 13 voice packs.
 */
class VoiceManager(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private var enabled = true
    private var volume = 0.9f
    private var currentPack: String = "raffina"
    private val voiceClips = mutableMapOf<String, File>()

    /** Voice event types and their clip counts. */
    val voiceEvents = mapOf(
        "start" to 13,
        "clear1" to 15,
        "clear2" to 15,
        "clear3" to 14,
        "tetris" to 30,
        "tspin" to 11,
        "b2b" to 13,
        "countdown" to 1,
        "warm" to 6,
        "zenCalm" to 4,
        "combo2" to 1,
        "combo3" to 1,
        "combo4" to 1,
        "combo5" to 3,
        "combo6" to 1,
        "combo7" to 2,
        "combo8" to 1,
        "combo10" to 2,
        "levelup" to 12,
        "danger" to 8,
        "saved" to 4,
        "pc" to 12,
        "score10k" to 1,
        "score25k" to 1,
        "score50k" to 1,
        "score100k" to 1,
        "score250k" to 1,
        "score500k" to 1,
        "score1m" to 3,
        "lines25" to 1,
        "lines50" to 2,
        "lines100" to 1,
        "lines150" to 1,
        "lines200" to 2,
        "record" to 9,
        "over" to 10,
        "win" to 8,
        "timeup" to 2,
        "hurry" to 4,
        "ultra" to 2,
        "ultra60" to 1,
        "ultra30" to 1,
        "pause" to 3,
        "resume" to 5,
        "idle" to 12,
        "speed" to 10,
        "modeselect" to 4
    )

    /** Voice packs catalog — see companion object. */
    // Accessible as VoiceManager.voicePacks (companion) or instance.voicePacks (delegated).

    // Event cooldown tracking
    private val lastEventTime = mutableMapOf<String, Long>()
    private var lastVoiceTime = 0L

    /** Load voice clips from assets/voices/ directory. */
    fun loadFromAssets(assetsDir: File, pack: String = "raffina") {
        currentPack = pack
        voiceClips.clear()
        val voicesDir = File(assetsDir, "voices")
        if (voicesDir.exists()) {
            voicesDir.listFiles()?.forEach { file ->
                if (file.name.endsWith(".mp3")) {
                    val key = file.nameWithoutExtension // e.g., "tetris-0"
                    voiceClips[key] = file
                }
            }
        }
    }

    /** Set the active voice pack. */
    fun setPack(pack: String) {
        currentPack = pack
    }

    /** Play a voice clip for an event. */
    fun play(event: String, priority: VoicePriority = VoicePriority.NORMAL, force: Boolean = false) {
        if (!enabled) return

        val now = System.currentTimeMillis()

        // Check cooldown
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

        lastVoiceTime = now
        lastEventTime[event] = now

        // Pick a random clip variant
        val count = voiceEvents[event] ?: return
        val variant = (0 until count).random()
        val clipKey = "$event-$variant"
        val clipFile = voiceClips[clipKey] ?: return

        // Stop current playback
        mediaPlayer?.let {
            try { it.stop() } catch (e: Exception) {}
            it.release()
        }

        // Apply voice pack effects
        val packConfig = voicePacks[currentPack] ?: voicePacks["raffina"]!!

        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(clipFile.absolutePath)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )

                val vol = if (packConfig.softVolume) volume * 0.5f else volume
                setVolume(vol, vol)

                // Apply pitch shift for voice packs
                if (packConfig.pitch != 1.0f) {
                    playbackParams = PlaybackParams().setPitch(packConfig.pitch)
                }

                setOnCompletionListener { mp ->
                    mp.release()
                    mediaPlayer = null
                }

                prepare()
                start()
            }
        } catch (e: Exception) {
            // Playback failed — skip
        }
    }

    /** Set voice volume (0.0 to 1.0). */
    fun setVolume(vol: Float) {
        volume = vol.coerceIn(0f, 1f)
    }

    /** Enable/disable voice. */
    fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
        if (!enabled) {
            mediaPlayer?.let {
                try { it.stop() } catch (e: Exception) {}
                it.release()
                mediaPlayer = null
            }
        }
    }

    /** Release resources. */
    fun release() {
        mediaPlayer?.release()
        mediaPlayer = null
        voiceClips.clear()
    }

    companion object {
        /** Voice pack catalog (static so StoreActivity can list without instantiating). */
        val voicePacks: Map<String, VoicePackConfig> = mapOf(
            "raffina" to VoicePackConfig(pitch = 1.0f, reverb = false, echo = false),
            "giant" to VoicePackConfig(pitch = 0.7f, reverb = false, echo = false),
            "chipmunk" to VoicePackConfig(pitch = 1.6f, reverb = false, echo = false),
            "cyber" to VoicePackConfig(pitch = 1.0f, reverb = true, echo = false),
            "stadium" to VoicePackConfig(pitch = 1.0f, reverb = true, echo = true),
            "echo" to VoicePackConfig(pitch = 1.0f, reverb = false, echo = true),
            "radio" to VoicePackConfig(pitch = 1.0f, reverb = false, echo = false, bandpass = true),
            "twin" to VoicePackConfig(pitch = 1.0f, reverb = false, echo = true, delay = 80),
            "alien" to VoicePackConfig(pitch = 0.5f, reverb = true, echo = false),
            "calm" to VoicePackConfig(pitch = 0.9f, reverb = true, echo = false, softVolume = true),
            "whisper" to VoicePackConfig(pitch = 1.0f, reverb = false, echo = true, softVolume = true),
            "hero" to VoicePackConfig(pitch = 0.85f, reverb = true, echo = false),
            "sage" to VoicePackConfig(pitch = 0.8f, reverb = true, echo = false)
        )
    }
}

/** Voice priority levels. */
enum class VoicePriority {
    HIGH,   // 1.2s cooldown (danger, tetris, etc.)
    NORMAL, // 2.2s cooldown (clear1-3, etc.)
    LOW     // 6s cooldown (warm, idle, etc.)
}

/** Voice pack configuration — defines audio effects per pack. */
data class VoicePackConfig(
    val pitch: Float = 1.0f,
    val reverb: Boolean = false,
    val echo: Boolean = false,
    val bandpass: Boolean = false,
    val delay: Int = 0,
    val softVolume: Boolean = false
)
