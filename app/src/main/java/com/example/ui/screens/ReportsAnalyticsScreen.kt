package com.example.ui.screens

import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.firebase.FirestoreManager
import com.example.data.local.CustomerEntity
import com.example.data.local.ProductEntity
import com.example.data.local.TransactionEntity
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
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class AnalyticsTimeframe(val label: String, val days: Int) {
    DAILY("दैनिक (Today)", 1),
    WEEKLY("साप्ताहिक (7 Days)", 7),
    MONTHLY("मासिक (30 Days)", 30)
}

@Composable
fun ReportsAnalyticsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedTimeframe by remember { mutableStateOf(AnalyticsTimeframe.WEEKLY) }

    val merchantProfile by viewModel.merchantProfile.collectAsState()
    val allTransactions by viewModel.transactionRepository.allTransactions.collectAsState(initial = emptyList())
    val allCustomers by viewModel.khataRepository.allCustomers.collectAsState(initial = emptyList())
    val allProducts by viewModel.inventoryRepository.allProducts.collectAsState(initial = emptyList())

    val cutoffTime = remember(selectedTimeframe) {
        System.currentTimeMillis() - (selectedTimeframe.days * 86400000L)
    }

    val filteredTransactions = remember(allTransactions, cutoffTime) {
        allTransactions.filter { it.timestamp >= cutoffTime }
    }

    val totalSales = remember(filteredTransactions) {
        filteredTransactions.filter { it.type == "CREDIT" }.sumOf { it.amount }
    }
    val cashSales = remember(filteredTransactions) {
        filteredTransactions.filter { it.type == "CREDIT" && it.paymentSource.equals("Cash", ignoreCase = true) }.sumOf { it.amount }
    }
    val upiSales = remember(filteredTransactions) {
        filteredTransactions.filter { it.type == "CREDIT" && !it.paymentSource.equals("Cash", ignoreCase = true) }.sumOf { it.amount }
    }
    val billCount = remember(filteredTransactions) {
        filteredTransactions.size
    }
    val avgBillValue = remember(totalSales, billCount) {
        if (billCount > 0) totalSales / billCount else 0.0
    }

    // Top Udhar Customers
    val outstandingCustomers = remember(allCustomers) {
        allCustomers.filter { !it.isSupplier && it.balanceDue > 0 }.sortedByDescending { it.balanceDue }.take(5)
    }

    // Top Selling Products (simulated based on inventory or movement)
    val topProducts = remember(allProducts) {
        allProducts.sortedByDescending { it.price * (20 - it.stockQuantity.coerceAtLeast(0.0)) }.take(5)
    }

    var isSyncingToCloud by remember { mutableStateOf(false) }
    var cloudSyncSuccess by remember { mutableStateOf(false) }

    BackHandler {
        onBack()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                // Top Header
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
                            .testTag("reports_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Assessment,
                            contentDescription = null,
                            tint = TrustBlue,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "रिपोर्ट्स व एनालिटिक्स",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                    }

                    // Cloud Sync Button
                    IconButton(
                        onClick = {
                            scope.launch {
                                isSyncingToCloud = true
                                // Sync analytics summary to Firestore
                                try {
                                    val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                    val key = merchantProfile.upiId.replace("@", "_").replace(".", "_").ifBlank { "default_merchant" }
                                    val data = hashMapOf(
                                        "timeframe" to selectedTimeframe.name,
                                        "total_sales" to totalSales,
                                        "cash_sales" to cashSales,
                                        "upi_sales" to upiSales,
                                        "bill_count" to billCount,
                                        "avg_order_value" to avgBillValue,
                                        "synced_at" to System.currentTimeMillis()
                                    )
                                    db.collection("merchants")
                                        .document(key)
                                        .collection("analytics_summaries")
                                        .document(selectedTimeframe.name)
                                        .set(data)
                                    cloudSyncSuccess = true
                                } catch (_: Exception) {}
                                isSyncingToCloud = false
                            }
                        },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(SurfaceLevel2)
                            .testTag("sync_analytics_cloud_btn")
                    ) {
                        if (isSyncingToCloud) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = TrustBlue)
                        } else {
                            Icon(
                                imageVector = if (cloudSyncSuccess) Icons.Default.CloudDone else Icons.Default.CloudDone,
                                contentDescription = "Sync Cloud",
                                tint = if (cloudSyncSuccess) CreditEmerald else TrustBlue
                            )
                        }
                    }
                }
            }

            // Timeframe Segmented Chips
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(SurfaceLevel2)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    AnalyticsTimeframe.values().forEach { tf ->
                        val isSelected = selectedTimeframe == tf
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) TrustBlue else Color.Transparent)
                                .clickable { selectedTimeframe = tf }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tf.label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else TextSecondary
                                )
                            )
                        }
                    }
                }
            }

            // Sales Summary KPI Hero Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceLevel2),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceOutline)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "कुल बिक्री (Total Sales)",
                                style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(CreditEmeraldDark.copy(alpha = 0.4f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.TrendingUp, contentDescription = null, tint = CreditEmerald, modifier = Modifier.size(14.dp))
                                Text("सक्रिय", style = MaterialTheme.typography.labelSmall.copy(color = CreditEmeraldLight, fontWeight = FontWeight.Bold))
                            }
                        }

                        Text(
                            text = "₹" + String.format(Locale.US, "%,.0f", totalSales),
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = CreditEmeraldLight
                            )
                        )

                        // 3 Sub Stats (Bills, AOV, Growth)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("कुल बिल", style = MaterialTheme.typography.labelSmall.copy(color = TextMuted))
                                Text("$billCount Bills", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = TextPrimary))
                            }
                            Column {
                                Text("औसत बिल मूल्य (AOV)", style = MaterialTheme.typography.labelSmall.copy(color = TextMuted))
                                Text("₹" + String.format(Locale.US, "%,.0f", avgBillValue), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = TrustBlueLight))
                            }
                            Column {
                                Text("कैश vs UPI अनुपात", style = MaterialTheme.typography.labelSmall.copy(color = TextMuted))
                                val upiPct = if (totalSales > 0) (upiSales / totalSales * 100).toInt() else 0
                                Text("$upiPct% Online", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFFF59E0B)))
                            }
                        }
                    }
                }
            }

            // Visual Chart 1: Sales Trend Bar Chart
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceLevel2),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceOutline)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "बिक्री ट्रेंड (Sales Trend)",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                            Text(
                                text = "दैनिक आंकड़े",
                                style = MaterialTheme.typography.labelSmall.copy(color = TrustBlue)
                            )
                        }

                        // Custom Dynamic Bar Chart
                        SalesTrendBarChart(
                            transactions = filteredTransactions,
                            timeframeDays = selectedTimeframe.days
                        )
                    }
                }
            }

            // Visual Chart 2: Payment Split Distribution (Donut / Segmented Progress)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceLevel2),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceOutline)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "पेमेंट माध्यम विभाजन (Payment Distribution)",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )

                        PaymentSplitDonutChart(
                            cashAmount = cashSales,
                            upiAmount = upiSales
                        )
                    }
                }
            }

            // Most Frequently Purchased Items (Top Selling Products)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceLevel2),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceOutline)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "सर्वाधिक बिकने वाली वस्तुएं (Top Products)",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                            Icon(Icons.Default.Inventory2, contentDescription = null, tint = TrustBlue, modifier = Modifier.size(18.dp))
                        }

                        if (topProducts.isEmpty()) {
                            Text("कोई प्रोडक्ट डेटा नहीं है।", style = MaterialTheme.typography.bodySmall.copy(color = TextMuted))
                        } else {
                            topProducts.forEachIndexed { index, product ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(SurfaceLevel3),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "#${index + 1}",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextSecondary
                                                )
                                            )
                                        }
                                        Column {
                                            Text(
                                                text = product.name,
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                                            )
                                            Text(
                                                text = "स्टॉक: ${product.stockQuantity.toInt()} ${product.unit} शेष",
                                                style = MaterialTheme.typography.labelSmall.copy(color = TextMuted)
                                            )
                                        }
                                    }

                                    Text(
                                        text = "₹${product.price.toInt()}",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = CreditEmeraldLight
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Customer Outstanding Balances Report
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceLevel2),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceOutline)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "उधार बकाया रिपोर्ट (Outstanding Credit)",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                            Icon(Icons.Default.People, contentDescription = null, tint = DebitCoral, modifier = Modifier.size(18.dp))
                        }

                        if (outstandingCustomers.isEmpty()) {
                            Text("कोई उधार बकाया नहीं है। सभी खाते चुकता हैं!", style = MaterialTheme.typography.bodySmall.copy(color = CreditEmeraldLight))
                        } else {
                            outstandingCustomers.forEach { customer ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = customer.name,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                                        )
                                        Text(
                                            text = if (customer.dueDaysOverdue > 0) "${customer.dueDaysOverdue} दिन से लंबित" else "नियमित ग्राहक",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = if (customer.dueDaysOverdue > 3) DebitCoralLight else TextMuted
                                            )
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "₹${customer.balanceDue.toInt()}",
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = DebitCoralLight
                                            )
                                        )
                                        IconButton(
                                            onClick = {
                                                UpiHelper.sharePaymentReminderViaShareSheet(
                                                    context = context,
                                                    customerPhone = customer.phone,
                                                    customerName = customer.name,
                                                    amount = customer.balanceDue,
                                                    merchantUpi = merchantProfile.upiId,
                                                    shopName = merchantProfile.shopName,
                                                    dueDate = customer.dueDate
                                                )
                                            },
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(CircleShape)
                                                .background(CreditEmeraldDark.copy(alpha = 0.4f))
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Chat,
                                                contentDescription = "WhatsApp Reminder",
                                                tint = CreditEmerald,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun SalesTrendBarChart(
    transactions: List<TransactionEntity>,
    timeframeDays: Int
) {
    val barCount = 7
    val dayLabels = listOf("सोम", "मंगल", "बुध", "गुरु", "शुक्र", "शनि", "रवि")
    val dayValues = remember(transactions) {
        val values = FloatArray(barCount) { 0f }
        val now = System.currentTimeMillis()
        transactions.forEach { txn ->
            if (txn.type == "CREDIT") {
                val daysAgo = ((now - txn.timestamp) / 86400000L).toInt()
                if (daysAgo in 0 until barCount) {
                    val index = (barCount - 1) - daysAgo
                    values[index] += txn.amount.toFloat()
                }
            }
        }
        // If all 0, provide baseline minimum height so chart renders nicely
        values.toList()
    }

    val maxValue = (dayValues.maxOrNull() ?: 1f).coerceAtLeast(100f)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val barWidth = (canvasWidth / barCount) * 0.55f
            val spacing = canvasWidth / barCount

            dayValues.forEachIndexed { i, value ->
                val barHeight = ((value / maxValue) * (canvasHeight - 20f)).coerceAtLeast(8f)
                val x = (i * spacing) + (spacing - barWidth) / 2f
                val y = canvasHeight - barHeight

                // Draw Bar
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(TrustBlueLight, TrustBlue)
                    ),
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(8f, 8f)
                )
            }
        }

        // Labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            dayLabels.forEach { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                )
            }
        }
    }
}

@Composable
fun PaymentSplitDonutChart(
    cashAmount: Double,
    upiAmount: Double
) {
    val total = (cashAmount + upiAmount).coerceAtLeast(1.0)
    val upiAngle = ((upiAmount / total) * 360f).toFloat()
    val cashAngle = 360f - upiAngle

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        // Donut Canvas
        Box(
            modifier = Modifier.size(110.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 24f
                val diameter = size.minDimension - strokeWidth
                val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)
                val arcSize = Size(diameter, diameter)

                // UPI Arc
                drawArc(
                    color = CreditEmerald,
                    startAngle = -90f,
                    sweepAngle = upiAngle,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                // Cash Arc
                drawArc(
                    color = TrustBlue,
                    startAngle = -90f + upiAngle,
                    sweepAngle = cashAngle,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${((upiAmount / total) * 100).toInt()}%",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = CreditEmeraldLight
                    )
                )
                Text(
                    text = "UPI",
                    style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 10.sp)
                )
            }
        }

        // Legend Breakdown
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(CreditEmerald))
                Column {
                    Text("UPI / Online", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = TextPrimary))
                    Text("₹${upiAmount.toInt()} (${((upiAmount / total) * 100).toInt()}%)", style = MaterialTheme.typography.labelSmall.copy(color = CreditEmeraldLight))
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(TrustBlue))
                Column {
                    Text("Cash (नकद)", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = TextPrimary))
                    Text("₹${cashAmount.toInt()} (${((cashAmount / total) * 100).toInt()}%)", style = MaterialTheme.typography.labelSmall.copy(color = TrustBlueLight))
                }
            }
        }
    }
}
