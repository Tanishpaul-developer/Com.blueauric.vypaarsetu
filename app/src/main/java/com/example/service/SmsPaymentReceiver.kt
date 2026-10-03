package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver listening for incoming SMS messages (android.provider.Telephony.SMS_RECEIVED).
 * Uses PaymentParser to detect bank payment credits and LedgerPaymentProcessor to update the merchant's ledger.
 */
class SmsPaymentReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        try {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            if (messages.isNullOrEmpty()) return

            val fullMessage = StringBuilder()
            var senderAddress = "Bank"

            for (sms in messages) {
                fullMessage.append(sms.messageBody)
                sms.originatingAddress?.let { senderAddress = it }
            }

            val body = fullMessage.toString()
            Log.d(TAG, "Incoming SMS from $senderAddress: $body")

            val parsedPayment = PaymentParser.parse(body, senderAddress)
            if (parsedPayment != null && parsedPayment.amount > 0.0) {
                Log.i(TAG, "Parsed payment credit: ₹${parsedPayment.amount} from ${parsedPayment.payerName ?: "Unknown"} via ${parsedPayment.bankOrApp}")

                // Asynchronously update Room DB, Customer Ledger, Soundbox and Notifications
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        LedgerPaymentProcessor.processPayment(context.applicationContext, parsedPayment)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to process SMS payment in ledger: ${e.message}", e)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in SmsPaymentReceiver: ${e.message}", e)
        }
    }

    companion object {
        private const val TAG = "SmsPaymentReceiver"
    }
}
