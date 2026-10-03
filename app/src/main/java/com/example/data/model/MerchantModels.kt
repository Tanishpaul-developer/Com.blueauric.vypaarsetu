package com.example.data.model

import android.graphics.Bitmap

data class LanguageOption(
    val code: String,
    val scriptChar: String,
    val localName: String,
    val englishName: String
)

val SupportedLanguages = listOf(
    LanguageOption("en", "Aa", "English", "India"),
    LanguageOption("hi", "अ", "हिन्दी", "Hindi"),
    LanguageOption("bn", "ক", "বাংলা", "Bengali"),
    LanguageOption("ta", "அ", "தமிழ்", "Tamil"),
    LanguageOption("mr", "ळ", "मराठी", "Marathi"),
    LanguageOption("te", "తె", "తెలుగు", "Telugu"),
    LanguageOption("gu", "ગુ", "ગુજરાતી", "Gujarati"),
    LanguageOption("pa", "ੴ", "ਪੰਜਾਬੀ", "Punjabi"),
    LanguageOption("mni", "ꯏ", "মৈতৈলোন্", "Manipuri")
)

data class MerchantProfile(
    val shopName: String = "",
    val ownerName: String = "",
    val upiId: String = "",
    val phone: String = "",
    val languageCode: String = "hi",
    val isOnboarded: Boolean = false,
    val autoSplitEnabled: Boolean = true,
    val soundboxEnabled: Boolean = true,
    val soundboxVolume: Float = 1.0f,
    val isDarkMode: Boolean = true,
    val biometricLockEnabled: Boolean = false,
    val businessCategory: String = "General Store / Kirana",
    val address: String = "",
    val email: String = "",
    val gstNumber: String = "",
    val logoUri: String = "",
    val autoCloudSyncEnabled: Boolean = true,
    val lastSyncTimestamp: Long = 0L,
    // Soundbox TTS Model Customization
    val soundboxLanguage: String = "default", // "default" = follows app language, or specific language
    val soundboxPitch: Float = 1.0f, // 0.75f (deep) to 1.25f (bright)
    val soundboxSpeechRate: Float = 1.0f, // 0.8f (steady) to 1.3f (brisk)
    val soundboxVoiceTone: String = "female", // "female", "male", "natural"
    val soundboxPreChime: Boolean = true // Play bell chime before announcement
)

sealed interface UiState<out T> {
    object Idle : UiState<Nothing>
    object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String, val cause: Throwable? = null) : UiState<Nothing>
}

enum class SplitStatus {
    PAID,
    AWAITING_SCAN
}

data class SplitPart(
    val index: Int,
    val totalParts: Int,
    val amount: Double,
    val status: SplitStatus = SplitStatus.AWAITING_SCAN,
    val upiUri: String = "",
    val qrBitmap: Bitmap? = null,
    val paidAtTimestamp: Long? = null,
    val senderName: String? = null,
    val txnId: String? = null
)

data class SplitBillResult(
    val totalAmount: Double,
    val isAutoSplit: Boolean,
    val parts: List<SplitPart>
)

data class OcrParsedTransaction(
    val payerName: String,
    val amount: Double,
    val upiRef: String,
    val time: String,
    val type: String = "CREDIT",
    val status: String = "SUCCESS",
    val rawText: String = ""
)

data class SoundboxNotificationEvent(
    val id: String,
    val appSource: String, // Google Pay, PhonePe, Paytm, BHIM
    val amount: Double,
    val senderName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val announcedText: String = ""
)

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

data class SmartTip(
    val title: String,
    val description: String,
    val iconType: String, // "OVERDUE", "LOW_STOCK", "CASH", "STREAK"
    val actionLabel: String,
    val customerPhone: String? = null,
    val customerDue: Double? = null,
    val customerName: String? = null,
    val targetScreen: String? = null
)
