package com.dietary.tracker.chandra.wakeword

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale

/**
 * FALLBACK IMPLEMENTATION - please read before relying on this in production.
 *
 * This is NOT a real wake-word engine. A real one (Picovoice Porcupine, etc.) uses a tiny
 * always-on DSP/keyword-spotting model that costs almost no battery and works fully offline.
 * This fallback instead repeatedly starts short-lived Android SpeechRecognizer sessions and
 * checks each transcript for "chandra" / "hi chandra" / "hey chandra". Concretely, that means:
 *
 *  - Battery cost is much higher than a real wake-word engine - this keeps the mic and (usually)
 *    a network-backed recognizer active in bursts continuously while enabled.
 *  - It generally will NOT keep working reliably once the screen is off or the device is deep in
 *    Doze, because Android restricts background microphone access outside of foreground-service
 *    windows and OEM battery managers frequently kill background services. A persistent
 *    foreground-service notification (see ChandraWakeService) is what makes this legal per
 *    Android's rules, but it does not guarantee OEMs won't still throttle it.
 *  - Each restart briefly drops audio between sessions, so a wake word spoken in that gap is
 *    missed - unlike a real streaming keyword spotter.
 *
 * This exists so "Always Listen" has a working, permission-compliant implementation today,
 * architected behind [WakeWordEngine] so it can be swapped for Porcupine or similar later without
 * touching ChandraWakeService's calling code.
 */
class SpeechRecognizerWakeWordEngine(private val context: Context) : WakeWordEngine {

    private var recognizer: SpeechRecognizer? = null
    private var running = false
    private val handler = Handler(Looper.getMainLooper())
    private val wakePhrases = listOf("chandra", "hi chandra", "hey chandra")

    override fun isRunning(): Boolean = running

    override fun start(onWakeWordDetected: () -> Unit, onError: (String) -> Unit) {
        if (running) return
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onError("Speech recognition isn't available on this device.")
            return
        }
        running = true
        listenOnce(onWakeWordDetected, onError)
    }

    private fun listenOnce(onWakeWordDetected: () -> Unit, onError: (String) -> Unit) {
        if (!running) return

        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: android.os.Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}

                override fun onResults(results: android.os.Bundle?) {
                    val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()?.lowercase().orEmpty()
                    if (wakePhrases.any { text.contains(it) }) {
                        onWakeWordDetected()
                    }
                    restartAfterDelay(onWakeWordDetected, onError)
                }

                override fun onError(error: Int) {
                    // Timeouts/no-match are expected constantly in a restart loop - just retry.
                    restartAfterDelay(onWakeWordDetected, onError)
                }

                override fun onPartialResults(partialResults: android.os.Bundle?) {}
                override fun onEvent(eventType: Int, params: android.os.Bundle?) {}
            })

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US.toString())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            }
            try {
                startListening(intent)
            } catch (e: Exception) {
                onError(e.message ?: "Couldn't start wake-word listening.")
                running = false
            }
        }
    }

    private fun restartAfterDelay(onWakeWordDetected: () -> Unit, onError: (String) -> Unit) {
        if (!running) return
        handler.postDelayed({ listenOnce(onWakeWordDetected, onError) }, 400L)
    }

    override fun stop() {
        running = false
        handler.removeCallbacksAndMessages(null)
        recognizer?.stopListening()
        recognizer?.destroy()
        recognizer = null
    }
}
