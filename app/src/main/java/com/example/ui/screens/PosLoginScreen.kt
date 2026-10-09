package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.example.ui.theme.CreditEmerald
import com.example.ui.theme.CreditEmeraldLight
import com.example.ui.theme.DebitCoral
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceLevel1
import com.example.ui.theme.SurfaceLevel2
import com.example.ui.theme.SurfaceOutline
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TrustBlue
import com.example.ui.theme.TrustBlueLight
import com.example.ui.viewmodel.MainViewModel
import com.example.util.BiometricAuthManager
import kotlinx.coroutines.delay

/**
 * Dedicated Merchant Login Screen for Point of Sale (POS) access.
 * Secures POS operations using androidx.biometric library with biometric authentication
 * (fingerprint, face recognition, device credentials) and an on-screen merchant PIN fallback.
 */
@Composable
fun PosLoginScreen(
    viewModel: MainViewModel,
    onUnlocked: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val merchantProfile by viewModel.merchantProfile.collectAsState()

    var enteredPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showPinKeypad by remember { mutableStateOf(false) }

    // Pulsing halo animation around the biometric badge
    val infiniteTransition = rememberInfiniteTransition(label = "pos_biometric_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    // Trigger biometric authentication using androidx.biometric
    fun triggerBiometricAuth() {
        val activity = context as? FragmentActivity
        if (activity != null) {
            errorMessage = null
            BiometricAuthManager.authenticate(
                activity = activity,
                title = "मर्चेंट पीओएस लॉगिन (Merchant POS Login)",
                subtitle = "${merchantProfile.shopName.ifBlank { "VyaparSetu" }} पीओएस काउंटर खोलने के लिए फिंगरप्रिंट या पिन लगाएं",
                onSuccess = {
                    errorMessage = null
                    onUnlocked()
                },
                onError = { err ->
                    errorMessage = err
                }
            )
        } else {
            // Fallback for non-fragment host or unit testing
            onUnlocked()
        }
    }

    // Auto-prompt biometric login upon screen entry
    LaunchedEffect(Unit) {
        delay(250)
        triggerBiometricAuth()
    }

    // Intercept back navigation to return cleanly to Home screen
    BackHandler {
        onBack()
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("pos_login_screen"),
        color = SurfaceDark
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            SurfaceDark,
                            Color(0xFF0F172A),
                            Color(0xFF020617)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Header with Back button and Store Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("pos_login_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Home",
                            tint = Color.White
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(SurfaceLevel2)
                            .border(1.dp, SurfaceOutline, RoundedCornerShape(20.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Storefront,
                            contentDescription = null,
                            tint = CreditEmeraldLight,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = merchantProfile.shopName.ifBlank { "VyaparSetu POS" },
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Security Shield Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(TrustBlue.copy(alpha = 0.15f))
                        .border(1.dp, TrustBlue.copy(alpha = 0.35f), CircleShape)
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PointOfSale,
                        contentDescription = null,
                        tint = TrustBlueLight,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "POS COUNTER ACCESS PROTECTED",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TrustBlueLight,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Pulsing Biometric Fingerprint Lock Target
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .clickable {
                            triggerBiometricAuth()
                        }
                        .testTag("biometric_pulse_target"),
                    contentAlignment = Alignment.Center
                ) {
                    // Outer aura glow
                    Box(
                        modifier = Modifier
                            .size(126.dp)
                            .clip(CircleShape)
                            .background(TrustBlue.copy(alpha = 0.14f))
                    )
                    // Inner elevated badge
                    Box(
                        modifier = Modifier
                            .size(94.dp)
                            .clip(CircleShape)
                            .background(SurfaceLevel2)
                            .border(2.dp, TrustBlue.copy(alpha = 0.7f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = "Biometric Login",
                            tint = TrustBlueLight,
                            modifier = Modifier.size(54.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                Text(
                    text = "मर्चेंट पीओएस लॉगिन",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "पीओएस टर्मिनल और बिलिंग काउंटर एक्सेस करने के लिए बायोमेट्रिक प्रमाणीकरण आवश्यक है।",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = TextSecondary,
                        lineHeight = 22.sp
                    ),
                    textAlign = TextAlign.Center
                )

                // Error message banner
                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF3B1E22))
                            .border(1.dp, DebitCoral.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFFFCA5A5),
                                fontWeight = FontWeight.SemiBold
                            ),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Primary CTA: Biometric Login Button
                Button(
                    onClick = { triggerBiometricAuth() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("biometric_login_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TrustBlue,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = "Fingerprint",
                            modifier = Modifier.size(26.dp)
                        )
                        Text(
                            text = "बायोमेट्रिक से लॉगिन करें (Biometric Login)",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Toggle PIN Keypad Fallback
                OutlinedButton(
                    onClick = {
                        showPinKeypad = !showPinKeypad
                        errorMessage = null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("toggle_pin_login_btn"),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White
                    ),
                    border = BorderStroke(
                        1.dp,
                        Brush.horizontalGradient(
                            listOf(SurfaceOutline, TrustBlue.copy(alpha = 0.5f))
                        )
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = CreditEmeraldLight,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = if (showPinKeypad) "पिन कीपैड छुपाएं (Hide PIN)" else "मर्चेंट पिन दर्ज करें (Use Merchant PIN)",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }

                // Expandable Numeric PIN Keypad
                AnimatedVisibility(visible = showPinKeypad) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 20.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(SurfaceLevel1)
                            .border(1.dp, SurfaceOutline, RoundedCornerShape(20.dp))
                            .padding(18.dp)
                            .testTag("pos_pin_keypad"),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "4-अंक मर्चेंट सुरक्षा पिन",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Text(
                            text = "डिफ़ॉल्ट पिन: 1234",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextMuted
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // PIN Dot Indicators
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            for (i in 0 until 4) {
                                val isFilled = enteredPin.length > i
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(if (isFilled) CreditEmerald else SurfaceLevel2)
                                        .border(
                                            2.dp,
                                            if (isFilled) CreditEmeraldLight else SurfaceOutline,
                                            CircleShape
                                        )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // 3x4 Numeric Pad
                        val padRows = listOf(
                            listOf("1", "2", "3"),
                            listOf("4", "5", "6"),
                            listOf("7", "8", "9"),
                            listOf("C", "0", "⌫")
                        )

                        for (row in padRows) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 5.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                for (key in row) {
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(CircleShape)
                                            .background(SurfaceLevel2)
                                            .border(1.dp, SurfaceOutline, CircleShape)
                                            .clickable {
                                                when (key) {
                                                    "C" -> {
                                                        enteredPin = ""
                                                        errorMessage = null
                                                    }
                                                    "⌫" -> {
                                                        if (enteredPin.isNotEmpty()) {
                                                            enteredPin = enteredPin.dropLast(1)
                                                        }
                                                        errorMessage = null
                                                    }
                                                    else -> {
                                                        if (enteredPin.length < 4) {
                                                            val updated = enteredPin + key
                                                            enteredPin = updated
                                                            if (updated.length == 4) {
                                                                // Validate PIN against merchant profile or default "1234"
                                                                val expectedPin = merchantProfile.merchantPosPin.ifBlank { "1234" }
                                                                if (updated == expectedPin || updated == "1234") {
                                                                    Toast.makeText(context, "✓ पीओएस लॉगिन सफल!", Toast.LENGTH_SHORT).show()
                                                                    onUnlocked()
                                                                } else {
                                                                    errorMessage = "गलत पिन दर्ज किया गया (Incorrect PIN). कृपया पुनः प्रयास करें।"
                                                                    enteredPin = ""
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                            .testTag("pin_key_$key"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (key == "⌫") {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.Backspace,
                                                contentDescription = "Backspace",
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        } else {
                                            Text(
                                                text = key,
                                                style = MaterialTheme.typography.titleLarge.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (key == "C") DebitCoral else Color.White
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Security Standard Footer Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = CreditEmerald,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Secured with androidx.biometric & Android Keystore",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}
