package com.example.ui.viewmodel

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.service.InvoiceItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.TimeUnit

data class ParsedVoiceItem(
    val name: String,
    val qty: Int,
    val price: Double
)

data class VoiceBillingResult(
    val action: String, // "ADD_UDHAR" or "CREATE_BILL"
    val customerName: String?,
    val amount: Double,
    val items: List<ParsedVoiceItem>,
    val rawTranscript: String
)

class VoiceBillingViewModel : ViewModel() {

    companion object {
        private const val TAG = "VoiceBillingVM"
        private const val GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent"
    }

    private var speechRecognizer: SpeechRecognizer? = null

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _rmsDb = MutableStateFlow(0f)
    val rmsDb: StateFlow<Float> = _rmsDb.asStateFlow()

    private val _transcript = MutableStateFlow("")
    val transcript: StateFlow<String> = _transcript.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _parsedResult = MutableStateFlow<VoiceBillingResult?>(null)
    val parsedResult: StateFlow<VoiceBillingResult?> = _parsedResult.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    fun startListening(context: Context, langCode: String = "hi-IN") {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _errorMessage.value = "Speech recognition is not available on this device"
            return
        }

        stopListening()
        _errorMessage.value = null
        _transcript.value = ""
        _parsedResult.value = null

        val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        speechRecognizer = recognizer

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, langCode)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "बोलिए (उदा. 'रमेश को 500 का राशन उधार' या '2 लक्स साबुन 80 रुपये')")
        }

        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                _isListening.value = true
                Log.d(TAG, "Ready for speech")
            }

            override fun onBeginningOfSpeech() {
                _isListening.value = true
            }

            override fun onRmsChanged(rmsdB: Float) {
                _rmsDb.value = rmsdB.coerceAtLeast(0f)
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                _isListening.value = false
            }

            override fun onError(error: Int) {
                _isListening.value = false
                val msg = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized. Please speak clearly into the mic."
                    SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network issue with voice recognition. Please try again."
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error. Check mic permissions."
                    SpeechRecognizer.ERROR_CLIENT -> "Voice client error."
                    else -> "Voice recognition stopped (Code $error)"
                }
                Log.w(TAG, "SpeechRecognizer error: $msg")
                if (error != SpeechRecognizer.ERROR_NO_MATCH) {
                    _errorMessage.value = msg
                }
            }

            override fun onResults(results: Bundle?) {
                _isListening.value = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val spokenText = matches?.firstOrNull() ?: ""
                if (spokenText.isNotBlank()) {
                    _transcript.value = spokenText
                    processTranscript(spokenText)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val partial = matches?.firstOrNull()
                if (!partial.isNullOrBlank()) {
                    _transcript.value = partial
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        try {
            recognizer.startListening(intent)
        } catch (e: Exception) {
            _isListening.value = false
            _errorMessage.value = "Failed to start listening: ${e.message}"
        }
    }

    fun stopListening() {
        _isListening.value = false
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            Log.w(TAG, "Error cleaning recognizer: ${e.message}")
        }
        speechRecognizer = null
    }

    fun setManualTranscript(text: String) {
        _transcript.value = text
        if (text.isNotBlank()) {
            processTranscript(text)
        }
    }

    /**
     * Sends spoken input to Gemini 2.5 Flash with structured JSON output schema:
     * {"action": "ADD_UDHAR | CREATE_BILL", "customer_name": String?, "amount": Double, "items": [{"name": String, "qty": Int, "price": Double}]}
     */
    fun processTranscript(transcriptText: String) {
        viewModelScope.launch {
            _isProcessing.value = true
            _errorMessage.value = null

            try {
                val result = callGeminiVoiceParser(transcriptText)
                _parsedResult.value = result
            } catch (e: Exception) {
                Log.e(TAG, "Gemini parsing failed, using fallback heuristic: ${e.message}")
                val fallback = heuristicFallbackParser(transcriptText)
                _parsedResult.value = fallback
            } finally {
                _isProcessing.value = false
            }
        }
    }

    private suspend fun callGeminiVoiceParser(text: String): VoiceBillingResult = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        val isNetworkAvailable = try {
            val cm = speechRecognizer?.let { null } // safe fallback
            true
        } catch (_: Exception) {
            true
        }

        if (apiKey.isBlank()) {
            return@withContext heuristicFallbackParser(text)
        }

        val prompt = """
            You are an expert Indian Kirana & Vyapar POS Voice Billing assistant.
            Process this merchant speech input in regional Indian language (Hindi, Hinglish, English):
            "$text"

            Extract the merchant's intent into this exact JSON structure:
            {
              "action": "ADD_UDHAR" or "CREATE_BILL",
              "customer_name": "Name of customer if mentioned, or null",
              "amount": total amount as a number,
              "items": [
                {
                  "name": "Item description",
                  "qty": quantity as integer,
                  "price": unit price or total item price as number
                }
              ]
            }

            Rules:
            1. If it mentions udhar, khata, baaki, jama, credit, or debt to a person (e.g., 'Ramesh ko 500 ka udhar', 'Mohan khata 250'), action MUST be 'ADD_UDHAR'.
            2. If it mentions items, bill, cash, sale, or items list (e.g., '2 Lux soap cash bill 80 rupees', '5kg atta 200 rs'), action MUST be 'CREATE_BILL'.
            3. Return raw JSON ONLY. No markdown formatting, no backticks.
        """.trimIndent()

        val jsonBody = JSONObject().apply {
            val contents = JSONArray().apply {
                put(JSONObject().apply {
                    val parts = JSONArray().apply {
                        put(JSONObject().apply { put("text", prompt) })
                    }
                    put("parts", parts)
                })
            }
            put("contents", contents)
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.1)
                put("responseMimeType", "application/json")
            })
        }

        val request = Request.Builder()
            .url("$GEMINI_URL?key=$apiKey")
            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = okHttpClient.newCall(request).execute()
        val responseBody = response.body?.string() ?: throw Exception("Empty response from Gemini")

        if (!response.isSuccessful) {
            throw Exception("Gemini error ${response.code}: $responseBody")
        }

        val root = JSONObject(responseBody)
        val candidates = root.optJSONArray("candidates")
        val content = candidates?.optJSONObject(0)?.optJSONObject("content")
        val parts = content?.optJSONArray("parts")
        val rawText = parts?.optJSONObject(0)?.optString("text") ?: ""

        val cleanJson = rawText.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        val parsedJson = JSONObject(cleanJson)

        val action = parsedJson.optString("action", "CREATE_BILL")
        val customerName = if (parsedJson.isNull("customer_name")) null else parsedJson.optString("customer_name")
        val totalAmount = parsedJson.optDouble("amount", 0.0)

        val itemsList = mutableListOf<ParsedVoiceItem>()
        val itemsArr = parsedJson.optJSONArray("items")
        if (itemsArr != null) {
            for (i in 0 until itemsArr.length()) {
                val itm = itemsArr.getJSONObject(i)
                itemsList.add(
                    ParsedVoiceItem(
                        name = itm.optString("name", "Item"),
                        qty = itm.optInt("qty", 1),
                        price = itm.optDouble("price", 0.0)
                    )
                )
            }
        }

        VoiceBillingResult(
            action = action,
            customerName = customerName?.ifBlank { null },
            amount = if (totalAmount > 0.0) totalAmount else itemsList.sumOf { it.qty * it.price },
            items = itemsList,
            rawTranscript = text
        )
    }

    /**
     * Offline heuristic parser for instant processing without network
     */
    private fun heuristicFallbackParser(text: String): VoiceBillingResult {
        val lower = text.lowercase(Locale.ROOT)
        val isUdhar = lower.contains("udhar") || lower.contains("उधार") ||
                lower.contains("khata") || lower.contains("खाता") ||
                lower.contains("baaki") || lower.contains("बाकी")

        val action = if (isUdhar) "ADD_UDHAR" else "CREATE_BILL"

        // Extract amount numbers
        val numberRegex = Regex("(\\d+(\\.\\d+)?)")
        val matches = numberRegex.findAll(text).toList()

        val amount = matches.lastOrNull()?.value?.toDoubleOrNull() ?: 100.0

        // Extract name
        val words = text.split(" ")
        var detectedName: String? = null
        val koIndex = words.indexOfFirst { it.equals("ko", ignoreCase = true) || it == "को" }
        if (koIndex > 0) {
            detectedName = words[koIndex - 1].replaceFirstChar { it.uppercase() }
        }

        val items = mutableListOf<ParsedVoiceItem>()
        if (isUdhar) {
            items.add(ParsedVoiceItem(name = "राशन / किराना लेन-देन", qty = 1, price = amount))
        } else {
            items.add(ParsedVoiceItem(name = text.take(25), qty = 1, price = amount))
        }

        return VoiceBillingResult(
            action = action,
            customerName = detectedName,
            amount = amount,
            items = items,
            rawTranscript = text
        )
    }

    fun toInvoiceItems(parsedItems: List<ParsedVoiceItem>): List<InvoiceItem> {
        return parsedItems.map {
            InvoiceItem(
                name = it.name,
                hsnCode = "9983",
                quantity = it.qty.toDouble().coerceAtLeast(1.0),
                unit = "pcs",
                rate = if (it.price > 0) it.price else 0.0,
                discountPercent = 0.0,
                taxRatePercent = 18.0
            )
        }
    }

    fun reset() {
        stopListening()
        _transcript.value = ""
        _parsedResult.value = null
        _errorMessage.value = null
        _isProcessing.value = false
    }

    override fun onCleared() {
        super.onCleared()
        stopListening()
    }
}
