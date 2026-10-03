package com.example.service

import java.util.regex.Pattern

data class ParsedPayment(
    val amount: Double,
    val payerName: String?,
    val upiRef: String?,
    val bankOrApp: String,
    val isCredit: Boolean,
    val rawText: String,
    val timestamp: Long = System.currentTimeMillis()
)

object PaymentParser {

    private val BANK_PREFIXES = mapOf(
        "SBI" to "State Bank of India",
        "HDFC" to "HDFC Bank",
        "ICICI" to "ICICI Bank",
        "AXIS" to "Axis Bank",
        "PNB" to "Punjab National Bank",
        "BOB" to "Bank of Baroda",
        "KOTAK" to "Kotak Mahindra Bank",
        "CANARA" to "Canara Bank",
        "UNION" to "Union Bank",
        "PAYTM" to "Paytm Payments Bank"
    )

    private val AMOUNT_PATTERNS = listOf(
        // "credited with INR 1,500.00" / "credited with Rs 500" / "received ₹350"
        Pattern.compile("(?:credited|received|deposited|paid|credited by|received from)\\s*(?:with\\s*)?(?:inr|rs\\.?|₹)\\s*([0-9,]+(?:\\.[0-9]{1,2})?)", Pattern.CASE_INSENSITIVE),
        // "INR 500.00 credited" / "Rs. 1,200 deposited"
        Pattern.compile("(?:inr|rs\\.?|₹)\\s*([0-9,]+(?:\\.[0-9]{1,2})?)\\s*(?:credited|received|deposited|sent to your a/c|added)", Pattern.CASE_INSENSITIVE),
        // "₹500 received" / "Rs 1,500 received"
        Pattern.compile("(?:₹|rs\\.?|inr)\\s*([0-9,]+(?:\\.[0-9]{1,2})?)", Pattern.CASE_INSENSITIVE),
        // Hindi: "₹ 500 प्राप्त हुए" / "₹ 500 जमा"
        Pattern.compile("([0-9,]+(?:\\.[0-9]{1,2})?)\\s*(?:रुपये|rupees|rs|inr|₹)?\\s*(?:प्राप्त|जमा)", Pattern.CASE_INSENSITIVE)
    )

    private val PAYER_PATTERNS = listOf(
        Pattern.compile("(?:from|by|से|द्वारा)\\s+([A-Za-z\\s]{3,25})(?:\\s+on|\\s+ref|\\s+upi|\\s+vpa|\\.|,|$)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("vpa\\s+([a-zA-Z0-9._-]+@[a-zA-Z]+)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("payer[:\\s]+([A-Za-z\\s]{3,20})", Pattern.CASE_INSENSITIVE),
        Pattern.compile("([A-Za-z\\s]{3,20})\\s+paid you", Pattern.CASE_INSENSITIVE)
    )

    private val UPI_REF_PATTERNS = listOf(
        Pattern.compile("(?:upi\\s*ref(?:\\s*no)?|rrn|utr|ref(?:\\s*no)?|txn\\s*id)[:\\s]*([0-9]{6,16})", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?:upi/|ref/)([0-9]{6,16})", Pattern.CASE_INSENSITIVE)
    )

    fun parse(text: String, sourceHint: String = "Bank SMS"): ParsedPayment? {
        if (text.isBlank()) return null
        val lower = text.lowercase()

        // 1. Credit Verification Check
        val isCredit = lower.contains("credited") ||
                lower.contains("received") ||
                lower.contains("deposited") ||
                lower.contains("paid you") ||
                lower.contains("a/c credited") ||
                lower.contains("acct credited") ||
                lower.contains("प्राप्त") ||
                lower.contains("जमा हुए") ||
                lower.contains("payment of")

        if (!isCredit) return null

        // Exclude debit messages explicitly
        val isDebit = lower.contains("debited") || lower.contains("withdrawn") || lower.contains("spent")
        if (isDebit && !lower.contains("credited")) return null

        // 2. Extract Amount
        var amount: Double? = null
        for (pattern in AMOUNT_PATTERNS) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                val clean = matcher.group(1)?.replace(",", "")
                val parsed = clean?.toDoubleOrNull()
                if (parsed != null && parsed > 0.0) {
                    amount = parsed
                    break
                }
            }
        }
        if (amount == null || amount <= 0.0) return null

        // 3. Extract Payer Name
        var payerName: String? = null
        for (pattern in PAYER_PATTERNS) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                val candidate = matcher.group(1)?.trim()
                if (!candidate.isNullOrBlank() &&
                    !candidate.equals("your", ignoreCase = true) &&
                    !candidate.equals("bank", ignoreCase = true) &&
                    !candidate.equals("account", ignoreCase = true)
                ) {
                    payerName = candidate
                    break
                }
            }
        }

        // 4. Extract UPI Ref / UTR
        var upiRef: String? = null
        for (pattern in UPI_REF_PATTERNS) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                upiRef = matcher.group(1)?.trim()
                break
            }
        }
        if (upiRef.isNullOrBlank()) {
            upiRef = "AUTO-${System.currentTimeMillis() % 1000000}"
        }

        // 5. Determine Bank or App Source
        var bankName = sourceHint
        for ((code, name) in BANK_PREFIXES) {
            if (sourceHint.contains(code, ignoreCase = true) || text.contains(code, ignoreCase = true)) {
                bankName = name
                break
            }
        }

        return ParsedPayment(
            amount = amount,
            payerName = payerName,
            upiRef = upiRef,
            bankOrApp = bankName,
            isCredit = true,
            rawText = text
        )
    }
}
