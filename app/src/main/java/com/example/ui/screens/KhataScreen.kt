package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.data.local.CreditRiskLevel
import com.example.data.local.CustomerTrustScore
import com.example.util.CreditRiskCalculator
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.itemsIndexed
import com.example.monetization.VyaparNativeAdCard
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.SouthWest
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import com.example.data.local.ProductEntity
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.fragment.app.FragmentActivity
import com.example.service.InvoiceDetails
import com.example.service.PdfInvoiceService
import com.example.util.BiometricAuthManager
import java.io.File
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.CustomerEntity
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
import java.util.Locale

@Composable
fun KhataScreen(
    viewModel: MainViewModel,
    showAddCustomerInitially: Boolean = false,
    onAddCustomerHandled: () -> Unit = {}
) {
    val context = LocalContext.current
    val merchantProfile by viewModel.merchantProfile.collectAsStateWithLifecycle()
    val customers by viewModel.filteredCustomers.collectAsStateWithLifecycle()
    val totalCustomerUdhar by viewModel.totalCustomerUdhar.collectAsStateWithLifecycle()
    val totalSupplierPayable by viewModel.totalSupplierPayable.collectAsStateWithLifecycle()
    val searchQuery by viewModel.khataSearchQuery.collectAsStateWithLifecycle()
    val isShowingSuppliers by viewModel.isShowingSuppliers.collectAsStateWithLifecycle()
    val khataFilter by viewModel.khataFilter.collectAsStateWithLifecycle()
    val isBiometricUnlocked by viewModel.isBiometricUnlocked.collectAsStateWithLifecycle()
    val isLocked = merchantProfile.biometricLockEnabled && !isBiometricUnlocked
    val allKhataEntries by viewModel.khataRepository.allKhataEntries.collectAsStateWithLifecycle(initialValue = emptyList())

    val pdfInvoiceService = remember { PdfInvoiceService(context) }
    var selectedInvoicePdf by remember { mutableStateOf<Triple<CustomerEntity, File, InvoiceDetails>?>(null) }
    var selectedTrustScoreCustomer by remember { mutableStateOf<CustomerTrustScore?>(null) }

    var showAddCustomerSheet by remember { mutableStateOf(showAddCustomerInitially) }
    var recordingJamaForCustomer by remember { mutableStateOf<CustomerEntity?>(null) }
    var addingUdharForCustomer by remember { mutableStateOf<CustomerEntity?>(null) }
    var jamaAmountText by remember { mutableStateOf("") }
    val allProducts by viewModel.allProducts.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                // Sticky Khata Metrics & Summary Card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(SurfaceLevel2)
                        .border(1.dp, SurfaceOutline, RoundedCornerShape(20.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.MenuBook, contentDescription = null, tint = TrustBlue, modifier = Modifier.size(22.dp))
                            Text(
                                text = "Khata Digital Ledger",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                        }

                        if (merchantProfile.biometricLockEnabled) {
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(if (isLocked) DebitCoral.copy(alpha = 0.2f) else CreditEmerald.copy(alpha = 0.2f))
                                    .border(1.dp, if (isLocked) DebitCoral else CreditEmerald, CircleShape)
                                    .clickable {
                                        if (isLocked) {
                                            val act = context as? FragmentActivity
                                            if (act != null) {
                                                BiometricAuthManager.authenticate(
                                                    activity = act,
                                                    title = "खाता बही अनलॉक करें",
                                                    subtitle = "उधार व खाता बही देखने के लिए बायोमेट्रिक लगाएं",
                                                    onSuccess = {
                                                        viewModel.setBiometricUnlocked(true)
                                                        Toast.makeText(context, "खाता बही अनलॉक हो गया!", Toast.LENGTH_SHORT).show()
                                                    },
                                                    onError = { err ->
                                                        Toast.makeText(context, "बायोमेट्रिक विफल: $err", Toast.LENGTH_SHORT).show()
                                                    }
                                                )
                                            } else {
                                                viewModel.setBiometricUnlocked(true)
                                            }
                                        } else {
                                            viewModel.lockBiometric()
                                            Toast.makeText(context, "खाता बही लॉक कर दिया गया", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                        contentDescription = null,
                                        tint = if (isLocked) DebitCoral else CreditEmerald,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = if (isLocked) "बायोमेट्रिक लॉक" else "अनलॉक (सुरक्षित)",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = if (isLocked) DebitCoral else CreditEmerald,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(SurfaceLevel3)
                                    .border(1.dp, SurfaceOutline, CircleShape)
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "आज का बहीखाता",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = TextSecondary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }
                        }
                    }

                    // Split Balance Metrics
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // To Receive (Udhar)
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(modifier = Modifier.size(6.dp).background(DebitCoral, CircleShape))
                                Text(
                                    text = "बाज़ार उधार (TO RECEIVE)",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = TextMuted,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                )
                            }
                            Text(
                                text = "₹ " + String.format(Locale.US, "%,.0f", totalCustomerUdhar ?: 48650.0),
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = DebitCoral
                                )
                            )
                            Text(
                                text = "${customers.count { !it.isSupplier }} ग्राहक बाकी",
                                style = MaterialTheme.typography.labelSmall.copy(color = TextMuted)
                            )
                        }

                        // You Pay (Suppliers)
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(modifier = Modifier.size(6.dp).background(TrustBlue, CircleShape))
                                Text(
                                    text = "देना बाकी (YOU PAY)",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = TextMuted,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                )
                            }
                            Text(
                                text = "₹ " + String.format(Locale.US, "%,.0f", totalSupplierPayable ?: 12300.0),
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TextPrimary
                                )
                            )
                            Text(
                                text = "5 सप्लायर्स",
                                style = MaterialTheme.typography.labelSmall.copy(color = TextMuted)
                            )
                        }
                    }

                    // Settlement Strip
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(SurfaceLevel3)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Verified, contentDescription = null, tint = CreditEmerald, modifier = Modifier.size(16.dp))
                            Text(
                                text = "ऑटो-रिमाइंडर सक्रिय (WhatsApp)",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = TextPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }

                        Button(
                            onClick = {
                                Toast.makeText(context, "Generating Bulk WhatsApp Reminders...", Toast.LENGTH_SHORT).show()
                            },
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CreditEmerald,
                                contentColor = Color.Black
                            ),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.DownloadDone, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Collect Now", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }
            }

            if (isLocked) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(SurfaceLevel2)
                            .border(1.dp, SurfaceOutline, RoundedCornerShape(20.dp))
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(68.dp)
                                .clip(CircleShape)
                                .background(TrustBlue.copy(alpha = 0.15f))
                                .border(2.dp, TrustBlue, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Fingerprint, contentDescription = null, tint = TrustBlue, modifier = Modifier.size(38.dp))
                        }

                        Text(
                            text = "खाता बही सुरक्षित है (Khata Ledger Locked)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                        )

                        Text(
                            text = "गोपनीय ग्राहक उधार व देनदारी देखने के लिए बायोमेट्रिक प्रमाणीकरण करें।",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextMuted),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        Button(
                            onClick = {
                                val act = context as? FragmentActivity
                                if (act != null) {
                                    BiometricAuthManager.authenticate(
                                        activity = act,
                                        title = "खाता बही अनलॉक करें",
                                        subtitle = "ग्राहकों का बहीखाता खोलने हेतु फिंगरप्रिंट लगाएं",
                                        onSuccess = {
                                            viewModel.setBiometricUnlocked(true)
                                            Toast.makeText(context, "खाता बही अनलॉक हो गया!", Toast.LENGTH_SHORT).show()
                                        },
                                        onError = { err ->
                                            Toast.makeText(context, "बायोमेट्रिक त्रुटि: $err", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                } else {
                                    viewModel.setBiometricUnlocked(true)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("unlock_biometric_btn"),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = TrustBlue, contentColor = Color.White)
                        ) {
                            Icon(Icons.Default.Fingerprint, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("फिंगरप्रिंट / फेस से अनलॉक करें", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                item {
                    // Search Input with Voice Trigger
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(CircleShape)
                            .background(SurfaceLevel2)
                            .border(1.dp, SurfaceOutline, CircleShape)
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted, modifier = Modifier.size(20.dp))
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.setKhataSearchQuery(it) },
                            placeholder = { Text("नाम या मोबाइल नंबर से खोजें...", color = TextMuted, fontSize = 13.sp) },
                            modifier = Modifier.weight(1f),
                            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            singleLine = true
                        )
                        IconButton(
                            onClick = {
                                Toast.makeText(context, "Voice Search listening...", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(TrustBlue.copy(alpha = 0.2f))
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = "Voice Search", tint = TrustBlue, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                item {
                    // Segmented Tabs (ग्राहक उधार vs सप्लायर्स)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(CircleShape)
                            .background(SurfaceLevel2)
                            .border(1.dp, SurfaceOutline, CircleShape)
                            .padding(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(CircleShape)
                                .background(if (!isShowingSuppliers) SurfaceLevel3 else Color.Transparent)
                                .clickable { viewModel.setShowSuppliers(false) }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Default.SouthWest,
                                    contentDescription = null,
                                    tint = if (!isShowingSuppliers) CreditEmerald else TextMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "ग्राहक उधार (${customers.size})",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (!isShowingSuppliers) CreditEmerald else TextMuted
                                    )
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(CircleShape)
                                .background(if (isShowingSuppliers) SurfaceLevel3 else Color.Transparent)
                                .clickable { viewModel.setShowSuppliers(true) }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Default.NorthEast,
                                    contentDescription = null,
                                    tint = if (isShowingSuppliers) TrustBlue else TextMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "सप्लायर्स (5)",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isShowingSuppliers) TrustBlue else TextMuted
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Sorting Chips
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val chips = listOf(
                            "ALL" to "सब देखें (All)",
                            "OVERDUE" to "तगादा बाकी (Overdue: 8)",
                            "TODAY" to "आज की देनदारी (Today)"
                        )
                        items(chips, key = { it.first }) { (code, label) ->
                            val isSelected = khataFilter == code
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(if (isSelected) TrustBlue else SurfaceLevel2)
                                    .border(1.dp, if (isSelected) TrustBlue else SurfaceOutline, CircleShape)
                                    .clickable { viewModel.setKhataFilter(code) }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else TextSecondary
                                    )
                                )
                            }
                        }
                    }
                }

                // Customer Cards Feed with Native Ad at position 5
                itemsIndexed(customers, key = { _, customer -> customer.id }) { index, customer ->
                    if (index == 4) {
                        VyaparNativeAdCard()
                    }
                    val trustScore = remember(customer, allKhataEntries) {
                        CreditRiskCalculator.calculateScore(customer, allKhataEntries)
                    }
                    CustomerLedgerCard(
                        customer = customer,
                        trustScore = trustScore,
                        onShowTrustScore = { selectedTrustScoreCustomer = it },
                        onSendWhatsApp = {
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
                        onRecordJama = {
                            recordingJamaForCustomer = customer
                            jamaAmountText = customer.balanceDue.toInt().toString()
                        },
                        onMarkPaid = {
                            viewModel.markCustomerUdharFullyPaid(customer.id)
                            Toast.makeText(context, "${customer.name} का उधार पूरा चुकता हुआ! 🎉", Toast.LENGTH_SHORT).show()
                        },
                        onAddMoreUdhar = {
                            addingUdharForCustomer = customer
                        },
                        onGenerateInvoice = {
                            val invoiceDetails = InvoiceDetails(
                                invoiceNo = "${System.currentTimeMillis() % 1000000}",
                                customerName = customer.name,
                                customerPhone = customer.phone,
                                previousBalance = customer.balanceDue,
                                transactionAmount = if (customer.balanceDue > 0) customer.balanceDue else 500.0,
                                isJama = false,
                                netBalanceDue = customer.balanceDue,
                                itemNote = customer.lastItemDescription.ifBlank { "दुकान राशन सामग्री लेन-देन" },
                                merchantShopName = merchantProfile.shopName,
                                merchantOwnerName = merchantProfile.ownerName,
                                merchantUpiId = merchantProfile.upiId,
                                merchantPhone = merchantProfile.phone
                            )
                            val file = pdfInvoiceService.generateInvoicePdf(invoiceDetails)
                            selectedInvoicePdf = Triple(customer, file, invoiceDetails)
                        }
                    )
                }

                item {
                    // Vyapar Tip Card
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(SurfaceLevel2)
                            .border(1.dp, SurfaceOutline, RoundedCornerShape(18.dp))
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(CreditEmeraldDark),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Campaign, contentDescription = null, tint = CreditEmeraldLight, modifier = Modifier.size(20.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "दुकानदारी टिप (Vyapar Tip):",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                            )
                            Text(
                                text = "शाम 6 बजे से पहले WhatsApp तगादा भेजने पर 40% तेजी से भुगतान आता है।",
                                style = MaterialTheme.typography.labelSmall.copy(color = TextMuted)
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }

        // Anchored Floating Action Button strictly in Bottom Thumb Zone (Only visible when unlocked)
        if (!isLocked) {
            Button(
                onClick = { showAddCustomerSheet = true },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
                    .height(54.dp)
                    .testTag("add_customer_fab"),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = TrustBlue,
                    contentColor = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("+ नया ग्राहक / उधार जोड़ें", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
            }
        }

        // Add Customer Modal Sheet
        if (showAddCustomerSheet) {
            AddCustomerDialog(
                onDismiss = {
                    showAddCustomerSheet = false
                    onAddCustomerHandled()
                },
                onSave = { name, phone, address, amount, note ->
                    viewModel.addNewCustomer(name, phone, address, amount, note)
                    showAddCustomerSheet = false
                    onAddCustomerHandled()
                    Toast.makeText(context, "Added $name to Khata", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // Record Jama Payment Dialog
        recordingJamaForCustomer?.let { cust ->
            RecordPaymentDialog(
                customer = cust,
                amountText = jamaAmountText,
                onAmountChange = { jamaAmountText = it },
                onDismiss = { recordingJamaForCustomer = null },
                onConfirm = {
                    val amt = jamaAmountText.toDoubleOrNull() ?: 0.0
                    viewModel.recordCustomerJama(cust.id, amt, "Jama Payment via UPI/Cash")
                    recordingJamaForCustomer = null
                    Toast.makeText(context, "₹$amt Jama recorded for ${cust.name}", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // Record Udhar with optional inventory items dialog
        addingUdharForCustomer?.let { cust ->
            AddCustomerUdharDialog(
                customer = cust,
                availableProducts = allProducts,
                onDismiss = { addingUdharForCustomer = null },
                onConfirm = { amount, note, selectedItems ->
                    viewModel.recordCustomerUdharWithItems(
                        customerId = cust.id,
                        customerName = cust.name,
                        amount = amount,
                        description = note,
                        selectedProductsWithQty = selectedItems
                    )
                    addingUdharForCustomer = null
                    Toast.makeText(context, "₹${amount.toInt()} उधार दर्ज हुआ और स्टॉक अपडेट हुआ!", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // Invoice Share and Print Dialog
        selectedInvoicePdf?.let { (customer, file, details) ->
            InvoiceSharePrintDialog(
                customer = customer,
                invoiceFile = file,
                invoiceDetails = details,
                onDismiss = { selectedInvoicePdf = null },
                onPrint = {
                    pdfInvoiceService.printInvoice(file, "Invoice_${details.invoiceNo}")
                },
                onWhatsAppShare = {
                    pdfInvoiceService.shareInvoiceViaWhatsApp(
                        file = file,
                        customerPhone = customer.phone,
                        customerName = customer.name,
                        amountDue = details.netBalanceDue
                    )
                }
            )
        }

        // Customer Trust Score & Credit Risk Breakdown Dialog (Killer Feature #2)
        selectedTrustScoreCustomer?.let { score ->
            AlertDialog(
                onDismissRequest = { selectedTrustScoreCustomer = null },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(score.riskLevel.badgeEmoji, fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("${score.customerName} - Trust Score", fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SurfaceLevel2),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Credit Risk: ${score.riskLevel.label} (${score.score}/100)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = when (score.riskLevel) {
                                        CreditRiskLevel.LOW_RISK -> CreditEmerald
                                        CreditRiskLevel.MODERATE_RISK -> Color(0xFFFBBF24)
                                        CreditRiskLevel.HIGH_RISK -> DebitCoral
                                    }
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Average debt clearance: ${score.avgRepaymentDays} days", fontSize = 13.sp)
                                Text("Historical repayments: ${score.totalRepaidCount} times", fontSize = 12.sp, color = TextMuted)
                                if (score.overdueCount > 0) {
                                    Text("Overdue entries: ${score.overdueCount}", fontSize = 12.sp, color = DebitCoral)
                                }
                            }
                        }

                        Text("Merchant Credit Advice:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(
                            text = CreditRiskCalculator.getMerchantAdvice(score),
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { selectedTrustScoreCustomer = null },
                        colors = ButtonDefaults.buttonColors(containerColor = TrustBlue)
                    ) {
                        Text("Understood")
                    }
                }
            )
        }
    }
}

@Composable
fun CustomerLedgerCard(
    customer: CustomerEntity,
    trustScore: CustomerTrustScore? = null,
    onShowTrustScore: (CustomerTrustScore) -> Unit = {},
    onSendWhatsApp: () -> Unit,
    onRecordJama: () -> Unit,
    onMarkPaid: () -> Unit = {},
    onAddMoreUdhar: () -> Unit,
    onGenerateInvoice: () -> Unit
) {
    val initials = customer.name.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("").ifBlank { "CU" }
    val isOverdue = customer.dueDaysOverdue > 0

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceLevel2)
            .border(
                1.dp,
                if (isOverdue) DebitCoral.copy(alpha = 0.4f) else SurfaceOutline,
                RoundedCornerShape(20.dp)
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Top row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(if (isOverdue) DebitCoralDark else SurfaceLevel3),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initials,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isOverdue) DebitCoralLight else TextPrimary
                        )
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = customer.name,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (isOverdue) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(DebitCoralDark)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${customer.dueDaysOverdue} दिन लेट",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = DebitCoralLight,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                )
                            }
                        }
                    }

                    Text(
                        text = "+91 ${customer.phone} • ${customer.address.ifBlank { "दुकान ग्राहक" }}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextMuted
                        )
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "कुल बाकी (Due)",
                    style = MaterialTheme.typography.labelSmall.copy(color = TextMuted)
                )
                Text(
                    text = "₹ " + String.format(Locale.US, "%,.0f", customer.balanceDue),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = if (customer.balanceDue > 0) DebitCoral else CreditEmerald
                    )
                )
            }
        }

        // Customer Trust Score & Credit Risk Badge (Killer Feature #2)
        if (trustScore != null) {
            val badgeBg = when (trustScore.riskLevel) {
                CreditRiskLevel.LOW_RISK -> Color(0xFF065F46)
                CreditRiskLevel.MODERATE_RISK -> Color(0xFF78350F)
                CreditRiskLevel.HIGH_RISK -> Color(0xFF7F1D1D)
            }
            val badgeText = when (trustScore.riskLevel) {
                CreditRiskLevel.LOW_RISK -> Color(0xFF34D399)
                CreditRiskLevel.MODERATE_RISK -> Color(0xFFFBBF24)
                CreditRiskLevel.HIGH_RISK -> Color(0xFFF87171)
            }

            Surface(
                color = badgeBg.copy(alpha = 0.35f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, badgeText.copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onShowTrustScore(trustScore) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(trustScore.riskLevel.badgeEmoji, fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${trustScore.riskLevel.label} (Score: ${trustScore.score}/100)",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = badgeText
                            )
                        )
                    }
                    Text(
                        text = "Avg ${trustScore.avgRepaymentDays} days to pay >",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    )
                }
            }
        }

        // Recent Basket Metadata Tag
        if (customer.lastItemDescription.isNotBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceLevel1)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.ShoppingBag, contentDescription = null, tint = TextMuted, modifier = Modifier.size(14.dp))
                    Text(
                        text = "अंतिम खरीद: ${customer.lastItemDescription}",
                        style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text("कल शाम", style = MaterialTheme.typography.labelSmall.copy(color = TextMuted))
            }
        }

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onSendWhatsApp,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("whatsapp_reminder_${customer.id}"),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = CreditEmerald,
                    contentColor = Color.Black
                )
            ) {
                Icon(Icons.Default.Share, contentDescription = "Share Reminder via Any App", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("तगादा शेयर (ShareSheet)", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
            }

            Button(
                onClick = onRecordJama,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("record_jama_${customer.id}"),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = TrustBlue,
                    contentColor = Color.White
                )
            ) {
                Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("जमा करें (Received)", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
            }
        }

        // Full Udhar Cleared (Mark Paid) Celebration Action
        if (customer.balanceDue > 0) {
            Button(
                onClick = onMarkPaid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .testTag("mark_paid_btn_${customer.id}"),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF10B981),
                    contentColor = Color.White
                )
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "पूरा उधार चुकता हुआ (Mark Paid - ₹${customer.balanceDue.toInt()}) 🎉",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold)
                )
            }
        }

        // PDF Invoice Generation & Print Action Button
        OutlinedButton(
            onClick = onGenerateInvoice,
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .testTag("generate_invoice_${customer.id}"),
            shape = CircleShape,
            border = BorderStroke(1.dp, TrustBlue.copy(alpha = 0.5f))
        ) {
            Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = TrustBlue, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "बिल रसीद (PDF Invoice) बनाएं व प्रिंट करें",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = TrustBlue)
            )
        }
    }
}

@Composable
fun AddCustomerDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, phone: String, address: String, amount: Double, note: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(SurfaceLevel2)
                .clickable(onClick = { /* keep dialog open */ })
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "नया खाता एंट्री (New Customer Khata)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("ग्राहक का नाम (Customer Name)") },
                modifier = Modifier.fillMaxWidth().testTag("add_cust_name"),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("मोबाइल नंबर (Mobile Number)") },
                modifier = Modifier.fillMaxWidth().testTag("add_cust_phone"),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true
            )

            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("उधार राशि (Due Amount ₹)") },
                modifier = Modifier.fillMaxWidth().testTag("add_cust_amount"),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("सामान विवरण (Items / Parchi Note)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceLevel3)
                ) {
                    Text("रद्द करें (Cancel)", color = TextPrimary)
                }

                Button(
                    onClick = {
                        val due = amount.toDoubleOrNull() ?: 0.0
                        onSave(name.ifBlank { "नया ग्राहक" }, phone, address, due, note)
                    },
                    modifier = Modifier.weight(1f).height(50.dp).testTag("save_customer_btn"),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = TrustBlue, contentColor = Color.White)
                ) {
                    Text("खाते में जोड़ें (Save)", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun RecordPaymentDialog(
    customer: CustomerEntity,
    amountText: String,
    onAmountChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clip(RoundedCornerShape(24.dp))
                .background(SurfaceLevel2)
                .clickable(onClick = { /* stay */ })
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "${customer.name} - भुगतान जमा (Receive Jama)",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
            )

            Text(
                text = "वर्तमान बाकी: ₹${customer.balanceDue.toInt()}",
                style = MaterialTheme.typography.bodySmall.copy(color = DebitCoralLight)
            )

            OutlinedTextField(
                value = amountText,
                onValueChange = onAmountChange,
                label = { Text("प्राप्त राशि (Received Amount ₹)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceLevel3)
                ) {
                    Text("Cancel", color = TextPrimary)
                }

                Button(
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = CreditEmerald, contentColor = Color.Black)
                ) {
                    Text("जमा करें / Save", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

fun openBrowserOrWhatsApp(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Cannot launch WhatsApp: $url", Toast.LENGTH_LONG).show()
    }
}

@Composable
fun InvoiceSharePrintDialog(
    customer: CustomerEntity,
    invoiceFile: File,
    invoiceDetails: InvoiceDetails,
    onDismiss: () -> Unit,
    onPrint: () -> Unit,
    onWhatsAppShare: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.82f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(24.dp))
                .background(SurfaceLevel2)
                .border(1.dp, SurfaceOutline, RoundedCornerShape(24.dp))
                .clickable(onClick = { /* keep open */ })
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = TrustBlue, modifier = Modifier.size(22.dp))
                    Text(
                        text = "व्यापार चालान (PDF Invoice)",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                }
            }

            // Customer and Invoice details preview box
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(SurfaceLevel3)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("ग्राहक (Customer):", style = MaterialTheme.typography.labelSmall.copy(color = TextMuted))
                    Text(customer.name, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = TextPrimary))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("बिल नंबर (Invoice No):", style = MaterialTheme.typography.labelSmall.copy(color = TextMuted))
                    Text("#${invoiceDetails.invoiceNo}", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = TextPrimary))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("कुल देय (Total Due):", style = MaterialTheme.typography.labelSmall.copy(color = TextMuted))
                    Text(
                        text = "₹ " + String.format(Locale.US, "%,.2f", invoiceDetails.netBalanceDue),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = if (invoiceDetails.netBalanceDue > 0) DebitCoral else CreditEmerald
                        )
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("फ़ाइल स्वरूप (Format):", style = MaterialTheme.typography.labelSmall.copy(color = TextMuted))
                    Text("A4 Professional PDF with UPI QR", style = MaterialTheme.typography.labelSmall.copy(color = CreditEmerald, fontWeight = FontWeight.SemiBold))
                }
            }

            Text(
                text = "चालान में दुकान का नाम, पिछला बकाया, नया लेन-देन और तुरंत भुगतान हेतु UPI QR कोड स्वतः संलग्न है।",
                style = MaterialTheme.typography.labelSmall.copy(color = TextMuted)
            )

            // Action Buttons
            Button(
                onClick = onWhatsAppShare,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("send_whatsapp_invoice_btn"),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = CreditEmerald, contentColor = Color.Black)
            ) {
                Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("WhatsApp पर PDF रसीद भेजें", fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = onPrint,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("print_invoice_btn"),
                shape = CircleShape
            ) {
                Icon(Icons.Default.Print, contentDescription = null, tint = TrustBlue, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("प्रिंटर पर प्रिंट करें (Print via PrintManager)", fontWeight = FontWeight.Bold, color = TrustBlue)
            }
        }
    }
}

@Composable
fun AddCustomerUdharDialog(
    customer: CustomerEntity,
    availableProducts: List<ProductEntity>,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, note: String, selectedItems: List<Pair<ProductEntity, Double>>) -> Unit
) {
    var mode by remember { mutableStateOf("INVENTORY") } // "INVENTORY" or "CUSTOM"
    var customAmountText by remember { mutableStateOf("") }
    var customNoteText by remember { mutableStateOf("") }
    var productSearchQuery by remember { mutableStateOf("") }

    // Map of product ID to quantity selected
    val selectedItemQtys = remember { androidx.compose.runtime.mutableStateMapOf<Long, Double>() }

    val filteredProducts = remember(availableProducts, productSearchQuery) {
        if (productSearchQuery.isBlank()) availableProducts
        else availableProducts.filter {
            it.name.contains(productSearchQuery, ignoreCase = true) ||
            it.localizedName.contains(productSearchQuery, ignoreCase = true)
        }
    }

    val inventoryTotal = remember(selectedItemQtys.values.toList()) {
        availableProducts.sumOf { prod ->
            val qty = selectedItemQtys[prod.id] ?: 0.0
            prod.price * qty
        }
    }

    val finalAmount = if (mode == "INVENTORY") inventoryTotal else (customAmountText.toDoubleOrNull() ?: 0.0)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(24.dp))
                .background(SurfaceLevel2)
                .border(1.dp, SurfaceOutline, RoundedCornerShape(24.dp))
                .clickable(onClick = {})
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "+ नया उधार दर्ज करें (Give Credit)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                    )
                    Text(
                        text = "${customer.name} • मौजूदा बकाया: ₹${customer.balanceDue.toInt()}",
                        style = MaterialTheme.typography.labelSmall.copy(color = DebitCoralLight)
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                }
            }

            // Mode Selector: Pick from Stock vs Manual Amount
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceLevel3)
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (mode == "INVENTORY") TrustBlue else Color.Transparent)
                        .clickable { mode = "INVENTORY" }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.Inventory2, contentDescription = null, tint = if (mode == "INVENTORY") Color.White else TextMuted, modifier = Modifier.size(16.dp))
                        Text("स्टॉक से चुनें", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = if (mode == "INVENTORY") Color.White else TextPrimary))
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (mode == "CUSTOM") TrustBlue else Color.Transparent)
                        .clickable { mode = "CUSTOM" }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("सीधी राशि (Custom)", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = if (mode == "CUSTOM") Color.White else TextPrimary))
                }
            }

            if (mode == "INVENTORY") {
                // Search inventory
                OutlinedTextField(
                    value = productSearchQuery,
                    onValueChange = { productSearchQuery = it },
                    placeholder = { Text("दुकान का सामान खोजें...", color = TextMuted, fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp)) }
                )

                // Products list with + / -
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(230.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (filteredProducts.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp), contentAlignment = Alignment.Center) {
                                Text("कोई सामान नहीं मिला।", style = MaterialTheme.typography.labelSmall.copy(color = TextMuted))
                            }
                        }
                    } else {
                        items(filteredProducts, key = { it.id }) { prod ->
                            val currentQty = selectedItemQtys[prod.id] ?: 0.0
                            val isSelected = currentQty > 0
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) TrustBlue.copy(alpha = 0.12f) else SurfaceLevel3)
                                    .border(1.dp, if (isSelected) TrustBlue.copy(alpha = 0.5f) else SurfaceOutline, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = prod.name,
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                                    )
                                    Text(
                                        text = "₹${prod.price.toInt()} / ${prod.unit} • स्टॉक: ${prod.stockQuantity.toInt()}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = if (prod.stockQuantity <= prod.lowStockThreshold) DebitCoralLight else CreditEmeraldLight
                                        )
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    if (currentQty > 0) {
                                        IconButton(
                                            onClick = {
                                                if (currentQty <= 1.0) selectedItemQtys.remove(prod.id)
                                                else selectedItemQtys[prod.id] = currentQty - 1.0
                                            },
                                            modifier = Modifier.size(28.dp).clip(CircleShape).background(SurfaceLevel2)
                                        ) {
                                            Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = DebitCoral, modifier = Modifier.size(16.dp))
                                        }

                                        Text(
                                            text = "${currentQty.toInt()}",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            selectedItemQtys[prod.id] = currentQty + 1.0
                                        },
                                        modifier = Modifier.size(28.dp).clip(CircleShape).background(CreditEmerald)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Increase", tint = Color.Black, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Manual Amount & Note
                OutlinedTextField(
                    value = customAmountText,
                    onValueChange = { customAmountText = it },
                    label = { Text("उधार राशि (Amount ₹)") },
                    modifier = Modifier.fillMaxWidth().testTag("udhar_amount_input"),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    singleLine = true
                )

                // Quick amount chips
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(200.0, 500.0, 1000.0, 2000.0).forEach { chipAmt ->
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(SurfaceLevel3)
                                .border(1.dp, SurfaceOutline, CircleShape)
                                .clickable { customAmountText = chipAmt.toInt().toString() }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text("+₹${chipAmt.toInt()}", style = MaterialTheme.typography.labelSmall.copy(color = TrustBlue, fontWeight = FontWeight.Bold))
                        }
                    }
                }

                OutlinedTextField(
                    value = customNoteText,
                    onValueChange = { customNoteText = it },
                    label = { Text("सामग्री विवरण (Item Note)") },
                    placeholder = { Text("उदा. राशन सामान, तेल, आटा...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            // Confirm Button
            Button(
                onClick = {
                    if (finalAmount > 0) {
                        val selectedList = if (mode == "INVENTORY") {
                            availableProducts
                                .filter { (selectedItemQtys[it.id] ?: 0.0) > 0 }
                                .map { it to (selectedItemQtys[it.id] ?: 0.0) }
                        } else emptyList()

                        val note = if (mode == "INVENTORY") {
                            selectedList.joinToString(", ") { "${it.first.name} x${it.second.toInt()}" }.ifBlank { "स्टॉक सामान उधार" }
                        } else {
                            customNoteText.ifBlank { "नया उधार" }
                        }

                        onConfirm(finalAmount, note, selectedList)
                    }
                },
                enabled = finalAmount > 0,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("confirm_record_udhar_btn"),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = DebitCoral, contentColor = Color.White)
            ) {
                Text(
                    text = "₹${finalAmount.toInt()} उधार दर्ज करें ${if (mode == "INVENTORY") "& स्टॉक घटाएं" else ""}",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}
