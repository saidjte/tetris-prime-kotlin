package com.tetrisprime.kotlin

import android.app.Application
import android.content.Context

/**
 * Phase 0 — Application class for Tetris Prime Kotlin.
 *
 * Initializes:
 *   - AppContextHolder (so singletons like GiftCodes can access assets)
 *   - AdMobManager (mobile ads SDK initialization)
 *   - TtsBridge (text-to-speech for accessibility captions)
 *
 * Wired into the manifest via android:name=".TetrisApp".
 */
class TetrisApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // Make app context available to singletons that need to read assets
        // or SharedPreferences outside of an Activity scope.
        AppContextHolder.context = applicationContext

        // Initialize AdMob (MobileAds.initialize is async + safe to call early)
        try {
            AdMobManager.init(this)
        } catch (e: Exception) {
            android.util.Log.e("TetrisApp", "AdMob init failed: ${e.message}")
        }

        // Pre-warm TTS for captioned speech (lazy loads on first speak())
        try {
            TtsBridge.init(this)
        } catch (e: Exception) {
            android.util.Log.e("TetrisApp", "TTS init failed: ${e.message}")
        }
    }
}

/**
 * Holds the application context for singletons that need to access
 * assets/SharedPreferences but don't have an Activity reference.
 * Set by TetrisApp.onCreate().
 */
object AppContextHolder {
    @Volatile var context: Context? = null
}
