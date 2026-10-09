package com.example.monetization

import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.inmobi.ads.AdMetaInfo
import com.inmobi.ads.InMobiAdRequestStatus
import com.inmobi.ads.InMobiBanner
import com.inmobi.ads.InMobiInterstitial
import com.inmobi.ads.InMobiNative
import com.inmobi.ads.listeners.BannerAdEventListener
import com.inmobi.ads.listeners.InterstitialAdEventListener
import com.inmobi.ads.listeners.NativeAdEventListener
import com.startapp.sdk.ads.banner.Banner
import com.startapp.sdk.ads.banner.BannerListener
import com.startapp.sdk.ads.nativead.NativeAdDetails
import com.startapp.sdk.ads.nativead.NativeAdPreferences
import com.startapp.sdk.ads.nativead.StartAppNativeAd
import com.startapp.sdk.adsbase.Ad
import com.startapp.sdk.adsbase.StartAppAd
import com.startapp.sdk.adsbase.adlisteners.AdDisplayListener
import com.startapp.sdk.adsbase.adlisteners.AdEventListener
import com.startapp.sdk.adsbase.adlisteners.VideoListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

enum class AdNetwork {
    INMOBI,
    START_IO,
    NONE
}

sealed class AdShowResult {
    data class Shown(val network: AdNetwork) : AdShowResult()
    data class Failed(val reason: String) : AdShowResult()
}

data class UnifiedNativeAd(
    val title: String,
    val description: String,
    val ctaText: String,
    val iconUrl: String? = null,
    val network: AdNetwork,
    val onClick: (Context) -> Unit
)

/**
 * Async Fallback Engine for Monetization.
 * Implements a strict 5000ms timeout waterfall:
 * InMobi (Primary) -> Start.io (Automatic Fallback).
 */
object AdFallbackManager {

    private const val TAG = "AdFallbackManager"
    private const val AD_TIMEOUT_MS = 5000L

    // ==========================================
    // 1. INTERSTITIAL ADS (InMobi -> Start.io)
    // ==========================================

    suspend fun showInterstitialWithFallback(activity: Activity): AdShowResult = withContext(Dispatchers.Main) {
        // Ensure SDKs are initialized
        AdSdkInitializer.initialize(activity)

        // Attempt InMobi Interstitial with 5s timeout
        val inMobiResult = withTimeoutOrNull(AD_TIMEOUT_MS) {
            loadInMobiInterstitial(activity)
        }

        if (inMobiResult is AdShowResult.Shown) {
            Log.d(TAG, "✓ Interstitial served by Primary network (InMobi)")
            return@withContext inMobiResult
        }

        Log.w(TAG, "InMobi Interstitial timed out or failed. Falling back to Start.io...")

        // Fallback: Start.io Interstitial
        val startIoResult = withTimeoutOrNull(AD_TIMEOUT_MS) {
            loadStartIoInterstitial(activity)
        } ?: AdShowResult.Failed("Start.io Interstitial timed out")

        return@withContext startIoResult
    }

    private suspend fun loadInMobiInterstitial(activity: Activity): AdShowResult =
        suspendCancellableCoroutine { continuation ->
            var isResumed = false

            val interstitial = InMobiInterstitial(
                activity,
                AdSdkInitializer.INMOBI_PLACEMENT_INTERSTITIAL,
                object : InterstitialAdEventListener() {
                    override fun onAdLoadSucceeded(ad: InMobiInterstitial, info: AdMetaInfo) {
                        if (!isResumed && continuation.isActive) {
                            isResumed = true
                            try {
                                ad.show()
                                continuation.resume(AdShowResult.Shown(AdNetwork.INMOBI))
                            } catch (e: Exception) {
                                continuation.resume(AdShowResult.Failed("InMobi show failed: ${e.message}"))
                            }
                        }
                    }

                    override fun onAdLoadFailed(ad: InMobiInterstitial, status: InMobiAdRequestStatus) {
                        if (!isResumed && continuation.isActive) {
                            isResumed = true
                            continuation.resume(AdShowResult.Failed("InMobi load failed: ${status.message}"))
                        }
                    }

                    override fun onAdDisplayFailed(ad: InMobiInterstitial) {
                        Log.w(TAG, "InMobi ad display failed")
                    }
                }
            )

            continuation.invokeOnCancellation {
                // Cancel ongoing ad request if coroutine timed out
            }

            try {
                interstitial.load()
            } catch (e: Exception) {
                if (!isResumed && continuation.isActive) {
                    isResumed = true
                    continuation.resume(AdShowResult.Failed("InMobi load exception: ${e.message}"))
                }
            }
        }

    private suspend fun loadStartIoInterstitial(activity: Activity): AdShowResult =
        suspendCancellableCoroutine { continuation ->
            var isResumed = false
            val startAppAd = StartAppAd(activity)

            startAppAd.loadAd(StartAppAd.AdMode.AUTOMATIC, object : AdEventListener {
                override fun onReceiveAd(ad: Ad) {
                    if (!isResumed && continuation.isActive) {
                        isResumed = true
                        val shown = startAppAd.showAd(object : AdDisplayListener {
                            override fun adHidden(ad: Ad?) {}
                            override fun adDisplayed(ad: Ad?) {}
                            override fun adClicked(ad: Ad?) {}
                            override fun adNotDisplayed(ad: Ad?) {
                                Log.w(TAG, "Start.io ad not displayed")
                            }
                        })
                        if (shown) {
                            continuation.resume(AdShowResult.Shown(AdNetwork.START_IO))
                        } else {
                            continuation.resume(AdShowResult.Failed("Start.io failed to show"))
                        }
                    }
                }

                override fun onFailedToReceiveAd(ad: Ad?) {
                    if (!isResumed && continuation.isActive) {
                        isResumed = true
                        continuation.resume(AdShowResult.Failed("Start.io ad receive failed: ${ad?.errorMessage}"))
                    }
                }
            })
        }

    // ==========================================
    // 2. REWARDED VIDEO ADS (InMobi -> Start.io)
    // ==========================================

    suspend fun showRewardedWithFallback(
        activity: Activity,
        onRewardGranted: () -> Unit
    ): AdShowResult = withContext(Dispatchers.Main) {
        AdSdkInitializer.initialize(activity)

        // Attempt InMobi Rewarded with 5s timeout
        val inMobiResult = withTimeoutOrNull(AD_TIMEOUT_MS) {
            loadInMobiRewarded(activity, onRewardGranted)
        }

        if (inMobiResult is AdShowResult.Shown) {
            Log.d(TAG, "✓ Rewarded video served by Primary network (InMobi)")
            return@withContext inMobiResult
        }

        Log.w(TAG, "InMobi Rewarded timed out or failed. Falling back to Start.io...")

        // Fallback: Start.io Rewarded Video
        val startIoResult = withTimeoutOrNull(AD_TIMEOUT_MS) {
            loadStartIoRewarded(activity, onRewardGranted)
        } ?: AdShowResult.Failed("Start.io Rewarded ad timed out")

        return@withContext startIoResult
    }

    private suspend fun loadInMobiRewarded(
        activity: Activity,
        onRewardGranted: () -> Unit
    ): AdShowResult = suspendCancellableCoroutine { continuation ->
        var isResumed = false
        var rewardUnlocked = false

        val rewardedAd = InMobiInterstitial(
            activity,
            AdSdkInitializer.INMOBI_PLACEMENT_REWARDED,
            object : InterstitialAdEventListener() {
                override fun onAdLoadSucceeded(ad: InMobiInterstitial, info: AdMetaInfo) {
                    if (!isResumed && continuation.isActive) {
                        isResumed = true
                        try {
                            ad.show()
                            continuation.resume(AdShowResult.Shown(AdNetwork.INMOBI))
                        } catch (e: Exception) {
                            continuation.resume(AdShowResult.Failed("InMobi show error: ${e.message}"))
                        }
                    }
                }

                override fun onAdLoadFailed(ad: InMobiInterstitial, status: InMobiAdRequestStatus) {
                    if (!isResumed && continuation.isActive) {
                        isResumed = true
                        continuation.resume(AdShowResult.Failed("InMobi Rewarded load failed: ${status.message}"))
                    }
                }

                override fun onRewardsUnlocked(ad: InMobiInterstitial, rewards: MutableMap<Any, Any>?) {
                    super.onRewardsUnlocked(ad, rewards)
                    if (!rewardUnlocked) {
                        rewardUnlocked = true
                        Log.i(TAG, "✓ InMobi Rewards unlocked callback received")
                        onRewardGranted()
                    }
                }

                override fun onAdDismissed(ad: InMobiInterstitial) {
                    super.onAdDismissed(ad)
                    // Ensure reward is delivered if completed
                    if (!rewardUnlocked) {
                        rewardUnlocked = true
                        onRewardGranted()
                    }
                }
            }
        )

        try {
            rewardedAd.load()
        } catch (e: Exception) {
            if (!isResumed && continuation.isActive) {
                isResumed = true
                continuation.resume(AdShowResult.Failed("InMobi Rewarded exception: ${e.message}"))
            }
        }
    }

    private suspend fun loadStartIoRewarded(
        activity: Activity,
        onRewardGranted: () -> Unit
    ): AdShowResult = suspendCancellableCoroutine { continuation ->
        var isResumed = false
        val startAppAd = StartAppAd(activity)
        var rewardDelivered = false

        startAppAd.setVideoListener(object : VideoListener {
            override fun onVideoCompleted() {
                if (!rewardDelivered) {
                    rewardDelivered = true
                    Log.i(TAG, "✓ Start.io Video completed. Unlocking user reward credits.")
                    onRewardGranted()
                }
            }
        })

        startAppAd.loadAd(StartAppAd.AdMode.REWARDED_VIDEO, object : AdEventListener {
            override fun onReceiveAd(ad: Ad) {
                if (!isResumed && continuation.isActive) {
                    isResumed = true
                    val shown = startAppAd.showAd()
                    if (shown) {
                        continuation.resume(AdShowResult.Shown(AdNetwork.START_IO))
                    } else {
                        continuation.resume(AdShowResult.Failed("Start.io Rewarded show failed"))
                    }
                }
            }

            override fun onFailedToReceiveAd(ad: Ad?) {
                if (!isResumed && continuation.isActive) {
                    isResumed = true
                    continuation.resume(AdShowResult.Failed("Start.io Rewarded failed to receive ad"))
                }
            }
        })
    }

    // ==========================================
    // 3. STICKY BANNER AD CONTAINER
    // ==========================================

    private fun findActivity(context: Context): Activity? {
        var ctx = context
        while (ctx is android.content.ContextWrapper) {
            if (ctx is Activity) return ctx
            ctx = ctx.baseContext
        }
        return null
    }

    fun createBannerContainerView(
        context: Context,
        onAdLoaded: (AdNetwork) -> Unit,
        onAdFailed: (String) -> Unit
    ): View {
        AdSdkInitializer.initialize(context)
        val activityContext = findActivity(context) ?: context

        val container = FrameLayout(context).apply {
            id = com.example.R.id.ad_container_layout
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            // Ensure any child views added by third-party ad SDKs have valid compiled resource IDs
            // to prevent AssetManager "No package ID ff found for resource ID 0xffffffff"
            setOnHierarchyChangeListener(object : ViewGroup.OnHierarchyChangeListener {
                override fun onChildViewAdded(parent: View?, child: View?) {
                    if (child != null && child.id == View.NO_ID) {
                        child.id = com.example.R.id.inmobi_banner_view
                    }
                    if (child is ViewGroup) {
                        for (i in 0 until child.childCount) {
                            val gc = child.getChildAt(i)
                            if (gc != null && gc.id == View.NO_ID) {
                                gc.id = com.example.R.id.inmobi_banner_view
                            }
                        }
                    }
                }
                override fun onChildViewRemoved(parent: View?, child: View?) {}
            })
        }

        // Try InMobi Banner (320x50 standard banner)
        try {
            val inMobiBanner = InMobiBanner(activityContext, AdSdkInitializer.INMOBI_PLACEMENT_BANNER).apply {
                id = com.example.R.id.inmobi_banner_view
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    gravity = android.view.Gravity.CENTER
                }
                setBannerSize(320, 50)
                setKeywords("business, retail, shop, merchant, vyapar, payments, finance, upi")
                setEnableAutoRefresh(true)
                setRefreshInterval(30)
            }

            inMobiBanner.setListener(object : BannerAdEventListener() {
                override fun onAdLoadSucceeded(ad: InMobiBanner, info: AdMetaInfo) {
                    Log.d(TAG, "✓ InMobi Banner loaded successfully")
                    onAdLoaded(AdNetwork.INMOBI)
                }

                override fun onAdLoadFailed(ad: InMobiBanner, status: InMobiAdRequestStatus) {
                    Log.w(TAG, "InMobi Banner failed: ${status.message}. Switching container to Start.io...")
                    container.post {
                        container.removeAllViews()
                        // Fallback to Start.io Banner
                        try {
                            val startIoBanner = Banner(activityContext, object : BannerListener {
                                override fun onReceiveAd(bannerView: View) {
                                    Log.d(TAG, "✓ Start.io Fallback Banner loaded")
                                    onAdLoaded(AdNetwork.START_IO)
                                }

                                override fun onFailedToReceiveAd(bannerView: View) {
                                    Log.w(TAG, "Start.io Banner returned no fill or error.")
                                    onAdFailed("Both InMobi and Start.io banner ads failed to load.")
                                }

                                override fun onClick(bannerView: View) {}
                                override fun onImpression(bannerView: View) {}
                            }).apply {
                                id = com.example.R.id.startio_banner_view
                                layoutParams = FrameLayout.LayoutParams(
                                    FrameLayout.LayoutParams.MATCH_PARENT,
                                    FrameLayout.LayoutParams.WRAP_CONTENT
                                ).apply {
                                    gravity = android.view.Gravity.CENTER
                                }
                            }
                            container.addView(startIoBanner)
                        } catch (e: Exception) {
                            onAdFailed("Start.io banner init error: ${e.message}")
                        }
                    }
                }
            })

            container.addView(inMobiBanner)
            inMobiBanner.load()
        } catch (e: Exception) {
            Log.e(TAG, "InMobi banner creation error: ${e.message}, falling back to Start.io", e)
            try {
                val startIoBanner = Banner(activityContext).apply {
                    id = com.example.R.id.startio_banner_view
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        gravity = android.view.Gravity.CENTER
                    }
                }
                container.addView(startIoBanner)
                onAdLoaded(AdNetwork.START_IO)
            } catch (err: Exception) {
                onAdFailed("Failed to initialize banner ad: ${err.message}")
            }
        }

        return container
    }

    // ==========================================
    // 4. NATIVE ADS FEED (InMobi -> Start.io)
    // ==========================================

    suspend fun loadNativeAd(context: Context): UnifiedNativeAd? = withContext(Dispatchers.Main) {
        AdSdkInitializer.initialize(context)

        // 1. Try InMobi Native
        val inMobiNative = withTimeoutOrNull(AD_TIMEOUT_MS) {
            loadInMobiNative(context)
        }

        if (inMobiNative != null) {
            Log.d(TAG, "✓ InMobi Native Ad loaded")
            return@withContext inMobiNative
        }

        Log.w(TAG, "InMobi Native timed out or failed. Falling back to Start.io Native...")

        // 2. Fallback: Start.io Native
        val startIoNative = withTimeoutOrNull(AD_TIMEOUT_MS) {
            loadStartIoNative(context)
        }

        return@withContext startIoNative
    }

    private suspend fun loadInMobiNative(context: Context): UnifiedNativeAd? =
        suspendCancellableCoroutine { continuation ->
            var isResumed = false

            val nativeAd = InMobiNative(
                context,
                AdSdkInitializer.INMOBI_PLACEMENT_NATIVE,
                object : NativeAdEventListener() {
                    override fun onAdLoadSucceeded(ad: InMobiNative, info: AdMetaInfo) {
                        if (!isResumed && continuation.isActive) {
                            isResumed = true
                            val unified = UnifiedNativeAd(
                                title = ad.adTitle ?: "Vyapar Partner Deal",
                                description = ad.adDescription ?: "Special business offer for Indian merchants.",
                                ctaText = ad.adCtaText ?: "Learn More",
                                iconUrl = ad.adIconUrl,
                                network = AdNetwork.INMOBI,
                                onClick = {
                                    ad.reportAdClickAndOpenLandingPage()
                                }
                            )
                            continuation.resume(unified)
                        }
                    }

                    override fun onAdLoadFailed(ad: InMobiNative, status: InMobiAdRequestStatus) {
                        if (!isResumed && continuation.isActive) {
                            isResumed = true
                            continuation.resume(null)
                        }
                    }
                }
            )

            try {
                nativeAd.load()
            } catch (e: Exception) {
                if (!isResumed && continuation.isActive) {
                    isResumed = true
                    continuation.resume(null)
                }
            }
        }

    private suspend fun loadStartIoNative(context: Context): UnifiedNativeAd? =
        suspendCancellableCoroutine { continuation ->
            var isResumed = false
            val startAppNative = StartAppNativeAd(context)
            val preferences = NativeAdPreferences().apply {
                adsNumber = 1
                isAutoBitmapDownload = true
            }

            startAppNative.loadAd(preferences, object : AdEventListener {
                override fun onReceiveAd(ad: Ad) {
                    if (!isResumed && continuation.isActive) {
                        isResumed = true
                        val adsList = startAppNative.nativeAds
                        if (!adsList.isNullOrEmpty()) {
                            val detail: NativeAdDetails = adsList[0]
                            val unified = UnifiedNativeAd(
                                title = detail.title ?: "Vyapar Recommended",
                                description = detail.description ?: "Grow your retail shop with top merchant tools.",
                                ctaText = detail.callToAction ?: "Install Now",
                                iconUrl = detail.imageUrl,
                                network = AdNetwork.START_IO,
                                onClick = { clickCtx ->
                                    val interactionView = android.view.View(clickCtx)
                                    detail.registerViewForInteraction(interactionView)
                                    interactionView.performClick()
                                }
                            )
                            continuation.resume(unified)
                        } else {
                            continuation.resume(null)
                        }
                    }
                }

                override fun onFailedToReceiveAd(ad: Ad?) {
                    if (!isResumed && continuation.isActive) {
                        isResumed = true
                        continuation.resume(null)
                    }
                }
            })
        }
}
