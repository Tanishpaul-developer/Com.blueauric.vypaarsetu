package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.data.firebase.FirestoreManager
import com.example.data.local.AppDatabase
import com.example.data.local.CustomerEntity
import com.example.data.local.KhataEntryEntity
import com.example.data.local.TransactionEntity
import com.example.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.first

object LedgerPaymentProcessor {

    private const val TAG = "LedgerPaymentProcessor"
    const val CHANNEL_ID = "vyaparsetu_payment_received_channel"
    const val CHANNEL_NAME = "Payment Received & Ledger Updates"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts for incoming customer UPI/Bank payments and automatic ledger updates"
                enableVibration(true)
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    suspend fun processPayment(context: Context, payment: ParsedPayment) {
        try {
            Log.d(TAG, "Processing incoming payment: ₹${payment.amount} from '${payment.payerName}' via ${payment.bankOrApp}")

            val db = AppDatabase.getInstance(context)
            val prefs = UserPreferencesRepository(context)
            val profile = prefs.merchantProfileFlow.first()
            val now = System.currentTimeMillis()

            // 1. Record Transaction in Room DB
            val txnId = db.transactionDao().insertTransaction(
                TransactionEntity(
                    amount = payment.amount,
                    payerName = payment.payerName ?: "UPI Customer",
                    upiRef = payment.upiRef ?: "AUTO-${now % 1000000}",
                    paymentSource = payment.bankOrApp,
                    type = "CREDIT",
                    status = "SETTLED",
                    timestamp = now,
                    note = "Auto-detected via ${payment.bankOrApp}"
                )
            )

            // 2. Intelligent Customer Ledger Matching & Balance Update
            var matchedCustomer: CustomerEntity? = null
            val customers = db.customerDao().getAllCustomersList()

            // A. Match by Name if payerName exists
            if (!payment.payerName.isNullOrBlank()) {
                val cleanPayer = payment.payerName.trim().lowercase()
                matchedCustomer = customers.firstOrNull { c ->
                    val cleanCustomer = c.name.trim().lowercase()
                    cleanCustomer.contains(cleanPayer) || cleanPayer.contains(cleanCustomer)
                }
            }

            // B. If no name match, check for unique debtor with exact matching balance due
            if (matchedCustomer == null) {
                val exactMatches = customers.filter { it.balanceDue == payment.amount }
                if (exactMatches.size == 1) {
                    matchedCustomer = exactMatches.first()
                }
            }

            // 3. Update Ledger if Customer Matched
            if (matchedCustomer != null) {
                val customerId = matchedCustomer.id
                db.customerDao().updateCustomerBalance(customerId, -payment.amount, now)

                val khataEntry = KhataEntryEntity(
                    customerId = customerId,
                    customerName = matchedCustomer.name,
                    amount = payment.amount,
                    type = "JAMA",
                    description = "ऑटो भुगतान जमा (Ref: ${payment.upiRef ?: "UPI"})",
                    timestamp = now
                )
                db.customerDao().insertKhataEntry(khataEntry)

                val updatedCustomer = db.customerDao().getCustomerById(customerId)
                if (updatedCustomer != null && updatedCustomer.balanceDue <= 0.0) {
                    db.customerDao().markAllCustomerEntriesCleared(customerId)
                }

                // Sync to Firestore if cloud is configured
                if (profile.upiId.isNotBlank()) {
                    if (updatedCustomer != null) {
                        FirestoreManager.syncCustomer(profile.upiId, updatedCustomer)
                    }
                    FirestoreManager.addKhataEntryToCloud(profile.upiId, khataEntry)
                }

                Log.i(TAG, "Ledger automatically updated for ${matchedCustomer.name}: -₹${payment.amount}")
            }

            // 4. Voice Soundbox Announcement
            if (profile.soundboxEnabled) {
                SoundboxSpeaker.announcePayment(
                    context = context,
                    appSource = payment.bankOrApp,
                    amount = payment.amount,
                    senderName = matchedCustomer?.name ?: payment.payerName ?: "",
                    languageCode = profile.languageCode
                )
            }

            // 5. Post Status Notification
            postPaymentNotification(context, payment, matchedCustomer)

        } catch (e: Exception) {
            Log.e(TAG, "Error in processPayment: ${e.message}", e)
        }
    }

    private fun postPaymentNotification(
        context: Context,
        payment: ParsedPayment,
        matchedCustomer: CustomerEntity?
    ) {
        try {
            createNotificationChannel(context)

            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                (payment.timestamp % 10000).toInt(),
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val subtitle = if (matchedCustomer != null) {
                "✓ ${matchedCustomer.name} का उधार ₹${payment.amount.toInt()} कम हुआ (Ledger Updated)"
            } else {
                "✓ ${payment.bankOrApp} से प्राप्त • दैनिक खाते में जोड़ा गया"
            }

            val notif = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_notify_chat)
                .setContentTitle("₹${payment.amount} प्राप्त हुए! (${payment.bankOrApp})")
                .setContentText(subtitle)
                .setStyle(NotificationCompat.BigTextStyle().bigText(
                    "भुगतान राशि: ₹${payment.amount}\n" +
                    "प्रेषक: ${matchedCustomer?.name ?: payment.payerName ?: "ग्राहक"}\n" +
                    "UPI Ref: ${payment.upiRef ?: "N/A"}\n" +
                    subtitle
                ))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            NotificationManagerCompat.from(context).notify(
                (payment.timestamp % 100000).toInt(),
                notif
            )
        } catch (e: SecurityException) {
            Log.w(TAG, "Notification permission missing: ${e.message}")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to post notification: ${e.message}")
        }
    }
}
