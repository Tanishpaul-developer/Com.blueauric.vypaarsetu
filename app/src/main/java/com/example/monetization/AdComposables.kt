package com.example.monetization

import android.app.Activity
import android.view.View
import android.widget.FrameLayout
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import coil.compose.rememberAsyncImagePainter
import com.example.ui.theme.CreditEmerald
import com.example.ui.theme.CreditEmeraldDark
import com.example.ui.theme.CreditEmeraldLight
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceLevel1
import com.example.ui.theme.SurfaceLevel2
import com.example.ui.theme.SurfaceOutline
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TrustBlue
import com.example.ui.theme.TrustBlueLight
import com.inmobi.ads.InMobiBanner
import kotlinx.coroutines.launch

/**
 * 1. Sticky Bottom Banner Ad with InMobi primary & Start.io fallback.
 * Includes complete Android lifecycle management (ON_PAUSE, ON_RESUME, ON_DESTROY).
 */
@Composable
fun VyaparStickyBannerAd(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var bannerViewRef by remember { mutableStateOf<View?>(null) }
    var activeNetwork by remember { mutableStateOf(AdNetwork.NONE) }
    var isAdVisible by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            val banner = bannerViewRef
            if (banner is FrameLayout) {
                for (i in 0 until banner.childCount) {
                    val child = banner.getChildAt(i)
                    if (child is InMobiBanner) {
                        when (event) {
                            Lifecycle.Event.ON_PAUSE -> child.pause()
                            Lifecycle.Event.ON_RESUME -> child.resume()
                            Lifecycle.Event.ON_DESTROY -> child.destroy()
                            else -> {}
                        }
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            val banner = bannerViewRef
            if (banner is FrameLayout) {
                for (i in 0 until banner.childCount) {
                    val child = banner.getChildAt(i)
                    if (child is InMobiBanner) {
                        child.destroy()
                    }
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
            .testTag("vyapar_sticky_banner_container"),
        contentAlignment = Alignment.Center
    ) {
        // Active third-party banner ad container (InMobi / Start.io)
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            factory = { ctx ->
                AdFallbackManager.createBannerContainerView(
                    context = ctx,
                    onAdLoaded = { network ->
                        activeNetwork = network
                        isAdVisible = true
                    },
                    onAdFailed = {
                        isAdVisible = false
                    }
                ).also {
                    bannerViewRef = it
                }
            }
        )

        // Resilient in-house merchant banner when ad networks return NO FILL
        if (!isAdVisible) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(SurfaceLevel2)
                    .border(1.dp, SurfaceOutline, RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(CreditEmeraldDark),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = null,
                            tint = CreditEmeraldLight,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    Text(
                        text = "VyaparSetu Pro • 100% सुरक्षित डिजिटल वहीखाता",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontSize = 12.sp
                        )
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(TrustBlue.copy(alpha = 0.2f))
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "UPI 2.0",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = TrustBlueLight,
                            fontSize = 10.sp
                        )
                    )
                }
            }
        }
    }
}

/**
 * 2. Material 3 Native Ad Card for LazyColumn feeds (Customer Ledger & Transaction History).
 * Renders seamlessly as the 5th item in long merchant lists.
 */
@Composable
fun VyaparNativeAdCard(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var nativeAd by remember { mutableStateOf<UnifiedNativeAd?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        isLoading = true
        nativeAd = AdFallbackManager.loadNativeAd(context)
        isLoading = false
    }

    val ad = nativeAd
    if (ad != null) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
                .clickable { ad.onClick(context) }
                .testTag("vyapar_native_ad_card"),
            colors = CardDefaults.cardColors(containerColor = SurfaceLevel1),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceOutline)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header with Badge & Network Attribution
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFF59E0B).copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Ad • प्रायोजित",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(0xFFF59E0B),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            )
                        }

                        Text(
                            text = if (ad.network == AdNetwork.INMOBI) "InMobi Partner" else "Start.io Partner",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.Campaign,
                        contentDescription = null,
                        tint = TrustBlueLight,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Main Ad Body Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (!ad.iconUrl.isNullOrBlank()) {
                        Image(
                            painter = rememberAsyncImagePainter(ad.iconUrl),
                            contentDescription = ad.title,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(SurfaceLevel2),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(TrustBlue.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MonetizationOn,
                                contentDescription = null,
                                tint = TrustBlueLight,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = ad.title,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = ad.description,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextSecondary,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Call To Action Button
                Button(
                    onClick = { ad.onClick(context) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .testTag("native_ad_cta_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TrustBlue,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = ad.ctaText,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    } else if (!isLoading) {
        // Fallback pro merchant card to maintain stable list height without CLS
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
                .testTag("vyapar_native_ad_fallback_card"),
            colors = CardDefaults.cardColors(containerColor = SurfaceLevel1),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceOutline)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF00C853).copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "विशेष सुविधा • Pro",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFF00C853),
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.Campaign,
                        contentDescription = null,
                        tint = CreditEmeraldLight,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(CreditEmeraldDark.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MonetizationOn,
                            contentDescription = null,
                            tint = CreditEmeraldLight,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "स्मार्ट साउंडबॉक्स व क्यूआर कोड",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "प्रत्येक यूपीआई भुगतान पर तुरंत हिंदी/स्थानीय भाषा में बोलकर पुष्टि पाएं।",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextSecondary,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Button(
                    onClick = {},
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .testTag("native_ad_fallback_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CreditEmeraldDark,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "साउंडबॉक्स सक्रिय करें (Free)",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }
}

/**
 * 3. Rewarded Ad Gate Dialog displayed when free daily feature credits run out.
 * Prompts user to watch a short sponsored video to unlock +5 credits.
 */
@Composable
fun RewardedAdGateDialog(
    featureName: String,
    onDismiss: () -> Unit,
    onWatchAd: () -> Unit,
    isLoading: Boolean = false
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .testTag("rewarded_ad_gate_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceLevel1),
            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceOutline)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Top Header Row with Close icon
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stars,
                            contentDescription = null,
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "दैनिक क्रेडिट समाप्त (0 Credits)",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = Color(0xFFF59E0B),
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
                            contentDescription = "Dismiss",
                            tint = TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Video Ad Icon Badge with Glow
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    TrustBlue.copy(alpha = 0.35f),
                                    Color.Transparent
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(SurfaceLevel2)
                            .border(2.dp, TrustBlue, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = "Watch Video",
                            tint = TrustBlueLight,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                // Title & Description
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "मुफ्त क्रेडिट्स अनलॉक करें",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        ),
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "आपकी आज की 5 मुफ्त '$featureName' सीमा समाप्त हो गई है। एक छोटा वीडियो देखकर +5 अतिरिक्त क्रेडिट तुरंत प्राप्त करें।",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary,
                            lineHeight = 18.sp
                        ),
                        textAlign = TextAlign.Center
                    )
                }

                // Primary CTA: Watch Video
                Button(
                    onClick = onWatchAd,
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("watch_rewarded_video_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CreditEmerald,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayCircle,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = "वीडियो देखें (+5 Credits)",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }

                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "बाद में करें (Cancel)",
                        style = MaterialTheme.typography.bodyMedium.copy(color = TextMuted)
                    )
                }
            }
        }
    }
}
