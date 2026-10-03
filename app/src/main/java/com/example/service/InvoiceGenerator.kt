package com.example.service

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
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
import android.util.Log
import androidx.core.content.FileProvider
import com.example.data.local.InvoiceEntity
import com.example.data.model.MerchantProfile
import com.example.util.UpiHelper
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

enum class InvoiceMode(val displayName: String, val title: String) {
    GST_TAX_INVOICE("GST Tax Invoice", "TAX INVOICE"),
    NON_GST_ESTIMATE("Estimate / Quotation", "ESTIMATE / QUOTATION"),
    BILL_OF_SUPPLY("Bill of Supply", "BILL OF SUPPLY")
}

data class InvoiceItem(
    val id: String = System.currentTimeMillis().toString(),
    val name: String,
    val hsnCode: String = "",
    val quantity: Double,
    val unit: String = "pcs",
    val rate: Double,
    val discountPercent: Double = 0.0,
    val taxRatePercent: Double = 18.0 // 0, 5, 12, 18, 28
) {
    val grossAmount: Double get() = quantity * rate
    val discountAmount: Double get() = grossAmount * (discountPercent / 100.0)
    val taxableAmount: Double get() = grossAmount - discountAmount
    val taxAmount: Double get() = taxableAmount * (taxRatePercent / 100.0)
    val totalAmount: Double get() = taxableAmount + taxAmount
}

data class InvoiceCalculation(
    val subtotal: Double,
    val totalDiscount: Double,
    val taxableAmount: Double,
    val cgst: Double,
    val sgst: Double,
    val igst: Double,
    val totalTax: Double,
    val roundOff: Double,
    val finalAmount: Double
)

data class InvoiceData(
    val invoiceNumber: String,
    val dateMillis: Long = System.currentTimeMillis(),
    val mode: InvoiceMode = InvoiceMode.GST_TAX_INVOICE,
    val merchantProfile: MerchantProfile,
    val customerName: String,
    val customerPhone: String = "",
    val customerAddress: String = "",
    val customerGst: String = "",
    val isInterState: Boolean = false,
    val items: List<InvoiceItem>,
    val paymentMode: String = "CASH", // CASH, UPI, UDHAR, SPLIT
    val paymentStatus: String = "PAID",
    val notes: String = "Thank you for your business! Please visit again.",
    val qrCodeBitmap: Bitmap? = null
) {
    val calculation: InvoiceCalculation by lazy {
        InvoiceGenerator.calculateInvoice(items, isInterState, mode)
    }

    val formattedDate: String
        get() = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(dateMillis))
}

class InvoiceGenerator(private val context: Context) {

    companion object {
        private const val TAG = "InvoiceGenerator"

        fun calculateInvoice(
            items: List<InvoiceItem>,
            isInterState: Boolean,
            mode: InvoiceMode = InvoiceMode.GST_TAX_INVOICE
        ): InvoiceCalculation {
            var subtotal = 0.0
            var totalDiscount = 0.0
            var taxableAmount = 0.0
            var totalTax = 0.0

            for (item in items) {
                val gross = item.quantity * item.rate
                val disc = gross * (item.discountPercent / 100.0)
                val taxable = gross - disc
                val tax = if (mode == InvoiceMode.GST_TAX_INVOICE) {
                    taxable * (item.taxRatePercent / 100.0)
                } else {
                    0.0
                }

                subtotal += gross
                totalDiscount += disc
                taxableAmount += taxable
                totalTax += tax
            }

            val cgst: Double
            val sgst: Double
            val igst: Double

            if (mode == InvoiceMode.GST_TAX_INVOICE) {
                if (isInterState) {
                    cgst = 0.0
                    sgst = 0.0
                    igst = totalTax
                } else {
                    cgst = totalTax / 2.0
                    sgst = totalTax / 2.0
                    igst = 0.0
                }
            } else {
                cgst = 0.0
                sgst = 0.0
                igst = 0.0
            }

            val rawFinal = taxableAmount + totalTax
            val roundedFinal = (rawFinal).roundToInt().toDouble()
            val roundOff = roundedFinal - rawFinal

            return InvoiceCalculation(
                subtotal = subtotal,
                totalDiscount = totalDiscount,
                taxableAmount = taxableAmount,
                cgst = cgst,
                sgst = sgst,
                igst = igst,
                totalTax = totalTax,
                roundOff = roundOff,
                finalAmount = roundedFinal
            )
        }

        fun itemsToJson(items: List<InvoiceItem>): String {
            val arr = JSONArray()
            for (item in items) {
                val obj = JSONObject()
                obj.put("id", item.id)
                obj.put("name", item.name)
                obj.put("hsnCode", item.hsnCode)
                obj.put("quantity", item.quantity)
                obj.put("unit", item.unit)
                obj.put("rate", item.rate)
                obj.put("discountPercent", item.discountPercent)
                obj.put("taxRatePercent", item.taxRatePercent)
                arr.put(obj)
            }
            return arr.toString()
        }

        fun jsonToItems(jsonStr: String): List<InvoiceItem> {
            val list = mutableListOf<InvoiceItem>()
            try {
                val arr = JSONArray(jsonStr)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(
                        InvoiceItem(
                            id = obj.optString("id", i.toString()),
                            name = obj.optString("name", "Item"),
                            hsnCode = obj.optString("hsnCode", ""),
                            quantity = obj.optDouble("quantity", 1.0),
                            unit = obj.optString("unit", "pcs"),
                            rate = obj.optDouble("rate", 0.0),
                            discountPercent = obj.optDouble("discountPercent", 0.0),
                            taxRatePercent = obj.optDouble("taxRatePercent", 18.0)
                        )
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing invoice items: ${e.message}")
            }
            return list
        }

        fun invoiceToEntity(invoice: InvoiceData, syncStatus: String = "PENDING"): InvoiceEntity {
            val calc = invoice.calculation
            return InvoiceEntity(
                invoiceNumber = invoice.invoiceNumber,
                customerName = invoice.customerName,
                customerPhone = invoice.customerPhone,
                customerAddress = invoice.customerAddress,
                customerGst = invoice.customerGst,
                invoiceDate = invoice.dateMillis,
                mode = invoice.mode.name,
                subtotal = calc.subtotal,
                totalDiscount = calc.totalDiscount,
                taxableAmount = calc.taxableAmount,
                cgst = calc.cgst,
                sgst = calc.sgst,
                igst = calc.igst,
                totalTax = calc.totalTax,
                roundOff = calc.roundOff,
                finalAmount = calc.finalAmount,
                paymentMode = invoice.paymentMode,
                paymentStatus = invoice.paymentStatus,
                itemsJson = itemsToJson(invoice.items),
                notes = invoice.notes,
                isInterState = invoice.isInterState,
                syncStatus = syncStatus
            )
        }
    }

    private fun getOutputDir(): File {
        val dir = File(context.cacheDir, "invoices")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    // ==========================================================
    // 1. A4 Format PDF Engine (Standard Tax Invoice / Estimate)
    // ==========================================================
    fun generateA4Pdf(invoice: InvoiceData): File {
        val pdfDocument = PdfDocument()
        val pageWidth = 595 // 72 DPI A4 points
        val pageHeight = 842

        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        drawA4OrA5Invoice(
            canvas = canvas,
            pageWidth = pageWidth.toFloat(),
            pageHeight = pageHeight.toFloat(),
            invoice = invoice,
            isA5 = false
        )

        pdfDocument.finishPage(page)

        val file = File(getOutputDir(), "Invoice_${invoice.invoiceNumber}_A4.pdf")
        FileOutputStream(file).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()
        return file
    }

    // ==========================================================
    // 2. A5 Format PDF Engine (Compact Half Page Invoice)
    // ==========================================================
    fun generateA5Pdf(invoice: InvoiceData): File {
        val pdfDocument = PdfDocument()
        val pageWidth = 420 // 72 DPI A5 points
        val pageHeight = 595

        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        drawA4OrA5Invoice(
            canvas = canvas,
            pageWidth = pageWidth.toFloat(),
            pageHeight = pageHeight.toFloat(),
            invoice = invoice,
            isA5 = true
        )

        pdfDocument.finishPage(page)

        val file = File(getOutputDir(), "Invoice_${invoice.invoiceNumber}_A5.pdf")
        FileOutputStream(file).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()
        return file
    }

    // ==========================================================
    // 3. 3-Inch (80mm) Thermal Receipt PDF Engine
    // ==========================================================
    fun generate3InchThermalPdf(invoice: InvoiceData): File {
        val pdfDocument = PdfDocument()
        val pageWidth = 226 // ~80mm width at 72 DPI (approx 226 points)
        // Dynamic height based on number of items
        val baseHeight = 380
        val itemsHeight = invoice.items.size * 28
        val pageHeight = (baseHeight + itemsHeight).coerceAtLeast(450)

        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        draw3InchThermalReceipt(
            canvas = canvas,
            pageWidth = pageWidth.toFloat(),
            pageHeight = pageHeight.toFloat(),
            invoice = invoice
        )

        pdfDocument.finishPage(page)

        val file = File(getOutputDir(), "Receipt_${invoice.invoiceNumber}_Thermal3Inch.pdf")
        FileOutputStream(file).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()
        return file
    }

    // ==========================================================
    // Canvas Drawing Helpers: A4 and A5 Invoices
    // ==========================================================
    private fun drawA4OrA5Invoice(
        canvas: Canvas,
        pageWidth: Float,
        pageHeight: Float,
        invoice: InvoiceData,
        isA5: Boolean
    ) {
        val margin = if (isA5) 20f else 32f
        val scale = if (isA5) 0.72f else 1.0f

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val calc = invoice.calculation
        val merchant = invoice.merchantProfile

        // Header Background Banner
        val headerHeight = if (isA5) 75f else 95f
        paint.color = Color.rgb(15, 23, 42) // Slate 900
        canvas.drawRect(0f, 0f, pageWidth, headerHeight, paint)

        // Accent Stripe
        paint.color = Color.rgb(16, 185, 129) // Emerald 500
        canvas.drawRect(0f, headerHeight, pageWidth, headerHeight + (3f * scale), paint)

        // Shop Name
        paint.color = Color.WHITE
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = if (isA5) 16f else 22f
        val shopName = merchant.shopName.ifBlank { "VyaparSetu Merchant" }
        canvas.drawText(shopName, margin, headerHeight * 0.45f, paint)

        // Subtitle / Contact
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = if (isA5) 8.5f else 10.5f
        paint.color = Color.rgb(203, 213, 225)
        val addressLine = "${merchant.address.ifBlank { "Market Road" }} | Phone: ${merchant.phone.ifBlank { "9876543210" }}"
        canvas.drawText(addressLine, margin, headerHeight * 0.70f, paint)

        if (merchant.gstNumber.isNotBlank()) {
            paint.color = Color.rgb(253, 224, 71) // Gold Yellow
            canvas.drawText("GSTIN: ${merchant.gstNumber}", margin, headerHeight * 0.90f, paint)
        }

        // Invoice Mode Title Badge on Header Right
        paint.color = Color.WHITE
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = if (isA5) 12f else 15f
        val titleText = invoice.mode.title
        val titleWidth = paint.measureText(titleText)
        canvas.drawText(titleText, pageWidth - margin - titleWidth, headerHeight * 0.45f, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = if (isA5) 8.5f else 10f
        paint.color = Color.rgb(226, 232, 240)
        val noText = "No: ${invoice.invoiceNumber}"
        val noWidth = paint.measureText(noText)
        canvas.drawText(noText, pageWidth - margin - noWidth, headerHeight * 0.70f, paint)

        val dateText = invoice.formattedDate
        val dateWidth = paint.measureText(dateText)
        canvas.drawText(dateText, pageWidth - margin - dateWidth, headerHeight * 0.90f, paint)

        // Customer & Billing Info Box
        var currentY = headerHeight + (18f * scale)
        val infoBoxHeight = if (isA5) 60f else 75f
        val infoBoxWidth = pageWidth - (margin * 2)

        paint.color = Color.rgb(248, 250, 252) // Slate 50
        canvas.drawRoundRect(margin, currentY, margin + infoBoxWidth, currentY + infoBoxHeight, 8f, 8f, paint)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        paint.color = Color.rgb(226, 232, 240)
        canvas.drawRoundRect(margin, currentY, margin + infoBoxWidth, currentY + infoBoxHeight, 8f, 8f, paint)
        paint.style = Paint.Style.FILL

        // "Billed To"
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = if (isA5) 9f else 11f
        paint.color = Color.rgb(15, 23, 42)
        canvas.drawText("BILLED TO:", margin + 12f, currentY + 18f, paint)

        paint.textSize = if (isA5) 8.5f else 10.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val custName = invoice.customerName.ifBlank { "Cash Customer / Walk-in" }
        canvas.drawText(custName, margin + 12f, currentY + 34f, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.color = Color.rgb(100, 116, 139)
        var custSub = if (invoice.customerPhone.isNotBlank()) "Phone: ${invoice.customerPhone}" else "Phone: N/A"
        if (invoice.customerAddress.isNotBlank()) custSub += " | ${invoice.customerAddress}"
        canvas.drawText(custSub, margin + 12f, currentY + 48f, paint)

        if (invoice.customerGst.isNotBlank()) {
            paint.color = Color.rgb(2, 132, 199)
            canvas.drawText("Customer GSTIN: ${invoice.customerGst}", margin + 12f, currentY + 62f, paint)
        }

        // Payment Mode Badge on info box right
        val payModeText = "Mode: ${invoice.paymentMode} | Status: ${invoice.paymentStatus}"
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = if (isA5) 8.5f else 10f
        paint.color = if (invoice.paymentStatus == "PAID") Color.rgb(16, 185, 129) else Color.rgb(239, 68, 68)
        val payWidth = paint.measureText(payModeText)
        canvas.drawText(payModeText, margin + infoBoxWidth - 12f - payWidth, currentY + 22f, paint)

        // Item Table Header
        currentY += infoBoxHeight + (14f * scale)
        val colH = if (isA5) 20f else 24f
        paint.color = Color.rgb(241, 245, 249)
        canvas.drawRect(margin, currentY, margin + infoBoxWidth, currentY + colH, paint)

        // Column Layout
        val colItemX = margin + 8f
        val colHsnX = margin + (infoBoxWidth * 0.38f)
        val colQtyX = margin + (infoBoxWidth * 0.52f)
        val colRateX = margin + (infoBoxWidth * 0.65f)
        val colGstX = margin + (infoBoxWidth * 0.78f)
        val colTotalX = margin + infoBoxWidth - 8f

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = if (isA5) 8f else 9.5f
        paint.color = Color.rgb(51, 65, 85)

        canvas.drawText("ITEM DESCRIPTION", colItemX, currentY + (colH * 0.68f), paint)
        canvas.drawText("HSN/SAC", colHsnX, currentY + (colH * 0.68f), paint)
        canvas.drawText("QTY", colQtyX, currentY + (colH * 0.68f), paint)
        canvas.drawText("RATE", colRateX, currentY + (colH * 0.68f), paint)
        if (invoice.mode == InvoiceMode.GST_TAX_INVOICE) {
            canvas.drawText("GST", colGstX, currentY + (colH * 0.68f), paint)
        }
        val amtTitle = "AMOUNT (₹)"
        val amtWidth = paint.measureText(amtTitle)
        canvas.drawText(amtTitle, colTotalX - amtWidth, currentY + (colH * 0.68f), paint)

        currentY += colH

        // Item Rows
        val rowH = if (isA5) 18f else 22f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = if (isA5) 8f else 9.5f

        for ((idx, item) in invoice.items.withIndex()) {
            if (idx % 2 == 1) {
                paint.color = Color.rgb(248, 250, 252)
                canvas.drawRect(margin, currentY, margin + infoBoxWidth, currentY + rowH, paint)
            }

            paint.color = Color.rgb(15, 23, 42)
            // Item name
            val trimmedName = if (item.name.length > 24) item.name.take(22) + ".." else item.name
            canvas.drawText(trimmedName, colItemX, currentY + (rowH * 0.68f), paint)

            paint.color = Color.rgb(100, 116, 139)
            canvas.drawText(item.hsnCode.ifBlank { "-" }, colHsnX, currentY + (rowH * 0.68f), paint)
            canvas.drawText("${item.quantity} ${item.unit}", colQtyX, currentY + (rowH * 0.68f), paint)
            canvas.drawText("₹${String.format(Locale.US, "%.1f", item.rate)}", colRateX, currentY + (rowH * 0.68f), paint)

            if (invoice.mode == InvoiceMode.GST_TAX_INVOICE) {
                canvas.drawText("${item.taxRatePercent.toInt()}%", colGstX, currentY + (rowH * 0.68f), paint)
            }

            paint.color = Color.rgb(15, 23, 42)
            val rowAmtStr = String.format(Locale.US, "₹%.2f", item.totalAmount)
            val rowAmtW = paint.measureText(rowAmtStr)
            canvas.drawText(rowAmtStr, colTotalX - rowAmtW, currentY + (rowH * 0.68f), paint)

            // Row bottom hairline
            paint.color = Color.rgb(241, 245, 249)
            canvas.drawLine(margin, currentY + rowH, margin + infoBoxWidth, currentY + rowH, paint)

            currentY += rowH
        }

        // Summary Totals Section
        currentY += 8f * scale
        val summaryWidth = if (isA5) 170f else 220f
        val summaryX = margin + infoBoxWidth - summaryWidth

        paint.color = Color.rgb(248, 250, 252)
        val summaryHeight = if (invoice.mode == InvoiceMode.GST_TAX_INVOICE) (110f * scale) else (70f * scale)
        canvas.drawRoundRect(summaryX, currentY, margin + infoBoxWidth, currentY + summaryHeight, 6f, 6f, paint)

        var sumY = currentY + (14f * scale)
        val sumLineH = 14f * scale

        fun drawSummaryRow(label: String, value: String, isBold: Boolean = false, isHighlight: Boolean = false) {
            paint.typeface = Typeface.create(Typeface.DEFAULT, if (isBold) Typeface.BOLD else Typeface.NORMAL)
            paint.textSize = if (isHighlight) (if (isA5) 10f else 13f) else (if (isA5) 7.5f else 9f)
            paint.color = if (isHighlight) Color.rgb(16, 185, 129) else Color.rgb(51, 65, 85)
            canvas.drawText(label, summaryX + 8f, sumY, paint)

            val valW = paint.measureText(value)
            canvas.drawText(value, margin + infoBoxWidth - 8f - valW, sumY, paint)
            sumY += sumLineH
        }

        drawSummaryRow("Subtotal:", "₹${String.format(Locale.US, "%.2f", calc.subtotal)}")
        if (calc.totalDiscount > 0) {
            drawSummaryRow("Discount:", "-₹${String.format(Locale.US, "%.2f", calc.totalDiscount)}")
        }
        if (invoice.mode == InvoiceMode.GST_TAX_INVOICE) {
            drawSummaryRow("Taxable Value:", "₹${String.format(Locale.US, "%.2f", calc.taxableAmount)}")
            if (invoice.isInterState) {
                drawSummaryRow("IGST:", "₹${String.format(Locale.US, "%.2f", calc.igst)}")
            } else {
                drawSummaryRow("CGST:", "₹${String.format(Locale.US, "%.2f", calc.cgst)}")
                drawSummaryRow("SGST:", "₹${String.format(Locale.US, "%.2f", calc.sgst)}")
            }
        }
        if (calc.roundOff != 0.0) {
            drawSummaryRow("Round Off:", "₹${String.format(Locale.US, "%+.2f", calc.roundOff)}")
        }
        sumY += 2f
        drawSummaryRow("GRAND TOTAL:", "₹${String.format(Locale.US, "%.2f", calc.finalAmount)}", isBold = true, isHighlight = true)

        // Left Side: UPI QR Code & Notes
        if (invoice.merchantProfile.upiId.isNotBlank()) {
            val qrBitmap = invoice.qrCodeBitmap ?: try {
                val upiUri = UpiHelper.createUpiUri(
                    upiId = invoice.merchantProfile.upiId,
                    merchantName = invoice.merchantProfile.shopName,
                    amount = calc.finalAmount,
                    note = "Inv ${invoice.invoiceNumber}"
                )
                UpiHelper.generateQrBitmap(upiUri, 140)
            } catch (e: Exception) {
                null
            }

            if (qrBitmap != null) {
                val qrSize = if (isA5) 55f else 75f
                val destRect = RectF(margin + 6f, currentY + 2f, margin + 6f + qrSize, currentY + 2f + qrSize)
                canvas.drawBitmap(qrBitmap, null, destRect, null)

                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                paint.textSize = if (isA5) 6.5f else 8f
                paint.color = Color.rgb(100, 116, 139)
                canvas.drawText("Scan UPI to Pay: ${invoice.merchantProfile.upiId}", margin + 6f, currentY + qrSize + 12f, paint)
            }
        }

        // Footer / Terms
        val footerY = pageHeight - (if (isA5) 24f else 36f)
        paint.color = Color.rgb(203, 213, 225)
        canvas.drawLine(margin, footerY - 10f, pageWidth - margin, footerY - 10f, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        paint.textSize = if (isA5) 7f else 8.5f
        paint.color = Color.rgb(100, 116, 139)
        canvas.drawText(invoice.notes, margin, footerY, paint)

        val brand = "VyaparSetu Smart Billing • Made with Pride in India"
        val brandW = paint.measureText(brand)
        canvas.drawText(brand, pageWidth - margin - brandW, footerY, paint)
    }

    // ==========================================================
    // Canvas Drawing Helpers: 3-Inch (80mm) Thermal Receipt
    // ==========================================================
    private fun draw3InchThermalReceipt(
        canvas: Canvas,
        pageWidth: Float,
        pageHeight: Float,
        invoice: InvoiceData
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val calc = invoice.calculation
        val merchant = invoice.merchantProfile
        val margin = 8f

        paint.color = Color.WHITE
        canvas.drawRect(0f, 0f, pageWidth, pageHeight, paint)

        var y = 20f

        // Centered Shop Header
        paint.color = Color.BLACK
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 13f

        fun drawCentered(text: String, size: Float, isBold: Boolean = false) {
            paint.textSize = size
            paint.typeface = Typeface.create(Typeface.DEFAULT, if (isBold) Typeface.BOLD else Typeface.NORMAL)
            val w = paint.measureText(text)
            canvas.drawText(text, (pageWidth - w) / 2f, y, paint)
            y += size + 4f
        }

        val shopName = merchant.shopName.ifBlank { "VyaparSetu Store" }
        drawCentered(shopName, 14f, isBold = true)
        if (merchant.address.isNotBlank()) {
            drawCentered(merchant.address, 8.5f)
        }
        if (merchant.phone.isNotBlank()) {
            drawCentered("Tel: ${merchant.phone}", 8.5f)
        }
        if (merchant.gstNumber.isNotBlank()) {
            drawCentered("GSTIN: ${merchant.gstNumber}", 8.5f, isBold = true)
        }

        // Invoice Title Badge
        drawCentered("--- ${invoice.mode.title} ---", 9.5f, isBold = true)

        fun drawDashedLine() {
            paint.pathEffect = DashPathEffect(floatArrayOf(4f, 4f), 0f)
            paint.strokeWidth = 1f
            paint.style = Paint.Style.STROKE
            paint.color = Color.BLACK
            canvas.drawLine(margin, y, pageWidth - margin, y, paint)
            paint.style = Paint.Style.FILL
            paint.pathEffect = null
            y += 8f
        }

        drawDashedLine()

        // Metadata
        paint.textSize = 8f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Inv No: ${invoice.invoiceNumber}", margin, y, paint)
        val dateStr = SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault()).format(Date(invoice.dateMillis))
        val dateW = paint.measureText(dateStr)
        canvas.drawText(dateStr, pageWidth - margin - dateW, y, paint)
        y += 12f

        if (invoice.customerName.isNotBlank()) {
            canvas.drawText("Cust: ${invoice.customerName}", margin, y, paint)
            if (invoice.customerPhone.isNotBlank()) {
                val phoneW = paint.measureText(invoice.customerPhone)
                canvas.drawText(invoice.customerPhone, pageWidth - margin - phoneW, y, paint)
            }
            y += 12f
        }

        drawDashedLine()

        // Item Header
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 8.5f
        canvas.drawText("ITEM", margin, y, paint)
        canvas.drawText("QTY", pageWidth * 0.45f, y, paint)
        canvas.drawText("RATE", pageWidth * 0.65f, y, paint)
        val amtT = "AMT"
        canvas.drawText(amtT, pageWidth - margin - paint.measureText(amtT), y, paint)
        y += 12f

        drawDashedLine()

        // Itemized Rows
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 8f
        for (item in invoice.items) {
            val shortName = if (item.name.length > 14) item.name.take(13) + "." else item.name
            canvas.drawText(shortName, margin, y, paint)
            canvas.drawText("${item.quantity} ${item.unit}", pageWidth * 0.45f, y, paint)
            canvas.drawText("${item.rate.toInt()}", pageWidth * 0.65f, y, paint)

            val amtStr = String.format(Locale.US, "%.1f", item.totalAmount)
            canvas.drawText(amtStr, pageWidth - margin - paint.measureText(amtStr), y, paint)
            y += 12f
        }

        drawDashedLine()

        // Totals
        fun drawThermalTotal(label: String, value: String, isBold: Boolean = false) {
            paint.typeface = Typeface.create(Typeface.DEFAULT, if (isBold) Typeface.BOLD else Typeface.NORMAL)
            paint.textSize = if (isBold) 10.5f else 8.5f
            canvas.drawText(label, margin, y, paint)
            val valW = paint.measureText(value)
            canvas.drawText(value, pageWidth - margin - valW, y, paint)
            y += if (isBold) 16f else 12f
        }

        drawThermalTotal("Subtotal", "₹${String.format(Locale.US, "%.2f", calc.subtotal)}")
        if (calc.totalDiscount > 0) {
            drawThermalTotal("Discount", "-₹${String.format(Locale.US, "%.2f", calc.totalDiscount)}")
        }
        if (invoice.mode == InvoiceMode.GST_TAX_INVOICE) {
            if (invoice.isInterState) {
                drawThermalTotal("IGST", "₹${String.format(Locale.US, "%.2f", calc.igst)}")
            } else {
                drawThermalTotal("CGST", "₹${String.format(Locale.US, "%.2f", calc.cgst)}")
                drawThermalTotal("SGST", "₹${String.format(Locale.US, "%.2f", calc.sgst)}")
            }
        }
        drawThermalTotal("TOTAL", "₹${String.format(Locale.US, "%.2f", calc.finalAmount)}", isBold = true)
        drawThermalTotal("Pay Mode", "${invoice.paymentMode} (${invoice.paymentStatus})")

        drawDashedLine()

        // UPI QR Code on receipt
        if (invoice.merchantProfile.upiId.isNotBlank()) {
            val qrBitmap = invoice.qrCodeBitmap ?: try {
                val upiUri = UpiHelper.createUpiUri(
                    upiId = invoice.merchantProfile.upiId,
                    merchantName = invoice.merchantProfile.shopName,
                    amount = calc.finalAmount,
                    note = "Inv ${invoice.invoiceNumber}"
                )
                UpiHelper.generateQrBitmap(upiUri, 110)
            } catch (e: Exception) {
                null
            }

            if (qrBitmap != null) {
                val qrSize = 65f
                val destRect = RectF((pageWidth - qrSize) / 2f, y, (pageWidth + qrSize) / 2f, y + qrSize)
                canvas.drawBitmap(qrBitmap, null, destRect, null)
                y += qrSize + 6f
                drawCentered("Scan UPI to Pay", 8f)
            }
        }

        drawCentered("Thank You! Please Visit Again", 8.5f, isBold = true)
        drawCentered("VyaparSetu Thermal Billing", 7.5f)
    }

    // ==========================================================
    // 4. WhatsApp & Android ShareSheet Sharing
    // ==========================================================
    fun shareInvoicePdf(
        pdfFile: File,
        customerPhone: String?,
        invoiceNo: String,
        amount: Double,
        shopName: String
    ) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile
        )

        val cleanPhone = customerPhone?.replace(Regex("[^0-9]"), "") ?: ""
        val fullPhone = if (cleanPhone.length == 10) "91$cleanPhone" else cleanPhone

        val message = "🙏 Namaste! Here is your bill/invoice #$invoiceNo from $shopName for amount ₹${String.format(Locale.US, "%.2f", amount)}. Thank you for shopping with us!"

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Invoice #$invoiceNo - $shopName")
            putExtra(Intent.EXTRA_TEXT, message)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            if (fullPhone.isNotBlank()) {
                // If phone provided, prioritize direct WhatsApp target if installed
                putExtra("jid", "$fullPhone@s.whatsapp.net")
            }
        }

        val chooser = Intent.createChooser(intent, "Share Invoice via WhatsApp or any App").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    // ==========================================================
    // 5. Android PrintManager Integration
    // ==========================================================
    fun printPdfDocument(pdfFile: File, jobName: String = "VyaparSetu_Invoice") {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        val printAdapter = object : PrintDocumentAdapter() {
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
                val info = PrintDocumentInfo.Builder(pdfFile.name)
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
                try {
                    val input = FileInputStream(pdfFile)
                    val output = FileOutputStream(destination?.fileDescriptor)
                    input.copyTo(output)
                    callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    Log.e(TAG, "Print write error: ${e.message}")
                    callback?.onWriteFailed(e.message)
                }
            }
        }

        printManager.print(jobName, printAdapter, PrintAttributes.Builder().build())
    }
}
