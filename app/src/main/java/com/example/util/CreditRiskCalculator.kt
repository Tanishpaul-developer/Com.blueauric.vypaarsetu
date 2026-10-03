package com.example.util

import com.example.data.local.CreditRiskLevel
import com.example.data.local.CustomerEntity
import com.example.data.local.CustomerTrustScore
import com.example.data.local.KhataEntryEntity
import java.util.concurrent.TimeUnit

object CreditRiskCalculator {

    /**
     * Calculates the customer's Trust Score and Credit Risk level based on historical ledger entries.
     * Speed of debt clearance vs due date determines the risk level.
     */
    fun calculateScore(
        customer: CustomerEntity,
        entries: List<KhataEntryEntity>
    ): CustomerTrustScore {
        val now = System.currentTimeMillis()
        val customerEntries = entries.filter { it.customerId == customer.id }

        val udharEntries = customerEntries.filter { it.type == "UDHAR" }
        val jamaEntries = customerEntries.filter { it.type == "JAMA" }

        val totalUdharAmount = udharEntries.sumOf { it.amount }
        val totalJamaAmount = jamaEntries.sumOf { it.amount }

        val repaymentRatio = if (totalUdharAmount > 0.0) {
            (totalJamaAmount / totalUdharAmount).coerceAtMost(1.0)
        } else {
            1.0
        }

        // Calculate repayment delays: Pair udhars with subsequent jamas
        val repaymentIntervalsDays = mutableListOf<Double>()
        var overdueCount = 0

        // Check currently outstanding entries
        for (udhar in udharEntries) {
            if (!udhar.isCleared && customer.balanceDue > 0) {
                val ageDays = TimeUnit.MILLISECONDS.toDays(now - udhar.timestamp).toDouble()
                if (udhar.dueDate != null && now > udhar.dueDate) {
                    val daysPastDue = TimeUnit.MILLISECONDS.toDays(now - udhar.dueDate).toInt()
                    if (daysPastDue > 0) {
                        overdueCount++
                    }
                } else if (ageDays > 30) {
                    overdueCount++
                }
            }
        }

        // Pair historical repayments
        for (jama in jamaEntries) {
            val matchingUdhar = udharEntries
                .filter { it.timestamp <= jama.timestamp }
                .maxByOrNull { it.timestamp }

            if (matchingUdhar != null) {
                val diffDays = TimeUnit.MILLISECONDS.toDays(jama.timestamp - matchingUdhar.timestamp).toDouble()
                repaymentIntervalsDays.add(diffDays.coerceAtLeast(1.0))
            }
        }

        // Compute average repayment days
        val avgDays = if (repaymentIntervalsDays.isNotEmpty()) {
            repaymentIntervalsDays.average()
        } else {
            // If no historical repayments yet, look at the age of current balance
            if (customer.balanceDue > 0) {
                TimeUnit.MILLISECONDS.toDays(now - customer.lastTransactionTimestamp).toDouble().coerceAtLeast(1.0)
            } else {
                3.0 // Default healthy start for new customer with no debt
            }
        }

        // Check if customer has severe current overdue
        val hasSevereOverdue = customer.balanceDue > 0 && (
                (customer.dueDate != null && now > customer.dueDate && TimeUnit.MILLISECONDS.toDays(now - customer.dueDate) > 30) ||
                        customer.dueDaysOverdue > 30 ||
                        avgDays > 30.0
                )

        // Classify Risk Level
        val riskLevel = when {
            hasSevereOverdue || avgDays > 30.0 -> CreditRiskLevel.HIGH_RISK
            avgDays in 7.0..30.0 || overdueCount > 0 -> CreditRiskLevel.MODERATE_RISK
            else -> CreditRiskLevel.LOW_RISK
        }

        // Calculate score out of 100
        val baseScore: Int = when (riskLevel) {
            CreditRiskLevel.LOW_RISK -> {
                val speedBonus = ((7.0 - avgDays.coerceIn(1.0, 7.0)) / 7.0 * 15).toInt()
                85 + speedBonus
            }
            CreditRiskLevel.MODERATE_RISK -> {
                val ratioFactor = (repaymentRatio * 20).toInt()
                (55 + ratioFactor - (overdueCount * 5)).coerceIn(50, 84)
            }
            CreditRiskLevel.HIGH_RISK -> {
                val penalty = (avgDays - 30).coerceAtMost(30.0).toInt()
                (45 - penalty - (overdueCount * 10)).coerceIn(5, 49)
            }
        }

        val lastRepayment = jamaEntries.maxByOrNull { it.timestamp }?.timestamp ?: 0L
        val daysSinceLastRepayment = if (lastRepayment > 0L) {
            TimeUnit.MILLISECONDS.toDays(now - lastRepayment)
        } else {
            0L
        }

        return CustomerTrustScore(
            customerId = customer.id,
            customerName = customer.name,
            score = baseScore,
            riskLevel = riskLevel,
            avgRepaymentDays = (avgDays * 10).toInt() / 10.0,
            totalRepaidCount = jamaEntries.size,
            overdueCount = overdueCount,
            repaymentRatio = (repaymentRatio * 100).toInt() / 100.0,
            daysSinceLastRepayment = daysSinceLastRepayment
        )
    }

    fun getMerchantAdvice(score: CustomerTrustScore): String {
        return when (score.riskLevel) {
            CreditRiskLevel.LOW_RISK ->
                "⭐ Excellent borrower! Clears dues within ${score.avgRepaymentDays.toInt()} days. Safe to extend credit up to ₹15,000."
            CreditRiskLevel.MODERATE_RISK ->
                "⚠️ Moderate repayment delay (avg ${score.avgRepaymentDays.toInt()} days). Recommend sending polite WhatsApp reminder before new udhar."
            CreditRiskLevel.HIGH_RISK ->
                "🚨 High risk debt! Average repayment delay exceeds 30 days. Do not extend fresh credit until existing balance is collected."
        }
    }
}
