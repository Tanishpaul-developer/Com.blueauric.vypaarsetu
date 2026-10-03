package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.AppDatabase
import com.example.data.local.CustomerEntity
import com.example.data.local.InvoiceEntity
import com.example.data.local.ProductEntity
import com.example.service.InvoiceData
import com.example.service.InvoiceGenerator
import com.example.service.InvoiceItem
import com.example.service.InvoiceMode
import com.example.service.PrinterPaperWidth
import com.example.service.SyncWorker
import com.example.service.ThermalPrinterManager
import com.example.ui.theme.CreditMint
import com.example.ui.theme.DebitCoral
import com.example.ui.theme.SurfaceLevel1
import com.example.ui.theme.SurfaceLevel2
import com.example.ui.theme.SurfaceOutline
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TrustBlue
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.VoiceBillingViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceBillingScreen(
    viewModel: MainViewModel,
    voiceViewModel: VoiceBillingViewModel,
    onBack: () -> Unit,
    onNavigatePrinterSetup: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val merchantProfile by viewModel.merchantProfile.collectAsState()
    val allCustomers by viewModel.khataRepository.allCustomers.collectAsState(initial = emptyList())
    val allProducts by viewModel.inventoryRepository.allProducts.collectAsState(initial = emptyList())

    val db = remember { AppDatabase.getInstance(context) }
    val allInvoices by db.invoiceDao().getAllInvoices().collectAsState(initial = emptyList())

    val invoiceGenerator = remember { InvoiceGenerator(context) }
    val printerManager = remember { ThermalPrinterManager.getInstance(context) }

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("New Invoice", "Invoice History (${allInvoices.size})")

    // Form state
    var invoiceMode by remember { mutableStateOf(InvoiceMode.GST_TAX_INVOICE) }
    var invoiceNumber by remember { mutableStateOf("INV-${System.currentTimeMillis() % 1000000}") }
    var customerName by remember { mutableStateOf("") }
    var customerPhone by remember { mutableStateOf("") }
    var customerAddress by remember { mutableStateOf("") }
    var customerGst by remember { mutableStateOf("") }
    var isInterState by remember { mutableStateOf(false) }
    var paymentMode by remember { mutableStateOf("CASH") }
    var paymentStatus by remember { mutableStateOf("PAID") }

    val items = remember {
        mutableStateListOf(
            InvoiceItem(name = "किराना दैनिक सामग्री", hsnCode = "1006", quantity = 1.0, unit = "kg", rate = 120.0, discountPercent = 0.0, taxRatePercent = 5.0)
        )
    }

    // Modal dialogs
    var showAddItemDialog by remember { mutableStateOf(false) }
    var showVoiceDialog by remember { mutableStateOf(false) }
    var isGeneratingPdf by remember { mutableStateOf(false) }

    val calculation = remember(items.toList(), isInterState, invoiceMode) {
        InvoiceGenerator.calculateInvoice(items, isInterState, invoiceMode)
    }

    // Mic permission launcher
    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            voiceViewModel.startListening(context)
            showVoiceDialog = true
        } else {
            Toast.makeText(context, "Microphone permission required for AI voice billing", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Customizable GST Invoice Generator", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            text = "${merchantProfile.shopName} • ${if (invoiceMode == InvoiceMode.GST_TAX_INVOICE) "Tax Invoice" else "Non-GST Estimate"}",
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("btn_back_invoice")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        },
                        modifier = Modifier.testTag("btn_voice_billing_top")
                    ) {
                        Surface(color = TrustBlue, shape = CircleShape, modifier = Modifier.size(36.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Mic, contentDescription = "Voice Billing", tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                    IconButton(onClick = onNavigatePrinterSetup, modifier = Modifier.testTag("btn_printer_setup_top")) {
                        Icon(Icons.Default.Print, contentDescription = "Printer Setup", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceLevel1)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            ScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = SurfaceLevel1,
                contentColor = TrustBlue,
                edgePadding = 16.dp
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            if (selectedTabIndex == 0) {
                // TAB 1: New Invoice Form
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Invoice Mode Selector
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SurfaceLevel1),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("Invoice Type", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    InvoiceMode.values().forEach { mode ->
                                        FilterChip(
                                            selected = invoiceMode == mode,
                                            onClick = { invoiceMode = mode },
                                            label = { Text(mode.displayName, fontSize = 12.sp) },
                                            leadingIcon = if (invoiceMode == mode) {
                                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                            } else null,
                                            modifier = Modifier.weight(1f).testTag("chip_mode_${mode.name}")
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 2. Customer & Place of Supply
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SurfaceLevel1),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Customer Details", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text("Inv: $invoiceNumber", fontSize = 12.sp, color = TextMuted)
                                }

                                OutlinedTextField(
                                    value = customerName,
                                    onValueChange = { customerName = it },
                                    label = { Text("Customer Name / Shop Name") },
                                    modifier = Modifier.fillMaxWidth().testTag("input_cust_name"),
                                    singleLine = true
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedTextField(
                                        value = customerPhone,
                                        onValueChange = { customerPhone = it },
                                        label = { Text("Mobile Phone") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                        modifier = Modifier.weight(1f).testTag("input_cust_phone"),
                                        singleLine = true
                                    )
                                    if (invoiceMode == InvoiceMode.GST_TAX_INVOICE) {
                                        OutlinedTextField(
                                            value = customerGst,
                                            onValueChange = { customerGst = it },
                                            label = { Text("GSTIN (Optional)") },
                                            modifier = Modifier.weight(1f).testTag("input_cust_gst"),
                                            singleLine = true
                                        )
                                    }
                                }

                                OutlinedTextField(
                                    value = customerAddress,
                                    onValueChange = { customerAddress = it },
                                    label = { Text("Billing Address") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )

                                if (invoiceMode == InvoiceMode.GST_TAX_INVOICE) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("Inter-State Supply (IGST)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                            Text(if (isInterState) "Applies Integrated GST (18%)" else "Intra-state CGST (9%) + SGST (9%)", fontSize = 11.sp, color = TextMuted)
                                        }
                                        Switch(
                                            checked = isInterState,
                                            onCheckedChange = { isInterState = it },
                                            modifier = Modifier.testTag("switch_interstate")
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 3. Itemized Entry List
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Items (${items.size})", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    },
                                    modifier = Modifier.testTag("btn_voice_billing_items")
                                ) {
                                    Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(16.dp), tint = TrustBlue)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Voice Add", color = TrustBlue, fontSize = 12.sp)
                                }
                                Button(
                                    onClick = { showAddItemDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = TrustBlue),
                                    modifier = Modifier.testTag("btn_add_item_dialog")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add Item", fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    // Item Cards
                    itemsIndexed(items) { index, item ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SurfaceLevel1),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(
                                        text = "${item.quantity} ${item.unit} @ ₹${item.rate} • GST: ${item.taxRatePercent.toInt()}% ${if (item.hsnCode.isNotBlank()) "• HSN ${item.hsnCode}" else ""}",
                                        fontSize = 12.sp,
                                        color = TextMuted
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "₹${String.format(Locale.US, "%.2f", item.totalAmount)}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = CreditMint
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    IconButton(
                                        onClick = { if (items.size > 1) items.removeAt(index) },
                                        enabled = items.size > 1
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = if (items.size > 1) DebitCoral else TextMuted, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }

                    // 4. Auto-calculated Summary Card
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SurfaceLevel1),
                            border = androidx.compose.foundation.BorderStroke(1.dp, TrustBlue.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Calculation Summary", fontWeight = FontWeight.Bold, fontSize = 15.sp)

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Subtotal", color = TextSecondary, fontSize = 13.sp)
                                    Text("₹${String.format(Locale.US, "%.2f", calculation.subtotal)}", fontSize = 13.sp)
                                }
                                if (calculation.totalDiscount > 0) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Discount", color = DebitCoral, fontSize = 13.sp)
                                        Text("-₹${String.format(Locale.US, "%.2f", calculation.totalDiscount)}", color = DebitCoral, fontSize = 13.sp)
                                    }
                                }
                                if (invoiceMode == InvoiceMode.GST_TAX_INVOICE) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Taxable Amount", color = TextSecondary, fontSize = 13.sp)
                                        Text("₹${String.format(Locale.US, "%.2f", calculation.taxableAmount)}", fontSize = 13.sp)
                                    }
                                    if (isInterState) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("IGST (Integrated)", color = TextSecondary, fontSize = 13.sp)
                                            Text("₹${String.format(Locale.US, "%.2f", calculation.igst)}", fontSize = 13.sp)
                                        }
                                    } else {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("CGST (Central)", color = TextSecondary, fontSize = 13.sp)
                                            Text("₹${String.format(Locale.US, "%.2f", calculation.cgst)}", fontSize = 13.sp)
                                        }
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("SGST (State)", color = TextSecondary, fontSize = 13.sp)
                                            Text("₹${String.format(Locale.US, "%.2f", calculation.sgst)}", fontSize = 13.sp)
                                        }
                                    }
                                }
                                if (calculation.roundOff != 0.0) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Round Off", color = TextSecondary, fontSize = 13.sp)
                                        Text("₹${String.format(Locale.US, "%+.2f", calculation.roundOff)}", fontSize = 13.sp)
                                    }
                                }

                                androidx.compose.material3.HorizontalDivider(color = SurfaceOutline, modifier = Modifier.padding(vertical = 4.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Final Amount Due", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text(
                                        text = "₹${String.format(Locale.US, "%.2f", calculation.finalAmount)}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp,
                                        color = CreditMint
                                    )
                                }
                            }
                        }
                    }

                    // 5. Payment Mode Chips
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SurfaceLevel1),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("Payment Method", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf("CASH", "UPI", "UDHAR", "SPLIT").forEach { mode ->
                                        FilterChip(
                                            selected = paymentMode == mode,
                                            onClick = { paymentMode = mode },
                                            label = { Text(mode, fontSize = 12.sp) },
                                            modifier = Modifier.weight(1f).testTag("chip_pay_$mode")
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 6. Action Execution Buttons
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            // Primary Save & Offline-First Commit
                            Button(
                                onClick = {
                                    scope.launch {
                                        val invoiceData = InvoiceData(
                                            invoiceNumber = invoiceNumber,
                                            mode = invoiceMode,
                                            merchantProfile = merchantProfile,
                                            customerName = customerName.ifBlank { "Cash Customer" },
                                            customerPhone = customerPhone,
                                            customerAddress = customerAddress,
                                            customerGst = customerGst,
                                            isInterState = isInterState,
                                            items = items.toList(),
                                            paymentMode = paymentMode,
                                            paymentStatus = paymentStatus
                                        )

                                        withContext(Dispatchers.IO) {
                                            val entity = InvoiceGenerator.invoiceToEntity(invoiceData, syncStatus = "PENDING")
                                            db.invoiceDao().insertInvoice(entity)
                                        }

                                        // Trigger auto background sync
                                        SyncWorker.enqueueImmediateSync(context)

                                        Toast.makeText(context, "Invoice $invoiceNumber saved locally!", Toast.LENGTH_SHORT).show()
                                        invoiceNumber = "INV-${System.currentTimeMillis() % 1000000}"
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("btn_save_invoice"),
                                colors = ButtonDefaults.buttonColors(containerColor = TrustBlue)
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Save Invoice (Offline First)", fontWeight = FontWeight.Bold)
                            }

                            // Share WhatsApp A4 / A5 PDF
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        scope.launch {
                                            isGeneratingPdf = true
                                            val invoiceData = InvoiceData(
                                                invoiceNumber = invoiceNumber,
                                                mode = invoiceMode,
                                                merchantProfile = merchantProfile,
                                                customerName = customerName.ifBlank { "Customer" },
                                                customerPhone = customerPhone,
                                                customerAddress = customerAddress,
                                                customerGst = customerGst,
                                                isInterState = isInterState,
                                                items = items.toList(),
                                                paymentMode = paymentMode,
                                                paymentStatus = paymentStatus
                                            )
                                            val pdfFile = withContext(Dispatchers.IO) {
                                                invoiceGenerator.generateA4Pdf(invoiceData)
                                            }
                                            isGeneratingPdf = false
                                            invoiceGenerator.shareInvoicePdf(
                                                pdfFile = pdfFile,
                                                customerPhone = customerPhone,
                                                invoiceNo = invoiceNumber,
                                                amount = calculation.finalAmount,
                                                shopName = merchantProfile.shopName
                                            )
                                        }
                                    },
                                    modifier = Modifier.weight(1f).testTag("btn_share_a4_pdf")
                                ) {
                                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("A4 PDF")
                                }

                                OutlinedButton(
                                    onClick = {
                                        scope.launch {
                                            isGeneratingPdf = true
                                            val invoiceData = InvoiceData(
                                                invoiceNumber = invoiceNumber,
                                                mode = invoiceMode,
                                                merchantProfile = merchantProfile,
                                                customerName = customerName.ifBlank { "Customer" },
                                                customerPhone = customerPhone,
                                                customerAddress = customerAddress,
                                                customerGst = customerGst,
                                                isInterState = isInterState,
                                                items = items.toList(),
                                                paymentMode = paymentMode,
                                                paymentStatus = paymentStatus
                                            )
                                            val pdfFile = withContext(Dispatchers.IO) {
                                                invoiceGenerator.generate3InchThermalPdf(invoiceData)
                                            }
                                            isGeneratingPdf = false
                                            invoiceGenerator.shareInvoicePdf(
                                                pdfFile = pdfFile,
                                                customerPhone = customerPhone,
                                                invoiceNo = invoiceNumber,
                                                amount = calculation.finalAmount,
                                                shopName = merchantProfile.shopName
                                            )
                                        }
                                    },
                                    modifier = Modifier.weight(1f).testTag("btn_share_thermal_pdf")
                                ) {
                                    Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("3\" Thermal PDF")
                                }
                            }

                            // ESC/POS Bluetooth Wireless Print Button
                            Button(
                                onClick = {
                                    scope.launch {
                                        val invoiceData = InvoiceData(
                                            invoiceNumber = invoiceNumber,
                                            mode = invoiceMode,
                                            merchantProfile = merchantProfile,
                                            customerName = customerName.ifBlank { "Customer" },
                                            customerPhone = customerPhone,
                                            customerAddress = customerAddress,
                                            customerGst = customerGst,
                                            isInterState = isInterState,
                                            items = items.toList(),
                                            paymentMode = paymentMode,
                                            paymentStatus = paymentStatus
                                        )

                                        val res = printerManager.printInvoice(invoiceData)
                                        if (res.isSuccess) {
                                            Toast.makeText(context, "Thermal invoice printed!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Printer error: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().testTag("btn_print_bluetooth_escpos"),
                                colors = ButtonDefaults.buttonColors(containerColor = CreditMint)
                            ) {
                                Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Wireless ESC/POS Bluetooth Print", fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            } else {
                // TAB 2: Invoice History
                if (allInvoices.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Receipt, contentDescription = null, tint = TextMuted, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No invoices saved yet", fontWeight = FontWeight.SemiBold, color = TextSecondary)
                            Text("Invoices created will appear here for reprint and sharing.", fontSize = 12.sp, color = TextMuted)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(allInvoices) { inv ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SurfaceLevel1),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(inv.invoiceNumber, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                            Text(
                                                SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(inv.invoiceDate)),
                                                fontSize = 11.sp,
                                                color = TextMuted
                                            )
                                        }
                                        Text(
                                            text = "₹${String.format(Locale.US, "%.2f", inv.finalAmount)}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = CreditMint
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Customer: ${inv.customerName} (${inv.customerPhone.ifBlank { "N/A" }})", fontSize = 13.sp)
                                    Text("Mode: ${inv.mode} • ${inv.paymentMode} (${inv.paymentStatus})", fontSize = 12.sp, color = TextSecondary)

                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                val parsedItems = InvoiceGenerator.jsonToItems(inv.itemsJson)
                                                val invData = InvoiceData(
                                                    invoiceNumber = inv.invoiceNumber,
                                                    dateMillis = inv.invoiceDate,
                                                    mode = try { InvoiceMode.valueOf(inv.mode) } catch (e: Exception) { InvoiceMode.GST_TAX_INVOICE },
                                                    merchantProfile = merchantProfile,
                                                    customerName = inv.customerName,
                                                    customerPhone = inv.customerPhone,
                                                    customerAddress = inv.customerAddress,
                                                    customerGst = inv.customerGst,
                                                    isInterState = inv.isInterState,
                                                    items = parsedItems,
                                                    paymentMode = inv.paymentMode,
                                                    paymentStatus = inv.paymentStatus
                                                )
                                                scope.launch {
                                                    val file = withContext(Dispatchers.IO) {
                                                        invoiceGenerator.generateA4Pdf(invData)
                                                    }
                                                    invoiceGenerator.shareInvoicePdf(
                                                        pdfFile = file,
                                                        customerPhone = inv.customerPhone,
                                                        invoiceNo = inv.invoiceNumber,
                                                        amount = inv.finalAmount,
                                                        shopName = merchantProfile.shopName
                                                    )
                                                }
                                            },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Share PDF", fontSize = 12.sp)
                                        }

                                        Button(
                                            onClick = {
                                                val parsedItems = InvoiceGenerator.jsonToItems(inv.itemsJson)
                                                val invData = InvoiceData(
                                                    invoiceNumber = inv.invoiceNumber,
                                                    dateMillis = inv.invoiceDate,
                                                    mode = try { InvoiceMode.valueOf(inv.mode) } catch (e: Exception) { InvoiceMode.GST_TAX_INVOICE },
                                                    merchantProfile = merchantProfile,
                                                    customerName = inv.customerName,
                                                    customerPhone = inv.customerPhone,
                                                    customerAddress = inv.customerAddress,
                                                    customerGst = inv.customerGst,
                                                    isInterState = inv.isInterState,
                                                    items = parsedItems,
                                                    paymentMode = inv.paymentMode,
                                                    paymentStatus = inv.paymentStatus
                                                )
                                                scope.launch {
                                                    printerManager.printInvoice(invData)
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = TrustBlue),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Thermal", fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Dialog: Add Item
    if (showAddItemDialog) {
        var newItemName by remember { mutableStateOf("") }
        var newHsn by remember { mutableStateOf("") }
        var newQty by remember { mutableStateOf("1") }
        var newUnit by remember { mutableStateOf("pcs") }
        var newRate by remember { mutableStateOf("") }
        var newDisc by remember { mutableStateOf("0") }
        var newTax by remember { mutableStateOf(18.0) }

        AlertDialog(
            onDismissRequest = { showAddItemDialog = false },
            title = { Text("Add Item to Invoice") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Quick inventory picker dropdown
                    if (allProducts.isNotEmpty()) {
                        Text("Pick from Inventory (Optional):", fontSize = 12.sp, color = TextMuted)
                        LazyColumn(modifier = Modifier.height(100.dp)) {
                            items(allProducts.take(8)) { prod ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            newItemName = prod.name
                                            newRate = prod.price.toString()
                                            newUnit = prod.unit
                                        }
                                        .padding(vertical = 4.dp),
                                    shape = RoundedCornerShape(6.dp),
                                    color = SurfaceLevel2
                                ) {
                                    Row(
                                        modifier = Modifier.padding(6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(prod.name, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                        Text("₹${prod.price}", fontSize = 12.sp, color = CreditMint)
                                    }
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = newItemName,
                        onValueChange = { newItemName = it },
                        label = { Text("Item Name") },
                        modifier = Modifier.fillMaxWidth().testTag("input_modal_item_name"),
                        singleLine = true
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = newQty,
                            onValueChange = { newQty = it },
                            label = { Text("Qty") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = newUnit,
                            onValueChange = { newUnit = it },
                            label = { Text("Unit (kg/pcs)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = newRate,
                            onValueChange = { newRate = it },
                            label = { Text("Rate (₹)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = newHsn,
                            onValueChange = { newHsn = it },
                            label = { Text("HSN/SAC") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = newDisc,
                            onValueChange = { newDisc = it },
                            label = { Text("Disc %") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Text("GST Bracket:", fontSize = 12.sp, color = TextMuted)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(0.0, 5.0, 12.0, 18.0, 28.0).forEach { bracket ->
                            FilterChip(
                                selected = newTax == bracket,
                                onClick = { newTax = bracket },
                                label = { Text("${bracket.toInt()}%", fontSize = 11.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val q = newQty.toDoubleOrNull() ?: 1.0
                        val r = newRate.toDoubleOrNull() ?: 0.0
                        val d = newDisc.toDoubleOrNull() ?: 0.0
                        if (newItemName.isNotBlank() && r > 0) {
                            items.add(
                                InvoiceItem(
                                    name = newItemName,
                                    hsnCode = newHsn,
                                    quantity = q,
                                    unit = newUnit.ifBlank { "pcs" },
                                    rate = r,
                                    discountPercent = d,
                                    taxRatePercent = newTax
                                )
                            )
                            showAddItemDialog = false
                        } else {
                            Toast.makeText(context, "Please enter item name and valid rate", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TrustBlue)
                ) {
                    Text("Add Item")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAddItemDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Modal Dialog: AI Voice-to-Billing Listener & Parser
    if (showVoiceDialog) {
        val isListening by voiceViewModel.isListening.collectAsState()
        val transcript by voiceViewModel.transcript.collectAsState()
        val isProcessing by voiceViewModel.isProcessing.collectAsState()
        val parsedResult by voiceViewModel.parsedResult.collectAsState()
        val voiceError by voiceViewModel.errorMessage.collectAsState()

        AlertDialog(
            onDismissRequest = {
                voiceViewModel.stopListening()
                showVoiceDialog = false
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = if (isListening) DebitCoral else TrustBlue,
                        shape = CircleShape,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Mic, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("AI Voice-to-Billing (Gemini 2.5)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (isListening) {
                        Text(
                            text = "Listening... Speak in Hindi, Hinglish or English",
                            color = DebitCoral,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "उदा: '2 Lux soap cash bill 80 rupees' या '10 kg atta 40 rate bill banao'",
                            color = TextMuted,
                            fontSize = 11.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }

                    if (isProcessing) {
                        CircularProgressIndicator(modifier = Modifier.size(32.dp), color = TrustBlue)
                        Text("Gemini 2.5 Flash parsing intent...", fontSize = 12.sp, color = TextMuted)
                    }

                    if (transcript.isNotBlank()) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SurfaceLevel2),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Heard:", fontSize = 11.sp, color = TextMuted)
                                Text("\"$transcript\"", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            }
                        }
                    }

                    if (parsedResult != null) {
                        val res = parsedResult!!
                        Card(
                            colors = CardDefaults.cardColors(containerColor = CreditMint.copy(alpha = 0.1f)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CreditMint),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Parsed Intent: ${res.action}", fontWeight = FontWeight.Bold, color = CreditMint, fontSize = 13.sp)
                                if (res.customerName != null) {
                                    Text("Customer: ${res.customerName}", fontSize = 12.sp)
                                }
                                Text("Total: ₹${res.amount}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Items: ${res.items.joinToString { "${it.qty}x ${it.name} (₹${it.price})" }}", fontSize = 12.sp)
                            }
                        }
                    }

                    if (voiceError != null) {
                        Text(voiceError ?: "", color = DebitCoral, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                if (parsedResult != null) {
                    Button(
                        onClick = {
                            val res = parsedResult!!
                            if (!res.customerName.isNullOrBlank()) {
                                customerName = res.customerName
                            }
                            if (res.items.isNotEmpty()) {
                                items.clear()
                                items.addAll(voiceViewModel.toInvoiceItems(res.items))
                            }
                            voiceViewModel.reset()
                            showVoiceDialog = false
                            Toast.makeText(context, "Populated bill items from voice!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CreditMint)
                    ) {
                        Text("Populate Bill")
                    }
                } else if (isListening) {
                    Button(
                        onClick = { voiceViewModel.stopListening() },
                        colors = ButtonDefaults.buttonColors(containerColor = DebitCoral)
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Stop")
                    }
                } else {
                    Button(
                        onClick = { voiceViewModel.startListening(context) },
                        colors = ButtonDefaults.buttonColors(containerColor = TrustBlue)
                    ) {
                        Text("Listen Again")
                    }
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        voiceViewModel.reset()
                        showVoiceDialog = false
                    }
                ) {
                    Text("Close")
                }
            }
        )
    }
}
