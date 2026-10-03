package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowOutward
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationImportant
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.VolumeUp
import com.example.ui.components.VyaparSetuBrandHeroHeader
import com.example.util.LocalAppStrings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.TransactionEntity
import com.example.data.model.SmartTip
import com.example.ui.components.NavScreen
import com.example.ui.theme.CreditEmerald
import com.example.ui.theme.CreditEmeraldDark
import com.example.ui.theme.CreditEmeraldLight
import com.example.ui.theme.DebitCoral
import com.example.ui.theme.DebitCoralDark
import com.example.ui.theme.DebitCoralLight
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
import com.example.util.UpiHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onNavigate: (NavScreen) -> Unit,
    onOpenAddCustomer: () -> Unit
) {
    val context = LocalContext.current
    val recentTransactions by viewModel.transactionRepository.recentTransactions.collectAsState(initial = emptyList())
    val todayCreditTotal by viewModel.transactionRepository.todayCreditTotal.collectAsState(initial = 0.0)
    val todayUpiTotal by viewModel.transactionRepository.todayUpiTotal.collectAsState(initial = 0.0)
    val todayCashTotal by viewModel.transactionRepository.todayCashTotal.collectAsState(initial = 0.0)
    val todayCount by viewModel.transactionRepository.todayTransactionCount.collectAsState(initial = 0)
    val totalUdhar by viewModel.totalCustomerUdhar.collectAsState()
    val lowStockCount by viewModel.lowStockCount.collectAsState()
    val lowStockProducts by viewModel.lowStockProducts.collectAsState()
    val streakCount by viewModel.ledgerStreak.collectAsState()
    val smartTip by viewModel.smartTip.collectAsState()
    val merchantProfile by viewModel.merchantProfile.collectAsState()
    val strings = LocalAppStrings.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Official VyaparSetu Brand Hero Header
        item {
            Spacer(modifier = Modifier.height(4.dp))
            VyaparSetuBrandHeroHeader(
                tagline = if (merchantProfile.languageCode == "en") {
                    "Smart Merchant POS • Instant UPI • Digital Soundbox"
                } else {
                    "स्मार्ट व्यापारी पीओएस • ऑटो-स्प्लिट क्यूआर • डिजिटल साउंडबॉक्स"
                },
                onClick = { onNavigate(NavScreen.CONTACT_FEEDBACK) }
            )
        }

        item {
            // 1. Live Soundbox Audio Announcement Banner
            SoundboxLiveBanner(
                onReplay = { viewModel.replayLastSoundboxAnnouncement() }
            )
        }

        // Daily Ledger Streak Counter Badge
        item {
            DailyStreakBadge(
                streakCount = streakCount,
                onStreakClick = { onNavigate(NavScreen.DAILY_SUMMARY) }
            )
        }

        // Proactive AI Smart Tip Card
        smartTip?.let { tip ->
            item {
                ProactiveSmartTipCard(
                    tip = tip,
                    onActionClick = {
                        if (tip.customerPhone != null && tip.customerDue != null) {
                            UpiHelper.sharePaymentReminderViaShareSheet(
                                context = context,
                                customerPhone = tip.customerPhone,
                                customerName = tip.customerName ?: "ग्राहक",
                                amount = tip.customerDue,
                                merchantUpi = merchantProfile.upiId,
                                shopName = merchantProfile.shopName
                            )
                        } else if (tip.targetScreen == "INVENTORY") {
                            onNavigate(NavScreen.INVENTORY)
                        } else if (tip.targetScreen == "KHATA") {
                            onNavigate(NavScreen.KHATA)
                        } else if (tip.targetScreen == "DAILY_SUMMARY") {
                            onNavigate(NavScreen.DAILY_SUMMARY)
                        }
                    }
                )
            }
        }

        item {
            // 2. Today's Total Sales Hero Card
            TodaySalesCard(
                totalAmount = todayCreditTotal ?: 0.0,
                transactionCount = todayCount
            )
        }

        if (lowStockCount > 0) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(DebitCoralDark.copy(alpha = 0.35f))
                        .border(1.dp, DebitCoral.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                        .clickable { onNavigate(NavScreen.INVENTORY) }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(DebitCoral),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "कम स्टॉक चेतावनी ($lowStockCount वस्तुएं)",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = DebitCoralLight
                            )
                        )
                        Text(
                            text = "${lowStockProducts.take(2).joinToString { it.name }} का स्टॉक न्यूनतम स्तर पर है।",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(DebitCoral)
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "स्टॉक देखें",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        )
                    }
                }
            }
        }

        item {
            // 3. Metric 3-Sub-Cards (Cash, UPI, Udhar)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Cash
                MetricSubCard(
                    modifier = Modifier.weight(1f),
                    title = "Cash",
                    amount = "₹" + String.format(Locale.US, "%,.0f", todayCashTotal ?: 0.0),
                    subtext = "${recentTransactions.count { it.paymentSource.equals("Cash", ignoreCase = true) }} Bills",
                    icon = Icons.Default.Payments,
                    accentColor = TextSecondary,
                    containerColor = SurfaceLevel2
                )
                // UPI
                MetricSubCard(
                    modifier = Modifier.weight(1f),
                    title = "UPI",
                    amount = "₹" + String.format(Locale.US, "%,.0f", todayUpiTotal ?: 0.0),
                    subtext = "${recentTransactions.count { !it.paymentSource.equals("Cash", ignoreCase = true) }} Paid",
                    icon = Icons.Default.QrCode2,
                    accentColor = CreditEmerald,
                    containerColor = SurfaceLevel2
                )
                // Udhar
                MetricSubCard(
                    modifier = Modifier.weight(1f),
                    title = "Udhar",
                    amount = "₹" + String.format(Locale.US, "%,.0f", totalUdhar ?: 0.0),
                    subtext = if ((totalUdhar ?: 0.0) > 0.0) "Due Pending" else "No Dues",
                    icon = Icons.Default.MenuBook,
                    accentColor = DebitCoral,
                    containerColor = SurfaceLevel2
                )
            }
        }

        item {
            // 4. Quick Actions
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.quickActions,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                }

                // Hero Action: New Sale / Instant QR
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.horizontalGradient(listOf(TrustBlueDark, TrustBlue))
                        )
                        .border(1.dp, TrustBlueLight.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                        .clickable { onNavigate(NavScreen.POS_QR) }
                        .padding(16.dp)
                        .testTag("action_new_sale_qr"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Column {
                            Text(
                                text = strings.newSaleQr,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                            Text(
                                text = if (merchantProfile.languageCode == "en") "Generate customized dynamic UPI code" else "कस्टमाइज्ड डायनामिक UPI QR कोड बनाएं",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Secondary Grid Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SecondaryActionTile(
                        modifier = Modifier.weight(1f),
                        title = strings.addCustomer,
                        subtitle = if (merchantProfile.languageCode == "en") "Record ledger credit" else "खाता में उधारी दर्ज करें",
                        icon = Icons.Default.PersonAdd,
                        tint = DebitCoral,
                        bg = DebitCoralDark.copy(alpha = 0.4f),
                        onClick = onOpenAddCustomer,
                        testTag = "action_add_udhar"
                    )
                    SecondaryActionTile(
                        modifier = Modifier.weight(1f),
                        title = strings.inventoryTitle,
                        subtitle = if (lowStockCount > 0) "⚠️ $lowStockCount ${strings.lowStockAlert}" else (if (merchantProfile.languageCode == "en") "Stock & Catalog" else "स्टॉक व कैटलॉग"),
                        icon = Icons.Default.Inventory2,
                        tint = if (lowStockCount > 0) DebitCoralLight else TrustBlue,
                        bg = if (lowStockCount > 0) DebitCoralDark.copy(alpha = 0.4f) else TrustBlueDark.copy(alpha = 0.4f),
                        onClick = { onNavigate(NavScreen.INVENTORY) },
                        testTag = "action_inventory"
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SecondaryActionTile(
                        modifier = Modifier.weight(1f),
                        title = strings.scanOcr,
                        subtitle = strings.scanOcrSubtitle,
                        icon = Icons.Default.DocumentScanner,
                        tint = TrustBlue,
                        bg = TrustBlueDark.copy(alpha = 0.4f),
                        onClick = { onNavigate(NavScreen.OCR_SCAN) },
                        testTag = "action_scan_ocr"
                    )
                    SecondaryActionTile(
                        modifier = Modifier.weight(1f),
                        title = strings.reminders,
                        subtitle = strings.remindersSubtitle,
                        icon = Icons.Default.Chat,
                        tint = CreditEmerald,
                        bg = CreditEmeraldDark.copy(alpha = 0.4f),
                        onClick = { onNavigate(NavScreen.KHATA) },
                        testTag = "action_reminders"
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SecondaryActionTile(
                        modifier = Modifier.weight(1f),
                        title = strings.aiBolTileTitle,
                        subtitle = strings.aiBolTileSubtitle,
                        icon = Icons.Default.Mic,
                        tint = Color(0xFFA855F7),
                        bg = Color(0xFF581C87).copy(alpha = 0.4f),
                        onClick = { onNavigate(NavScreen.AI_BOL) },
                        testTag = "action_ai_bol"
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SecondaryActionTile(
                        modifier = Modifier.weight(1f),
                        title = strings.reports,
                        subtitle = strings.reportsSubtitle,
                        icon = Icons.Default.Assessment,
                        tint = TrustBlue,
                        bg = TrustBlueDark.copy(alpha = 0.4f),
                        onClick = { onNavigate(NavScreen.REPORTS_ANALYTICS) },
                        testTag = "action_reports_analytics"
                    )
                    SecondaryActionTile(
                        modifier = Modifier.weight(1f),
                        title = strings.pastBills,
                        subtitle = strings.pastBillsSubtitle,
                        icon = Icons.Default.History,
                        tint = CreditEmerald,
                        bg = CreditEmeraldDark.copy(alpha = 0.4f),
                        onClick = { onNavigate(NavScreen.TRANSACTION_HISTORY) },
                        testTag = "action_txn_history"
                    )
                }

                // Killer Feature Tiles: GST Invoicing & Thermal Printing
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SecondaryActionTile(
                        modifier = Modifier.weight(1f),
                        title = strings.invoices,
                        subtitle = strings.invoicesSubtitle,
                        icon = Icons.Default.ReceiptLong,
                        tint = Color(0xFF38BDF8),
                        bg = Color(0xFF0369A1).copy(alpha = 0.35f),
                        onClick = { onNavigate(NavScreen.INVOICE_BILLING) },
                        testTag = "action_invoice_billing"
                    )
                    SecondaryActionTile(
                        modifier = Modifier.weight(1f),
                        title = strings.thermalPrinter,
                        subtitle = strings.thermalPrinterSubtitle,
                        icon = Icons.Default.QrCode2,
                        tint = Color(0xFF34D399),
                        bg = Color(0xFF065F46).copy(alpha = 0.35f),
                        onClick = { onNavigate(NavScreen.PRINTER_SETUP) },
                        testTag = "action_printer_setup"
                    )
                }

                // Killer Feature Tile: WhatsApp Mini-Store & Contact / Community Forum
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SecondaryActionTile(
                        modifier = Modifier.weight(1f),
                        title = strings.whatsAppStore,
                        subtitle = strings.whatsAppStoreSubtitle,
                        icon = Icons.Default.Share,
                        tint = Color(0xFF22C55E),
                        bg = Color(0xFF14532D).copy(alpha = 0.35f),
                        onClick = { onNavigate(NavScreen.MINI_STORE) },
                        testTag = "action_mini_store"
                    )
                    SecondaryActionTile(
                        modifier = Modifier.weight(1f),
                        title = strings.contactFeedback,
                        subtitle = strings.contactFeedbackSubtitle,
                        icon = Icons.Default.Forum,
                        tint = Color(0xFF60A5FA),
                        bg = Color(0xFF1E3A8A).copy(alpha = 0.35f),
                        onClick = { onNavigate(NavScreen.CONTACT_FEEDBACK) },
                        testTag = "action_contact_feedback"
                    )
                }
            }
        }

        item {
            // Daily Closing Ritual 8:30 PM Quick Banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(SurfaceLevel2)
                    .border(1.dp, Color(0xFFF59E0B).copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                    .clickable { onNavigate(NavScreen.DAILY_SUMMARY) }
                    .padding(14.dp)
                    .testTag("action_daily_closing_banner"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF59E0B).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Celebration,
                        contentDescription = null,
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = strings.dailyClosingRitualTitle,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF59E0B)
                        )
                    )
                    Text(
                        text = strings.dailyClosingRitualSubtitle,
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color(0xFFF59E0B))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = strings.viewSummaryBtn,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    )
                }
            }
        }

        item {
            // 5. Merchant Insights Banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(SurfaceLevel2)
                    .border(1.dp, Color(0xFF78350F).copy(alpha = 0.4f), RoundedCornerShape(18.dp))
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF451A03)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🌾",
                        fontSize = 24.sp
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "VYAPAR INSIGHTS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color(0xFFFBBF24),
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                    )
                    Text(
                        text = if (lowStockProducts.isNotEmpty()) {
                            "${lowStockProducts.first().name} का स्टॉक कम है (${lowStockProducts.first().stockQuantity} ${lowStockProducts.first().unit} शेष)"
                        } else {
                            "दुकान का स्टॉक सुरक्षित है"
                        },
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    Text(
                        text = if (lowStockProducts.isNotEmpty()) {
                            "स्टॉक रिफिल करने के लिए इन्वेंट्री मॉड्यूल खोलें"
                        } else {
                            "सभी उत्पाद न्यूनतम सीमा से ऊपर उपलब्ध हैं"
                        },
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextMuted
                        )
                    )
                }
            }
        }

        item {
            // 6. Recent Transactions Feed
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Recent Transactions",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(SurfaceLevel3)
                            .border(1.dp, SurfaceOutline, CircleShape)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Today",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextSecondary,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }

                Text(
                    text = "View All",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = TrustBlue,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.clickable { onNavigate(NavScreen.KHATA) }
                )
            }
        }

        if (recentTransactions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceLevel2)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = "अभी कोई लेन-देन नहीं है (No Transactions Yet)",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = TextSecondary,
                                fontWeight = FontWeight.SemiBold
                            ),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "POS से ग्राहक के लिए बिल बनाएं या UPI QR से भुगतान प्राप्त करें",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextMuted),
                            textAlign = TextAlign.Center
                        )
                        FilledTonalButton(
                            onClick = { onNavigate(NavScreen.POS_QR) },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.QrCode2, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("बिल बनाएं (Create Bill)")
                        }
                    }
                }
            }
        } else {
            // Transaction list items
            items(recentTransactions.take(8)) { txn ->
                TransactionRowItem(
                    txn = txn,
                    onShare = {
                        shareReceipt(context, txn)
                    }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun SoundboxLiveBanner(onReplay: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        CreditEmeraldDark.copy(alpha = 0.9f),
                        Color(0xFF0E241C)
                    )
                )
            )
            .border(1.dp, CreditEmerald.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(CreditEmerald.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.VolumeUp,
                    contentDescription = null,
                    tint = CreditEmeraldLight,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "LIVE SOUNDBOX ANNOUNCEMENT",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = CreditEmeraldLight,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                    )
                    Box(modifier = Modifier.size(4.dp).background(CreditEmerald, CircleShape))
                    Text(
                        text = "2m ago",
                        style = MaterialTheme.typography.labelSmall.copy(color = TextMuted)
                    )
                }

                Text(
                    text = "₹150 received via UPI • Ramesh Kumar",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
            }
        }

        Button(
            onClick = onReplay,
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = CreditEmerald.copy(alpha = 0.25f),
                contentColor = CreditEmeraldLight
            ),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            modifier = Modifier.testTag("replay_soundbox_btn")
        ) {
            Icon(Icons.Default.Replay, contentDescription = "Replay", modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Replay", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
        }
    }
}

@Composable
fun TodaySalesCard(totalAmount: Double, transactionCount: Int) {
    val strings = LocalAppStrings.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFF1D4ED8),
                        Color(0xFF1E3A8A),
                        Color(0xFF0F172A)
                    )
                )
            )
            .border(1.dp, TrustBlue.copy(alpha = 0.35f), RoundedCornerShape(24.dp))
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
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
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = TrustBlueLight,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = strings.todaySales.uppercase(),
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = TrustBlueLight,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    )
                }

                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(CreditEmerald.copy(alpha = 0.2f))
                        .border(1.dp, CreditEmerald.copy(alpha = 0.4f), CircleShape)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.TrendingUp,
                        contentDescription = null,
                        tint = CreditEmeraldLight,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "$transactionCount",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = CreditEmeraldLight,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            // Big Currency Amount
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "₹",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = TrustBlueLight
                    )
                )
                Text(
                    text = String.format(Locale.US, "%,.0f", totalAmount),
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        letterSpacing = (-1).sp
                    )
                )
            }

            // Footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Verified,
                        contentDescription = null,
                        tint = TrustBlueLight,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "$transactionCount ${strings.todayTransactions}",
                        style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "Settlement details",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun MetricSubCard(
    modifier: Modifier = Modifier,
    title: String,
    amount: String,
    subtext: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    containerColor: Color
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(containerColor)
            .border(1.dp, SurfaceOutline, RoundedCornerShape(18.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = amount,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary
            )
        )
        Text(
            text = subtext,
            style = MaterialTheme.typography.labelSmall.copy(
                color = TextMuted,
                fontSize = 10.sp
            )
        )
    }
}

@Composable
fun SecondaryActionTile(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    bg: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(SurfaceLevel2)
            .border(1.dp, SurfaceOutline, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(14.dp)
            .testTag(testTag),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(bg),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = title, tint = tint, modifier = Modifier.size(22.dp))
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelSmall.copy(
                color = TextMuted,
                fontSize = 11.sp
            ),
            maxLines = 1
        )
    }
}

@Composable
fun TransactionRowItem(
    txn: TransactionEntity,
    onShare: () -> Unit
) {
    val isCredit = txn.type == "CREDIT"
    val initials = txn.payerName.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("").ifBlank { "TX" }
    val formattedTime = SimpleDateFormat("hh:mm a", Locale.US).format(Date(txn.timestamp))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceLevel2)
            .border(1.dp, SurfaceOutline, RoundedCornerShape(16.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(SurfaceLevel3)
                    .border(1.dp, SurfaceOutline, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = txn.payerName,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
                Text(
                    text = "${txn.paymentSource} • $formattedTime",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TextMuted
                    )
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = (if (isCredit) "+" else "-") + "₹" + String.format(Locale.US, "%,.0f", txn.amount),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isCredit) CreditEmerald else DebitCoral
                    )
                )
                Text(
                    text = if (isCredit) "Jama (Received)" else "Udhar (Due)",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = if (isCredit) CreditEmeraldLight else DebitCoralLight,
                        fontSize = 10.sp
                    )
                )
            }

            IconButton(
                onClick = onShare,
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(SurfaceLevel3)
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share",
                    tint = TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

fun shareReceipt(context: Context, txn: TransactionEntity) {
    val text = "VyaparSetu Receipt\nParty: ${txn.payerName}\nAmount: ₹${txn.amount}\nRef: ${txn.upiRef}\nTime: ${SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(Date(txn.timestamp))}\nStatus: Settled"
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        putExtra(Intent.EXTRA_TEXT, text)
        type = "text/plain"
    }
    context.startActivity(Intent.createChooser(sendIntent, "Share Receipt via"))
}

@Composable
fun DailyStreakBadge(
    streakCount: Int,
    onStreakClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .border(1.dp, Color(0xFFF59E0B).copy(alpha = 0.5f), RoundedCornerShape(18.dp))
            .clickable(onClick = onStreakClick)
            .testTag("daily_streak_badge"),
        colors = CardDefaults.cardColors(containerColor = SurfaceLevel2)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color(0xFFF59E0B).copy(alpha = 0.15f),
                            SurfaceLevel2
                        )
                    )
                )
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF59E0B).copy(alpha = 0.25f))
                    .border(1.dp, Color(0xFFF59E0B), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "🔥", fontSize = 22.sp)
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "🔥 $streakCount-Day Ledger Streak!",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFF59E0B)
                        )
                    )
                }
                Text(
                    text = "दैनिक एक्टिविटी बरकरार रखें। 8:30 PM क्लोजिंग समरी देखें।",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }

            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0xFFF59E0B))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "क्लोजिंग 📊",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                )
            }
        }
    }
}

@Composable
fun ProactiveSmartTipCard(
    tip: SmartTip,
    onActionClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .border(1.dp, TrustBlue.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
            .clickable(onClick = onActionClick)
            .testTag("proactive_smart_tip_card"),
        colors = CardDefaults.cardColors(containerColor = SurfaceLevel2)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(
                        when (tip.iconType) {
                            "OVERDUE" -> DebitCoralDark.copy(alpha = 0.5f)
                            "LOW_STOCK" -> Color(0xFFF59E0B).copy(alpha = 0.2f)
                            "STREAK" -> Color(0xFFF59E0B).copy(alpha = 0.2f)
                            else -> TrustBlueDark.copy(alpha = 0.4f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (tip.iconType) {
                        "OVERDUE" -> Icons.Default.Chat
                        "LOW_STOCK" -> Icons.Default.Warning
                        "STREAK" -> Icons.Default.Celebration
                        else -> Icons.Default.Lightbulb
                    },
                    contentDescription = null,
                    tint = when (tip.iconType) {
                        "OVERDUE" -> DebitCoralLight
                        "LOW_STOCK" -> Color(0xFFF59E0B)
                        "STREAK" -> Color(0xFFF59E0B)
                        else -> TrustBlue
                    },
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tip.title,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = when (tip.iconType) {
                            "OVERDUE" -> DebitCoralLight
                            else -> TextPrimary
                        }
                    )
                )
                Text(
                    text = tip.description,
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }

            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(TrustBlue)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = tip.actionLabel,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
            }
        }
    }
}
