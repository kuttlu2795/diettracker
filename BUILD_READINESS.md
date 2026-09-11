# Diet Tracker — Android Studio Readiness

## Project configuration
- Android Gradle Plugin: 8.4.1
- Kotlin: 1.9.24
- Gradle distribution: 8.6
- compileSdk / targetSdk: 34
- minSdk: 26
- Jetpack Compose compiler: 1.5.14
- Room KSP: 2.6.1 / KSP 1.9.24-1.0.20

## Fixes applied in this audit
1. Added `kotlinx-coroutines-play-services` because ML Kit Task `.await()` extensions require the Play Services coroutine bridge.
2. Added Health Connect read permissions for blood pressure and oxygen saturation.
3. Added Health Connect reads for the latest blood-pressure and SpO2 records.
4. Added Android 13+ notification permission request so water reminders can actually display when enabled.
5. Kept wearable data honest: stress is not represented as a fabricated Health Connect value.
6. Kept Health Connect as the vendor-neutral bridge for compatible GOBOULT companion data.

## Build
Open the `DietTracker` folder in Android Studio and let Gradle sync. Then run:
`app > Tasks > build > assembleDebug`

The supplied environment did not contain an Android SDK/Gradle executable, so a local APK compilation could not be executed in this session. This archive therefore does not claim a successful APK build test; it contains the source/configuration fixes above.
