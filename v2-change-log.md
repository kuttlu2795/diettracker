# Diet Tracker v2 change log

## Added
- My Meals / Recipe Builder using Room.
- Health Connect permission flow and import of steps, weight, heart rate and sleep sessions.
- Wearables & Health Connect screen.
- GOBOULT/Boult profiles: Crown R Pro 2, Drift+, Rover Ultra.
- README architecture notes and current integration caveat: Boult companion app is the bridge; direct BLE sync is not claimed without an official data API.
- Updated HTML report and interactive HTML mock.

## Validation note
The uploaded project did not contain a Gradle wrapper and this environment does not have the Android SDK/Gradle toolchain configured, so a full Android APK build was not possible here. Source-level structure, Room wiring, manifest permissions, navigation and generated HTML artifacts were checked. Open the project in Android Studio and run Gradle sync/build before device deployment.

## Full MyFitnessPal-style mock update — 2026-09-11
- Added `diet-tracker-full-mock.html` covering Today, Diary, Add Food, barcode, Meal Scan, Voice Log, quick macros, exercise, recipes/my meals, meal planner, grocery-list action, progress/export, fasting, goals, settings, health vitals and wearable sync.
- Mock wearable pipeline: GOBOULT Watch → GOBOULT Fit/GOBOULT Track → Google Fit (where supported) → Health Connect → Diet Tracker.
- Mock metrics include steps, heart rate, sleep, SpO2, stress and blood pressure; production code must only ingest metrics actually exposed by the selected GOBOULT model/app and should label consumer BP/SpO2 as wellness data.
