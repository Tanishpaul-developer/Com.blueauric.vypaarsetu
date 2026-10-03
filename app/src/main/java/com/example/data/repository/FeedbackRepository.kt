package com.example.data.repository

import com.example.data.firebase.FirestoreManager
import com.example.data.local.FeedbackDao
import com.example.data.local.FeedbackEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class FeedbackRepository(private val feedbackDao: FeedbackDao) {

    val allFeedbacks: Flow<List<FeedbackEntity>> = feedbackDao.getAllFeedbacks()

    suspend fun submitFeedback(feedback: FeedbackEntity): Long {
        val id = feedbackDao.insertFeedback(feedback)
        val created = feedback.copy(id = id)
        // Fire-and-forget sync to Firestore
        FirestoreManager.submitFeedbackToCloud(created)
        return id
    }

    suspend fun upvote(feedbackId: Long) {
        feedbackDao.upvoteFeedback(feedbackId)
    }

    suspend fun seedInitialCommunityForumIfEmpty() {
        val current = feedbackDao.getAllFeedbacks().first()
        if (current.isEmpty()) {
            val now = System.currentTimeMillis()
            val day = 86400000L
            val seeds = listOf(
                FeedbackEntity(
                    authorName = "Ramesh Sharma (Kirana Store, Jaipur)",
                    authorEmail = "ramesh.sharma@kirana.in",
                    category = "UI/UX",
                    rating = 5,
                    title = "Auto-Split QR saved ₹850 in MDR fees this week!",
                    message = "The feature where bills over ₹2000 are split into two zero-fee UPI QR codes works seamlessly. Customers scan both quickly and I don't lose money on merchant discount rates. Great work!",
                    timestamp = now - (day * 3),
                    upvotes = 14,
                    developerReply = "Thank you so much Ramesh ji! We built the auto-split algorithm specifically to protect small retail merchants from MDR charges. More automated financial features coming soon.",
                    replyAuthor = "Tanish Paul (Lead Developer)",
                    replyTimestamp = now - (day * 3) + 3600000L,
                    syncStatus = "SYNCED"
                ),
                FeedbackEntity(
                    authorName = "Pooja Verma (Dairy & Sweets, Indore)",
                    authorEmail = "pooja.dairy@gmail.com",
                    category = "Billing & Soundbox",
                    rating = 5,
                    title = "Soundbox announcement voice in Hindi is very loud and clear",
                    message = "I tested with PhonePe and Paytm payments. The audio prompt speaks immediately without delay even on 4G network. Can we have option for thermal printer receipts in 3-inch format?",
                    timestamp = now - (day * 2),
                    upvotes = 9,
                    developerReply = "Hi Pooja! 3-inch (80mm) thermal printing is fully supported in the 'Printer Setup' screen! Connect via Bluetooth to your ESC/POS thermal printer and select '80mm (3-Inch)'. Enjoy!",
                    replyAuthor = "Tanish Paul (Lead Developer)",
                    replyTimestamp = now - (day * 2) + 7200000L,
                    syncStatus = "SYNCED"
                ),
                FeedbackEntity(
                    authorName = "Sunil Gupta (Hardware Store, Lucknow)",
                    authorEmail = "gupta.hardware@outlook.com",
                    category = "Feature Request",
                    rating = 5,
                    title = "Customer Trust Score helped avoid bad credit debt",
                    message = "The green/yellow/red risk badges on Khata ledger cards are super helpful. Now I know exactly who repays within 7 days and who takes over 30 days before giving udhar.",
                    timestamp = now - day,
                    upvotes = 11,
                    developerReply = "Delighted to hear that Sunil ji! The credit risk predictor calculates repayment speed automatically in the background without any manual input required from you.",
                    replyAuthor = "Tanish Paul (Lead Developer)",
                    replyTimestamp = now - day + 1800000L,
                    syncStatus = "SYNCED"
                )
            )
            feedbackDao.insertAll(seeds)
        }
    }
}
