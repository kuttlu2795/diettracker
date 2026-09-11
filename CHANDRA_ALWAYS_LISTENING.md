# Chandra Always-Listening Mode

## User control
Profile -> Chandra Voice Assistant -> **Always listen for “Hi Chandra”**.

- OFF: Chandra background microphone service is stopped.
- ON: Chandra starts a foreground microphone service and listens for “Hi Chandra”, “Hey Chandra”, or “Chandra”.
- The mode can continue while the phone is locked after it was enabled while the app was visible.
- Android shows an ongoing notification while the microphone foreground service is active.

## Important Android limitation
This version uses Android `SpeechRecognizer` as a compatibility fallback. It is **not** a dedicated low-power hardware wake-word engine. Android/OEM background policies may affect continuous recognition and battery usage. A production release should replace the continuous recognizer loop with a dedicated on-device wake-word engine if true low-power hotword behavior is required.

## Lock screen
Chandra can answer supported Health Connect queries from the foreground service and attempts to open the app to the Chandra screen. Android may restrict background activity launches on some devices; in that case the notification remains the safe user-visible entry point.
