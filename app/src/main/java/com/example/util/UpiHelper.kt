package com.example.util

import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object UpiHelper {

    fun createUpiUri(
        upiId: String,
        merchantName: String,
        amount: Double,
        note: String = "VyaparSetu POS",
        txnRef: String = "VPS-${System.currentTimeMillis() % 1000000}"
    ): String {
        val encodedName = URLEncoder.encode(merchantName, StandardCharsets.UTF_8.toString())
        val formattedAmount = String.format(java.util.Locale.US, "%.2f", amount)
        val encodedNote = URLEncoder.encode(note, StandardCharsets.UTF_8.toString())

        return "upi://pay?pa=$upiId&pn=$encodedName&am=$formattedAmount&cu=INR&tn=$encodedNote&tr=$txnRef"
    }

    fun generateQrBitmap(content: String, size: Int = 512): Bitmap {
        val hints = hashMapOf<EncodeHintType, Any>().apply {
            put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H)
            put(EncodeHintType.MARGIN, 1)
        }
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size, hints)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        for (x in 0 until width) {
            for (y in 0 until height) {
                bmp.setPixel(x, y, if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE)
            }
        }
        return bmp
    }

    fun createWhatsAppReminderUrl(
        customerPhone: String,
        amount: Double,
        merchantUpi: String,
        shopName: String
    ): String {
        // Formats WhatsApp deep link per specification:
        // https://wa.me/{phone}?text=Dear%20Customer,%20your%20pending%20bill%20is%20Rs.{amount}.%20Pay%20here:%20upi://pay?pa={merchant_upi}&pn={shop_name}&am={amount}
        val cleanPhone = customerPhone.replace("+", "").replace(" ", "").replace("-", "")
        val formattedPhone = if (cleanPhone.length == 10) "91$cleanPhone" else cleanPhone

        val upiLink = "upi://pay?pa=$merchantUpi&pn=${URLEncoder.encode(shopName, "UTF-8")}&am=${String.format(java.util.Locale.US, "%.2f", amount)}&cu=INR"
        val message = "Dear Customer, your pending bill with $shopName is Rs.${String.format(java.util.Locale.US, "%.2f", amount)}. Please click here to pay instantly via UPI: $upiLink"

        val encodedMessage = URLEncoder.encode(message, "UTF-8")
        return "https://wa.me/$formattedPhone?text=$encodedMessage"
    }

    fun generateReminderText(
        customerName: String,
        amount: Double,
        merchantUpi: String,
        shopName: String,
        dueDate: Long? = null
    ): String {
        val upiLink = "upi://pay?pa=$merchantUpi&pn=${URLEncoder.encode(shopName, "UTF-8")}&am=${String.format(java.util.Locale.US, "%.2f", amount)}&cu=INR"
        val dueText = if (dueDate != null && dueDate > 0) {
            val sdf = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
            "\nअंतिम तिथि (Due Date): ${sdf.format(java.util.Date(dueDate))}\n"
        } else ""

        return """
            नमस्ते $customerName जी,
            $shopName से आपका ₹${String.format(java.util.Locale.US, "%.2f", amount)} का बकाया बिल है।$dueText
            कृपया नीचे दिए गए लिंक से किसी भी UPI ऐप (GPay / PhonePe / Paytm / BHIM) द्वारा भुगतान करें:
            $upiLink

            मर्चेंट UPI ID: $merchantUpi
            धन्यवाद! ($shopName)
        """.trimIndent()
    }

    /**
     * Opens the native Android ShareSheet (Intent.ACTION_SEND) with chooser,
     * allowing merchants to pick any installed messaging app (Telegram, Instagram, SMS, WhatsApp).
     */
    fun sharePaymentReminderViaShareSheet(
        context: android.content.Context,
        customerPhone: String,
        customerName: String,
        amount: Double,
        merchantUpi: String,
        shopName: String,
        dueDate: Long? = null
    ) {
        val text = generateReminderText(customerName, amount, merchantUpi, shopName, dueDate)
        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(android.content.Intent.EXTRA_TEXT, text)
            putExtra(android.content.Intent.EXTRA_SUBJECT, "Payment Reminder / भुगतान रिमाइंडर ($shopName)")
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = android.content.Intent.createChooser(
            shareIntent,
            "तगादा रिमाइंडर शेयर करें (Telegram, SMS, WhatsApp, Instagram...)"
        ).apply {
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(chooser)
        } catch (e: Exception) {
            shareGeneral(context, text)
        }
    }

    fun sharePaymentReminder(
        context: android.content.Context,
        customerPhone: String,
        customerName: String,
        amount: Double,
        merchantUpi: String,
        shopName: String,
        targetApp: String? = null
    ) {
        val text = generateReminderText(customerName, amount, merchantUpi, shopName)
        val cleanPhone = customerPhone.replace("+", "").replace(" ", "").replace("-", "")

        when (targetApp?.lowercase()) {
            "whatsapp" -> {
                try {
                    val formattedPhone = if (cleanPhone.length == 10) "91$cleanPhone" else cleanPhone
                    val uri = Uri.parse("https://wa.me/$formattedPhone?text=${URLEncoder.encode(text, "UTF-8")}")
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
                    context.startActivity(intent)
                } catch (e: Exception) {
                    sharePaymentReminderViaShareSheet(context, customerPhone, customerName, amount, merchantUpi, shopName)
                }
            }
            "sms" -> {
                try {
                    val uri = Uri.parse("smsto:$cleanPhone")
                    val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO, uri).apply {
                        putExtra("sms_body", text)
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    sharePaymentReminderViaShareSheet(context, customerPhone, customerName, amount, merchantUpi, shopName)
                }
            }
            "telegram" -> {
                try {
                    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        `package` = "org.telegram.messenger"
                        putExtra(android.content.Intent.EXTRA_TEXT, text)
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    sharePaymentReminderViaShareSheet(context, customerPhone, customerName, amount, merchantUpi, shopName)
                }
            }
            "instagram" -> {
                try {
                    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        `package` = "com.instagram.android"
                        putExtra(android.content.Intent.EXTRA_TEXT, text)
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    sharePaymentReminderViaShareSheet(context, customerPhone, customerName, amount, merchantUpi, shopName)
                }
            }
            else -> {
                sharePaymentReminderViaShareSheet(context, customerPhone, customerName, amount, merchantUpi, shopName)
            }
        }
    }

    private fun shareGeneral(context: android.content.Context, text: String) {
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(android.content.Intent.EXTRA_TEXT, text)
            putExtra(android.content.Intent.EXTRA_SUBJECT, "Payment Reminder / भुगतान रिमाइंडर")
        }
        context.startActivity(android.content.Intent.createChooser(intent, "पेमेंट रिमाइंडर शेयर करें (Share Reminder)"))
    }
}
