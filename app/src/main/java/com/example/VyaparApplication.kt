package com.example

import android.app.Application
import android.content.Context
import android.util.Log

/**
 * Custom Application class for VyaparSetu.
 * Pre-configures environment settings, suppresses auto-initialization of non-essential
 * background daemons on startup, and sets default error guards.
 */
class VyaparApplication : Application() {

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        try {
            // Force FCM auto_init to false in SharedPreferences synchronously before
            // FirebaseInitProvider or FirebaseMessaging components can instantiate.
            val sp = base.getSharedPreferences("com.google.firebase.messaging", Context.MODE_PRIVATE)
            sp.edit().putBoolean("auto_init", false).commit()
        } catch (e: Exception) {
            Log.d("VyaparApp", "Early SharedPreferences setup note: ${e.message}")
        }
    }

    override fun onCreate() {
        super.onCreate()
        try {
            val sp = getSharedPreferences("com.google.firebase.messaging", Context.MODE_PRIVATE)
            sp.edit().putBoolean("auto_init", false).commit()
        } catch (_: Exception) {}
    }
}
