package com.example.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.auth.AppLockManager
import com.example.util.AppLocalization
import com.example.util.LocalAppStrings
import com.example.ui.components.AppBottomNav
import com.example.ui.components.AppTopBar
import com.example.ui.components.AppTourOverlay
import com.example.ui.components.ConfettiExplosionOverlay
import com.example.ui.components.NavScreen
import com.example.ui.screens.AiBolScreen
import com.example.ui.screens.AppLockScreen
import com.example.ui.screens.ContactFeedbackScreen
import com.example.ui.screens.DailySummaryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.InventoryScreen
import com.example.ui.screens.InvoiceBillingScreen
import com.example.ui.screens.KhataScreen
import com.example.ui.screens.MiniStoreCatalogScreen
import com.example.ui.screens.OcrScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.PosLoginScreen
import com.example.ui.screens.PosQrScreen
import com.example.ui.screens.PrinterSetupScreen
import com.example.ui.screens.ReportsAnalyticsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TransactionHistoryScreen
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.VoiceBillingViewModel

@Composable
fun VyaparSetuNavGraph(
    viewModel: MainViewModel,
    appLockManager: AppLockManager,
    initialScreen: NavScreen = NavScreen.HOME
) {
    val profile by viewModel.merchantProfile.collectAsState()
    val isLocked by appLockManager.isLocked.collectAsState()

    var currentScreen by remember { mutableStateOf(initialScreen) }
    var openAddCustomerModal by remember { mutableStateOf(false) }

    val voiceBillingViewModel: VoiceBillingViewModel = androidx.lifecycle.viewmodel.compose.viewModel()

    val isTourActive by viewModel.isAppTourActive.collectAsState()
    val tourStep by viewModel.appTourStep.collectAsState()
    val isConfettiActive by viewModel.showConfetti.collectAsState()

    // Background App Lock Gate via DisposableEffect lifecycle observer
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_STOP) {
                appLockManager.onAppBackgrounded()
                viewModel.lockPos()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // 1. App Lock Gate: Intercept navigation when locked
    if (isLocked) {
        AppLockScreen(
            appLockManager = appLockManager,
            shopName = profile.shopName,
            onUnlocked = {
                appLockManager.unlock()
            }
        )
        return
    }

    // 2. Authentication & Onboarding Gate
    if (!profile.isOnboarded) {
        OnboardingScreen(
            initialLanguage = profile.languageCode,
            onComplete = { shop, owner, upi, phone, email, address, gst, logo, cat, lang ->
                viewModel.completeOnboarding(
                    shopName = shop,
                    ownerName = owner,
                    upiId = upi,
                    phone = phone,
                    langCode = lang,
                    address = address,
                    email = email,
                    gstNumber = gst,
                    logoUri = logo,
                    category = cat
                )
                viewModel.startAppTour()
            }
        )
        return
    }

    // 3. Back button handling: Return to Home from any secondary destination
    BackHandler(enabled = currentScreen != NavScreen.HOME) {
        currentScreen = NavScreen.HOME
    }

    val currentStrings = remember(profile.languageCode) {
        AppLocalization.getStrings(profile.languageCode)
    }

    CompositionLocalProvider(LocalAppStrings provides currentStrings) {
        // 4. Main App Scaffold
        Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            if (currentScreen != NavScreen.DAILY_SUMMARY &&
                currentScreen != NavScreen.REPORTS_ANALYTICS &&
                currentScreen != NavScreen.TRANSACTION_HISTORY &&
                currentScreen != NavScreen.SETTINGS &&
                currentScreen != NavScreen.INVOICE_BILLING &&
                currentScreen != NavScreen.PRINTER_SETUP &&
                currentScreen != NavScreen.MINI_STORE &&
                currentScreen != NavScreen.CONTACT_FEEDBACK
            ) {
                AppTopBar(
                    profile = profile,
                    onLanguageSelected = { viewModel.updateLanguage(it) },
                    onOpenSettings = { currentScreen = NavScreen.SETTINGS },
                    onSoundboxTap = {
                        viewModel.replayLastSoundboxAnnouncement()
                    },
                    onOpenContactFeedback = {
                        currentScreen = NavScreen.CONTACT_FEEDBACK
                    },
                    onToggleTheme = {
                        viewModel.setDarkMode(!profile.isDarkMode)
                    }
                )
            }
        },
        bottomBar = {
            if (currentScreen.isBottomBarTab) {
                AppBottomNav(
                    currentScreen = currentScreen,
                    onScreenSelected = { currentScreen = it }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (currentScreen.isBottomBarTab) innerPadding else PaddingValues())
        ) {
            when (currentScreen) {
                NavScreen.HOME -> HomeScreen(
                    viewModel = viewModel,
                    onNavigate = { currentScreen = it },
                    onOpenAddCustomer = {
                        currentScreen = NavScreen.KHATA
                        openAddCustomerModal = true
                    }
                )
                NavScreen.POS_QR -> {
                    val isPosUnlocked by viewModel.isPosUnlocked.collectAsState()
                    if (profile.posBiometricLockEnabled && !isPosUnlocked) {
                        PosLoginScreen(
                            viewModel = viewModel,
                            onUnlocked = { viewModel.setPosUnlocked(true) },
                            onBack = { currentScreen = NavScreen.HOME }
                        )
                    } else {
                        PosQrScreen(
                            viewModel = viewModel,
                            onLockPos = { viewModel.setPosUnlocked(false) }
                        )
                    }
                }
                NavScreen.KHATA -> KhataScreen(
                    viewModel = viewModel,
                    showAddCustomerInitially = openAddCustomerModal
                ).also {
                    openAddCustomerModal = false
                }
                NavScreen.INVENTORY -> InventoryScreen(viewModel = viewModel)
                NavScreen.OCR_SCAN -> OcrScreen(viewModel = viewModel)
                NavScreen.AI_BOL -> AiBolScreen(viewModel = viewModel)
                NavScreen.DAILY_SUMMARY -> DailySummaryScreen(
                    viewModel = viewModel,
                    onBack = { currentScreen = NavScreen.HOME }
                )
                NavScreen.REPORTS_ANALYTICS -> ReportsAnalyticsScreen(
                    viewModel = viewModel,
                    onBack = { currentScreen = NavScreen.HOME }
                )
                NavScreen.TRANSACTION_HISTORY -> TransactionHistoryScreen(
                    viewModel = viewModel,
                    onBack = { currentScreen = NavScreen.HOME }
                )
                NavScreen.SETTINGS -> SettingsScreen(
                    viewModel = viewModel,
                    appLockManager = appLockManager,
                    onBack = { currentScreen = NavScreen.HOME },
                    onOpenDailyClosing = { currentScreen = NavScreen.DAILY_SUMMARY },
                    onOpenContactFeedback = { currentScreen = NavScreen.CONTACT_FEEDBACK }
                )
                NavScreen.INVOICE_BILLING -> InvoiceBillingScreen(
                    viewModel = viewModel,
                    voiceViewModel = voiceBillingViewModel,
                    onBack = { currentScreen = NavScreen.HOME },
                    onNavigatePrinterSetup = { currentScreen = NavScreen.PRINTER_SETUP }
                )
                NavScreen.PRINTER_SETUP -> PrinterSetupScreen(
                    viewModel = viewModel,
                    onBack = { currentScreen = NavScreen.HOME }
                )
                NavScreen.MINI_STORE -> MiniStoreCatalogScreen(
                    viewModel = viewModel,
                    onBack = { currentScreen = NavScreen.HOME }
                )
                NavScreen.CONTACT_FEEDBACK -> ContactFeedbackScreen(
                    viewModel = viewModel,
                    onBack = { currentScreen = NavScreen.HOME }
                )
            }

            // Interactive Guided Onboarding Tour Overlay
            AppTourOverlay(
                active = isTourActive,
                currentStep = tourStep,
                onNextStep = { viewModel.nextAppTourStep() },
                onDismiss = { viewModel.dismissAppTour() }
            )

            // Full-Screen Particle Confetti Explosion Overlay
            ConfettiExplosionOverlay(
                active = isConfettiActive,
                durationMillis = 3200L,
                onFinished = { viewModel.dismissConfetti() }
            )
        }
    }
}
}
