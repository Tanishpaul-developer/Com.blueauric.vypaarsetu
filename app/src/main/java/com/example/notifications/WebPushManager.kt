package com.example.notifications

import android.content.Context
import android.os.Build
import android.util.Log
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import java.io.File
import kotlinx.coroutines.tasks.await

/**
 * Web Push & Cloud Messaging Manager for VyaparSetu.
 * Manages FCM registration tokens, VAPID Web Push certificate integration,
 * and syncs tokens to Firestore under the merchant's authenticated account.
 */
object WebPushManager {

    private const val TAG = "WebPushManager"

    // Merchant Web Push VAPID Public Certificate Key provided by user
    const val WEB_PUSH_VAPID_KEY =
        "BNs5Uiev0hXyJf49o4MBX1nagAEVADgFp856o1r4OoEtKuMZRUJTRQQmMqlI0vbWi6Y_5-MGyy-uuhmsV585TOc"

    /**
     * Checks if the app is executing within an emulator/virtual container environment
     * lacking Google Account Play Services credentials.
     * Includes detection for standard emulators, Genymotion, QEMU, Ranchu, and Cloud Cuttlefish instances.
     */
    fun isEmulatorEnvironment(): Boolean {
        val fingerprint = Build.FINGERPRINT.lowercase()
        val model = Build.MODEL.lowercase()
        val manufacturer = Build.MANUFACTURER.lowercase()
        val brand = Build.BRAND.lowercase()
        val device = Build.DEVICE.lowercase()
        val product = Build.PRODUCT.lowercase()
        val hardware = Build.HARDWARE.lowercase()
        val board = Build.BOARD.lowercase()

        return brand.startsWith("generic")
                || device.startsWith("generic")
                || fingerprint.startsWith("generic")
                || fingerprint.startsWith("unknown")
                || fingerprint.contains("test-keys")
                || fingerprint.contains("cutf")
                || fingerprint.contains("cvd")
                || hardware.contains("goldfish")
                || hardware.contains("ranchu")
                || hardware.contains("cutf")
                || hardware.contains("cvd")
                || hardware.contains("vsoc")
                || hardware.contains("virtio")
                || model.contains("google_sdk")
                || model.contains("emulator")
                || model.contains("android sdk built for x86")
                || model.contains("cuttlefish")
                || model.contains("cf_x86")
                || model.contains("sdk_gphone")
                || manufacturer.contains("genymotion")
                || product.contains("sdk_google")
                || product.contains("google_sdk")
                || product.contains("sdk")
                || product.contains("sdk_x86")
                || product.contains("vbox86p")
                || product.contains("emulator")
                || product.contains("simulator")
                || product.contains("cf_x86")
                || product.contains("cuttlefish")
                || product.contains("cvd")
                || device.contains("vsoc")
                || device.contains("cutf")
                || device.contains("cf_x86")
                || board.contains("vsoc")
                || board.contains("cutf")
                || File("/dev/socket/qemud").exists()
                || File("/dev/qemu_pipe").exists()
                || File("/dev/goldfish_pipe").exists()
    }

    /**
     * Initializes Push Messaging, fetches the current device registration token,
     * and syncs it to Firestore under the merchant's profile.
     * Safely guards against hard failure exceptions when running on emulators or devices without Google Play Services.
     */
    fun initializePushMessaging(context: Context) {
        try {
            // Guard 1: Skip token polling on emulator/container instances immediately
            if (isEmulatorEnvironment()) {
                Log.d(TAG, "Virtual/emulator runtime detected. Bypassing FCM registration to avoid non-GMS network failure.")
                return
            }

            // Guard 2: Direct SharedPreferences write to ensure auto-init is disabled without loading FCM classes
            try {
                val sp = context.getSharedPreferences("com.google.firebase.messaging", Context.MODE_PRIVATE)
                sp.edit().putBoolean("auto_init", false).commit()
            } catch (_: Exception) {}

            // Guard 3: Verify Google Play Services is available and operational
            val gmsAvailability = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context)
            if (gmsAvailability != ConnectionResult.SUCCESS) {
                Log.w(TAG, "Google Play Services not operational (status code: $gmsAvailability). Skipping FCM push registration on this device.")
                return
            }

            // Guard 4: Only query device token if a merchant is actively authenticated
            val user = FirebaseAuth.getInstance().currentUser
            if (user == null) {
                Log.d(TAG, "No authenticated merchant logged in yet. Deferring token sync until sign-in.")
                return
            }

            // Step 5: On-demand token query
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w(TAG, "FCM registration token retrieval unconfirmed: ${task.exception?.message}")
                    return@addOnCompleteListener
                }

                // Get new FCM registration token
                val token = task.result
                if (!token.isNullOrBlank()) {
                    Log.i(TAG, "✓ FCM Device Registration Token retrieved: $token")
                    // Sync token with Firestore
                    syncTokenWithFirestore(token)
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "WebPushManager initialization deferred: ${e.message}")
        }
    }

    /**
     * Syncs the device FCM / Web push token to Firestore so backend push senders can message the merchant.
     */
    fun syncTokenWithFirestore(token: String) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val firestore = FirebaseFirestore.getInstance()

        val tokenData = mapOf(
            "fcm_token" to token,
            "web_push_vapid_key" to WEB_PUSH_VAPID_KEY,
            "device_platform" to "Android",
            "last_token_update" to com.google.firebase.Timestamp.now()
        )

        firestore.collection("merchants")
            .document(user.uid)
            .update(tokenData)
            .addOnSuccessListener {
                Log.d(TAG, "Successfully synced FCM token to Firestore merchant profile.")
            }
            .addOnFailureListener {
                // If document does not exist yet, set with merge
                firestore.collection("merchants")
                    .document(user.uid)
                    .set(tokenData, com.google.firebase.firestore.SetOptions.merge())
            }
    }
}
