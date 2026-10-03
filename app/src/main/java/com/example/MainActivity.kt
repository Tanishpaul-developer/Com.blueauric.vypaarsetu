package com.example

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.fragment.app.FragmentActivity
import com.example.auth.AppLockManager
import com.example.ui.components.NavScreen
import com.example.ui.navigation.VyaparSetuNavGraph
import com.example.ui.theme.VyaparSetuTheme
import com.example.ui.viewmodel.MainViewModel

class MainActivity : FragmentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private lateinit var appLockManager: AppLockManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        appLockManager = AppLockManager(applicationContext, viewModel.preferencesRepository)
        lifecycle.addObserver(appLockManager)

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
}

