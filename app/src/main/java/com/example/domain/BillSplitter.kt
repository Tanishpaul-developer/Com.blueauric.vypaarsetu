package com.example.domain

import com.example.data.model.SplitBillResult
import com.example.data.model.SplitPart
import com.example.data.model.SplitStatus
import com.example.util.UpiHelper
import kotlin.math.ceil

object BillSplitter {

    const val NPCI_ZERO_MDR_THRESHOLD = 2000.0

    /**
     * Splits total amount into sequential portions each strictly under 2,000 if autoSplit is enabled and amount > 2,000.
     * For example:
     * - ₹3,800 -> 2 splits: [₹1,900, ₹1,900]
     * - ₹3,500 -> 2 splits: [₹1,800, ₹1,700]
     * - ₹5,000 -> 3 splits: [₹1,700, ₹1,700, ₹1,600]
     * - ₹1,500 -> 1 ticket: [₹1,500]
     */
    fun calculateSplits(
        totalAmount: Double,
        autoSplitEnabled: Boolean,
        upiId: String,
        shopName: String,
        generateBitmaps: Boolean = true
    ): SplitBillResult {
        if (totalAmount <= 0) {
            return SplitBillResult(totalAmount = 0.0, isAutoSplit = false, parts = emptyList())
        }

        if (!autoSplitEnabled || totalAmount <= NPCI_ZERO_MDR_THRESHOLD) {
            val upiUri = UpiHelper.createUpiUri(upiId, shopName, totalAmount, "Bill Total")
            val qrBmp = if (generateBitmaps) UpiHelper.generateQrBitmap(upiUri) else null
            return SplitBillResult(
                totalAmount = totalAmount,
                isAutoSplit = false,
                parts = listOf(
                    SplitPart(
                        index = 1,
                        totalParts = 1,
                        amount = totalAmount,
                        status = SplitStatus.AWAITING_SCAN,
                        upiUri = upiUri,
                        qrBitmap = qrBmp
                    )
                )
            )
        }

        // Split total into parts strictly <= 2,000
        val numSplits = ceil(totalAmount / 2000.0).toInt().coerceAtLeast(2)

        val splitAmounts = distributeAmount(totalAmount, numSplits)

        val parts = splitAmounts.mapIndexed { index, amount ->
            val partIndex = index + 1
            val note = "Split $partIndex of $numSplits"
            val uri = UpiHelper.createUpiUri(
                upiId = upiId,
                merchantName = shopName,
                amount = amount,
                note = note,
                txnRef = "VPS-${System.currentTimeMillis() % 100000}-$partIndex"
            )
            val qrBmp = if (generateBitmaps) UpiHelper.generateQrBitmap(uri) else null

            SplitPart(
                index = partIndex,
                totalParts = numSplits,
                amount = amount,
                status = SplitStatus.AWAITING_SCAN,
                upiUri = uri,
                qrBitmap = qrBmp
            )
        }

        return SplitBillResult(
            totalAmount = totalAmount,
            isAutoSplit = true,
            parts = parts
        )
    }

    fun distributeAmount(total: Double, partsCount: Int): List<Double> {
        val totalCents = Math.round(total * 100.0)
        val baseCents = totalCents / partsCount
        val remainderCents = totalCents % partsCount

        return List(partsCount) { i ->
            val cents = baseCents + (if (i < remainderCents) 1 else 0)
            cents / 100.0
        }
    }
}
