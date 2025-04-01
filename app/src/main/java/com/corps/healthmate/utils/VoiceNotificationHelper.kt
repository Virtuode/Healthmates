package com.corps.healthmate.utils

import android.content.Context
import android.speech.tts.TextToSpeech
import timber.log.Timber
import java.util.*

class VoiceNotificationHelper(private val context: Context) {
    private var textToSpeech: TextToSpeech? = null
    private var isInitialized = false
    private var pendingText: String? = null
    private var currentLanguage = Locale.getDefault()

    init {
        initTTS()
    }

    private fun initTTS() {
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isInitialized = true
                pendingText?.let {
                    speakText(it)
                    pendingText = null
                }
            } else {
                Timber.tag("VoiceNotification").e("Failed to initialize TextToSpeech")
            }
        }
    }

    fun setLanguage(isHindi: Boolean) {
        currentLanguage = if (isHindi) {
            Locale("hi", "IN")
        } else {
            Locale.ENGLISH
        }
        textToSpeech?.language = currentLanguage
    }


    fun speakText(text: String) {
        if (!isInitialized) {
            pendingText = text
            return
        }

        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "medicine_reminder")
    }

    fun shutdown() {
        textToSpeech?.let { tts ->
            tts.stop()
            tts.shutdown()
        }
        textToSpeech = null
        isInitialized = false
    }
}
