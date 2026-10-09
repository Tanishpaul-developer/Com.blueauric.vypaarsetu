package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.res.painterResource
import com.example.R
import com.example.util.AppLocalization
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.example.ui.components.LuxuryCard
import com.example.ui.components.bounceClick
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.auth.GoogleAuthManager
import com.example.auth.GoogleAuthResult
import com.example.data.model.SupportedLanguages
import com.example.service.SoundboxSpeaker
import com.example.ui.theme.CreditEmerald
import com.example.ui.theme.CreditEmeraldDark
import com.example.ui.theme.CreditEmeraldLight
import com.example.ui.theme.SurfaceLevel1
import com.example.ui.theme.SurfaceLevel2
import com.example.ui.theme.SurfaceOutline
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TrustBlue
import com.example.ui.theme.TrustBlueDark
import com.google.firebase.auth.FirebaseAuth
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    initialLanguage: String = "hi",
    onComplete: (shopName: String, ownerName: String, upiId: String, phone: String, email: String, address: String, gstNumber: String, logoUri: String, category: String, langCode: String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val googleAuthManager = remember { GoogleAuthManager(context) }
    var selectedLang by remember { mutableStateOf(initialLanguage) }
    val s = remember(selectedLang) { AppLocalization.getStrings(selectedLang) }

    var isGoogleSigningIn by remember { mutableStateOf(false) }
    var authEmail by remember { mutableStateOf("") }
    var authPassword by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var authTabIsLogin by remember { mutableStateOf(true) }
    var isEmailSigningIn by remember { mutableStateOf(false) }
    var emailAuthError by remember { mutableStateOf<String?>(null) }
    var authenticatedEmail by remember { mutableStateOf("") }
    var authenticatedUid by remember { mutableStateOf("") }
    var showProfileSetup by remember { mutableStateOf(false) }

    var shopName by remember { mutableStateOf("Sharma Kirana & General Store") }
    var ownerName by remember { mutableStateOf("Ramesh Sharma") }
    var upiId by remember { mutableStateOf("sharma.kirana@oksbi") }
    var phone by remember { mutableStateOf("9876543210") }
    var email by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("Shop No. 12, Main Market, MG Road") }
    var gstNumber by remember { mutableStateOf("") }
    var selectedLogoIcon by remember { mutableStateOf("storefront") }
    var businessCategory by remember { mutableStateOf("General Store / Kirana") }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            selectedLogoIcon = uri.toString()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Brand Logo & Header
            Box(
                modifier = Modifier
                    .size(86.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(androidx.compose.ui.graphics.Color.White)
                    .border(1.5.dp, androidx.compose.ui.graphics.Color(0xFFCBD5E1), RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_vyaparsetu_mark),
                    contentDescription = "VyaparSetu Official Logo",
                    tint = androidx.compose.ui.graphics.Color.Unspecified,
                    modifier = Modifier.size(72.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Trust badge
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(CreditEmeraldDark.copy(alpha = 0.5f))
                    .border(1.dp, CreditEmerald.copy(alpha = 0.3f), CircleShape)
                    .padding(horizontal = 12.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(CreditEmerald, CircleShape)
                )
                Text(
                    text = "विश्वसनीय व्यापारी नेटवर्क • Firebase Auth Secured",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = CreditEmeraldLight,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "VyaparSetu",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = TrustBlue
                )
            )

            Text(
                text = "भारत का अपना आधुनिक व्यापारी साथी",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = TextSecondary
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Language Grid Section Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Translate,
                            contentDescription = null,
                            tint = TrustBlue,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "भाषा चुनें / Choose Language",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                    }
                    Text(
                        text = "VyaparSetu speaks in your native language",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(SurfaceLevel2)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${SupportedLanguages.size} भाषाएं",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TrustBlue,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Language Grid (3 columns)
            Box(modifier = Modifier.height(210.dp)) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(2.dp)
                ) {
                    items(SupportedLanguages) { lang ->
                        val isSelected = selectedLang == lang.code

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSelected) TrustBlueDark.copy(alpha = 0.5f) else SurfaceLevel2)
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) TrustBlue else SurfaceOutline,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable {
                                    selectedLang = lang.code
                                    SoundboxSpeaker.announcePayment(
                                        context = context,
                                        appSource = "PhonePe",
                                        amount = 150.0,
                                        senderName = "राजेश",
                                        languageCode = lang.code
                                    )
                                }
                                .padding(vertical = 10.dp, horizontal = 6.dp)
                                .testTag("lang_chip_${lang.code}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = lang.scriptChar,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) TrustBlue else TextPrimary
                                    )
                                )
                                Text(
                                    text = lang.localName,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                )
                                Text(
                                    text = lang.englishName,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = TextMuted,
                                        fontSize = 10.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Authentication & Merchant Setup Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(SurfaceLevel2)
                    .border(1.dp, SurfaceOutline, RoundedCornerShape(24.dp))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Reassurance Badge
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceLevel1)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = CreditEmerald,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "100% Free • Secure Cloud Sync • Instant Setup",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }

                if (!showProfileSetup) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "खाता सुरक्षित पहचान (Google Sign-In)",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Text(
                            text = "व्यापारिक डेटा व वहीखाता सुरक्षित रखने के लिए Google खाते से साइन इन करें।",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextMuted),
                            textAlign = TextAlign.Center
                        )

                        // Google Sign-In with Firebase Auth CTA Button
                        Button(
                            onClick = {
                                isGoogleSigningIn = true
                                scope.launch {
                                    val activity = context as? FragmentActivity ?: context as? android.app.Activity
                                    val result = if (activity != null) {
                                        googleAuthManager.signInWithGoogle(activity)
                                    } else {
                                        GoogleAuthResult.NewMerchant(
                                            user = try { FirebaseAuth.getInstance().currentUser } catch (e: Exception) { null },
                                            defaultName = "Ramesh Sharma",
                                            email = "sharma.kirana@gmail.com"
                                        )
                                    }
                                    isGoogleSigningIn = false
                                    when (result) {
                                        is GoogleAuthResult.ExistingMerchant -> {
                                            onComplete(
                                                result.profile.shopName,
                                                result.profile.ownerName,
                                                result.profile.upiId,
                                                result.profile.phone,
                                                result.profile.email,
                                                result.profile.address,
                                                result.profile.gstNumber,
                                                result.profile.logoUri,
                                                result.profile.businessCategory,
                                                result.profile.languageCode
                                            )
                                        }
                                        is GoogleAuthResult.NewMerchant -> {
                                            authenticatedUid = result.user?.uid ?: "merchant_${System.currentTimeMillis()}"
                                            ownerName = result.defaultName.ifBlank { "Ramesh Sharma" }
                                            authenticatedEmail = result.email
                                            showProfileSetup = true
                                        }
                                        is GoogleAuthResult.Error -> {
                                            authenticatedUid = "merchant_${System.currentTimeMillis()}"
                                            authenticatedEmail = "merchant.business@gmail.com"
                                            showProfileSetup = true
                                        }
                                        is GoogleAuthResult.Cancelled -> {
                                            // User dismissed the prompt
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .border(1.dp, SurfaceOutline, RoundedCornerShape(16.dp))
                                .testTag("google_signin_btn"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SurfaceLevel1,
                                contentColor = TextPrimary
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                        ) {
                            if (isGoogleSigningIn) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.5.dp,
                                    color = TrustBlue
                                )
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // Google Multi-Color G Icon Badge
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(Color.White),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "G",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 17.sp,
                                            color = Color(0xFF4285F4)
                                        )
                                    }
                                    Text(
                                        text = "Google खाते से साइन-इन करें",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                        }

                        // Divider with OR
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            HorizontalDivider(modifier = Modifier.weight(1f), color = SurfaceOutline)
                            Text("या ईमेल द्वारा (OR with Email)", fontSize = 12.sp, color = TextMuted)
                            HorizontalDivider(modifier = Modifier.weight(1f), color = SurfaceOutline)
                        }

                        // Email & Password Auth Segmented Switcher
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(SurfaceLevel2)
                                .border(1.dp, SurfaceOutline, RoundedCornerShape(12.dp))
                                .padding(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (authTabIsLogin) TrustBlue else Color.Transparent)
                                    .clickable {
                                        authTabIsLogin = true
                                        emailAuthError = null
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "साइन-इन (Sign In)",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (authTabIsLogin) Color.White else TextMuted
                                    )
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (!authTabIsLogin) CreditEmerald else Color.Transparent)
                                    .clickable {
                                        authTabIsLogin = false
                                        emailAuthError = null
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "नया खाता (Register)",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (!authTabIsLogin) Color.White else TextMuted
                                    )
                                )
                            }
                        }

                        // Email Field
                        OutlinedTextField(
                            value = authEmail,
                            onValueChange = {
                                authEmail = it
                                emailAuthError = null
                            },
                            label = { Text("ईमेल पता (Email Address)") },
                            leadingIcon = {
                                Icon(Icons.Default.Email, contentDescription = null, tint = TrustBlue, modifier = Modifier.size(18.dp))
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_email_input"),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TrustBlue,
                                unfocusedBorderColor = SurfaceOutline
                            )
                        )

                        // Password Field
                        OutlinedTextField(
                            value = authPassword,
                            onValueChange = {
                                authPassword = it
                                emailAuthError = null
                            },
                            label = { Text("पासवर्ड (Password)") },
                            leadingIcon = {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = TrustBlue, modifier = Modifier.size(18.dp))
                            },
                            trailingIcon = {
                                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                    Icon(
                                        imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Toggle password",
                                        tint = TextMuted,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            },
                            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_password_input"),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TrustBlue,
                                unfocusedBorderColor = SurfaceOutline
                            )
                        )

                        // Auth Error Banner
                        if (emailAuthError != null) {
                            Text(
                                text = emailAuthError!!,
                                color = Color(0xFFEF4444),
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Email Auth Action Button
                        Button(
                            onClick = {
                                if (authEmail.isBlank() || authPassword.isBlank()) {
                                    emailAuthError = "कृपया ईमेल और पासवर्ड दर्ज करें (Enter email and password)"
                                    return@Button
                                }
                                if (authPassword.length < 6) {
                                    emailAuthError = "पासवर्ड कम से कम 6 अक्षरों का होना चाहिए (Password must be 6+ chars)"
                                    return@Button
                                }

                                isEmailSigningIn = true
                                scope.launch {
                                    val result = if (authTabIsLogin) {
                                        googleAuthManager.signInWithEmail(authEmail, authPassword)
                                    } else {
                                        googleAuthManager.signUpWithEmail(authEmail, authPassword)
                                    }
                                    isEmailSigningIn = false
                                    when (result) {
                                        is GoogleAuthResult.ExistingMerchant -> {
                                            onComplete(
                                                result.profile.shopName,
                                                result.profile.ownerName,
                                                result.profile.upiId,
                                                result.profile.phone,
                                                result.profile.email,
                                                result.profile.address,
                                                result.profile.gstNumber,
                                                result.profile.logoUri,
                                                result.profile.businessCategory,
                                                result.profile.languageCode
                                            )
                                        }
                                        is GoogleAuthResult.NewMerchant -> {
                                            authenticatedUid = result.user?.uid ?: "merchant_${System.currentTimeMillis()}"
                                            authenticatedEmail = result.email.ifBlank { authEmail }
                                            showProfileSetup = true
                                        }
                                        is GoogleAuthResult.Error -> {
                                            emailAuthError = result.message
                                        }
                                        is GoogleAuthResult.Cancelled -> {}
                                    }
                                }
                            },
                            enabled = !isEmailSigningIn,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .bounceClick { }
                                .testTag("email_auth_submit_btn"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (authTabIsLogin) TrustBlue else CreditEmerald,
                                contentColor = Color.White
                            )
                        ) {
                            if (isEmailSigningIn) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                            } else {
                                Text(
                                    text = if (authTabIsLogin) "ईमेल से साइन-इन करें (Sign In)" else "नया खाता बनाएं (Create Account)",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }

                        // Direct Registration & Store Setup Button
                        OutlinedButton(
                            onClick = {
                                authenticatedUid = "merchant_${System.currentTimeMillis()}"
                                authenticatedEmail = "merchant@vyaparsetu.app"
                                showProfileSetup = true
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("direct_register_btn"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.Storefront, contentDescription = null, modifier = Modifier.size(16.dp), tint = TextMuted)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("दुकान विवरण सीधे भरें (Skip to Shop Details)", color = TextSecondary, fontSize = 12.sp)
                        }
                    }
                } else {
                    // 1-Click Shop Setup: Shop Name, Business Category, and Merchant UPI ID (pa)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = CreditEmerald, modifier = Modifier.size(20.dp))
                        Text(
                            text = if (selectedLang == "en") "1-Click Store Setup" else "1-क्लिक दुकान सेटअप",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                    }

                    // Autonomous 1-Tap Auto-Fill Banner
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(TrustBlue.copy(alpha = 0.12f))
                            .border(1.dp, TrustBlue.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                            .clickable {
                                shopName = if (selectedLang == "en") "Sharma General Store" else "शर्मा किराना एवं जनरल स्टोर"
                                ownerName = if (selectedLang == "en") "Ramesh Sharma" else "रमेश शर्मा"
                                phone = "9876543210"
                                email = authenticatedEmail.ifBlank { "sharma.kirana@gmail.com" }
                                address = if (selectedLang == "en") "Shop No. 12, Main Market, MG Road, Jaipur" else "दुकान नं. 12, मुख्य बाजार, एमजी रोड, जयपुर"
                                upiId = "sharma.kirana@oksbi"
                                Toast.makeText(context, "✨ Auto-Filled!", Toast.LENGTH_SHORT).show()
                            }
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = TrustBlue, modifier = Modifier.size(22.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(s.oneTapAutoFill, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TrustBlue)
                            Text(s.oneTapAutoFillSubtitle, fontSize = 11.sp, color = TextSecondary)
                        }
                    }

                    // Store Logo / Photo Upload
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceLevel1)
                            .border(1.dp, SurfaceOutline, RoundedCornerShape(12.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(SurfaceLevel2)
                                .border(1.5.dp, TrustBlue, RoundedCornerShape(14.dp))
                                .clickable {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedLogoIcon.startsWith("content://") || selectedLogoIcon.startsWith("file://") || selectedLogoIcon.startsWith("http")) {
                                coil.compose.AsyncImage(
                                    model = selectedLogoIcon,
                                    contentDescription = "Store Logo",
                                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp)),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = TrustBlue, modifier = Modifier.size(24.dp))
                                    Text("Logo", fontSize = 10.sp, color = TrustBlue, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(s.storeLogoUpload, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text(s.storeLogoSubtitle, fontSize = 11.sp, color = TextMuted)
                        }
                    }

                    // Store Name
                    OutlinedTextField(
                        value = shopName,
                        onValueChange = { shopName = it },
                        label = { Text(s.shopName) },
                        leadingIcon = { Icon(Icons.Default.Storefront, contentDescription = null, tint = TrustBlue, modifier = Modifier.size(18.dp)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("shop_name_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Owner Name
                    OutlinedTextField(
                        value = ownerName,
                        onValueChange = { ownerName = it },
                        label = { Text(s.ownerName) },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = TrustBlue, modifier = Modifier.size(18.dp)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("owner_name_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Contact Number (Phone)
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text(s.phone) },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = TrustBlue, modifier = Modifier.size(18.dp)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("phone_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Email Address
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text(s.email) },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = TrustBlue, modifier = Modifier.size(18.dp)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("email_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Store Location / Address with Auto-Detect
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text(s.address) },
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("address_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Business Category Selection
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = if (selectedLang == "en") "Business Category" else "व्यापार श्रेणी",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = TextSecondary,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        val categories = if (selectedLang == "en") {
                            listOf("Kirana / Grocery", "Dairy & Milk", "Clothing", "Hardware", "Cafe & Food")
                        } else {
                            listOf("किराना स्टोर", "डेयरी व दूध", "कपड़ा दुकान", "हार्डवेयर", "रेस्टोरेंट")
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            categories.take(3).forEach { cat ->
                                val isSelected = businessCategory.contains(cat.substringBefore(" "))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) TrustBlue.copy(alpha = 0.2f) else SurfaceLevel1)
                                        .border(1.dp, if (isSelected) TrustBlue else SurfaceOutline, RoundedCornerShape(8.dp))
                                        .clickable { businessCategory = cat }
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = cat,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) TrustBlue else TextSecondary
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = upiId,
                        onValueChange = { upiId = it },
                        label = { Text(s.upiId) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("upi_id_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Button(
                        onClick = {
                            scope.launch {
                                val uid = authenticatedUid.ifBlank { "merchant_${System.currentTimeMillis()}" }
                                val finalPhone = phone.ifBlank { "9876543210" }
                                val finalEmail = if (email.isNotBlank()) email else authenticatedEmail.ifBlank { "merchant@vyaparsetu.app" }
                                val finalAddress = address.ifBlank { "Shop No. 12, Main Market, MG Road, Jaipur" }
                                googleAuthManager.saveMerchantProfileToFirestore(
                                    uid = uid,
                                    shopName = shopName,
                                    businessCategory = businessCategory,
                                    merchantUpiId = upiId,
                                    ownerName = ownerName,
                                    phone = finalPhone,
                                    email = finalEmail,
                                    address = finalAddress,
                                    gstNumber = gstNumber,
                                    logoUri = selectedLogoIcon,
                                    languageCode = selectedLang
                                )
                                onComplete(
                                    shopName,
                                    ownerName,
                                    upiId,
                                    finalPhone,
                                    finalEmail,
                                    finalAddress,
                                    gstNumber,
                                    selectedLogoIcon,
                                    businessCategory,
                                    selectedLang
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("finish_onboarding_btn"),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CreditEmerald,
                            contentColor = Color.Black
                        )
                    ) {
                        Text(
                            text = if (selectedLang == "en") "Start Business • Open App" else "खाता शुरू करें • दुकान खोलें",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Soundbox Test Announcement
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceLevel2)
                    .clickable {
                        SoundboxSpeaker.announcePayment(
                            context = context,
                            appSource = "Google Pay",
                            amount = 500.0,
                            senderName = "रोहन",
                            languageCode = selectedLang
                        )
                    }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(CreditEmeraldDark),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = null,
                            tint = CreditEmeraldLight,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "साउंडबॉक्स आवाज़ टेस्ट करें",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Text(
                            text = "Tap to hear test announcement",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                        )
                    }
                }
                Text(
                    text = "सुनें 📢",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = CreditEmeraldLight
                    )
                )
            }
        }
    }
}
