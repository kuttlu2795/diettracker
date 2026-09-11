

## Always-listening toggle

Chandra now has an **Always listen for “Hi Chandra”** switch. When ON, the app starts a foreground microphone service that keeps a lightweight speech recognizer active and looks for “Hi Chandra”, “Hey Chandra”, or “Chandra”. It can continue while the phone is locked after the service has been started from the app. When OFF, the service is stopped. Android displays an ongoing microphone-service notification while it is active.

This implementation uses Android SpeechRecognizer as a compatibility fallback; it is not a dedicated low-power hardware wake-word engine. OEM battery policies can affect reliability, and the service should not be described as a guaranteed Google-Assistant-style hotword implementation.
