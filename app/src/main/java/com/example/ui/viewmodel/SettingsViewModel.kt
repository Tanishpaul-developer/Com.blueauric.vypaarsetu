package com.example.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.GeminiClient
import com.example.data.model.LanguageOption
import com.example.data.model.MerchantProfile
import com.example.data.model.SupportedLanguages
import com.example.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * UI state representation for Language Selection.
 */
data class LanguageSelectionState(
    val selectedLanguage: LanguageOption,
    val availableLanguages: List<LanguageOption> = SupportedLanguages,
    val isUpdating: Boolean = false,
    val statusMessage: String? = null
)

/**
 * Dedicated LanguageSelection state manager.
 * Encapsulates language state, persistence in UserPreferencesRepository,
 * and automatic synchronization with GeminiClient AI locale-specific prompts.
 */
class LanguageSelection(
    private val preferencesRepository: UserPreferencesRepository,
    private val scope: kotlinx.coroutines.CoroutineScope,
    private val exceptionHandler: CoroutineExceptionHandler
) {
    private val TAG = "LanguageSelection"

    private val _isUpdating = MutableStateFlow(false)
    val isUpdating: StateFlow<Boolean> = _isUpdating.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    val currentLanguageCode: StateFlow<String> = preferencesRepository.merchantProfileFlow
        .map { it.languageCode }
        .stateIn(scope, SharingStarted.Eagerly, "hi")

    val state: StateFlow<LanguageSelectionState> = combine(
        preferencesRepository.merchantProfileFlow,
        _isUpdating,
        _statusMessage
    ) { profile, updating, message ->
        val selectedOption = SupportedLanguages.firstOrNull { it.code.equals(profile.languageCode, ignoreCase = true) }
            ?: SupportedLanguages.first { it.code == "hi" }
        LanguageSelectionState(
            selectedLanguage = selectedOption,
            availableLanguages = SupportedLanguages,
            isUpdating = updating,
            statusMessage = message
        )
    }.stateIn(
        scope,
        SharingStarted.Eagerly,
        LanguageSelectionState(
            selectedLanguage = SupportedLanguages.first { it.code == "hi" }
        )
    )

    init {
        // Automatically inject selected language into GeminiClient on startup
        scope.launch(exceptionHandler) {
            preferencesRepository.merchantProfileFlow.collect { profile ->
                GeminiClient.setLanguage(profile.languageCode)
            }
        }
    }

    /**
     * Updates the user's selected language, persists it to UserPreferencesRepository,
     * and configures the Gemini API client to inject the matching locale-specific prompts.
     */
    fun selectLanguage(langCode: String, onCompleted: (() -> Unit)? = null) {
        scope.launch(exceptionHandler) {
            try {
                _isUpdating.value = true
                val matchedOption = SupportedLanguages.firstOrNull { it.code.equals(langCode, ignoreCase = true) }
                    ?: SupportedLanguages.first { it.code == "hi" }

                // 1. Persist to preferences repository
                preferencesRepository.updateLanguage(matchedOption.code)

                // 2. Configure Gemini API client to inject locale-specific prompts
                GeminiClient.setLanguage(matchedOption.code)

                _statusMessage.value = "भाषा बदली: ${matchedOption.localName} (${matchedOption.englishName})"
                Log.i(TAG, "✓ Language updated to ${matchedOption.code}. GeminiClient prompts reconfigured for ${matchedOption.englishName}.")
                onCompleted?.invoke()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update language: ${e.message}", e)
                _statusMessage.value = "Error updating language: ${e.message}"
            } finally {
                _isUpdating.value = false
            }
        }
    }

    fun selectLanguage(option: LanguageOption, onCompleted: (() -> Unit)? = null) {
        selectLanguage(option.code, onCompleted)
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }
}

/**
 * SettingsViewModel hosting the LanguageSelection state manager
 * and core merchant configuration flows.
 */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e("SettingsViewModel", "Unhandled error: ${throwable.message}", throwable)
    }

    val preferencesRepository = UserPreferencesRepository(application)

    val merchantProfile: StateFlow<MerchantProfile> = preferencesRepository.merchantProfileFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, MerchantProfile())

    // LanguageSelection state manager instance
    val languageSelection = LanguageSelection(
        preferencesRepository = preferencesRepository,
        scope = viewModelScope,
        exceptionHandler = exceptionHandler
    )

    fun selectLanguage(langCode: String) {
        languageSelection.selectLanguage(langCode)
    }

    fun clearStatusMessage() {
        languageSelection.clearStatusMessage()
    }
}
