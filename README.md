# Diet Tracker (Android)

A complete, native Android Studio project (Kotlin + Jetpack Compose) implementing:

- **Dashboard** – animated rings for steps, calories burnt, calories intake, water, protein
- **Food entry** – search any food by free text; tries Open Food Facts + Edamam first, and if
  nothing matches, offers an **AI estimate (Gemini, free tier)** or manual entry so no food is
  ever "not found." Quick customizers for **eggs** (count, yolk in/out, salt, pepper) and
  **juice** (type, added sugar or not, serving size) turn a few taps into a precise description
  before it's looked up. Every macro field is editable so AI/manual values can be corrected.
  A **Recent foods** list lets you re-log something you eat often in one tap. Running daily
  totals (sugar/protein/calories/etc.) add up as you log more.
- **Barcode scanning** – CameraX + ML Kit barcode scanning, looked up against Open Food Facts
- **Photo label scanning** – take/choose a photo of a nutrition facts panel, ML Kit OCR extracts
  calories/sugar/protein/fat/carbs/fiber/sodium automatically
- **History** – view by Today / Yesterday / Week / Month / Year, with an animated bar chart and entry list
- **BMI calculator** – live BMI + category from height/weight
- **Weight trend & calorie balance** – BMR (Mifflin-St Jeor) + TDEE + steps-based extra burn vs.
  intake gives an estimated daily weight change (kg), plus a weight-over-time line chart
- **Steps** – reads the phone's built-in step-counter sensor (no extra permissions/app needed
  beyond Activity Recognition on Android 10+)
- Material 3 theme, light/dark support, animated progress rings/bars/lines throughout

## How to build the APK

You need a PC with **Android Studio** (free, from developer.android.com/studio) — this is the
standard tool for building Android apps and is required because compiling an APK needs the
Android SDK and build tools.

1. Unzip this project.
2. Open Android Studio → **File → Open** → select the unzipped `DietTracker` folder.
3. Let Gradle sync (Android Studio will auto-download the Gradle wrapper, Kotlin, and all
   dependencies the first time — this needs internet access and can take a few minutes).
4. Plug in an Android phone (USB debugging on) or use an emulator, then click **Run ▶**.
   OR
5. To get a plain `.apk` file: **Build → Build Bundle(s)/APK(s) → Build APK(s)**.
   The file appears at `app/build/outputs/apk/debug/app-debug.apk` — copy it to your phone
   and install it (enable "Install unknown apps" for your file manager/browser).

For a signed release APK: **Build → Generate Signed Bundle / APK**, follow the wizard to
create a keystore, choose APK, and build the `release` variant.

## Chandra Voice Assistant

A tap-the-mic (and optional always-listening) voice assistant, reachable via the small orange mic
button on the Dashboard.

**What it does today:**
- Understands English, Tanglish, and common Tamil-romanized phrases ("evlo", "sollu", "pannu",
  "enna") via a rule-based, fully on-device parser (`ChandraCommandParser`) - no command text is
  sent to any NLP service.
- Every action calls the *exact same* `Repository` / `HealthConnectManager` / `NutritionRepository`
  functions the on-screen UI uses (`ChandraVoiceRepository`) - Chandra and the UI always show the
  same real data, and nothing is ever a fake/dummy value. If data isn't available (no Health
  Connect heart-rate reading, for example), Chandra says so explicitly.
- Covers all 23 actions from the spec: heart rate, steps, weight, sleep, SpO2, blood pressure, add
  food (via the same Open Food Facts → Edamam → Gemini chain as manual entry), calories/protein/
  water status, add water, open diary, add exercise (new: real MET-based calorie calc, see
  `ExerciseCalculator`), start/end/status fasting (new: real session tracking), read/set all four
  goals, daily/weekly report + progress navigation, and watch/Health Connect sync.
- Responds by voice (Android TextToSpeech) and on-screen text.
- "Chandra Voice Commands" screen lets you view every feature's default phrases and add/edit/
  delete your own custom aliases per feature (e.g. add "My pulse" → Heart Rate), stored in Room.
- "Always Listen for Hi Chandra" runs a real foreground service (`ChandraWakeService`) with the
  required persistent notification and a Stop action.

**Please read before relying on "Always Listen" in production** — this is the one place the spec
explicitly asked to be documented rather than faked, so here it is plainly: Android has no public
API for a true low-power, always-on wake-word engine. What ships here
(`SpeechRecognizerWakeWordEngine`) is a *documented fallback* that repeatedly restarts Android's
standard `SpeechRecognizer` in short bursts and checks each transcript for "chandra". Concretely:
- It uses meaningfully more battery than Alexa/Google Assistant's dedicated hardware/DSP wake-word
  detection.
- It is **not guaranteed to keep working with the screen off or the app backgrounded** - Android
  restricts background microphone access, and OEM battery managers frequently kill background
  services regardless of the foreground-service notification. The persistent notification is what
  makes this *legal* under Android's rules; it doesn't make OEMs stop throttling it.
- There's a small gap between each restart where a spoken wake word can be missed.
- A real production build should swap in a dedicated on-device engine (e.g. Picovoice Porcupine,
  which supports training a custom "Hi Chandra" keyword and needs a free personal AccessKey from
  console.picovoice.ai). The code is already structured for this: everything that drives wake-word
  listening codes against the `WakeWordEngine` interface, not a concrete class, so dropping in a
  real engine is a matter of implementing that interface and changing one line in
  `ChandraWakeService` - no other Chandra code needs to change.
- When the wake word fires with the phone locked, Chandra cannot reliably bring its own UI to the
  foreground - Android's background-activity-launch restrictions prevent that. The response is
  instead delivered via voice (TTS) and the persistent notification's text, which is what the app
  actually does rather than claiming otherwise.

**Speech-to-text note:** whether `SpeechRecognizer` transcribes on-device or via network depends
entirely on the phone/OS and which recognition service is installed (standard Android platform
behavior, not something this app controls). This app makes no additional network calls of its own
for speech recognition, and command *parsing* always happens on-device either way.

**New small features added to support real (not fake) voice actions:** basic exercise logging
(name, duration, MET-based calories) and fasting session tracking (start/stop/target hours) -
Repository-backed, real data, currently driven by voice/tests rather than having their own
dedicated screens yet.

## Notes

- **minSdk 26** (Android 8.0+), **targetSdk 34**.
- **Nothing is hardcoded** — all food, nutrient, and meal-plan data comes from live online APIs:
  - **Open Food Facts** (free, no key/signup needed) — packaged/branded product search + barcode lookups.
  - **Edamam Nutrition Analysis API** (free tier, needs a free key) — natural-language nutrition
    parsing for fruits, home-cooked meals, and generic ingredients ("1 medium banana", "100g grilled
    chicken breast").
  - **Spoonacular Meal Planner API** (free tier, needs a free key) — generates a real, algorithm-picked
    daily meal plan (Veg and Regular/non-veg) sized to your calculated calorie target, shown on the
    Plan tab with live recipe titles, prep time, and source links.
  - **Google Gemini API** (`gemini-1.5-flash`, genuinely free tier, needs a free key) — AI fallback
    nutrition estimate for any food description that Open Food Facts and Edamam can't match. Used
    for the Add Food AI-estimate button and for the egg/juice quick customizers. Every AI result is
    clearly badged and every field stays editable before saving.
  - **ML Kit** (on-device, Google, free) — barcode scanning and OCR text recognition for nutrition
    label photos. This is on-device inference, not a static table.
- **Add your free API keys**: open (or create) `local.properties` at the project root and add:
  ```
  EDAMAM_APP_ID=your_edamam_app_id
  EDAMAM_APP_KEY=your_edamam_app_key
  SPOONACULAR_API_KEY=your_spoonacular_api_key
  GEMINI_API_KEY=your_gemini_api_key
  ```
  See `local.properties.example` for the full template and sign-up links. The keys are read into
  `BuildConfig` at build time — they are never committed or hardcoded in source (`local.properties`
  is git-ignored). Without them, Open Food Facts search/barcode still works out of the box; the
  Edamam natural-language search and the Plan tab's meal suggestions will show a short message
  explaining how to enable them.
- OCR label reading is heuristic (regex over ML Kit's recognized text) — always double-check the
  extracted numbers before saving, especially on blurry photos.
- Camera, Activity Recognition, and Notification permissions are requested at runtime.
- Room (SQLite) is used for all local storage (your logged entries, profile, goals) — that data
  stays on-device; only nutrition/meal lookups go over the network.
- **Favorites & Recent** — save any food as a Favorite (⭐ button in the food editor) or just
  re-tap something from your Recent list on the Add Food screen for one-tap logging.
- **Micronutrients** — when Edamam provides them, Vitamin C, A, Calcium, Iron, Potassium,
  Magnesium, Zinc, Vitamin D, B12, and Folate are stored per entry and summed on the Dashboard
  against general adult daily-reference values.
- **CSV export** — Profile → "Export Data (CSV)" shares your full food/water/weight log via the
  Android share sheet (save to Drive, email it, open in Sheets, etc.).
- **Wearable sync (Health Connect)** — Profile → "Wearable Sync" reads today's steps, latest
  weight, and active calories from Android's Health Connect and folds them into this app's own
  data. This only works if your wearable's companion app writes to Health Connect on your phone.
  **boAt watches specifically**: boAt's Android companion app (boAt Wearables / ProGear, depending
  on your watch model) does not currently have publicly confirmed Health Connect support — its
  iOS app connects to Apple HealthKit, but Android/Health Connect support isn't documented at the
  time of writing. If your boAt app doesn't show a Health Connect option yet, this card will
  simply have nothing to sync; this app's own phone step sensor (on the Home tab) keeps working
  regardless, and you can always log steps/weight manually. If boAt adds Health Connect support,
  no app update is needed here — sync will start working automatically.
  **GoBoult (Boult) watches, e.g. Trail Pro**: same situation — its companion app (Boult Fit /
  GOBOULT Fit) lists Bluetooth-only sync with no Health Connect support found as of this build.
  Check the Boult Fit app's own settings for a Health Connect toggle in case one was added later;
  otherwise this app's phone step sensor and manual entry keep working, and Trail Pro data will
  start appearing automatically if/when Boult adds Health Connect support. Real specs for both
  brands' current models are listed in Profile → "See supported watch brands & specs" — this app
  has no watch-specific code, it just reads whatever's genuinely in Health Connect.
- **Health Connect dependency risk**: the `androidx.health.connect:connect-client` library was
  still evolving as of this build. If Gradle can't resolve the pinned version, check
  https://developer.android.com/health-and-fitness/guides/health-connect/setup for the current
  version and update it in `app/build.gradle`.

## Project structure

```
app/src/main/java/com/dietary/tracker/
  data/            Room entities, DAOs, database, Repository
  network/         Open Food Facts API, built-in food table, nutrition models
  ocr/             ML Kit text recognition + label parsing
  barcode/         ML Kit barcode analyzer for CameraX
  sensors/         Step counter sensor manager
  util/            BMI/BMR/TDEE/weight-change formulas, date range helpers
  ui/
    theme/         Colors, typography, Material3 theme
    components/    Reusable progress rings, stat cards, custom animated charts
    navigation/    Bottom-nav + NavHost wiring all screens
    screens/       dashboard, foodentry, history, bmi, profile (each with a ViewModel)
```

## Extending it further

- Swap the OCR regex parser for a cloud nutrition-label API for higher accuracy.
- Add a proper "portion size" picker per food (cup, bowl, piece) instead of grams-only.
- Sync to a backend (Firebase/Supabase) if you want multi-device or cloud backup.
- Add notifications/reminders (drink water, log meals) with WorkManager.
