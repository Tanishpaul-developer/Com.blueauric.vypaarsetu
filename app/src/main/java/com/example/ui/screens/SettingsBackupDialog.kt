package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.VolumeUp
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
import androidx.fragment.app.FragmentActivity
import com.example.util.BiometricAuthManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.example.ui.theme.CreditEmerald
import com.example.ui.theme.SurfaceLevel2
import com.example.ui.theme.SurfaceLevel3
import com.example.ui.theme.SurfaceOutline
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TrustBlue
import com.example.ui.viewmodel.MainViewModel

@Composable
fun SettingsBackupDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    onStartTour: () -> Unit
) {
    val context = LocalContext.current
    val profile by viewModel.merchantProfile.collectAsState()
    val backupStatus by viewModel.backupStatus.collectAsState()

    var shopName by remember { mutableStateOf(profile.shopName) }
    var ownerName by remember { mutableStateOf(profile.ownerName) }
    var upiId by remember { mutableStateOf(profile.upiId) }
    var phone by remember { mutableStateOf(profile.phone) }

    var importJsonText by remember { mutableStateOf("") }
    var showImportField by remember { mutableStateOf(false) }

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
                .clickable(onClick = { /* stay */ })
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
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
                    Icon(Icons.Default.Settings, contentDescription = null, tint = TrustBlue)
                    Text(
                        text = "सेटिंग्स और बैकअप (Settings)",
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

            // Merchant details update
            Text("दुकान प्रोफ़ाइल (Merchant Info)", style = MaterialTheme.typography.labelMedium.copy(color = TextSecondary, fontWeight = FontWeight.Bold))

            OutlinedTextField(
                value = shopName,
                onValueChange = { shopName = it },
                label = { Text("दुकान का नाम") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = ownerName,
                onValueChange = { ownerName = it },
                label = { Text("दुकानदार का नाम") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = upiId,
                onValueChange = { upiId = it },
                label = { Text("UPI VPA (pa)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Button(
                onClick = {
                    viewModel.completeOnboarding(shopName, ownerName, upiId, phone, profile.languageCode)
                    Toast.makeText(context, "Profile Saved!", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth().height(46.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = TrustBlue, contentColor = Color.White)
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Save Profile", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Dynamic App Theme Switching (Light / Dark)
            Text(
                "ऐप थीम (Dynamic App Theme)",
                style = MaterialTheme.typography.labelMedium.copy(color = TextSecondary, fontWeight = FontWeight.Bold)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceLevel3)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (profile.isDarkMode) TrustBlue.copy(alpha = 0.2f) else Color(0xFFF59E0B).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (profile.isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                            contentDescription = null,
                            tint = if (profile.isDarkMode) TrustBlue else Color(0xFFF59E0B),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = if (profile.isDarkMode) {
                                if (profile.languageCode == "en") "Dark Mode Active" else "डार्क मोड सक्रिय"
                            } else {
                                if (profile.languageCode == "en") "Light Mode Active" else "लाइट मोड सक्रिय"
                            },
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                        )
                        Text(
                            text = if (profile.isDarkMode) {
                                if (profile.languageCode == "en") "High-contrast dark theme for counter & night" else "दुकान/काउंटर हेतु हाई-कंट्रास्ट डार्क थीम"
                            } else {
                                if (profile.languageCode == "en") "Crisp daylight theme for bright outdoors" else "धूप/दिन के उजाले हेतु ब्राइट थीम"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(color = TextMuted)
                        )
                    }
                }

                Switch(
                    checked = profile.isDarkMode,
                    onCheckedChange = { isDark ->
                        viewModel.setDarkMode(isDark)
                        Toast.makeText(
                            context,
                            if (isDark) {
                                if (profile.languageCode == "en") "Dark Mode enabled" else "डार्क मोड सक्रिय किया गया"
                            } else {
                                if (profile.languageCode == "en") "Light Mode enabled" else "लाइट मोड सक्रिय किया गया"
                            },
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = TrustBlue,
                        uncheckedThumbColor = Color(0xFFF59E0B),
                        uncheckedTrackColor = SurfaceLevel2
                    ),
                    modifier = Modifier.testTag("theme_toggle_switch")
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Biometric Security Lock
            Text(
                "बायोमेट्रिक सुरक्षा लॉक (Biometric Khata Lock)",
                style = MaterialTheme.typography.labelMedium.copy(color = TextSecondary, fontWeight = FontWeight.Bold)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceLevel3)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (profile.biometricLockEnabled) CreditEmerald.copy(alpha = 0.2f) else TextMuted.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fingerprint,
                                contentDescription = null,
                                tint = if (profile.biometricLockEnabled) CreditEmerald else TextMuted,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Column {
                            Text(
                                text = if (profile.biometricLockEnabled) "बायोमेट्रिक लॉक सक्रिय है" else "बायोमेट्रिक लॉक निष्क्रिय",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                            )
                            Text(
                                text = "संवेदनशील खाता बही व उधार डेटा सुरक्षित रखें",
                                style = MaterialTheme.typography.labelSmall.copy(color = TextMuted)
                            )
                        }
                    }

                    Switch(
                        checked = profile.biometricLockEnabled,
                        onCheckedChange = { enable ->
                            val activity = context as? FragmentActivity
                            if (enable) {
                                if (activity != null) {
                                    BiometricAuthManager.authenticate(
                                        activity = activity,
                                        title = "बायोमेट्रिक लॉक सक्षम करें",
                                        subtitle = "लॉक ऑन करने हेतु कृपया फिंगरप्रिंट या पिन से पुष्टि करें",
                                        onSuccess = {
                                            viewModel.setBiometricLockEnabled(true)
                                            viewModel.setBiometricUnlocked(true)
                                            Toast.makeText(context, "बायोमेट्रिक लॉक सक्रिय हो गया!", Toast.LENGTH_SHORT).show()
                                        },
                                        onError = { err ->
                                            Toast.makeText(context, "बायोमेट्रिक विफल: $err", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                } else {
                                    viewModel.setBiometricLockEnabled(true)
                                    viewModel.setBiometricUnlocked(true)
                                }
                            } else {
                                viewModel.setBiometricLockEnabled(false)
                                viewModel.setBiometricUnlocked(false)
                                Toast.makeText(context, "बायोमेट्रिक लॉक बंद किया गया", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = CreditEmerald
                        ),
                        modifier = Modifier.testTag("biometric_toggle_switch")
                    )
                }

                OutlinedButton(
                    onClick = {
                        val activity = context as? FragmentActivity
                        if (activity != null) {
                            BiometricAuthManager.authenticate(
                                activity = activity,
                                title = "सुरक्षा टेस्ट (Biometric Test)",
                                subtitle = "सत्यापन सफल होने पर त्वरित सूचना मिलेगी",
                                onSuccess = {
                                    Toast.makeText(context, "✓ बायोमेट्रिक सत्यापन सफल रहा!", Toast.LENGTH_SHORT).show()
                                },
                                onError = { err ->
                                    Toast.makeText(context, "त्रुटि: $err", Toast.LENGTH_SHORT).show()
                                }
                            )
                        } else {
                            Toast.makeText(context, "बायोमेट्रिक टेस्ट उपलब्ध है", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .testTag("test_biometric_btn"),
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("बायोमेट्रिक टेस्ट करें (Test Biometric)", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Backup & Data Export Section
            Text("डेटा बैकअप व रिस्टोर (Data Backup & JSON)", style = MaterialTheme.typography.labelMedium.copy(color = TextSecondary, fontWeight = FontWeight.Bold))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        viewModel.exportData { json ->
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("VyaparSetu Backup", json))

                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/json"
                                putExtra(Intent.EXTRA_TEXT, json)
                                putExtra(Intent.EXTRA_SUBJECT, "VyaparSetu_Backup.json")
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Export Backup JSON"))
                        }
                    },
                    modifier = Modifier.weight(1f).height(46.dp).testTag("export_backup_btn"),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceLevel3, contentColor = TextPrimary)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, tint = CreditEmerald, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Export JSON", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                }

                Button(
                    onClick = { showImportField = !showImportField },
                    modifier = Modifier.weight(1f).height(46.dp).testTag("import_backup_btn"),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceLevel3, contentColor = TextPrimary)
                ) {
                    Icon(Icons.Default.Upload, contentDescription = null, tint = TrustBlue, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Restore JSON", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                }
            }

            if (showImportField) {
                OutlinedTextField(
                    value = importJsonText,
                    onValueChange = { importJsonText = it },
                    label = { Text("Paste JSON Backup Here") },
                    modifier = Modifier.fillMaxWidth().height(110.dp),
                    shape = RoundedCornerShape(12.dp)
                )

                Button(
                    onClick = {
                        viewModel.importData(importJsonText)
                    },
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = CreditEmerald, contentColor = Color.Black)
                ) {
                    Text("Confirm Restore Data", fontWeight = FontWeight.Bold)
                }
            }

            backupStatus?.let { status ->
                Text(
                    text = status,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = CreditEmerald,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Soundbox Test & App Tour triggers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        viewModel.triggerSoundboxAnnouncement("PhonePe", 500.0, "Test Customer")
                    },
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.VolumeUp, contentDescription = null, tint = CreditEmerald, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Test Voice", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                }

                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onStartTour()
                    },
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.HelpOutline, contentDescription = null, tint = TrustBlue, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Restart Tour", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                }
            }
        }
    }
}
