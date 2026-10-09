package com.example.monetization

import android.content.Context
import android.util.Log
import com.inmobi.sdk.InMobiSdk
import com.inmobi.sdk.SdkInitializationListener
import com.startapp.sdk.adsbase.StartAppSDK
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Thread-safe SDK Initializer for VyaparSetu Ad Networks.
 * Configures InMobi SDK (Primary) and Start.io SDK (Automatic Fallback).
 */
object AdSdkInitializer {

    private const val TAG = "AdSdkInitializer"

    // Hardcoded InMobi & Start.io Credentials
    const val INMOBI_ACCOUNT_ID = "693fa15bda374aa8a030eb4c16df45da"
    const val INMOBI_PLACEMENT_INTERSTITIAL = 10000829611L
    const val INMOBI_PLACEMENT_BANNER = 10000829612L
    const val INMOBI_PLACEMENT_REWARDED = 10000829613L
    const val INMOBI_PLACEMENT_NATIVE = 10000829625L

    const val STARTIO_APP_ID = "209447443"

    private val isInMobiInitialized = AtomicBoolean(false)
    private val isStartIoInitialized = AtomicBoolean(false)
    private val isInitializing = AtomicBoolean(false)

    fun isReady(): Boolean = isInMobiInitialized.get() || isStartIoInitialized.get()
    fun isInMobiReady(): Boolean = isInMobiInitialized.get()
    fun isStartIoReady(): Boolean = isStartIoInitialized.get()

    /**
     * Initializes both InMobi and Start.io monetization SDKs in a thread-safe, non-blocking manner.
     */
    fun initialize(context: Context, onComplete: ((Boolean) -> Unit)? = null) {
        val appContext = context.applicationContext

        if (isReady()) {
            Log.d(TAG, "Ad SDKs already initialized.")
            onComplete?.invoke(true)
            return
        }

        if (!isInitializing.compareAndSet(false, true)) {
            Log.d(TAG, "Ad SDK initialization already in progress.")
            return
        }

        // 1. Initialize Start.io SDK (Fallback Network)
        try {
            StartAppSDK.init(appContext, STARTIO_APP_ID, false)
            StartAppSDK.enableReturnAds(false)
            StartAppSDK.setTestAdsEnabled(com.example.BuildConfig.DEBUG)
            isStartIoInitialized.set(true)
            Log.i(TAG, "✓ Start.io SDK initialized successfully (App ID: $STARTIO_APP_ID)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Start.io SDK: ${e.message}", e)
        }

        // 2. Initialize InMobi SDK (Primary Network)
        try {
            InMobiSdk.setLogLevel(InMobiSdk.LogLevel.NONE)
            InMobiSdk.setLocationWithCityStateCountry("Delhi", "Delhi", "India")
            val consentJson = JSONObject().apply {
                put("gdpr_consent_available", true)
                put("gdpr", "0")
            }

            InMobiSdk.init(
                appContext,
                INMOBI_ACCOUNT_ID,
                consentJson,
                object : SdkInitializationListener {
                    override fun onInitializationComplete(error: java.lang.Error?) {
                        isInitializing.set(false)
                        if (error == null) {
                            isInMobiInitialized.set(true)
                            Log.i(TAG, "✓ InMobi SDK initialized successfully (Account: $INMOBI_ACCOUNT_ID)")
                            onComplete?.invoke(true)
                        } else {
                            Log.w(TAG, "InMobi SDK initialization error: ${error.message}. Fallback to Start.io ready.")
                            onComplete?.invoke(isStartIoInitialized.get())
                        }
                    }
                }
            )
        } catch (e: Exception) {
            isInitializing.set(false)
            Log.e(TAG, "Exception during InMobi SDK init: ${e.message}", e)
            onComplete?.invoke(isStartIoInitialized.get())
        }
    }
}
