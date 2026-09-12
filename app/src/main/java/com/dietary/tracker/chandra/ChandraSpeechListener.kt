package com.dietary.tracker.chandra

import android.content.Context
import android.content.Intent
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale

/**
 * Wraps Android's built-in SpeechRecognizer for a single push-to-talk capture ("tap mic, speak,
 * get transcript"). Whether this processes audio on-device or via the network depends entirely
 * on the device/OS and installed recognition service (this is standard Android platform
 * behavior, not something this app changes) - see the README for details. This app makes no
 * additional network calls of its own for speech recognition.
 */
class ChandraSpeechListener(private val context: Context) {

    private var recognizer: SpeechRecognizer? = null

    fun isAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    fun startListening(
        preferTamil: Boolean = false,
        onResult: (String) -> Unit,
        onError: (String) -> Unit,
        onReadyForSpeech: () -> Unit = {}
    ) {
        if (!isAvailable()) {
            onError("Speech recognition isn't available on this device.")
            return
        }
        stop()
        recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: android.os.Bundle?) { onReadyForSpeech() }
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onError(error: Int) {
                    onError(errorMessage(error))
                }
                override fun onResults(results: android.os.Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val best = matches?.firstOrNull()
                    if (best.isNullOrBlank()) onError("I didn't catch that.") else onResult(best)
                }
                override fun onPartialResults(partialResults: android.os.Bundle?) {}
                override fun onEvent(eventType: Int, params: android.os.Bundle?) {}
            })
        }

        val locale = if (preferTamil) "ta-IN" else Locale.US.toString()
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        }
        recognizer?.startListening(intent)
    }

    fun stop() {
        recognizer?.stopListening()
        recognizer?.destroy()
        recognizer = null
    }

    private fun errorMessage(error: Int): String = when (error) {
        SpeechRecognizer.ERROR_NO_MATCH -> "I didn't catch that."
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "I didn't hear anything."
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is required for Chandra voice commands."
        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network error during speech recognition."
        else -> "Sorry, I didn't understand that. Please try again."
    }
}
