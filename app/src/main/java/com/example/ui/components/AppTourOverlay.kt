package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CreditEmerald
import com.example.ui.theme.SurfaceLevel2
import com.example.ui.theme.SurfaceOutline
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TrustBlue

data class TourStepData(
    val title: String,
    val description: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val highlightTarget: String
)

val TourSteps = listOf(
    TourStepData(
        title = "Quick POS & Smart Auto-Split",
        description = "Large tactile keypad generates instant NPCI QR codes. Bills over ₹2,000 are split automatically to avoid MDR charges!",
        icon = Icons.Default.QrCode2,
        highlightTarget = "POS / QR"
    ),
    TourStepData(
        title = "Live Phone Soundbox",
        description = "Turns your phone into a commercial speaker! Automatically announces Google Pay, PhonePe, and Paytm alerts in your chosen local language.",
        icon = Icons.Default.VolumeUp,
        highlightTarget = "Phone Soundbox"
    ),
    TourStepData(
        title = "Khata Digital Ledger",
        description = "Track customer Udhar & Jama balances. Send WhatsApp payment reminders with direct 1-click UPI links.",
        icon = Icons.Default.MenuBook,
        highlightTarget = "Khata Ledger"
    ),
    TourStepData(
        title = "Gemini 2.5 Flash Screenshot OCR",
        description = "Take or upload payment screenshots; Gemini AI extracts payer name, UTR reference, and amount directly into your ledger.",
        icon = Icons.Default.DocumentScanner,
        highlightTarget = "OCR Scan"
    ),
    TourStepData(
        title = "AI Bol Retail Assistant",
        description = "Got questions about retail margins, inventory, or ONDC? Ask AI Bol in your native tongue anytime!",
        icon = Icons.Default.AutoAwesome,
        highlightTarget = "AI Bol"
    )
)

@Composable
fun AppTourOverlay(
    active: Boolean,
    currentStep: Int,
    onNextStep: () -> Unit,
    onDismiss: () -> Unit
) {
    AnimatedVisibility(
        visible = active && currentStep < TourSteps.size,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        val step = TourSteps.getOrNull(currentStep) ?: return@AnimatedVisibility

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.78f))
                .clickable(onClick = { /* prevent click-through */ })
                .testTag("app_tour_scrim"),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(SurfaceLevel2)
                    .border(1.dp, TrustBlue.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header with step counter & close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(TrustBlue.copy(alpha = 0.15f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Step ${currentStep + 1} of ${TourSteps.size}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TrustBlue,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss Tour",
                            tint = TextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Icon Highlight
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(TrustBlue.copy(alpha = 0.2f))
                        .border(2.dp, TrustBlue, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = step.icon,
                        contentDescription = step.title,
                        tint = TrustBlue,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = step.title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = TextPrimary,
                        fontWeight = FontWeight.ExtraBold
                    ),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = step.description,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = TextMuted,
                        lineHeight = 22.sp
                    ),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Progress dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TourSteps.indices.forEach { idx ->
                        Box(
                            modifier = Modifier
                                .size(if (idx == currentStep) 20.dp else 8.dp, 8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (idx == currentStep) TrustBlue else SurfaceOutline)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = TextMuted
                        )
                    ) {
                        Text("Skip Tour", fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = onNextStep,
                        modifier = Modifier
                            .weight(1.5f)
                            .height(48.dp)
                            .testTag("tour_next_btn"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TrustBlue,
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = if (currentStep == TourSteps.size - 1) "Get Started" else "Next",
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
