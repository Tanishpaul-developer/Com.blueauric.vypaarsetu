package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.MainActivity
import com.example.data.local.AppDatabase
import kotlinx.coroutines.flow.firstOrNull
import java.util.Calendar
import java.util.concurrent.TimeUnit

class DailyClosingWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val TAG = "DailyClosingWorker"
        const val CHANNEL_ID = "vyaparsetu_daily_closing_channel"
        const val CHANNEL_NAME = "Shop Closing Ritual"
        const val NOTIFICATION_ID = 2026
        const val WORK_NAME = "daily_closing_ritual_work"

        fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Daily 8:30 PM shop closing collection celebration notification"
                    enableVibration(true)
                }
                val manager = context.getSystemService(NotificationManager::class.java)
                manager?.createNotificationChannel(channel)
            }
        }

        fun showNotificationNow(context: Context) {
            createNotificationChannel(context)
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("NAV_SCREEN", "DAILY_SUMMARY")
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                NOTIFICATION_ID,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Shop Closing Time! 📊")
                .setContentText("Tap to see today's cash & UPI collection summary!")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()
            try {
                NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
            } catch (_: SecurityException) {}
        }
        fun scheduleDailyClosing(context: Context) {
            try {
                val now = Calendar.getInstance()
                val target = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 20)
                    set(Calendar.MINUTE, 30)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                    if (before(now)) {
                        add(Calendar.DAY_OF_YEAR, 1)
                    }
                }
                val initialDelay = target.timeInMillis - now.timeInMillis

                val workRequest = PeriodicWorkRequestBuilder<DailyClosingWorker>(24, TimeUnit.HOURS)
                    .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                    .build()

                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    workRequest
                )
                Log.d(TAG, "Daily Closing Ritual scheduled with initial delay of ${initialDelay / 1000 / 60} minutes")
            } catch (e: Exception) {
                Log.e(TAG, "Error scheduling daily closing work", e)
            }
        }
    }

    override suspend fun doWork(): Result {
        return try {
            createNotificationChannel(context)

            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startOfDay = cal.timeInMillis

            val db = AppDatabase.getInstance(context)
            val todayTotal = db.transactionDao().getTodayCreditTotal(startOfDay).firstOrNull() ?: 0.0
            val todayCash = db.transactionDao().getTodayCashTotal(startOfDay).firstOrNull() ?: 0.0
            val todayUpi = db.transactionDao().getTodayUpiTotal(startOfDay).firstOrNull() ?: 0.0
            val todayCount = db.transactionDao().getTodayTransactionCount(startOfDay).firstOrNull() ?: 0

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("NAV_SCREEN", "DAILY_SUMMARY")
                putExtra("EXTRA_TODAY_TOTAL", todayTotal)
                putExtra("EXTRA_TODAY_CASH", todayCash)
                putExtra("EXTRA_TODAY_UPI", todayUpi)
                putExtra("EXTRA_TODAY_COUNT", todayCount)
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                NOTIFICATION_ID,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Shop Closing Time! 📊")
                .setContentText("Tap to see today's cash & UPI collection summary!")
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText("Shop Closing Time! 📊\nToday's Total: ₹${todayTotal.toInt()} ($todayCount transactions). Tap to see cash vs UPI collection summary & hear announcements!")
                )
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

            try {
                NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
            } catch (e: SecurityException) {
                Log.w(TAG, "POST_NOTIFICATIONS permission not granted yet: ${e.message}")
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error executing DailyClosingWorker", e)
            Result.retry()
        }
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Daily 8:30 PM shop closing collection celebration notification"
                enableVibration(true)
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }
}
