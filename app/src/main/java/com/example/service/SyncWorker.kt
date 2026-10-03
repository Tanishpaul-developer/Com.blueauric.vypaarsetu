package com.example.service

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.firebase.FirestoreManager
import com.example.data.local.AppDatabase
import com.example.data.preferences.UserPreferencesRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * Robust CoroutineWorker for Local-First Room to Cloud Firestore synchronization.
 * Triggers automatically when network connectivity is restored or on background entry.
 */
class SyncWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val TAG = "SyncWorker"
        const val WORK_NAME_PERIODIC = "vyapar_periodic_cloud_sync"
        const val WORK_NAME_ONE_TIME = "vyapar_onetime_cloud_sync"

        fun isOnline(context: Context): Boolean {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
            val activeNetwork = cm.activeNetwork ?: return false
            val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return false
            return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        }

        fun enqueueImmediateSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .addTag(TAG)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME_ONE_TIME,
                ExistingWorkPolicy.REPLACE,
                request
            )
            Log.d(TAG, "Immediate cloud sync worker enqueued")
        }

        fun enqueuePeriodicSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .addTag(TAG)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME_PERIODIC,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
            Log.d(TAG, "Periodic cloud sync worker enqueued (15m interval)")
        }
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "SyncWorker execution started")

        // 1. Verify Internet Connectivity
        if (!isOnline(context)) {
            Log.d(TAG, "Device is offline. Will retry when network becomes available.")
            return Result.retry()
        }

        return try {
            val prefs = UserPreferencesRepository(context)

            // Check if auto sync is enabled
            val isAutoSyncEnabled = prefs.autoCloudSyncEnabledFlow.first()
            if (!isAutoSyncEnabled) {
                Log.d(TAG, "Sync skipped: auto cloud sync disabled in merchant settings")
                return Result.success()
            }

            // Verify merchant identity
            val auth = FirebaseAuth.getInstance()
            val currentUser = auth.currentUser
            val profile = prefs.merchantProfileFlow.first()
            val merchantKey = currentUser?.uid ?: profile.upiId.ifBlank { "" }

            if (merchantKey.isBlank()) {
                Log.d(TAG, "Merchant key not found. Sync postponed until authentication.")
                return Result.success()
            }

            val db = AppDatabase.getInstance(context)

            // 2. Query and sync pending Invoices
            val pendingInvoices = db.invoiceDao().getPendingInvoices()
            if (pendingInvoices.isNotEmpty()) {
                Log.d(TAG, "Flushing ${pendingInvoices.size} pending invoices to Firestore...")
                val success = FirestoreManager.syncInvoicesToCloud(merchantKey, pendingInvoices)
                if (success) {
                    val syncedIds = pendingInvoices.map { it.id }
                    db.invoiceDao().markInvoicesSynced(syncedIds)
                    Log.d(TAG, "Marked ${syncedIds.size} invoices as SYNCED")
                }
            }

            // 3. Query and sync pending Transactions
            val pendingTransactions = db.transactionDao().getPendingTransactions()
            if (pendingTransactions.isNotEmpty()) {
                Log.d(TAG, "Flushing ${pendingTransactions.size} pending transactions to Firestore...")
                val success = FirestoreManager.syncTransactionsToCloud(merchantKey, pendingTransactions)
                if (success) {
                    val syncedIds = pendingTransactions.map { it.id }
                    db.transactionDao().markTransactionsSynced(syncedIds)
                    Log.d(TAG, "Marked ${syncedIds.size} transactions as SYNCED")
                }
            }

            // 4. Query and sync pending Customers
            val pendingCustomers = db.customerDao().getPendingCustomers()
            if (pendingCustomers.isNotEmpty()) {
                Log.d(TAG, "Flushing ${pendingCustomers.size} pending customers to Firestore...")
                val success = FirestoreManager.syncAllKhataToCloud(merchantKey, pendingCustomers)
                if (success) {
                    val syncedIds = pendingCustomers.map { it.id }
                    db.customerDao().markCustomersSynced(syncedIds)
                    Log.d(TAG, "Marked ${syncedIds.size} customers as SYNCED")
                }
            }

            // 5. Query and sync pending Khata Entries
            val pendingEntries = db.customerDao().getPendingKhataEntries()
            if (pendingEntries.isNotEmpty()) {
                Log.d(TAG, "Flushing ${pendingEntries.size} pending khata entries to Firestore...")
                val success = FirestoreManager.syncKhataEntriesToCloud(merchantKey, pendingEntries)
                if (success) {
                    val syncedIds = pendingEntries.map { it.id }
                    db.customerDao().markKhataEntriesSynced(syncedIds)
                    Log.d(TAG, "Marked ${syncedIds.size} khata entries as SYNCED")
                }
            }

            // 6. Record successful sync timestamp
            prefs.recordCloudSyncSuccess(System.currentTimeMillis())
            Log.d(TAG, "Cloud sync complete for merchant: $merchantKey")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "SyncWorker error: ${e.message}", e)
            Result.retry()
        }
    }
}
