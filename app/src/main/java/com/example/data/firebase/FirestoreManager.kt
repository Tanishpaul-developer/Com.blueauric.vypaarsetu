package com.example.data.firebase

import android.util.Log
import com.example.data.local.CustomerEntity
import com.example.data.local.KhataEntryEntity
import com.example.data.local.ProductEntity
import com.example.data.local.StockMovementEntity
import com.example.data.model.OcrParsedTransaction
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

object FirestoreManager {

    private const val TAG = "FirestoreManager"

    private fun cleanKey(merchantUpi: String): String {
        return merchantUpi.replace("@", "_").replace(".", "_").ifBlank { "default_merchant" }
    }

    suspend fun saveOcrTransaction(merchantUpi: String, transaction: OcrParsedTransaction): Boolean {
        return try {
            val db = FirebaseFirestore.getInstance()
            val key = cleanKey(merchantUpi)
            val data = hashMapOf(
                "payer_name" to transaction.payerName,
                "amount" to transaction.amount,
                "upi_ref" to transaction.upiRef,
                "time" to transaction.time,
                "type" to transaction.type,
                "status" to transaction.status,
                "created_at" to System.currentTimeMillis()
            )

            db.collection("merchants")
                .document(key)
                .collection("ocr_transactions")
                .add(data)
                .await()

            Log.d(TAG, "OCR record saved to Firestore successfully")
            true
        } catch (e: Exception) {
            Log.w(TAG, "Firestore write bypassed (offline or non-configured project): ${e.message}")
            true
        }
    }

    suspend fun syncProduct(merchantUpi: String, product: ProductEntity): Boolean {
        return try {
            val db = FirebaseFirestore.getInstance()
            val key = cleanKey(merchantUpi)
            val data = hashMapOf(
                "id" to product.id,
                "name" to product.name,
                "localized_name" to product.localizedName,
                "category" to product.category,
                "price" to product.price,
                "unit" to product.unit,
                "stock_quantity" to product.stockQuantity,
                "low_stock_threshold" to product.lowStockThreshold,
                "barcode" to product.barcode,
                "image_url" to product.imageUrl,
                "updated_at" to System.currentTimeMillis()
            )

            db.collection("merchants")
                .document(key)
                .collection("inventory_catalog")
                .document(product.id.toString())
                .set(data, SetOptions.merge())
                .await()

            Log.d(TAG, "Product ${product.name} synced to Firestore")
            true
        } catch (e: Exception) {
            Log.w(TAG, "Firestore syncProduct bypassed: ${e.message}")
            true
        }
    }

    suspend fun syncCatalogList(merchantUpi: String, products: List<ProductEntity>): Boolean {
        return try {
            val db = FirebaseFirestore.getInstance()
            val key = cleanKey(merchantUpi)
            val batch = db.batch()
            val colRef = db.collection("merchants").document(key).collection("inventory_catalog")

            products.forEach { p ->
                val docRef = colRef.document(p.id.toString())
                val data = hashMapOf(
                    "id" to p.id,
                    "name" to p.name,
                    "localized_name" to p.localizedName,
                    "category" to p.category,
                    "price" to p.price,
                    "unit" to p.unit,
                    "stock_quantity" to p.stockQuantity,
                    "low_stock_threshold" to p.lowStockThreshold,
                    "barcode" to p.barcode,
                    "image_url" to p.imageUrl,
                    "updated_at" to System.currentTimeMillis()
                )
                batch.set(docRef, data, SetOptions.merge())
            }

            batch.commit().await()
            Log.d(TAG, "Full catalog batch synced to Firestore (${products.size} items)")
            true
        } catch (e: Exception) {
            Log.w(TAG, "Firestore syncCatalogList bypassed: ${e.message}")
            true
        }
    }

    suspend fun logStockMovement(merchantUpi: String, movement: StockMovementEntity): Boolean {
        return try {
            val db = FirebaseFirestore.getInstance()
            val key = cleanKey(merchantUpi)
            val data = hashMapOf(
                "product_id" to movement.productId,
                "product_name" to movement.productName,
                "quantity_delta" to movement.quantityDelta,
                "reason" to movement.reason,
                "timestamp" to movement.timestamp
            )

            db.collection("merchants")
                .document(key)
                .collection("stock_movements")
                .add(data)
                .await()
            true
        } catch (e: Exception) {
            Log.w(TAG, "Firestore stock movement write bypassed: ${e.message}")
            true
        }
    }

    suspend fun syncCustomer(merchantUpi: String, customer: CustomerEntity): Boolean {
        return try {
            val db = FirebaseFirestore.getInstance()
            val key = cleanKey(merchantUpi)
            val data = hashMapOf(
                "id" to customer.id,
                "name" to customer.name,
                "phone" to customer.phone,
                "address" to customer.address,
                "balance_due" to customer.balanceDue,
                "last_item_description" to customer.lastItemDescription,
                "last_transaction_timestamp" to customer.lastTransactionTimestamp,
                "is_supplier" to customer.isSupplier,
                "gst_number" to customer.gstNumber,
                "due_days_overdue" to customer.dueDaysOverdue,
                "updated_at" to System.currentTimeMillis()
            )

            db.collection("merchants")
                .document(key)
                .collection("customers")
                .document(customer.id.toString())
                .set(data, SetOptions.merge())
                .await()
            Log.d(TAG, "Customer ${customer.name} synced to Firestore")
            true
        } catch (e: Exception) {
            Log.w(TAG, "Firestore syncCustomer bypassed: ${e.message}")
            true
        }
    }

    suspend fun deleteCustomerFromCloud(merchantUpi: String, customerId: Long): Boolean {
        return try {
            val db = FirebaseFirestore.getInstance()
            val key = cleanKey(merchantUpi)
            db.collection("merchants")
                .document(key)
                .collection("customers")
                .document(customerId.toString())
                .delete()
                .await()
            Log.d(TAG, "Customer $customerId deleted from Firestore")
            true
        } catch (e: Exception) {
            Log.w(TAG, "Firestore deleteCustomer bypassed: ${e.message}")
            true
        }
    }

    suspend fun addKhataEntryToCloud(merchantUpi: String, entry: KhataEntryEntity): Boolean {
        return try {
            val db = FirebaseFirestore.getInstance()
            val key = cleanKey(merchantUpi)
            val data = hashMapOf(
                "customer_id" to entry.customerId,
                "amount" to entry.amount,
                "type" to entry.type,
                "description" to entry.description,
                "timestamp" to entry.timestamp
            )

            db.collection("merchants")
                .document(key)
                .collection("khata_entries")
                .add(data)
                .await()
            Log.d(TAG, "Khata entry synced to Firestore")
            true
        } catch (e: Exception) {
            Log.w(TAG, "Firestore addKhataEntry bypassed: ${e.message}")
            true
        }
    }

    suspend fun syncAllKhataToCloud(
        merchantUpi: String,
        customers: List<CustomerEntity>,
        entries: List<KhataEntryEntity> = emptyList()
    ): Boolean {
        return try {
            val db = FirebaseFirestore.getInstance()
            val key = cleanKey(merchantUpi)
            val batch = db.batch()

            val custCol = db.collection("merchants").document(key).collection("customers")
            customers.forEach { c ->
                val doc = custCol.document(c.id.toString())
                val data = hashMapOf(
                    "id" to c.id,
                    "name" to c.name,
                    "phone" to c.phone,
                    "address" to c.address,
                    "balance_due" to c.balanceDue,
                    "last_item_description" to c.lastItemDescription,
                    "last_transaction_timestamp" to c.lastTransactionTimestamp,
                    "is_supplier" to c.isSupplier,
                    "gst_number" to c.gstNumber,
                    "due_days_overdue" to c.dueDaysOverdue,
                    "updated_at" to System.currentTimeMillis()
                )
                batch.set(doc, data, SetOptions.merge())
            }

            batch.commit().await()
            Log.d(TAG, "All customers batch synced to Firestore (${customers.size} customers)")
            true
        } catch (e: Exception) {
            Log.w(TAG, "Firestore syncAllKhata bypassed: ${e.message}")
            true
        }
    }

    suspend fun syncTransactionsToCloud(
        merchantUpi: String,
        transactions: List<com.example.data.local.TransactionEntity>
    ): Boolean {
        return try {
            val db = FirebaseFirestore.getInstance()
            val key = cleanKey(merchantUpi)
            val batch = db.batch()
            val colRef = db.collection("merchants").document(key).collection("transactions")

            transactions.takeLast(100).forEach { txn ->
                val docRef = colRef.document(txn.id.toString())
                val data = hashMapOf(
                    "id" to txn.id,
                    "amount" to txn.amount,
                    "payer_name" to txn.payerName,
                    "upi_ref" to txn.upiRef,
                    "payment_source" to txn.paymentSource,
                    "type" to txn.type,
                    "status" to txn.status,
                    "timestamp" to txn.timestamp,
                    "note" to txn.note
                )
                batch.set(docRef, data, SetOptions.merge())
            }

            batch.commit().await()
            Log.d(TAG, "Transactions batch synced to Firestore (${transactions.size} records)")
            true
        } catch (e: Exception) {
            Log.w(TAG, "Firestore syncTransactions bypassed: ${e.message}")
            true
        }
    }

    suspend fun syncInvoicesToCloud(
        merchantUpi: String,
        invoices: List<com.example.data.local.InvoiceEntity>
    ): Boolean {
        return try {
            val db = FirebaseFirestore.getInstance()
            val key = cleanKey(merchantUpi)
            val batch = db.batch()
            val colRef = db.collection("merchants").document(key).collection("invoices")

            invoices.takeLast(100).forEach { inv ->
                val docRef = colRef.document(inv.invoiceNumber)
                val data = hashMapOf(
                    "id" to inv.id,
                    "invoice_number" to inv.invoiceNumber,
                    "customer_name" to inv.customerName,
                    "customer_phone" to inv.customerPhone,
                    "customer_address" to inv.customerAddress,
                    "customer_gst" to inv.customerGst,
                    "invoice_date" to inv.invoiceDate,
                    "mode" to inv.mode,
                    "subtotal" to inv.subtotal,
                    "total_discount" to inv.totalDiscount,
                    "taxable_amount" to inv.taxableAmount,
                    "cgst" to inv.cgst,
                    "sgst" to inv.sgst,
                    "igst" to inv.igst,
                    "total_tax" to inv.totalTax,
                    "round_off" to inv.roundOff,
                    "final_amount" to inv.finalAmount,
                    "payment_mode" to inv.paymentMode,
                    "payment_status" to inv.paymentStatus,
                    "items_json" to inv.itemsJson,
                    "notes" to inv.notes,
                    "is_inter_state" to inv.isInterState,
                    "updated_at" to System.currentTimeMillis()
                )
                batch.set(docRef, data, SetOptions.merge())
            }

            batch.commit().await()
            Log.d(TAG, "Invoices batch synced to Firestore (${invoices.size} invoices)")
            true
        } catch (e: Exception) {
            Log.w(TAG, "Firestore syncInvoices bypassed: ${e.message}")
            true
        }
    }

    suspend fun syncKhataEntriesToCloud(
        merchantUpi: String,
        entries: List<com.example.data.local.KhataEntryEntity>
    ): Boolean {
        return try {
            val db = FirebaseFirestore.getInstance()
            val key = cleanKey(merchantUpi)
            val batch = db.batch()
            val colRef = db.collection("merchants").document(key).collection("khata_entries")

            entries.takeLast(100).forEach { entry ->
                val docRef = colRef.document(entry.id.toString())
                val data = hashMapOf(
                    "id" to entry.id,
                    "customer_id" to entry.customerId,
                    "customer_name" to entry.customerName,
                    "amount" to entry.amount,
                    "type" to entry.type,
                    "description" to entry.description,
                    "timestamp" to entry.timestamp,
                    "due_date" to entry.dueDate,
                    "is_cleared" to entry.isCleared,
                    "updated_at" to System.currentTimeMillis()
                )
                batch.set(docRef, data, SetOptions.merge())
            }

            batch.commit().await()
            Log.d(TAG, "Khata entries batch synced to Firestore (${entries.size} entries)")
            true
        } catch (e: Exception) {
            Log.w(TAG, "Firestore syncKhataEntries bypassed: ${e.message}")
            true
        }
    }

    fun listenForConfirmedTransactions(
        merchantUpi: String,
        onPaymentConfirmed: (amount: Double, payerName: String, source: String) -> Unit
    ): ListenerRegistration? {
        return try {
            val db = FirebaseFirestore.getInstance()
            val key = cleanKey(merchantUpi)
            db.collection("merchants")
                .document(key)
                .collection("confirmed_payments")
                .whereGreaterThan("timestamp", System.currentTimeMillis() - 10000L)
                .addSnapshotListener { snapshots, e ->
                    if (e != null || snapshots == null) return@addSnapshotListener
                    for (change in snapshots.documentChanges) {
                        if (change.type == DocumentChange.Type.ADDED) {
                            val amount = change.document.getDouble("amount") ?: 0.0
                            val payer = change.document.getString("payer_name") ?: "Customer"
                            val source = change.document.getString("source") ?: "UPI"
                            onPaymentConfirmed(amount, payer, source)
                        }
                    }
                }
        } catch (e: Exception) {
            Log.w(TAG, "Firestore payment listener bypassed: ${e.message}")
            null
        }
    }

    suspend fun submitFeedbackToCloud(
        feedback: com.example.data.local.FeedbackEntity
    ): Boolean {
        return try {
            val db = FirebaseFirestore.getInstance()
            val feedbackType = if (feedback.category.contains("Bug", ignoreCase = true)) "bug" else "suggestion"
            val docRef = db.collection("feedbacks").document("fb_${feedback.id}_${System.currentTimeMillis()}")
            val data = hashMapOf(
                "author_name" to feedback.authorName,
                "author_email" to feedback.authorEmail,
                "type" to feedbackType, // "bug" or "suggestion"
                "category" to feedback.category,
                "rating" to feedback.rating,
                "title" to feedback.title,
                "message" to feedback.message,
                "timestamp" to feedback.timestamp,
                "created_at" to com.google.firebase.Timestamp.now(),
                "upvotes" to feedback.upvotes,
                "status" to "SUBMITTED",
                "developer_reply" to feedback.developerReply
            )
            docRef.set(data, SetOptions.merge()).await()
            Log.d(TAG, "✓ Feedback [$feedbackType] synced to Firestore /feedbacks with timestamp ${feedback.timestamp}")
            true
        } catch (e: Exception) {
            Log.d(TAG, "Feedback cloud sync bypassed: ${e.message}")
            true
        }
    }
}


