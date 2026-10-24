package com.tetrisprime.kotlin

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * Phase 0 — Text-to-speech bridge mirroring the original WebView app's
 * `AndroidTTS` JavaScript interface.
 *
 * The original JS bundle has a `captions` setting and plays voice lines
 * via the bundled MP3 voice packs. The AndroidTTS bridge exists as a
 * fallback for captioned speech when voice packs aren't available.
 *
 * Kotlin uses this for accessibility — reading captions aloud when
 * `settings.captions` is enabled and `settings.voiceReal` is off.
 */
object TtsBridge : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    @Volatile private var ready = false
    private var pendingUtterance: String? = null
    private var currentLang = "ar"

    fun init(context: Context, lang: String = "ar") {
        currentLang = lang
        if (tts == null) {
            try {
                tts = TextToSpeech(context.applicationContext, this)
            } catch (e: Exception) {
                android.util.Log.e("TtsBridge", "init failed: ${e.message}")
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            ready = true
            setLanguage(currentLang)
            pendingUtterance?.let { speak(it); pendingUtterance = null }
        } else {
            ready = false
        }
    }

    fun setLanguage(lang: String) {
        currentLang = lang
        val locale = when (lang) {
            "ar" -> Locale("ar")
            "en" -> Locale.US
            "fr" -> Locale.FRANCE
            "es" -> Locale("es")
            "de" -> Locale.GERMANY
            "it" -> Locale.ITALY
            "ru" -> Locale("ru")
            "tr" -> Locale("tr")
            "fa" -> Locale("fa")
            "ur" -> Locale("ur")
            "hi" -> Locale("hi")
            "ko" -> Locale.KOREA
            "zh" -> Locale.SIMPLIFIED_CHINESE
            "nl" -> Locale("nl")
            else -> Locale.US
        }
        try { tts?.language = locale } catch (_: Exception) {}
    }

    /**
     * Speak the given text. If TTS isn't ready yet, queue it for when
     * initialization completes.
     */
    fun speak(text: String) {
        if (!ready) {
            pendingUtterance = text
            return
        }
        try {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "tetris_tts_${System.currentTimeMillis()}")
        } catch (e: Exception) {
            android.util.Log.e("TtsBridge", "speak failed: ${e.message}")
        }
    }

    fun stop() {
        try { tts?.stop() } catch (_: Exception) {}
    }

    fun release() {
        try {
            tts?.stop()
            tts?.shutdown()
            tts = null
            ready = false
        } catch (_: Exception) {}
    }
}
