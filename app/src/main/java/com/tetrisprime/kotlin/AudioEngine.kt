package com.tetrisprime.kotlin

import android.content.Context

/**
 * Phase 2 — Unified audio engine that wires the SFX, voice, and music
 * subsystems to every engine event.
 *
 * Mirrors the original WebView app's audio scheduling:
 *   - Idle voice after 30s of no input
 *   - Warm voice 25s after game start
 *   - Danger voice 8s after entering danger zone
 *   - Hurry voice when ultra < 30s
 *   - Pause/resume with timed voice cues
 *   - Score milestones (10k/25k/50k/100k/250k/500k/1m)
 *   - Line milestones (25/50/100/150/200)
 *   - Sprint line milestones (10/20/30/40)
 *   - Ultra time warnings (60s, 30s remaining)
 *   - Zen calm at 70s
 *   - Near-record detection
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
        val voiceFreq: String = "normal",
        val musicEnabled: Boolean = true,
        val musicVolume: Float = 0.6f,
        val captionsEnabled: Boolean = false
    )

    private var settings = AudioSettings()

    // State tracking for milestone events (avoid repeating)
    private var scoreMilestoneIdx = -1
    private var lineMilestoneIdx = -1
    private var sprintMilestoneIdx = -1
    private var ultra60Announced = false
    private var ultra30Announced = false
    private var dangerAnnounced = false
    private var warmAnnounced = false
    private var zenCalmAnnounced = false
    private var idleAnnounced = false
    private var gameStartTimeMs = 0L
    private var lastInputTimeMs = 0L
    private var currentMode: GameMode? = null

    /** Score milestone thresholds (mirrors original: lA=[1e4,25e3,5e4,1e5,25e4,5e5,1e6]). */
    private val SCORE_THRESHOLDS = longArrayOf(10_000L, 25_000L, 50_000L, 100_000L, 250_000L, 500_000L, 1_000_000L)
    private val SCORE_VOICE_KEYS = listOf("score10k", "score25k", "score50k", "score100k", "score250k", "score500k", "score1m")

    /** Line milestone thresholds (mirrors original: lL=[25,50,100,150,200]). */
    private val LINE_THRESHOLDS = intArrayOf(25, 50, 100, 150, 200)
    private val LINE_VOICE_KEYS = listOf("lines25", "lines50", "lines100", "lines150", "lines200")

    /** Sprint line milestones. */
    private val SPRINT_THRESHOLDS = intArrayOf(10, 20, 30, 40)
    private val SPRINT_VOICE_KEYS = listOf("sprint10", "sprint20", "sprint30", "sprint40")

    fun init() {
        if (initialized) return
        sfx.loadFromAssets()
        sfx.setVolume(settings.sfxVolume)
        sfx.setEnabled(settings.sfxEnabled)

        voice.loadFromAssets()
        voice.setVolume(settings.voiceVolume)
        voice.setEnabled(settings.voiceEnabled)
        voice.setFrequency(settings.voiceFreq)

        music.setVolume(settings.musicVolume)
        music.setEnabled(settings.musicEnabled)

        initialized = true
    }

    fun applySettings(newSettings: AudioSettings) {
        settings = newSettings
        sfx.setVolume(newSettings.sfxVolume)
        sfx.setEnabled(newSettings.sfxEnabled)
        voice.setVolume(newSettings.voiceVolume)
        voice.setEnabled(newSettings.voiceEnabled)
        voice.setFrequency(newSettings.voiceFreq)
        music.setVolume(newSettings.musicVolume)
        music.setEnabled(newSettings.musicEnabled)
    }

    // ============================================================
    // SFX events — short sound effects for discrete actions
    // ============================================================
    fun playMove() = sfx.play("move")
    fun playRotate() = sfx.play("rotate")
    fun playSoftDrop() = sfx.play("soft")
    fun playHardDrop() = sfx.play("hard")
    fun playLock() = sfx.play("lock")
    fun playHold() = sfx.play("hold")
    fun playClick() = sfx.play("click")
    fun playSave() = sfx.play("save")

    fun playClear(lines: Int) = when (lines) {
        1 -> sfx.play("clear1")
        2 -> sfx.play("clear2")
        3 -> sfx.play("clear3")
        4 -> sfx.play("tetris")
        else -> {}
    }

    fun playTSpin(lines: Int) = when (lines) {
        1 -> sfx.play("tspin")
        2 -> sfx.play("tspin2")
        else -> sfx.play("tspin3")
    }

    fun playLevelUp() = sfx.play("levelup")
    fun playDanger() = sfx.play("danger")
    fun playRecord() = sfx.play("record")
    fun playTick() = sfx.play("tick")
    fun playGo() = sfx.play("go")
    fun playGameOver() = sfx.play("over")

    // ============================================================
    // Voice events — commentary by raffina pack
    // ============================================================
    fun voiceStart() = voice.play("start", VoicePriority.HIGH, force = true)
    fun voiceSprintIntro() = voice.play("sprintIntro", VoicePriority.HIGH, force = true)
    fun voiceCountdown() = voice.play("countdown", VoicePriority.HIGH, force = true)

    fun voiceClear(lines: Int) {
        when (lines) {
            1 -> voice.play("clear1")
            2 -> voice.play("clear2")
            3 -> voice.play("clear3")
            4 -> voice.play("tetris", VoicePriority.HIGH)
        }
    }
    fun voiceTSpin(lines: Int) {
        when (lines) {
            1 -> voice.play("tspin", VoicePriority.HIGH)
            2 -> voice.play("tspin2", VoicePriority.HIGH)
            else -> voice.play("tspin3", VoicePriority.HIGH)
        }
    }
    fun voiceB2B() = voice.play("b2b")
    fun voicePerfectClear() = voice.play("pc", VoicePriority.HIGH)

    fun voiceCombo(combo: Int) {
        // Combo is 0-indexed in engine (combo=0 means first chain). Voice keys are 1-indexed.
        val key = when {
            combo >= 10 -> "combo10"
            combo >= 9 -> "combo9"
            combo >= 8 -> "combo8"
            combo >= 7 -> "combo7"
            combo >= 6 -> "combo6"
            combo >= 5 -> "combo5"
            combo >= 4 -> "combo4"
            combo >= 3 -> "combo3"
            combo >= 2 -> "combo2"
            else -> return
        }
        voice.play(key, if (combo >= 5) VoicePriority.HIGH else VoicePriority.NORMAL)
    }

    fun voiceLevelUp(level: Int) {
        if (level >= 15) voice.play("speed", VoicePriority.HIGH)
        else voice.play("levelup", VoicePriority.HIGH)
    }

    fun voiceDanger() = voice.play("danger", VoicePriority.HIGH)
    fun voiceHurry() = voice.play("hurry", VoicePriority.HIGH)
    fun voiceIdle() = voice.play("idle", VoicePriority.LOW)
    fun voiceWarm() = voice.play("warm", VoicePriority.LOW)
    fun voiceZen() = voice.play("zen", VoicePriority.LOW)
    fun voiceZenCalm() = voice.play("zenCalm", VoicePriority.LOW)

    fun voiceSaved() = voice.play("saved")
    fun voiceRecord() = voice.play("record", VoicePriority.HIGH)
    fun voiceNear() = voice.play("near", VoicePriority.NORMAL)
    fun voiceGameOver() = voice.play("over", VoicePriority.HIGH, force = true)
    fun voiceWin() = voice.play("win", VoicePriority.HIGH)
    fun voiceTimeUp() = voice.play("timeup", VoicePriority.HIGH)
    fun voicePause() = voice.play("pause")
    fun voiceResume() = voice.play("resume", force = true)
    fun voiceUltraStart() = voice.play("ultra", VoicePriority.HIGH)
    fun voiceUltra60() = voice.play("ultra60", VoicePriority.HIGH, force = true)
    fun voiceUltra30() = voice.play("ultra30", VoicePriority.HIGH, force = true)
    fun voiceModeSelect() = voice.play("modeselect", VoicePriority.NORMAL, force = true)

    // ============================================================
    // Milestone events — fire once per threshold crossing
    // ============================================================

    /** Score milestone (10k/25k/50k/100k/250k/500k/1m). */
    fun onScoreChange(score: Long) {
        var newIdx = -1
        for (i in SCORE_THRESHOLDS.indices) {
            if (score >= SCORE_THRESHOLDS[i]) newIdx = i
            else break
        }
        while (scoreMilestoneIdx < newIdx) {
            scoreMilestoneIdx++
            if (scoreMilestoneIdx < SCORE_VOICE_KEYS.size) {
                voice.play(SCORE_VOICE_KEYS[scoreMilestoneIdx], VoicePriority.NORMAL)
            }
        }
    }

    /** Cumulative line milestone (25/50/100/150/200). */
    fun onTotalLinesChange(totalLines: Int) {
        var newIdx = -1
        for (i in LINE_THRESHOLDS.indices) {
            if (totalLines >= LINE_THRESHOLDS[i]) newIdx = i
            else break
        }
        while (lineMilestoneIdx < newIdx) {
            lineMilestoneIdx++
            if (lineMilestoneIdx < LINE_VOICE_KEYS.size) {
                voice.play(LINE_VOICE_KEYS[lineMilestoneIdx], VoicePriority.NORMAL)
            }
        }
    }

    /** Sprint progress milestone (10/20/30/40 lines). */
    fun onSprintProgress(lines: Int) {
        var newIdx = -1
        for (i in SPRINT_THRESHOLDS.indices) {
            if (lines >= SPRINT_THRESHOLDS[i]) newIdx = i
            else break
        }
        while (sprintMilestoneIdx < newIdx) {
            sprintMilestoneIdx++
            if (sprintMilestoneIdx < SPRINT_VOICE_KEYS.size) {
                voice.play(SPRINT_VOICE_KEYS[sprintMilestoneIdx], VoicePriority.NORMAL)
            }
        }
    }

    /** Ultra time remaining (call every tick from game loop). */
    fun onUltraTimeRemaining(secondsRemaining: Int) {
        if (!ultra60Announced && secondsRemaining <= 60) {
            ultra60Announced = true
            voiceUltra60()
        }
        if (!ultra30Announced && secondsRemaining <= 30) {
            ultra30Announced = true
            voiceUltra30()
        }
    }

    // ============================================================
    // State-based voice cues (warm/idle/danger/zenCalm)
    // ============================================================

    /** Called when any input is received (resets idle timer). */
    fun onInput() {
        lastInputTimeMs = System.currentTimeMillis()
        idleAnnounced = false
    }

    /** Call every frame from the game loop.
     *  Triggers warm/idle/danger/zenCalm voices based on elapsed time. */
    fun onGameTick(elapsedMs: Long, isInDanger: Boolean) {
        val now = System.currentTimeMillis()

        // Warm state — 25s into the game
        if (!warmAnnounced && elapsedMs >= 25_000L) {
            warmAnnounced = true
            voiceWarm()
        }

        // Idle state — 30s since last input
        if (!idleAnnounced && now - lastInputTimeMs >= 30_000L) {
            idleAnnounced = true
            voiceIdle()
        }

        // Danger state — entered danger zone
        if (isInDanger && !dangerAnnounced) {
            dangerAnnounced = true
            voiceDanger()
        } else if (!isInDanger) {
            // Reset danger flag when out of danger
            dangerAnnounced = false
        }

        // Zen calm — 70s into zen mode
        if (currentMode == GameMode.ZEN && !zenCalmAnnounced && elapsedMs >= 70_000L) {
            zenCalmAnnounced = true
            voiceZenCalm()
        }
    }

    // ============================================================
    // Music control
    // ============================================================
    fun playMenuMusic() = music.play("menu")
    fun playGameMusic(mode: GameMode) {
        val track = when (mode) {
            GameMode.SPRINT, GameMode.ULTRA, GameMode.CHEESE, GameMode.BATTLE -> "drive"
            GameMode.ZEN -> "zen"
            else -> "tetris"
        }
        music.play(track)
    }
    fun stopMusic() = music.stop()
    fun pauseMusic() = music.pause()
    fun resumeMusic() = music.resume()

    // ============================================================
    // Game lifecycle hooks
    // ============================================================

    fun onGameStart(mode: GameMode) {
        currentMode = mode
        gameStartTimeMs = System.currentTimeMillis()
        lastInputTimeMs = gameStartTimeMs
        // Reset all milestone trackers
        scoreMilestoneIdx = -1
        lineMilestoneIdx = -1
        sprintMilestoneIdx = -1
        ultra60Announced = false
        ultra30Announced = false
        dangerAnnounced = false
        warmAnnounced = false
        zenCalmAnnounced = false
        idleAnnounced = false
        playGameMusic(mode)
        when (mode) {
            GameMode.SPRINT -> voiceSprintIntro()
            GameMode.ULTRA -> voiceUltraStart()
            GameMode.ZEN -> voiceZen()
            else -> voiceStart()
        }
    }

    fun onGamePause() {
        pauseMusic()
        voicePause()
    }

    fun onGameResume() {
        resumeMusic()
        voiceResume()
        lastInputTimeMs = System.currentTimeMillis() // reset idle on resume
    }

    fun onGameOver() {
        stopMusic()
        playGameOver()
        voiceGameOver()
    }

    fun onCountdownTick() = playTick()

    fun onCountdownEnd() {
        playGo()
        voiceStart()
    }

    /** Called from ModeSelectActivity.onResume to announce menu music. */
    fun onModeSelect() {
        playMenuMusic()
        voiceModeSelect()
    }

    fun release() {
        sfx.release()
        voice.release()
        music.release()
        initialized = false
    }
}
