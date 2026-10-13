package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.example.ai.GeminiClient
import com.example.ai.StoreSnapshot
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.FeedbackEntity
import com.example.ui.theme.CreditEmerald
import com.example.ui.theme.CreditEmeraldDark
import com.example.ui.theme.CreditEmeraldLight
import com.example.ui.theme.DebitCoral
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
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactFeedbackScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val profile by viewModel.merchantProfile.collectAsStateWithLifecycle()
    val feedbacks by viewModel.allFeedbacks.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Contact & Forum, 1: Submit Feedback, 2: App FAQ
    var selectedCategoryFilter by remember { mutableStateOf("All") }

    // Feedback submission form state
    var feedbackTitle by remember { mutableStateOf("") }
    var feedbackMessage by remember { mutableStateOf("") }
    var feedbackCategory by remember { mutableStateOf("Bug Report") }
    var feedbackType by remember { mutableStateOf("bug") } // "bug" or "suggestion"
    var feedbackRating by remember { mutableIntStateOf(5) }
    var isSubmitting by remember { mutableStateOf(false) }
    var showConfirmationDialog by remember { mutableStateOf(false) }
    var lastSubmittedFeedbackTitle by remember { mutableStateOf("") }

    BackHandler {
        onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Contact Us & Feedback",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "VyaparSetu Developer Support & Community Forum",
                            style = MaterialTheme.typography.labelSmall.copy(color = TextMuted)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("btn_contact_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfaceLevel1,
                    titleContentColor = TextPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Modern Segmented Navigation Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = SurfaceLevel1,
                contentColor = TrustBlue,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = TrustBlue,
                        height = 3.dp
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Developer & Forum", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Post Feedback", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("🤖 AI FAQ & Guide", fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal) }
                )
            }

            when (selectedTab) {
                0 -> {
                    // TAB 0: Developer Contact Card + Community Forum Feed
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Developer Contact Card
                        item {
                            DeveloperContactCard(context = context)
                        }

                        // Community Forum Section Header
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Forum,
                                        contentDescription = null,
                                        tint = TrustBlue,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Text(
                                        text = "व्यापारी कम्युनिटी फोरम (Community)",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                    )
                                }

                                Button(
                                    onClick = { selectedTab = 1 },
                                    colors = ButtonDefaults.buttonColors(containerColor = TrustBlue),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.AddComment, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Post", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Category Filter Chips
                        item {
                            val categories = listOf("All", "Feature Request", "Billing & Soundbox", "UI/UX", "Bug Report")
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(categories, key = { it }) { cat ->
                                    val isSelected = selectedCategoryFilter == cat
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { selectedCategoryFilter = cat },
                                        label = { Text(cat, fontSize = 12.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = TrustBlue.copy(alpha = 0.2f),
                                            selectedLabelColor = TrustBlue
                                        )
                                    )
                                }
                            }
                        }

                        // Feedback items
                        val filteredList = if (selectedCategoryFilter == "All") {
                            feedbacks
                        } else {
                            feedbacks.filter { it.category.equals(selectedCategoryFilter, ignoreCase = true) }
                        }

                        if (filteredList.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No feedback threads in this category yet. Be the first to post!",
                                        style = MaterialTheme.typography.bodyMedium.copy(color = TextMuted),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            items(filteredList, key = { it.id }) { feedback ->
                                FeedbackForumCard(
                                    feedback = feedback,
                                    onUpvote = {
                                        viewModel.upvoteFeedback(feedback.id)
                                        Toast.makeText(context, "Upvoted feedback!", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }
                    }
                }

                1 -> {
                    // TAB 1: Post Feedback Form
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = SurfaceLevel2),
                                shape = RoundedCornerShape(16.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceOutline)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Text(
                                        text = "आपका सुझाव या समस्या बताएं (Submit Feedback)",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                    )
                                    Text(
                                        text = "आपका फीडबैक सीधे डेवलपर Tanish Paul को भेजा जाएगा और कम्युनिटी फोरम पर लाइव दिखेगा।",
                                        style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                                    )

                                    // Feedback Type (Bug Report vs Feature Suggestion)
                                    Text("प्रकार चुनें (Feedback Type)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        val isBug = feedbackCategory.contains("Bug", ignoreCase = true)
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(if (isBug) DebitCoral.copy(alpha = 0.2f) else SurfaceLevel1)
                                                .border(1.5.dp, if (isBug) DebitCoral else SurfaceOutline, RoundedCornerShape(10.dp))
                                                .clickable {
                                                    feedbackCategory = "Bug Report"
                                                    feedbackType = "bug"
                                                }
                                                .padding(vertical = 10.dp, horizontal = 12.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                "🐞 बग रिपोर्ट (Bug)",
                                                fontSize = 12.sp,
                                                fontWeight = if (isBug) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isBug) DebitCoral else TextSecondary
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(if (!isBug) TrustBlue.copy(alpha = 0.2f) else SurfaceLevel1)
                                                .border(1.5.dp, if (!isBug) TrustBlue else SurfaceOutline, RoundedCornerShape(10.dp))
                                                .clickable {
                                                    feedbackCategory = "Feature Request"
                                                    feedbackType = "suggestion"
                                                }
                                                .padding(vertical = 10.dp, horizontal = 12.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                "💡 नया सुझाव (Suggestion)",
                                                fontSize = 12.sp,
                                                fontWeight = if (!isBug) FontWeight.Bold else FontWeight.Normal,
                                                color = if (!isBug) TrustBlue else TextSecondary
                                            )
                                        }
                                    }

                                    // Category selector
                                    Text("श्रेणी चुनें (Select Category)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
                                    val formCats = if (feedbackCategory.contains("Bug", ignoreCase = true)) {
                                        listOf("Bug Report", "POS Crash / Bug", "Printer Issue", "QR Glitch", "Soundbox Issue")
                                    } else {
                                        listOf("Feature Request", "UI/UX", "Billing & Invoicing", "WhatsApp Store", "General Idea")
                                    }
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        items(formCats, key = { it }) { cat ->
                                            val isSel = feedbackCategory == cat
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (isSel) TrustBlue.copy(alpha = 0.2f) else SurfaceLevel1)
                                                    .border(1.dp, if (isSel) TrustBlue else SurfaceOutline, RoundedCornerShape(8.dp))
                                                    .clickable { feedbackCategory = cat }
                                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                            ) {
                                                Text(
                                                    cat,
                                                    fontSize = 12.sp,
                                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isSel) TrustBlue else TextSecondary
                                                )
                                            }
                                        }
                                    }

                                    // Star Rating
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text("रेटिंग (Rating):", fontSize = 13.sp, color = TextSecondary)
                                        (1..5).forEach { star ->
                                            Icon(
                                                imageVector = Icons.Default.Star,
                                                contentDescription = null,
                                                tint = if (star <= feedbackRating) Color(0xFFF59E0B) else Color.Gray.copy(alpha = 0.4f),
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .clickable { feedbackRating = star }
                                            )
                                        }
                                    }

                                    // Feedback Title
                                    OutlinedTextField(
                                        value = feedbackTitle,
                                        onValueChange = { feedbackTitle = it },
                                        label = { Text("शीर्षक (Title / Subject)") },
                                        placeholder = { Text("उदा. Auto-Split QR में नया बैंक सपोर्ट") },
                                        modifier = Modifier.fillMaxWidth().testTag("input_feedback_title"),
                                        shape = RoundedCornerShape(12.dp),
                                        singleLine = true
                                    )

                                    // Feedback Detailed Message
                                    OutlinedTextField(
                                        value = feedbackMessage,
                                        onValueChange = { feedbackMessage = it },
                                        label = { Text("विवरण (Detailed Message)") },
                                        placeholder = { Text("कृपया अपनी राय या बग का विवरण लिखें...") },
                                        modifier = Modifier.fillMaxWidth().height(130.dp).testTag("input_feedback_msg"),
                                        shape = RoundedCornerShape(12.dp)
                                    )

                                    // Submit Button
                                    Button(
                                        onClick = {
                                            if (feedbackTitle.isBlank() || feedbackMessage.isBlank()) {
                                                Toast.makeText(context, "कृपया शीर्षक और विवरण दर्ज करें (Fill all fields)", Toast.LENGTH_SHORT).show()
                                                return@Button
                                            }
                                            isSubmitting = true
                                            scope.launch {
                                                val submittedTitle = feedbackTitle
                                                val entity = FeedbackEntity(
                                                    authorName = profile.ownerName.ifBlank { "Merchant (${profile.shopName})" },
                                                    authorEmail = profile.email.ifBlank { "merchant@vyaparsetu.app" },
                                                    category = feedbackCategory,
                                                    rating = feedbackRating,
                                                    title = feedbackTitle,
                                                    message = feedbackMessage,
                                                    timestamp = System.currentTimeMillis()
                                                )
                                                viewModel.submitFeedback(entity)
                                                // Log Google Analytics for Firebase feedback event
                                                com.example.analytics.AnalyticsManager.logFeedbackSubmitted(
                                                    type = if (feedbackCategory.contains("Bug", ignoreCase = true)) "BUG" else "SUGGESTION",
                                                    category = feedbackCategory,
                                                    rating = feedbackRating
                                                )
                                                isSubmitting = false
                                                lastSubmittedFeedbackTitle = submittedTitle
                                                feedbackTitle = ""
                                                feedbackMessage = ""
                                                showConfirmationDialog = true
                                            }
                                        },
                                        enabled = !isSubmitting,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(50.dp)
                                            .testTag("btn_submit_feedback"),
                                        colors = ButtonDefaults.buttonColors(containerColor = CreditEmerald, contentColor = Color.Black),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Submit to Firebase & Community", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // TAB 2: AI Trained FAQ & How to use features
                    AiFaqSection(viewModel = viewModel, context = context)
                }
            }

            // Confirmation Dialog upon successful feedback submission
            if (showConfirmationDialog) {
                androidx.compose.material3.AlertDialog(
                    onDismissRequest = {
                        showConfirmationDialog = false
                        selectedTab = 0
                    },
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = CreditEmerald,
                                modifier = Modifier.size(24.dp)
                            )
                            Text("फीडबैक सफलतापूर्वक दर्ज (Feedback Received)")
                        }
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "धन्यवाद! आपका ${if (feedbackCategory.contains("Bug", ignoreCase = true)) "बग रिपोर्ट (Bug Report)" else "सुझाव (Suggestion)"} क्लाउड फायरस्टोर (Firebase Firestore) में टाइमस्टैम्प सहित सुरक्षित रूप से दर्ज कर लिया गया है।",
                                style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary)
                            )
                            Text(
                                text = "\"$lastSubmittedFeedbackTitle\"",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TrustBlue,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = "डेवलपर Tanish Paul और कम्युनिटी फोरम इसे तुरंत रिव्यू करेंगे।",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showConfirmationDialog = false
                                selectedTab = 0 // Return to community forum
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = TrustBlue)
                        ) {
                            Text("ठीक है (View Forum)", fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun DeveloperContactCard(context: Context) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceLevel2),
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, TrustBlue.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header with Studio Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(TrustBlue.copy(alpha = 0.2f))
                            .border(1.5.dp, TrustBlue, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = TrustBlue,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Tanish Paul",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TextPrimary
                                )
                            )
                            Icon(Icons.Default.Verified, contentDescription = "Verified Developer", tint = TrustBlue, modifier = Modifier.size(18.dp))
                        }
                        Text(
                            text = "Blue Auric Studio • Lead Architect",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TrustBlueLight,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(CreditEmeraldDark.copy(alpha = 0.4f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("Official Dev", fontSize = 11.sp, color = CreditEmeraldLight, fontWeight = FontWeight.Bold)
                }
            }

            // Contact details box
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceLevel1)
                    .border(1.dp, SurfaceOutline, RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
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
            }

            // Direct Contact Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:blueauricstudio@gmail.com")
                            putExtra(Intent.EXTRA_SUBJECT, "VyaparSetu Feedback & Support")
                        }
                        try {
                            context.startActivity(Intent.createChooser(emailIntent, "Send Email to Tanish Paul"))
                        } catch (e: Exception) {
                            Toast.makeText(context, "Email: blueauricstudio@gmail.com", Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier.weight(1f).testTag("btn_email_developer"),
                    colors = ButtonDefaults.buttonColors(containerColor = TrustBlue),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Email Dev", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                OutlinedButton(
                    onClick = {
                        val waUrl = "https://wa.me/?text=Hello%20Tanish,%20I%20have%20feedback%20regarding%20VyaparSetu."
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(waUrl))
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Opening WhatsApp...", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.weight(1f).testTag("btn_whatsapp_support"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("💬 WhatsApp", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun FeedbackForumCard(
    feedback: FeedbackEntity,
    onUpvote: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceLevel2),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceOutline)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header with Author & Category
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = feedback.authorName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = TextPrimary
                    )
                    val dateStr = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(feedback.timestamp))
                    Text(text = dateStr, fontSize = 11.sp, color = TextMuted)
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(TrustBlue.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = feedback.category,
                        fontSize = 11.sp,
                        color = TrustBlueLight,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Rating Stars
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                (1..feedback.rating).forEach { _ ->
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Title & Message
            Text(
                text = feedback.title,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = TextPrimary
            )
            Text(
                text = feedback.message,
                fontSize = 13.sp,
                color = TextSecondary,
                lineHeight = 18.sp
            )

            // Developer Reply Section if present
            if (!feedback.developerReply.isNullOrBlank()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceLevel1)
                        .border(1.dp, CreditEmerald.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Verified, contentDescription = null, tint = CreditEmerald, modifier = Modifier.size(16.dp))
                        Text(
                            text = feedback.replyAuthor ?: "Tanish Paul (Lead Developer)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = CreditEmeraldLight
                        )
                    }
                    Text(
                        text = feedback.developerReply,
                        fontSize = 12.sp,
                        color = TextPrimary,
                        lineHeight = 17.sp
                    )
                }
            }

            // Upvote Footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onUpvote,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.ThumbUp, contentDescription = null, modifier = Modifier.size(14.dp), tint = TrustBlue)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Upvote (${feedback.upvotes})", fontSize = 12.sp, color = TrustBlue, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun AiFaqSection(
    viewModel: MainViewModel,
    context: Context
) {
    val profile by viewModel.merchantProfile.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var userQueryInput by remember { mutableStateOf("") }
    var isThinking by remember { mutableStateOf(false) }

    // Conversation state for AI FAQ Assistant
    val conversation = remember {
        mutableStateListOf(
            "AI" to "नमस्ते! मैं VyaparSetu AI FAQ सहायक हूँ। मुझे ऐप के सभी फीचर्स — जैसे Auto-Split QR, Store Logo अपलोड, ब्लूटूथ प्रिंटर, GST बिलिंग, साउंडबॉक्स और डेवलपर सपोर्ट — की पूरी ट्रेनिंग प्राप्त है।\n\nआप मुझसे नीचे दिए गए किसी भी बटन पर टैप करके या अपना सवाल लिखकर पूछ सकते हैं!"
        )
    }

    val quickQuestions = listOf(
        "Auto-Split QR कैसे काम करता है?",
        "दुकान का Logo कैसे अपलोड करें?",
        "थर्मल प्रिंटर कैसे कनेक्ट करें?",
        "उधारी / खाता में नया ग्राहक कैसे जोड़ें?",
        "Voice-to-Billing का उपयोग कैसे करें?",
        "Customer Trust Score क्या है?",
        "डेवलपर Tanish Paul से कैसे संपर्क करें?"
    )

    fun askAi(question: String) {
        if (question.isBlank() || isThinking) return
        conversation.add("User" to question)
        isThinking = true
        scope.launch {
            val storeSnapshot = StoreSnapshot(
                shopName = profile.shopName.ifBlank { "VyaparSetu Store" },
                ownerName = profile.ownerName.ifBlank { "व्यापारी" }
            )
            val history = conversation.map { it.second to (it.first == "User") }
            val answer = GeminiClient.chatWithGemini(
                history = history,
                merchantLanguage = profile.languageCode,
                shopName = profile.shopName,
                userQuery = question,
                storeSnapshot = storeSnapshot
            )
            conversation.add("AI" to answer)
            isThinking = false
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // AI FAQ Crown Header Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceLevel2),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFA855F7).copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFA855F7).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFFA855F7), modifier = Modifier.size(24.dp))
                        }
                        Column {
                            Text(
                                text = "व्यापारसेतु AI FAQ सहायक",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                            Text(
                                text = "Ask AI how to use any feature of VyaparSetu",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                            )
                        }
                    }

                    // Quick Prompt Chips
                    Text("त्वरित प्रश्न (Quick Feature Questions):", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(quickQuestions, key = { it }) { q ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(SurfaceLevel1)
                                    .border(1.dp, SurfaceOutline, RoundedCornerShape(10.dp))
                                    .clickable {
                                        askAi(q)
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(text = q, fontSize = 12.sp, color = TrustBlue, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }

        // Ask Bar (Input field + Send button)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = userQueryInput,
                    onValueChange = { userQueryInput = it },
                    placeholder = { Text("उदा. GST बिल कैसे बनाएं, लोगो कैसे लगाएं...") },
                    modifier = Modifier.weight(1f).testTag("input_ai_faq"),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )

                Button(
                    onClick = {
                        val q = userQueryInput.trim()
                        if (q.isNotBlank()) {
                            userQueryInput = ""
                            askAi(q)
                        }
                    },
                    enabled = !isThinking && userQueryInput.isNotBlank(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TrustBlue),
                    modifier = Modifier.height(52.dp).testTag("btn_send_ai_faq")
                ) {
                    if (isThinking) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Send, contentDescription = "Ask AI", modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        // Conversation history bubbles
        items(conversation, key = { "${it.first}_${it.second.hashCode()}" }) { (role, message) ->
            val isUser = role == "User"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(if (isUser) 0.85f else 0.95f)
                        .clip(RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isUser) 16.dp else 4.dp,
                            bottomEnd = if (isUser) 4.dp else 16.dp
                        ))
                        .background(if (isUser) TrustBlue else SurfaceLevel2)
                        .border(1.dp, if (isUser) TrustBlue else SurfaceOutline, RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (isUser) Icons.Default.Person else Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = if (isUser) Color.White else Color(0xFFA855F7),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (isUser) "आप (You)" else "व्यापारसेतु AI FAQ",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (isUser) Color.White else Color(0xFFA855F7)
                            )
                        }
                        Text(
                            text = message,
                            fontSize = 13.sp,
                            color = if (isUser) Color.White else TextPrimary,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }

        if (isThinking) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(8.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = TrustBlue, strokeWidth = 2.dp)
                    Text("AI उत्तर तैयार कर रहा है (Analyzing feature guidance)...", fontSize = 12.sp, color = TextMuted)
                }
            }
        }

        // Section divider for standard Accordion FAQs
        item {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.HelpOutline, contentDescription = null, tint = CreditEmerald, modifier = Modifier.size(18.dp))
                Text(
                    text = "सामान्य पूछे जाने वाले प्रश्न (Popular FAQs)",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                )
            }
        }

        // Render accordion items inside the same LazyColumn
        val faqItems = listOf(
            "How does Auto-Split QR save me money on MDR fees?" to
                    "When you create a UPI bill over ₹2000, NPCI regulations permit merchant acquiring fees. VyaparSetu's autonomous engine detects amounts over ₹2000 and splits them into equal sub-₹2000 QR codes (e.g. ₹3600 -> two ₹1800 QR codes). Customers scan both effortlessly and you enjoy 100% zero MDR transaction charges.",

            "How do I upload or change my Store Logo / Photo?" to
                    "Go to Settings -> 'Shop Profile' or during the initial registration screen, tap the circular Store Logo icon. You can select an image directly from your phone gallery or choose a preset retail badge. The logo will automatically appear on all printed and PDF GST invoices!",

            "How do I connect my Bluetooth Thermal Receipt Printer?" to
                    "Tap 'Printer Setup' on the Home screen or Invoices screen. Enable Bluetooth on your Android device and tap 'Scan'. Select your paired or discovered 58mm (2-inch) or 80mm (3-inch) wireless thermal printer and tap 'Test Print'.",

            "How does Voice-to-Billing work?" to
                    "Tap the prominent Microphone icon on the Invoices, POS, or Khata screen. Speak in Hindi, English, or Hinglish (e.g. 'Ramesh ko 500 ka ration udhar' or '2 Lux soap cash bill 80 rupees'). AI Bol translates spoken input into a structured invoice or customer ledger entry automatically.",

            "How does the Customer Trust Score predict credit risk?" to
                    "The engine analyzes past repayment durations against due dates. 🟢 Low Risk: Customer repays debts in under 7 days. 🟡 Moderate Risk: Repays within 7-30 days. 🔴 High Risk: Debt overdue for over 30 days. This helps you decide whether to grant udhar.",

            "How to contact Developer Tanish Paul?" to
                    "You can email Tanish Paul directly at blueauricstudio@gmail.com, use the 'Email Dev' button on the Contact tab, or post your query to the Community Forum."
        )

        items(faqItems, key = { it.first }) { (question, answer) ->
            FaqAccordionCard(question = question, answer = answer)
        }
    }
}

@Composable
private fun FaqAccordionCard(question: String, answer: String) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = SurfaceLevel2),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceOutline)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = question,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    ),
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = TrustBlue
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = answer,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary,
                            lineHeight = 18.sp
                        )
                    )
                }
            }
        }
    }
}
