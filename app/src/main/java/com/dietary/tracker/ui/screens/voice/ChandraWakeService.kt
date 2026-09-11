package com.dietary.tracker.ui.screens.voice

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.core.app.NotificationCompat
import com.dietary.tracker.MainActivity
import com.dietary.tracker.R
import com.dietary.tracker.health.HealthConnectManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel
import java.util.Locale

class ChandraWakeService : Service(), TextToSpeech.OnInitListener {
    companion object {
        const val ACTION_START = "com.dietary.tracker.CHANDRA_START"
        const val ACTION_STOP = "com.dietary.tracker.CHANDRA_STOP"
        private const val CHANNEL_ID = "chandra_voice"
        private const val NOTIFICATION_ID = 4401
    }

    private var recognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var waitingForCommand = false
    private var restarting = false
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()
        createChannel()
        tts = TextToSpeech(this, this)
        startForeground(NOTIFICATION_ID, notification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> { stopListening(); stopSelf(); return START_NOT_STICKY }
            else -> {
                if (ChandraVoiceSettings.isAlwaysListeningEnabled(this)) startListening()
            }
        }
        return START_STICKY
    }

    private fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            speak("Voice recognition is not available on this phone.")
            return
        }
        if (recognizer == null) {
            recognizer = SpeechRecognizer.createSpeechRecognizer(this)
            recognizer?.setRecognitionListener(listener)
        }
        waitingForCommand = false
        restarting = false
        recognizer?.startListening(recognitionIntent())
    }

    private fun listenForCommand() {
        waitingForCommand = true
        restarting = false
        recognizer?.cancel()
        recognizer?.startListening(recognitionIntent())
    }

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}
        override fun onPartialResults(partialResults: Bundle?) {}
        override fun onEvent(eventType: Int, params: Bundle?) {}

        override fun onError(error: Int) {
            if (ChandraVoiceSettings.isAlwaysListeningEnabled(this@ChandraWakeService) && !restarting) {
                restarting = true
                android.os.Handler(mainLooper).postDelayed({
                    if (ChandraVoiceSettings.isAlwaysListeningEnabled(this@ChandraWakeService)) startListening()
                }, 700L)
            }
        }

        override fun onResults(results: Bundle?) {
            val raw = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull().orEmpty()
            if (waitingForCommand) {
                waitingForCommand = false
                handleCommand(raw)
            } else {
                val lower = raw.lowercase(Locale.getDefault())
                val wakeIndex = listOf("hi chandra", "hey chandra", "chandra").firstNotNullOfOrNull { phrase ->
                    lower.indexOf(phrase).takeIf { it >= 0 }
                }
                if (wakeIndex != null) {
                    val phrase = listOf("hi chandra", "hey chandra", "chandra")
                        .first { lower.contains(it) }
                    val remainder = raw.substring((wakeIndex + phrase.length).coerceAtMost(raw.length)).trim(' ', ',', '.', '?', '!')
                    openApp()
                    if (remainder.isNotBlank()) handleCommand(remainder) else {
                        speak("Yes, I'm listening.")
                        listenForCommand()
                    }
                } else {
                    startListening()
                }
            }
        }
    }

    private fun handleCommand(raw: String) {
        val q = raw.lowercase(Locale.getDefault())
        val hc = HealthConnectManager(this)
        scope.launch {
            val answer = when {
                q.contains("heart") || q.contains("pulse") || q.contains("heartbeat") || q.contains("ஹார்ட்") ->
                    hc.latestHeartRate()?.let { "உங்களுடைய current heart rate $it BPM." }
                        ?: "Heart rate data கிடைக்கவில்லை. Please sync your watch."
                q.contains("step") || q.contains("நடந்த") || q.contains("ஸ்டெப்") ->
                    "இன்னைக்கு ${hc.todaySteps()} steps நடந்திருக்கீங்க."
                q.contains("weight") || q.contains("எடை") ->
                    hc.latestWeightKg()?.let { "Current weight ${String.format(Locale.US, "%.1f", it)} kg." }
                        ?: "Weight data கிடைக்கவில்லை."
                q.contains("sleep") || q.contains("தூக்கம்") ->
                    hc.latestSleepHours()?.let { "நேத்து sleep ${String.format(Locale.US, "%.1f", it)} hours." }
                        ?: "Sleep data கிடைக்கவில்லை."
                q.contains("spo2") || q.contains("oxygen") ->
                    hc.latestOxygenSaturation()?.let { "Latest SpO2 is ${String.format(Locale.US, "%.0f", it)} percent." }
                        ?: "SpO2 data கிடைக்கவில்லை."
                q.contains("blood pressure") || q == "bp" || q.contains("pressure") ->
                    hc.latestBloodPressure()?.let { "Latest blood pressure is ${it.first.toInt()} over ${it.second.toInt()}." }
                        ?: "Blood pressure data கிடைக்கவில்லை."
                q.contains("watch") || q.contains("sync") || q.contains("goboult") -> {
                    openApp(); "Wearable sync screen opened. Please refresh Health Connect data."
                }
                else -> "I heard: $raw. Open Chandra in the app for more commands."
            }
            speak(answer)
            if (ChandraVoiceSettings.isAlwaysListeningEnabled(this@ChandraWakeService)) startListening()
        }
    }

    private fun openApp() {
        val launch = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("open_chandra", true)
        }
        try { startActivity(launch) } catch (_: Exception) {}
    }

    private fun recognitionIntent() = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en-IN")
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
    }

    private fun speak(text: String) {
        if (ttsReady) tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "chandra")
    }

    override fun onInit(status: Int) {
        ttsReady = status == TextToSpeech.SUCCESS
        if (ttsReady) tts?.language = Locale("en", "IN")
    }

    private fun stopListening() {
        recognizer?.cancel()
        recognizer?.destroy()
        recognizer = null
    }

    private fun createChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL_ID, "Chandra Voice", NotificationManager.IMPORTANCE_LOW))
    }

    private fun notification(): Notification {
        val pending = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java).putExtra("open_chandra", true),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Chandra is active")
            .setContentText("Say “Hi Chandra” to ask about your Diet Tracker.")
            .setOngoing(true)
            .setContentIntent(pending)
            .build()
    }

    override fun onDestroy() {
        stopListening()
        tts?.stop(); tts?.shutdown(); tts = null
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
