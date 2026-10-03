package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.LocalIsDarkTheme
import com.example.ui.theme.TrustBlueDark
import com.example.ui.theme.CreditEmerald
import com.example.ui.theme.CreditEmeraldDark
import com.example.ui.theme.SurfaceLevel1
import com.example.ui.theme.SurfaceLevel2
import com.example.ui.theme.SurfaceOutline
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TrustBlue
import com.example.ui.theme.TrustBlueLight

/**
 * Official VyaparSetu Brand Logo component.
 * Reproduces the authentic brand mark: interlocking Royal Blue and Emerald Green Setu arches
 * with the central Indian Rupee symbol and "VyaparSetu" typography.
 */
@Composable
fun VyaparSetuEmblem(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_vyaparsetu_mark),
            contentDescription = "VyaparSetu Emblem",
            tint = Color.Unspecified,
            modifier = Modifier.size(size * 0.82f)
        )
    }
}

/**
 * Hero Brand Header card for the Home Screen and Onboarding screen.
 */
@Composable
fun VyaparSetuBrandHeroHeader(
    modifier: Modifier = Modifier,
    tagline: String = "Digital Bridge for Smart Merchants",
    onClick: (() -> Unit)? = null
) {
    val isDark = LocalIsDarkTheme.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    if (isDark) {
                        listOf(Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF0F172A))
                    } else {
                        listOf(Color(0xFFFFFFFF), Color(0xFFF8FAFC), Color(0xFFEFF6FF))
                    }
                )
            )
            .border(
                1.5.dp,
                if (isDark) TrustBlue.copy(alpha = 0.45f) else TrustBlueDark.copy(alpha = 0.25f),
                RoundedCornerShape(20.dp)
            )
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Logo Mark Box
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White)
                .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_vyaparsetu_mark),
                contentDescription = "VyaparSetu Logo",
                tint = Color.Unspecified,
                modifier = Modifier.size(46.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Dual-color Brand Typography
                Row {
                    Text(
                        text = "Vyapar",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isDark) Color(0xFF38BDF8) else TrustBlueDark,
                            letterSpacing = (-0.5).sp
                        )
                    )
                    Text(
                        text = "Setu",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isDark) Color(0xFF34D399) else Color(0xFF059669),
                            letterSpacing = (-0.5).sp
                        )
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isDark) TrustBlue.copy(alpha = 0.25f) else TrustBlue.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "PRO",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDark) Color(0xFF60A5FA) else TrustBlueDark
                    )
                }
            }

            Text(
                text = tagline,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextSecondary,
                    fontSize = 11.5.sp
                )
            )
        }
    }
}
