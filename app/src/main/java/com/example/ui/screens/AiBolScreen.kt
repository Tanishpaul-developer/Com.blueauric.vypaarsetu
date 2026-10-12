package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.rememberCoroutineScope
import com.example.monetization.AdFallbackManager
import com.example.monetization.AdPolicyManager
import com.example.monetization.RewardedAdGateDialog
import com.example.monetization.VyaparStickyBannerAd
import kotlinx.coroutines.launch
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ai.StoreSnapshot
import com.example.data.model.ChatMessage
import com.example.data.model.SupportedLanguages
import com.example.ui.theme.CreditEmerald
import com.example.ui.theme.CreditEmeraldLight
import com.example.ui.theme.DebitCoral
import com.example.ui.theme.DebitCoralLight
import com.example.ui.theme.LocalIsDarkTheme
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
import com.example.ui.viewmodel.ChatCategory
import com.example.ui.viewmodel.ChatViewModel
import com.example.ui.viewmodel.MainViewModel

@Composable
fun AiBolScreen(
    viewModel: MainViewModel,
    chatViewModel: ChatViewModel = viewModel()
) {
    val context = LocalContext.current
    val isDark = LocalIsDarkTheme.current
    val uiState by chatViewModel.uiState.collectAsStateWithLifecycle()
    val merchantProfile by viewModel.merchantProfile.collectAsStateWithLifecycle()

    var inputText by remember { mutableStateOf("") }
    var showLanguageDropdown by remember { mutableStateOf(false) }
    var showFinancialSnapshot by remember { mutableStateOf(true) }
    val listState = rememberLazyListState()

    val adPolicy = remember { AdPolicyManager.getInstance(context) }
    val remainingCredits by adPolicy.creditsState.collectAsStateWithLifecycle()
    var showRewardedAdDialog by remember { mutableStateOf(false) }
    var isAdLoading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // Auto-scroll to the newest message
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        // --- 1. AI BOL TOP HEADER ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Animated Glowing Avatar
                val transition = rememberInfiniteTransition(label = "pulse")
                val scale by transition.animateFloat(
                    initialValue = 0.95f,
                    targetValue = 1.05f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(900, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "scale"
                )

                Box(
                    modifier = Modifier
                        .size((42 * scale).dp)
                        .clip(CircleShape)
                        .background(if (isDark) TrustBlueDark.copy(alpha = 0.6f) else TrustBlue.copy(alpha = 0.15f))
                        .border(1.5.dp, TrustBlue, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SmartToy,
                        contentDescription = "Vyapar AI Sahayak",
                        tint = if (isDark) TrustBlueLight else TrustBlueDark,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "FIREBASE AI SAHAYAK",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TrustBlue,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.2.sp
                            )
                        )
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = TrustBlue, modifier = Modifier.size(13.dp))
                    }
                    Text(
                        text = if (uiState.selectedLanguage == "en") "Merchant AI Financial & Support Advisor" else "व्यापारी वित्तीय व ग्राहक सहायक",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                }
            }

            // Top Action Controls (Language Selector & Reset)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Language Picker Pill
                Box {
                    val currentLang = SupportedLanguages.firstOrNull { it.code == uiState.selectedLanguage }
                    val displayLang = currentLang?.localName ?: "हिन्दी"

                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(SurfaceLevel2)
                            .border(1.dp, SurfaceOutline, CircleShape)
                            .clickable { showLanguageDropdown = true }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("ai_chat_language_btn"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.Translate, contentDescription = null, tint = TrustBlue, modifier = Modifier.size(14.dp))
                        Text(
                            text = displayLang,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    DropdownMenu(
                        expanded = showLanguageDropdown,
                        onDismissRequest = { showLanguageDropdown = false }
                    ) {
                        SupportedLanguages.forEach { lang ->
                            DropdownMenuItem(
                                text = { Text("${lang.scriptChar}  ${lang.localName} (${lang.englishName})") },
                                onClick = {
                                    chatViewModel.setLanguage(lang.code)
                                    showLanguageDropdown = false
                                }
                            )
                        }
                    }
                }

                // Clear Chat Button
                IconButton(
                    onClick = { chatViewModel.clearChat() },
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(SurfaceLevel2)
                        .border(1.dp, SurfaceOutline, CircleShape)
                        .testTag("clear_chat_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Clear Chat",
                        tint = TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // --- 2. LIVE REAL-TIME FINANCIAL CONTEXT CARD ---
        uiState.storeSnapshot?.let { snapshot ->
            AnimatedVisibility(visible = showFinancialSnapshot) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = SurfaceLevel2,
                    border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceOutline)
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(CreditEmerald)
                                )
                                Text(
                                    text = if (uiState.selectedLanguage == "en") "Live Store Financial Snapshot" else "लाइव स्टोर वित्तीय स्नैपशॉट",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TextSecondary
                                    )
                                )
                            }
                            Text(
                                text = if (uiState.selectedLanguage == "en") "Hide" else "छिपाएं",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = TrustBlue,
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier.clickable { showFinancialSnapshot = false }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            FinancialPill(
                                label = if (uiState.selectedLanguage == "en") "Today's Sales" else "आज की बिक्री",
                                value = "₹${String.format(java.util.Locale.US, "%.0f", snapshot.todaySalesTotal)}",
                                icon = Icons.Default.TrendingUp,
                                color = TrustBlue
                            )
                            FinancialPill(
                                label = if (uiState.selectedLanguage == "en") "UPI Total" else "UPI कुल",
                                value = "₹${String.format(java.util.Locale.US, "%.0f", snapshot.todayUpiTotal)}",
                                icon = Icons.Default.AccountBalanceWallet,
                                color = CreditEmerald
                            )
                            FinancialPill(
                                label = if (uiState.selectedLanguage == "en") "Pending Udhar" else "कुल उधार",
                                value = "₹${String.format(java.util.Locale.US, "%.0f", snapshot.totalUdharDue)}",
                                icon = Icons.Default.Group,
                                color = DebitCoral
                            )
                        }
                    }
                }
            }
        }

        // --- 3. CATEGORY FILTER TABS ---
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(ChatCategory.values(), key = { it.name }) { category ->
                val isSelected = uiState.selectedCategory == category
                val title = if (uiState.selectedLanguage == "en") category.titleEn else category.titleHi

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isSelected) TrustBlue else SurfaceLevel2
                        )
                        .border(
                            1.dp,
                            if (isSelected) TrustBlue else SurfaceOutline,
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { chatViewModel.selectCategory(category) }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (isSelected) Color.White else TextSecondary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    )
                }
            }
        }

        // --- 4. QUICK CONTEXTUAL PROMPT CHIPS ---
        val quickPrompts = chatViewModel.getQuickPrompts()
        if (quickPrompts.isNotEmpty()) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(quickPrompts, key = { it.prompt }) { promptItem ->
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(SurfaceLevel2)
                            .border(1.dp, SurfaceOutline, CircleShape)
                            .clickable { chatViewModel.sendMessage(promptItem.prompt) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = promptItem.label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }
            }
        }

        // --- 5. CHAT MESSAGE THREAD ---
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(uiState.messages, key = { it.id }) { message ->
                ModernChatBubble(
                    message = message,
                    isSpeaking = uiState.speakingMessageId == message.id,
                    onSpeak = { chatViewModel.speakMessage(message) },
                    onCopy = { chatViewModel.copyToClipboard(context, message.text) },
                    onFollowUpClick = { followUpText ->
                        chatViewModel.sendMessage(followUpText)
                    }
                )
            }

            if (uiState.isLoading) {
                item {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceLevel2)
                            .border(1.dp, SurfaceOutline, RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = TrustBlue,
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = if (uiState.selectedLanguage == "en") "VyaparSetu AI analyzing financials..." else "व्यापारसेतु AI उत्तर तैयार कर रहा है...",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                        )
                    }
                }
            }
        }

        // --- 6. CHAT INPUT BAR ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp)
                .clip(CircleShape)
                .background(SurfaceLevel2)
                .border(1.dp, SurfaceOutline, CircleShape)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Voice Mic Quick Prompt
            IconButton(
                onClick = {
                    val defaultVoicePrompt = if (uiState.selectedLanguage == "en") {
                        "Summarize my store financial standing and debtor list"
                    } else {
                        "दुकान की आज की कुल बिक्री और उधारी का हिसाब बताओ"
                    }
                    chatViewModel.sendMessage(defaultVoicePrompt)
                },
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(TrustBlue.copy(alpha = 0.15f))
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Voice Query",
                    tint = TrustBlue,
                    modifier = Modifier.size(20.dp)
                )
            }

            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = {
                    Text(
                        text = if (uiState.selectedLanguage == "en") "Ask e.g. Today's sales, UPI dispute, Khata reminder..." else "पूछें: आज की बिक्री, ग्राहक विवाद, उधार वसूली संदेश...",
                        color = TextMuted,
                        fontSize = 12.5.sp,
                        maxLines = 1
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("ai_bol_input_field"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                singleLine = true
            )

            IconButton(
                onClick = {
                    if (inputText.isNotBlank()) {
                        if (adPolicy.hasFeatureCredits()) {
                            adPolicy.consumeCredit()
                            chatViewModel.sendMessage(inputText)
                            inputText = ""
                        } else {
                            showRewardedAdDialog = true
                        }
                    }
                },
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(TrustBlue)
                    .testTag("ai_bol_send_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        VyaparStickyBannerAd()
    }

    if (showRewardedAdDialog) {
        RewardedAdGateDialog(
            featureName = "AI Voice Assistant",
            isLoading = isAdLoading,
            onDismiss = { showRewardedAdDialog = false },
            onWatchAd = {
                val activity = context as? Activity
                if (activity != null) {
                    isAdLoading = true
                    coroutineScope.launch {
                        AdFallbackManager.showRewardedWithFallback(activity) {
                            adPolicy.grantRewardCredits(5)
                            Toast.makeText(context, "🎉 5 मुफ्त क्रेडिट जोड़े गए!", Toast.LENGTH_SHORT).show()
                        }
                        isAdLoading = false
                        showRewardedAdDialog = false
                    }
                }
            }
        )
    }
}

@Composable
private fun FinancialPill(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(12.dp))
            Text(label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, color = TextMuted))
        }
        Text(value, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = color))
    }
}

@Composable
fun ModernChatBubble(
    message: ChatMessage,
    isSpeaking: Boolean,
    onSpeak: () -> Unit,
    onCopy: () -> Unit,
    onFollowUpClick: (String) -> Unit
) {
    val isUser = message.isUser
    val isDark = LocalIsDarkTheme.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .clip(
                    RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        bottomStart = if (isUser) 18.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 18.dp
                    )
                )
                .background(
                    if (isUser) {
                        if (isDark) TrustBlueDark else TrustBlue
                    } else {
                        SurfaceLevel2
                    }
                )
                .border(
                    width = 1.dp,
                    color = if (isUser) TrustBlue.copy(alpha = 0.5f) else SurfaceOutline,
                    shape = RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        bottomStart = if (isUser) 18.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 18.dp
                    )
                )
                .padding(14.dp)
        ) {
            // Header for AI Bubble (Category Tag + Speaker info)
            if (!isUser) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(TrustBlue.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.SmartToy, contentDescription = null, tint = TrustBlue, modifier = Modifier.size(12.dp))
                        }
                        Text(
                            text = "Vyapar AI Advisor",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = TrustBlue
                            )
                        )
                    }

                    if (message.category.isNotBlank() && message.category != "GENERAL") {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(SurfaceLevel3)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = message.category.replace("_", " "),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextMuted
                                )
                            )
                        }
                    }
                }
            }

            // Message Body Text
            Text(
                text = message.text,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = if (isUser) Color.White else TextPrimary,
                    lineHeight = 22.sp
                )
            )

            // Action Controls (Copy to WhatsApp, Read Aloud via Soundbox)
            if (!isUser) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Copy Button
                    IconButton(
                        onClick = onCopy,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy message",
                            tint = TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Audio Soundbox Readout Button
                    IconButton(
                        onClick = onSpeak,
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(if (isSpeaking) CreditEmerald.copy(alpha = 0.2f) else Color.Transparent)
                    ) {
                        Icon(
                            imageVector = if (isSpeaking) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                            contentDescription = if (isSpeaking) "Stop Speaking" else "Read aloud via Soundbox",
                            tint = if (isSpeaking) CreditEmeraldLight else TextMuted,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }

                // Contextual Follow-up Quick Prompts
                if (message.suggestedFollowUps.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Suggested Questions:",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        message.suggestedFollowUps.forEach { followUp ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(SurfaceLevel3.copy(alpha = 0.7f))
                                    .clickable { onFollowUpClick(followUp) }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = TrustBlue, modifier = Modifier.size(11.dp))
                                    Text(
                                        text = followUp,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = TextSecondary,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
