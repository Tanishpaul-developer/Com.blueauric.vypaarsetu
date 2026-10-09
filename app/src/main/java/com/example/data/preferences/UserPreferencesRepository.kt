package com.example.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.MerchantProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "vyapar_settings")

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val LANGUAGE_CODE = stringPreferencesKey("language_code")
        val SHOP_NAME = stringPreferencesKey("shop_name")
        val OWNER_NAME = stringPreferencesKey("owner_name")
        val UPI_ID = stringPreferencesKey("upi_id")
        val PHONE = stringPreferencesKey("phone")
        val ADDRESS = stringPreferencesKey("address")
        val EMAIL = stringPreferencesKey("email")
        val GST_NUMBER = stringPreferencesKey("gst_number")
        val LOGO_URI = stringPreferencesKey("logo_uri")
        val BUSINESS_CATEGORY = stringPreferencesKey("business_category")
        val IS_ONBOARDED = booleanPreferencesKey("is_onboarded")
        val AUTO_SPLIT_ENABLED = booleanPreferencesKey("auto_split_enabled")
        val SOUNDBOX_ENABLED = booleanPreferencesKey("soundbox_enabled")
        val SOUNDBOX_VOLUME = floatPreferencesKey("soundbox_volume")
        val IS_DARK_MODE = booleanPreferencesKey("is_dark_mode")
        val BIOMETRIC_LOCK_ENABLED = booleanPreferencesKey("biometric_lock_enabled")
        val LEDGER_STREAK = intPreferencesKey("ledger_streak")
        val LAST_ACTIVE_EPOCH_DAY = longPreferencesKey("last_active_epoch_day")
        val BEST_STREAK = intPreferencesKey("best_streak")
        val DAILY_CLOSING_NOTIF_ENABLED = booleanPreferencesKey("daily_closing_notif_enabled")
        val AUTO_CLOUD_SYNC_ENABLED = booleanPreferencesKey("auto_cloud_sync_enabled")
        val LAST_CLOUD_SYNC_TIMESTAMP = longPreferencesKey("last_cloud_sync_timestamp")
        val SOUNDBOX_TTS_LANGUAGE = stringPreferencesKey("soundbox_tts_language")
        val SOUNDBOX_PITCH = floatPreferencesKey("soundbox_pitch")
        val SOUNDBOX_SPEECH_RATE = floatPreferencesKey("soundbox_speech_rate")
        val SOUNDBOX_VOICE_TONE = stringPreferencesKey("soundbox_voice_tone")
        val SOUNDBOX_PRE_CHIME = booleanPreferencesKey("soundbox_pre_chime")
        val DEFAULT_LOW_STOCK_THRESHOLD = doublePreferencesKey("default_low_stock_threshold")
        val LOW_STOCK_ALERT_SOUND_ENABLED = booleanPreferencesKey("low_stock_alert_sound_enabled")
        val POS_BIOMETRIC_LOCK_ENABLED = booleanPreferencesKey("pos_biometric_lock_enabled")
        val MERCHANT_POS_PIN = stringPreferencesKey("merchant_pos_pin")
    }

    val ledgerStreakFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        val currentStreak = preferences[Keys.LEDGER_STREAK] ?: 1
        if (currentStreak < 1) 1 else currentStreak
    }

    val isAppLockEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[Keys.BIOMETRIC_LOCK_ENABLED] ?: false
    }

    val dailyClosingNotifEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[Keys.DAILY_CLOSING_NOTIF_ENABLED] ?: true
    }

    val autoCloudSyncEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[Keys.AUTO_CLOUD_SYNC_ENABLED] ?: true
    }

    val lastCloudSyncTimestampFlow: Flow<Long> = context.dataStore.data.map { preferences ->
        preferences[Keys.LAST_CLOUD_SYNC_TIMESTAMP] ?: 0L
    }

    val merchantProfileFlow: Flow<MerchantProfile> = context.dataStore.data.map { preferences ->
        MerchantProfile(
            shopName = preferences[Keys.SHOP_NAME] ?: "",
            ownerName = preferences[Keys.OWNER_NAME] ?: "",
            upiId = preferences[Keys.UPI_ID] ?: "",
            phone = preferences[Keys.PHONE] ?: "",
            languageCode = preferences[Keys.LANGUAGE_CODE] ?: "hi",
            isOnboarded = preferences[Keys.IS_ONBOARDED] ?: false,
            autoSplitEnabled = preferences[Keys.AUTO_SPLIT_ENABLED] ?: true,
            soundboxEnabled = preferences[Keys.SOUNDBOX_ENABLED] ?: true,
            soundboxVolume = preferences[Keys.SOUNDBOX_VOLUME] ?: 1.0f,
            isDarkMode = preferences[Keys.IS_DARK_MODE] ?: true,
            biometricLockEnabled = preferences[Keys.BIOMETRIC_LOCK_ENABLED] ?: false,
            businessCategory = preferences[Keys.BUSINESS_CATEGORY] ?: "General Store / Kirana",
            address = preferences[Keys.ADDRESS] ?: "",
            email = preferences[Keys.EMAIL] ?: "",
            gstNumber = preferences[Keys.GST_NUMBER] ?: "",
            logoUri = preferences[Keys.LOGO_URI] ?: "",
            autoCloudSyncEnabled = preferences[Keys.AUTO_CLOUD_SYNC_ENABLED] ?: true,
            lastSyncTimestamp = preferences[Keys.LAST_CLOUD_SYNC_TIMESTAMP] ?: 0L,
            soundboxLanguage = preferences[Keys.SOUNDBOX_TTS_LANGUAGE] ?: "default",
            soundboxPitch = preferences[Keys.SOUNDBOX_PITCH] ?: 1.0f,
            soundboxSpeechRate = preferences[Keys.SOUNDBOX_SPEECH_RATE] ?: 1.0f,
            soundboxVoiceTone = preferences[Keys.SOUNDBOX_VOICE_TONE] ?: "female",
            soundboxPreChime = preferences[Keys.SOUNDBOX_PRE_CHIME] ?: true,
            defaultLowStockThreshold = preferences[Keys.DEFAULT_LOW_STOCK_THRESHOLD] ?: 5.0,
            lowStockAlertSoundEnabled = preferences[Keys.LOW_STOCK_ALERT_SOUND_ENABLED] ?: true,
            posBiometricLockEnabled = preferences[Keys.POS_BIOMETRIC_LOCK_ENABLED] ?: true,
            merchantPosPin = preferences[Keys.MERCHANT_POS_PIN] ?: "1234"
        )
    }

    suspend fun updateInventoryAlertSettings(
        defaultThreshold: Double,
        soundEnabled: Boolean
    ) {
        context.dataStore.edit { preferences ->
            preferences[Keys.DEFAULT_LOW_STOCK_THRESHOLD] = defaultThreshold
            preferences[Keys.LOW_STOCK_ALERT_SOUND_ENABLED] = soundEnabled
        }
    }

    suspend fun updateSoundboxSettings(
        language: String,
        pitch: Float,
        speechRate: Float,
        voiceTone: String,
        preChime: Boolean
    ) {
        context.dataStore.edit { preferences ->
            preferences[Keys.SOUNDBOX_TTS_LANGUAGE] = language
            preferences[Keys.SOUNDBOX_PITCH] = pitch
            preferences[Keys.SOUNDBOX_SPEECH_RATE] = speechRate
            preferences[Keys.SOUNDBOX_VOICE_TONE] = voiceTone
            preferences[Keys.SOUNDBOX_PRE_CHIME] = preChime
        }
    }

    suspend fun updateLanguage(langCode: String) {
        context.dataStore.edit { preferences ->
            preferences[Keys.LANGUAGE_CODE] = langCode
        }
    }

    suspend fun saveProfile(
        shopName: String,
        ownerName: String,
        upiId: String,
        phone: String,
        langCode: String,
        address: String = "",
        email: String = "",
        gstNumber: String = "",
        logoUri: String = "",
        businessCategory: String = "General Store / Kirana"
    ) {
        context.dataStore.edit { preferences ->
            preferences[Keys.SHOP_NAME] = shopName
            preferences[Keys.OWNER_NAME] = ownerName
            preferences[Keys.UPI_ID] = upiId
            preferences[Keys.PHONE] = phone
            preferences[Keys.LANGUAGE_CODE] = langCode
            preferences[Keys.ADDRESS] = address
            preferences[Keys.EMAIL] = email
            preferences[Keys.GST_NUMBER] = gstNumber
            preferences[Keys.LOGO_URI] = logoUri
            preferences[Keys.BUSINESS_CATEGORY] = businessCategory
            preferences[Keys.IS_ONBOARDED] = true
        }
    }

    suspend fun updateStoreLogo(logoUri: String) {
        context.dataStore.edit { preferences ->
            preferences[Keys.LOGO_URI] = logoUri
        }
    }

    suspend fun updateMerchantContactDetails(phone: String, email: String, address: String) {
        context.dataStore.edit { preferences ->
            preferences[Keys.PHONE] = phone
            preferences[Keys.EMAIL] = email
            preferences[Keys.ADDRESS] = address
        }
    }


    suspend fun setAutoSplitEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.AUTO_SPLIT_ENABLED] = enabled
        }
    }

    suspend fun setSoundboxEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.SOUNDBOX_ENABLED] = enabled
        }
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.IS_DARK_MODE] = enabled
        }
    }

    suspend fun setBiometricLockEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.BIOMETRIC_LOCK_ENABLED] = enabled
        }
    }

    suspend fun setAppLockEnabled(enabled: Boolean) {
        setBiometricLockEnabled(enabled)
    }

    suspend fun setDailyClosingNotifEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.DAILY_CLOSING_NOTIF_ENABLED] = enabled
        }
    }

    suspend fun setAutoCloudSyncEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.AUTO_CLOUD_SYNC_ENABLED] = enabled
        }
    }

    suspend fun recordCloudSyncSuccess(timestamp: Long = System.currentTimeMillis()) {
        context.dataStore.edit { preferences ->
            preferences[Keys.LAST_CLOUD_SYNC_TIMESTAMP] = timestamp
        }
    }

    suspend fun recordDailyActivity(): Int {
        var updatedStreak = 1
        context.dataStore.edit { preferences ->
            val currentDay = System.currentTimeMillis() / 86400000L
            val lastDay = preferences[Keys.LAST_ACTIVE_EPOCH_DAY] ?: 0L
            val currentStreak = preferences[Keys.LEDGER_STREAK] ?: 1
            val bestStreak = preferences[Keys.BEST_STREAK] ?: 1

            if (lastDay == currentDay) {
                updatedStreak = currentStreak
            } else if (lastDay == currentDay - 1) {
                updatedStreak = currentStreak + 1
                preferences[Keys.LEDGER_STREAK] = updatedStreak
                preferences[Keys.LAST_ACTIVE_EPOCH_DAY] = currentDay
                if (updatedStreak > bestStreak) {
                    preferences[Keys.BEST_STREAK] = updatedStreak
                }
            } else {
                updatedStreak = 1
                preferences[Keys.LEDGER_STREAK] = 1
                preferences[Keys.LAST_ACTIVE_EPOCH_DAY] = currentDay
            }
        }
        return updatedStreak
    }

    suspend fun setPosBiometricLockEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.POS_BIOMETRIC_LOCK_ENABLED] = enabled
        }
    }

    suspend fun setMerchantPosPin(pin: String) {
        context.dataStore.edit { preferences ->
            preferences[Keys.MERCHANT_POS_PIN] = pin
        }
    }
}
