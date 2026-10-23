package com.tetrisprime.kotlin

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.net.Uri
import java.io.File

/**
 * SFX (Sound Effects) system.
 * Plays short sound effects for game actions.
 *
 * 22 SFX samples matching the original game.
 */
class SfxManager(private val context: Context) {

    private val soundPool: SoundPool
    private val soundMap = mutableMapOf<String, Int>()
    private var loaded = false
    private var volume = 0.8f
    private var enabled = true

    /** All SFX sample names. */
    val sfxNames = listOf(
        "sfx_move", "sfx_rotate", "sfx_soft", "sfx_hard", "sfx_lock",
        "sfx_hold", "sfx_click", "sfx_deny",
        "sfx_clear1", "sfx_clear2", "sfx_clear3", "sfx_tetris",
        "sfx_tspin", "sfx_tspin2", "sfx_tspin3",
        "sfx_levelup", "sfx_danger", "sfx_save",
        "sfx_record", "sfx_tick", "sfx_go", "sfx_over"
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

        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            // Track loaded samples
        }
    }

    /** Load all SFX from assets/sfx/ directory. */
    fun loadFromAssets(assetsDir: File) {
        for (name in sfxNames) {
            val file = File(assetsDir, "$name.mp3")
            if (file.exists()) {
                try {
                    val soundId = soundPool.load(file.absolutePath, 1)
                    soundMap[name] = soundId
                } catch (e: Exception) {
                    // File not found — skip
                }
            }
        }
        loaded = true
    }

    /** Load SFX from raw resources (alternative). */
    fun loadFromResources() {
        // In a real app, load from res/raw/
        // For now, mark as loaded — sounds will be added as assets
        loaded = true
    }

    /** Play a sound effect by name. */
    fun play(name: String) {
        if (!enabled || !loaded) return
        val soundId = soundMap[name] ?: return
        soundPool.play(soundId, volume, volume, 1, 0, 1.0f)
    }

    /** Set SFX volume (0.0 to 1.0). */
    fun setVolume(vol: Float) {
        volume = vol.coerceIn(0f, 1f)
    }

    /** Enable/disable SFX. */
    fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
    }

    /** Release resources. */
    fun release() {
        soundPool.release()
        soundMap.clear()
        loaded = false
    }

    companion object {
        // Convenience: play from GameEngine events
        fun playForEvent(sfx: SfxManager, event: String) {
            when (event) {
                "move" -> sfx.play("sfx_move")
                "rotate" -> sfx.play("sfx_rotate")
                "soft" -> sfx.play("sfx_soft")
                "hard" -> sfx.play("sfx_hard")
                "lock" -> sfx.play("sfx_lock")
                "hold" -> sfx.play("sfx_hold")
                "click" -> sfx.play("sfx_click")
                "clear1" -> sfx.play("sfx_clear1")
                "clear2" -> sfx.play("sfx_clear2")
                "clear3" -> sfx.play("sfx_clear3")
                "tetris" -> sfx.play("sfx_tetris")
                "tspin" -> sfx.play("sfx_tspin")
                "tspin2" -> sfx.play("sfx_tspin2")
                "tspin3" -> sfx.play("sfx_tspin3")
                "levelup" -> sfx.play("sfx_levelup")
                "danger" -> sfx.play("sfx_danger")
                "saved" -> sfx.play("sfx_save")
                "record" -> sfx.play("sfx_record")
                "tick" -> sfx.play("sfx_tick")
                "go" -> sfx.play("sfx_go")
                "over" -> sfx.play("sfx_over")
            }
        }
    }
}
