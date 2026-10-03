package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.data.local.AppDatabase
import com.example.data.local.TransactionEntity
import com.example.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.regex.Pattern

class SoundboxNotificationListenerService : NotificationListenerService() {

    private val scope = CoroutineScope(Dispatchers.IO)

    companion object {
        private const val TAG = "SoundboxListener"
        const val CHANNEL_ID = "vyaparsetu_soundbox_channel"
        const val CHANNEL_NAME = "VyaparSetu Payment Soundbox"

        val TARGET_PACKAGES = mapOf(
            "com.google.android.apps.nbu.paisa.user" to "Google Pay",
            "com.phonepe.app" to "PhonePe",
            "net.one97.paytm" to "Paytm",
            "in.org.npci.upiapp" to "BHIM",
            "com.bharatpe.app" to "BharatPe",
            "in.amazon.mShop.android.shopping" to "Amazon Pay",
            "com.dreamplug.androidapp" to "CRED"
        )

        // Matches: "received Rs. 150", "credited by ₹500", "₹1,250 received from Rajesh", "INR 350.00"
        private val AMOUNT_PATTERN = Pattern.compile(
            "(?:₹|Rs\\.?|INR)\\s*([0-9]{1,3}(?:,[0-9]{3})*(?:\\.[0-9]{1,2})?|[0-9]+(?:\\.[0-9]{1,2})?)",
            Pattern.CASE_INSENSITIVE
        )

        private val REVERSE_AMOUNT_PATTERN = Pattern.compile(
            "([0-9]{1,3}(?:,[0-9]{3})*(?:\\.[0-9]{1,2})?|[0-9]+(?:\\.[0-9]{1,2})?)\\s*(?:₹|Rs\\.?|INR|रुपये|rupees)",
            Pattern.CASE_INSENSITIVE
        )

        fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Announces incoming UPI payment confirmations aloud"
                    enableVibration(true)
                }
                val manager = context.getSystemService(NotificationManager::class.java)
                manager?.createNotificationChannel(channel)
            }
        }

        fun extractAmount(text: String): Double? {
            var matcher = AMOUNT_PATTERN.matcher(text)
            if (matcher.find()) {
                val clean = matcher.group(1)?.replace(",", "")
                return clean?.toDoubleOrNull()
            }
            matcher = REVERSE_AMOUNT_PATTERN.matcher(text)
            if (matcher.find()) {
                val clean = matcher.group(1)?.replace(",", "")
                return clean?.toDoubleOrNull()
            }
            return null
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel(applicationContext)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val pkg = sbn.packageName ?: return
        val appSource = TARGET_PACKAGES[pkg]

        val extras = sbn.notification?.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString().orEmpty()

        val fullContent = "$title $text $bigText"

        val isPaymentNotification = appSource != null ||
                fullContent.contains("received", ignoreCase = true) ||
                fullContent.contains("credited", ignoreCase = true) ||
                fullContent.contains("प्राप्त", ignoreCase = true) ||
                fullContent.contains("जमा", ignoreCase = true)

        if (!isPaymentNotification) return

        val source = appSource ?: "UPI"
        val parsed = PaymentParser.parse(fullContent, source)
        val detectedAmount = parsed?.amount ?: extractAmount(fullContent) ?: return
        val sender = parsed?.payerName ?: extractSenderName(fullContent)

        Log.d(TAG, "Payment intercepted: Source=$source, Amount=$detectedAmount, Sender=$sender")

        // Broadcast notification update to PaymentNotificationReceiver
        try {
            val broadcastIntent = Intent(PaymentNotificationReceiver.ACTION_PAYMENT_NOTIFICATION).apply {
                setPackage(packageName)
                putExtra(PaymentNotificationReceiver.EXTRA_PAYMENT_TEXT, fullContent)
                putExtra(PaymentNotificationReceiver.EXTRA_PAYMENT_SOURCE, source)
            }
            sendBroadcast(broadcastIntent)
        } catch (e: Exception) {
            Log.w(TAG, "Broadcast error: ${e.message}")
        }

        scope.launch {
            val paymentToProcess = parsed ?: ParsedPayment(
                amount = detectedAmount,
                payerName = sender.ifBlank { null },
                upiRef = "NOTIF-${System.currentTimeMillis() % 1000000}",
                bankOrApp = source,
                isCredit = true,
                rawText = fullContent
            )
            LedgerPaymentProcessor.processPayment(applicationContext, paymentToProcess)
        }
    }

    private fun extractSenderName(content: String): String {
        val fromRegex = Pattern.compile("(?:from|by|से)\\s+([A-Za-z\\s]{3,25})", Pattern.CASE_INSENSITIVE)
        val m = fromRegex.matcher(content)
        if (m.find()) {
            return m.group(1)?.trim() ?: ""
        }
        return ""
    }
}
