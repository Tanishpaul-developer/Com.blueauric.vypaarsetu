package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.DiscoveredPrinter
import com.example.service.PrinterConnectionState
import com.example.service.PrinterPaperWidth
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
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrinterSetupScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val printerManager = remember { ThermalPrinterManager.getInstance(context) }

    val connectionState by printerManager.connectionState.collectAsStateWithLifecycle()
    val connectedDevice by printerManager.connectedDevice.collectAsStateWithLifecycle()
    val discoveredDevices by printerManager.discoveredDevices.collectAsStateWithLifecycle()
    val statusMessage by printerManager.statusMessage.collectAsStateWithLifecycle()
    val merchantProfile by viewModel.merchantProfile.collectAsStateWithLifecycle()

    var selectedPaperWidth by remember { mutableStateOf(printerManager.currentPaperWidth) }
    var isTestPrinting by remember { mutableStateOf(false) }

    // Bluetooth Permission Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.all { it }
        if (granted) {
            printerManager.startScan()
        } else {
            Toast.makeText(context, "Bluetooth permissions required to connect to printers", Toast.LENGTH_LONG).show()
        }
    }

    fun requestAndScan() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT
                )
            )
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("ESC/POS Thermal Printer Setup", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            text = if (connectionState == PrinterConnectionState.CONNECTED)
                                "Connected: ${connectedDevice?.name}"
                            else
                                "Wireless 58mm & 80mm Bluetooth Receipts",
                            fontSize = 12.sp,
                            color = if (connectionState == PrinterConnectionState.CONNECTED) CreditMint else TextMuted
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("btn_back_printer")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceLevel1)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Bluetooth Status & Warning
            if (!printerManager.isBluetoothEnabled) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DebitCoral.copy(alpha = 0.12f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DebitCoral.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.Bluetooth, contentDescription = null, tint = DebitCoral, modifier = Modifier.size(28.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Bluetooth is Turned Off", fontWeight = FontWeight.Bold, color = DebitCoral)
                                    Text("Please enable Bluetooth to pair with wireless printers.", fontSize = 12.sp, color = TextMuted)
                                }
                            }
                            Button(
                                onClick = {
                                    context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = DebitCoral)
                            ) {
                                Text("Enable")
                            }
                        }
                    }
                }
            }

            // 2. Active Printer Connection Card
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (connectionState == PrinterConnectionState.CONNECTED)
                            CreditMint.copy(alpha = 0.08f)
                        else
                            SurfaceLevel1
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.5.dp,
                        if (connectionState == PrinterConnectionState.CONNECTED) CreditMint else SurfaceOutline
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = if (connectionState == PrinterConnectionState.CONNECTED) CreditMint else TrustBlue,
                                    shape = CircleShape,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = if (connectionState == PrinterConnectionState.CONNECTED)
                                                Icons.Default.BluetoothConnected
                                            else
                                                Icons.Default.Print,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = connectedDevice?.name ?: "No Printer Connected",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                    Text(
                                        text = connectedDevice?.address ?: "Tap scan below to pair a printer",
                                        fontSize = 12.sp,
                                        color = TextMuted
                                    )
                                }
                            }

                            // Status Chip
                            Surface(
                                color = when (connectionState) {
                                    PrinterConnectionState.CONNECTED -> CreditMint.copy(alpha = 0.2f)
                                    PrinterConnectionState.CONNECTING -> TrustBlue.copy(alpha = 0.2f)
                                    PrinterConnectionState.SCANNING -> TrustBlue.copy(alpha = 0.15f)
                                    else -> TextMuted.copy(alpha = 0.15f)
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = connectionState.name,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when (connectionState) {
                                        PrinterConnectionState.CONNECTED -> CreditMint
                                        PrinterConnectionState.CONNECTING -> TrustBlue
                                        else -> TextMuted
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Status: $statusMessage",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    if (connectionState == PrinterConnectionState.CONNECTED) {
                                        printerManager.disconnect()
                                    } else {
                                        connectedDevice?.let { dev ->
                                            scope.launch { printerManager.connect(dev.address, dev.name) }
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f).testTag("btn_toggle_connect"),
                                enabled = connectedDevice != null
                            ) {
                                Text(if (connectionState == PrinterConnectionState.CONNECTED) "Disconnect" else "Reconnect")
                            }

                            Button(
                                onClick = {
                                    scope.launch {
                                        isTestPrinting = true
                                        val res = printerManager.printTestReceipt(
                                            shopName = merchantProfile.shopName.ifBlank { "VyaparSetu Store" },
                                            paperWidth = selectedPaperWidth
                                        )
                                        isTestPrinting = false
                                        if (res.isSuccess) {
                                            Toast.makeText(context, "Test receipt printed!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Print failed: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f).testTag("btn_test_print"),
                                enabled = connectionState == PrinterConnectionState.CONNECTED && !isTestPrinting,
                                colors = ButtonDefaults.buttonColors(containerColor = TrustBlue)
                            ) {
                                if (isTestPrinting) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Test Print")
                            }
                        }
                    }
                }
            }

            // 3. Paper Width Configuration Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceLevel1),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Thermal Paper Roll Size", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("Select your printer roll width to format column layout and text wrapping.", fontSize = 12.sp, color = TextMuted)
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            FilterChip(
                                selected = selectedPaperWidth == PrinterPaperWidth.WIDTH_58MM,
                                onClick = {
                                    selectedPaperWidth = PrinterPaperWidth.WIDTH_58MM
                                    printerManager.currentPaperWidth = PrinterPaperWidth.WIDTH_58MM
                                },
                                label = { Text("2-Inch / 58mm (32 Col)") },
                                leadingIcon = if (selectedPaperWidth == PrinterPaperWidth.WIDTH_58MM) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null,
                                modifier = Modifier.weight(1f).testTag("chip_paper_58mm")
                            )

                            FilterChip(
                                selected = selectedPaperWidth == PrinterPaperWidth.WIDTH_80MM,
                                onClick = {
                                    selectedPaperWidth = PrinterPaperWidth.WIDTH_80MM
                                    printerManager.currentPaperWidth = PrinterPaperWidth.WIDTH_80MM
                                },
                                label = { Text("3-Inch / 80mm (48 Col)") },
                                leadingIcon = if (selectedPaperWidth == PrinterPaperWidth.WIDTH_80MM) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null,
                                modifier = Modifier.weight(1f).testTag("chip_paper_80mm")
                            )
                        }
                    }
                }
            }

            // 4. Scan & Discovery Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Available Bluetooth Printers", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Pair thermal printer in Android Bluetooth or scan directly", fontSize = 12.sp, color = TextMuted)
                    }

                    Button(
                        onClick = { requestAndScan() },
                        enabled = connectionState != PrinterConnectionState.SCANNING,
                        colors = ButtonDefaults.buttonColors(containerColor = TrustBlue),
                        modifier = Modifier.testTag("btn_scan_printers")
                    ) {
                        if (connectionState == PrinterConnectionState.SCANNING) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Scanning...")
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Scan")
                        }
                    }
                }
            }

            // 5. Device List
            if (discoveredDevices.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceLevel2),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.BluetoothSearching, contentDescription = null, tint = TextMuted, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No printers found yet", fontWeight = FontWeight.SemiBold, color = TextSecondary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Ensure your thermal printer is turned on and Bluetooth is discoverable.",
                                fontSize = 12.sp,
                                color = TextMuted,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(discoveredDevices, key = { it.address }) { device ->
                    val isCurrent = connectedDevice?.address == device.address
                    val isConnectedNow = isCurrent && connectionState == PrinterConnectionState.CONNECTED

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                scope.launch {
                                    printerManager.connect(device.address, device.name)
                                }
                            }
                            .testTag("item_printer_${device.address}"),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isConnectedNow) CreditMint.copy(alpha = 0.08f) else SurfaceLevel1
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isConnectedNow) CreditMint else SurfaceOutline
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    imageVector = if (device.isBonded) Icons.Default.Devices else Icons.Default.Bluetooth,
                                    contentDescription = null,
                                    tint = if (isConnectedNow) CreditMint else TrustBlue,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(device.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(
                                        text = "${device.address} • ${if (device.isBonded) "Paired" else "Discovered"}",
                                        fontSize = 11.sp,
                                        color = TextMuted
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    scope.launch {
                                        printerManager.connect(device.address, device.name)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isConnectedNow) CreditMint else TrustBlue
                                )
                            ) {
                                Text(if (isConnectedNow) "Connected" else "Connect")
                            }
                        }
                    }
                }
            }
        }
    }
}
