package com.example.service

import android.content.Context
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
 * Background worker that automatically triggers a data sync to Firestore
 * when the app enters the background, provided the user has authenticated
 * and authorized periodic/automatic sync in settings.
 */
class BackgroundSyncWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        Log.d(TAG, "BackgroundSyncWorker triggered")
        return try {
            val prefs = UserPreferencesRepository(context)

            // 1. Verify user authorized sync in settings
            val isAuthorized = prefs.autoCloudSyncEnabledFlow.first()
            if (!isAuthorized) {
                Log.d(TAG, "Background sync aborted: merchant disabled auto cloud sync in settings")
                return Result.success()
            }

            // 2. Verify merchant is authenticated via Firebase Auth or active merchant profile
            val auth = FirebaseAuth.getInstance()
            val currentUser = auth.currentUser
            val profile = prefs.merchantProfileFlow.first()

            val merchantKey = currentUser?.uid ?: profile.upiId.ifBlank { "" }
            if (merchantKey.isBlank()) {
                Log.d(TAG, "Background sync aborted: merchant is not authenticated")
                return Result.success()
            }

            Log.d(TAG, "Starting background Firestore sync for merchant: $merchantKey")

            // 3. Gather local Room database records
            val db = AppDatabase.getInstance(context)
            val customers = db.customerDao().getAllCustomersList()
            val khataEntries = db.customerDao().getAllEntriesList()
            val products = db.productDao().getAllProductsList()
            val transactions = db.transactionDao().getAllTransactionsList()

            // 4. Batch sync all datasets to Firestore
            val khataSuccess = FirestoreManager.syncAllKhataToCloud(merchantKey, customers, khataEntries)
            val catalogSuccess = FirestoreManager.syncCatalogList(merchantKey, products)
            val txnSuccess = FirestoreManager.syncTransactionsToCloud(merchantKey, transactions)

            // Also sync under merchant UPI if available and different from uid
            if (profile.upiId.isNotBlank() && profile.upiId != merchantKey) {
                FirestoreManager.syncAllKhataToCloud(profile.upiId, customers, khataEntries)
                FirestoreManager.syncCatalogList(profile.upiId, products)
                FirestoreManager.syncTransactionsToCloud(profile.upiId, transactions)
            }

            val now = System.currentTimeMillis()
            prefs.recordCloudSyncSuccess(now)

            Log.i(TAG, "Background sync completed successfully! ($customers customers, ${products.size} items, ${transactions.size} txns) at $now")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error during background Firestore sync: ${e.message}", e)
            Result.retry()
        }
    }

    companion object {
        const val TAG = "BackgroundSyncWorker"
        const val UNIQUE_ONE_TIME_WORK = "vyapar_background_sync_work"
        const val UNIQUE_PERIODIC_WORK = "vyapar_periodic_cloud_sync_work"

        /**
         * Enqueues an immediate background sync when the app enters the background (onStop).
         * Will only execute when network connectivity is available.
         */
        fun enqueueBackgroundSync(context: Context) {
            try {
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

                val workRequest = OneTimeWorkRequestBuilder<BackgroundSyncWorker>()
                    .setConstraints(constraints)
                    .build()

                WorkManager.getInstance(context).enqueueUniqueWork(
                    UNIQUE_ONE_TIME_WORK,
                    ExistingWorkPolicy.REPLACE,
                    workRequest
                )
                Log.d(TAG, "Enqueued OneTime background sync on app background transition")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to enqueue background sync: ${e.message}")
            }
        }

        /**
         * Schedules periodic background sync (e.g., every 6 hours) if authorized in settings.
         */
        fun schedulePeriodicSync(context: Context, intervalHours: Long = 6) {
            try {
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

                val periodicRequest = PeriodicWorkRequestBuilder<BackgroundSyncWorker>(
                    intervalHours, TimeUnit.HOURS
                )
                    .setConstraints(constraints)
                    .build()

                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    UNIQUE_PERIODIC_WORK,
                    ExistingPeriodicWorkPolicy.KEEP,
                    periodicRequest
                )
                Log.d(TAG, "Scheduled periodic background sync (every $intervalHours hours)")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to schedule periodic sync: ${e.message}")
            }
        }

        fun cancelPeriodicSync(context: Context) {
            try {
                WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_PERIODIC_WORK)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to cancel periodic sync: ${e.message}")
            }
        }
    }
}
