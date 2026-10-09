package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.rememberCoroutineScope
import com.example.monetization.AdFallbackManager
import com.example.monetization.AdPolicyManager
import com.example.monetization.VyaparStickyBannerAd
import kotlinx.coroutines.launch
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.SoundboxSpeaker
import com.example.ui.components.ConfettiExplosionOverlay
import com.example.ui.theme.CreditEmerald
import com.example.ui.theme.CreditEmeraldDark
import com.example.ui.theme.CreditEmeraldLight
import com.example.ui.theme.DebitCoral
import com.example.ui.theme.SurfaceLevel1
import com.example.ui.theme.SurfaceLevel2
import com.example.ui.theme.SurfaceLevel3
import com.example.ui.theme.SurfaceOutline
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TrustBlue
import com.example.ui.theme.TrustBlueDark
import com.example.ui.theme.TrustBlueLight
import com.example.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DailySummaryScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val merchantProfile by viewModel.merchantProfile.collectAsState()
    val todayCreditTotal by viewModel.transactionRepository.todayCreditTotal.collectAsState(initial = 0.0)
    val todayCashTotal by viewModel.transactionRepository.todayCashTotal.collectAsState(initial = 0.0)
    val todayUpiTotal by viewModel.transactionRepository.todayUpiTotal.collectAsState(initial = 0.0)
    val todayCount by viewModel.transactionRepository.todayTransactionCount.collectAsState(initial = 0)
    val streakCount by viewModel.ledgerStreak.collectAsState()

    val total = todayCreditTotal ?: 0.0
    val cash = todayCashTotal ?: 0.0
    val upi = todayUpiTotal ?: 0.0
    val count = todayCount

    val isSpeaking by SoundboxSpeaker.isSpeaking.collectAsState()

    // Animated number counter
    val animatedTotal = remember { Animatable(0f) }
    LaunchedEffect(total) {
        animatedTotal.animateTo(
            targetValue = total.toFloat(),
            animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing)
        )
    }

    // Auto-trigger speech on first open
    LaunchedEffect(Unit) {
        SoundboxSpeaker.announceDailyClosing(
            context = context,
            totalAmount = total,
            cashAmount = cash,
            upiAmount = upi,
            billCount = count,
            languageCode = merchantProfile.languageCode
        )
    }

    BackHandler {
        onBack()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Top Navigation Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(SurfaceLevel2)
                            .testTag("daily_summary_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Celebration,
                            contentDescription = null,
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "दुकान क्लोजिंग समरी",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                    }

                    // Share button
                    IconButton(
                        onClick = {
                            val dateStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())
                            val shareText = """
                                📊 *दुकान क्लोजिंग समरी - ${merchantProfile.shopName.ifBlank { "VyaparSetu" }}*
                                📅 तारीख: $dateStr
                                💰 कुल दैनिक बिक्री: ₹${total.toInt()}
                                💵 कैश कलेक्शन: ₹${cash.toInt()}
                                📱 UPI कलेक्शन: ₹${upi.toInt()}
                                🧾 कुल बिल: $count
                                🔥 व्यापार स्ट्रीक: $streakCount दिन
                                
                                — VyaparSetu डिजिटल वहीखाता
                            """.trimIndent()

                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, shareText)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share Daily Summary"))
                        },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(SurfaceLevel2)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = TrustBlue
                        )
                    }
                }

                // Celebration Hero Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .border(1.5.dp, Color(0xFFF59E0B).copy(alpha = 0.5f), RoundedCornerShape(24.dp)),
                    colors = CardDefaults.cardColors(containerColor = SurfaceLevel2)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color(0xFFF59E0B).copy(alpha = 0.15f),
                                        SurfaceLevel2
                                    )
                                )
                            )
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF59E0B).copy(alpha = 0.2f))
                                .border(1.5.dp, Color(0xFFF59E0B), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Text(
                            text = "Shop Closing Ritual 📊",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF59E0B)
                            )
                        )

                        Text(
                            text = "आज की कुल दुकान कमाई",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = TextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                        )

                        Text(
                            text = "₹" + String.format(Locale.US, "%,.0f", animatedTotal.value),
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontSize = 42.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = CreditEmeraldLight
                            )
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(CreditEmeraldDark.copy(alpha = 0.4f))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = CreditEmerald,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "आज $count सफल लेन-देन संपन्न",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = CreditEmeraldLight
                                )
                            )
                        }
                    }
                }

                // Text-To-Speech Interactive Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSpeaking) TrustBlueDark.copy(alpha = 0.5f) else SurfaceLevel2
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isSpeaking) TrustBlue else SurfaceOutline
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(if (isSpeaking) TrustBlue else SurfaceLevel3),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isSpeaking) Icons.Default.Stop else Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = null,
                                tint = if (isSpeaking) Color.White else TrustBlue,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isSpeaking) "ऑडियो रिपोर्ट बज रही है..." else "दैनिक हिसाब आवाज़ में सुनें",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                            Text(
                                text = "TTS Soundbox announcement in regional language",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                            )
                        }

                        Button(
                            onClick = {
                                SoundboxSpeaker.announceDailyClosing(
                                    context = context,
                                    totalAmount = total,
                                    cashAmount = cash,
                                    upiAmount = upi,
                                    billCount = count,
                                    languageCode = merchantProfile.languageCode
                                )
                            },
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = TrustBlue,
                                contentColor = Color.White
                            ),
                            modifier = Modifier.testTag("play_closing_tts_btn")
                        ) {
                            Text("सुनें", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }

                // Collection Breakdown (Cash vs UPI)
                Text(
                    text = "पेमेंट माध्यम विवरण (Collection Breakdown)",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Cash Card
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceLevel2),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceOutline)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(SurfaceLevel3),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Payments,
                                        contentDescription = null,
                                        tint = TextSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                val cashPct = if (total > 0) (cash / total * 100).toInt() else 0
                                Text(
                                    text = "$cashPct%",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TextSecondary
                                    )
                                )
                            }
                            Text(
                                text = "कैश कलेक्शन",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                            )
                            Text(
                                text = "₹" + String.format(Locale.US, "%,.0f", cash),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                        }
                    }

                    // UPI Card
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceLevel2),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CreditEmerald.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(CreditEmeraldDark.copy(alpha = 0.4f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.QrCode2,
                                        contentDescription = null,
                                        tint = CreditEmerald,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                val upiPct = if (total > 0) (upi / total * 100).toInt() else 0
                                Text(
                                    text = "$upiPct%",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = CreditEmeraldLight
                                    )
                                )
                            }
                            Text(
                                text = "UPI कलेक्शन",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                            )
                            Text(
                                text = "₹" + String.format(Locale.US, "%,.0f", upi),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = CreditEmeraldLight
                                )
                            )
                        }
                    }
                }

                // Streak Celebration Badge
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceLevel2),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF59E0B).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "🔥", fontSize = 24.sp)
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "$streakCount-Day Ledger Streak!",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFF59E0B)
                                )
                            )
                            Text(
                                text = "लगातार दैनिक वहीखाता एक्टिव रखने के लिए बधाई!",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bottom Action Button with Interstitial Cooldown Rule
                val coroutineScope = rememberCoroutineScope()
                Button(
                    onClick = {
                        val activity = context as? Activity
                        val adPolicy = AdPolicyManager.getInstance(context)
                        if (activity != null && adPolicy.canShowInterstitial(AdPolicyManager.Workflows.DAILY_SUMMARY_VIEWED)) {
                            coroutineScope.launch {
                                AdFallbackManager.showInterstitialWithFallback(activity)
                                adPolicy.recordInterstitialShown()
                                onBack()
                            }
                        } else {
                            onBack()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("complete_daily_closing_btn"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TrustBlue,
                        contentColor = Color.White
                    )
                ) {
                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "दैनिक क्लोजिंग संपन्न करें व होम पर जाएं",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Sticky Bottom Banner Ad (InMobi primary + Start.io fallback)
                VyaparStickyBannerAd()
            }
        }

        // Celebration Confetti
        ConfettiExplosionOverlay(
            active = true,
            durationMillis = 3500L
        )
    }
}
