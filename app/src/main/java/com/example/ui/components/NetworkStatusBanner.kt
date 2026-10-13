package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SignalWifiConnectedNoInternet4
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import com.example.ui.theme.DebitCoral

/**
 * Non-intrusive network status indicator banner.
 * Displays when device loses internet access, alerting the merchant that
 * offline mode is active and network calls will be queued or fall back gracefully.
 */
@Composable
fun NetworkStatusBanner(
    isOnline: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = !isOnline,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag("offline_network_banner"),
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFF1E1B18),
            shadowElevation = 8.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, DebitCoral.copy(alpha = 0.6f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(DebitCoral.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.WifiOff,
                        contentDescription = "No Internet Connection",
                        tint = DebitCoral,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "ऑफलाइन मोड सक्रिय • 100% सुरक्षित स्थानीय खाता",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFEE2E2),
                            fontSize = 12.sp
                        )
                    )
                    Text(
                        text = "OFFLINE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Black,
                            color = DebitCoral,
                            fontSize = 10.sp,
                            letterSpacing = 0.8.sp
                        )
                    )
                }
            }
        }
    }
}

/**
 * Compact status icon displayed in app bars or cards.
 */
@Composable
fun NetworkStatusIcon(
    isOnline: Boolean,
    modifier: Modifier = Modifier
) {
    if (!isOnline) {
        Box(
            modifier = modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(DebitCoral.copy(alpha = 0.15f))
                .border(1.dp, DebitCoral.copy(alpha = 0.5f), CircleShape)
                .testTag("offline_status_icon"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.SignalWifiConnectedNoInternet4,
                contentDescription = "No Network Connection",
                tint = DebitCoral,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
