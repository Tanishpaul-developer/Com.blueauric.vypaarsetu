package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.NotificationImportant
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import com.example.data.local.StockMovementEntity
import java.text.SimpleDateFormat
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
import com.example.data.local.ProductEntity
import com.example.service.SoundboxSpeaker
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
import com.example.ui.viewmodel.MainViewModel
import java.util.Locale

@Composable
fun InventoryScreen(
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    val profile by viewModel.merchantProfile.collectAsStateWithLifecycle()
    val allProducts by viewModel.allProducts.collectAsStateWithLifecycle()
    val lowStockProducts by viewModel.lowStockProducts.collectAsStateWithLifecycle()
    val lowStockCount by viewModel.lowStockCount.collectAsStateWithLifecycle()
    val totalInventoryValue by viewModel.totalInventoryValue.collectAsStateWithLifecycle()
    val recentMovements by viewModel.recentStockMovements.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("सभी (All)") }

    var showAddDialog by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<ProductEntity?>(null) }
    var restockingProduct by remember { mutableStateOf<ProductEntity?>(null) }
    var deletingProduct by remember { mutableStateOf<ProductEntity?>(null) }
    var editingThresholdProduct by remember { mutableStateOf<ProductEntity?>(null) }
    var showAlertSettingsDialog by remember { mutableStateOf(false) }
    var showStockHistoryDialog by remember { mutableStateOf(false) }
    var isSyncingCloud by remember { mutableStateOf(false) }

    val categories = listOf(
        "सभी (All)",
        "कम स्टॉक (Low Stock)",
        "किराना (Grocery)",
        "तेल व घी (Oils & Ghee)",
        "डेयरी (Dairy)",
        "मसाले (Spices)",
        "स्नैक्स (Snacks)",
        "साफ-सफाई (Cleaning)"
    )

    val filteredProducts = allProducts.filter { product ->
        val matchesCategory = when (selectedCategory) {
            "सभी (All)" -> true
            "कम स्टॉक (Low Stock)" -> product.stockQuantity <= product.lowStockThreshold
            else -> product.category.contains(selectedCategory.split(" ")[0])
        }
        val matchesSearch = searchQuery.isBlank() ||
                product.name.contains(searchQuery, ignoreCase = true) ||
                product.localizedName.contains(searchQuery, ignoreCase = true) ||
                product.category.contains(searchQuery, ignoreCase = true) ||
                product.barcode.contains(searchQuery)

        matchesCategory && matchesSearch
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                // Top Header with Firebase Sync Action
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.Inventory2,
                                contentDescription = null,
                                tint = TrustBlue,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "INVENTORY & CATALOG",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = TrustBlue,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            )
                        }
                        Text(
                            text = "स्टॉक व कैटलॉग प्रबंधन",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = TextPrimary
                            )
                        )
                    }

                    // Action Pills: Stock Movement History, Alert Settings, Cloud Sync
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Stock Movement History Pill
                        Row(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(SurfaceLevel2)
                                .border(1.dp, SurfaceOutline, CircleShape)
                                .clickable { showStockHistoryDialog = true }
                                .padding(horizontal = 9.dp, vertical = 6.dp)
                                .testTag("stock_history_btn"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.History, contentDescription = "History", tint = TrustBlue, modifier = Modifier.size(15.dp))
                            Text("Logs", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = TextPrimary))
                        }

                        // Low-Stock Alert Settings Pill
                        Row(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(SurfaceLevel2)
                                .border(1.dp, if (lowStockCount > 0) DebitCoral.copy(alpha = 0.6f) else SurfaceOutline, CircleShape)
                                .clickable { showAlertSettingsDialog = true }
                                .padding(horizontal = 9.dp, vertical = 6.dp)
                                .testTag("inventory_alert_settings_btn"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = "Alerts", tint = if (lowStockCount > 0) DebitCoral else TrustBlue, modifier = Modifier.size(15.dp))
                            Text("Alerts", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = if (lowStockCount > 0) DebitCoralLight else TextPrimary))
                        }

                        // Cloud Sync Pill
                        Row(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(SurfaceLevel2)
                                .border(1.dp, SurfaceOutline, CircleShape)
                                .clickable {
                                    isSyncingCloud = true
                                    viewModel.syncInventoryWithCloud { success ->
                                        isSyncingCloud = false
                                        Toast.makeText(
                                            context,
                                            if (success) "✓ Firebase Firestore Synced!" else "Cloud sync offline",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                                .padding(horizontal = 9.dp, vertical = 6.dp)
                                .testTag("sync_inventory_firebase_btn"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (isSyncingCloud) Icons.Default.CloudSync else Icons.Default.CloudDone,
                                contentDescription = "Sync",
                                tint = if (isSyncingCloud) TrustBlue else CreditEmerald,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }

            // Summary Metrics Card
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(SurfaceLevel2)
                        .border(1.dp, SurfaceOutline, RoundedCornerShape(20.dp))
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("कुल उत्पाद (Total SKUs)", style = MaterialTheme.typography.labelSmall.copy(color = TextMuted))
                        Text(
                            text = "${allProducts.size} आइटम्स",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = TextPrimary
                            )
                        )
                        Text("स्टॉक में: ${allProducts.count { it.stockQuantity > it.lowStockThreshold }}", style = MaterialTheme.typography.labelSmall.copy(color = CreditEmerald))
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("कम स्टॉक (Alerts)", style = MaterialTheme.typography.labelSmall.copy(color = TextMuted))
                        Text(
                            text = "$lowStockCount",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = if (lowStockCount > 0) DebitCoral else CreditEmerald
                            ),
                            modifier = Modifier.testTag("low_stock_badge_count")
                        )
                        Text(if (lowStockCount > 0) "तुरंत ऑर्डर करें" else "सब पर्याप्त है", style = MaterialTheme.typography.labelSmall.copy(color = if (lowStockCount > 0) DebitCoralLight else TextMuted))
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text("स्टॉक मूल्य (Value)", style = MaterialTheme.typography.labelSmall.copy(color = TextMuted))
                        Text(
                            text = "₹ " + String.format(Locale.US, "%,.0f", totalInventoryValue ?: 0.0),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = TrustBlue
                            )
                        )
                        Text("दुकान माल पूंजी", style = MaterialTheme.typography.labelSmall.copy(color = TextMuted))
                    }
                }
            }

            // Low Stock Alert Banner (Prominent In-App Warning with TTS soundbox trigger)
            if (lowStockProducts.isNotEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(DebitCoralDark.copy(alpha = 0.35f))
                            .border(1.dp, DebitCoral.copy(alpha = 0.6f), RoundedCornerShape(18.dp))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
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
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(DebitCoral),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.NotificationImportant,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "कम स्टॉक चेतावनी (Low Stock Alert: ${lowStockProducts.size} Items)",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = DebitCoralLight
                                        )
                                    )
                                    Text(
                                        text = "ये सामान खत्म होने की कगार पर हैं, सप्लायर से नया स्टॉक मंगाएं",
                                        style = MaterialTheme.typography.labelSmall.copy(color = TextMuted)
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // Settings button to adjust thresholds
                                IconButton(
                                    onClick = { showAlertSettingsDialog = true },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(DebitCoral.copy(alpha = 0.25f))
                                        .testTag("alert_banner_settings_btn")
                                ) {
                                    Icon(
                                        Icons.Default.Tune,
                                        contentDescription = "Settings",
                                        tint = DebitCoralLight,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                // TTS Voice Alert Trigger Button
                                IconButton(
                                    onClick = {
                                        val firstLow = lowStockProducts.firstOrNull()
                                        if (firstLow != null) {
                                            SoundboxSpeaker.announceLowStock(
                                                context = context,
                                                productName = firstLow.name,
                                                remainingQty = firstLow.stockQuantity,
                                                unit = firstLow.unit,
                                                languageCode = profile.languageCode
                                            )
                                            Toast.makeText(context, "साउंडबॉक्स चेतावनी बोली गई!", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(DebitCoral.copy(alpha = 0.3f))
                                        .testTag("tts_low_stock_announcement_btn")
                                ) {
                                    Icon(
                                        Icons.Default.VolumeUp,
                                        contentDescription = "Voice Alert",
                                        tint = DebitCoralLight,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        // Horizontal ticker of low stock items
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            items(lowStockProducts, key = { it.id }) { item ->
                                Row(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(SurfaceLevel2)
                                        .border(1.dp, DebitCoral.copy(alpha = 0.5f), CircleShape)
                                        .clickable { restockingProduct = item }
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = item.name.take(18) + (if (item.name.length > 18) "..." else ""),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                    )
                                    Text(
                                        text = "${item.stockQuantity.toInt()} ${item.unit} बाकी",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = DebitCoral,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                    Icon(
                                        Icons.Default.AddCircle,
                                        contentDescription = "Restock",
                                        tint = CreditEmerald,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Search Bar
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(CircleShape)
                        .background(SurfaceLevel2)
                        .border(1.dp, SurfaceOutline, CircleShape)
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp))
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("सामान या बारकोड खोजें...", color = TextMuted, fontSize = 13.sp) },
                        modifier = Modifier.weight(1f),
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        singleLine = true
                    )
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextMuted, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // Category Filter Chips
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories, key = { it }) { cat ->
                        val isSelected = selectedCategory == cat
                        val isLowStockCat = cat.contains("कम स्टॉक")
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) {
                                        if (isLowStockCat) DebitCoral else TrustBlue
                                    } else SurfaceLevel2
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) Color.Transparent else if (isLowStockCat) DebitCoral.copy(alpha = 0.5f) else SurfaceOutline,
                                    CircleShape
                                )
                                .clickable { selectedCategory = cat }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = cat,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else if (isLowStockCat) DebitCoralLight else TextSecondary
                                )
                            )
                        }
                    }
                }
            }

            // Products List
            if (filteredProducts.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Inventory2, contentDescription = null, tint = TextMuted, modifier = Modifier.size(40.dp))
                        Text(
                            text = "कोई उत्पाद नहीं मिला (No Products Found)",
                            style = MaterialTheme.typography.bodyMedium.copy(color = TextMuted)
                        )
                        OutlinedButton(
                            onClick = { showAddDialog = true },
                            shape = CircleShape
                        ) {
                            Text("+ नया सामान कैटलॉग में जोड़ें")
                        }
                    }
                }
            } else {
                items(filteredProducts, key = { it.id }) { product ->
                    ProductCatalogItemCard(
                        product = product,
                        onRestock = { restockingProduct = product },
                        onEdit = { editingProduct = product },
                        onDelete = { deletingProduct = product },
                        onEditThreshold = { editingThresholdProduct = product }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        // Add Product Floating Action Button
        Button(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 20.dp, end = 20.dp)
                .height(52.dp)
                .testTag("add_product_fab"),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = TrustBlue,
                contentColor = Color.White
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("+ नया सामान जोड़ें", fontWeight = FontWeight.Bold)
        }

        // Add / Edit Product Dialog
        if (showAddDialog) {
            AddEditProductDialog(
                productToEdit = null,
                onDismiss = { showAddDialog = false },
                onSave = { name, localizedName, category, price, unit, stock, threshold, barcode ->
                    viewModel.addProduct(name, localizedName, category, price, unit, stock, threshold, barcode)
                    showAddDialog = false
                    Toast.makeText(context, "$name कैटलॉग में जोड़ा गया!", Toast.LENGTH_SHORT).show()
                }
            )
        }

        editingProduct?.let { prod ->
            AddEditProductDialog(
                productToEdit = prod,
                onDismiss = { editingProduct = null },
                onSave = { name, localizedName, category, price, unit, stock, threshold, barcode ->
                    val updated = prod.copy(
                        name = name,
                        localizedName = localizedName,
                        category = category,
                        price = price,
                        unit = unit,
                        stockQuantity = stock,
                        lowStockThreshold = threshold,
                        barcode = barcode
                    )
                    viewModel.updateProduct(updated)
                    editingProduct = null
                    Toast.makeText(context, "उत्पाद अपडेट हुआ!", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // Quick Restock Dialog
        restockingProduct?.let { prod ->
            RestockStockDialog(
                product = prod,
                onDismiss = { restockingProduct = null },
                onConfirm = { addedQty, reason ->
                    viewModel.restockProduct(prod.id, addedQty, reason)
                    restockingProduct = null
                    Toast.makeText(context, "+$addedQty ${prod.unit} स्टॉक में जोड़ा गया!", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // Delete Confirmation Dialog
        deletingProduct?.let { prod ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f))
                    .clickable { deletingProduct = null },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.88f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(SurfaceLevel2)
                        .border(1.dp, SurfaceOutline, RoundedCornerShape(20.dp))
                        .padding(20.dp)
                        .clickable(onClick = {}),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "उत्पाद हटाएं (Delete Item)?",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                    )
                    Text(
                        text = "क्या आप '${prod.name}' को कैटलॉग से हटाना चाहते हैं?",
                        style = MaterialTheme.typography.bodyMedium.copy(color = TextMuted)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { deletingProduct = null },
                            modifier = Modifier.weight(1f),
                            shape = CircleShape
                        ) {
                            Text("रद्द करें")
                        }
                        Button(
                            onClick = {
                                viewModel.deleteProduct(prod.id)
                                deletingProduct = null
                                Toast.makeText(context, "हटा दिया गया", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = DebitCoral, contentColor = Color.White)
                        ) {
                            Text("हटाएं (Delete)")
                        }
                    }
                }
            }
        }

        // Quick Adjust Threshold Dialog
        editingThresholdProduct?.let { prod ->
            QuickAdjustThresholdDialog(
                product = prod,
                onDismiss = { editingThresholdProduct = null },
                onSave = { newThreshold ->
                    viewModel.updateProductThreshold(prod.id, newThreshold)
                    editingThresholdProduct = null
                    Toast.makeText(context, "${prod.name} की अलर्ट सीमा ${newThreshold.toInt()} ${prod.unit} सेट की गई", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // Low-Stock Alert Settings & Test Dialog
        if (showAlertSettingsDialog) {
            LowStockAlertSettingsDialog(
                profile = profile,
                lowStockProducts = lowStockProducts,
                onDismiss = { showAlertSettingsDialog = false },
                onSave = { defaultThreshold, soundEnabled ->
                    viewModel.updateInventoryAlertSettings(defaultThreshold, soundEnabled)
                    showAlertSettingsDialog = false
                    Toast.makeText(context, "अलर्ट सेटिंग्स सहेजी गईं!", Toast.LENGTH_SHORT).show()
                },
                onTestSound = { name, qty, unit ->
                    viewModel.testLowStockAnnouncement(name, qty, unit)
                }
            )
        }

        // Recent Stock Movement Logs Dialog
        if (showStockHistoryDialog) {
            StockMovementsHistoryDialog(
                movements = recentMovements,
                onDismiss = { showStockHistoryDialog = false }
            )
        }
    }
}

@Composable
fun ProductCatalogItemCard(
    product: ProductEntity,
    onRestock: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onEditThreshold: () -> Unit = {}
) {
    val isOutOfStock = product.stockQuantity <= 0
    val isLowStock = product.stockQuantity <= product.lowStockThreshold

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(SurfaceLevel2)
            .border(
                1.dp,
                if (isLowStock) DebitCoral.copy(alpha = 0.5f) else SurfaceOutline,
                RoundedCornerShape(18.dp)
            )
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Top row: Product name, category & Price
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (product.localizedName.isNotBlank() && product.localizedName != product.name) {
                    Text(
                        text = product.localizedName,
                        style = MaterialTheme.typography.bodySmall.copy(color = TextMuted),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(SurfaceLevel3)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = product.category,
                            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 10.sp)
                        )
                    }

                    if (product.barcode.isNotBlank()) {
                        Text(
                            text = "|||| ${product.barcode}",
                            style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 10.sp)
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "₹ " + String.format(Locale.US, "%,.2f", product.price),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                )
                Text(
                    text = "प्रति ${product.unit}",
                    style = MaterialTheme.typography.labelSmall.copy(color = TextMuted)
                )
            }
        }

        // Middle row: Stock level badge and threshold info
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(SurfaceLevel1)
                .padding(horizontal = 10.dp, vertical = 8.dp),
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
                        .background(if (isLowStock) DebitCoral else CreditEmerald)
                )
                val stockText = if (isOutOfStock) {
                    "आउट ऑफ स्टॉक (0 ${product.unit})"
                } else if (isLowStock) {
                    "कम स्टॉक: केवल ${product.stockQuantity.toInt()} ${product.unit} शेष"
                } else {
                    "स्टॉक में उपलब्ध: ${product.stockQuantity.toInt()} ${product.unit}"
                }
                Text(
                    text = stockText,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isLowStock) DebitCoralLight else CreditEmeraldLight
                    )
                )
            }

            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(SurfaceLevel3)
                    .clickable(onClick = onEditThreshold)
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(Icons.Default.Tune, contentDescription = null, tint = TrustBlue, modifier = Modifier.size(12.dp))
                Text(
                    text = "अलर्ट सीमा: ${product.lowStockThreshold.toInt()} ${product.unit} ✏️",
                    style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontWeight = FontWeight.SemiBold)
                )
            }
        }

        // Action Buttons Row: Quick Restock, Edit, Delete
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onRestock,
                modifier = Modifier
                    .weight(1.3f)
                    .height(40.dp)
                    .testTag("restock_btn_${product.id}"),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = CreditEmerald,
                    contentColor = Color.Black
                )
            ) {
                Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("+ माल आया (Restock)", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
            }

            OutlinedButton(
                onClick = onEdit,
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .testTag("edit_product_${product.id}"),
                shape = CircleShape
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("बदलें (Edit)", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold))
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(SurfaceLevel3)
                    .testTag("delete_product_${product.id}")
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = DebitCoral, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun AddEditProductDialog(
    productToEdit: ProductEntity?,
    onDismiss: () -> Unit,
    onSave: (name: String, localizedName: String, category: String, price: Double, unit: String, stock: Double, threshold: Double, barcode: String) -> Unit
) {
    var name by remember { mutableStateOf(productToEdit?.name ?: "") }
    var localizedName by remember { mutableStateOf(productToEdit?.localizedName ?: "") }
    var category by remember { mutableStateOf(productToEdit?.category ?: "किराना (Grocery)") }
    var priceStr by remember { mutableStateOf(productToEdit?.price?.toString() ?: "") }
    var unit by remember { mutableStateOf(productToEdit?.unit ?: "packet") }
    var stockStr by remember { mutableStateOf(productToEdit?.stockQuantity?.toString() ?: "10") }
    var thresholdStr by remember { mutableStateOf(productToEdit?.lowStockThreshold?.toString() ?: "5") }
    var barcode by remember { mutableStateOf(productToEdit?.barcode ?: "") }

    val categories = listOf("किराना (Grocery)", "तेल व घी (Oils & Ghee)", "डेयरी (Dairy)", "मसाले (Spices)", "स्नैक्स (Snacks)", "साफ-सफाई (Cleaning)")
    val units = listOf("kg", "packet", "bottle", "bag", "box", "piece", "L")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.84f))
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (productToEdit == null) "+ नया उत्पाद जोड़ें (Add Product)" else "उत्पाद विवरण बदलें (Edit Product)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("उत्पाद का नाम (Product Name in English/Hindi)") },
                modifier = Modifier.fillMaxWidth().testTag("product_name_input"),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = localizedName,
                onValueChange = { localizedName = it },
                label = { Text("स्थानीय नाम (Localized Regional/Hindi Name)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            // Category Chips
            Text("श्रेणी (Category):", style = MaterialTheme.typography.labelSmall.copy(color = TextMuted))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(categories, key = { it }) { cat ->
                    val sel = category == cat
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (sel) TrustBlue else SurfaceLevel3)
                            .clickable { category = cat }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(cat, style = MaterialTheme.typography.labelSmall.copy(color = if (sel) Color.White else TextPrimary, fontSize = 11.sp))
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = priceStr,
                    onValueChange = { priceStr = it },
                    label = { Text("कीमत (Price ₹)") },
                    modifier = Modifier.weight(1f).testTag("product_price_input"),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                OutlinedTextField(
                    value = unit,
                    onValueChange = { unit = it },
                    label = { Text("इकाई (Unit)") },
                    modifier = Modifier.weight(0.8f),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = stockStr,
                    onValueChange = { stockStr = it },
                    label = { Text("आरंभिक स्टॉक (Stock Qty)") },
                    modifier = Modifier.weight(1f).testTag("product_stock_input"),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                OutlinedTextField(
                    value = thresholdStr,
                    onValueChange = { thresholdStr = it },
                    label = { Text("अलर्ट सीमा (Min Threshold)") },
                    modifier = Modifier.weight(1f).testTag("product_threshold_input"),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

            Button(
                onClick = {
                    val p = priceStr.toDoubleOrNull() ?: 0.0
                    val s = stockStr.toDoubleOrNull() ?: 0.0
                    val t = thresholdStr.toDoubleOrNull() ?: 5.0
                    if (name.isNotBlank() && p > 0) {
                        onSave(name, localizedName.ifBlank { name }, category, p, unit, s, t, barcode)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("save_product_btn"),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = TrustBlue, contentColor = Color.White)
            ) {
                Text("कैटलॉग में सुरक्षित करें (Save to Catalog)", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun RestockStockDialog(
    product: ProductEntity,
    onDismiss: () -> Unit,
    onConfirm: (addedQty: Double, reason: String) -> Unit
) {
    var addedQtyStr by remember { mutableStateOf("10") }
    var reason by remember { mutableStateOf("सप्लायर माल आया (Restock Arrival)") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .clip(RoundedCornerShape(20.dp))
                .background(SurfaceLevel2)
                .border(1.dp, SurfaceOutline, RoundedCornerShape(20.dp))
                .clickable(onClick = {})
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "+ माल आया / स्टॉक जोड़ें (Restock)",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
            )

            Text(
                text = "उत्पाद: ${product.name}\nवर्तमान स्टॉक: ${product.stockQuantity.toInt()} ${product.unit}",
                style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(5.0, 10.0, 20.0, 50.0).forEach { preset ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(CircleShape)
                            .background(SurfaceLevel3)
                            .clickable { addedQtyStr = preset.toInt().toString() }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("+${preset.toInt()}", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }

            OutlinedTextField(
                value = addedQtyStr,
                onValueChange = { addedQtyStr = it },
                label = { Text("नई प्राप्त मात्रा (${product.unit})") },
                modifier = Modifier.fillMaxWidth().testTag("restock_qty_input"),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Button(
                onClick = {
                    val qty = addedQtyStr.toDoubleOrNull() ?: 0.0
                    if (qty > 0) {
                        onConfirm(qty, reason)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("confirm_restock_btn"),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = CreditEmerald, contentColor = Color.Black)
            ) {
                Text("स्टॉक अपडेट करें (Confirm Stock-In)", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun QuickAdjustThresholdDialog(
    product: ProductEntity,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit
) {
    var thresholdText by remember { mutableStateOf(product.lowStockThreshold.toInt().toString()) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .clip(RoundedCornerShape(22.dp))
                .background(SurfaceLevel2)
                .border(1.dp, SurfaceOutline, RoundedCornerShape(22.dp))
                .clickable(onClick = {})
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "चेतावनी सीमा बदलें",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                    )
                    Text(
                        text = "${product.name} (${product.unit})",
                        style = MaterialTheme.typography.labelSmall.copy(color = TrustBlue)
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                }
            }

            Text(
                text = "जब स्टॉक इस संख्या से कम होगा, तो ऐप और साउंडबॉक्स कम स्टॉक की चेतावनी देंगे।",
                style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
            )

            // Preset Chips
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(2.0, 5.0, 10.0, 15.0, 20.0).forEach { preset ->
                    val isSel = thresholdText == preset.toInt().toString()
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(CircleShape)
                            .background(if (isSel) TrustBlue else SurfaceLevel3)
                            .clickable { thresholdText = preset.toInt().toString() }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${preset.toInt()}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isSel) Color.White else TextPrimary
                            )
                        )
                    }
                }
            }

            OutlinedTextField(
                value = thresholdText,
                onValueChange = { thresholdText = it },
                label = { Text("कस्टम सीमा संख्या (${product.unit})") },
                modifier = Modifier.fillMaxWidth().testTag("custom_threshold_input"),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )

            Button(
                onClick = {
                    val t = thresholdText.toDoubleOrNull() ?: 5.0
                    onSave(t)
                },
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("save_threshold_btn"),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = TrustBlue, contentColor = Color.White)
            ) {
                Text("अलर्ट सीमा सहेजें", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun LowStockAlertSettingsDialog(
    profile: com.example.data.model.MerchantProfile,
    lowStockProducts: List<ProductEntity>,
    onDismiss: () -> Unit,
    onSave: (defaultThreshold: Double, soundEnabled: Boolean) -> Unit,
    onTestSound: (name: String, qty: Double, unit: String) -> Unit
) {
    var defaultThresholdText by remember { mutableStateOf(profile.defaultLowStockThreshold.toInt().toString()) }
    var soundEnabled by remember { mutableStateOf(profile.lowStockAlertSoundEnabled) }

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier.size(36.dp).clip(CircleShape).background(DebitCoral.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = DebitCoral, modifier = Modifier.size(20.dp))
                    }
                    Column {
                        Text(
                            text = "कम स्टॉक चेतावनी सेटिंग्स",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                        )
                        Text(
                            text = "Soundbox & In-App Alerts",
                            style = MaterialTheme.typography.labelSmall.copy(color = TextMuted)
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                }
            }

            // Soundbox Voice Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(SurfaceLevel3)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "साउंडबॉक्स वॉइस अलर्ट",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                    )
                    Text(
                        text = "बिक्री के बाद स्टॉक सीमा से कम होने पर आवाज़ में चेतावनी",
                        style = MaterialTheme.typography.labelSmall.copy(color = TextMuted)
                    )
                }

                Switch(
                    checked = soundEnabled,
                    onCheckedChange = { soundEnabled = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = CreditEmerald,
                        uncheckedTrackColor = SurfaceOutline
                    )
                )
            }

            // Default Threshold Setting
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "नए उत्पादों हेतु डिफ़ॉल्ट सीमा (Default Threshold):",
                    style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontWeight = FontWeight.Bold)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(3.0, 5.0, 10.0, 15.0).forEach { preset ->
                        val isSel = defaultThresholdText == preset.toInt().toString()
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(CircleShape)
                                .background(if (isSel) TrustBlue else SurfaceLevel3)
                                .clickable { defaultThresholdText = preset.toInt().toString() }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${preset.toInt()}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) Color.White else TextPrimary
                                )
                            )
                        }
                    }
                }
            }

            // Test Voice Alert Button
            OutlinedButton(
                onClick = {
                    val sampleName = lowStockProducts.firstOrNull()?.name ?: "आशीर्वाद आटा"
                    val sampleUnit = lowStockProducts.firstOrNull()?.unit ?: "kg"
                    onTestSound(sampleName, 2.0, sampleUnit)
                },
                modifier = Modifier.fillMaxWidth().height(44.dp),
                shape = CircleShape
            ) {
                Icon(Icons.Default.VolumeUp, contentDescription = null, tint = TrustBlue, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("🔊 टेस्ट चेतावनी सुनें (Audition Voice Alert)", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
            }

            Button(
                onClick = {
                    val defT = defaultThresholdText.toDoubleOrNull() ?: 5.0
                    onSave(defT, soundEnabled)
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = TrustBlue, contentColor = Color.White)
            ) {
                Text("सेटिंग्स सहेजें", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun StockMovementsHistoryDialog(
    movements: List<StockMovementEntity>,
    onDismiss: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }

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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier.size(36.dp).clip(CircleShape).background(TrustBlue.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.History, contentDescription = null, tint = TrustBlue, modifier = Modifier.size(20.dp))
                    }
                    Column {
                        Text(
                            text = "स्टॉक आवाजाही इतिहास (Movement Log)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                        )
                        Text(
                            text = "POS, खाता बिक्री व आवक की प्रविष्टियां",
                            style = MaterialTheme.typography.labelSmall.copy(color = TextMuted)
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                }
            }

            if (movements.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
                    Text("कोई हालिया स्टॉक आवाजाही नहीं मिली।", style = MaterialTheme.typography.bodySmall.copy(color = TextMuted))
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().height(320.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(movements, key = { it.id }) { mov ->
                        val isPositive = mov.quantityDelta > 0
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(SurfaceLevel3)
                                .border(1.dp, SurfaceOutline, RoundedCornerShape(12.dp))
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(if (isPositive) CreditEmerald.copy(alpha = 0.2f) else DebitCoral.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isPositive) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                        contentDescription = null,
                                        tint = if (isPositive) CreditEmeraldLight else DebitCoralLight,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                Column {
                                    Text(
                                        text = mov.productName,
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                                    )
                                    Text(
                                        text = mov.reason + " • " + dateFormat.format(java.util.Date(mov.timestamp)),
                                        style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 10.sp)
                                    )
                                }
                            }

                            Text(
                                text = (if (isPositive) "+" else "") + "${mov.quantityDelta.toInt()}",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isPositive) CreditEmeraldLight else DebitCoralLight
                                )
                            )
                        }
                    }
                }
            }

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(46.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceLevel3, contentColor = TextPrimary)
            ) {
                Text("बंद करें (Close)")
            }
        }
    }
}
