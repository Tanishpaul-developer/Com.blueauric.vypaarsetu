package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import com.example.data.local.CustomerEntity
import com.example.data.local.ProductEntity
import com.example.ui.viewmodel.PosCartItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SplitPart
import com.example.data.model.SplitStatus
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
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun PosQrScreen(
    viewModel: MainViewModel,
    onLockPos: () -> Unit = {}
) {
    val context = LocalContext.current
    val posAmount by viewModel.posAmount.collectAsStateWithLifecycle()
    val autoSplitEnabled by viewModel.autoSplitEnabled.collectAsStateWithLifecycle()
    val splitBillResult by viewModel.splitBillResult.collectAsStateWithLifecycle()
    val allProducts by viewModel.allProducts.collectAsStateWithLifecycle()
    val posCart by viewModel.posCart.collectAsStateWithLifecycle()
    val customers by viewModel.allCustomers.collectAsStateWithLifecycle()

    var showKhataCustomerPicker by remember { mutableStateOf(false) }
    var selectedPosMode by remember { mutableStateOf("CATALOG") } // "CATALOG" or "KEYPAD"
    var catalogSearchQuery by remember { mutableStateOf("") }
    var selectedCatalogCategory by remember { mutableStateOf("सभी (All)") }

    var secondsRemaining by remember { mutableIntStateOf(165) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            if (secondsRemaining > 0) secondsRemaining--
        }
    }

    val formattedTimer = remember(secondsRemaining) {
        val mins = String.format(Locale.US, "%02d", secondsRemaining / 60)
        val secs = String.format(Locale.US, "%02d", secondsRemaining % 60)
        "$mins:$secs"
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            // Direct Checkout Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PointOfSale,
                            contentDescription = null,
                            tint = TrustBlue,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "DIRECT CHECKOUT",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TrustBlue,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        )
                    }

                    Text(
                        text = "Quick POS & Dynamic QR",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary
                        )
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Quick Lock POS Button to re-secure the terminal
                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(DebitCoralDark.copy(alpha = 0.5f))
                            .border(1.dp, DebitCoral.copy(alpha = 0.4f), CircleShape)
                            .clickable { onLockPos() }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                            .testTag("pos_quick_lock_btn"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Lock POS",
                            tint = DebitCoralLight,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "Lock",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = DebitCoralLight,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(CreditEmeraldDark.copy(alpha = 0.6f))
                            .border(1.dp, CreditEmerald.copy(alpha = 0.4f), CircleShape)
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(modifier = Modifier.size(6.dp).background(CreditEmerald, CircleShape))
                        Text(
                            text = "UPI 2.0 Ready",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = CreditEmeraldLight,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }

        item {
            // Active Amount Display Card in Elevated Dark Slate
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(SurfaceLevel2)
                    .border(1.dp, SurfaceOutline, RoundedCornerShape(20.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TOTAL BILL AMOUNT",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TextMuted,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    )

                    Row(
                        modifier = Modifier
                            .clickable { viewModel.resetKeypad() }
                            .testTag("pos_reset_btn"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = "Reset",
                            tint = DebitCoralLight,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Reset",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = DebitCoralLight,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "₹",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextMuted
                            )
                        )
                        Text(
                            text = String.format(Locale.US, "%,.0f", posAmount),
                            style = MaterialTheme.typography.displayMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            ),
                            modifier = Modifier.testTag("pos_amount_display")
                        )
                    }

                    IconButton(
                        onClick = { viewModel.backspaceKeypad() },
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(SurfaceLevel3)
                            .border(1.dp, SurfaceOutline, CircleShape)
                            .testTag("pos_backspace_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Backspace,
                            contentDescription = "Backspace",
                            tint = TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Quick Add Presets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(50.0 to "+ ₹50", 100.0 to "+ ₹100", 500.0 to "+ ₹500", 1000.0 to "+ ₹1,000").forEach { (valAmt, label) ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(CircleShape)
                                .background(SurfaceLevel3)
                                .border(1.dp, SurfaceOutline, CircleShape)
                                .clickable { viewModel.addPresetAmount(valAmt) }
                                .padding(vertical = 8.dp)
                                .testTag("preset_${valAmt.toInt()}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                        }
                    }
                }

                // AI Voice Billing Microphone Action Banner (Feature #1)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Brush.horizontalGradient(listOf(Color(0xFF581C87), Color(0xFF7E22CE))))
                        .clickable {
                            Toast.makeText(context, "बोलिए (Voice Billing): उदा. '2 Lux soap cash bill 80 rupees'", Toast.LENGTH_LONG).show()
                            viewModel.setPosAmount(80.0)
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .testTag("btn_pos_voice_billing"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = Color.White.copy(alpha = 0.2f),
                            shape = CircleShape,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Mic, contentDescription = "Voice Billing", tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                        Column {
                            Text("AI Voice-to-Billing (Gemini 2.5)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("बोलकर बिल बनाएं: '2 साबुन 80 रुपये'", color = Color.White.copy(alpha = 0.8f), fontSize = 10.sp)
                        }
                    }
                    Text("बोलें 🎙️", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        item {
            // Smart Auto-Split Banner with Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(CreditEmeraldDark.copy(alpha = 0.35f))
                    .border(1.dp, CreditEmerald.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(CreditEmerald),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CallSplit,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Smart Auto-Split < ₹2,000",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = CreditEmeraldLight
                                )
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(CreditEmerald)
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "99.8% SUCCESS",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color.Black,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 9.sp
                                    )
                                )
                            }
                        }

                        Text(
                            text = if (posAmount > 2000.0) {
                                val count = splitBillResult?.parts?.size ?: 2
                                "Avoid high-value bank surcharges and network drops by splitting ₹${String.format(Locale.US, "%,.0f", posAmount)} into $count safe QR tickets."
                            } else {
                                "Single bill strictly under ₹2,000 limit. Zero MDR charge guaranteed."
                            },
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = CreditEmeraldLight.copy(alpha = 0.85f),
                                lineHeight = 16.sp
                            )
                        )
                    }
                }

                Switch(
                    checked = autoSplitEnabled,
                    onCheckedChange = { viewModel.toggleAutoSplit(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = CreditEmerald,
                        uncheckedTrackColor = SurfaceLevel3
                    ),
                    modifier = Modifier.testTag("auto_split_switch")
                )
            }
        }

        // Sequential QR Code Cards
        splitBillResult?.let { bill ->
            items(bill.parts, key = { it.index }) { part ->
                SplitQrCard(
                    part = part,
                    timerText = formattedTimer,
                    onVoicePrompt = {
                        viewModel.triggerSoundboxAnnouncement("UPI", part.amount, "Customer")
                        Toast.makeText(context, "Announcing ₹${part.amount.toInt()} on Soundbox", Toast.LENGTH_SHORT).show()
                    },
                    onSimulatePaid = {
                        viewModel.markSplitAsPaid(part.index, "Simulated Customer")
                        viewModel.confirmPosSale("UPI QR")
                    }
                )
            }
        }

        // Mode Switcher: Item Catalog vs Tactile Keypad
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(SurfaceLevel2)
                    .border(1.dp, SurfaceOutline, RoundedCornerShape(14.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Catalog Tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selectedPosMode == "CATALOG") TrustBlue else Color.Transparent)
                        .clickable { selectedPosMode = "CATALOG" }
                        .padding(vertical = 10.dp)
                        .testTag("pos_tab_catalog"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Inventory2,
                            contentDescription = null,
                            tint = if (selectedPosMode == "CATALOG") Color.White else TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "सामग्री कैटलॉग (Catalog)",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (selectedPosMode == "CATALOG") Color.White else TextMuted
                            )
                        )
                    }
                }

                // Keypad Tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selectedPosMode == "KEYPAD") TrustBlue else Color.Transparent)
                        .clickable { selectedPosMode = "KEYPAD" }
                        .padding(vertical = 10.dp)
                        .testTag("pos_tab_keypad"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PointOfSale,
                            contentDescription = null,
                            tint = if (selectedPosMode == "KEYPAD") Color.White else TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "कीपैड (Keypad)",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (selectedPosMode == "KEYPAD") Color.White else TextMuted
                            )
                        )
                    }
                }
            }
        }

        // Active Cart Breakdown (Visible whenever items are in cart)
        if (posCart.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(SurfaceLevel2)
                        .border(1.dp, TrustBlue.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
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
                            Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = TrustBlue, modifier = Modifier.size(20.dp))
                            Text(
                                text = "बिल सामग्री (${posCart.sumOf { it.quantity.toInt() }} वस्तुएं)",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                        }

                        Text(
                            text = "खाली करें (Clear)",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = DebitCoralLight,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier
                                .clickable { viewModel.clearPosCart() }
                                .padding(4.dp)
                                .testTag("clear_pos_cart")
                        )
                    }

                    // Cart Items List
                    posCart.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(SurfaceLevel3)
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.product.name,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    ),
                                    maxLines = 1
                                )
                                Text(
                                    text = "₹${item.product.price.toInt()} x ${item.quantity.toInt()} ${item.product.unit} = ₹${(item.product.price * item.quantity).toInt()}",
                                    style = MaterialTheme.typography.labelSmall.copy(color = TextMuted)
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        viewModel.updatePosCartItemQuantity(item.product.id, item.quantity - 1.0)
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = DebitCoralLight, modifier = Modifier.size(16.dp))
                                }

                                Text(
                                    text = "${item.quantity.toInt()}",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                                )

                                IconButton(
                                    onClick = {
                                        viewModel.updatePosCartItemQuantity(item.product.id, item.quantity + 1.0)
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Increase", tint = CreditEmerald, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    // Summary Total row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "स्वतः परिकलित कुल (Auto Total):",
                            style = MaterialTheme.typography.labelMedium.copy(color = TextMuted)
                        )
                        Text(
                            text = "₹ ${String.format(Locale.US, "%,.2f", posAmount)}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = CreditEmerald
                            )
                        )
                    }
                }
            }
        }

        if (selectedPosMode == "CATALOG") {
            // Category Filter Chips & Search
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Search Bar
                    OutlinedTextField(
                        value = catalogSearchQuery,
                        onValueChange = { catalogSearchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("pos_catalog_search"),
                        placeholder = { Text("कैटलॉग में खोजें (Search products...)", style = MaterialTheme.typography.bodySmall) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp)) },
                        singleLine = true,
                        shape = CircleShape
                    )

                    // Category Chips
                    val categories = listOf("सभी (All)", "किराना", "अनाज व दालें", "मसाले", "डेयरी", "पेय व स्नैक्स")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(categories, key = { it }) { cat ->
                            val isSelected = selectedCatalogCategory == cat
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(if (isSelected) TrustBlue else SurfaceLevel2)
                                    .border(1.dp, if (isSelected) TrustBlue else SurfaceOutline, CircleShape)
                                    .clickable { selectedCatalogCategory = cat }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = cat,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color.White else TextSecondary
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Products Feed
            val filteredProducts = allProducts.filter { prod ->
                val matchesCat = selectedCatalogCategory == "सभी (All)" || prod.category.contains(selectedCatalogCategory) || selectedCatalogCategory.contains(prod.category)
                val matchesSearch = catalogSearchQuery.isBlank() || prod.name.contains(catalogSearchQuery, ignoreCase = true) || prod.localizedName.contains(catalogSearchQuery, ignoreCase = true)
                matchesCat && matchesSearch
            }

            if (filteredProducts.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "कोई उत्पाद नहीं मिला। (No products found)",
                            style = MaterialTheme.typography.bodyMedium.copy(color = TextMuted)
                        )
                    }
                }
            } else {
                items(filteredProducts, key = { it.id }) { product ->
                    val cartItem = posCart.find { it.product.id == product.id }
                    val isLowStock = product.stockQuantity <= product.lowStockThreshold

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(SurfaceLevel2)
                            .border(1.dp, SurfaceOutline, RoundedCornerShape(16.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = product.name,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                            if (product.localizedName.isNotBlank() && product.localizedName != product.name) {
                                Text(
                                    text = product.localizedName,
                                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                                )
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "₹${product.price.toInt()} / ${product.unit}",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TrustBlue
                                    )
                                )

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (isLowStock) DebitCoralDark.copy(alpha = 0.5f) else CreditEmeraldDark.copy(alpha = 0.5f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (isLowStock) "⚠️ कम स्टॉक: ${product.stockQuantity.toInt()}" else "स्टॉक: ${product.stockQuantity.toInt()}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = if (isLowStock) DebitCoralLight else CreditEmeraldLight,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        )
                                    )
                                }
                            }
                        }

                        // Add / Quantity Controller
                        if (cartItem != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(SurfaceLevel3)
                                        .clickable { viewModel.updatePosCartItemQuantity(product.id, cartItem.quantity - 1.0) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = DebitCoralLight, modifier = Modifier.size(16.dp))
                                }

                                Text(
                                    text = "${cartItem.quantity.toInt()}",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                                )

                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(TrustBlue)
                                        .clickable { viewModel.updatePosCartItemQuantity(product.id, cartItem.quantity + 1.0) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Increase", tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }
                        } else {
                            Button(
                                onClick = { viewModel.addProductToPosCart(product) },
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(containerColor = TrustBlue, contentColor = Color.White),
                                modifier = Modifier
                                    .height(36.dp)
                                    .testTag("add_item_${product.id}")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("+ जोड़ें", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                            }
                        }
                    }
                }
            }
        } else {
            // Tactile Billing Keypad
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(SurfaceLevel2)
                        .border(1.dp, SurfaceOutline, RoundedCornerShape(20.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TACTILE BILLING KEYPAD",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextMuted,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        )
                        Text(
                            text = "One-Tap Entry",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TrustBlue,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    // 3x4 Keypad Grid
                    val keys = listOf(
                        listOf("1", "2", "3"),
                        listOf("4", "5", "6"),
                        listOf("7", "8", "9"),
                        listOf("00", "0", "GENERATE")
                    )

                    keys.forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            row.forEach { k ->
                                if (k == "GENERATE") {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(54.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(TrustBlue)
                                            .clickable {
                                                viewModel.generateSplitsForAmount(posAmount, autoSplitEnabled)
                                                Toast.makeText(context, "Dynamic QR Generated for ₹${posAmount.toInt()}", Toast.LENGTH_SHORT).show()
                                            }
                                            .testTag("keypad_generate_qr"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.QrCode2,
                                            contentDescription = "Generate QR",
                                            tint = Color.White,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(54.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(SurfaceLevel3)
                                            .border(1.dp, SurfaceOutline, RoundedCornerShape(14.dp))
                                            .clickable { viewModel.appendKeypad(k) }
                                            .testTag("keypad_$k"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = k,
                                            style = MaterialTheme.typography.titleLarge.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
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

        item {
            // Action Buttons (Thermal Slip, Record Cash Sale, Speak Dues, Add Items)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(bottom = 24.dp)) {
                if (posAmount > 0) {
                    Button(
                        onClick = {
                            viewModel.confirmPosSale("Cash")
                            Toast.makeText(context, "₹${posAmount.toInt()} नकद बिक्री दर्ज की गई और स्टॉक अपडेट हुआ", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("pos_record_cash_sale_btn"),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = CreditEmerald, contentColor = Color.Black)
                    ) {
                        Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("💵 नकद बिक्री दर्ज करें (Record Cash Sale & Deduct Stock)", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { showKhataCustomerPicker = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("pos_record_khata_sale_btn"),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = TrustBlue, contentColor = Color.White)
                    ) {
                        Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("📖 उधार / खाता पर बेचें (Sell on Khata & Deduct Stock)", fontWeight = FontWeight.Bold)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { Toast.makeText(context, "Printing Thermal Slip...", Toast.LENGTH_SHORT).show() },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                        border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.linearGradient(listOf(SurfaceOutline, SurfaceOutline)))
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Print, contentDescription = null, tint = TrustBlue, modifier = Modifier.size(18.dp))
                            Text("Thermal Slip", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp))
                        }
                    }

                    Button(
                        onClick = { viewModel.triggerSoundboxAnnouncement("POS", posAmount, "Customer") },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TrustBlue,
                            contentColor = Color.White
                        )
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("Speak Dues", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp))
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            selectedPosMode = if (selectedPosMode == "CATALOG") "KEYPAD" else "CATALOG"
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                        border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.linearGradient(listOf(SurfaceOutline, SurfaceOutline)))
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(if (selectedPosMode == "CATALOG") Icons.Default.PointOfSale else Icons.Default.Inventory2, contentDescription = null, tint = TrustBlue, modifier = Modifier.size(18.dp))
                            Text(if (selectedPosMode == "CATALOG") "Keypad" else "Catalog", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp))
                        }
                    }
                }
            }
        }
    }

    if (showKhataCustomerPicker) {
        PosKhataCustomerPickerDialog(
            customers = customers,
            totalAmount = posAmount,
            cartItemCount = posCart.sumOf { it.quantity }.toInt(),
            onDismiss = { showKhataCustomerPicker = false },
            onSelectCustomer = { customer ->
                viewModel.confirmPosSaleToKhata(customer.id, customer.name)
                showKhataCustomerPicker = false
                Toast.makeText(context, "✓ ₹${posAmount.toInt()} ${customer.name} के खाते में दर्ज, स्टॉक अपडेट हुआ!", Toast.LENGTH_SHORT).show()
            },
            onAddNewCustomer = { name, phone ->
                viewModel.addNewCustomer(name, phone, initialUdhar = 0.0, itemNote = "POS Customer", isSupplier = false)
            }
        )
    }
}

@Composable
fun SplitQrCard(
    part: SplitPart,
    timerText: String,
    onVoicePrompt: () -> Unit,
    onSimulatePaid: () -> Unit
) {
    val isPaid = part.status == SplitStatus.PAID

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceLevel2)
            .border(
                1.dp,
                if (isPaid) CreditEmerald.copy(alpha = 0.5f) else TrustBlue.copy(alpha = 0.4f),
                RoundedCornerShape(20.dp)
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Card Header
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
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(if (isPaid) CreditEmerald else TrustBlue),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = part.index.toString(),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isPaid) Color.Black else Color.White
                        )
                    )
                }

                Text(
                    text = "QR Code #${part.index}: ₹ ${String.format(Locale.US, "%,.0f", part.amount)}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
            }

            if (isPaid) {
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(CreditEmeraldDark.copy(alpha = 0.6f))
                        .border(1.dp, CreditEmerald.copy(alpha = 0.4f), CircleShape)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.Verified, contentDescription = null, tint = CreditEmeraldLight, modifier = Modifier.size(14.dp))
                    Text(
                        text = "Paid • 12s ago",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = CreditEmeraldLight,
                            fontWeight = FontWeight.ExtraBold
                        )
                    )
                }
            } else {
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(TrustBlueDark.copy(alpha = 0.5f))
                        .border(1.dp, TrustBlue.copy(alpha = 0.4f), CircleShape)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.HourglassTop, contentDescription = null, tint = TrustBlueLight, modifier = Modifier.size(14.dp))
                    Text(
                        text = "Awaiting Scan",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TrustBlueLight,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }

        // Body: QR + Info
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // QR Code Graphic container
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                if (part.qrBitmap != null) {
                    Image(
                        bitmap = part.qrBitmap.asImageBitmap(),
                        contentDescription = "Dynamic UPI QR Code for ₹${part.amount}",
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.QrCode2,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                if (isPaid) {
                    // Settled Watermark Scrim
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF0E1320).copy(alpha = 0.88f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(CreditEmerald),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(28.dp))
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "SETTLED",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = CreditEmeraldLight,
                                    letterSpacing = 1.sp
                                )
                            )
                        }
                    }
                } else {
                    // Scanning laser beam animation
                    val transition = rememberInfiniteTransition(label = "laser")
                    val offsetVal by transition.animateFloat(
                        initialValue = -50f,
                        targetValue = 50f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1800, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "laser_y"
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .offset(y = offsetVal.dp)
                            .background(
                                Brush.horizontalGradient(listOf(Color.Transparent, TrustBlue, Color.Transparent))
                            )
                    )
                }
            }

            // Info column
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isPaid) {
                    Text(
                        text = "Txn ID: ${part.txnId ?: "VPS-882910-UPI"}",
                        style = MaterialTheme.typography.labelSmall.copy(color = TextMuted)
                    )
                    Text(
                        text = "Sender: ${part.senderName ?: "Rajesh Verma (+91 98201 ••••)"}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                    )
                    Text(
                        text = "Instant Credit to State Bank of India ••4012",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = CreditEmeraldLight,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("GPay", "BHIM", "FastPay").forEach { badge ->
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(SurfaceLevel3)
                                    .border(1.dp, SurfaceOutline, CircleShape)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(badge, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, color = TextSecondary))
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "DYNAMIC TOKEN",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextMuted,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(DebitCoralDark.copy(alpha = 0.5f))
                                .border(1.dp, DebitCoral.copy(alpha = 0.4f), CircleShape)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = timerText,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = DebitCoralLight,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }

                    Text(
                        text = "Ask customer to point any UPI App camera at this QR.",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                    )

                    // Prompt Voice Button
                    Button(
                        onClick = onVoicePrompt,
                        modifier = Modifier.fillMaxWidth(),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SurfaceLevel3,
                            contentColor = TextPrimary
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.VolumeUp, contentDescription = null, tint = TrustBlue, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Prompt Voice on Soundbox", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                    }

                    // Test Settle Button
                    OutlinedButton(
                        onClick = onSimulatePaid,
                        modifier = Modifier.fillMaxWidth(),
                        shape = CircleShape,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CreditEmeraldLight)
                    ) {
                        Text("Simulate Payment Received", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }
        }
    }
}

@Composable
fun PosKhataCustomerPickerDialog(
    customers: List<CustomerEntity>,
    totalAmount: Double,
    cartItemCount: Int,
    onDismiss: () -> Unit,
    onSelectCustomer: (CustomerEntity) -> Unit,
    onAddNewCustomer: (name: String, phone: String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var isAddingNew by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var newPhone by remember { mutableStateOf("") }

    val filtered = remember(customers, searchQuery) {
        if (searchQuery.isBlank()) customers
        else customers.filter { it.name.contains(searchQuery, ignoreCase = true) || it.phone.contains(searchQuery) }
    }

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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(TrustBlue.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.MenuBook, contentDescription = null, tint = TrustBlue, modifier = Modifier.size(20.dp))
                    }
                    Column {
                        Text(
                            text = "खाता उधार बिक्री (Khata Sale)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                        )
                        Text(
                            text = "₹${totalAmount.toInt()} • $cartItemCount आइटम्स स्टॉक से घटेंगे",
                            style = MaterialTheme.typography.labelSmall.copy(color = CreditEmerald, fontWeight = FontWeight.SemiBold)
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                }
            }

            if (!isAddingNew) {
                // Search box
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("ग्राहक का नाम या मोबाइल नंबर खोजें...", color = TextMuted, fontSize = 13.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted) }
                )

                // Quick Add Customer button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isAddingNew = true }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = TrustBlue, modifier = Modifier.size(18.dp))
                    Text("+ नया ग्राहक जोड़ें (Add New Customer)", style = MaterialTheme.typography.labelMedium.copy(color = TrustBlue, fontWeight = FontWeight.Bold))
                }

                // Customers List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (filtered.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 30.dp), contentAlignment = Alignment.Center) {
                                Text("कोई ग्राहक नहीं मिला। नया जोड़ें।", style = MaterialTheme.typography.bodySmall.copy(color = TextMuted))
                            }
                        }
                    } else {
                        items(filtered, key = { it.id }) { customer ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(SurfaceLevel3)
                                    .border(1.dp, SurfaceOutline, RoundedCornerShape(14.dp))
                                    .clickable { onSelectCustomer(customer) }
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(TrustBlue.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = customer.name.take(1).uppercase(),
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = TrustBlue)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = customer.name,
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                                        )
                                        Text(
                                            text = customer.phone,
                                            style = MaterialTheme.typography.labelSmall.copy(color = TextMuted)
                                        )
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "बाकी: ₹${customer.balanceDue.toInt()}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = if (customer.balanceDue > 0) DebitCoralLight else CreditEmeraldLight,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                    Text(
                                        text = "दर्ज करें >",
                                        style = MaterialTheme.typography.labelSmall.copy(color = TrustBlue, fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Inline Add Customer form
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("नया ग्राहक विवरण दर्ज करें", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = TextPrimary))

                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("ग्राहक का नाम (Customer Name)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = newPhone,
                        onValueChange = { newPhone = it },
                        label = { Text("मोबाइल नंबर (10-digit Phone)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { isAddingNew = false },
                            modifier = Modifier.weight(1f),
                            shape = CircleShape
                        ) {
                            Text("रद्द करें")
                        }

                        Button(
                            onClick = {
                                if (newName.isNotBlank() && newPhone.length >= 10) {
                                    onAddNewCustomer(newName, newPhone)
                                    isAddingNew = false
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = TrustBlue, contentColor = Color.White)
                        ) {
                            Text("ग्राहक सहेजें")
                        }
                    }
                }
            }
        }
    }
}
