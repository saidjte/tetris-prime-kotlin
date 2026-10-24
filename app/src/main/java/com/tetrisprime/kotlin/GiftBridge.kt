package com.tetrisprime.kotlin

import android.content.Context
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * Phase 0 — Gift code loader mirroring the original WebView app's
 * `AndroidGift` JavaScript interface.
 *
 * The original app fetches gift-codes.json from a public GitHub Gist URL.
 * When fetched, it parses the same JSON shape we bundle in
 * assets/www/gift-codes.json:
 *
 *   {
 *     "version": 4,
 *     "updated": "2026-10-01",
 *     "codes": [
 *       { "code": "WELCOME100", "coins": 100, "note": "..." },
 *       ...
 *     ]
 *   }
 *
 * Kotlin strategy:
 *   1. Bundled fallback: assets/www/gift-codes.json (always available)
 *   2. Optional remote refresh from a configurable URL (executed in
 *      background; if it fails we silently keep the bundled version).
 *
 * redeemedCodes are persisted in SharedPreferences so each code only pays
 * out once per device.
 */
object GiftBridge {

    private const val PREFS = "tetris_prime_gifts"
    private const val KEY_REDEEMED = "redeemed_codes"
    private const val DEFAULT_GIST_URL = "https://raw.githubusercontent.com/saidjte/tetris-prime-android/main/src/gift-codes.json"

    data class GiftCode(val code: String, val coins: Int, val note: String?)

    /** Callback signature matches AndroidGift.httpGet(requestId, url). */
    interface Callback {
        fun onResult(requestId: Int, codes: List<GiftCode>, source: String)
        fun onError(requestId: Int, message: String)
    }

    /**
     * Equivalent of AndroidGift.httpGet(requestId, url).
     * Fetches the JSON from the given URL (or DEFAULT_GIST_URL if null),
     * parses the codes, and invokes the callback on the main thread.
     *
     * Always falls back to the bundled assets/www/gift-codes.json if the
     * network request fails.
     */
    fun httpGet(context: Context, requestId: Int, url: String? = null, callback: Callback) {
        val targetUrl = url ?: DEFAULT_GIST_URL
        Thread {
            try {
                val codes = fetchFromNetwork(targetUrl)
                if (codes.isNotEmpty()) {
                    deliverSuccess(requestId, codes, "network", callback)
                    return@Thread
                }
            } catch (e: Exception) {
                android.util.Log.d("GiftBridge", "Network fetch failed, falling back to bundled: ${e.message}")
            }
            // Fallback to bundled
            try {
                val bundled = loadBundled(context)
                deliverSuccess(requestId, bundled, "bundled", callback)
            } catch (e: Exception) {
                deliverError(requestId, "Failed to load gift codes: ${e.message}", callback)
            }
        }.start()
    }

    private fun fetchFromNetwork(urlStr: String): List<GiftCode> {
        val url = URL(urlStr)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 5000
            readTimeout = 5000
            setRequestProperty("Accept", "application/json")
        }
        try {
            if (conn.responseCode != 200) return emptyList()
            val body = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
            return parseJson(body)
        } finally {
            conn.disconnect()
        }
    }

    private fun loadBundled(context: Context): List<GiftCode> {
        val json = context.assets.open("www/gift-codes.json").bufferedReader().use { it.readText() }
        return parseJson(json)
    }

    private fun parseJson(json: String): List<GiftCode> {
        val root = JSONObject(json)
        val arr = root.optJSONArray("codes") ?: return emptyList()
        val out = mutableListOf<GiftCode>()
        for (i in 0 until arr.length()) {
            val c = arr.getJSONObject(i)
            out.add(GiftCode(
                code = c.getString("code"),
                coins = c.getInt("coins"),
                note = c.optString("note", null)
            ))
        }
        return out
    }

    private fun deliverSuccess(requestId: Int, codes: List<GiftCode>, source: String, callback: Callback) {
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            callback.onResult(requestId, codes, source)
        }
    }

    private fun deliverError(requestId: Int, msg: String, callback: Callback) {
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            callback.onError(requestId, msg)
        }
    }

    // ---------------- Redeemed code tracking ----------------

    fun isRedeemed(context: Context, code: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val set = prefs.getStringSet(KEY_REDEEMED, emptySet()) ?: emptySet()
        return set.contains(code)
    }

    fun markRedeemed(context: Context, code: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val set = (prefs.getStringSet(KEY_REDEEMED, emptySet()) ?: emptySet()).toMutableSet()
        set.add(code)
        prefs.edit().putStringSet(KEY_REDEEMED, set).apply()
    }
}
