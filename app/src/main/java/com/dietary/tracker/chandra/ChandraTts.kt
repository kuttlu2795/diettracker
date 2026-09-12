package com.dietary.tracker.chandra

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale
import java.util.UUID

/**
 * Thin wrapper around Android's built-in TextToSpeech engine. This runs on-device via whatever
 * TTS engine is installed (typically Google's) - no audio or text is sent anywhere by this class
 * itself. Tamil output quality depends entirely on whether the device has a Tamil TTS voice
 * installed; if not, English is used as a fallback so Chandra still responds audibly.
 */
class ChandraTts(context: Context, private val onReady: (() -> Unit)? = null) {

    private var isReady = false
    private val engine: TextToSpeech = TextToSpeech(context.applicationContext) { status ->
        isReady = status == TextToSpeech.SUCCESS
        if (isReady) onReady?.invoke()
    }

    fun speak(text: String, preferTamil: Boolean = false) {
        if (!isReady) return
        val locale = if (preferTamil) Locale("ta", "IN") else Locale.US
        val result = engine.setLanguage(locale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            engine.setLanguage(Locale.US) // fall back to English if the requested voice isn't installed
        }
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, UUID.randomUUID().toString())
    }

    fun setOnDoneListener(onDone: () -> Unit) {
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) { onDone() }
            @Deprecated("Deprecated in Java") override fun onError(utteranceId: String?) {}
        })
    }

    fun shutdown() {
        engine.stop()
        engine.shutdown()
    }
}
