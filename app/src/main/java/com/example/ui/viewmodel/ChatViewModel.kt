package com.example.ui.viewmodel

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.GeminiClient
import com.example.ai.StoreSnapshot
import com.example.data.local.AppDatabase
import com.example.data.model.ChatMessage
import com.example.data.model.MerchantProfile
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.InventoryRepository
import com.example.data.repository.KhataRepository
import com.example.data.repository.TransactionRepository
import com.example.service.SoundboxSpeaker
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Filter categories for merchant chat queries.
 */
enum class ChatCategory(val titleHi: String, val titleEn: String) {
    ALL("सभी", "All"),
    FINANCIAL("वित्तीय व बिक्री", "Financial & Sales"),
    CUSTOMER_SUPPORT("ग्राहक सहायता", "Customer Support"),
    KHATA_RECOVERY("उधार वसूली", "Khata Recovery"),
    GST_BILLING("जीएसटी व बिल", "GST & Billing")
}

/**
 * Quick contextual prompt chip.
 */
data class QuickChatPrompt(
    val id: String,
    val label: String,
    val prompt: String,
    val category: ChatCategory
)

/**
 * UI State for the Firebase AI Chat Assistant.
 */
data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val selectedLanguage: String = "hi",
    val selectedCategory: ChatCategory = ChatCategory.ALL,
    val storeSnapshot: StoreSnapshot? = null,
    val speakingMessageId: String? = null,
    val error: String? = null
)

/**
 * ChatViewModel powered by Firebase AI (Gemini 2.5 Flash).
 * Enables real-time multilingual customer support, customer dispute handling,
 * and live business financial queries for the merchant.
 */
class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val TAG = "ChatViewModel"

    private val db = AppDatabase.getInstance(application)
    val preferencesRepository = UserPreferencesRepository(application)
    val transactionRepository = TransactionRepository(db.transactionDao())
    val khataRepository = KhataRepository(db.customerDao())
    val inventoryRepository = InventoryRepository(db.productDao())

    val merchantProfile: StateFlow<MerchantProfile> = preferencesRepository.merchantProfileFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, MerchantProfile())

    // Firebase AI Generative Model instance (Gemini 2.5 Flash)
    private val firebaseAiModel by lazy {
        try {
            Firebase.ai.generativeModel(modelName = "gemini-2.5-flash")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to initialize Firebase AI GenerativeModel, will use fallback engine", e)
            null
        }
    }

    // Stateful Firebase AI Chat session
    private var firebaseChatSession: com.google.firebase.ai.Chat? = null

    private val _uiState = MutableStateFlow(
        ChatUiState(
            messages = listOf(
                ChatMessage(
                    text = "नमस्ते! मैं आपका VyaparSetu AI वित्तीय व ग्राहक सहायता सलाहकार हूँ। आज की बिक्री, उधार हिसाब, या ग्राहकों के लिए संदेश तैयार करने हेतु मुझसे कुछ भी पूछें।",
                    isUser = false,
                    category = "GENERAL",
                    suggestedFollowUps = listOf(
                        "आज की कुल बिक्री और UPI?",
                        "उधार वसूली के लिए विनम्र संदेश",
                        "सबसे ज्यादा उधार किसका बाकी है?",
                        "ग्राहक का UPI पेंडिंग विवाद कैसे सुलझाएं?"
                    )
                )
            ),
            selectedLanguage = "hi"
        )
    )
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        // Initialize chat session and language from merchant profile
        viewModelScope.launch {
            val profile = merchantProfile.first()
            _uiState.value = _uiState.value.copy(
                selectedLanguage = profile.languageCode.ifBlank { "hi" }
            )
            refreshStoreSnapshot()
            initFirebaseChatSession()
        }
    }

    private fun initFirebaseChatSession() {
        try {
            firebaseChatSession = firebaseAiModel?.startChat()
            Log.d(TAG, "Firebase AI Chat session successfully initialized")
        } catch (e: Exception) {
            Log.w(TAG, "Could not start Firebase AI Chat session, fallback will be used", e)
            firebaseChatSession = null
        }
    }

    /**
     * Refreshes the real-time financial snapshot from Room DB.
     */
    suspend fun refreshStoreSnapshot(): StoreSnapshot = withContext(Dispatchers.IO) {
        val profile = merchantProfile.value
        val todayCredit = transactionRepository.todayCreditTotal.first() ?: 0.0
        val todayUpi = transactionRepository.todayUpiTotal.first() ?: 0.0
        val todayCash = transactionRepository.todayCashTotal.first() ?: 0.0
        val todayCount = transactionRepository.todayTransactionCount.first() ?: 0

        val customers = khataRepository.allCustomers.first()
        val totalUdhar = customers.sumOf { it.balanceDue }
        val topDebtors = customers.filter { it.balanceDue > 0 }
            .sortedByDescending { it.balanceDue }
            .take(5)
            .map { it.name to it.balanceDue }

        val lowProducts = inventoryRepository.lowStockProducts.first().map { it.name }

        val snapshot = StoreSnapshot(
            shopName = profile.shopName.ifBlank { "VyaparSetu Store" },
            ownerName = profile.ownerName.ifBlank { "व्यापारी" },
            todaySalesTotal = todayCredit,
            todayUpiTotal = todayUpi,
            todayCashTotal = todayCash,
            todayTxnCount = todayCount,
            totalUdharDue = totalUdhar,
            topDebtors = topDebtors,
            lowStockCount = lowProducts.size,
            lowStockItems = lowProducts
        )

        _uiState.value = _uiState.value.copy(storeSnapshot = snapshot)
        snapshot
    }

    /**
     * Sends a message to the Firebase AI model with real-time financial context.
     */
    fun sendMessage(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank() || _uiState.value.isLoading) return

        val userMessage = ChatMessage(
            text = trimmed,
            isUser = true,
            languageCode = _uiState.value.selectedLanguage
        )

        _uiState.value = _uiState.value.copy(
            messages = _uiState.value.messages + userMessage,
            isLoading = true,
            error = null
        )

        viewModelScope.launch {
            try {
                val snapshot = refreshStoreSnapshot()
                val profile = merchantProfile.value
                val lang = _uiState.value.selectedLanguage

                val replyText = generateAiResponse(
                    userQuery = trimmed,
                    language = lang,
                    snapshot = snapshot,
                    profile = profile
                )

                val detectedCategory = detectCategory(trimmed)
                val followUps = generateFollowUps(detectedCategory, lang)

                val aiMessage = ChatMessage(
                    text = replyText,
                    isUser = false,
                    category = detectedCategory.name,
                    languageCode = lang,
                    suggestedFollowUps = followUps
                )

                _uiState.value = _uiState.value.copy(
                    messages = _uiState.value.messages + aiMessage,
                    isLoading = false
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error in sendMessage", e)
                val fallbackReply = generateFallbackResponse(trimmed, _uiState.value.selectedLanguage)
                val aiMessage = ChatMessage(
                    text = fallbackReply,
                    isUser = false,
                    category = detectCategory(trimmed).name,
                    languageCode = _uiState.value.selectedLanguage
                )
                _uiState.value = _uiState.value.copy(
                    messages = _uiState.value.messages + aiMessage,
                    isLoading = false
                )
            }
        }
    }

    /**
     * Executes AI generation using Firebase AI primary engine with graceful fallback.
     */
    private suspend fun generateAiResponse(
        userQuery: String,
        language: String,
        snapshot: StoreSnapshot,
        profile: MerchantProfile
    ): String = withContext(Dispatchers.IO) {
        // Construct system context with live financial data
        val systemContext = buildFinancialAndSupportContext(snapshot, profile, language)
        val promptWithContext = """
            $systemContext

            User Question: $userQuery
        """.trimIndent()

        // 1. Try Firebase AI Chat Session
        if (firebaseChatSession != null) {
            try {
                Log.d(TAG, "Querying Firebase AI chat session...")
                val response = firebaseChatSession?.sendMessage(promptWithContext)
                val text = response?.text
                if (!text.isNullOrBlank()) {
                    return@withContext text.trim()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Firebase AI session error, trying direct generative model or fallback: ${e.message}")
            }
        }

        // 2. Try starting a fresh Firebase AI Chat session if previous was null or expired
        if (firebaseAiModel != null) {
            try {
                val freshChat = firebaseAiModel?.startChat()
                val response = freshChat?.sendMessage(promptWithContext)
                val text = response?.text
                if (!text.isNullOrBlank()) {
                    firebaseChatSession = freshChat
                    return@withContext text.trim()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Firebase AI fresh chat error: ${e.message}")
            }
        }

        // 3. Fallback to GeminiClient / Offline Intelligence Engine
        Log.i(TAG, "Using integrated fallback intelligence engine")
        val history = _uiState.value.messages.map { it.text to it.isUser }
        return@withContext GeminiClient.chatWithGemini(
            history = history,
            merchantLanguage = language,
            shopName = snapshot.shopName,
            userQuery = userQuery,
            storeSnapshot = snapshot
        )
    }

    /**
     * Builds contextual prompt containing real-time store financial metrics
     * and multilingual customer support instructions.
     */
    private fun buildFinancialAndSupportContext(
        snapshot: StoreSnapshot,
        profile: MerchantProfile,
        language: String
    ): String {
        val debtorsFormatted = if (snapshot.topDebtors.isNotEmpty()) {
            snapshot.topDebtors.joinToString("; ") { "${it.first}: ₹${String.format(java.util.Locale.US, "%.2f", it.second)}" }
        } else {
            "None / All clear"
        }

        val lowStockFormatted = if (snapshot.lowStockItems.isNotEmpty()) {
            snapshot.lowStockItems.joinToString(", ")
        } else {
            "All items well stocked"
        }

        val langName = when (language.lowercase()) {
            "hi" -> "Hindi (हिन्दी)"
            "bn" -> "Bengali (বাংলা)"
            "mr" -> "Marathi (मराठी)"
            "gu" -> "Gujarati (ગુજરાતી)"
            "ta" -> "Tamil (தமிழ்)"
            "te" -> "Telugu (తెలుగు)"
            "pa" -> "Punjabi (ਪੰਜਾਬੀ)"
            else -> "English"
        }

        return """
            [SYSTEM INSTRUCTION: VYAPARSETU INTELLIGENT MERCHANT ADVISOR & MULTILINGUAL SUPPORT SPECIALIST]
            You are VyaparSetu AI, an expert retail commerce advisor and customer support specialist built for Indian merchants.
            
            MERCHANT IDENTITY:
            - Store Name: ${snapshot.shopName}
            - Owner Name: ${snapshot.ownerName}
            - Target Response Language: $langName
            
            LIVE REAL-TIME STORE FINANCIAL SNAPSHOT:
            - Today's Total Sales: ₹${String.format(java.util.Locale.US, "%.2f", snapshot.todaySalesTotal)}
            - Today's UPI Collections: ₹${String.format(java.util.Locale.US, "%.2f", snapshot.todayUpiTotal)}
            - Today's Cash Collections: ₹${String.format(java.util.Locale.US, "%.2f", snapshot.todayCashTotal)}
            - Today's Completed Sales Count: ${snapshot.todayTxnCount}
            - Total Outstanding Customer Udhar (Credit Due): ₹${String.format(java.util.Locale.US, "%.2f", snapshot.totalUdharDue)}
            - Top Pending Debtors: $debtorsFormatted
            - Low Stock Products Count: ${snapshot.lowStockCount} (${lowStockFormatted})
            
            CORE CAPABILITIES:
            1. FINANCIAL QUERIES:
               - Answer any questions about today's sales, UPI vs Cash split, highest debtor, total pending credit, and inventory status with 100% accuracy using the live snapshot numbers above.
               - Offer practical tips on improving cash flow, settling outstanding debt, and inventory turnover.
            2. MULTILINGUAL CUSTOMER SUPPORT:
               - Help the merchant handle customer disputes (e.g. pending UPI payment verification, failed transactions, return/replacement requests).
               - Provide ready-to-copy polite customer communication templates (WhatsApp reminders, invoice notes, festive discounts) in $langName.
               - Guide on GST invoice creation, HSN code tax slabs (0%, 5%, 12%, 18%, 28%), and Bluetooth thermal receipt printing.
            
            STRICT GUIDELINES:
            - Always respond in $langName.
            - Keep responses clear, polite, and actionable for busy shop owners.
            - When generating a customer message template, format it clearly so the merchant can copy-paste it directly to WhatsApp.
        """.trimIndent()
    }

    private fun detectCategory(query: String): ChatCategory {
        val q = query.lowercase()
        return when {
            q.contains("sale") || q.contains("बिक्री") || q.contains("earn") || q.contains("कमाई") ||
                q.contains("cash") || q.contains("upi") || q.contains("profit") || q.contains("मुनाफा") ||
                q.contains("वित्तीय") || q.contains("balance") -> ChatCategory.FINANCIAL

            q.contains("उधार") || q.contains("udhar") || q.contains("credit") || q.contains("due") ||
                q.contains("debt") || q.contains("वसूली") || q.contains("reminder") || q.contains("बाकी") -> ChatCategory.KHATA_RECOVERY

            q.contains("gst") || q.contains("invoice") || q.contains("bill") || q.contains("बिल") ||
                q.contains("रसीद") || q.contains("tax") || q.contains("टैक्स") -> ChatCategory.GST_BILLING

            q.contains("customer") || q.contains("ग्राहक") || q.contains("dispute") || q.contains("विवाद") ||
                q.contains("refund") || q.contains("वापसी") || q.contains("message") || q.contains("मैसेज") -> ChatCategory.CUSTOMER_SUPPORT

            else -> ChatCategory.ALL
        }
    }

    private fun generateFollowUps(category: ChatCategory, lang: String): List<String> {
        val isHindi = lang == "hi"
        return when (category) {
            ChatCategory.FINANCIAL -> if (isHindi) {
                listOf("आज का UPI और कैश प्रतिशत?", "कौन सा सामान खत्म हो रहा है?", "मुनाफा कैसे बढ़ाएं?")
            } else {
                listOf("UPI vs Cash percentage today?", "Which items are low in stock?", "How to increase margins?")
            }
            ChatCategory.KHATA_RECOVERY -> if (isHindi) {
                listOf("उधार वसूली का कड़ा संदेश", "त्योहारी ऑफर के साथ पेमेंट याद दिलाएं", "ग्राहकों का ट्रस्ट स्कोर कैसे देखें?")
            } else {
                listOf("Firm payment reminder message", "Festive payment reminder", "How to check customer trust scores?")
            }
            ChatCategory.CUSTOMER_SUPPORT -> if (isHindi) {
                listOf("ग्राहकों को धन्यवाद मैसेज", "दुकान की रिटर्न पॉलिसी क्या रखें?", "UPI पेंडिंग स्टेटस चेक गाइड")
            } else {
                listOf("Thank you message for customers", "Standard store return policy", "Pending UPI troubleshooting guide")
            }
            ChatCategory.GST_BILLING -> if (isHindi) {
                listOf("18% GST बिल कैसे बनाएं?", "ब्लूटूथ प्रिंटर से रसीद प्रिंट", "बिना GST वाला बिल")
            } else {
                listOf("How to make 18% GST bill?", "Bluetooth thermal print guide", "Non-GST cash memo format")
            }
            ChatCategory.ALL -> if (isHindi) {
                listOf("आज की कुल कमाई?", "उधार वसूली संदेश", "सामान का नया ऑर्डर बनाएं")
            } else {
                listOf("Today's total earnings?", "Payment reminder template", "Create distributor restock order")
            }
        }
    }

    private fun generateFallbackResponse(query: String, lang: String): String {
        val snapshot = _uiState.value.storeSnapshot ?: StoreSnapshot()
        val isHindi = lang == "hi"
        val q = query.lowercase()

        return when {
            q.contains("sale") || q.contains("बिक्री") || q.contains("कमाई") || q.contains("today") -> {
                if (isHindi) {
                    "आज आपके स्टोर '${snapshot.shopName}' में कुल ₹${String.format(java.util.Locale.US, "%.2f", snapshot.todaySalesTotal)} की बिक्री दर्ज हुई है। इसमें UPI से ₹${String.format(java.util.Locale.US, "%.2f", snapshot.todayUpiTotal)} और नकद ₹${String.format(java.util.Locale.US, "%.2f", snapshot.todayCashTotal)} प्राप्त हुए हैं।"
                } else {
                    "Today at '${snapshot.shopName}', total sales reached ₹${String.format(java.util.Locale.US, "%.2f", snapshot.todaySalesTotal)} (UPI: ₹${String.format(java.util.Locale.US, "%.2f", snapshot.todayUpiTotal)}, Cash: ₹${String.format(java.util.Locale.US, "%.2f", snapshot.todayCashTotal)})."
                }
            }
            q.contains("उधार") || q.contains("udhar") || q.contains("reminder") || q.contains("मैसेज") -> {
                if (isHindi) {
                    "नमस्ते! आपके बकाया भुगतान के लिए विनम्र WhatsApp संदेश:\n\n'नमस्ते जी! ${snapshot.shopName} से आपका कुल बकाया ₹... है। कृपया सुविधा अनुसार UPI द्वारा भुगतान करें। धन्यवाद!'\n\n(इसे कॉपी करके ग्राहक को भेजें)"
                } else {
                    "Polite Payment Reminder Template:\n\n'Dear customer, this is a gentle reminder from ${snapshot.shopName} regarding your outstanding balance of ₹... Please pay via UPI at your convenience. Thank you!'\n\n(You can copy and send via WhatsApp)"
                }
            }
            else -> {
                if (isHindi) {
                    "नमस्ते! मैं आपका VyaparSetu AI सहायक हूँ। आप मुझसे आज की बिक्री, UPI व नकद हिसाब, उधार वसूली संदेश, या GST बिलिंग के बारे में पूछ सकते हैं।"
                } else {
                    "Hello! I am your VyaparSetu AI Assistant. Feel free to ask about today's sales, UPI/Cash split, customer payment reminders, or GST invoice creation."
                }
            }
        }
    }

    /**
     * Changes the active conversation language and updates prompt guidance.
     */
    fun setLanguage(langCode: String) {
        _uiState.value = _uiState.value.copy(selectedLanguage = langCode)
    }

    /**
     * Filters quick prompts by category.
     */
    fun selectCategory(category: ChatCategory) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
    }

    /**
     * Clears all messages and reinitializes chat.
     */
    fun clearChat() {
        initFirebaseChatSession()
        _uiState.value = _uiState.value.copy(
            messages = listOf(
                ChatMessage(
                    text = if (_uiState.value.selectedLanguage == "en") {
                        "Hello! I am your VyaparSetu AI Financial & Customer Support Assistant. How can I assist your business today?"
                    } else {
                        "नमस्ते! मैं आपका VyaparSetu AI वित्तीय व ग्राहक सहायता सलाहकार हूँ। आज आपके व्यापार में मैं क्या सहायता करूँ?"
                    },
                    isUser = false,
                    category = "GENERAL"
                )
            ),
            error = null
        )
    }

    /**
     * Speaks the assistant response via SoundboxSpeaker for hands-free listening.
     */
    fun speakMessage(message: ChatMessage) {
        if (_uiState.value.speakingMessageId == message.id) {
            SoundboxSpeaker.stopSpeaking()
            _uiState.value = _uiState.value.copy(speakingMessageId = null)
        } else {
            _uiState.value = _uiState.value.copy(speakingMessageId = message.id)
            SoundboxSpeaker.speakText(
                context = getApplication(),
                text = message.text,
                languageCode = message.languageCode.ifBlank { _uiState.value.selectedLanguage }
            )
        }
    }

    /**
     * Copies message text to the Android system clipboard.
     */
    fun copyToClipboard(context: Context, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("VyaparSetu AI", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(
            context,
            if (_uiState.value.selectedLanguage == "en") "Copied to clipboard" else "क्लिपबोर्ड में कॉपी किया गया",
            Toast.LENGTH_SHORT
        ).show()
    }

    /**
     * Returns curated quick prompts for the active category and language.
     */
    fun getQuickPrompts(): List<QuickChatPrompt> {
        val isHindi = _uiState.value.selectedLanguage == "hi"
        val prompts = listOf(
            QuickChatPrompt(
                id = "sales_today",
                label = if (isHindi) "📊 आज की कुल बिक्री" else "📊 Today's Total Sales",
                prompt = if (isHindi) "आज मेरे स्टोर की कुल बिक्री और UPI vs Cash का हिसाब बताओ" else "Show me my total sales and UPI vs Cash split for today",
                category = ChatCategory.FINANCIAL
            ),
            QuickChatPrompt(
                id = "top_debtors",
                label = if (isHindi) "👥 सबसे बड़ा बकायादार कौन?" else "👥 Highest Debtors?",
                prompt = if (isHindi) "दुकान में सबसे ज्यादा उधार किस ग्राहक पर बाकी है और कुल बकाया कितना है?" else "Who owes the most debt and what is the total outstanding credit?",
                category = ChatCategory.KHATA_RECOVERY
            ),
            QuickChatPrompt(
                id = "whatsapp_reminder",
                label = if (isHindi) "💬 उधार वसूली संदेश" else "💬 WhatsApp Reminder Template",
                prompt = if (isHindi) "उधार वसूली के लिए एक विनम्र और प्रभावी WhatsApp संदेश का ड्राफ्ट बनाओ" else "Draft a polite and effective WhatsApp payment reminder template for customers",
                category = ChatCategory.CUSTOMER_SUPPORT
            ),
            QuickChatPrompt(
                id = "dispute_upi",
                label = if (isHindi) "⚠️ UPI पेंडिंग विवाद समाधान" else "⚠️ UPI Payment Dispute Guide",
                prompt = if (isHindi) "ग्राहक के फोन से पैसे कट गए पर मेरे खाते में नहीं आए, इस स्थिति को कैसे संभालें?" else "Customer money got debited but not credited to merchant account, how to handle this dispute?",
                category = ChatCategory.CUSTOMER_SUPPORT
            ),
            QuickChatPrompt(
                id = "low_stock",
                label = if (isHindi) "📦 कम स्टॉक सामान" else "📦 Low Stock Alerts",
                prompt = if (isHindi) "मेरी दुकान में कौन सा सामान खत्म होने वाला है और मुझे क्या आर्डर करना चाहिए?" else "Which items are running low on stock and what should I reorder?",
                category = ChatCategory.FINANCIAL
            ),
            QuickChatPrompt(
                id = "gst_calc",
                label = if (isHindi) "🧾 18% GST बिल कैसे बनाएं?" else "🧾 18% GST Invoicing Guide",
                prompt = if (isHindi) "18% GST के साथ ₹1000 के सामान का CGST और SGST बिल कैसे बनेगा?" else "How to calculate CGST and SGST for ₹1000 item at 18% GST slab?",
                category = ChatCategory.GST_BILLING
            ),
            QuickChatPrompt(
                id = "winback_offer",
                label = if (isHindi) "🎁 ग्राहकों के लिए डिस्काउंट मैसेज" else "🎁 Customer Winback Offer",
                prompt = if (isHindi) "पुराने ग्राहकों को वापस बुलाने के लिए एक आकर्षक डिस्काउंट ऑफर मैसेज बनाओ" else "Create an attractive discount winback message for old customers",
                category = ChatCategory.CUSTOMER_SUPPORT
            )
        )

        val cat = _uiState.value.selectedCategory
        return if (cat == ChatCategory.ALL) prompts else prompts.filter { it.category == cat }
    }
}
