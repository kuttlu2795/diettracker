package com.dietary.tracker.chandra.wakeword

/**
 * A dedicated, low-power, offline wake-word engine (e.g. Picovoice Porcupine, which supports
 * training a custom "Hi Chandra" model) continuously monitors the mic using a tiny keyword-
 * spotting model and barely touches the battery. Integrating one requires adding that SDK,
 * shipping its model file, and (for Porcupine specifically) a free personal AccessKey from
 * https://console.picovoice.ai. That integration is NOT included here - see
 * [SpeechRecognizerWakeWordEngine] for the working fallback that ships instead, and the README
 * for exactly what swapping in a real engine would involve.
 *
 * Anything that drives wake-word listening (the foreground service) codes against this
 * interface, not a concrete engine, so a real engine can be dropped in later by implementing
 * this interface and changing one line in ChandraWakeService.
 */
interface WakeWordEngine {
    /** Starts listening for the wake word. Calls [onWakeWordDetected] each time it fires. */
    fun start(onWakeWordDetected: () -> Unit, onError: (String) -> Unit)

    fun stop()

    fun isRunning(): Boolean
}
