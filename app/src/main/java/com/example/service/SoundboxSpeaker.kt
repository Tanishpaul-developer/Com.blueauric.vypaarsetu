package com.example.service

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.example.data.model.SoundboxNotificationEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

object SoundboxSpeaker {

    private const val TAG = "SoundboxSpeaker"

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private val _eventsFlow = MutableSharedFlow<SoundboxNotificationEvent>(replay = 1)
    val eventsFlow: SharedFlow<SoundboxNotificationEvent> = _eventsFlow.asSharedFlow()

    private val _lastAnnouncement = MutableStateFlow<SoundboxNotificationEvent?>(null)
    val lastAnnouncement: StateFlow<SoundboxNotificationEvent?> = _lastAnnouncement.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    fun init(context: Context, onReady: (() -> Unit)? = null) {
        if (tts != null && isInitialized) {
            onReady?.invoke()
            return
        }
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isInitialized = true
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _isSpeaking.value = true
                    }

                    override fun onDone(utteranceId: String?) {
                        _isSpeaking.value = false
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        _isSpeaking.value = false
                    }

                    override fun onError(utteranceId: String?, errorCode: Int) {
                        _isSpeaking.value = false
                        Log.e(TAG, "TTS Utterance error code: $errorCode")
                    }
                })
                Log.d(TAG, "TTS engine initialized successfully")
                onReady?.invoke()
            } else {
                Log.e(TAG, "TTS initialization failed: $status")
            }
        }
    }

    private fun resolveBestLocale(engine: TextToSpeech, languageCode: String): Locale {
        val targetLocale = when (languageCode.lowercase()) {
            "hi" -> Locale("hi", "IN")
            "bn" -> Locale("bn", "IN")
            "mr" -> Locale("mr", "IN")
            "ta" -> Locale("ta", "IN")
            "te" -> Locale("te", "IN")
            "gu" -> Locale("gu", "IN")
            "pa" -> Locale("pa", "IN")
            "mni" -> Locale("bn", "IN") // Manipuri Bengali script phonetics
            else -> Locale("en", "IN")
        }

        try {
            val availability = engine.isLanguageAvailable(targetLocale)
            if (availability >= TextToSpeech.LANG_AVAILABLE) {
                return targetLocale
            }
        } catch (_: Exception) {}

        // Fallback 1: Language without country
        try {
            val langOnly = Locale(languageCode.lowercase())
            if (engine.isLanguageAvailable(langOnly) >= TextToSpeech.LANG_AVAILABLE) {
                return langOnly
            }
        } catch (_: Exception) {}

        // Fallback 2: Hindi
        try {
            val hindi = Locale("hi", "IN")
            if (engine.isLanguageAvailable(hindi) >= TextToSpeech.LANG_AVAILABLE) {
                return hindi
            }
        } catch (_: Exception) {}

        // Fallback 3: English (India)
        try {
            val englishIn = Locale("en", "IN")
            if (engine.isLanguageAvailable(englishIn) >= TextToSpeech.LANG_AVAILABLE) {
                return englishIn
            }
        } catch (_: Exception) {}

        return Locale.US
    }

    fun buildAnnouncementText(
        appSource: String,
        amount: Double,
        languageCode: String
    ): String {
        val intAmount = if (amount % 1.0 == 0.0) amount.toLong().toString() else String.format(Locale.US, "%.2f", amount)
        val source = if (appSource.isBlank()) "UPI" else appSource

        return when (languageCode.lowercase()) {
            "hi" -> "$source पर ₹$intAmount प्राप्त हुए"
            "bn" -> "$source এ ₹$intAmount টাকা প্রাপ্ত হয়েছে"
            "mr" -> "$source वर ₹$intAmount रुपये मिळाले"
            "ta" -> "$source இல் ₹$intAmount பெறப்பட்டது"
            "te" -> "$source లో ₹$intAmount రూపాయలు వచ్చాయి"
            "gu" -> "$source પર ₹$intAmount રૂપિયા મળ્યા"
            "pa" -> "$source 'ਤੇ ₹$intAmount ਰੁਪਏ ਪ੍ਰਾਪਤ ਹੋਏ"
            "mni" -> "$source da ₹$intAmount phangley"
            else -> "Received $intAmount rupees on $source"
        }
    }

    fun buildLowStockText(
        productName: String,
        remainingQty: Double,
        unit: String,
        languageCode: String
    ): String {
        val qtyStr = if (remainingQty % 1.0 == 0.0) remainingQty.toLong().toString() else String.format(Locale.US, "%.1f", remainingQty)
        return when (languageCode.lowercase()) {
            "hi" -> "चेतावनी: $productName का स्टॉक कम है, केवल $qtyStr $unit शेष है!"
            "bn" -> "সতর্কতা: $productName এর স্টক কম, মাত্র $qtyStr $unit অবশিষ্ট আছে!"
            "mr" -> "सावधान: $productName चा साठा कमी आहे, फक्त $qtyStr $unit उरले आहेत!"
            "ta" -> "எச்சரிக்கை: $productName கையிருப்பு குறைவு, $qtyStr $unit மட்டுமே உள்ளது!"
            "te" -> "హెచ్చరిక: $productName స్టాక్ తక్కువగా ఉంది, కేవలం $qtyStr $unit మాత్రమే మిగిలి ఉంది!"
            "gu" -> "ચેતવણી: $productName નો સ્ટોક ઓછો છે, ફક્ત $qtyStr $unit બાકી છે!"
            "pa" -> "ਸਾਵਧਾਨ: $productName ਦਾ ਸਟਾਕ ਘੱਟ ਹੈ, ਸਿਰਫ਼ $qtyStr $unit ਬਾਕੀ ਹੈ!"
            "mni" -> "Cheirak: $productName gi stock hanthare, mathanta $qtyStr $unit leihoure!"
            else -> "Warning: Low stock for $productName, only $qtyStr $unit remaining!"
        }
    }

    fun announceLowStock(
        context: Context,
        productName: String,
        remainingQty: Double,
        unit: String,
        languageCode: String = "hi"
    ) {
        init(context) {
            val spokenText = buildLowStockText(productName, remainingQty, unit, languageCode)
            speak(spokenText, languageCode)
        }
    }

    fun announcePayment(
        context: Context,
        appSource: String,
        amount: Double,
        senderName: String = "",
        languageCode: String = "hi",
        pitch: Float = 1.0f,
        speechRate: Float = 1.0f,
        voiceTone: String = "female",
        preChime: Boolean = true
    ) {
        init(context) {
            val spokenText = buildAnnouncementText(appSource, amount, languageCode)

            val event = SoundboxNotificationEvent(
                id = System.currentTimeMillis().toString(),
                appSource = appSource,
                amount = amount,
                senderName = senderName,
                timestamp = System.currentTimeMillis(),
                announcedText = spokenText
            )

            _lastAnnouncement.value = event
            _eventsFlow.tryEmit(event)

            if (preChime) {
                playChimeSound()
            }

            speak(spokenText, languageCode, pitch, speechRate, voiceTone)
        }
    }

    private fun playChimeSound() {
        try {
            val toneGen = android.media.ToneGenerator(android.media.AudioManager.STREAM_MUSIC, 85)
            toneGen.startTone(android.media.ToneGenerator.TONE_PROP_ACK, 220)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to play pre-chime tone: ${e.message}")
        }
    }

    fun stopSpeaking() {
        try {
            tts?.stop()
        } catch (_: Exception) {}
        _isSpeaking.value = false
    }

    fun speakText(
        context: Context,
        text: String,
        languageCode: String = "hi",
        pitch: Float = 1.0f,
        speechRate: Float = 1.0f,
        voiceTone: String = "female"
    ) {
        init(context) {
            speak(text, languageCode, pitch, speechRate, voiceTone)
        }
    }

    private fun speak(
        text: String,
        languageCode: String,
        pitch: Float = 1.0f,
        speechRate: Float = 1.0f,
        voiceTone: String = "female"
    ) {
        tts?.let { engine ->
            val resolvedLocale = resolveBestLocale(engine, languageCode)
            try {
                engine.language = resolvedLocale
            } catch (e: Exception) {
                Log.w(TAG, "Locale set error: ${e.message}")
            }

            // Adjust Pitch & Speed according to user settings and voice tone
            val effectivePitch = when (voiceTone.lowercase()) {
                "female" -> (pitch * 1.15f).coerceIn(0.5f, 2.0f)
                "male" -> (pitch * 0.82f).coerceIn(0.5f, 2.0f)
                else -> pitch.coerceIn(0.5f, 2.0f)
            }
            engine.setPitch(effectivePitch)
            engine.setSpeechRate(speechRate.coerceIn(0.5f, 2.0f))

            // Try selecting specific voice for gender if available in TTS engine
            try {
                val voices = engine.voices
                if (!voices.isNullOrEmpty()) {
                    val matchingVoice = voices.firstOrNull { voice ->
                        voice.locale.language == resolvedLocale.language &&
                                when (voiceTone.lowercase()) {
                                    "female" -> voice.name.contains("female", ignoreCase = true) || voice.name.contains("f0", ignoreCase = true)
                                    "male" -> voice.name.contains("male", ignoreCase = true) || voice.name.contains("m0", ignoreCase = true)
                                    else -> true
                                }
                    }
                    if (matchingVoice != null) {
                        engine.voice = matchingVoice
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Voice selection error: ${e.message}")
            }

            _isSpeaking.value = true
            engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "vps_${System.currentTimeMillis()}")
        }
    }

    fun announceDailyClosing(
        context: Context,
        totalAmount: Double,
        cashAmount: Double,
        upiAmount: Double,
        billCount: Int,
        languageCode: String = "hi"
    ) {
        init(context) {
            val totalInt = totalAmount.toLong()
            val cashInt = cashAmount.toLong()
            val upiInt = upiAmount.toLong()
            val text = when (languageCode.lowercase()) {
                "hi" -> "दुकान बंद करने का समय! आज की कुल बिक्री ₹$totalInt है। कैश ₹$cashInt और यूपीआई से ₹$upiInt प्राप्त हुए। आज कुल $billCount बिल बने। बहुत बढ़िया व्यापार!"
                "bn" -> "দোকান বন্ধ করার সময়! আজকের মোট বিক্রি ₹$totalInt টাকা। ক্যাশ ₹$cashInt এবং ইউপিআই ₹$upiInt। দারুণ কাজ!"
                "mr" -> "दुकान बंद करण्याची वेळ! आजची एकूण विक्री ₹$totalInt आहे. रोख ₹$cashInt आणि यूपीआय ₹$upiInt. उत्तम व्यापार!"
                "ta" -> "கடை மூடும் நேரம்! இன்றைய மொத்த விற்பனை ₹$totalInt. ரொக்கம் ₹$cashInt மற்றும் யுபிஐ ₹$upiInt."
                "te" -> "దుకాణం ముగింపు సమయం! నేటి మొత్తం అమ్మకాలు ₹$totalInt. నగదు ₹$cashInt మరియు యుపిఐ ₹$upiInt."
                "gu" -> "દુકાન બંધ કરવાનો સમય! આજનું કુલ વેચાણ ₹$totalInt છે. રોકડ ₹$cashInt અને યુપીઆઈ ₹$upiInt."
                "pa" -> "ਦੁਕਾਨ ਬੰਦ ਕਰਨ ਦਾ ਸਮਾਂ! ਅੱਜ ਦੀ ਕੁੱਲ ਵਿਕਰੀ ₹$totalInt ਹੈ। ਕੈਸ਼ ₹$cashInt ਅਤੇ ਯੂਪੀਆਈ ₹$upiInt।"
                else -> "Shop closing time! Today's total sales are $totalInt rupees. Cash $cashInt rupees and UPI $upiInt rupees across $billCount transactions. Excellent job!"
            }
            speak(text, languageCode)
        }
    }

    fun replayLast(context: Context, languageCode: String = "hi") {
        val last = _lastAnnouncement.value
        if (last != null) {
            announcePayment(
                context = context,
                appSource = last.appSource,
                amount = last.amount,
                senderName = last.senderName,
                languageCode = languageCode
            )
        } else {
            init(context) {
                val readyText = when (languageCode.lowercase()) {
                    "hi" -> "VyaparSetu साउंडबॉक्स सक्रिय है और भुगतानों के लिए तैयार है।"
                    "bn" -> "VyaparSetu সাউন্ডবক্স সক্রিয় এবং প্রস্তুত।"
                    "mr" -> "VyaparSetu साऊंडबॉक्स चालू असून तयार आहे."
                    "ta" -> "VyaparSetu சவுண்ட்பாக்ஸ் தயாராக உள்ளது."
                    "te" -> "VyaparSetu సౌండ్‌బాక్స్ సిద్ధంగా ఉంది."
                    "gu" -> "VyaparSetu સાઉન્ડબોક્સ સક્રિય છે."
                    "pa" -> "VyaparSetu ਸਾਊਂਡਬਾਕਸ ਚਾਲੂ ਹੈ।"
                    "mni" -> "VyaparSetu Soundbox sem-saana leire."
                    else -> "VyaparSetu Soundbox is active and ready for payments."
                }
                speak(readyText, languageCode)
            }
        }
    }
}
