package com.example

import com.example.domain.BillSplitter
import com.example.service.SoundboxNotificationListenerService
import com.example.service.SoundboxSpeaker
import com.example.util.UpiHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VyaparSetuCoreTest {

    @Test
    fun testUpiUriGeneration_standardNpciFormat() {
        val uri = UpiHelper.createUpiUri(
            upiId = "sharma.kirana@oksbi",
            merchantName = "Sharma Kirana",
            amount = 1500.0,
            note = "Bill 101"
        )
        assertTrue(uri.startsWith("upi://pay?"))
        assertTrue(uri.contains("pa=sharma.kirana@oksbi"))
        assertTrue(uri.contains("pn=Sharma+Kirana"))
        assertTrue(uri.contains("am=1500.00"))
        assertTrue(uri.contains("cu=INR"))
    }

    @Test
    fun testWhatsAppReminderUrl_deepLinkFormat() {
        val url = UpiHelper.createWhatsAppReminderUrl(
            customerPhone = "9823144521",
            amount = 4200.0,
            merchantUpi = "sharma.kirana@oksbi",
            shopName = "Sharma Kirana"
        )
        assertTrue(url.startsWith("https://wa.me/919823144521?text="))
        assertTrue(url.contains("Rs.4200.00"))
        assertTrue(url.contains("sharma.kirana%40oksbi"))
    }

    @Test
    fun testBillSplitter_underThreshold_singleQr() {
        val result = BillSplitter.calculateSplits(
            totalAmount = 1500.0,
            autoSplitEnabled = true,
            upiId = "test@upi",
            shopName = "Test Shop",
            generateBitmaps = false
        )
        assertEquals(1500.0, result.totalAmount, 0.01)
        assertEquals(false, result.isAutoSplit)
        assertEquals(1, result.parts.size)
        assertEquals(1500.0, result.parts[0].amount, 0.01)
    }

    @Test
    fun testBillSplitter_overThreshold_autoSplitUnder2000() {
        val result = BillSplitter.calculateSplits(
            totalAmount = 3800.0,
            autoSplitEnabled = true,
            upiId = "test@upi",
            shopName = "Test Shop",
            generateBitmaps = false
        )
        assertEquals(3800.0, result.totalAmount, 0.01)
        assertEquals(true, result.isAutoSplit)
        assertEquals(2, result.parts.size)
        assertEquals(1900.0, result.parts[0].amount, 0.01)
        assertEquals(1900.0, result.parts[1].amount, 0.01)
        // All parts must be strictly <= 1999
        assertTrue(result.parts.all { it.amount <= 1999.0 })
    }

    @Test
    fun testBillSplitter_largeAmount_splitsEvenly() {
        val result = BillSplitter.calculateSplits(
            totalAmount = 5000.0,
            autoSplitEnabled = true,
            upiId = "test@upi",
            shopName = "Test Shop",
            generateBitmaps = false
        )
        assertEquals(5000.0, result.totalAmount, 0.01)
        assertEquals(true, result.isAutoSplit)
        assertEquals(3, result.parts.size)
        val sum = result.parts.sumOf { it.amount }
        assertEquals(5000.0, sum, 0.01)
        assertTrue(result.parts.all { it.amount <= 1999.0 })
    }

    @Test
    fun testNotificationAmountExtraction_regexPatterns() {
        assertEquals(150.0, SoundboxNotificationListenerService.extractAmount("Received Rs. 150 from Ramesh") ?: 0.0, 0.01)
        assertEquals(500.0, SoundboxNotificationListenerService.extractAmount("A/C credited by ₹500 via PhonePe") ?: 0.0, 0.01)
        assertEquals(1250.0, SoundboxNotificationListenerService.extractAmount("You have received ₹1,250.00 on Google Pay") ?: 0.0, 0.01)
        assertEquals(350.50, SoundboxNotificationListenerService.extractAmount("INR 350.50 deposited to account") ?: 0.0, 0.01)
        assertEquals(85.0, SoundboxNotificationListenerService.extractAmount("85 रुपये प्राप्त हुए Paytm पर") ?: 0.0, 0.01)
    }

    @Test
    fun testSoundboxSpeakerText_multilingualPhrasing() {
        val hindi = SoundboxSpeaker.buildAnnouncementText("PhonePe", 150.0, "hi")
        assertEquals("PhonePe पर ₹150 प्राप्त हुए", hindi)

        val bengali = SoundboxSpeaker.buildAnnouncementText("Google Pay", 250.0, "bn")
        assertEquals("Google Pay এ ₹250 টাকা প্রাপ্ত হয়েছে", bengali)

        val marathi = SoundboxSpeaker.buildAnnouncementText("Paytm", 500.0, "mr")
        assertEquals("Paytm वर ₹500 रुपये मिळाले", marathi)

        val english = SoundboxSpeaker.buildAnnouncementText("UPI", 100.0, "en")
        assertEquals("Received 100 rupees on UPI", english)
    }

    @Test
    fun testMerchantProfile_themeAndBiometricDefaults() {
        val defaultProfile = com.example.data.model.MerchantProfile()
        assertTrue(defaultProfile.isDarkMode)
        assertFalse(defaultProfile.biometricLockEnabled)

        val updatedProfile = defaultProfile.copy(
            isDarkMode = false,
            biometricLockEnabled = true
        )
        assertFalse(updatedProfile.isDarkMode)
        assertTrue(updatedProfile.biometricLockEnabled)
    }

    @Test
    fun testInvoiceDetails_calculationAndProperties() {
        val invoice = com.example.service.InvoiceDetails(
            invoiceNo = "12345",
            customerName = "Ramesh Gupta",
            customerPhone = "9876543210",
            previousBalance = 1500.0,
            transactionAmount = 500.0,
            isJama = false,
            netBalanceDue = 2000.0,
            itemNote = "आटा व तेल खरीद",
            merchantShopName = "Sharma Kirana",
            merchantOwnerName = "Ramesh Sharma",
            merchantUpiId = "sharma@oksbi",
            merchantPhone = "9876543210"
        )

        assertEquals("12345", invoice.invoiceNo)
        assertEquals("Ramesh Gupta", invoice.customerName)
        assertEquals(2000.0, invoice.netBalanceDue, 0.01)
        assertFalse(invoice.isJama)
    }
}
