package com.example.monetization

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong

private val Context.adDataStore by preferencesDataStore(name = "ad_policy_prefs")

/**
 * Ad Capping & Policy Manager for VyaparSetu.
 * Enforces strict interstitial frequency capping and daily rewarded feature credits.
 */
class AdPolicyManager(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO)

    object Workflows {
        const val NEW_SALE = "WORKFLOW_NEW_SALE"
        const val CONTACT_FEEDBACK = "WORKFLOW_CONTACT_FEEDBACK"
        const val TRANSACTION_COMPLETED = "WORKFLOW_TRANSACTION_COMPLETED"
        const val DAILY_SUMMARY_VIEWED = "WORKFLOW_DAILY_SUMMARY_VIEWED"
        const val REPORTS_VIEWED = "WORKFLOW_REPORTS_VIEWED"
        const val INVOICE_GENERATED = "WORKFLOW_INVOICE_GENERATED"
    }

    object MonetizedFeatures {
        const val SCAN_OCR = "Scan OCR Bill Verification"
        const val AI_VOICE = "AI Voice Assistant Billing"
        const val PDF_EXPORT = "PDF Ledger & Invoice Export"
        const val WHATSAPP_CATALOG = "WhatsApp Catalog Setup"
    }

    private object Keys {
        val FEATURE_CREDITS = intPreferencesKey("feature_credits")
        val LAST_CREDIT_RESET_DAY = longPreferencesKey("last_credit_reset_day")
        val LAST_INTERSTITIAL_EPOCH_MS = longPreferencesKey("last_interstitial_epoch_ms")
    }

    companion object {
        const val INTERSTITIAL_COOLDOWN_MS = 180_000L // 3 minutes strictly
        const val DAILY_FREE_CREDITS = 5
        const val REWARDED_BONUS_CREDITS = 5

        @Volatile
        private var instance: AdPolicyManager? = null

        fun getInstance(context: Context): AdPolicyManager {
            return instance ?: synchronized(this) {
                instance ?: AdPolicyManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val lastInterstitialTime = AtomicLong(0L)
    private val _creditsState = MutableStateFlow(DAILY_FREE_CREDITS)
    val creditsState: StateFlow<Int> = _creditsState.asStateFlow()

    init {
        scope.launch {
            checkAndResetDailyCredits()
            context.adDataStore.data.collect { preferences ->
                val credits = preferences[Keys.FEATURE_CREDITS] ?: DAILY_FREE_CREDITS
                _creditsState.value = credits
                val lastAdTime = preferences[Keys.LAST_INTERSTITIAL_EPOCH_MS] ?: 0L
                lastInterstitialTime.set(lastAdTime)
            }
        }
    }

    // ==========================================
    // 1. INTERSTITIAL CAPPING & BLOCKING RULES
    // ==========================================

    /**
     * Checks if an interstitial ad is eligible to be shown.
     * 1. Blocks on sensitive workflows: New Sale / Instant QR and Contact/Feedback.
     * 2. Enforces a strict 3-minute (180,000ms) cooldown between displays.
     */
    fun canShowInterstitial(workflow: String): Boolean {
        // Block sensitive transaction and customer support journeys
        if (workflow == Workflows.NEW_SALE || workflow == Workflows.CONTACT_FEEDBACK) {
            return false
        }

        val now = System.currentTimeMillis()
        val elapsed = now - lastInterstitialTime.get()

        return elapsed >= INTERSTITIAL_COOLDOWN_MS
    }

    fun getRemainingCooldownSeconds(): Long {
        val now = System.currentTimeMillis()
        val elapsed = now - lastInterstitialTime.get()
        return if (elapsed < INTERSTITIAL_COOLDOWN_MS) {
            (INTERSTITIAL_COOLDOWN_MS - elapsed) / 1000L
        } else {
            0L
        }
    }

    fun recordInterstitialShown() {
        val now = System.currentTimeMillis()
        lastInterstitialTime.set(now)
        scope.launch {
            context.adDataStore.edit { preferences ->
                preferences[Keys.LAST_INTERSTITIAL_EPOCH_MS] = now
            }
        }
    }

    // ==========================================
    // 2. REWARDED CREDITS GATE
    // ==========================================

    private suspend fun checkAndResetDailyCredits() {
        val currentDay = System.currentTimeMillis() / 86_400_000L
        context.adDataStore.edit { preferences ->
            val lastResetDay = preferences[Keys.LAST_CREDIT_RESET_DAY] ?: 0L
            if (currentDay > lastResetDay) {
                preferences[Keys.LAST_CREDIT_RESET_DAY] = currentDay
                preferences[Keys.FEATURE_CREDITS] = DAILY_FREE_CREDITS
                _creditsState.value = DAILY_FREE_CREDITS
            }
        }
    }

    val featureCreditsFlow: Flow<Int> = context.adDataStore.data.map { preferences ->
        preferences[Keys.FEATURE_CREDITS] ?: DAILY_FREE_CREDITS
    }

    fun hasFeatureCredits(): Boolean {
        return _creditsState.value > 0
    }

    fun consumeCredit(): Boolean {
        val current = _creditsState.value
        if (current > 0) {
            val updated = current - 1
            _creditsState.value = updated
            scope.launch {
                context.adDataStore.edit { preferences ->
                    preferences[Keys.FEATURE_CREDITS] = updated
                }
            }
            return true
        }
        return false
    }

    fun grantRewardCredits(amount: Int = REWARDED_BONUS_CREDITS) {
        val updated = _creditsState.value + amount
        _creditsState.value = updated
        scope.launch {
            context.adDataStore.edit { preferences ->
                preferences[Keys.FEATURE_CREDITS] = updated
            }
        }
    }
}
