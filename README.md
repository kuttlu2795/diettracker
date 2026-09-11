# Diet Tracker

Android diet and wellness tracker built with Kotlin + Jetpack Compose.

## Android Studio

1. Open this **DietTracker** folder in Android Studio.
2. Use a JDK 17+ installation (Android Studio's bundled JDK is recommended).
3. Allow Gradle to download the configured Gradle 8.6 distribution and dependencies.
4. Add optional API keys to `local.properties` using `local.properties.example`.
5. Sync Project with Gradle Files.
6. Build with **Build > Make Project** or `app > Tasks > build > assembleDebug`.

## Included functionality

- Food search, manual entry, barcode scanning and nutrition-label OCR
- AI nutrition fallback and editable nutrition values
- Recent foods and reusable My Meals / Recipes
- BMI and personalized nutrition targets
- Water logging and scheduled reminders
- Steps via phone sensor
- Health Connect bridge for compatible health/wearable apps
- GOBOULT companion-app bridge design through Health Connect where the companion exposes records
- Health Connect reads for steps, weight, heart rate, sleep, SpO2 and blood pressure

## Important integration note

GOBOULT watches do not expose one universal public Android API that guarantees every model/metric. The app therefore uses the vendor-neutral Android Health Connect path rather than pretending that a direct BLE connection exists for every watch. If a specific GOBOULT companion app/model exposes a metric to Health Connect, Diet Tracker can read it after the user grants permission.

Stress is not represented as a fake Health Connect value because Health Connect does not provide a generic standard stress record. A future vendor-specific connector can map it when an actual source is available.

See `BUILD_READINESS.md` for the audit changes and build-environment limitation.

## Chandra Voice Assistant

Added an Android voice assistant screen using Android SpeechRecognizer + TextToSpeech. It supports English/Tamil/Tanglish-friendly command routing for heart rate, steps, calories, water, weight, sleep, SpO2, BP, wearable sync, fasting, food, exercise, diary and goals. Health metrics are read from the existing Health Connect layer where available.

Note: Android SpeechRecognizer is push-to-talk here; a true always-listening “Hi Chandra” wake word requires a dedicated on-device wake-word engine/foreground service and should be added as a later production phase.
