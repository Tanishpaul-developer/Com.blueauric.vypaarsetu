package com.example.data.repository

import com.example.data.firebase.FirestoreManager
import com.example.data.local.CustomerDao
import com.example.data.local.CustomerEntity
import com.example.data.local.KhataEntryEntity
import kotlinx.coroutines.flow.Flow

class KhataRepository(private val customerDao: CustomerDao) {

    val allCustomers: Flow<List<CustomerEntity>> = customerDao.getAllCustomers()
    val customersOnly: Flow<List<CustomerEntity>> = customerDao.getCustomersOnly()
    val suppliersOnly: Flow<List<CustomerEntity>> = customerDao.getSuppliersOnly()
    val allKhataEntries: Flow<List<KhataEntryEntity>> = customerDao.getAllKhataEntries()

    val totalCustomerUdhar: Flow<Double?> = customerDao.getTotalCustomerUdhar()
    val totalSupplierPayable: Flow<Double?> = customerDao.getTotalSupplierPayable()

    suspend fun addCustomer(customer: CustomerEntity, merchantUpi: String = ""): Long {
        val id = customerDao.insertCustomer(customer)
        if (merchantUpi.isNotBlank()) {
            FirestoreManager.syncCustomer(merchantUpi, customer.copy(id = id))
        }
        return id
    }

    suspend fun recordUdhar(customerId: Long, amount: Double, itemDescription: String, merchantUpi: String = "") {
        val now = System.currentTimeMillis()
        customerDao.updateCustomerBalance(customerId, amount, now)
        val entry = KhataEntryEntity(
            customerId = customerId,
            amount = amount,
            type = "UDHAR",
            description = itemDescription,
            timestamp = now
        )
        customerDao.insertKhataEntry(entry)
        if (merchantUpi.isNotBlank()) {
            val updated = customerDao.getCustomerById(customerId)
            if (updated != null) {
                FirestoreManager.syncCustomer(merchantUpi, updated)
            }
            FirestoreManager.addKhataEntryToCloud(merchantUpi, entry)
        }
    }

    suspend fun recordJama(customerId: Long, amount: Double, paymentNote: String, merchantUpi: String = "") {
        val now = System.currentTimeMillis()
        customerDao.updateCustomerBalance(customerId, -amount, now)
        val entry = KhataEntryEntity(
            customerId = customerId,
            amount = amount,
            type = "JAMA",
            description = paymentNote,
            timestamp = now
        )
        customerDao.insertKhataEntry(entry)
        if (merchantUpi.isNotBlank()) {
            val updated = customerDao.getCustomerById(customerId)
            if (updated != null) {
                FirestoreManager.syncCustomer(merchantUpi, updated)
            }
            FirestoreManager.addKhataEntryToCloud(merchantUpi, entry)
        }
    }

    suspend fun deleteCustomer(customerId: Long, merchantUpi: String = "") {
        customerDao.deleteCustomer(customerId)
        if (merchantUpi.isNotBlank()) {
            FirestoreManager.deleteCustomerFromCloud(merchantUpi, customerId)
        }
    }

    fun getEntriesForCustomer(customerId: Long): Flow<List<KhataEntryEntity>> {
        return customerDao.getEntriesForCustomer(customerId)
    }

    suspend fun getCustomerById(customerId: Long): CustomerEntity? {
        return customerDao.getCustomerById(customerId)
    }

    suspend fun getAllCustomersList(): List<CustomerEntity> {
        return customerDao.getAllCustomersList()
    }

    suspend fun getAllEntriesList(): List<KhataEntryEntity> {
        return customerDao.getAllEntriesList()
    }

    suspend fun restoreData(customers: List<CustomerEntity>, entries: List<KhataEntryEntity>) {
        customerDao.insertAllCustomers(customers)
        customerDao.insertAllEntries(entries)
    }

    suspend fun syncAllWithCloud(merchantUpi: String): Boolean {
        if (merchantUpi.isBlank()) return false
        val customers = getAllCustomersList()
        val entries = getAllEntriesList()
        return FirestoreManager.syncAllKhataToCloud(merchantUpi, customers, entries)
    }
}
