package com.example.auth

import android.content.Context
import android.util.Log
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.example.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AppLockManager(
    private val context: Context,
    private val preferencesRepository: UserPreferencesRepository
) : DefaultLifecycleObserver {

    private val scope = CoroutineScope(Dispatchers.Main)

    private val _isLocked = MutableStateFlow(false)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    private val _isAppLockEnabled = MutableStateFlow(false)
    val isAppLockEnabled: StateFlow<Boolean> = _isAppLockEnabled.asStateFlow()

    init {
        // Collect current setting from DataStore
        scope.launch {
            preferencesRepository.isAppLockEnabledFlow.collect { enabled ->
                _isAppLockEnabled.value = enabled
                if (!enabled) {
                    _isLocked.value = false
                }
            }
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        onAppBackgrounded()
    }

    fun onAppBackgrounded() {
        if (_isAppLockEnabled.value) {
            Log.d(TAG, "App sent to background, locking app gate")
            _isLocked.value = true
        }
        // Automatically trigger data sync to Firestore when app enters the background
        com.example.service.BackgroundSyncWorker.enqueueBackgroundSync(context)
    }

    fun lock() {
        if (_isAppLockEnabled.value) {
            _isLocked.value = true
        }
    }

    fun unlock() {
        _isLocked.value = false
    }

    fun canAuthenticate(): Boolean {
        val biometricManager = BiometricManager.from(context)
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        val result = biometricManager.canAuthenticate(authenticators)
        return result == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun authenticate(
        activity: FragmentActivity,
        title: String = "सुरक्षित ऐप लॉक (VyaparSetu App Lock)",
        subtitle: String = "खाता बही व वित्तीय रिकॉर्ड खोलने के लिए फिंगरप्रिंट, फेस या पिन/पैटर्न लगाएं",
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val biometricPrompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    unlock()
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    if (errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                        errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON
                    ) {
                        onError("प्रमाणीकरण रद्द किया गया")
                    } else {
                        onError(errString.toString())
                    }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onError("पहचान मेल नहीं खाई, कृपया पुनः प्रयास करें")
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()

        try {
            biometricPrompt.authenticate(promptInfo)
        } catch (e: Exception) {
            Log.e(TAG, "Biometric authenticate error: ${e.message}")
            onError("Biometric authentication error: ${e.localizedMessage}")
        }
    }

    suspend fun setAppLockEnabled(enabled: Boolean) {
        preferencesRepository.setAppLockEnabled(enabled)
        _isAppLockEnabled.value = enabled
        if (!enabled) {
            _isLocked.value = false
        }
    }

    companion object {
        private const val TAG = "AppLockManager"
    }
}
