package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver listening for banking app notification updates and payment broadcasts.
 * Parses payment details from notifications and updates merchant ledger.
 */
class PaymentNotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        Log.d(TAG, "PaymentNotificationReceiver received action: $action")

        val text = intent.getStringExtra(EXTRA_PAYMENT_TEXT)
            ?: intent.getStringExtra("android.intent.extra.TEXT")
            ?: intent.getStringExtra("message")
            ?: return

        val source = intent.getStringExtra(EXTRA_PAYMENT_SOURCE) ?: "Banking App"

        val parsed = PaymentParser.parse(text, source)
        if (parsed != null && parsed.amount > 0.0) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    LedgerPaymentProcessor.processPayment(context.applicationContext, parsed)
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing notification payment: ${e.message}", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    companion object {
        private const val TAG = "PaymentNotifReceiver"
        const val ACTION_PAYMENT_NOTIFICATION = "com.example.vyaparsetu.ACTION_PAYMENT_NOTIFICATION"
        const val EXTRA_PAYMENT_TEXT = "extra_payment_text"
        const val EXTRA_PAYMENT_SOURCE = "extra_payment_source"
    }
}
