package com.example.service

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.util.UpiHelper
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class InvoiceDetails(
    val invoiceNo: String,
    val customerName: String,
    val customerPhone: String,
    val previousBalance: Double,
    val transactionAmount: Double,
    val isJama: Boolean, // true = Jama (Credit/Payment received), false = Udhar (Debit/New debt)
    val netBalanceDue: Double,
    val itemNote: String = "दुकान राशन व दैनिक सामग्री लेन-देन",
    val dateFormatted: String = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date()),
    val merchantShopName: String,
    val merchantOwnerName: String,
    val merchantUpiId: String,
    val merchantPhone: String
)

class PdfInvoiceService(private val context: Context) {

    /**
     * Generates a clean, professional A4 PDF invoice using Android's native PdfDocument API.
     */
    fun generateInvoicePdf(details: InvoiceDetails): File {
        val invoicesDir = File(context.cacheDir, "invoices").apply { mkdirs() }
        val outputFile = File(invoicesDir, "Invoice_${details.invoiceNo}.pdf")

        // Standard A4 dimensions in PostScript points: 595 x 842 points (72 dpi)
        val pageWidth = 595
        val pageHeight = 842

        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        renderInvoiceOnCanvas(canvas, pageWidth, pageHeight, details)

        document.finishPage(page)

        try {
            FileOutputStream(outputFile).use { fos ->
                document.writeTo(fos)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            document.close()
        }

        return outputFile
    }

    private fun renderInvoiceOnCanvas(
        canvas: Canvas,
        width: Int,
        height: Int,
        details: InvoiceDetails
    ) {
        // Background
        canvas.drawColor(Color.WHITE)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // 1. Top Header Banner (Trust Blue)
        paint.color = Color.parseColor("#0F3968")
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, width.toFloat(), 110f, paint)

        // Accent Gold / Orange Line
        paint.color = Color.parseColor("#F59E0B")
        canvas.drawRect(0f, 107f, width.toFloat(), 112f, paint)

        // Shop Name & Header Text
        paint.color = Color.WHITE
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 22f
        canvas.drawText(details.merchantShopName, 36f, 44f, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 12f
        paint.color = Color.parseColor("#BFDBFE")
        canvas.drawText("प्रो: ${details.merchantOwnerName} • फोन: +91 ${details.merchantPhone}", 36f, 66f, paint)
        canvas.drawText("UPI ID: ${details.merchantUpiId} • GST / Vyapar Registered", 36f, 86f, paint)

        // Right side badge
        paint.color = Color.WHITE
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 18f
        val invoiceTitle = "बिल व खाता रसीद"
        val titleWidth = paint.measureText(invoiceTitle)
        canvas.drawText(invoiceTitle, width - 36f - titleWidth, 48f, paint)

        paint.textSize = 11f
        paint.color = Color.parseColor("#FDE68A")
        val invNoText = "No: #${details.invoiceNo}"
        canvas.drawText(invNoText, width - 36f - paint.measureText(invNoText), 72f, paint)

        val dateText = details.dateFormatted
        canvas.drawText(dateText, width - 36f - paint.measureText(dateText), 92f, paint)

        // 2. Info Cards (Customer & Meta)
        var curY = 140f

        // Customer Info Box
        paint.color = Color.parseColor("#F1F5F9")
        paint.style = Paint.Style.FILL
        val custBox = RectF(36f, curY, width - 36f, curY + 68f)
        canvas.drawRoundRect(custBox, 8f, 8f, paint)

        paint.color = Color.parseColor("#CBD5E1")
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas.drawRoundRect(custBox, 8f, 8f, paint)

        // Text inside customer box
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#0F172A")
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 13f
        canvas.drawText("ग्राहक का नाम (Customer): ${details.customerName}", 52f, curY + 26f, paint)

        paint.color = Color.parseColor("#475569")
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 11f
        canvas.drawText("मोबाइल नंबर: +91 ${details.customerPhone}", 52f, curY + 48f, paint)

        val txnType = if (details.isJama) "जमा रसीद (Payment Received)" else "उधार चालान (Credit Bill)"
        val txnColor = if (details.isJama) Color.parseColor("#059669") else Color.parseColor("#DC2626")
        paint.color = txnColor
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("प्रकार: $txnType", width - 230f, curY + 26f, paint)

        paint.color = Color.parseColor("#475569")
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("खाता बही (VyaparSetu Khata Ledger)", width - 230f, curY + 48f, paint)

        curY += 90f

        // 3. Ledger & Transaction Table
        paint.color = Color.parseColor("#1E293B")
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(RectF(36f, curY, width - 36f, curY + 30f), 6f, 6f, paint)

        paint.color = Color.WHITE
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 11f
        canvas.drawText("क्र.सं.", 48f, curY + 20f, paint)
        canvas.drawText("विवरण (Description)", 90f, curY + 20f, paint)
        canvas.drawText("प्रकार (Type)", 320f, curY + 20f, paint)
        canvas.drawText("राशि (Amount)", width - 110f, curY + 20f, paint)

        curY += 34f

        fun drawTableRow(index: String, desc: String, type: String, amountStr: String, isAlt: Boolean, isBold: Boolean = false, amountColor: Int = Color.parseColor("#0F172A")) {
            if (isAlt) {
                paint.color = Color.parseColor("#F8FAFC")
                paint.style = Paint.Style.FILL
                canvas.drawRect(36f, curY, width - 36f, curY + 28f, paint)
            }
            paint.color = Color.parseColor("#E2E8F0")
            paint.strokeWidth = 1f
            paint.style = Paint.Style.STROKE
            canvas.drawLine(36f, curY + 28f, width - 36f, curY + 28f, paint)

            paint.style = Paint.Style.FILL
            paint.color = Color.parseColor("#0F172A")
            paint.typeface = Typeface.create(Typeface.DEFAULT, if (isBold) Typeface.BOLD else Typeface.NORMAL)
            paint.textSize = 11f
            canvas.drawText(index, 48f, curY + 18f, paint)
            canvas.drawText(desc, 90f, curY + 18f, paint)
            canvas.drawText(type, 320f, curY + 18f, paint)

            paint.color = amountColor
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val amtW = paint.measureText(amountStr)
            canvas.drawText(amountStr, width - 48f - amtW, curY + 18f, paint)

            curY += 28f
        }

        drawTableRow("1", "पिछला बकाया (Previous Balance)", "बकाया", String.format(Locale.US, "₹ %,.2f", details.previousBalance), false)
        val currentTxnLabel = if (details.isJama) "जमा भुगतान (Cash/UPI Received)" else details.itemNote
        val currentType = if (details.isJama) "जमा (+)" else "उधार (-)"
        val currentAmountStr = String.format(Locale.US, "₹ %,.2f", details.transactionAmount)
        val currentAmountColor = if (details.isJama) Color.parseColor("#059669") else Color.parseColor("#DC2626")
        drawTableRow("2", currentTxnLabel, currentType, currentAmountStr, true, isBold = true, amountColor = currentAmountColor)

        // 4. Net Total Highlight Box
        curY += 12f
        paint.color = Color.parseColor("#EFF6FF")
        paint.style = Paint.Style.FILL
        val totalBox = RectF( width - 260f, curY, width - 36f, curY + 44f)
        canvas.drawRoundRect(totalBox, 8f, 8f, paint)

        paint.color = Color.parseColor("#3B82F6")
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.5f
        canvas.drawRoundRect(totalBox, 8f, 8f, paint)

        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#1E3A8A")
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 12f
        canvas.drawText("कुल शुद्ध देय (Net Due):", width - 248f, curY + 27f, paint)

        val netAmountStr = String.format(Locale.US, "₹ %,.2f", details.netBalanceDue)
        paint.color = if (details.netBalanceDue > 0) Color.parseColor("#DC2626") else Color.parseColor("#059669")
        paint.textSize = 14f
        val netAmtW = paint.measureText(netAmountStr)
        canvas.drawText(netAmountStr, width - 48f - netAmtW, curY + 27f, paint)

        curY += 70f

        // 5. UPI Instant Payment QR Code Block
        if (details.netBalanceDue > 0) {
            val upiUri = UpiHelper.createUpiUri(
                upiId = details.merchantUpiId,
                merchantName = details.merchantShopName,
                amount = details.netBalanceDue,
                note = "Bill-${details.invoiceNo}"
            )
            val qrBitmap: Bitmap? = UpiHelper.generateQrBitmap(upiUri, 150)

            // QR Container
            paint.color = Color.parseColor("#F8FAFC")
            paint.style = Paint.Style.FILL
            val qrCard = RectF(36f, curY, width - 36f, curY + 160f)
            canvas.drawRoundRect(qrCard, 10f, 10f, paint)

            paint.color = Color.parseColor("#CBD5E1")
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1f
            canvas.drawRoundRect(qrCard, 10f, 10f, paint)

            // Draw QR bitmap
            qrBitmap?.let { bmp ->
                canvas.drawBitmap(bmp, 52f, curY + 10f, null)
            }

            paint.style = Paint.Style.FILL
            paint.color = Color.parseColor("#0F172A")
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 13f
            canvas.drawText("त्वरित UPI भुगतान (Instant UPI QR Pay)", 200f, curY + 36f, paint)

            paint.color = Color.parseColor("#475569")
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textSize = 10.5f
            canvas.drawText("Google Pay, PhonePe, Paytm या BHIM से QR स्कैन कर तुरंत भुगतान करें।", 200f, curY + 58f, paint)
            canvas.drawText("UPI VPA: ${details.merchantUpiId}", 200f, curY + 76f, paint)
            canvas.drawText("देय राशि: $netAmountStr", 200f, curY + 94f, paint)

            paint.color = Color.parseColor("#059669")
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 10f
            canvas.drawText("✓ भुगतान होते ही फ़ोन साउंडबॉक्स पर तुरंत ध्वनि पुष्टि होगी।", 200f, curY + 120f, paint)

            curY += 175f
        } else {
            // Settled Notice
            paint.color = Color.parseColor("#ECFDF5")
            paint.style = Paint.Style.FILL
            val settledCard = RectF(36f, curY, width - 36f, curY + 50f)
            canvas.drawRoundRect(settledCard, 8f, 8f, paint)

            paint.color = Color.parseColor("#059669")
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 13f
            canvas.drawText("✓ इस खाते का पूर्ण निपटान हो चुका है (Account Settled in Full). धन्यवाद!", 52f, curY + 30f, paint)

            curY += 70f
        }

        // 6. Terms & Digital Seal
        paint.color = Color.parseColor("#64748B")
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 9.5f
        canvas.drawText("शर्तें: यह रसीद VyaparSetu डिजिटल खाता बही द्वारा प्रमाणित है।", 36f, height - 70f, paint)
        canvas.drawText("किसी भी विसंगति की स्थिति में दुकान पर 7 दिनों के भीतर संपर्क करें।", 36f, height - 56f, paint)

        // Merchant Digital Seal
        paint.color = Color.parseColor("#1E3A8A")
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 11f
        val sealOwner = "हस्ताक्षर / डिजिटल मुहर: ${details.merchantOwnerName}"
        val sealW = paint.measureText(sealOwner)
        canvas.drawText(sealOwner, width - 36f - sealW, height - 60f, paint)

        paint.color = Color.parseColor("#94A3B8")
        paint.textSize = 8.5f
        val footerApp = "VyaparSetu Smart Merchant POS & Digital Soundbox"
        val footW = paint.measureText(footerApp)
        canvas.drawText(footerApp, (width - footW) / 2f, height - 25f, paint)
    }

    /**
     * Prints the invoice using Android PrintManager.
     */
    fun printInvoice(file: File, jobName: String = "VyaparSetu_Invoice") {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
        if (printManager == null) {
            Toast.makeText(context, "PrintManager not available on this device", Toast.LENGTH_SHORT).show()
            return
        }

        val adapter = PdfPrintDocumentAdapter(file)
        val printAttributes = PrintAttributes.Builder()
            .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
            .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
            .build()

        printManager.print(jobName, adapter, printAttributes)
    }

    /**
     * Shares invoice PDF via WhatsApp or other apps using FileProvider.
     */
    fun shareInvoiceViaWhatsApp(
        file: File,
        customerPhone: String,
        customerName: String,
        amountDue: Double
    ) {
        try {
            val contentUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareMessage = "नमस्ते ${customerName} जी, ${detailsShopNameOr(context)} से आपका खाता बिल रसीद संलग्‍न है। कुल बाकी: ₹${String.format(Locale.US, "%,.2f", amountDue)}। कृपया समय पर भुगतान करें। धन्यवाद!"

            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_TEXT, shareMessage)
                putExtra(Intent.EXTRA_SUBJECT, "व्यापार चालान / बिल रसीद")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            // Target WhatsApp specifically if installed
            val cleanPhone = customerPhone.filter { it.isDigit() }
            val formattedPhone = if (cleanPhone.startsWith("91") && cleanPhone.length == 12) {
                cleanPhone
            } else if (cleanPhone.length == 10) {
                "91$cleanPhone"
            } else {
                cleanPhone
            }

            // Try targeting WhatsApp
            val whatsappIntent = Intent(sendIntent).apply {
                setPackage("com.whatsapp")
                if (formattedPhone.isNotBlank()) {
                    putExtra("jid", "$formattedPhone@s.whatsapp.net")
                }
            }

            if (whatsappIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(whatsappIntent)
            } else {
                // Fallback to system share chooser
                val chooser = Intent.createChooser(sendIntent, "बिल रसीद (PDF Invoice) भेजें").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Share error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    private fun detailsShopNameOr(context: Context): String {
        return "दुकान"
    }
}

/**
 * PrintDocumentAdapter implementation that writes a PDF file stream to the destination ParcelFileDescriptor.
 */
class PdfPrintDocumentAdapter(private val file: File) : PrintDocumentAdapter() {

    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes?,
        cancellationSignal: CancellationSignal?,
        callback: LayoutResultCallback?,
        extras: Bundle?
    ) {
        if (cancellationSignal?.isCanceled == true) {
            callback?.onLayoutCancelled()
            return
        }

        val info = PrintDocumentInfo.Builder(file.name)
            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
            .setPageCount(1)
            .build()

        callback?.onLayoutFinished(info, true)
    }

    override fun onWrite(
        pages: Array<out PageRange>?,
        destination: ParcelFileDescriptor?,
        cancellationSignal: CancellationSignal?,
        callback: WriteResultCallback?
    ) {
        if (cancellationSignal?.isCanceled == true) {
            callback?.onWriteCancelled()
            return
        }

        if (destination == null) {
            callback?.onWriteFailed("Destination is null")
            return
        }

        try {
            FileInputStream(file).use { input ->
                FileOutputStream(destination.fileDescriptor).use { output ->
                    val buffer = ByteArray(1024 * 4)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } >= 0) {
                        if (cancellationSignal?.isCanceled == true) {
                            callback?.onWriteCancelled()
                            return
                        }
                        output.write(buffer, 0, bytesRead)
                    }
                }
            }
            callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
        } catch (e: IOException) {
            callback?.onWriteFailed(e.message)
        }
    }
}
