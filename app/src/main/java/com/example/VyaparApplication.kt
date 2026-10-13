package com.example

import android.app.Application
import android.content.Context
import android.util.Log
import com.example.monetization.AdSdkInitializer
import com.inmobi.sdk.InMobiSdk
import com.inmobi.sdk.SdkInitializationListener
import com.startapp.sdk.adsbase.StartAppSDK
import org.json.JSONObject

/**
 * Custom Application class for VyaparSetu.
 * Pre-configures environment settings, initializes InMobi and Start.io monetization SDKs
 * on the main application thread with real production ad parameters, and sets default error guards.
 */
open class VyaparApplication : Application() {

    companion object {
        private const val TAG = "VyaparApplication"
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        try {
            // Force FCM auto_init to false in SharedPreferences synchronously before
            // FirebaseInitProvider or FirebaseMessaging components can instantiate.
            val sp = base.getSharedPreferences("com.google.firebase.messaging", Context.MODE_PRIVATE)
            sp.edit().putBoolean("auto_init", false).commit()
        } catch (e: Exception) {
            Log.d(TAG, "Early SharedPreferences setup note: ${e.message}")
        }
    }

    override fun onCreate() {
        super.onCreate()
        try {
            val sp = getSharedPreferences("com.google.firebase.messaging", Context.MODE_PRIVATE)
            sp.edit().putBoolean("auto_init", false).commit()
        } catch (_: Exception) {}

        // Global uncaught exception handler guard to prevent unexpected crashes
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e(TAG, "Uncaught exception intercepted in thread ${thread.name}: ${throwable.message}", throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }

        // 1. Initialize Google Analytics for Firebase
        try {
            com.example.analytics.AnalyticsManager.initialize(this)
            Log.i(TAG, "✓ Google Analytics for Firebase initialized")
        } catch (e: Exception) {
            Log.w(TAG, "Firebase Analytics init note: ${e.message}")
        }

        // 2. Initialize Monetization SDKs safely on Application main thread
        // InMobi is Primary; Start.io is Secondary Fallback (All Real Ads, Test Ads DISABLED)
        initMonetizationSdks()
    }

    /**
     * Executes InMobi (Primary) and Start.io (Secondary) SDK initialization on the main thread
     * with production real ads enabled and test mode explicitly disabled.
     */
    private fun initMonetizationSdks() {
        // 1. Initialize InMobi SDK (Primary Real Ads Network)
        try {
            InMobiSdk.setLogLevel(InMobiSdk.LogLevel.NONE)
            InMobiSdk.setLocationWithCityStateCountry("Delhi", "Delhi", "India")
            val consentJson = JSONObject().apply {
                put("gdpr_consent_available", true)
                put("gdpr", "0")
                put("ccpa", "1---")
            }

            InMobiSdk.init(
                this,
                AdSdkInitializer.INMOBI_ACCOUNT_ID,
                consentJson,
                object : SdkInitializationListener {
                    override fun onInitializationComplete(error: java.lang.Error?) {
                        if (error == null) {
                            Log.i(TAG, "✓ InMobi SDK (Primary) initialized on Application start (Account ID: ${AdSdkInitializer.INMOBI_ACCOUNT_ID})")
                        } else {
                            Log.w(TAG, "InMobi SDK initialization completed with notice: ${error.message}")
                        }
                    }
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "InMobi early init error: ${e.message}", e)
        }

        // 2. Initialize Start.io SDK in Production Mode (Secondary Fallback, Test Ads explicitly DISABLED)
        try {
            StartAppSDK.init(this, AdSdkInitializer.STARTIO_APP_ID, false)
            StartAppSDK.enableReturnAds(false)
            StartAppSDK.setTestAdsEnabled(false)
            Log.i(TAG, "✓ Start.io SDK (Secondary) initialized in Production Mode (Test Ads DISABLED, App ID: ${AdSdkInitializer.STARTIO_APP_ID})")
        } catch (e: Exception) {
            Log.e(TAG, "Start.io early init error: ${e.message}", e)
        }
    }
}
