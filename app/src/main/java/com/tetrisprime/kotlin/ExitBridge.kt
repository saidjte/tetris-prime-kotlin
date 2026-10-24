package com.tetrisprime.kotlin

import android.app.Activity

/**
 * Phase 0 — Exit bridge mirroring the original WebView app's
 * `AndroidExit` JavaScript interface.
 *
 * The original app's JS calls `AndroidExit.exitApp()` when the user
 * confirms exit from the in-game exit dialog. In Kotlin we provide
 * the same API for native UI (e.g. back-button double-press or
 * menu → exit).
 */
object ExitBridge {

    /**
     * Equivalent of AndroidExit.exitApp().
     * Finishes all activities in the task — effectively exits the app
     * without killing the process (Android recommended pattern).
     */
    fun exitApp(activity: Activity) {
        try {
            activity.finishAffinity()
        } catch (e: Exception) {
            // Fallback: just finish this activity
            try { activity.finish() } catch (_: Exception) {}
        }
    }
}
