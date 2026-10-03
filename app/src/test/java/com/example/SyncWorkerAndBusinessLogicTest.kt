package com.example

import com.example.data.local.CreditRiskLevel
import com.example.data.local.CustomerEntity
import com.example.data.local.KhataEntryEntity
import com.example.domain.BillSplitter
import com.example.service.InvoiceItem
import com.example.service.SyncWorker
import com.example.util.CreditRiskCalculator
import com.example.util.UpiHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class SyncWorkerAndBusinessLogicTest {

    // =========================================================================
    // 1. Auto-Split QR & NPCI UPI Formatting Tests
    // =========================================================================

    @Test
    fun testBillSplitter_singleTransactionBelowThreshold() {
        val result = BillSplitter.calculateSplits(
            totalAmount = 1499.0,
            autoSplitEnabled = true,
            upiId = "merchant@icici",
            shopName = "Vyapar Setu",
            generateBitmaps = false
        )
        assertFalse("Amounts below threshold should not be split", result.isAutoSplit)
        assertEquals(1, result.parts.size)
        assertEquals(1499.0, result.parts[0].amount, 0.01)
        assertTrue(result.parts[0].upiUri.contains("am=1499.00"))
    }

    @Test
    fun testBillSplitter_twoWaySplitAboveThreshold() {
        val result = BillSplitter.calculateSplits(
            totalAmount = 3600.0,
            autoSplitEnabled = true,
            upiId = "merchant@icici",
            shopName = "Vyapar Setu",
            generateBitmaps = false
        )
        assertTrue("Amounts above threshold must be auto-split", result.isAutoSplit)
        assertEquals(2, result.parts.size)
        assertEquals(1800.0, result.parts[0].amount, 0.01)
        assertEquals(1800.0, result.parts[1].amount, 0.01)

        // Verify all sub-parts are strictly under 2000 INR
        assertTrue("All parts must be <= 1999 INR", result.parts.all { it.amount <= 1999.0 })
        assertEquals("Total sum of parts must match original", 3600.0, result.parts.sumOf { it.amount }, 0.01)
    }

    @Test
    fun testBillSplitter_multiPartSplitForLargeAmount() {
        val result = BillSplitter.calculateSplits(
            totalAmount = 7500.0,
            autoSplitEnabled = true,
            upiId = "merchant@icici",
            shopName = "Vyapar Setu",
            generateBitmaps = false
        )
        assertTrue(result.isAutoSplit)
        assertEquals(4, result.parts.size)
        val sum = result.parts.sumOf { it.amount }
        assertEquals(7500.0, sum, 0.01)
        assertTrue("Every split part must remain <= 1999.0", result.parts.all { it.amount <= 1999.0 })
    }

    @Test
    fun testUpiUri_npciStandardSpecification() {
        val upiUri = UpiHelper.createUpiUri(
            upiId = "sharma.store@oksbi",
            merchantName = "Sharma Kirana",
            amount = 1850.50,
            note = "Invoice INV-0042"
        )
        assertTrue(upiUri.startsWith("upi://pay?"))
        assertTrue(upiUri.contains("pa=sharma.store@oksbi"))
        assertTrue(upiUri.contains("pn=Sharma+Kirana"))
        assertTrue(upiUri.contains("am=1850.50"))
        assertTrue(upiUri.contains("cu=INR"))
    }

    // =========================================================================
    // 2. Customer Trust Score & Credit Risk Calculation Tests
    // =========================================================================

    @Test
    fun testCreditRisk_fastRepayment_lowRisk() {
        val now = System.currentTimeMillis()
        val customer = CustomerEntity(
            id = 1L,
            name = "Ravi Kumar",
            phone = "9876543210",
            balanceDue = 0.0,
            lastTransactionTimestamp = now
        )
        val entries = listOf(
            KhataEntryEntity(
                id = 1L,
                customerId = 1L,
                amount = 2000.0,
                type = "UDHAR",
                timestamp = now - TimeUnit.DAYS.toMillis(5)
            ),
            KhataEntryEntity(
                id = 2L,
                customerId = 1L,
                amount = 2000.0,
                type = "JAMA",
                timestamp = now - TimeUnit.DAYS.toMillis(1) // Repaid within 4 days (< 7 days)
            )
        )

        val score = CreditRiskCalculator.calculateScore(customer, entries)
        assertEquals(CreditRiskLevel.LOW_RISK, score.riskLevel)
        assertTrue("Low risk score must be >= 85", score.score >= 85)
        assertEquals("🟢", score.riskLevel.badgeEmoji)
        assertEquals("Low Risk", score.riskLevel.label)
    }

    @Test
    fun testCreditRisk_moderateRepayment_moderateRisk() {
        val now = System.currentTimeMillis()
        val customer = CustomerEntity(
            id = 2L,
            name = "Vijay Pal",
            phone = "9876543211",
            balanceDue = 500.0,
            lastTransactionTimestamp = now
        )
        val entries = listOf(
            KhataEntryEntity(
                id = 10L,
                customerId = 2L,
                amount = 3000.0,
                type = "UDHAR",
                timestamp = now - TimeUnit.DAYS.toMillis(25)
            ),
            KhataEntryEntity(
                id = 11L,
                customerId = 2L,
                amount = 2500.0,
                type = "JAMA",
                timestamp = now - TimeUnit.DAYS.toMillis(10) // Repaid in 15 days (7-30 days)
            )
        )

        val score = CreditRiskCalculator.calculateScore(customer, entries)
        assertEquals(CreditRiskLevel.MODERATE_RISK, score.riskLevel)
        assertTrue("Moderate risk score should be in 50..84", score.score in 50..84)
        assertEquals("🟡", score.riskLevel.badgeEmoji)
        assertEquals("Moderate Risk", score.riskLevel.label)
    }

    @Test
    fun testCreditRisk_severeOverdue_highRisk() {
        val now = System.currentTimeMillis()
        val customer = CustomerEntity(
            id = 3L,
            name = "Manoj Singh",
            phone = "9876543212",
            balanceDue = 4000.0,
            dueDate = now - TimeUnit.DAYS.toMillis(35), // Overdue by 35 days
            dueDaysOverdue = 35
        )
        val entries = listOf(
            KhataEntryEntity(
                id = 20L,
                customerId = 3L,
                amount = 4000.0,
                type = "UDHAR",
                timestamp = now - TimeUnit.DAYS.toMillis(45),
                dueDate = now - TimeUnit.DAYS.toMillis(35),
                isCleared = false
            )
        )

        val score = CreditRiskCalculator.calculateScore(customer, entries)
        assertEquals(CreditRiskLevel.HIGH_RISK, score.riskLevel)
        assertTrue("High risk score should be < 50", score.score < 50)
        assertEquals("🔴", score.riskLevel.badgeEmoji)
        assertEquals("High Risk", score.riskLevel.label)
    }

    // =========================================================================
    // 3. GST & Non-GST Invoice Calculation Tests
    // =========================================================================

    @Test
    fun testInvoiceItem_calculationAccuracy() {
        val item = InvoiceItem(
            name = "Aashirvaad Atta 10kg",
            hsnCode = "1101",
            quantity = 3.0,
            unit = "Bag",
            rate = 420.0,
            discountPercent = 10.0, // 10% discount
            taxRatePercent = 5.0    // 5% GST
        )
        // Gross: 3 * 420 = 1260.0
        assertEquals(1260.0, item.grossAmount, 0.01)
        // Discount: 1260 * 0.10 = 126.0
        assertEquals(126.0, item.discountAmount, 0.01)
        // Taxable: 1260 - 126 = 1134.0
        assertEquals(1134.0, item.taxableAmount, 0.01)
        // Tax (5%): 1134 * 0.05 = 56.70
        assertEquals(56.70, item.taxAmount, 0.01)
        // Total: 1134 + 56.70 = 1190.70
        assertEquals(1190.70, item.totalAmount, 0.01)

        // Intra-state CGST & SGST (50% each of total tax)
        val cgst = item.taxAmount / 2.0
        val sgst = item.taxAmount / 2.0
        assertEquals(28.35, cgst, 0.01)
        assertEquals(28.35, sgst, 0.01)
    }

    // =========================================================================
    // 4. SyncWorker Constraints & Task Tags
    // =========================================================================

    @Test
    fun testSyncWorker_configurationConstants() {
        assertEquals("SyncWorker", SyncWorker.TAG)
        assertEquals("vyapar_periodic_cloud_sync", SyncWorker.WORK_NAME_PERIODIC)
        assertEquals("vyapar_onetime_cloud_sync", SyncWorker.WORK_NAME_ONE_TIME)
    }
}
