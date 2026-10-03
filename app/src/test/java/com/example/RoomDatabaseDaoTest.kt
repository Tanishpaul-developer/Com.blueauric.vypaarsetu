package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.CustomerDao
import com.example.data.local.CustomerEntity
import com.example.data.local.InvoiceDao
import com.example.data.local.InvoiceEntity
import com.example.data.local.KhataEntryEntity
import com.example.data.local.ProductDao
import com.example.data.local.ProductEntity
import com.example.data.local.TransactionDao
import com.example.data.local.TransactionEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RoomDatabaseDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var transactionDao: TransactionDao
    private lateinit var customerDao: CustomerDao
    private lateinit var invoiceDao: InvoiceDao
    private lateinit var productDao: ProductDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        transactionDao = db.transactionDao()
        customerDao = db.customerDao()
        invoiceDao = db.invoiceDao()
        productDao = db.productDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun testCustomerDao_insertAndQueryCustomer() = runBlocking {
        val customer = CustomerEntity(
            id = 1L,
            name = "Ramesh Kumar",
            phone = "9876543210",
            address = "MG Road, Ward 4",
            balanceDue = 1250.0,
            isSupplier = false,
            syncStatus = "PENDING"
        )
        customerDao.insertCustomer(customer)

        val retrieved = customerDao.getCustomerById(1L)
        assertNotNull(retrieved)
        assertEquals("Ramesh Kumar", retrieved?.name)
        assertEquals(1250.0, retrieved?.balanceDue ?: 0.0, 0.01)
        assertEquals("PENDING", retrieved?.syncStatus)
    }

    @Test
    fun testCustomerDao_updateCustomerBalance() = runBlocking {
        val customer = CustomerEntity(
            id = 2L,
            name = "Suresh Verma",
            phone = "9123456780",
            balanceDue = 500.0
        )
        customerDao.insertCustomer(customer)
        customerDao.updateCustomerBalance(2L, 1000.0, System.currentTimeMillis())

        val updated = customerDao.getCustomerById(2L)
        assertEquals(1500.0, updated?.balanceDue ?: 0.0, 0.01)
    }

    @Test
    fun testKhataEntries_insertAndFetchLedger() = runBlocking {
        val customer = CustomerEntity(
            id = 10L,
            name = "Anita Devi",
            phone = "9988776655",
            balanceDue = 0.0
        )
        customerDao.insertCustomer(customer)

        val entry1 = KhataEntryEntity(
            id = 101L,
            customerId = 10L,
            amount = 450.0,
            type = "UDHAR", // Credit given to customer
            description = "Grocery and oil",
            timestamp = System.currentTimeMillis() - 86400000L,
            syncStatus = "PENDING"
        )
        val entry2 = KhataEntryEntity(
            id = 102L,
            customerId = 10L,
            amount = 200.0,
            type = "JAMA", // Payment received from customer
            description = "UPI repayment",
            timestamp = System.currentTimeMillis(),
            syncStatus = "PENDING"
        )
        customerDao.insertKhataEntry(entry1)
        customerDao.insertKhataEntry(entry2)

        val entries = customerDao.getAllEntriesList().filter { it.customerId == 10L }
        assertEquals(2, entries.size)
        val totalUdhar = entries.filter { it.type == "UDHAR" }.sumOf { it.amount }
        val totalJama = entries.filter { it.type == "JAMA" }.sumOf { it.amount }
        assertEquals(450.0, totalUdhar, 0.01)
        assertEquals(200.0, totalJama, 0.01)
        val balance = totalUdhar - totalJama
        assertEquals(250.0, balance, 0.01)
    }

    @Test
    fun testInvoiceDao_insertAndQueryPendingInvoices() = runBlocking {
        val invoice = InvoiceEntity(
            invoiceNumber = "INV-2026-001",
            invoiceDate = System.currentTimeMillis(),
            customerName = "Gupta Traders",
            customerPhone = "9876501234",
            customerGst = "07AAAAA0000A1Z5",
            itemsJson = """[{"name":"Basmati Rice 10kg","hsnCode":"1006","quantity":2.0,"unit":"Bag","rate":850.0,"discountPercent":5.0,"taxPercent":5.0}]""",
            subtotal = 1615.0,
            taxableAmount = 1615.0,
            cgst = 40.38,
            sgst = 40.38,
            igst = 0.0,
            totalTax = 80.76,
            totalDiscount = 85.0,
            roundOff = -0.06,
            finalAmount = 1695.70,
            paymentMode = "UPI",
            mode = "GST_TAX_INVOICE",
            syncStatus = "PENDING"
        )
        val id = invoiceDao.insertInvoice(invoice)
        assertTrue(id > 0)

        val pending = invoiceDao.getPendingInvoices()
        assertEquals(1, pending.size)
        assertEquals("INV-2026-001", pending[0].invoiceNumber)
        assertEquals(1695.70, pending[0].finalAmount, 0.01)

        // Mark as synced and verify
        invoiceDao.markInvoicesSynced(listOf(pending[0].id))
        val remainingPending = invoiceDao.getPendingInvoices()
        assertEquals(0, remainingPending.size)
    }

    @Test
    fun testTransactionDao_insertAndRetrieve() = runBlocking {
        val now = System.currentTimeMillis()
        val tx1 = TransactionEntity(
            amount = 500.0,
            payerName = "Vikram Singh",
            upiRef = "UPI1234567890",
            paymentSource = "Google Pay",
            type = "CREDIT",
            status = "SETTLED",
            timestamp = now,
            syncStatus = "PENDING"
        )
        val tx2 = TransactionEntity(
            amount = 750.0,
            payerName = "Pooja Sharma",
            upiRef = "UPI9876543210",
            paymentSource = "PhonePe",
            type = "CREDIT",
            status = "SETTLED",
            timestamp = now + 1000L,
            syncStatus = "PENDING"
        )
        transactionDao.insertTransaction(tx1)
        transactionDao.insertTransaction(tx2)

        val allTx = transactionDao.getAllTransactionsList()
        assertEquals(2, allTx.size)
        val total = allTx.sumOf { it.amount }
        assertEquals(1250.0, total, 0.01)
    }

    @Test
    fun testProductDao_inventoryManagement() = runBlocking {
        val product = ProductEntity(
            id = 501L,
            name = "Tata Salt 1kg",
            category = "Kirana",
            price = 28.0,
            stockQuantity = 50.0,
            unit = "Pack",
            syncStatus = "PENDING"
        )
        productDao.insertProduct(product)

        val products = productDao.getAllProductsList()
        assertEquals(1, products.size)
        assertEquals("Tata Salt 1kg", products[0].name)
        assertEquals(50.0, products[0].stockQuantity, 0.01)

        val updated = product.copy(stockQuantity = 45.0)
        productDao.updateProduct(updated)
        val afterSale = productDao.getAllProductsList()
        assertEquals(45.0, afterSale[0].stockQuantity, 0.01)
    }
}
