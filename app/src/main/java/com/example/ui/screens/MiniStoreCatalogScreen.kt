package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Discount
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.CustomerEntity
import com.example.data.local.ProductEntity
import com.example.data.model.MerchantProfile
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
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiniStoreCatalogScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val merchantProfile by viewModel.merchantProfile.collectAsState()
    val allProducts by viewModel.inventoryRepository.allProducts.collectAsState(initial = emptyList())
    val allCustomers: List<CustomerEntity> by viewModel.khataRepository.allCustomers.collectAsState(initial = emptyList())

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("WhatsApp Store Catalog", "Loyalty Winback (30+ Days)")

    // Catalog state
    val selectedProductIds = remember { mutableStateListOf<Long>() }
    var promoHeadline by remember { mutableStateOf("⚡ Specialkirana Offer: Free Home Delivery on orders above ₹300!") }
    var promoCouponCode by remember { mutableStateOf("VYAPAR5") }

    // Winback state
    var winbackDiscountCode by remember { mutableStateOf("WELCOME10") }
    var winbackDiscountPercent by remember { mutableStateOf("10%") }

    val thirtyDaysMillis = 30L * 24 * 60 * 60 * 1000
    val now = System.currentTimeMillis()
    val inactiveCustomers = remember(allCustomers) {
        allCustomers.filter { customer ->
            !customer.isSupplier && (now - customer.lastTransactionTimestamp) >= thirtyDaysMillis
        }.sortedByDescending { now - it.lastTransactionTimestamp }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("WhatsApp Mini-Store & Winback", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            text = if (selectedTabIndex == 0) "Generate digital menu for WhatsApp" else "${inactiveCustomers.size} customers inactive for 30+ days",
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("btn_back_ministore")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (index == 0) Icons.Default.Storefront else Icons.Default.VolunteerActivism,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (selectedTabIndex == index) TrustBlue else TextMuted
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = title,
                                    fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    )
                }
            }

            if (selectedTabIndex == 0) {
                // TAB 1: WhatsApp Catalog Generator
                CatalogGeneratorTab(
                    context = context,
                    merchant = merchantProfile,
                    products = allProducts,
                    selectedIds = selectedProductIds,
                    promoHeadline = promoHeadline,
                    onPromoHeadlineChange = { promoHeadline = it },
                    couponCode = promoCouponCode,
                    onCouponCodeChange = { promoCouponCode = it },
                    onCopyText = { text ->
                        clipboardManager.setText(AnnotatedString(text))
                        Toast.makeText(context, "Catalog copied to clipboard!", Toast.LENGTH_SHORT).show()
                    },
                    onShareWhatsApp = { text ->
                        shareTextViaWhatsApp(context, text, null)
                    }
                )
            } else {
                // TAB 2: Automated Customer Loyalty Winback
                LoyaltyWinbackTab(
                    context = context,
                    merchant = merchantProfile,
                    inactiveCustomers = inactiveCustomers,
                    discountCode = winbackDiscountCode,
                    onDiscountCodeChange = { winbackDiscountCode = it },
                    discountPercent = winbackDiscountPercent,
                    onDiscountPercentChange = { winbackDiscountPercent = it },
                    onSendWinback = { customer, message ->
                        shareTextViaWhatsApp(context, message, customer.phone)
                    }
                )
            }
        }
    }
}

@Composable
fun CatalogGeneratorTab(
    context: Context,
    merchant: MerchantProfile,
    products: List<ProductEntity>,
    selectedIds: MutableList<Long>,
    promoHeadline: String,
    onPromoHeadlineChange: (String) -> Unit,
    couponCode: String,
    onCouponCodeChange: (String) -> Unit,
    onCopyText: (String) -> Unit,
    onShareWhatsApp: (String) -> Unit
) {
    val catalogText = remember(selectedIds.size, promoHeadline, couponCode, merchant) {
        buildCatalogMessage(merchant, products.filter { selectedIds.contains(it.id) }, promoHeadline, couponCode)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceLevel1),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Campaign, contentDescription = null, tint = TrustBlue)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("WhatsApp Catalog Banner & Offer", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = promoHeadline,
                        onValueChange = onPromoHeadlineChange,
                        label = { Text("Store Headline / Promotion") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = couponCode,
                        onValueChange = onCouponCodeChange,
                        label = { Text("Discount Coupon Code") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Select Products (${selectedIds.size}/${products.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Row {
                    FilterChip(
                        selected = selectedIds.size == products.size && products.isNotEmpty(),
                        onClick = {
                            if (selectedIds.size == products.size) {
                                selectedIds.clear()
                            } else {
                                selectedIds.clear()
                                selectedIds.addAll(products.map { it.id })
                            }
                        },
                        label = { Text(if (selectedIds.size == products.size) "Deselect All" else "Select All") }
                    )
                }
            }
        }

        // Product Selection List
        if (products.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceLevel2),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("No inventory items found. Add products in Stock tab.", color = TextMuted)
                    }
                }
            }
        } else {
            items(products) { prod ->
                val isSelected = selectedIds.contains(prod.id)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (isSelected) selectedIds.remove(prod.id) else selectedIds.add(prod.id)
                        },
                    colors = CardDefaults.cardColors(containerColor = if (isSelected) SurfaceLevel1 else SurfaceLevel2),
                    border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, TrustBlue) else null,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = if (isSelected) TrustBlue else TextMuted
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(prod.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text("${prod.category} • ${prod.unit}", fontSize = 12.sp, color = TextMuted)
                            }
                        }
                        Text(
                            text = "₹${String.format(Locale.US, "%.1f", prod.price)}",
                            fontWeight = FontWeight.Bold,
                            color = CreditMint,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }

        // Live Preview & Sharing
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceLevel1),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = CreditMint)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("WhatsApp Message Preview", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = catalogText,
                            color = Color(0xFFE2E8F0),
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 16.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onCopyText(catalogText) },
                            modifier = Modifier.weight(1f).testTag("btn_copy_catalog")
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy Menu")
                        }
                        Button(
                            onClick = { onShareWhatsApp(catalogText) },
                            modifier = Modifier.weight(1f).testTag("btn_share_catalog_wa"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share WhatsApp", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LoyaltyWinbackTab(
    context: Context,
    merchant: MerchantProfile,
    inactiveCustomers: List<CustomerEntity>,
    discountCode: String,
    onDiscountCodeChange: (String) -> Unit,
    discountPercent: String,
    onDiscountPercentChange: (String) -> Unit,
    onSendWinback: (CustomerEntity, String) -> Unit
) {
    val now = System.currentTimeMillis()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceLevel1),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.VolunteerActivism, contentDescription = null, tint = DebitCoral)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("30+ Days Inactive Customer Winback", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Bring back lost customers with an exclusive, 1-tap personalized WhatsApp discount offer.",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = discountCode,
                            onValueChange = onDiscountCodeChange,
                            label = { Text("Coupon Code") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = discountPercent,
                            onValueChange = onDiscountPercentChange,
                            label = { Text("Discount Off") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = "Inactive Customers (${inactiveCustomers.size})",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }

        if (inactiveCustomers.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceLevel1),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = CreditMint, modifier = Modifier.size(42.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Great news!", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(
                            "All registered customers have visited within the last 30 days.",
                            color = TextMuted,
                            fontSize = 13.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(inactiveCustomers) { customer ->
                val daysInactive = TimeUnit.MILLISECONDS.toDays(now - customer.lastTransactionTimestamp)
                val winbackMessage = buildWinbackMessage(merchant, customer, daysInactive, discountCode, discountPercent)

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
                                Text(customer.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text(customer.phone.ifBlank { "No phone registered" }, fontSize = 12.sp, color = TextMuted)
                            }
                            Surface(
                                color = DebitCoral.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.HourglassEmpty, contentDescription = null, tint = DebitCoral, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("$daysInactive days inactive", color = DebitCoral, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        if (customer.lastItemDescription.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Last bought: ${customer.lastItemDescription}",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = { onSendWinback(customer, winbackMessage) },
                            modifier = Modifier.fillMaxWidth().testTag("btn_send_winback_${customer.id}"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("1-Tap WhatsApp Winback Offer", color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

private fun buildCatalogMessage(
    merchant: MerchantProfile,
    products: List<ProductEntity>,
    headline: String,
    couponCode: String
): String {
    val shopName = merchant.shopName.ifBlank { "VyaparSetu Kirana Store" }
    val address = merchant.address.ifBlank { "Main Bazaar" }
    val phone = merchant.phone.ifBlank { "9876543210" }

    val sb = StringBuilder()
    sb.append("🛒 *${shopName}* - Digital Store Menu\n")
    sb.append("📍 $address | 📞 $phone\n\n")

    if (headline.isNotBlank()) {
        sb.append("$headline\n\n")
    }

    sb.append("🌟 *Store Items & Prices:*\n")
    if (products.isEmpty()) {
        sb.append("▫️ All daily groceries, pulses, spices, oil & dairy available!\n")
    } else {
        for (p in products) {
            sb.append("▫️ ${p.name} (${p.unit}) - *₹${String.format(Locale.US, "%.1f", p.price)}*\n")
        }
    }

    if (couponCode.isNotBlank()) {
        sb.append("\n🎁 Use Coupon Code: *${couponCode}* for instant savings!\n")
    }

    if (merchant.upiId.isNotBlank()) {
        sb.append("💳 Pay via UPI: ${merchant.upiId}\n")
    }

    sb.append("⚡ Reply here to place your order for fast delivery or shop pickup!")
    return sb.toString()
}

private fun buildWinbackMessage(
    merchant: MerchantProfile,
    customer: CustomerEntity,
    daysInactive: Long,
    couponCode: String,
    discountPercent: String
): String {
    val shopName = merchant.shopName.ifBlank { "VyaparSetu Store" }
    val contact = merchant.phone.ifBlank { "" }

    return """
        🙏 *Namaste ${customer.name} ji!*
        
        We miss seeing you at *${shopName}*! It has been over ${daysInactive} days since your last visit.
        
        🎁 As our privileged customer, we are offering you an exclusive *${discountPercent} DISCOUNT* on your next shopping visit!
        
        🏷️ Simply use coupon code: *${couponCode}* at checkout.
        
        📍 Visit us today or send your grocery list on this chat for fast home delivery.
        ${if (contact.isNotBlank()) "📞 Contact: $contact" else ""}
        
        Have a wonderful day! 😊
    """.trimIndent()
}

private fun shareTextViaWhatsApp(context: Context, text: String, phone: String?) {
    val cleanPhone = phone?.replace(Regex("[^0-9]"), "") ?: ""
    val fullPhone = if (cleanPhone.length == 10) "91$cleanPhone" else cleanPhone

    try {
        if (fullPhone.isNotBlank()) {
            val url = "https://api.whatsapp.com/send?phone=$fullPhone&text=${URLEncoder.encode(text, "UTF-8")}"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } else {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
                `package` = "com.whatsapp"
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(sendIntent)
        }
    } catch (e: Exception) {
        // Fallback to Android ShareSheet
        val chooserIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(Intent.createChooser(chooserIntent, "Share via WhatsApp or any App"))
    }
}
