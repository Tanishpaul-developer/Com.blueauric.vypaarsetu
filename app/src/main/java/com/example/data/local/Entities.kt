package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: Double,
    val payerName: String,
    val upiRef: String,
    val paymentSource: String, // "Google Pay", "PhonePe", "Paytm", "BHIM", "Cash"
    val type: String, // "CREDIT", "DEBIT"
    val status: String = "SETTLED", // "SETTLED", "PENDING"
    val timestamp: Long = System.currentTimeMillis(),
    val note: String = "",
    val splitGroupId: String? = null,
    val isSplitPart: Boolean = false,
    val splitIndex: Int = 0,
    val syncStatus: String = "PENDING" // "PENDING", "SYNCED"
)

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val phone: String,
    val address: String = "",
    val gstNumber: String = "",
    val balanceDue: Double = 0.0, // positive = Udhar (customer owes merchant), negative = Advance
    val lastItemDescription: String = "",
    val lastTransactionTimestamp: Long = System.currentTimeMillis(),
    val isSupplier: Boolean = false,
    val dueDaysOverdue: Int = 0,
    val dueDate: Long? = null, // Target repayment due date in epoch millis
    val syncStatus: String = "PENDING" // "PENDING", "SYNCED"
)

@Entity(tableName = "khata_entries")
data class KhataEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val customerId: Long,
    val customerName: String = "",
    val amount: Double,
    val type: String, // "UDHAR" (given/credit to customer) or "JAMA" (received payment)
    val description: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val dueDate: Long? = null, // Due date timestamp in millis
    val paymentRef: String = "",
    val isCleared: Boolean = false,
    val syncStatus: String = "PENDING" // "PENDING", "SYNCED"
)

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val localizedName: String = "",
    val category: String = "किराना (Grocery)",
    val price: Double,
    val unit: String = "kg", // kg, g, L, ml, packet, piece, box
    val stockQuantity: Double = 0.0,
    val lowStockThreshold: Double = 5.0,
    val barcode: String = "",
    val imageUrl: String = "",
    val lastRestockedTimestamp: Long = System.currentTimeMillis(),
    val syncStatus: String = "PENDING" // "PENDING", "SYNCED"
)

@Entity(tableName = "stock_movements")
data class StockMovementEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val productId: Long,
    val productName: String,
    val quantityDelta: Double, // positive = stock in / restocked, negative = sold
    val reason: String = "POS Sale", // "POS Sale", "Restock Arrival", "Manual Adjustment"
    val timestamp: Long = System.currentTimeMillis(),
    val syncStatus: String = "PENDING"
)

@Entity(tableName = "invoices")
data class InvoiceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val invoiceNumber: String,
    val customerName: String,
    val customerPhone: String = "",
    val customerAddress: String = "",
    val customerGst: String = "",
    val invoiceDate: Long = System.currentTimeMillis(),
    val mode: String = "GST_TAX_INVOICE", // "GST_TAX_INVOICE", "NON_GST_ESTIMATE", "BILL_OF_SUPPLY"
    val subtotal: Double = 0.0,
    val totalDiscount: Double = 0.0,
    val taxableAmount: Double = 0.0,
    val cgst: Double = 0.0,
    val sgst: Double = 0.0,
    val igst: Double = 0.0,
    val totalTax: Double = 0.0,
    val roundOff: Double = 0.0,
    val finalAmount: Double = 0.0,
    val paymentMode: String = "CASH", // "CASH", "UPI", "UDHAR", "SPLIT"
    val paymentStatus: String = "PAID", // "PAID", "UNPAID", "PARTIAL"
    val itemsJson: String = "[]",
    val notes: String = "",
    val isInterState: Boolean = false,
    val syncStatus: String = "PENDING", // "PENDING", "SYNCED"
    val createdAt: Long = System.currentTimeMillis()
)

enum class CreditRiskLevel(val label: String, val colorHex: Long, val badgeEmoji: String) {
    LOW_RISK("Low Risk", 0xFF10B981, "🟢"),
    MODERATE_RISK("Moderate Risk", 0xFFF59E0B, "🟡"),
    HIGH_RISK("High Risk", 0xFFEF4444, "🔴")
}

data class CustomerTrustScore(
    val customerId: Long,
    val customerName: String,
    val score: Int, // 0 - 100
    val riskLevel: CreditRiskLevel,
    val avgRepaymentDays: Double,
    val totalRepaidCount: Int,
    val overdueCount: Int,
    val repaymentRatio: Double,
    val daysSinceLastRepayment: Long = 0
)

@Entity(tableName = "feedbacks")
data class FeedbackEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val authorName: String,
    val authorEmail: String = "",
    val category: String = "Feature Request", // "Feature Request", "Bug Report", "UI/UX", "Billing & Soundbox", "General"
    val rating: Int = 5,
    val title: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val upvotes: Int = 0,
    val developerReply: String? = null,
    val replyAuthor: String? = null,
    val replyTimestamp: Long? = null,
    val syncStatus: String = "PENDING"
)

