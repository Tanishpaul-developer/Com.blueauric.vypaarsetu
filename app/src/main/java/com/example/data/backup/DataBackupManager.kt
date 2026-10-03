package com.example.data.backup

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.CustomerEntity
import com.example.data.local.KhataEntryEntity
import com.example.data.local.TransactionEntity
import com.example.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DataBackupManager {

    suspend fun createExportJson(context: Context): String = withContext(Dispatchers.IO) {
        val db = AppDatabase.getInstance(context)
        val prefs = UserPreferencesRepository(context)
        val profile = prefs.merchantProfileFlow.firstOrNull()

        val customers = db.customerDao().getAllCustomersList()
        val entries = db.customerDao().getAllEntriesList()
        val transactions = db.transactionDao().getAllTransactionsList()

        val root = JSONObject().apply {
            put("app", "VyaparSetu")
            put("version", "1.0")
            put("exported_at", System.currentTimeMillis())
            put("export_date", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date()))

            put("merchant", JSONObject().apply {
                put("shop_name", profile?.shopName.orEmpty())
                put("owner_name", profile?.ownerName.orEmpty())
                put("upi_id", profile?.upiId.orEmpty())
                put("phone", profile?.phone.orEmpty())
                put("language_code", profile?.languageCode.orEmpty())
            })

            put("customers", JSONArray().apply {
                customers.forEach { c ->
                    put(JSONObject().apply {
                        put("id", c.id)
                        put("name", c.name)
                        put("phone", c.phone)
                        put("address", c.address)
                        put("gst_number", c.gstNumber)
                        put("balance_due", c.balanceDue)
                        put("last_item", c.lastItemDescription)
                        put("last_timestamp", c.lastTransactionTimestamp)
                        put("is_supplier", c.isSupplier)
                    })
                }
            })

            put("khata_entries", JSONArray().apply {
                entries.forEach { e ->
                    put(JSONObject().apply {
                        put("id", e.id)
                        put("customer_id", e.customerId)
                        put("amount", e.amount)
                        put("type", e.type)
                        put("description", e.description)
                        put("timestamp", e.timestamp)
                    })
                }
            })

            put("transactions", JSONArray().apply {
                transactions.forEach { t ->
                    put(JSONObject().apply {
                        put("id", t.id)
                        put("amount", t.amount)
                        put("payer_name", t.payerName)
                        put("upi_ref", t.upiRef)
                        put("payment_source", t.paymentSource)
                        put("type", t.type)
                        put("status", t.status)
                        put("timestamp", t.timestamp)
                        put("note", t.note)
                    })
                }
            })
        }

        root.toString(2)
    }

    suspend fun restoreFromJson(context: Context, jsonString: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonString)
            if (!root.has("app") || root.getString("app") != "VyaparSetu") {
                return@withContext Result.failure(IllegalArgumentException("Invalid VyaparSetu backup file"))
            }

            val db = AppDatabase.getInstance(context)

            val customersArray = root.optJSONArray("customers") ?: JSONArray()
            val restoredCustomers = mutableListOf<CustomerEntity>()
            for (i in 0 until customersArray.length()) {
                val c = customersArray.getJSONObject(i)
                restoredCustomers.add(
                    CustomerEntity(
                        id = c.optLong("id", 0L),
                        name = c.optString("name", "Unknown"),
                        phone = c.optString("phone", ""),
                        address = c.optString("address", ""),
                        gstNumber = c.optString("gst_number", ""),
                        balanceDue = c.optDouble("balance_due", 0.0),
                        lastItemDescription = c.optString("last_item", ""),
                        lastTransactionTimestamp = c.optLong("last_timestamp", System.currentTimeMillis()),
                        isSupplier = c.optBoolean("is_supplier", false)
                    )
                )
            }

            val entriesArray = root.optJSONArray("khata_entries") ?: JSONArray()
            val restoredEntries = mutableListOf<KhataEntryEntity>()
            for (i in 0 until entriesArray.length()) {
                val e = entriesArray.getJSONObject(i)
                restoredEntries.add(
                    KhataEntryEntity(
                        id = e.optLong("id", 0L),
                        customerId = e.optLong("customer_id", 0L),
                        amount = e.optDouble("amount", 0.0),
                        type = e.optString("type", "UDHAR"),
                        description = e.optString("description", ""),
                        timestamp = e.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }

            val txArray = root.optJSONArray("transactions") ?: JSONArray()
            val restoredTx = mutableListOf<TransactionEntity>()
            for (i in 0 until txArray.length()) {
                val t = txArray.getJSONObject(i)
                restoredTx.add(
                    TransactionEntity(
                        id = t.optLong("id", 0L),
                        amount = t.optDouble("amount", 0.0),
                        payerName = t.optString("payer_name", ""),
                        upiRef = t.optString("upi_ref", ""),
                        paymentSource = t.optString("payment_source", "UPI"),
                        type = t.optString("type", "CREDIT"),
                        status = t.optString("status", "SETTLED"),
                        timestamp = t.optLong("timestamp", System.currentTimeMillis()),
                        note = t.optString("note", "")
                    )
                )
            }

            db.customerDao().insertAllCustomers(restoredCustomers)
            db.customerDao().insertAllEntries(restoredEntries)
            db.transactionDao().insertAll(restoredTx)

            Result.success(restoredCustomers.size + restoredTx.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
