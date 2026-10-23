package com.tetrisprime.kotlin

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.sin

/**
 * Music system — procedurally synthesized background music.
 * 3 tracks: menu (84 BPM), tetris (149 BPM), drive (118 BPM).
 *
 * Uses AudioTrack to generate real-time audio with oscillators.
 */
class MusicManager(private val context: Context) {

    private var audioTrack: AudioTrack? = null
    private var isPlaying = false
    private var currentTrack: String = "menu"
    private var volume = 0.6f
    private var enabled = true
    private var thread: Thread? = null

    /** Track BPM and sub-beats. */
    private val trackBpm = mapOf("menu" to 84, "tetris" to 149, "drive" to 118)
    private val trackSubBeats = mapOf("menu" to 4, "tetris" to 2, "drive" to 4)

    /** Chord progression (4 chords, 3 notes each). */
    private val chords = listOf(
        listOf(110.0, 130.81, 164.81),    // Am
        listOf(146.83, 174.61, 220.0),    // Dm
        listOf(130.81, 164.81, 196.0),    // C
        listOf(98.0, 123.47, 146.83)      // G
    )

    /** Melody sequence (frequency, duration in beats). */
    private val melody = listOf(
        440.0 to 0.5, 523.25 to 0.5, 587.33 to 1.0,
        523.25 to 0.5, 440.0 to 0.5, 392.0 to 1.0,
        349.23 to 0.5, 392.0 to 0.5, 440.0 to 1.0,
        523.25 to 0.5, 587.33 to 0.5, 659.25 to 1.0,
        587.33 to 0.5, 523.25 to 0.5, 440.0 to 1.0,
        392.0 to 0.5, 349.23 to 0.5, 329.63 to 1.0,
        349.23 to 0.5, 392.0 to 0.5, 440.0 to 0.5, 523.25 to 0.5,
        587.33 to 1.0, 523.25 to 1.0
    )

    /** Bass line (frequency, duration). */
    private val bass = listOf(
        55.0 to 1.0, 73.42 to 1.0, 65.41 to 1.0, 49.0 to 1.0
    )

    /** Start playing a track. */
    fun play(track: String) {
        if (!enabled) return
        if (currentTrack == track && isPlaying) return
        stop()
        currentTrack = track
        startPlayback()
    }

    /** Stop music. */
    fun stop() {
        isPlaying = false
        thread?.let {
            it.interrupt()
            try { it.join(100) } catch (e: Exception) {}
        }
        thread = null
        audioTrack?.let {
            try { it.stop() } catch (e: Exception) {}
            it.release()
        }
        audioTrack = null
    }

    /** Pause music (can resume). */
    fun pause() {
        isPlaying = false
        audioTrack?.let {
            try { it.pause() } catch (e: Exception) {}
        }
    }

    /** Resume music. */
    fun resume() {
        if (!enabled) return
        if (audioTrack != null && !isPlaying) {
            isPlaying = true
            audioTrack?.let {
                try { it.play() } catch (e: Exception) {}
            }
        }
    }

    /** Set music volume (0.0 to 1.0). */
    fun setVolume(vol: Float) {
        volume = vol.coerceIn(0f, 1f)
        audioTrack?.setVolume(volume)
    }

    /** Enable/disable music. */
    fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
        if (!enabled) stop()
    }

    private fun startPlayback() {
        val sampleRate = 44100
        val bufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        audioTrack?.setVolume(volume)
        isPlaying = true

        thread = Thread {
            val bpm = trackBpm[currentTrack] ?: 84
            val subBeats = trackSubBeats[currentTrack] ?: 4
            val stepSeconds = 60.0 / bpm / subBeats
            val stepSamples = (stepSeconds * sampleRate).toInt()

            val buffer = ShortArray(stepSamples)
            var stepIndex = 0

            audioTrack?.play()

            while (isPlaying && !Thread.currentThread().isInterrupted) {
                // Generate one step of audio
                generateStep(buffer, stepIndex, sampleRate)
                audioTrack?.write(buffer, 0, buffer.size)
                stepIndex++
            }
        }.also { it.start() }
    }

    /** Generate one step of audio (chord + melody + bass). */
    private fun generateStep(buffer: ShortArray, stepIndex: Int, sampleRate: Int) {
        val chordIdx = (stepIndex / 4) % chords.size
        val chord = chords[chordIdx]
        val melodyIdx = stepIndex % melody.size
        val bassIdx = (stepIndex / 2) % bass.size

        for (i in buffer.indices) {
            val t = i.toDouble() / sampleRate
            val stepT = (i.toDouble() / sampleRate)

            // Chord (soft pad)
            var sample = 0.0
            for (freq in chord) {
                sample += sin(2 * PI * freq * t) * 0.08
            }

            // Melody (lead)
            val (melodyFreq, _) = melody[melodyIdx]
            sample += sin(2 * PI * melodyFreq * t) * 0.15

            // Bass
            val (bassFreq, _) = bass[bassIdx]
            sample += sin(2 * PI * bassFreq * t) * 0.10

            // Envelope (attack/decay)
            val env = if (i < 100) i / 100.0 else 1.0 - (i.toDouble() / buffer.size) * 0.3

            val value = (sample * env * Short.MAX_VALUE * 0.5).toInt()
            buffer[i] = value.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
    }

    /** Release resources. */
    fun release() {
        stop()
    }
}
