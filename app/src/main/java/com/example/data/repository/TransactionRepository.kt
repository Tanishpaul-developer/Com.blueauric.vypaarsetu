package com.example.data.repository

import com.example.data.local.TransactionDao
import com.example.data.local.TransactionEntity
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class TransactionRepository(private val transactionDao: TransactionDao) {

    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()
    val recentTransactions: Flow<List<TransactionEntity>> = transactionDao.getRecentTransactions(20)

    private fun getStartOfDay(): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }

    val todayCreditTotal: Flow<Double?> = transactionDao.getTodayCreditTotal(getStartOfDay())
    val todayUpiTotal: Flow<Double?> = transactionDao.getTodayUpiTotal(getStartOfDay())
    val todayCashTotal: Flow<Double?> = transactionDao.getTodayCashTotal(getStartOfDay())
    val todayTransactionCount: Flow<Int> = transactionDao.getTodayTransactionCount(getStartOfDay())

    suspend fun recordTransaction(transaction: TransactionEntity): Long {
        return transactionDao.insertTransaction(transaction)
    }

    suspend fun insertAll(list: List<TransactionEntity>) {
        transactionDao.insertAll(list)
    }

    suspend fun getAllTransactionsList(): List<TransactionEntity> {
        return transactionDao.getAllTransactionsList()
    }
}
