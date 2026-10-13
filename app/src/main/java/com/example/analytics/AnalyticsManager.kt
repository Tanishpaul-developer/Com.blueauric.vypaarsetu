package com.example.analytics

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics

/**
 * Centralized Google Analytics for Firebase Manager for VyaparSetu.
 * Tracks Critical Merchant CUJs and feature events:
 * - Onboarding completion
 * - POS transactions & checkout
 * - Dynamic UPI QR generation & Auto-Split
 * - Soundbox mode activation & audio alerts
 * - Screenshot OCR receipt parsing
 * - Digital Khata & Ledger entries (Udhar/Jama)
 * - AI Bol chatbot interactions & advice queries
 * - Bug reports and feature suggestions
 */
object AnalyticsManager {

    private const val TAG = "AnalyticsManager"

    @Volatile
    private var firebaseAnalytics: FirebaseAnalytics? = null

    fun initialize(context: Context) {
        if (firebaseAnalytics == null) {
            synchronized(this) {
                if (firebaseAnalytics == null) {
                    try {
                        firebaseAnalytics = FirebaseAnalytics.getInstance(context.applicationContext).apply {
                            setAnalyticsCollectionEnabled(true)
                        }
                        Log.i(TAG, "✓ Google Analytics for Firebase initialized successfully")
                    } catch (e: Exception) {
                        Log.w(TAG, "Firebase Analytics initialization skipped or offline: ${e.message}")
                    }
                }
            }
        }
    }

    private fun logEvent(eventName: String, params: Bundle.() -> Unit = {}) {
        try {
            val bundle = Bundle().apply(params)
            firebaseAnalytics?.logEvent(eventName, bundle)
            Log.d(TAG, "Logged event '$eventName': $bundle")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to log analytics event $eventName: ${e.message}")
        }
    }

    // 1. Merchant Onboarding Completion
    fun logOnboardingCompleted(
        shopName: String,
        category: String,
        language: String,
        hasGst: Boolean,
        hasLogo: Boolean
    ) {
        logEvent("merchant_onboarding_complete") {
            putString("shop_name", shopName)
            putString("business_category", category)
            putString("language_code", language)
            putBoolean("has_gst", hasGst)
            putBoolean("has_logo", hasLogo)
            putLong("timestamp", System.currentTimeMillis())
        }
    }

    // 2. POS Billing & Checkout Transaction
    fun logPosTransaction(
        amount: Double,
        itemCount: Int,
        paymentMode: String, // "CASH", "UPI", "CREDIT"
        isTaxInvoice: Boolean,
        hasDiscount: Boolean
    ) {
        logEvent("pos_transaction_completed") {
            putDouble(FirebaseAnalytics.Param.VALUE, amount)
            putString(FirebaseAnalytics.Param.CURRENCY, "INR")
            putInt("item_count", itemCount)
            putString(FirebaseAnalytics.Param.PAYMENT_TYPE, paymentMode)
            putBoolean("is_tax_invoice", isTaxInvoice)
            putBoolean("has_discount", hasDiscount)
        }
    }

    // 3. Dynamic UPI QR Generation & Auto-Split
    fun logUpiQrGenerated(
        amount: Double,
        isAutoSplit: Boolean,
        splitParts: Int = 1
    ) {
        logEvent("upi_qr_generated") {
            putDouble(FirebaseAnalytics.Param.VALUE, amount)
            putString(FirebaseAnalytics.Param.CURRENCY, "INR")
            putBoolean("is_auto_split", isAutoSplit)
            putInt("split_parts", splitParts)
        }
    }

    // 4. Soundbox Voice Announcement Mode
    fun logSoundboxModeActivated(
        language: String,
        voiceTone: String,
        amountAnnounced: Double? = null
    ) {
        logEvent("soundbox_mode_activated") {
            putString("language", language)
            putString("voice_tone", voiceTone)
            if (amountAnnounced != null) {
                putDouble("announced_amount", amountAnnounced)
            }
        }
    }

    // 5. Screenshot OCR Usage
    fun logOcrUsed(
        detectedAmount: Double,
        payerFound: Boolean,
        upiRefFound: Boolean,
        language: String
    ) {
        logEvent("ocr_receipt_scan") {
            putDouble(FirebaseAnalytics.Param.VALUE, detectedAmount)
            putBoolean("payer_found", payerFound)
            putBoolean("upi_ref_found", upiRefFound)
            putString("language_code", language)
        }
    }

    // 6. Ledger & Khata Operations (Credit given / Udhar vs Payment received / Jama)
    fun logLedgerOperation(
        operationType: String, // "UDHAR" or "JAMA"
        amount: Double,
        customerId: Long,
        hasWhatsappReminderSent: Boolean = false
    ) {
        logEvent("khata_ledger_operation") {
            putString("operation_type", operationType)
            putDouble(FirebaseAnalytics.Param.VALUE, amount)
            putString(FirebaseAnalytics.Param.CURRENCY, "INR")
            putLong("customer_id", customerId)
            putBoolean("whatsapp_reminder_sent", hasWhatsappReminderSent)
        }
    }

    // 7. AI Bol Chatbot Interactions
    fun logChatbotInteraction(
        queryCategory: String, // "FINANCE", "DEBTORS", "INVENTORY", "SUPPORT"
        language: String,
        isVoiceInput: Boolean = false
    ) {
        logEvent("ai_bol_interaction") {
            putString("query_category", queryCategory)
            putString("language", language)
            putBoolean("is_voice_input", isVoiceInput)
        }
    }

    // 8. Feedback / Bug Report / Feature Suggestion Submission
    fun logFeedbackSubmitted(
        type: String, // "BUG" or "SUGGESTION"
        category: String,
        rating: Int
    ) {
        logEvent("user_feedback_submitted") {
            putString("feedback_type", type)
            putString("feedback_category", category)
            putInt("rating", rating)
            putLong("timestamp", System.currentTimeMillis())
        }
    }
}
