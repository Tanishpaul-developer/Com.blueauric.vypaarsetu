package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import java.io.File
import com.example.auth.AppLockManager
import com.example.ui.components.NavScreen
import com.example.ui.navigation.VyaparSetuNavGraph
import com.example.ui.theme.VyaparSetuTheme
import com.example.ui.viewmodel.MainViewModel

class MainActivity : FragmentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private lateinit var appLockManager: AppLockManager

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        // Permissions handled gracefully
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        appLockManager = AppLockManager(applicationContext, viewModel.preferencesRepository)
        lifecycle.addObserver(appLockManager)

        // Pre-create WebView cache directories to prevent Chromium disk cache index warnings
        ensureWebViewCacheDirs()

        // Request runtime permissions for notifications and location targeting
        requestAppPermissions()

        // Initialize Monetization Ad SDKs (InMobi Primary + Start.io Fallback)
        com.example.monetization.AdSdkInitializer.initialize(applicationContext)

        val navScreenExtra = intent?.getStringExtra("NAV_SCREEN")

        setContent {
            val profile by viewModel.merchantProfile.collectAsState()

            VyaparSetuTheme(darkTheme = profile.isDarkMode) {
                VyaparSetuNavGraph(
                    viewModel = viewModel,
                    appLockManager = appLockManager,
                    initialScreen = if (navScreenExtra == "DAILY_SUMMARY") NavScreen.DAILY_SUMMARY else NavScreen.HOME
                )
            }
        }
    }

    private fun requestAppPermissions() {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (permissions.isNotEmpty()) {
            permissionLauncher.launch(permissions.toTypedArray())
        }
    }

    private fun ensureWebViewCacheDirs() {
        try {
            val jsCacheDir = File(cacheDir, "WebView/Default/HTTP Cache/Code Cache/js")
            if (!jsCacheDir.exists()) jsCacheDir.mkdirs()
            val wasmCacheDir = File(cacheDir, "WebView/Default/HTTP Cache/Code Cache/wasm")
            if (!wasmCacheDir.exists()) wasmCacheDir.mkdirs()
        } catch (_: Exception) {
            // Ignore filesystem creation errors
        }
    }
}

