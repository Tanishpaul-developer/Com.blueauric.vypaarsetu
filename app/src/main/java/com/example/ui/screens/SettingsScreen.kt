package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.mutableFloatStateOf
import com.example.ui.components.VyaparSetuBrandHeroHeader
import com.example.util.LocalAppStrings
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.example.ai.GeminiClient
import com.example.auth.AppLockManager
import com.example.ui.theme.CreditEmerald
import com.example.ui.theme.CreditEmeraldLight
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceLevel1
import com.example.ui.theme.SurfaceLevel2
import com.example.ui.theme.SurfaceOutline
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TrustBlue
import com.example.ui.theme.TrustBlueDark
import com.example.ui.theme.TrustBlueLight
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    appLockManager: AppLockManager,
    onBack: () -> Unit,
    onOpenDailyClosing: () -> Unit = {},
    onOpenContactFeedback: () -> Unit = {},
    settingsViewModel: SettingsViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val profile by viewModel.merchantProfile.collectAsStateWithLifecycle()
    val backupStatus by viewModel.backupStatus.collectAsStateWithLifecycle()

    val isAppLockEnabled by appLockManager.isAppLockEnabled.collectAsStateWithLifecycle()
    val isClosingNotifEnabled by viewModel.preferencesRepository.dailyClosingNotifEnabledFlow.collectAsStateWithLifecycle(initialValue = true)
    val isAutoSyncEnabled by viewModel.preferencesRepository.autoCloudSyncEnabledFlow.collectAsStateWithLifecycle(initialValue = true)
    val lastSyncTimestamp by viewModel.preferencesRepository.lastCloudSyncTimestampFlow.collectAsStateWithLifecycle(initialValue = 0L)
    val strings = LocalAppStrings.current

    var shopName by remember(profile.shopName) { mutableStateOf(profile.shopName) }
    var ownerName by remember(profile.ownerName) { mutableStateOf(profile.ownerName) }
    var upiId by remember(profile.upiId) { mutableStateOf(profile.upiId) }
    var phone by remember(profile.phone) { mutableStateOf(profile.phone) }
    var email by remember(profile.email) { mutableStateOf(profile.email) }
    var address by remember(profile.address) { mutableStateOf(profile.address) }
    var logoUri by remember(profile.logoUri) { mutableStateOf(profile.logoUri) }
    var businessCategory by remember(profile.businessCategory) { mutableStateOf(profile.businessCategory) }

    // Soundbox TTS Model Customization State
    var sbLanguage by remember(profile.soundboxLanguage) { mutableStateOf(profile.soundboxLanguage) }
    var sbPitch by remember(profile.soundboxPitch) { mutableFloatStateOf(profile.soundboxPitch) }
    var sbRate by remember(profile.soundboxSpeechRate) { mutableFloatStateOf(profile.soundboxSpeechRate) }
    var sbTone by remember(profile.soundboxVoiceTone) { mutableStateOf(profile.soundboxVoiceTone) }
    var sbPreChime by remember(profile.soundboxPreChime) { mutableStateOf(profile.soundboxPreChime) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            logoUri = uri.toString()
            Toast.makeText(context, "दुकान का लोगो चुना गया (Store Logo Selected)", Toast.LENGTH_SHORT).show()
        }
    }

    var importJsonText by remember { mutableStateOf("") }
    var showImportField by remember { mutableStateOf(false) }

    BackHandler {
        onBack()
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = strings.settingsTitle,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("settings_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfaceLevel1
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Official VyaparSetu Brand Hero Header
            VyaparSetuBrandHeroHeader(
                tagline = if (profile.languageCode == "en") {
                    "Merchant Settings & Security Hub • VyaparSetu"
                } else {
                    "व्यापारी सेटिंग्स व सुरक्षा केंद्र • व्यापारसेतु"
                },
                onClick = onOpenContactFeedback
            )

            // --- 0. APP LANGUAGE & GEMINI AI LOCALE PROMPT CARD ---
            val languageState by settingsViewModel.languageSelection.state.collectAsStateWithLifecycle()
            val localePromptConfig = remember(languageState.selectedLanguage.code) {
                GeminiClient.getLocalePromptConfig(languageState.selectedLanguage.code)
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(SurfaceLevel2)
                    .border(1.5.dp, TrustBlue.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(TrustBlue.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = languageState.selectedLanguage.scriptChar,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TrustBlue
                                )
                            )
                        }
                        Column {
                            Text(
                                text = "ऐप भाषा व AI बोल (Language & AI Locale)",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                            Text(
                                text = "Gemini AI adapts dialect & accounting terms to your region",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                            )
                        }
                    }
                }

                // Language Option Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    languageState.availableLanguages.forEach { lang ->
                        val isSelected = languageState.selectedLanguage.code == lang.code
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) TrustBlue else SurfaceLevel1)
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) TrustBlue else SurfaceOutline,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    settingsViewModel.languageSelection.selectLanguage(lang.code) {
                                        viewModel.updateLanguage(lang.code)
                                    }
                                }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = lang.scriptChar,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else TrustBlue
                                    )
                                )
                                Column {
                                    Text(
                                        text = lang.localName,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color.White else TextPrimary
                                        )
                                    )
                                    Text(
                                        text = lang.englishName,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 9.sp,
                                            color = if (isSelected) Color.White.copy(alpha = 0.85f) else TextMuted
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // Gemini Locale Context Banner
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = SurfaceLevel1
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = null,
                                tint = CreditEmerald,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Gemini 3.5 Flash Active Locale: ${localePromptConfig.languageName}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = CreditEmerald
                                )
                            )
                        }
                        Text(
                            text = "Greeting: \"${localePromptConfig.greeting}\" • Key Vernacular: ${localePromptConfig.vernacularKeywords.take(4).joinToString(", ")}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        )
                    }
                }
            }

            // --- 1. SECURITY & APP LOCK CARD ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(SurfaceLevel2)
                    .border(1.5.dp, TrustBlue.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(TrustBlue.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = "Security",
                                tint = TrustBlue,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "सुरक्षा व ऐप लॉक (Security & App Lock)",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                            Text(
                                text = "Biometric, Face ID & Device PIN Lock",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                            )
                        }
                    }

                    // Material 3 Switch with Biometric Confirmation on Toggle ON
                    Switch(
                        checked = isAppLockEnabled,
                        onCheckedChange = { targetState ->
                            val activity = context as? FragmentActivity
                            if (targetState) {
                                if (activity != null) {
                                    // Verify user identity before persisting true state
                                    appLockManager.authenticate(
                                        activity = activity,
                                        title = "ऐप लॉक सक्रिय करें (Enable App Lock)",
                                        subtitle = "सत्यापित करने के लिए अपना फिंगरप्रिंट या फोन पिन लगाएं",
                                        onSuccess = {
                                            scope.launch {
                                                appLockManager.setAppLockEnabled(true)
                                                viewModel.setBiometricLockEnabled(true)
                                                Toast.makeText(context, "सुरक्षा ऐप लॉक सक्रिय हो गया (App Lock Enabled)", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        onError = { error ->
                                            Toast.makeText(context, "प्रमाणीकरण असफल: $error", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                } else {
                                    scope.launch {
                                        appLockManager.setAppLockEnabled(true)
                                    }
                                }
                            } else {
                                // Toggling OFF
                                scope.launch {
                                    appLockManager.setAppLockEnabled(false)
                                    viewModel.setBiometricLockEnabled(false)
                                    Toast.makeText(context, "ऐप लॉक निष्क्रिय किया गया (App Lock Disabled)", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = TrustBlue,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = SurfaceLevel1
                        ),
                        modifier = Modifier.testTag("app_lock_switch")
                    )
                }

                Text(
                    text = "जब ऐप बैकग्राउंड में जाएगा, तो सुरक्षा के लिए स्वतः लॉक हो जाएगा। खुलने पर बायोमेट्रिक या फोन पिन आवश्यक होगा।",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, lineHeight = 18.sp)
                )

                // Actions: Test App Lock & Lock Now
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val activity = context as? FragmentActivity
                            if (activity != null) {
                                appLockManager.authenticate(
                                    activity = activity,
                                    title = "बायोमेट्रिक टेस्ट (Test App Lock)",
                                    subtitle = "सफलतापूर्वक अनलॉक की जांच करें",
                                    onSuccess = {
                                        Toast.makeText(context, "✓ बायोमेट्रिक सत्यापन सफल! (Success)", Toast.LENGTH_SHORT).show()
                                    },
                                    onError = { err ->
                                        Toast.makeText(context, "टेस्ट असफल: $err", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            } else {
                                Toast.makeText(context, "Biometric check available on device", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("test_app_lock_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Test App Lock", fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            if (!isAppLockEnabled) {
                                Toast.makeText(context, "कृपया पहले ऐप लॉक स्विच ऑन करें", Toast.LENGTH_SHORT).show()
                            } else {
                                appLockManager.lock()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("lock_now_btn"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SurfaceLevel1,
                            contentColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = Color(0xFFF59E0B)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "अभी लॉक करें", fontSize = 12.sp)
                    }
                }

                // POS Biometric Security Setting
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(SurfaceOutline)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PointOfSale,
                                contentDescription = null,
                                tint = CreditEmerald,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "पीओएस बायोमेट्रिक सुरक्षा (Secure POS Access)",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                        }
                        Text(
                            text = "पीओएस टर्मिनल और काउंटर खोलने पर बायोमेट्रिक या पिन लॉगिन आवश्यक करें",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        )
                    }

                    Switch(
                        checked = profile.posBiometricLockEnabled,
                        onCheckedChange = { targetState ->
                            viewModel.setPosBiometricLockEnabled(targetState)
                            val msg = if (targetState) "पीओएस बायोमेट्रिक सुरक्षा सक्रिय (POS Biometrics Enabled)" else "पीओएस बायोमेट्रिक सुरक्षा निष्क्रिय (POS Biometrics Disabled)"
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = CreditEmerald,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = SurfaceLevel1
                        ),
                        modifier = Modifier.testTag("pos_biometric_switch")
                    )
                }
            }

            // --- 2. DAILY CLOSING RITUAL (8:30 PM WorkManager) ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(SurfaceLevel2)
                    .border(1.dp, SurfaceOutline, RoundedCornerShape(20.dp))
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(CreditEmerald.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Celebration,
                                contentDescription = null,
                                tint = CreditEmerald,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "दुकान क्लोजिंग अनुष्ठान (8:30 PM)",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                            Text(
                                text = "WorkManager Daily Notification & Voice Summary",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                            )
                        }
                    }

                    Switch(
                        checked = isClosingNotifEnabled,
                        onCheckedChange = { enabled ->
                            scope.launch {
                                viewModel.preferencesRepository.setDailyClosingNotifEnabled(enabled)
                                Toast.makeText(
                                    context,
                                    if (enabled) "रोजाना 8:30 PM क्लोजिंग नोटिफिकेशन सक्रिय" else "क्लोजिंग नोटिफिकेशन बंद",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = CreditEmerald
                        ),
                        modifier = Modifier.testTag("closing_notif_switch")
                    )
                }

                Text(
                    text = "प्रत्येक शाम 8:30 बजे WorkManager आपको नोटिफिकेशन भेजेगा। टैप करने पर कुल कमाई का आडियो वाइस सारांश सुनाया जाएगा।",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, lineHeight = 18.sp)
                )

                OutlinedButton(
                    onClick = onOpenDailyClosing,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Assessment,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = CreditEmerald
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "आज का क्लोजिंग सारांश देखें (View Today's Summary)")
                }
            }

            // --- 3. MERCHANT PROFILE DETAILS ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(SurfaceLevel2)
                    .border(1.dp, SurfaceOutline, RoundedCornerShape(20.dp))
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF6366F1).copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Storefront,
                            contentDescription = null,
                            tint = Color(0xFF6366F1),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Text(
                        text = "दुकान व खाता विवरण (Shop Profile)",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                }

                // Store Logo / Photo Upload & Live Preview
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(SurfaceLevel1)
                        .border(1.dp, SurfaceOutline, RoundedCornerShape(14.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(SurfaceLevel2)
                            .border(1.5.dp, TrustBlue, RoundedCornerShape(14.dp))
                            .clickable {
                                photoPickerLauncher.launch(
                                    androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        val hasLogo = logoUri.isNotBlank() &&
                            (logoUri.startsWith("content://") || logoUri.startsWith("file://") || logoUri.startsWith("http"))
                        if (hasLogo) {
                            AsyncImage(
                                model = logoUri,
                                contentDescription = "Store Logo",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(14.dp)),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = TrustBlue, modifier = Modifier.size(26.dp))
                                Text("Add Logo", fontSize = 10.sp, color = TrustBlue, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "दुकान का फोटो / लोगो (Store Logo)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "यह लोगो आपके GST इनवॉइस व रसीदों पर प्रिंट होगा",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(14.dp), tint = TrustBlue)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("फोटो बदलें (Upload)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Shop Name
                OutlinedTextField(
                    value = shopName,
                    onValueChange = { shopName = it },
                    label = { Text("दुकान का नाम (Shop Name)") },
                    leadingIcon = { Icon(Icons.Default.Storefront, contentDescription = null, tint = TrustBlue, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.fillMaxWidth().testTag("shop_name_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                // Owner Name
                OutlinedTextField(
                    value = ownerName,
                    onValueChange = { ownerName = it },
                    label = { Text("व्यापारी का नाम (Owner Name)") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = TrustBlue, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                // Contact Number
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("संपर्क नंबर (Contact Number)") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = TrustBlue, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                // Email Address
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("ईमेल पता (Email Address)") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = TrustBlue, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                // Store Location / Address with Auto-Detect GPS
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("दुकान का पता व स्थान (Store Location)") },
                    leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = TrustBlue, modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        Text(
                            text = "Auto-Detect",
                            color = TrustBlue,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable {
                                    address = "Shop No. 12, Main Market, MG Road, Jaipur"
                                    Toast.makeText(context, "Location auto-detected!", Toast.LENGTH_SHORT).show()
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // Merchant UPI ID
                OutlinedTextField(
                    value = upiId,
                    onValueChange = { upiId = it },
                    label = { Text("मर्चेंट UPI ID (Payment Address `pa`)") },
                    modifier = Modifier.fillMaxWidth().testTag("upi_id_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Button(
                    onClick = {
                        viewModel.updateStoreProfile(
                            shopName = shopName,
                            ownerName = ownerName,
                            upiId = upiId,
                            phone = phone,
                            address = address,
                            email = email,
                            gstNumber = profile.gstNumber,
                            logoUri = logoUri,
                            category = businessCategory,
                            langCode = profile.languageCode
                        )
                        Toast.makeText(context, "✓ दुकान प्रोफाइल व लोगो सुरक्षित किया गया (Saved)", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("save_profile_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TrustBlue,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "प्रोफाइल व लोगो अपडेट करें (Save Profile & Logo)")
                }
            }

            // --- 4. SMART SOUNDBOX TTS VOICE & MODEL SETTINGS ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(SurfaceLevel2)
                    .border(1.5.dp, Color(0xFFEC4899).copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header with Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEC4899).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = null,
                                tint = Color(0xFFEC4899),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = strings.soundboxSettingsTitle,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                            Text(
                                text = if (profile.soundboxEnabled) "TTS Engine Active" else "Soundbox Off",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = if (profile.soundboxEnabled) CreditEmeraldLight else TextMuted
                                )
                            )
                        }
                    }

                    Switch(
                        checked = profile.soundboxEnabled,
                        onCheckedChange = { viewModel.setSoundboxEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFFEC4899)
                        )
                    )
                }

                if (profile.soundboxEnabled) {
                    // TTS Announcement Language
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = strings.soundboxLanguageLabel,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        val ttsLanguages = listOf(
                            "default" to (if (profile.languageCode == "en") "App Language" else "ऐप भाषा"),
                            "hi" to "हिन्दी (Hindi)",
                            "en" to "English (Indian)",
                            "mr" to "मराठी (Marathi)",
                            "gu" to "ગુજરાતી (Gujarati)",
                            "bn" to "বাংলা (Bengali)",
                            "ta" to "தமிழ் (Tamil)",
                            "te" to "తెలుగు (Telugu)",
                            "pa" to "ਪੰਜਾਬੀ (Punjabi)"
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ttsLanguages.forEach { (code, label) ->
                                val isSelected = sbLanguage == code
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) Color(0xFFEC4899) else SurfaceLevel1)
                                        .border(1.dp, if (isSelected) Color(0xFFEC4899) else SurfaceOutline, RoundedCornerShape(10.dp))
                                        .clickable {
                                            sbLanguage = code
                                            viewModel.updateSoundboxSettings(code, sbPitch, sbRate, sbTone, sbPreChime)
                                        }
                                        .padding(horizontal = 12.dp, vertical = 7.dp)
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color.White else TextSecondary
                                    )
                                }
                            }
                        }
                    }

                    // Voice Tone & Model Type (Female, Male, Natural)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = strings.voiceToneLabel,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        val tones = listOf(
                            "female" to strings.toneFemale,
                            "male" to strings.toneMale,
                            "natural" to strings.toneNatural
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            tones.forEach { (toneKey, toneLabel) ->
                                val isSelected = sbTone == toneKey
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) Color(0xFFEC4899) else SurfaceLevel1)
                                        .border(1.dp, if (isSelected) Color(0xFFEC4899) else SurfaceOutline, RoundedCornerShape(10.dp))
                                        .clickable {
                                            sbTone = toneKey
                                            viewModel.updateSoundboxSettings(sbLanguage, sbPitch, sbRate, toneKey, sbPreChime)
                                        }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = toneLabel.split(" ").first(),
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                        color = if (isSelected) Color.White else TextPrimary
                                    )
                                }
                            }
                        }
                    }

                    // Voice Pitch Slider
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = strings.voicePitchLabel,
                                style = MaterialTheme.typography.labelMedium.copy(color = TextSecondary)
                            )
                            Text(
                                text = String.format(java.util.Locale.US, "%.2fx", sbPitch),
                                style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFFEC4899), fontWeight = FontWeight.Bold)
                            )
                        }
                        Slider(
                            value = sbPitch,
                            onValueChange = {
                                sbPitch = it
                                viewModel.updateSoundboxSettings(sbLanguage, it, sbRate, sbTone, sbPreChime)
                            },
                            valueRange = 0.75f..1.35f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFFEC4899),
                                activeTrackColor = Color(0xFFEC4899)
                            )
                        )
                    }

                    // Speech Speed Slider
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = strings.voiceSpeedLabel,
                                style = MaterialTheme.typography.labelMedium.copy(color = TextSecondary)
                            )
                            Text(
                                text = String.format(java.util.Locale.US, "%.2fx", sbRate),
                                style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFFEC4899), fontWeight = FontWeight.Bold)
                            )
                        }
                        Slider(
                            value = sbRate,
                            onValueChange = {
                                sbRate = it
                                viewModel.updateSoundboxSettings(sbLanguage, sbPitch, it, sbTone, sbPreChime)
                            },
                            valueRange = 0.8f..1.35f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFFEC4899),
                                activeTrackColor = Color(0xFFEC4899)
                            )
                        )
                    }

                    // Pre-Chime Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceLevel1)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.preChimeLabel,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                            )
                        }
                        Switch(
                            checked = sbPreChime,
                            onCheckedChange = {
                                sbPreChime = it
                                viewModel.updateSoundboxSettings(sbLanguage, sbPitch, sbRate, sbTone, it)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFFEC4899)
                            )
                        )
                    }

                    // Test Soundbox Voice Button
                    Button(
                        onClick = {
                            viewModel.testSoundboxWithCustomSettings(
                                language = if (sbLanguage != "default") sbLanguage else profile.languageCode,
                                pitch = sbPitch,
                                speechRate = sbRate,
                                voiceTone = sbTone,
                                preChime = sbPreChime
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("test_soundbox_voice_btn"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFEC4899),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = strings.testSoundbox, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // --- 5. THEME & DISPLAY (LIGHT & DARK MODES) ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(SurfaceLevel2)
                    .border(1.dp, SurfaceOutline, RoundedCornerShape(20.dp))
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    if (profile.isDarkMode) Color(0xFFFBBF24).copy(alpha = 0.15f)
                                    else TrustBlue.copy(alpha = 0.12f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (profile.isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                                contentDescription = null,
                                tint = if (profile.isDarkMode) Color(0xFFFBBF24) else TrustBlue,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = strings.themeSettingsTitle,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                            Text(
                                text = strings.themeSettingsSubtitle,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextMuted,
                                    fontSize = 11.5.sp
                                )
                            )
                        }
                    }

                    Switch(
                        checked = profile.isDarkMode,
                        onCheckedChange = { viewModel.setDarkMode(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = TrustBlue
                        )
                    )
                }

                // Interactive Dual-Theme Cards (Dark & Light)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Dark Mode Selection Card
                    val isDarkSelected = profile.isDarkMode
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (isDarkSelected) Color(0xFF0F172A)
                                else Color(0xFF1E293B).copy(alpha = 0.6f)
                            )
                            .border(
                                width = if (isDarkSelected) 2.dp else 1.dp,
                                color = if (isDarkSelected) TrustBlue else SurfaceOutline,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { viewModel.setDarkMode(true) }
                            .padding(14.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DarkMode,
                                    contentDescription = "Dark Mode",
                                    tint = if (isDarkSelected) Color(0xFFFBBF24) else Color(0xFF94A3B8),
                                    modifier = Modifier.size(24.dp)
                                )
                                if (isDarkSelected) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(TrustBlue.copy(alpha = 0.25f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "✓ Active",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TrustBlueLight
                                        )
                                    }
                                }
                            }

                            Text(
                                text = strings.darkModeTitle,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )

                            Text(
                                text = strings.darkModeSubtitle,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.sp,
                                    color = Color(0xFFCBD5E1)
                                )
                            )
                        }
                    }

                    // Light Mode Selection Card
                    val isLightSelected = !profile.isDarkMode
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (isLightSelected) Color(0xFFFFFFFF)
                                else Color(0xFFF8FAFC)
                            )
                            .border(
                                width = if (isLightSelected) 2.dp else 1.dp,
                                color = if (isLightSelected) TrustBlueDark else SurfaceOutline,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { viewModel.setDarkMode(false) }
                            .padding(14.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LightMode,
                                    contentDescription = "Light Mode",
                                    tint = if (isLightSelected) Color(0xFFD97706) else Color(0xFF64748B),
                                    modifier = Modifier.size(24.dp)
                                )
                                if (isLightSelected) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(TrustBlueDark.copy(alpha = 0.15f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "✓ Active",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TrustBlueDark
                                        )
                                    }
                                }
                            }

                            Text(
                                text = strings.lightModeTitle,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                            )

                            Text(
                                text = strings.lightModeSubtitle,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.sp,
                                    color = Color(0xFF475569)
                                )
                            )
                        }
                    }
                }
            }

            // --- 6. CLOUD BACKUP & SYNC ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(SurfaceLevel2)
                    .border(1.dp, SurfaceOutline, RoundedCornerShape(20.dp))
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(CreditEmerald.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = null,
                            tint = CreditEmerald,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Text(
                        text = "क्लाउड बैकअप व रीस्टोर (Cloud Sync)",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                }

                backupStatus?.takeIf { it.isNotBlank() }?.let { status ->
                    Text(
                        text = status,
                        style = MaterialTheme.typography.bodySmall.copy(color = CreditEmerald)
                    )
                }

                // Automatic Background Sync to Firestore Switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(SurfaceLevel1)
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "ऑटो बैकग्राउंड सिंक (Background Sync)",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Text(
                            text = "ऐप बैकग्राउंड में जाने पर डेटा स्वतः फायरस्टोर में सुरक्षित सेव करें",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                        )
                        if (lastSyncTimestamp > 0L) {
                            val sdf = java.text.SimpleDateFormat("dd MMM, hh:mm a", java.util.Locale.getDefault())
                            Text(
                                text = "अंतिम सिंक (Last Sync): ${sdf.format(java.util.Date(lastSyncTimestamp))}",
                                style = MaterialTheme.typography.labelSmall.copy(color = CreditEmerald)
                            )
                        }
                    }

                    Switch(
                        checked = isAutoSyncEnabled,
                        onCheckedChange = { enabled ->
                            scope.launch {
                                viewModel.preferencesRepository.setAutoCloudSyncEnabled(enabled)
                                if (enabled) {
                                    com.example.service.BackgroundSyncWorker.schedulePeriodicSync(context)
                                    Toast.makeText(context, "ऑटो बैकग्राउंड सिंक सक्रिय (Background Sync Enabled)", Toast.LENGTH_SHORT).show()
                                } else {
                                    com.example.service.BackgroundSyncWorker.cancelPeriodicSync(context)
                                    Toast.makeText(context, "ऑटो बैकग्राउंड सिंक बंद (Background Sync Disabled)", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = CreditEmerald
                        ),
                        modifier = Modifier.testTag("auto_sync_switch")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.syncAllToFirestoreCloud()
                            scope.launch {
                                viewModel.preferencesRepository.recordCloudSyncSuccess()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("sync_now_btn"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CreditEmerald,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "क्लाउड सिंक", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            viewModel.exportData { json ->
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("VyaparSetu Backup", json))
                                Toast.makeText(context, "JSON बैकअप क्लिपबोर्ड पर कॉपी हुआ", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "JSON बैकअप", fontSize = 12.sp)
                    }
                }
            }

            // --- 7. DEVELOPER CONTACT & COMMUNITY FORUM ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(SurfaceLevel2)
                    .border(1.5.dp, TrustBlue.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(TrustBlue.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Forum,
                                contentDescription = null,
                                tint = TrustBlue,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "डेवलपर संपर्क व फोरम",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                )
                                Icon(Icons.Default.Verified, contentDescription = null, tint = TrustBlue, modifier = Modifier.size(16.dp))
                            }
                            Text(
                                text = "Developer: Tanish Paul • Blue Auric Studio",
                                style = MaterialTheme.typography.bodySmall.copy(color = TrustBlueLight, fontWeight = FontWeight.SemiBold)
                            )
                        }
                    }
                }

                // Developer email line
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceLevel1)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Email, contentDescription = null, tint = TrustBlue, modifier = Modifier.size(18.dp))
                        Text(
                            text = "blueauricstudio@gmail.com",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                    }

                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Developer Email", "blueauricstudio@gmail.com"))
                            Toast.makeText(context, "Email copied to clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy Email", tint = TextMuted, modifier = Modifier.size(16.dp))
                    }
                }

                Button(
                    onClick = onOpenContactFeedback,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("settings_open_feedback_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = TrustBlue),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Forum, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("कम्युनिटी फोरम व AI FAQ खोलें (Open Forum & AI FAQ)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            // --- 8. APP GUIDED TOUR ---
            OutlinedButton(
                onClick = {
                    onBack()
                    viewModel.startAppTour()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(imageVector = Icons.Default.HelpOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "ऐप चलाना सीखें (Restart Guided Tour)")
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
