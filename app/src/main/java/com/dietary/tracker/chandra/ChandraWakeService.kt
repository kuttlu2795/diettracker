package com.dietary.tracker.chandra

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.dietary.tracker.DietTrackerApp
import com.dietary.tracker.MainActivity
import com.dietary.tracker.R
import com.dietary.tracker.chandra.wakeword.SpeechRecognizerWakeWordEngine
import com.dietary.tracker.chandra.wakeword.WakeWordEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Foreground service backing the "Always Listen for Hi Chandra" setting.
 *
 * IMPORTANT - read this before assuming this behaves like Alexa/Google Assistant's wake word:
 * the [WakeWordEngine] this drives ([SpeechRecognizerWakeWordEngine] by default) is a documented
 * fallback built from Android's standard SpeechRecognizer, NOT a dedicated low-power keyword
 * spotter. It only keeps running while this foreground service is alive - which Android allows
 * with a persistent notification (shown below) - and even then OEM battery optimization can still
 * kill it. See [SpeechRecognizerWakeWordEngine]'s doc comment and the README for the full
 * limitation and what a production swap-in (e.g. Picovoice Porcupine) would require.
 *
 * On wake-word detection, this captures one command via [ChandraSpeechListener], executes it
 * through the exact same [ChandraVoiceRepository] the on-screen Chandra tab uses, and speaks the
 * result via [ChandraTts]. It cannot reliably bring the app's UI to the foreground from the
 * background on modern Android (background-activity-launch restrictions) - the response is
 * delivered by voice and by updating the persistent notification's text instead, per Android's
 * own rules. This is stated plainly rather than pretending otherwise.
 */
class ChandraWakeService : Service() {

    companion object {
        const val CHANNEL_ID = "chandra_listening_channel"
        const val NOTIFICATION_ID = 7001
        const val ACTION_STOP = "com.dietary.tracker.chandra.ACTION_STOP_LISTENING"

        fun start(context: Context) {
            val intent = Intent(context, ChandraWakeService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, ChandraWakeService::class.java))
        }
    }

    private var wakeWordEngine: WakeWordEngine? = null
    private var speechListener: ChandraSpeechListener? = null
    private var tts: ChandraTts? = null
    private lateinit var voiceRepository: ChandraVoiceRepository
    private val serviceScope = CoroutineScope(Dispatchers.Main)
    private var commandJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val app = application as DietTrackerApp
        voiceRepository = ChandraVoiceRepository(app.repository, app.nutritionRepository)
        tts = ChandraTts(this)
        speechListener = ChandraSpeechListener(this)
        wakeWordEngine = SpeechRecognizerWakeWordEngine(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(NOTIFICATION_ID, buildNotification("Listening for \"Hi Chandra\"…"))
        wakeWordEngine?.start(
            onWakeWordDetected = { onWakeWordDetected() },
            onError = { message -> updateNotification("Chandra: $message") }
        )
        return START_STICKY
    }

    private fun onWakeWordDetected() {
        wakeWordEngine?.stop()
        updateNotification("Yes, I'm listening…")
        tts?.speak("Yes, I'm listening.")

        speechListener?.startListening(
            onResult = { heard -> handleCommand(heard) },
            onError = { message ->
                updateNotification("Chandra: $message")
                tts?.speak(message)
                resumeWakeWordListening()
            }
        )
    }

    private fun handleCommand(heardText: String) {
        commandJob = serviceScope.launch {
            val app = application as DietTrackerApp
            val customCommands = app.repository.getAllChandraCustomCommandsOnce()
            val parsed = ChandraCommandParser.parse(heardText, customCommands)
            val result = voiceRepository.execute(parsed, this@ChandraWakeService)

            app.repository.addChandraRecentCommand(
                com.dietary.tracker.data.entities.ChandraRecentCommand(
                    timestamp = System.currentTimeMillis(),
                    heardText = heardText,
                    actionId = parsed.actionId?.name,
                    response = result.spokenText
                )
            )

            updateNotification("Chandra: ${result.spokenText}")
            tts?.speak(result.spokenText)
            resumeWakeWordListening()
        }
    }

    private fun resumeWakeWordListening() {
        // Give TTS a moment to finish before re-arming the wake-word engine, so it doesn't
        // immediately "hear" Chandra's own spoken response.
        android.os.Handler(mainLooper).postDelayed({
            updateNotification("Listening for \"Hi Chandra\"…")
            wakeWordEngine?.start(
                onWakeWordDetected = { onWakeWordDetected() },
                onError = { message -> updateNotification("Chandra: $message") }
            )
        }, 2500L)
    }

    private fun buildNotification(text: String): Notification {
        ensureChannel()
        val openAppIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this, 1, Intent(this, ChandraWakeService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle("Chandra")
            .setContentText(text)
            .setOngoing(true)
            .setContentIntent(openAppIntent)
            .addAction(android.R.drawable.ic_media_pause, "Stop listening", stopIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(text: String) {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification(text))
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID, "Chandra Voice Assistant", NotificationManager.IMPORTANCE_LOW
                ).apply { description = "Shows when Chandra is listening for \"Hi Chandra\"" }
                manager.createNotificationChannel(channel)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        commandJob?.cancel()
        wakeWordEngine?.stop()
        speechListener?.stop()
        tts?.shutdown()
    }
}
