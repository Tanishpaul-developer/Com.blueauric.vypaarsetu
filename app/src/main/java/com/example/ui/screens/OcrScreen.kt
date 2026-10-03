package com.example.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.net.Uri
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CreditEmerald
import com.example.ui.theme.CreditEmeraldDark
import com.example.ui.theme.CreditEmeraldLight
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

@Composable
fun OcrScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val ocrBitmap by viewModel.ocrBitmap.collectAsState()
    val isOcrProcessing by viewModel.isOcrProcessing.collectAsState()
    val ocrResult by viewModel.ocrResult.collectAsState()

    // Zero-permission Android Photo Picker contract
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                @Suppress("DEPRECATION")
                val bmp = MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                viewModel.setOcrBitmap(bmp)
            } catch (e: Exception) {
                Toast.makeText(context, "Could not open image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            // Header
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
                        Icon(Icons.Default.DocumentScanner, contentDescription = null, tint = TrustBlue, modifier = Modifier.size(18.dp))
                        Text(
                            text = "SMART RECONCILIATION",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TrustBlue,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        )
                    }

                    Text(
                        text = "Gemini 2.5 Flash Screenshot OCR",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary
                        )
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(TrustBlueDark.copy(alpha = 0.5f))
                        .border(1.dp, TrustBlue.copy(alpha = 0.4f), CircleShape)
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "AI Vision",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TrustBlueLight,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }

        item {
            // Upload Drop Area
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(SurfaceLevel2)
                    .border(1.5.dp, TrustBlue.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                    .clickable {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                    .padding(24.dp)
                    .testTag("ocr_upload_box"),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(TrustBlue.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.UploadFile,
                        contentDescription = null,
                        tint = TrustBlue,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Text(
                    text = "बैंक या UPI स्क्रीनशॉट चुनें",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )

                Text(
                    text = "Select scrolling screenshot of bank transaction or passbook",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextMuted
                    )
                )

                Button(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TrustBlue,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.testTag("select_screenshot_btn")
                ) {
                    Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("गैलरी से चुनें (Select Image)", fontWeight = FontWeight.Bold)
                }
            }
        }

        item {
            // Production Guidance Card for Merchants
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(SurfaceLevel2)
                    .border(1.dp, SurfaceOutline, RoundedCornerShape(18.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color(0xFFFBBF24),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "स्क्रीनशॉट स्कैन कैसे काम करता है (How It Works)",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                }

                Text(
                    text = "1. ग्राहक के फोन पर दिखे सफल UPI भुगतान (GPay, PhonePe, Paytm, BHIM) का फोटो खींचें या स्क्रीनशॉट चुनें।\n2. AI विजन ऑटोमेटिकली ग्राहक का नाम, राशि, समय एवं 12-अंक का UTR संदर्भ नंबर निकाल लेगा।\n3. 'खाते में जोड़ें' दबाकर सीधे ग्राहक के बहीखाते में रिकॉर्ड करें।",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextSecondary,
                        lineHeight = 18.sp
                    )
                )
            }
        }

        // Preview & Processing indicator
        if (ocrBitmap != null) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(SurfaceLevel2)
                        .border(1.dp, SurfaceOutline, RoundedCornerShape(20.dp))
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Screen Preview", style = MaterialTheme.typography.labelMedium.copy(color = TextSecondary, fontWeight = FontWeight.Bold))
                        IconButton(onClick = { viewModel.clearOcr() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Clear", tint = TextMuted)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .height(160.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceLevel1),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = ocrBitmap!!.asImageBitmap(),
                            contentDescription = "Uploaded Screenshot",
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    if (isOcrProcessing) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = TrustBlue,
                                strokeWidth = 2.dp
                            )
                            Text(
                                text = "Gemini 2.5 Flash OCR parsing...",
                                style = MaterialTheme.typography.bodySmall.copy(color = TrustBlueLight)
                            )
                        }
                    }
                }
            }
        }

        // OCR Extracted Result Card
        if (ocrResult != null) {
            val res = ocrResult!!
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(SurfaceLevel2)
                        .border(1.5.dp, CreditEmerald.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
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
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Verified, contentDescription = null, tint = CreditEmerald, modifier = Modifier.size(20.dp))
                            Text(
                                text = "Parsed Result (Structured JSON)",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(CreditEmeraldDark.copy(alpha = 0.6f))
                                .border(1.dp, CreditEmerald.copy(alpha = 0.4f), CircleShape)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "SUCCESS",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = CreditEmeraldLight,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            )
                        }
                    }

                    // Field breakdown
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceLevel1)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OcrFieldRow(label = "Payer Name", value = res.payerName)
                        OcrFieldRow(label = "Amount", value = "₹" + String.format(java.util.Locale.US, "%,.2f", res.amount), isAmount = true)
                        OcrFieldRow(label = "UPI Ref / UTR", value = res.upiRef)
                        OcrFieldRow(label = "Transaction Time", value = res.time)
                        OcrFieldRow(label = "Transaction Type", value = res.type)
                    }

                    // Cloud Firestore sync reassurance
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.CloudDone, contentDescription = null, tint = CreditEmeraldLight, modifier = Modifier.size(16.dp))
                        Text(
                            text = "Saved to Cloud Firestore & Local Room Database",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = CreditEmeraldLight,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }

                    // Announce on Soundbox
                    Button(
                        onClick = {
                            viewModel.triggerSoundboxAnnouncement("UPI OCR", res.amount, res.payerName)
                            Toast.makeText(context, "Announcing ₹${res.amount.toInt()} on Soundbox", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CreditEmerald,
                            contentColor = Color.Black
                        )
                    ) {
                        Icon(Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Announce on Soundbox", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun OcrFieldRow(label: String, value: String, isAmount: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (isAmount) FontWeight.ExtraBold else FontWeight.SemiBold,
                color = if (isAmount) CreditEmerald else TextPrimary,
                fontFamily = if (!isAmount) FontFamily.Default else FontFamily.Monospace
            )
        )
    }
}

