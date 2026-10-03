package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalMall
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.SurfaceLevel1
import com.example.ui.theme.SurfaceOutline
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TrustBlue
import com.example.util.LocalAppStrings

enum class NavScreen(val title: String, val icon: ImageVector, val tag: String, val isBottomBarTab: Boolean = true) {
    HOME("Home", Icons.Default.Storefront, "tab_home", true),
    POS_QR("POS", Icons.Default.QrCode2, "tab_pos_qr", true),
    KHATA("Khata", Icons.Default.MenuBook, "tab_khata", true),
    INVENTORY("Stock", Icons.Default.Inventory2, "tab_inventory", true),
    OCR_SCAN("Scan", Icons.Default.DocumentScanner, "tab_ocr_scan", true),
    AI_BOL("AI Bol", Icons.Default.Mic, "tab_ai_bol", true),
    INVOICE_BILLING("Invoices", Icons.Default.Receipt, "screen_invoice_billing", false),
    PRINTER_SETUP("Printer", Icons.Default.Print, "screen_printer_setup", false),
    MINI_STORE("Mini-Store", Icons.Default.LocalMall, "screen_mini_store", false),
    DAILY_SUMMARY("Closing", Icons.Default.Celebration, "screen_daily_summary", false),
    REPORTS_ANALYTICS("Analytics", Icons.Default.Assessment, "screen_reports_analytics", false),
    TRANSACTION_HISTORY("History", Icons.Default.History, "screen_transaction_history", false),
    SETTINGS("Settings", Icons.Default.Settings, "screen_settings", false),
    CONTACT_FEEDBACK("Contact & Feedback", Icons.Default.Forum, "screen_contact_feedback", false)
}

@Composable
fun AppBottomNav(
    currentScreen: NavScreen,
    onScreenSelected: (NavScreen) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = SurfaceLevel1.copy(alpha = 0.96f),
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(68.dp)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavScreen.values().filter { it.isBottomBarTab }.forEach { screen ->
                val isSelected = currentScreen == screen

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onScreenSelected(screen) }
                        )
                        .testTag(screen.tag),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (screen == NavScreen.AI_BOL) {
                        // Ambient blue glow for AI Bol
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) TrustBlue.copy(alpha = 0.25f) else Color.Transparent),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = screen.title,
                                tint = if (isSelected) TrustBlue else TextMuted,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    } else {
                        Icon(
                            imageVector = screen.icon,
                            contentDescription = screen.title,
                            tint = if (isSelected) TrustBlue else TextMuted,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Text(
                        text = screen.title,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) TrustBlue else TextMuted
                        ),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}
