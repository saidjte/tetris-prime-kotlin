package com.tetrisprime.kotlin

import android.app.Activity
import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Phase 0 — AdMob integration mirroring the original WebView app.
 *
 * The original Android app exposes an `AndroidAd` JavaScript interface with
 * six methods:
 *   - showBanner()
 *   - hideBanner()
 *   - showInterstitial()
 *   - showRewarded()
 *   - isInterstitialReady()
 *   - isRewardedReady()
 *
 * This singleton provides the same API for Kotlin callers, plus an
 * `onRewardEarned` callback so the ContinueSystem can grant the reward
 * when a rewarded ad completes.
 *
 * Ad unit IDs are copied verbatim from the original APK's strings.xml:
 *   - App ID:         ca-app-pub-1782722148244932~1950528375
 *   - Banner:         ca-app-pub-1782722148244932/7011283361
 *   - Interstitial:   ca-app-pub-1782722148244932/9042550332
 *   - Rewarded:       ca-app-pub-1782722148244932/3458302730
 *
 * In test/debug builds we use Google's official test ad unit IDs so we
 * don't violate AdMob policy by clicking real ads during development.
 */
object AdMobManager {

    // Real ad unit IDs (from original APK strings.xml)
    private const val APP_ID = "ca-app-pub-1782722148244932~1950528375"
    private const val BANNER_UNIT_ID = "ca-app-pub-1782722148244932/7011283361"
    private const val INTERSTITIAL_UNIT_ID = "ca-app-pub-1782722148244932/9042550332"
    private const val REWARDED_UNIT_ID = "ca-app-pub-1782722148244932/3458302730"

    // Google's official test ad unit IDs (used when BuildConfig.DEBUG)
    private const val TEST_BANNER = "ca-app-pub-3940256099942544/6300978111"
    private const val TEST_INTERSTITIAL = "ca-app-pub-3940256099942544/1033173712"
    private const val TEST_REWARDED = "ca-app-pub-3940256099942544/5224354917"

    private val initialized = AtomicBoolean(false)

    private var bannerAd: AdView? = null
    private var bannerHost: FrameLayout? = null
    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null
    private var currentActivity: Activity? = null

    /** Called once from Application.onCreate or first Activity.onCreate. */
    fun init(context: Context) {
        if (!initialized.compareAndSet(false, true)) return
        try {
            MobileAds.initialize(context) { /* init complete */ }
            // Pre-load interstitial + rewarded so they're ready when needed.
            // We need an Activity context for ad load calls, so defer preloading
            // to first showBanner/showInterstitial/showRewarded call from an Activity.
            currentActivity = null
        } catch (e: Exception) {
            android.util.Log.e("AdMobManager", "init failed: ${e.message}")
        }
    }

    private fun isDebug(): Boolean = try {
        com.tetrisprime.kotlin.BuildConfig.DEBUG
    } catch (e: Exception) { false }

    // ---------------- Banner ----------------

    /**
     * Show the banner ad at the bottom of the activity's content view.
     * Creates a FrameLayout overlay host that floats above all other content.
     * Also triggers preloading of interstitial/rewarded if not yet loaded.
     */
    fun showBanner(activity: Activity) {
        try {
            // Lazy preload interstitial/rewarded on first banner show
            if (interstitialAd == null) preloadInterstitial(activity)
            if (rewardedAd == null) preloadRewarded(activity)
            currentActivity = activity

            if (bannerAd != null) {
                bannerAd?.visibility = View.VISIBLE
                return
            }
            val adView = AdView(activity).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = if (isDebug()) TEST_BANNER else BANNER_UNIT_ID
            }
            val host = FrameLayout(activity).apply {
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.BOTTOM
                )
            }
            host.addView(adView)
            (activity.window.decorView as ViewGroup).addView(host)
            adView.loadAd(AdRequest.Builder().build())
            bannerAd = adView
            bannerHost = host
        } catch (e: Exception) {
            android.util.Log.e("AdMobManager", "showBanner failed: ${e.message}")
        }
    }

    fun hideBanner() {
        try {
            bannerAd?.visibility = View.GONE
        } catch (e: Exception) {}
    }

    // ---------------- Interstitial ----------------

    private fun preloadInterstitial(activity: Activity) {
        try {
            val unitId = if (isDebug()) TEST_INTERSTITIAL else INTERSTITIAL_UNIT_ID
            InterstitialAd.load(activity, unitId,
                AdRequest.Builder().build(),
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: InterstitialAd) {
                        interstitialAd = ad
                    }
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        interstitialAd = null
                    }
                })
        } catch (e: Exception) {}
    }

    fun isInterstitialReady(): Boolean = interstitialAd != null

    fun showInterstitial(activity: Activity, onClosed: () -> Unit = {}) {
        val ad = interstitialAd
        if (ad != null) {
            ad.fullScreenContentCallback = object : com.google.android.gms.ads.FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null
                    preloadInterstitial(activity)
                    onClosed()
                }
                override fun onAdFailedToShowFullScreenContent(p0: com.google.android.gms.ads.AdError) {
                    interstitialAd = null
                    preloadInterstitial(activity)
                    onClosed()
                }
            }
            ad.show(activity)
        } else {
            // Not ready — reload and notify
            preloadInterstitial(activity)
            onClosed()
        }
    }

    // ---------------- Rewarded ----------------

    private fun preloadRewarded(activity: Activity) {
        try {
            val unitId = if (isDebug()) TEST_REWARDED else REWARDED_UNIT_ID
            RewardedAd.load(activity, unitId,
                AdRequest.Builder().build(),
                object : RewardedAdLoadCallback() {
                    override fun onAdLoaded(ad: RewardedAd) {
                        rewardedAd = ad
                    }
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        rewardedAd = null
                    }
                })
        } catch (e: Exception) {}
    }

    fun isRewardedReady(): Boolean = rewardedAd != null

    /**
     * Show a rewarded ad. If the user completes viewing, [onRewardEarned] is
     * called (granting coins / continue). [onClosed] is always called after.
     */
    fun showRewarded(activity: Activity, onRewardEarned: () -> Unit, onClosed: () -> Unit = {}) {
        val ad = rewardedAd
        if (ad != null) {
            var earned = false
            ad.fullScreenContentCallback = object : com.google.android.gms.ads.FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    rewardedAd = null
                    preloadRewarded(activity)
                    if (earned) onRewardEarned()
                    onClosed()
                }
                override fun onAdFailedToShowFullScreenContent(p0: com.google.android.gms.ads.AdError) {
                    rewardedAd = null
                    preloadRewarded(activity)
                    onClosed()
                }
            }
            ad.show(activity) { rewardItem ->
                // User earned the reward
                earned = true
                val amount = rewardItem.amount
                android.util.Log.d("AdMobManager", "Reward earned: $amount")
            }
        } else {
            // Not ready — try to reload and notify (no reward)
            preloadRewarded(activity)
            onClosed()
        }
    }

    /** Called from Activity.onDestroy to release banner. */
    fun release() {
        try {
            bannerAd?.destroy()
            bannerHost?.let { (currentActivity?.window?.decorView as? ViewGroup)?.removeView(it) }
            bannerAd = null
            bannerHost = null
            currentActivity = null
        } catch (e: Exception) {}
    }
}
