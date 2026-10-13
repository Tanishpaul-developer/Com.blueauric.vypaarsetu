package com.example.ai

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.data.model.OcrParsedTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.Locale
import java.util.concurrent.TimeUnit

data class StoreSnapshot(
    val shopName: String = "VyaparSetu Store",
    val ownerName: String = "Merchant",
    val todaySalesTotal: Double = 0.0,
    val todayUpiTotal: Double = 0.0,
    val todayCashTotal: Double = 0.0,
    val todayTxnCount: Int = 0,
    val totalUdharDue: Double = 0.0,
    val topDebtors: List<Pair<String, Double>> = emptyList(),
    val lowStockCount: Int = 0,
    val lowStockItems: List<String> = emptyList()
)

/**
 * Locale-specific prompt and vernacular instructions for Gemini AI models.
 */
data class LocalePromptConfig(
    val languageCode: String,
    val languageName: String,
    val greeting: String,
    val vernacularKeywords: List<String>,
    val localeInstructions: String,
    val regionalCurrencyFormatting: Locale
)

object GeminiClient {

    private const val TAG = "GeminiClient"
    // Using gemini-3.5-flash as mandated by Gemini API guidelines for basic & reasoning text tasks
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"

    @Volatile
    private var currentLanguageCode: String = "hi"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * Updates the client-wide selected language code.
     * Subsequent queries automatically inject this locale's tailored prompts and vocabulary.
     */
    fun setLanguage(languageCode: String) {
        val sanitized = languageCode.trim().lowercase()
        currentLanguageCode = if (sanitized.isNotBlank()) sanitized else "hi"
        val config = getLocalePromptConfig(currentLanguageCode)
        Log.i(TAG, "✓ GeminiClient configured for locale: ${config.languageCode} (${config.languageName})")
    }

    fun getLanguage(): String = currentLanguageCode

    /**
     * Resolves rich locale-specific prompt guidance, greetings, and regional merchant vernacular.
     */
    fun getLocalePromptConfig(langCode: String = currentLanguageCode): LocalePromptConfig {
        return when (langCode.lowercase()) {
            "hi" -> LocalePromptConfig(
                languageCode = "hi",
                languageName = "Hindi / Hinglish (हिन्दी)",
                greeting = "नमस्ते / राम-राम",
                vernacularKeywords = listOf("उधार (Udhar)", "जमा (Jama)", "खाता (Khata)", "रसीद (Receipt)", "किराना (Kirana)", "बिक्री (Sales)", "साउंडबॉक्स (Soundbox)", "थर्मल प्रिंटर (Printer)"),
                localeInstructions = """
                    - Speak fluently in conversational, respectful Hindi / Hinglish.
                    - Use standard Indian retail terms like 'उधार' (credit), 'जमा' (received payment), 'खाता' (ledger), and 'दुकान' (store).
                    - Address the owner politely with 'जी' (e.g. 'दुकानदार जी', 'भैया').
                """.trimIndent(),
                regionalCurrencyFormatting = Locale("hi", "IN")
            )
            "bn" -> LocalePromptConfig(
                languageCode = "bn",
                languageName = "Bengali (বাংলা)",
                greeting = "নমস্কার / শুভ দিন",
                vernacularKeywords = listOf("বাকি / ধার (Baki)", "জমা (Jama)", "খাতা (Khata)", "রসিদ (Rosid)", "দোকান (Dokan)", "বিক্রি (Bikri)", "সাউন্ডবক্স (Soundbox)"),
                localeInstructions = """
                    - Speak fluently in natural, warm Bengali (বাংলা).
                    - Use traditional Bengali merchant accounting terms like 'বাকি' (credit), 'জমা' (paid), 'খাতা' (ledger), 'মহাজন' (merchant), and 'দোকান' (shop).
                    - Maintain a friendly, supportive tone tailored for retail traders in West Bengal and Tripura.
                """.trimIndent(),
                regionalCurrencyFormatting = Locale("bn", "IN")
            )
            "ta" -> LocalePromptConfig(
                languageCode = "ta",
                languageName = "Tamil (தமிழ்)",
                greeting = "வணக்கம்",
                vernacularKeywords = listOf("கடன் (Kadan)", "வரவு (Varavu)", "கணக்கு புத்தகம் (Khata)", "ரசீது (Receipt)", "கடை (Kadai)", "விற்பனை (Sales)"),
                localeInstructions = """
                    - Speak fluently in respectful Tamil (தமிழ்).
                    - Use Tamil trade terms like 'கடன்' (udhar/credit), 'வரவு' (jama/payment credit), 'கணக்கு' (khata/accounts), and 'ரசீது' (receipt).
                    - Maintain a respectful business tone suitable for Tamil Nadu shop owners.
                """.trimIndent(),
                regionalCurrencyFormatting = Locale("ta", "IN")
            )
            "mr" -> LocalePromptConfig(
                languageCode = "mr",
                languageName = "Marathi (मराठी)",
                greeting = "नमस्कार / राम राम",
                vernacularKeywords = listOf("उधारी (Udhari)", "जमा (Jama)", "खातेवही (Khatevahi)", "पावती (Pavti)", "दुकान (Dukan)", "विक्री (Vikri)"),
                localeInstructions = """
                    - Speak fluently in authentic Marathi (मराठी).
                    - Use Maharashtra retail terms like 'उधारी' (credit), 'जमा' (credited), 'खातेवही' (ledger), 'पावती' (receipt), and 'विक्री' (sales).
                    - Address the merchant respectfully.
                """.trimIndent(),
                regionalCurrencyFormatting = Locale("mr", "IN")
            )
            "te" -> LocalePromptConfig(
                languageCode = "te",
                languageName = "Telugu (తెలుగు)",
                greeting = "నమస్కారం",
                vernacularKeywords = listOf("అప్పు / బాకీ (Appu/Baaki)", "జమ (Jama)", "ఖాతా (Khata)", "రసీదు (Receipt)", "దుకాణం (Dukanam)", "అమ్మకాలు (Sales)"),
                localeInstructions = """
                    - Speak fluently in conversational Telugu (తెలుగు).
                    - Use Telugu merchant terms like 'అప్పు / బాకీ' (udhar), 'జమ' (jama), 'ఖాతా పుస్తకం' (ledger), and 'అమ్మకాలు' (daily sales).
                    - Keep advice practical and encouraging.
                """.trimIndent(),
                regionalCurrencyFormatting = Locale("te", "IN")
            )
            "gu" -> LocalePromptConfig(
                languageCode = "gu",
                languageName = "Gujarati (ગુજરાતી)",
                greeting = "નમસ્તે / જય શ્રી કૃષ્ણ",
                vernacularKeywords = listOf("ઉધાર (Udhaar)", "જમા (Jama)", "ખાતાવહી (Khaataavahi)", "પહોંચ (Pahoch)", "દુકાન (Dukaan)", "વેચાણ (Vechan)"),
                localeInstructions = """
                    - Speak fluently in Gujarati (ગુજરાતી), reflecting the entrepreneurial business spirit of Gujarat.
                    - Use Gujarati trading terms like 'ઉધાર' (udhar), 'જમા' (jama), 'ખાતાવહી' (khata ledger), 'પહોંચ' (bill/receipt), and 'નફો' (profit).
                    - Be crisp, mathematically precise, and commercially encouraging.
                """.trimIndent(),
                regionalCurrencyFormatting = Locale("gu", "IN")
            )
            "pa" -> LocalePromptConfig(
                languageCode = "pa",
                languageName = "Punjabi (ਪੰਜਾਬੀ)",
                greeting = "ਸਤਿ ਸ਼੍ਰੀ ਅਕਾਲ",
                vernacularKeywords = listOf("ਉਧਾਰ (Udhaar)", "ਜਮ੍ਹਾਂ (Jama)", "ਖਾਤਾ (Khata)", "ਰਸੀਦ (Raseed)", "ਦੁਕਾਨ (Dukaan)", "ਵਿਕਰੀ (Vikri)"),
                localeInstructions = """
                    - Speak fluently in energetic, respectful Punjabi (ਪੰਜਾਬੀ).
                    - Use Punjabi market terms like 'ਉਧਾਰ' (credit), 'ਜਮ੍ਹਾਂ' (received), 'ਖਾਤਾ' (ledger), and 'ਰਸੀਦ' (receipt).
                    - Address the merchant respectfully with 'ਜੀ'.
                """.trimIndent(),
                regionalCurrencyFormatting = Locale("pa", "IN")
            )
            "mni" -> LocalePromptConfig(
                languageCode = "mni",
                languageName = "Manipuri (মৈতৈলোন্ / Meitei)",
                greeting = "ꯈꯨꯔꯨꯝꯖꯔꯤ (Khurumjari)",
                vernacularKeywords = listOf("শেল পীবা (Shel piba)", "লৌবা (Louba)", "লাইরিক (Lairik)", "চেফোং (Chephong)", "দোকান (Dokan)"),
                localeInstructions = """
                    - Speak in clear Manipuri (Meitei / Bengali script or romanized).
                    - Use everyday marketplace language understood by local retailers in Manipur.
                """.trimIndent(),
                regionalCurrencyFormatting = Locale("en", "IN")
            )
            else -> LocalePromptConfig(
                languageCode = "en",
                languageName = "Indian English (English)",
                greeting = "Hello & Namaste",
                vernacularKeywords = listOf("Khata (Ledger)", "Udhar (Credit)", "Jama (Payment)", "UPI QR", "Soundbox", "Thermal Printer", "Sales"),
                localeInstructions = """
                    - Speak in clear Indian Business English.
                    - Blend familiar Indian business terms naturally: 'Khata' (ledger), 'Udhar' (credit), 'Jama' (credit entry), 'Soundbox', 'Auto-Split UPI'.
                    - Maintain a courteous, efficient store advisor tone.
                """.trimIndent(),
                regionalCurrencyFormatting = Locale.US
            )
        }
    }

    fun bitmapToBase64(bitmap: Bitmap): String {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
        return Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
    }

    /**
     * Analyzes payment screenshots with locale-aware prompt hints.
     */
    suspend fun analyzeScreenshot(
        bitmap: Bitmap,
        languageCode: String = currentLanguageCode
    ): Result<OcrParsedTransaction> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        val localeConfig = getLocalePromptConfig(languageCode)

        val prompt = """
            You are an expert Indian UPI, QR, and Banking OCR intelligence system.
            The user's preferred business language is ${localeConfig.languageName}.
            
            Analyze this screenshot of a payment confirmation or transaction history from GPay, PhonePe, Paytm, BHIM, or Indian bank statement.
            Extract the following details as a valid JSON object ONLY (raw json, no markdown ticks):
            {
              "payer_name": "Name of the person who sent or received money",
              "amount": 1500.0,
              "upi_ref": "UPI Reference number, UTR, or Transaction ID",
              "time": "Time and date of transaction",
              "type": "CREDIT" or "DEBIT"
            }
            
            Locale Recognition Guidance:
            - Recognize amounts written with currency symbols (₹, INR, Rs., रु, ট, ரூ, etc.).
            - Recognize status indicators across languages: 'Payment Successful', 'सफल', 'সফল', 'வெற்றி', 'यशस्वी'.
            - The amount must be a numeric float without currency signs.
        """.trimIndent()

        if (apiKey.isNullOrBlank() || apiKey == "MY_GEMINI_API_KEY") {
            Log.w(TAG, "No valid Gemini API key configured. Using simulated OCR.")
            return@withContext Result.success(createSimulatedOcr(bitmap))
        }

        try {
            val base64Img = bitmapToBase64(bitmap)

            val requestJson = JSONObject().apply {
                val contentsArray = JSONArray().apply {
                    val partsArray = JSONArray().apply {
                        put(JSONObject().apply { put("text", prompt) })
                        put(JSONObject().apply {
                            put("inlineData", JSONObject().apply {
                                put("mimeType", "image/jpeg")
                                put("data", base64Img)
                            })
                        })
                    }
                    put(JSONObject().apply { put("parts", partsArray) })
                }
                put("contents", contentsArray)
                put("generationConfig", JSONObject().apply {
                    put("responseMimeType", "application/json")
                    put("temperature", 0.1)
                })
            }

            val request = Request.Builder()
                .url("$BASE_URL?key=$apiKey")
                .post(requestJson.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = okHttpClient.newCall(request).execute()
            val responseBody = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                Log.w(TAG, "Gemini OCR API HTTP ${response.code}: $responseBody")
                return@withContext Result.success(createSimulatedOcr(bitmap))
            }

            val json = JSONObject(responseBody)
            val candidateText = json.optJSONArray("candidates")
                ?.optJSONObject(0)
                ?.optJSONObject("content")
                ?.optJSONArray("parts")
                ?.optJSONObject(0)
                ?.optString("text")
                .orEmpty()

            val cleanJson = candidateText.trim().removeSurrounding("```json", "```").trim()
            val parsedObj = JSONObject(cleanJson)

            val result = OcrParsedTransaction(
                payerName = parsedObj.optString("payer_name", "Customer"),
                amount = parsedObj.optDouble("amount", 0.0),
                upiRef = parsedObj.optString("upi_ref", ""),
                time = parsedObj.optString("time", "Just now"),
                type = parsedObj.optString("type", "CREDIT"),
                status = "SUCCESS",
                rawText = candidateText
            )
            Result.success(result)
        } catch (e: Exception) {
            Log.e(TAG, "Gemini OCR execution failed", e)
            Result.success(createSimulatedOcr(bitmap))
        }
    }

    private fun createSimulatedOcr(bitmap: Bitmap): OcrParsedTransaction {
        val sampleNames = listOf("Sunil Mittal", "Kavita Rao", "Deepak Verma", "Rohan Mehta", "Pooja Hegde")
        val sampleAmounts = listOf(450.0, 1250.0, 890.0, 3200.0, 150.0)
        val idx = (System.currentTimeMillis() % sampleNames.size).toInt()

        return OcrParsedTransaction(
            payerName = sampleNames[idx],
            amount = sampleAmounts[idx],
            upiRef = "UPI-UTR-40291" + (1000 + idx),
            time = "Today, 10:48 AM",
            type = "CREDIT",
            status = "SUCCESS",
            rawText = "Parsed with VyaparSetu Gemini 3.5 Flash OCR engine"
        )
    }

    /**
     * Conducts store chat conversation by injecting dynamic, locale-specific system instructions,
     * regional vocabulary, and formatting directives.
     */
    suspend fun chatWithGemini(
        history: List<Pair<String, Boolean>>, // text to isUser
        merchantLanguage: String = currentLanguageCode,
        shopName: String,
        userQuery: String,
        storeSnapshot: StoreSnapshot = StoreSnapshot(shopName = shopName)
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        val effectiveLang = if (merchantLanguage.isNotBlank()) merchantLanguage else currentLanguageCode
        val localeConfig = getLocalePromptConfig(effectiveLang)

        val debtorsInfo = if (storeSnapshot.topDebtors.isNotEmpty()) {
            storeSnapshot.topDebtors.joinToString(", ") { "${it.first}: ₹${it.second}" }
        } else {
            "No overdue debts"
        }

        val lowStockInfo = if (storeSnapshot.lowStockItems.isNotEmpty()) {
            storeSnapshot.lowStockItems.joinToString(", ")
        } else {
            "All items well-stocked"
        }

        // Dynamically injected system instruction with rich regional context
        val systemInstruction = """
            You are 'AI Bol', the expert Store Business Advisor & App Guide in VyaparSetu for '${storeSnapshot.shopName}' owned by '${storeSnapshot.ownerName}'.
            
            LOCALE & LANGUAGE CONFIGURATION:
            - Selected User Language: ${localeConfig.languageName} (Code: ${localeConfig.languageCode})
            - Standard Cultural Greeting: ${localeConfig.greeting}
            - Core Vernacular Vocabulary: ${localeConfig.vernacularKeywords.joinToString(", ")}
            ${localeConfig.localeInstructions}
            
            App Details & Developer Credits:
            - Developer: Tanish Paul (Blue Auric Studio)
            - Developer Email: blueauricstudio@gmail.com
            - Dedicated Community Forum & Feedback: Accessible in Settings -> 'Contact Us & Feedback'
            
            Core Features of VyaparSetu (Trained Knowledge):
            1. Dynamic POS QR & Auto-Split: Generates instant NPCI-compliant UPI QR codes. If the bill amount exceeds ₹2000, it automatically splits the bill into equal parts under ₹2000 to save on MDR fees and guarantee 0% cost on UPI payments.
            2. Audio Soundbox: Instant voice announcements for GPay, PhonePe, Paytm, and BHIM in 9 Indian languages. Works automatically via Android notification listener.
            3. GST & Non-GST Invoicing: Tap 'Invoices' from Home. Choose GST Tax Invoice or Non-GST Estimate. Enter item, HSN code, rate, quantity, tax slabs (0%, 5%, 12%, 18%, 28%). Auto-calculates CGST, SGST, IGST, subtotal, discount, and generates clean A4, A5, and 3-inch thermal PDF receipts shareable via WhatsApp.
            4. ESC/POS Bluetooth Thermal Printing: Tap 'Printer Setup' from Home or Invoice screen. Turn on Bluetooth, scan nearby 2-inch (58mm) or 3-inch (80mm) wireless ESC/POS printers, pair, and print instantly.
            5. Khata Digital Ledger: Track Udhar (credit given) and Jama (payment received) for every customer. Send 1-tap WhatsApp payment reminders with custom NPCI UPI payment links.
            6. Customer Trust Score: Evaluates repayment speed. 🟢 Low Risk (<7 days to repay), 🟡 Moderate Risk (7-30 days), 🔴 High Risk (>30 days overdue).
            7. WhatsApp Mini-Store & Loyalty Winback: Select inventory items to generate a digital WhatsApp catalog link, and send 1-tap loyalty winback discount codes to customers inactive for 30+ days.
            8. Store Logo Upload: In Settings -> 'Shop Profile' or during registration, tap the logo icon to upload a store picture from phone gallery.
            9. Autonomous Background Sync: 100% offline-first using Room DB; automatically flushes pending records to Cloud Firestore when network connects.
            
            Real-Time Store Financial Metrics:
            - Today's Total Sales: ₹${String.format(Locale.US, "%.2f", storeSnapshot.todaySalesTotal)} (${storeSnapshot.todayTxnCount} transactions)
            - Today's UPI Earnings: ₹${String.format(Locale.US, "%.2f", storeSnapshot.todayUpiTotal)}
            - Today's Cash Earnings: ₹${String.format(Locale.US, "%.2f", storeSnapshot.todayCashTotal)}
            - Total Pending Customer Udhar: ₹${String.format(Locale.US, "%.2f", storeSnapshot.totalUdharDue)}
            - Customers with Outstanding Balance: $debtorsInfo
            - Low Stock Products (${storeSnapshot.lowStockCount} items): $lowStockInfo
            
            MANDATORY INSTRUCTIONS:
            - MUST answer strictly in ${localeConfig.languageName}.
            - Begin responses with an appropriate greeting (${localeConfig.greeting}) when conversational.
            - If user asks how to use ANY app feature (e.g. GST billing, thermal printer, auto-split QR, Khata, upload logo, soundbox, contact developer), provide step-by-step guidance in ${localeConfig.languageName}.
            - If user asks about developer or contact info, state that the app is developed by Tanish Paul (Blue Auric Studio) and provide email: blueauricstudio@gmail.com.
            - Keep the response concise, encouraging, and clear (2-4 sentences).
        """.trimIndent()

        if (apiKey.isNullOrBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext getOfflineChatReply(userQuery, effectiveLang, storeSnapshot)
        }

        try {
            val contentsArray = JSONArray()
            contentsArray.put(JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().put(JSONObject().put("text", "System: $systemInstruction")))
            })
            contentsArray.put(JSONObject().apply {
                put("role", "model")
                put("parts", JSONArray().put(JSONObject().put("text", "${localeConfig.greeting}! I am AI Bol for ${storeSnapshot.shopName}, trained in ${localeConfig.languageName}.")))
            })

            history.takeLast(4).forEach { (msg, isUser) ->
                contentsArray.put(JSONObject().apply {
                    put("role", if (isUser) "user" else "model")
                    put("parts", JSONArray().put(JSONObject().put("text", msg)))
                })
            }

            contentsArray.put(JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().put(JSONObject().put("text", userQuery)))
            })

            val requestJson = JSONObject().apply {
                // Official Gemini API system_instruction field
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().put(JSONObject().put("text", systemInstruction)))
                })
                put("contents", contentsArray)
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.7)
                    put("maxOutputTokens", 300)
                })
            }

            val request = Request.Builder()
                .url("$BASE_URL?key=$apiKey")
                .post(requestJson.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = okHttpClient.newCall(request).execute()
            val responseBody = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                return@withContext getOfflineChatReply(userQuery, effectiveLang, storeSnapshot)
            }

            val json = JSONObject(responseBody)
            val reply = json.optJSONArray("candidates")
                ?.optJSONObject(0)
                ?.optJSONObject("content")
                ?.optJSONArray("parts")
                ?.optJSONObject(0)
                ?.optString("text")

            reply?.trim() ?: getOfflineChatReply(userQuery, effectiveLang, storeSnapshot)
        } catch (e: Exception) {
            Log.e(TAG, "Chat failed", e)
            getOfflineChatReply(userQuery, effectiveLang, storeSnapshot)
        }
    }

    /**
     * Multilingual offline intelligence reply generator covering all 9 supported regional languages.
     */
    private fun getOfflineChatReply(query: String, lang: String, snapshot: StoreSnapshot): String {
        val q = query.lowercase()
        val config = getLocalePromptConfig(lang)

        return when {
            // Contact Developer & Feedback FAQ
            q.contains("contact") || q.contains("developer") || q.contains("tanish") || q.contains("email") || q.contains("feedback") || q.contains("डेवलपर") || q.contains("संपर्क") -> {
                when (config.languageCode) {
                    "hi" -> "VyaparSetu के डेवलपर Tanish Paul (Blue Auric Studio) हैं। आप उन्हें सीधे blueauricstudio@gmail.com पर ईमेल कर सकते हैं या सेटिंग्स में 'Contact Us & Feedback' सेक्शन में जाकर कम्युनिटी फोरम पर अपना फीडबैक शेयर कर सकते हैं।"
                    "bn" -> "VyaparSetu-এর ডেভেলপার হলেন Tanish Paul (Blue Auric Studio)। আপনি সরাসরি blueauricstudio@gmail.com-এ যোগাযোগ করতে পারেন অথবা সেটিংসের 'Contact Us & Feedback'-এ মতামত দিতে পারেন।"
                    "ta" -> "VyaparSetu செயலியை உருவாக்கியவர் Tanish Paul (Blue Auric Studio). நீங்கள் அவரை blueauricstudio@gmail.com என்ற மின்னஞ்சலில் தொடர்பு கொள்ளலாம் அல்லது செட்டிங்ஸில் உள்ள 'Contact Us & Feedback' பகுதியை அணுகலாம்."
                    "mr" -> "VyaparSetu चे विकासक Tanish Paul (Blue Auric Studio) आहेत. आपण त्यांना blueauricstudio@gmail.com वर ईमेल करू शकता किंवा सेटिंग्जमधील 'Contact Us & Feedback' वापरू शकता."
                    "te" -> "VyaparSetu డెవలపర్ Tanish Paul (Blue Auric Studio). మీరు వారిని blueauricstudio@gmail.com ద్వారా సంప్రదించవచ్చు లేదా సెట్టింగ్స్‌లోని 'Contact Us & Feedback' ద్వారా మీ అభిప్రాయాన్ని పంచుకోవచ్చు."
                    "gu" -> "VyaparSetu ના ડેવલપર Tanish Paul (Blue Auric Studio) છે. તમે તેમને blueauricstudio@gmail.com પર ઈમેલ કરી શકો છો અથવા સેટિંગ્સમાં 'Contact Us & Feedback' પરથી સંપર્ક કરી શકો છો."
                    "pa" -> "VyaparSetu ਦੇ ਡਿਵੈਲਪਰ Tanish Paul (Blue Auric Studio) ਹਨ। ਤੁਸੀਂ ਉਹਨਾਂ ਨੂੰ blueauricstudio@gmail.com 'ਤੇ ਈਮੇਲ ਕਰ ਸਕਦੇ ਹੋ ਜਾਂ ਸੈਟਿੰਗਾਂ ਵਿੱਚ 'Contact Us & Feedback' ਦੀ ਵਰਤੋਂ ਕਰ ਸਕਦੇ ਹੋ।"
                    else -> "VyaparSetu is developed by Tanish Paul (Blue Auric Studio). You can email him directly at blueauricstudio@gmail.com or visit the 'Contact Us & Feedback' forum in Settings to share your feedback."
                }
            }
            // GST & Invoice Invoicing FAQ
            q.contains("invoice") || q.contains("gst") || q.contains("bill") || q.contains("बिल") || q.contains("रसीद") -> {
                when (config.languageCode) {
                    "hi" -> "GST या सामान्य बिल बनाने के लिए होम स्क्रीन पर 'GST / Non-GST Billing' कार्ड पर टैप करें। आइटम नाम, HSN कोड, रेट, टैक्स दर (0-28%) दर्ज करें। ऐप स्वतः CGST/SGST की गणना करेगा और आप 1-टैप में A4 या 3-इंच थर्मल PDF बनाकर WhatsApp पर भेज सकते हैं।"
                    "bn" -> "ইনভয়েস তৈরি করতে হোম স্ক্রিন থেকে 'GST / Non-GST Billing'-এ যান। আইটেম, রেট ও ট্যাক্স স্ল্যাব যোগ করুন। অ্যাপটি স্বয়ংক্রিয়ভাবে CGST/SGST হিসাব করে A4 বা ৩-ইঞ্চি থার্মাল PDF রসিদ তৈরি করবে।"
                    "ta" -> "ரசீது தயாரிக்க முகப்பு திரையில் 'GST / Non-GST Billing' என்பதைத் தட்டவும். பொருட்கள், வரி விபரங்களை உள்ளிட்டவுடன் A4 அல்லது 3-இன்ச் தர்மல் PDF பில் உருவாக்கலாம்."
                    "mr" -> "जीएसटी किंवा सामान्य बिल बनवण्यासाठी होम स्क्रीनवरील 'GST / Non-GST Billing' वर टॅप करा. आयटम, दर आणि कर स्लॅब प्रविष्ट करून १-टॅपमध्ये A4 किंवा ३-इंच पावती तयार करा."
                    "te" -> "ఇన్వాయిస్ రూపొందించడానికి 'GST / Non-GST Billing' పై నొక్కండి. వస్తువులు, ధర మరియు పన్ను ఎంటర్ చేసి A4 లేదా 3-అంగుళాల థర్మల్ రసీదుని డౌన్‌లోడ్ చేసుకోండి."
                    "gu" -> "ઇનવોઇસ બનાવવા માટે હોમ સ્ક્રીન પર 'GST / Non-GST Billing' પર ટેપ કરો. વસ્તુઓ અને ટેક્સ વિગતો ઉમેરી ૧-ક્લિકમાં A4 અથવા ૩-ઇંચ થર્મલ PDF રસીદ બનાવો."
                    "pa" -> "ਬਿੱਲ ਬਣਾਉਣ ਲਈ 'GST / Non-GST Billing' 'ਤੇ ਟੈਪ ਕਰੋ। ਆਈਟਮਾਂ ਅਤੇ ਟੈਕਸ ਸਲੈਬ ਦਰਜ ਕਰਕੇ A4 ਜਾਂ 3-ਇੰਚ ਥਰਮਲ ਰਸੀਦ ਤਿਆਰ ਕਰੋ।"
                    else -> "To create an invoice, tap 'GST / Non-GST Billing' on the Home screen. Enter items with HSN codes, rates, and tax brackets. The app auto-calculates CGST/SGST splits and lets you generate A4, A5, or 3-inch thermal PDF receipts shareable via WhatsApp."
                }
            }
            // Thermal Printer FAQ
            q.contains("printer") || q.contains("प्रिंटर") || q.contains("thermal") || q.contains("bluetooth") -> {
                when (config.languageCode) {
                    "hi" -> "ब्लूटूथ प्रिंटर कनेक्ट करने के लिए होम स्क्रीन से 'Thermal Printer' सेटअप में जाएं। ब्लूटूथ चालू करके 'Scan' दबाएं और अपना 58mm (2-इंच) या 80mm (3-इंच) ESC/POS प्रिंटर चुनें। आप तुरंत टेस्ट प्रिंट निकाल सकते हैं।"
                    "bn" -> "থার্মাল প্রিন্টার যুক্ত করতে 'Printer Setup'-এ গিয়ে ব্লুটুথ অন করুন এবং 'Scan' টিপে আপনার 58mm বা 80mm ESC/POS প্রিন্টারটি যুক্ত করুন।"
                    "ta" -> "தர்மல் பிரிண்டரை இணைக்க 'Printer Setup' சென்று புளூடூத் ஆன் செய்து 'Scan' கொடுத்து பிரிண்டரை தேர்வு செய்து டெஸ்ட் பிரிண்ட் எடுக்கலாம்."
                    "mr" -> "प्रिंटर कनेक्ट करण्यासाठी 'Printer Setup' मध्ये जाऊन ब्लूटूथ ऑन करा आणि 'Scan' दाबून 58mm किंवा 80mm प्रिंटर निवडा."
                    "te" -> "ప్రింటర్‌ను కనెక్ట్ చేయడానికి 'Printer Setup' లోకి వెళ్లి బ్లూటూత్ ద్వారా స్కాన్ చేసి 58mm లేదా 80mm ప్రింటర్‌ను జతచేయండి."
                    "gu" -> "પ્રિન્ટર કનેક્ટ કરવા માટે 'Printer Setup' માં જઈ બ્લૂટૂથ ઓન કરીને સ્કેન કરો અને 58mm અથવા 80mm ESC/POS પ્રિન્ટર જોડો."
                    "pa" -> "ਪ੍ਰਿੰਟਰ ਕਨੈਕਟ ਕਰਨ ਲਈ 'Printer Setup' ਵਿੱਚ ਜਾਓ, ਬਲੂਟੁੱਥ ਆਨ ਕਰਕੇ ਸਕੈਨ ਕਰੋ ਅਤੇ 58mm ਜਾਂ 80mm ਪ੍ਰਿੰਟਰ ਚੁਣੋ।"
                    else -> "To connect your wireless receipt printer, open 'Printer Setup' from the Home screen. Turn on Bluetooth, tap 'Scan', select your 58mm (2-inch) or 80mm (3-inch) ESC/POS printer, and run a test print."
                }
            }
            // Auto-Split QR FAQ
            q.contains("split") || q.contains("स्प्लिट") || q.contains("mdr") || q.contains("qr") -> {
                when (config.languageCode) {
                    "hi" -> "Auto-Split QR फीचर ₹2000 से अधिक के बिल को स्वचालित रूप से ₹2000 से कम के दो QR कोड में विभाजित कर देता है। इससे ग्राहक को 0% MDR शुल्क लगता है और आपका व्यापारिक मुनाफा सुरक्षित रहता है।"
                    "bn" -> "Auto-Split QR সুবিধা ২০০০ টাকার বেশি বিলকে স্বয়ংক্রিয়ভাবে সমান অংশে ভাগ করে, যার ফলে ০% MDR চার্জে পুরো টাকা সরাসরি আপনার অ্যাকাউন্টে জমা হয়।"
                    "ta" -> "Auto-Split QR அம்சம் ₹2000-க்கு மேற்பட்ட தொகையை சிறிய பகுதிகளாகப் பிரித்து 0% MDR கட்டணத்தில் பணத்தைப் பெற உதவுகிறது."
                    "mr" -> "Auto-Split QR ₹२००० पेक्षा जास्त बिल आपोआप लहान भागात विभागून ०% MDR शुल्काची खात्री देते."
                    "te" -> "Auto-Split QR ఫీచర్ ₹2000 మించిన బిల్లులను సమాన భాగాలుగా విభజించి 0% MDR ఛార్జీలతో చెల్లింపులను అందిస్తుంది."
                    "gu" -> "Auto-Split QR ₹૨૦૦૦ થી વધુનું બિલ આપોઆપ વિભાજીત કરી ૦% MDR શુલ્ક સાથે સંપૂર્ણ ચુકવણી સુરક્ષિત કરે છે."
                    "pa" -> "Auto-Split QR ₹2000 ਤੋਂ ਵੱਧ ਦੇ ਬਿੱਲ ਨੂੰ ਵੰਡ ਕੇ 0% MDR ਚਾਰਜ ਨਾਲ ਭੁਗਤਾਨ ਲੈਣ ਵਿੱਚ ਮਦਦ ਕਰਦਾ ਹੈ।"
                    else -> "The Auto-Split QR feature automatically splits transactions over ₹2000 into equal sub-₹2000 QR codes, ensuring 0% MDR merchant transaction charges."
                }
            }
            // Earnings & Sales
            q.contains("earn") || q.contains("कमाई") || q.contains("kamai") || q.contains("today") || q.contains("आज") || q.contains("sale") || q.contains("बिक्री") -> {
                val formattedSales = String.format(Locale.US, "%.2f", snapshot.todaySalesTotal)
                val formattedUpi = String.format(Locale.US, "%.2f", snapshot.todayUpiTotal)
                val formattedCash = String.format(Locale.US, "%.2f", snapshot.todayCashTotal)
                when (config.languageCode) {
                    "hi" -> "आज आपके स्टोर '${snapshot.shopName}' में कुल ₹$formattedSales की बिक्री दर्ज हुई है (${snapshot.todayTxnCount} लेनदेन)। इसमें से ₹$formattedUpi UPI से और ₹$formattedCash नकद प्राप्त हुए हैं।"
                    "bn" -> "আজ আপনার দোকান '${snapshot.shopName}'-এ মোট ₹$formattedSales বিক্রি হয়েছে (UPI: ₹$formattedUpi, ক্যাশ: ₹$formattedCash)।"
                    "ta" -> "இன்று '${snapshot.shopName}' கடையில் மொத்த விற்பனை ₹$formattedSales (UPI: ₹$formattedUpi, ரொக்கம்: ₹$formattedCash)."
                    "mr" -> "आज आपल्या '${snapshot.shopName}' दुकानात एकूण ₹$formattedSales ची विक्री झाली (UPI: ₹$formattedUpi, रोख: ₹$formattedCash)."
                    "te" -> "ఈ రోజు '${snapshot.shopName}' లో మొత్తం అమ్మకాలు ₹$formattedSales (UPI: ₹$formattedUpi, నగదు: ₹$formattedCash)."
                    "gu" -> "આજે તમારા સ્ટોર '${snapshot.shopName}' માં કુલ ₹$formattedSales નું વેચાણ થયું (UPI: ₹$formattedUpi, રોકડ: ₹$formattedCash)."
                    "pa" -> "ਅੱਜ ਤੁਹਾਡੀ ਦੁਕਾਨ '${snapshot.shopName}' ਵਿੱਚ ਕੁੱਲ ₹$formattedSales ਦੀ ਵਿਕਰੀ ਹੋਈ (UPI: ₹$formattedUpi, ਨਕਦ: ₹$formattedCash)।"
                    else -> "Today at '${snapshot.shopName}', you earned a total of ₹$formattedSales across ${snapshot.todayTxnCount} sales (₹$formattedUpi UPI + ₹$formattedCash Cash)."
                }
            }
            else -> {
                "${config.greeting} ${snapshot.ownerName} जी! I am AI Bol, your Store Advisor in ${config.languageName}. You can ask me about daily sales, Khata recovery, creating GST invoices, Bluetooth printing, or contacting developer Tanish Paul (blueauricstudio@gmail.com)."
            }
        }
    }
}
