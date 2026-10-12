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
import kotlinx.coroutines.CoroutineExceptionHandler
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
 * Implements a strict, stable 7000ms timeout waterfall:
 * Start.io (Primary Real Ads) -> InMobi (Automatic Fallback with verified placement IDs).
 */
object AdFallbackManager {

    private const val TAG = "AdFallbackManager"
    // Stable 7-second loading window for real ad auctions & high-fill rendering
    private const val AD_TIMEOUT_MS = 7000L

    private val adExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e(TAG, "Ad coroutine exception handled gracefully: ${throwable.message}", throwable)
    }

    // ==========================================
    // 1. INTERSTITIAL ADS (Start.io -> InMobi)
    // ==========================================

    suspend fun showInterstitialWithFallback(activity: Activity): AdShowResult = withContext(Dispatchers.Main + adExceptionHandler) {
        try {
            // Ensure SDKs are initialized
            AdSdkInitializer.initialize(activity)

            // Attempt Start.io Interstitial first (Primary Real Ad Network)
            val startIoResult = withTimeoutOrNull(AD_TIMEOUT_MS) {
                loadStartIoInterstitial(activity)
            }

            if (startIoResult is AdShowResult.Shown) {
                Log.d(TAG, "✓ Interstitial served by Primary real ad network (Start.io)")
                return@withContext startIoResult
            }

            Log.w(TAG, "Start.io Interstitial timed out or failed. Falling back to InMobi...")

            // Fallback: InMobi Interstitial (7-second timeout window)
            val inMobiResult = withTimeoutOrNull(AD_TIMEOUT_MS) {
                loadInMobiInterstitial(activity)
            } ?: AdShowResult.Failed("Both Start.io and InMobi Interstitial failed to load within window")

            return@withContext inMobiResult
        } catch (e: Exception) {
            Log.e(TAG, "Error during interstitial waterfall: ${e.message}", e)
            AdShowResult.Failed("Interstitial exception: ${e.message}")
        }
    }

    private suspend fun loadInMobiInterstitial(activity: Activity): AdShowResult =
        suspendCancellableCoroutine { continuation ->
            var isResumed = false

            try {
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
            try {
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
            } catch (e: Exception) {
                if (!isResumed && continuation.isActive) {
                    isResumed = true
                    continuation.resume(AdShowResult.Failed("Start.io load exception: ${e.message}"))
                }
            }
        }

    // ==========================================
    // 2. REWARDED VIDEO ADS (Start.io -> InMobi)
    // ==========================================

    suspend fun showRewardedWithFallback(
        activity: Activity,
        onRewardGranted: () -> Unit
    ): AdShowResult = withContext(Dispatchers.Main + adExceptionHandler) {
        try {
            AdSdkInitializer.initialize(activity)

            // Attempt Start.io Rewarded Video first (Primary Real Ad Network)
            val startIoResult = withTimeoutOrNull(AD_TIMEOUT_MS) {
                loadStartIoRewarded(activity, onRewardGranted)
            }

            if (startIoResult is AdShowResult.Shown) {
                Log.d(TAG, "✓ Rewarded video served by Primary real ad network (Start.io)")
                return@withContext startIoResult
            }

            Log.w(TAG, "Start.io Rewarded timed out or failed. Falling back to InMobi...")

            // Fallback: InMobi Rewarded (7-second timeout window)
            val inMobiResult = withTimeoutOrNull(AD_TIMEOUT_MS) {
                loadInMobiRewarded(activity, onRewardGranted)
            } ?: AdShowResult.Failed("Both Start.io and InMobi Rewarded ads failed")

            return@withContext inMobiResult
        } catch (e: Exception) {
            Log.e(TAG, "Error in rewarded video waterfall: ${e.message}", e)
            AdShowResult.Failed("Rewarded ad error: ${e.message}")
        }
    }

    private suspend fun loadInMobiRewarded(
        activity: Activity,
        onRewardGranted: () -> Unit
    ): AdShowResult = suspendCancellableCoroutine { continuation ->
        var isResumed = false
        var rewardUnlocked = false

        try {
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
                        if (!rewardUnlocked) {
                            rewardUnlocked = true
                            onRewardGranted()
                        }
                    }
                }
            )

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
        var rewardDelivered = false

        try {
            val startAppAd = StartAppAd(activity)

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
        } catch (e: Exception) {
            if (!isResumed && continuation.isActive) {
                isResumed = true
                continuation.resume(AdShowResult.Failed("Start.io Rewarded exception: ${e.message}"))
            }
        }
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
        }

        // Recursively ensures any child or nested views added by third-party ad SDKs (Start.io, InMobi)
        // have valid compiled application resource IDs, preventing AssetManager from logging
        // "No package ID ff found for resource ID 0xffffffff".
        fun recursivelySanitizeViewIds(view: View) {
            if (view.id == View.NO_ID) {
                view.id = com.example.R.id.startio_banner_view
            }
            if (view is ViewGroup) {
                view.setOnHierarchyChangeListener(object : ViewGroup.OnHierarchyChangeListener {
                    override fun onChildViewAdded(parent: View?, child: View?) {
                        child?.let { recursivelySanitizeViewIds(it) }
                    }
                    override fun onChildViewRemoved(parent: View?, child: View?) {}
                })
                for (i in 0 until view.childCount) {
                    val child = view.getChildAt(i)
                    if (child != null) {
                        recursivelySanitizeViewIds(child)
                    }
                }
            }
        }

        recursivelySanitizeViewIds(container)

        // Try Start.io Banner first (Primary Real Ads Network)
        try {
            val startIoBanner = Banner(activityContext, object : BannerListener {
                override fun onReceiveAd(bannerView: View) {
                    Log.d(TAG, "✓ Start.io Real Banner loaded successfully")
                    onAdLoaded(AdNetwork.START_IO)
                }

                override fun onFailedToReceiveAd(bannerView: View) {
                    Log.w(TAG, "Start.io Banner returned NO FILL. Waterfalling to InMobi...")
                    container.post {
                        loadInMobiBannerFallback(container, activityContext, onAdLoaded, onAdFailed, ::recursivelySanitizeViewIds)
                    }
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

            recursivelySanitizeViewIds(startIoBanner)
            container.addView(startIoBanner)
        } catch (e: Exception) {
            Log.e(TAG, "Start.io banner creation error: ${e.message}, falling back to InMobi", e)
            loadInMobiBannerFallback(container, activityContext, onAdLoaded, onAdFailed, ::recursivelySanitizeViewIds)
        }

        return container
    }

    private fun loadInMobiBannerFallback(
        container: FrameLayout,
        activityContext: Context,
        onAdLoaded: (AdNetwork) -> Unit,
        onAdFailed: (String) -> Unit,
        sanitizeIds: ((View) -> Unit)? = null
    ) {
        try {
            container.removeAllViews()
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

            sanitizeIds?.invoke(inMobiBanner)

            inMobiBanner.setListener(object : BannerAdEventListener() {
                override fun onAdLoadSucceeded(ad: InMobiBanner, info: AdMetaInfo) {
                    Log.d(TAG, "✓ InMobi Fallback Banner loaded successfully")
                    onAdLoaded(AdNetwork.INMOBI)
                }

                override fun onAdLoadFailed(ad: InMobiBanner, status: InMobiAdRequestStatus) {
                    Log.w(TAG, "InMobi Fallback Banner returned status: ${status.message}")
                    onAdFailed("Ad network returned no fill: ${status.message}")
                }
            })

            container.addView(inMobiBanner)
            inMobiBanner.load()
        } catch (e: Exception) {
            Log.e(TAG, "InMobi fallback banner creation error: ${e.message}", e)
            onAdFailed("Failed to initialize fallback banner: ${e.message}")
        }
    }

    // ==========================================
    // 4. NATIVE ADS FEED (Start.io -> InMobi)
    // ==========================================

    suspend fun loadNativeAd(context: Context): UnifiedNativeAd? = withContext(Dispatchers.Main + adExceptionHandler) {
        try {
            AdSdkInitializer.initialize(context)

            // 1. Try Start.io Native first (Primary Real Ads Network)
            val startIoNative = withTimeoutOrNull(AD_TIMEOUT_MS) {
                loadStartIoNative(context)
            }

            if (startIoNative != null) {
                Log.d(TAG, "✓ Start.io Real Native Ad loaded")
                return@withContext startIoNative
            }

            Log.w(TAG, "Start.io Native timed out or failed. Falling back to InMobi Native...")

            // 2. Fallback: InMobi Native (7-second timeout window)
            val inMobiNative = withTimeoutOrNull(AD_TIMEOUT_MS) {
                loadInMobiNative(context)
            }

            return@withContext inMobiNative
        } catch (e: Exception) {
            Log.e(TAG, "Error loading native ad: ${e.message}", e)
            null
        }
    }

    private suspend fun loadInMobiNative(context: Context): UnifiedNativeAd? =
        suspendCancellableCoroutine { continuation ->
            var isResumed = false

            try {
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
            try {
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
            } catch (e: Exception) {
                if (!isResumed && continuation.isActive) {
                    isResumed = true
                    continuation.resume(null)
                }
            }
        }
}
