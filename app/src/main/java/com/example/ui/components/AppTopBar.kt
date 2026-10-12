package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.VolumeUp
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.res.painterResource
import com.example.R
import com.example.util.LocalAppStrings
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MerchantProfile
import com.example.data.model.SupportedLanguages
import com.example.service.SoundboxSpeaker
import com.example.ui.theme.CreditEmerald
import com.example.ui.theme.CreditEmeraldDark
import com.example.ui.theme.CreditEmeraldLight
import com.example.ui.theme.SurfaceLevel2
import com.example.ui.theme.SurfaceOutline
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TrustBlue

@Composable
fun AppTopBar(
    profile: MerchantProfile,
    onLanguageSelected: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onSoundboxTap: () -> Unit,
    onOpenContactFeedback: (() -> Unit)? = null,
    onToggleTheme: (() -> Unit)? = null
) {
    var showLangMenu by remember { mutableStateOf(false) }
    val isSpeaking by SoundboxSpeaker.isSpeaking.collectAsStateWithLifecycle()

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Brand Logo & Shop Name
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // VyaparSetu Custom Vector Brand Mark or Merchant Uploaded Store Logo
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    val hasCustomLogo = profile.logoUri.isNotBlank() &&
                        (profile.logoUri.startsWith("content://") || profile.logoUri.startsWith("file://") || profile.logoUri.startsWith("http"))
                    if (hasCustomLogo) {
                        AsyncImage(
                            model = profile.logoUri,
                            contentDescription = "Store Logo",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_vyaparsetu_mark),
                            contentDescription = "VyaparSetu Logo",
                            tint = Color.Unspecified,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "VYAPARSETU",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TrustBlue,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.2.sp
                        )
                    )
                    Text(
                        text = profile.shopName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Right Action Controls
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Soundbox Live Waveform Badge
                SoundboxIndicator(
                    isSpeaking = isSpeaking,
                    onClick = onSoundboxTap
                )

                // Contact & Community Forum Button
                if (onOpenContactFeedback != null) {
                    IconButton(
                        onClick = onOpenContactFeedback,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(SurfaceLevel2)
                            .border(1.dp, TrustBlue.copy(alpha = 0.4f), CircleShape)
                            .testTag("topbar_forum_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Forum,
                            contentDescription = "Contact Us & Feedback Forum",
                            tint = TrustBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Global Theme Toggle (Light / Dark Mode)
                if (onToggleTheme != null) {
                    val isDark = profile.isDarkMode
                    IconButton(
                        onClick = onToggleTheme,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                if (isDark) Color(0xFFFBBF24).copy(alpha = 0.15f)
                                else TrustBlue.copy(alpha = 0.12f)
                            )
                            .border(
                                1.dp,
                                if (isDark) Color(0xFFFBBF24).copy(alpha = 0.4f)
                                else TrustBlue.copy(alpha = 0.4f),
                                CircleShape
                            )
                            .testTag("global_theme_toggle_btn")
                    ) {
                        Icon(
                            imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = if (isDark) "Switch to Light Mode" else "Switch to Dark Mode",
                            tint = if (isDark) Color(0xFFFBBF24) else TrustBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Language Pill
                Box {
                    val currentLang = SupportedLanguages.firstOrNull { it.code == profile.languageCode }
                    val displayLang = currentLang?.localName ?: "हिन्दी"

                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(SurfaceLevel2)
                            .border(1.dp, SurfaceOutline, CircleShape)
                            .clickable { showLangMenu = true }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("language_selector_btn"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "🇮🇳 $displayLang",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }

                    DropdownMenu(
                        expanded = showLangMenu,
                        onDismissRequest = { showLangMenu = false }
                    ) {
                        SupportedLanguages.forEach { lang ->
                            DropdownMenuItem(
                                text = {
                                    Text("${lang.scriptChar}  ${lang.localName} (${lang.englishName})")
                                },
                                onClick = {
                                    onLanguageSelected(lang.code)
                                    showLangMenu = false
                                }
                            )
                        }
                    }
                }

                // Profile Avatar / Settings Button
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(SurfaceLevel2)
                        .border(1.dp, TrustBlue.copy(alpha = 0.5f), CircleShape)
                        .testTag("profile_settings_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = "Merchant Profile & Backup Settings",
                        tint = TrustBlue,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SoundboxIndicator(
    isSpeaking: Boolean,
    onClick: () -> Unit
) {
    val transition = rememberInfiniteTransition(label = "wave")
    val barHeight1 by transition.animateFloat(
        initialValue = 4f,
        targetValue = 14f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "h1"
    )
    val barHeight2 by transition.animateFloat(
        initialValue = 12f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(350, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "h2"
    )

    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(CreditEmeraldDark.copy(alpha = 0.6f))
            .border(1.dp, CreditEmerald.copy(alpha = 0.4f), CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag("soundbox_indicator_pill"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Soundwave animated bars
        Row(
            modifier = Modifier.height(14.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(2.5.dp)
                    .height(if (isSpeaking) barHeight1.dp else 6.dp)
                    .background(CreditEmeraldLight, RoundedCornerShape(1.dp))
            )
            Box(
                modifier = Modifier
                    .width(2.5.dp)
                    .height(if (isSpeaking) barHeight2.dp else 12.dp)
                    .background(CreditEmeraldLight, RoundedCornerShape(1.dp))
            )
            Box(
                modifier = Modifier
                    .width(2.5.dp)
                    .height(if (isSpeaking) barHeight1.dp else 8.dp)
                    .background(CreditEmeraldLight, RoundedCornerShape(1.dp))
            )
        }

        Text(
            text = if (isSpeaking) "Speaking..." else "Soundbox ON",
            style = MaterialTheme.typography.labelSmall.copy(
                color = CreditEmeraldLight,
                fontWeight = FontWeight.Bold
            )
        )
    }
}
