package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentTransactions(limit: Int = 20): Flow<List<TransactionEntity>>

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'CREDIT' AND timestamp >= :startOfDayTimestamp")
    fun getTodayCreditTotal(startOfDayTimestamp: Long): Flow<Double?>

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'CREDIT' AND paymentSource != 'Cash' AND timestamp >= :startOfDayTimestamp")
    fun getTodayUpiTotal(startOfDayTimestamp: Long): Flow<Double?>

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'CREDIT' AND paymentSource = 'Cash' AND timestamp >= :startOfDayTimestamp")
    fun getTodayCashTotal(startOfDayTimestamp: Long): Flow<Double?>

    @Query("SELECT COUNT(*) FROM transactions WHERE timestamp >= :startOfDayTimestamp")
    fun getTodayTransactionCount(startOfDayTimestamp: Long): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<TransactionEntity>)

    @Query("DELETE FROM transactions")
    suspend fun clearAll()

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    suspend fun getAllTransactionsList(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE syncStatus = 'PENDING'")
    suspend fun getPendingTransactions(): List<TransactionEntity>

    @Query("UPDATE transactions SET syncStatus = 'SYNCED' WHERE id IN (:ids)")
    suspend fun markTransactionsSynced(ids: List<Long>)
}

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers ORDER BY balanceDue DESC")
    fun getAllCustomers(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE isSupplier = 0 ORDER BY balanceDue DESC")
    fun getCustomersOnly(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE isSupplier = 1 ORDER BY balanceDue DESC")
    fun getSuppliersOnly(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE id = :id LIMIT 1")
    suspend fun getCustomerById(id: Long): CustomerEntity?

    @Query("SELECT SUM(balanceDue) FROM customers WHERE isSupplier = 0 AND balanceDue > 0")
    fun getTotalCustomerUdhar(): Flow<Double?>

    @Query("SELECT SUM(balanceDue) FROM customers WHERE isSupplier = 1 AND balanceDue > 0")
    fun getTotalSupplierPayable(): Flow<Double?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: CustomerEntity): Long

    @Update
    suspend fun updateCustomer(customer: CustomerEntity)

    @Query("UPDATE customers SET balanceDue = balanceDue + :delta, lastTransactionTimestamp = :timestamp, syncStatus = 'PENDING' WHERE id = :customerId")
    suspend fun updateCustomerBalance(customerId: Long, delta: Double, timestamp: Long)

    @Query("DELETE FROM customers WHERE id = :id")
    suspend fun deleteCustomer(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKhataEntry(entry: KhataEntryEntity): Long

    @Query("SELECT * FROM khata_entries WHERE customerId = :customerId ORDER BY timestamp DESC")
    fun getEntriesForCustomer(customerId: Long): Flow<List<KhataEntryEntity>>

    @Query("SELECT * FROM khata_entries ORDER BY timestamp DESC")
    fun getAllKhataEntries(): Flow<List<KhataEntryEntity>>

    @Query("SELECT * FROM khata_entries WHERE type = 'UDHAR' AND isCleared = 0 AND dueDate IS NOT NULL AND dueDate <= :thresholdTimestamp ORDER BY dueDate ASC")
    fun getOverdueEntries(thresholdTimestamp: Long): Flow<List<KhataEntryEntity>>

    @Query("UPDATE khata_entries SET isCleared = 1, syncStatus = 'PENDING' WHERE id = :entryId")
    suspend fun markEntryCleared(entryId: Long)

    @Query("UPDATE khata_entries SET isCleared = 1, syncStatus = 'PENDING' WHERE customerId = :customerId AND type = 'UDHAR'")
    suspend fun markAllCustomerEntriesCleared(customerId: Long)

    @Query("SELECT * FROM customers")
    suspend fun getAllCustomersList(): List<CustomerEntity>

    @Query("SELECT * FROM khata_entries")
    suspend fun getAllEntriesList(): List<KhataEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllCustomers(customers: List<CustomerEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllEntries(entries: List<KhataEntryEntity>)

    @Query("SELECT * FROM customers WHERE syncStatus = 'PENDING'")
    suspend fun getPendingCustomers(): List<CustomerEntity>

    @Query("UPDATE customers SET syncStatus = 'SYNCED' WHERE id IN (:ids)")
    suspend fun markCustomersSynced(ids: List<Long>)

    @Query("SELECT * FROM khata_entries WHERE syncStatus = 'PENDING'")
    suspend fun getPendingKhataEntries(): List<KhataEntryEntity>

    @Query("UPDATE khata_entries SET syncStatus = 'SYNCED' WHERE id IN (:ids)")
    suspend fun markKhataEntriesSynced(ids: List<Long>)
}

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY name ASC")
    fun getAllProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE stockQuantity <= lowStockThreshold ORDER BY stockQuantity ASC")
    fun getLowStockProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun getProductById(id: Long): ProductEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(products: List<ProductEntity>)

    @Update
    suspend fun updateProduct(product: ProductEntity)

    @Query("UPDATE products SET stockQuantity = stockQuantity + :delta, lastRestockedTimestamp = :timestamp, syncStatus = 'PENDING' WHERE id = :productId")
    suspend fun updateStock(productId: Long, delta: Double, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM products WHERE id = :id")
    suspend fun deleteProduct(id: Long)

    @Query("SELECT COUNT(*) FROM products WHERE stockQuantity <= lowStockThreshold")
    fun getLowStockCount(): Flow<Int>

    @Query("SELECT SUM(stockQuantity * price) FROM products")
    fun getTotalInventoryValue(): Flow<Double?>

    @Query("SELECT * FROM products")
    suspend fun getAllProductsList(): List<ProductEntity>

    // Stock Movement logs
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStockMovement(movement: StockMovementEntity): Long

    @Query("SELECT * FROM stock_movements ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentStockMovements(limit: Int = 30): Flow<List<StockMovementEntity>>

    @Query("SELECT * FROM stock_movements WHERE productId = :productId ORDER BY timestamp DESC")
    fun getMovementsForProduct(productId: Long): Flow<List<StockMovementEntity>>

    @Query("SELECT * FROM products WHERE syncStatus = 'PENDING'")
    suspend fun getPendingProducts(): List<ProductEntity>

    @Query("UPDATE products SET syncStatus = 'SYNCED' WHERE id IN (:ids)")
    suspend fun markProductsSynced(ids: List<Long>)
}

@Dao
interface InvoiceDao {
    @Query("SELECT * FROM invoices ORDER BY invoiceDate DESC")
    fun getAllInvoices(): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM invoices ORDER BY invoiceDate DESC LIMIT :limit")
    fun getRecentInvoices(limit: Int = 20): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM invoices WHERE id = :id LIMIT 1")
    suspend fun getInvoiceById(id: Long): InvoiceEntity?

    @Query("SELECT * FROM invoices WHERE invoiceNumber = :invoiceNumber LIMIT 1")
    suspend fun getInvoiceByNumber(invoiceNumber: String): InvoiceEntity?

    @Query("SELECT COUNT(*) FROM invoices")
    fun getInvoiceCount(): Flow<Int>

    @Query("SELECT SUM(finalAmount) FROM invoices WHERE invoiceDate >= :startOfDayTimestamp")
    fun getTodayInvoiceTotal(startOfDayTimestamp: Long): Flow<Double?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoice(invoice: InvoiceEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(invoices: List<InvoiceEntity>)

    @Update
    suspend fun updateInvoice(invoice: InvoiceEntity)

    @Query("DELETE FROM invoices WHERE id = :id")
    suspend fun deleteInvoice(id: Long)

    @Query("SELECT * FROM invoices WHERE syncStatus = 'PENDING'")
    suspend fun getPendingInvoices(): List<InvoiceEntity>

    @Query("UPDATE invoices SET syncStatus = 'SYNCED' WHERE id IN (:ids)")
    suspend fun markInvoicesSynced(ids: List<Long>)
}

@Dao
interface FeedbackDao {
    @Query("SELECT * FROM feedbacks ORDER BY timestamp DESC")
    fun getAllFeedbacks(): Flow<List<FeedbackEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeedback(feedback: FeedbackEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(feedbacks: List<FeedbackEntity>)

    @Query("UPDATE feedbacks SET upvotes = upvotes + 1 WHERE id = :id")
    suspend fun upvoteFeedback(id: Long)

    @Query("SELECT * FROM feedbacks WHERE syncStatus = 'PENDING'")
    suspend fun getPendingFeedbacks(): List<FeedbackEntity>

    @Query("UPDATE feedbacks SET syncStatus = 'SYNCED' WHERE id IN (:ids)")
    suspend fun markFeedbacksSynced(ids: List<Long>)
}

