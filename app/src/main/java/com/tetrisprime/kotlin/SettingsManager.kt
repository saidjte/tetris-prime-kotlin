package com.tetrisprime.kotlin

import android.content.Context
import android.content.SharedPreferences

/**
 * 18 user-configurable settings persisted to SharedPreferences.
 * Matches the original game's Settings data class.
 */
data class Settings(
    val lang: String = "auto",          // auto | ar | en | fr
    val music: Boolean = true,
    val musicVol: Float = 0.6f,
    val sfx: Boolean = true,
    val sfxVol: Float = 0.8f,
    val voice: Boolean = true,
    val voiceVol: Float = 0.9f,
    val voiceFreq: String = "normal",   // sparse | normal | dense
    val captions: Boolean = true,
    val fx: String = "high",            // off | low | medium | high | ultra | legendary
    val gestures: Boolean = true,
    val next5: Boolean = true,          // show 5 next pieces (true) vs 3 (false)
    val garbage: String = "off",        // off | withLines | always
    val battleLevel: String = "normal", // easy | normal | hard
    val gamepad: Boolean = true,
    val pieceSymbols: Boolean = false,
    val allowRotation: Boolean = false, // allow rotating in impossible directions
    val hideCtlButtons: Boolean = false,
    val performance: String = "auto"    // Phase 11: auto | ultra | high | balanced | eco
)

/**
 * Singleton loader/saver for settings.
 */
object SettingsManager {

    private const val PREFS_NAME = "tetris_prime_settings"
    private const val KEY_SETTINGS_JSON = "settings_json_v1"

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Load settings from disk (or defaults if none saved). */
    fun load(ctx: Context): Settings {
        val json = prefs(ctx).getString(KEY_SETTINGS_JSON, null) ?: return Settings()
        return try {
            parseJson(json)
        } catch (e: Exception) {
            Settings()
        }
    }

    /** Persist settings to disk. */
    fun save(ctx: Context, s: Settings) {
        prefs(ctx).edit().putString(KEY_SETTINGS_JSON, toJson(s)).apply()
    }

    // ---- Minimal JSON encoder/decoder (no external deps) ----

    private fun toJson(s: Settings): String {
        val sb = StringBuilder(256)
        sb.append('{')
        sb.append("\"lang\":\"").append(esc(s.lang)).append("\",")
        sb.append("\"music\":").append(s.music).append(',')
        sb.append("\"musicVol\":").append(s.musicVol).append(',')
        sb.append("\"sfx\":").append(s.sfx).append(',')
        sb.append("\"sfxVol\":").append(s.sfxVol).append(',')
        sb.append("\"voice\":").append(s.voice).append(',')
        sb.append("\"voiceVol\":").append(s.voiceVol).append(',')
        sb.append("\"voiceFreq\":\"").append(esc(s.voiceFreq)).append("\",")
        sb.append("\"captions\":").append(s.captions).append(',')
        sb.append("\"fx\":\"").append(esc(s.fx)).append("\",")
        sb.append("\"gestures\":").append(s.gestures).append(',')
        sb.append("\"next5\":").append(s.next5).append(',')
        sb.append("\"garbage\":\"").append(esc(s.garbage)).append("\",")
        sb.append("\"battleLevel\":\"").append(esc(s.battleLevel)).append("\",")
        sb.append("\"gamepad\":").append(s.gamepad).append(',')
        sb.append("\"pieceSymbols\":").append(s.pieceSymbols).append(',')
        sb.append("\"allowRotation\":").append(s.allowRotation).append(',')
        sb.append("\"hideCtlButtons\":").append(s.hideCtlButtons).append(',')
        sb.append("\"performance\":\"").append(esc(s.performance)).append('"')
        sb.append('}')
        return sb.toString()
    }

    private fun parseJson(json: String): Settings {
        // tiny key-value parser (assumes flat object of primitives)
        val map = HashMap<String, String>()
        val body = json.trim().removePrefix("{").removeSuffix("}")
        var i = 0
        while (i < body.length) {
            // skip comma/whitespace
            while (i < body.length && (body[i] == ',' || body[i].isWhitespace())) i++
            if (i >= body.length) break
            // expect "key"
            if (body[i] != '"') break
            val keyEnd = body.indexOf('"', i + 1)
            if (keyEnd < 0) break
            val key = body.substring(i + 1, keyEnd)
            i = keyEnd + 1
            // skip :
            while (i < body.length && (body[i] == ':' || body[i].isWhitespace())) i++
            if (i >= body.length) break
            val value: String = when (body[i]) {
                '"' -> {
                    val vEnd = body.indexOf('"', i + 1)
                    if (vEnd < 0) break
                    val v = body.substring(i + 1, vEnd)
                    i = vEnd + 1
                    v
                }
                't', 'f' -> {
                    val end = body.indexOfAny(charArrayOf(',', '}'), i).let { if (it < 0) body.length else it }
                    val v = body.substring(i, end).trim()
                    i = end
                    v
                }
                else -> {
                    val end = body.indexOfAny(charArrayOf(',', '}'), i).let { if (it < 0) body.length else it }
                    val v = body.substring(i, end).trim()
                    i = end
                    v
                }
            }
            map[key] = value
        }
        return Settings(
            lang = map["lang"] ?: "auto",
            music = map["music"]?.toBooleanStrictOrNull() ?: true,
            musicVol = map["musicVol"]?.toFloatOrNull() ?: 0.6f,
            sfx = map["sfx"]?.toBooleanStrictOrNull() ?: true,
            sfxVol = map["sfxVol"]?.toFloatOrNull() ?: 0.8f,
            voice = map["voice"]?.toBooleanStrictOrNull() ?: true,
            voiceVol = map["voiceVol"]?.toFloatOrNull() ?: 0.9f,
            voiceFreq = map["voiceFreq"] ?: "normal",
            captions = map["captions"]?.toBooleanStrictOrNull() ?: true,
            fx = map["fx"] ?: "high",
            gestures = map["gestures"]?.toBooleanStrictOrNull() ?: true,
            next5 = map["next5"]?.toBooleanStrictOrNull() ?: true,
            garbage = map["garbage"] ?: "off",
            battleLevel = map["battleLevel"] ?: "normal",
            gamepad = map["gamepad"]?.toBooleanStrictOrNull() ?: true,
            pieceSymbols = map["pieceSymbols"]?.toBooleanStrictOrNull() ?: false,
            allowRotation = map["allowRotation"]?.toBooleanStrictOrNull() ?: false,
            hideCtlButtons = map["hideCtlButtons"]?.toBooleanStrictOrNull() ?: false,
            performance = map["performance"] ?: "auto"
        )
    }

    private fun esc(s: String): String = s.replace("\\", "\\\\").replace("\"", "\\\"")
}
