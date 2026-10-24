package com.tetrisprime.kotlin

import android.content.Context

/**
 * Unified audio engine that manages SFX, voice, and music.
 * Connects to game engine events and plays appropriate sounds.
 */
class AudioEngine(private val context: Context) {

    val sfx = SfxManager(context)
    val voice = VoiceManager(context)
    val music = MusicManager(context)

    private var initialized = false

    /** Settings applied to all subsystems. */
    data class AudioSettings(
        val sfxEnabled: Boolean = true,
        val sfxVolume: Float = 0.8f,
        val voiceEnabled: Boolean = true,
        val voiceVolume: Float = 0.9f,
        val musicEnabled: Boolean = true,
        val musicVolume: Float = 0.6f,
        val voiceFreq: String = "normal" // normal, low, high
    )

    private var settings = AudioSettings()

    /** Initialize the audio engine. */
    fun init() {
        if (initialized) return

        sfx.loadFromResources()
        sfx.setVolume(settings.sfxVolume)
        sfx.setEnabled(settings.sfxEnabled)

        voice.setVolume(settings.voiceVolume)
        voice.setEnabled(settings.voiceEnabled)

        music.setVolume(settings.musicVolume)
        music.setEnabled(settings.musicEnabled)

        initialized = true
    }

    /** Apply new settings. */
    fun applySettings(newSettings: AudioSettings) {
        settings = newSettings
        sfx.setVolume(newSettings.sfxVolume)
        sfx.setEnabled(newSettings.sfxEnabled)
        voice.setVolume(newSettings.voiceVolume)
        voice.setEnabled(newSettings.voiceEnabled)
        music.setVolume(newSettings.musicVolume)
        music.setEnabled(newSettings.musicEnabled)
    }

    // === SFX events ===
    fun playMove() = sfx.play("sfx_move")
    fun playRotate() = sfx.play("sfx_rotate")
    fun playSoftDrop() = sfx.play("sfx_soft")
    fun playHardDrop() = sfx.play("sfx_hard")
    fun playLock() = sfx.play("sfx_lock")
    fun playHold() = sfx.play("sfx_hold")
    fun playClick() = sfx.play("sfx_click")
    fun playClear(lines: Int) = when (lines) {
        1 -> sfx.play("sfx_clear1")
        2 -> sfx.play("sfx_clear2")
        3 -> sfx.play("sfx_clear3")
        4 -> sfx.play("sfx_tetris")
        else -> {}
    }
    fun playTSpin(lines: Int) = when (lines) {
        1 -> sfx.play("sfx_tspin")
        2 -> sfx.play("sfx_tspin2")
        else -> sfx.play("sfx_tspin3")
    }
    fun playLevelUp() = sfx.play("sfx_levelup")
    fun playDanger() = sfx.play("sfx_danger")
    fun playSaved() = sfx.play("sfx_save")
    fun playRecord() = sfx.play("sfx_record")
    fun playTick() = sfx.play("sfx_tick")
    fun playGo() = sfx.play("sfx_go")
    fun playGameOver() = sfx.play("sfx_over")

    // === Voice events ===
    fun voiceStart() = voice.play("start", VoicePriority.HIGH, force = true)
    fun voiceCountdown() = voice.play("countdown", VoicePriority.HIGH, force = true)
    fun voiceClear(lines: Int) {
        when (lines) {
            1 -> voice.play("clear1")
            2 -> voice.play("clear2")
            3 -> voice.play("clear3")
            4 -> voice.play("tetris", VoicePriority.HIGH)
        }
    }
    fun voiceTSpin() = voice.play("tspin", VoicePriority.HIGH)
    fun voiceB2B() = voice.play("b2b")
    fun voicePerfectClear() = voice.play("pc", VoicePriority.HIGH)
    fun voiceCombo(combo: Int) {
        when {
            combo >= 10 -> voice.play("combo10", VoicePriority.HIGH)
            combo >= 8 -> voice.play("combo8")
            combo >= 7 -> voice.play("combo7")
            combo >= 6 -> voice.play("combo6")
            combo >= 5 -> voice.play("combo5")
            combo >= 4 -> voice.play("combo4")
            combo >= 3 -> voice.play("combo3")
            combo >= 2 -> voice.play("combo2")
        }
    }
    fun voiceLevelUp(level: Int) {
        if (level >= 15) voice.play("speed", VoicePriority.HIGH)
        else voice.play("levelup", VoicePriority.HIGH)
    }
    fun voiceDanger() = voice.play("danger", VoicePriority.HIGH)
    fun voiceSaved() = voice.play("saved")
    fun voiceRecord() = voice.play("record", VoicePriority.HIGH)
    fun voiceGameOver() = voice.play("over", VoicePriority.HIGH, force = true)
    fun voiceWin() = voice.play("win", VoicePriority.HIGH)
    fun voiceTimeUp() = voice.play("timeup", VoicePriority.HIGH)
    fun voiceHurry() = voice.play("hurry", VoicePriority.HIGH)
    fun voiceUltra60() = voice.play("ultra60", VoicePriority.HIGH)
    fun voiceUltra30() = voice.play("ultra30", VoicePriority.HIGH)
    fun voiceWarm() = voice.play("warm", VoicePriority.LOW)
    fun voicePause() = voice.play("pause")
    fun voiceResume() = voice.play("resume", force = true)

    // === Music control ===
    fun playMenuMusic() = music.play("menu")
    fun playGameMusic(mode: GameMode) {
        val track = when (mode) {
            GameMode.SPRINT, GameMode.ULTRA, GameMode.CHEESE, GameMode.BATTLE -> "drive"
            else -> "tetris"
        }
        music.play(track)
    }
    fun stopMusic() = music.stop()
    fun pauseMusic() = music.pause()
    fun resumeMusic() = music.resume()

    /** Handle game state change. */
    fun onGameStart(mode: GameMode) {
        playGameMusic(mode)
    }

    fun onGamePause() {
        pauseMusic()
        voicePause()
    }

    fun onGameResume() {
        resumeMusic()
        voiceResume()
    }

    fun onGameOver() {
        stopMusic()
        playGameOver()
        voiceGameOver()
    }

    fun onCountdownTick() {
        playTick()
    }

    fun onCountdownEnd() {
        playGo()
        voiceStart()
    }

    /** Release all audio resources. */
    fun release() {
        sfx.release()
        voice.release()
        music.release()
        initialized = false
    }
}
