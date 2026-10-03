package com.example.util

import android.media.AudioManager
import android.media.ToneGenerator
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object ChimeSoundManager {

    private const val TAG = "ChimeSoundManager"

    fun playCashRegisterChime() {
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
                // First high chime (cash drawer pop)
                toneGen.startTone(ToneGenerator.TONE_PROP_BEEP2, 140)
                delay(120)
                // Second resonant bell (coins landing / transaction cleared)
                toneGen.startTone(ToneGenerator.TONE_PROP_ACK, 260)
                delay(300)
                toneGen.release()
            } catch (e: Exception) {
                Log.w(TAG, "Audio tone generation bypassed: ${e.message}")
            }
        }
    }

    fun playSuccessBeep() {
        try {
            val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 90)
            toneGen.startTone(ToneGenerator.TONE_PROP_ACK, 180)
            CoroutineScope(Dispatchers.Default).launch {
                delay(220)
                toneGen.release()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Beep error: ${e.message}")
        }
    }
}
