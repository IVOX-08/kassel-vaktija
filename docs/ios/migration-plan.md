# Kassel Vaktija — iOS / Kotlin Multiplatform migration plan

*Authoritative migration plan. Status basis: `docs/play/launch-readiness.md` section F. Android baseline today: `compileSdk 35`, `minSdk 26`, `targetSdk 35`, single-module app at `app/src/main/java/de/igbdsandzakkassel/vaktija/`. The version catalog already declares `kotlin-multiplatform` (`gradle/libs.versions.toml:139`), so the plugin is available.*

---

## 1. Executive summary, effort & phasing

### What we are building
A phone-only (iPhone, then iPad) iOS edition of Kassel Vaktija, sharing the **Kotlin business logic and most of the Compose UI** via Kotlin Multiplatform (KMP) + Compose Multiplatform (CMP). The Android app keeps shipping unchanged throughout; the migration extracts a `shared` module that **both** the existing Android app and a new SwiftUI-hosted iOS app consume.

### Realistic reuse, given the maps
The package maps are honest about what is *not* portable. A grounded reuse estimate:
- **Near-100% portable:** prayer math (`PrayerTimeCalculator`, `domain/PrayerScheduleCalculator`, `MonthCalendarRepository` calibration math), all domain models, the scraper parser (`VaktijaEuSource`), `CommunityGlossary`, the theme (`Color.kt`, `Theme.kt`), UI-state data classes, `DhikrData`.
- **Portable after mechanical swaps:** `java.time → kotlinx-datetime` everywhere, Hilt→Koin, OkHttp→Ktor, most ViewModels and screens.
- **Needs expect/actual seams:** persistence (Room/DataStore/SharedPreferences), Firebase, assets, locale, sensors/haptics, notifications, background scheduling.
- **Android-only or native iOS rewrite:** foreground-service Adhan playback, AlarmManager, BroadcastReceivers, FCM service, Glance widget, the whole TV board (`ui/tv/*`), ML Kit translation, the Android image compressor, and the permission-heavy `OnboardingPermissions.kt` / `SettingsScreen.kt`.

The launch-readiness doc's "≈ 8–14 weeks for phone-only iOS v1" remains the right order of magnitude. The shareable core is large, but the *un-shareable* pieces (notifications, scheduling, widget, Firebase wiring, Swift app shell, App Store process) are exactly where a beginner spends most of the time.

### The constraint that shapes this plan
The developer is **a KMP/iOS beginner, on a borrowed Intel Mac, for only 3–4 days now, then no Mac for ~1 month.** This is the dominant planning fact. We therefore split the work into:

1. **Mac window (3–4 days) — "Foundation Milestone".** The single highest-value, irreversible-knowledge outcome: a `shared` Kotlin module that **compiles to an iOS framework and runs in the iPhone Simulator inside a minimal SwiftUI app, showing real Kassel prayer times computed by shared Kotlin.** This proves the toolchain end-to-end (Gradle KMP build → Xcode → Simulator on an **Intel** Mac). Everything after this is "fill in features"; getting *here* is the part you cannot do without a Mac.
2. **Mac-less month — preparation, not iOS builds.** All work that needs **no Mac**: refactor the Android codebase *in place* toward KMP-friendliness (kotlinx-datetime, Koin, Ktor, decoupling resources) while it still compiles and ships as the Android app. Plus study (Swift basics, SwiftUI, UNUserNotificationCenter, WidgetKit). None of this requires Apple hardware — it's plain Kotlin/Gradle on the dev's normal Windows machine, verified by the existing Android build.

### Phase overview

| Phase | Where | Mac needed? | Outcome |
|---|---|---|---|
| **0 — Foundation Milestone** | 3–4 day Mac window | **Yes** | `shared` builds an iOS framework; SwiftUI app shows real prayer times in the Simulator (iosX64). |
| **1 — De-Android the core (in place)** | Mac-less month | No | Android app refactored: kotlinx-datetime, Koin, Ktor, resource/BuildConfig decoupling. Still ships on Android. |
| **2 — Persistence & settings to KMP** | Mac-less / next Mac window | Partial | Room-KMP + DataStore-multiplatform + a `KeyValueStore` seam. |
| **3 — Firebase + networking shared** | Next Mac window | Yes (link iOS SDK) | GitLive Firebase, news/rules/admin in common. |
| **4 — Shared Compose UI on iOS** | Next Mac window | Yes | Dashboard, calendar, quran, hadith, dhikr, tasbih, tracker, ramadan, qibla rendering on iPhone. |
| **5 — iOS-native platform features** | Next Mac window | Yes | UNUserNotificationCenter Adhan, BGTaskScheduler, WidgetKit, iOS settings/onboarding, APNs. |
| **6 — Polish, RTL/8-locale QA, App Store** | Next Mac window | Yes | TestFlight → App Store submission. |

> Be realistic with the owner: Phases 3–6 are a *second* multi-week Mac block, not part of the 3–4 day window. The 3–4 days buys exactly one thing — a proven foundation — plus the confidence to keep refactoring Android-side for a month.

---

## 2. Target module / source-set architecture

### Module layout
```
kassel-vaktija/
├── app/                  ← existing Android app (com.android.application)
│                            keeps Glance widget, FGS, AlarmManager,
│                            BroadcastReceivers, FCM, TV board, Hilt-free wiring via Koin-android
├── shared/               ← NEW Kotlin Multiplatform module (the migration target)
│   ├── build.gradle.kts  (org.jetbrains.kotlin.multiplatform + com.android.library
│   │                       + org.jetbrains.compose + compose compiler + serialization + KSP)
│   └── src/
│       ├── commonMain/   ← portable Kotlin: models, prayer math, repos (interfaces + portable impls),
│       │                    ViewModels, Compose-MP screens, theme, DI module declarations (Koin)
│       ├── androidMain/   ← actuals: AssetReader (AssetManager), KeyValueStore, DataStore factory,
│       │                    Room driver, DndController, AlarmScheduler, notifications, BuildConfig flag
│       └── iosMain/       ← actuals: AssetReader (NSBundle), NSUserDefaults store, Room iOS driver,
│                            no-op DND, UNUserNotificationCenter scheduling, CoreMotion heading, etc.
└── iosApp/               ← NEW Xcode project (SwiftUI)
    ├── iosApp.xcodeproj
    └── iosApp/
        ├── iOSApp.swift            (App entry; ComposeUIViewController host)
        ├── AppDelegate.swift       (UNUserNotificationCenter delegate, APNs, BGTaskScheduler)
        ├── NotificationScheduler   (schedules ≤30s-sound local notifications from shared data)
        └── KasselWidget/           (WidgetKit extension target — SwiftUI, separate from shared)
```

A `shared/` directory already exists in the working tree (`?? shared/` in git status) — confirm/clean it before scaffolding.

### Source-set responsibilities

- **`commonMain`** — the bulk of the value. Holds: every domain model, `PrayerTimeCalculator`, `PrayerScheduleCalculator`, `MonthCalendarRepository`, `VaktijaEuSource`, `OfflinePrayerTimesRepository`, all repository **interfaces**, the **expect** declarations, the Koin module *declarations*, the Compose-MP screens/theme, and all 8 ViewModels (on the KMP `androidx.lifecycle.ViewModel`).
- **`androidMain`** — Android **actual** implementations. The Android app's truly platform-only services (`service/*` AlarmManager/FGS/BroadcastReceiver/Glance/FCM, `ui/onboarding/OnboardingPermissions.kt`, `ui/settings/SettingsScreen.kt`, `ui/tv/*`) can stay in the **`app` module's** `androidMain`/main rather than `shared`, to keep `shared` clean.
- **`iosMain`** — iOS **actual** implementations in Kotlin/Native (NSBundle, NSUserDefaults, CoreMotion, UNUserNotificationCenter bridges where Kotlin can call them; otherwise thin and the real work lives in Swift).
- **`iosApp` (Swift/SwiftUI)** — the app shell, the notification scheduler/delegate, APNs, BGTaskScheduler, and the **WidgetKit** extension. These are *not* Kotlin.

### Kotlin/Native targets — **Intel Mac caveat**
The borrowed Mac is **Intel (x86_64)**, so the **Simulator target is `iosX64`**, not `iosSimulatorArm64`. Declare all three so the same code builds for device and both simulator architectures:
```kotlin
listOf(iosX64(), iosArm64(), iosSimulatorArm64()).forEach {
    it.binaries.framework { baseName = "Shared"; isStatic = true }
}
```
For the 3–4 day window you will **build and run `iosX64`** (the Intel-Mac simulator). `iosArm64` (real device) and `iosSimulatorArm64` (Apple-Silicon simulator) still compile but you won't *run* them now. Configure the Xcode "Run Script" / framework embed to select the slice matching the active SDK+arch (the standard KMP `embedAndSignAppleFrameworkForXcode` task handles this).

### What stays Android-only (never moves to `shared`)
`service/alarm/PrayerAlarmReceiver.kt`, `service/alarm/RescheduleReceiver.kt`, `service/audio/AdhanForegroundService.kt`, `service/notification/PushMessagingService.kt`, `service/widget/PrayerTimesWidgetReceiver.kt`, `service/work/NewsCheckWorker.kt`, `service/work/VaktijaRefreshWorker.kt`, `ui/onboarding/OnboardingPermissions.kt`, `ui/settings/SettingsScreen.kt`, `ui/tv/TvDashboardScreen.kt`, `ui/tv/TvHadithViewModel.kt`, and the Glance/RemoteViews widget. iOS gets **separately written native counterparts** (Swift) where a feature has an iOS analogue, and **nothing** where it doesn't (DND auto-toggle, exact-alarm, battery-optimization, widget-pin, TV board).

---

## 3. Dependency replacement table

From the dependency map, the concrete swaps:

| Android dependency | Status | iOS / KMP replacement (concrete artifact) |
|---|---|---|
| `com.google.dagger:hilt-android` + compilers + `hilt.android` plugin | needs-replacement | **Koin**: `io.insert-koin:koin-core` (commonMain), `koin-android` + `koin-androidx-compose` (androidMain). No Gradle plugin, no KSP. |
| `androidx.hilt:hilt-navigation-compose` | needs-replacement | **`koinViewModel()`** from `io.insert-koin:koin-compose-viewmodel` (commonMain). |
| `androidx.hilt:hilt-work` | needs-replacement | Koin `KoinWorkerFactory` (Android only); iOS has no WorkManager. |
| `com.squareup.retrofit2:retrofit` + kotlinx converter | needs-replacement | **Ktor**: `io.ktor:ktor-client-core` (common) + `ktor-serialization-kotlinx-json` + `ktor-client-content-negotiation`. |
| `com.squareup.okhttp3:okhttp` | needs-replacement | Ktor engines: `ktor-client-okhttp` (androidMain), **`ktor-client-darwin`** (iosMain). |
| `okhttp3:logging-interceptor` | needs-replacement | `io.ktor:ktor-client-logging` (common). |
| `org.jsoup:jsoup` | kmp-equivalent | **`com.fleeksoft.ksoup:ksoup`** (KMP jsoup port). *(Note: `VaktijaEuSource` parses JSON-LD, so it actually needs only kotlinx-serialization + Regex — jsoup may not be required at all in shared code; verify.)* |
| `androidx.room:*` | kmp-equivalent | **Room-KMP 2.7+** in commonMain + `androidx.sqlite:sqlite-bundled` driver, KSP2 generates iOS code. (Alt: SQLDelight.) |
| `androidx.datastore:datastore-preferences` | kmp-equivalent | **DataStore-multiplatform** `androidx.datastore:datastore-preferences-core`/`datastore-core` (KMP), iOS file-path factory. |
| SharedPreferences (in `QuranProgress` + several screens) | needs-replacement | **`com.russhwolf:multiplatform-settings`** (NSUserDefaults on iOS) behind a common `KeyValueStore`. |
| `io.coil-kt:coil-compose` / `coil-gif` | kmp-equivalent | **Coil 3** `io.coil-kt.coil3:coil-compose` + `coil-network-ktor3` (+ `coil3:coil-gif`). |
| `com.batoulapps.adhan:adhan2` | already-multiplatform | **Keep**, move dependency to commonMain. |
| `kotlinx-datetime`, `kotlinx-serialization-json`, `kotlinx-coroutines-core` | already-multiplatform | **Keep**, move to commonMain. `coroutines-android` stays androidMain; iOS Main dispatcher comes from core's Darwin support. |
| Compose BOM + `androidx.compose.*` + `material3` + `material-icons-extended` | kmp-equivalent | **Compose Multiplatform**: `org.jetbrains.compose.*`, `org.jetbrains.compose.material3`, `…material:material-icons-extended` (prune to used icons). |
| `androidx.navigation:navigation-compose` | kmp-equivalent | **`org.jetbrains.androidx.navigation:navigation-compose`** (KMP). (Alt: Decompose/Voyager.) |
| `androidx.lifecycle:*-compose`, `*-viewmodel-compose`, `*-runtime` | kmp-equivalent | **`org.jetbrains.androidx.lifecycle:*`** (KMP lifecycle/ViewModel). |
| `androidx.activity:activity-compose` | ios-native | Android-only entry point. iOS uses CMP **`ComposeUIViewController { }`**. |
| `androidx.compose.ui:ui-text-google-fonts` | ios-native | Bundle the **Inter** font as a CMP resource; load via `Font(Res.font.…)`. |
| `com.google.firebase:firebase-bom` / `firestore-ktx` / `auth-ktx` | needs-replacement | **GitLive** `dev.gitlive:firebase-firestore`, `dev.gitlive:firebase-auth` (common; links Firebase iOS SDK via SPM/CocoaPods). |
| `firebase-messaging-ktx` | needs-replacement | Android keeps FCM. iOS = **APNs via Firebase iOS SDK in Swift** (`UNUserNotificationCenter`). Payload/business logic shared. |
| `kotlinx-coroutines-play-services` | drop | Drop (GitLive APIs are already suspend/Flow). |
| `com.google.android.play:app-update` | ios-native | Android only. iOS = App Store; optional version check via App Store lookup API. |
| `com.google.mlkit:translate` / `language-id` | ios-native | iOS: **Apple Translation** + **NaturalLanguage (`NLLanguageRecognizer`)** via expect/actual; **shared Gemini path** (Ktor) as primary cross-platform translator. Admin-only → deferrable. |
| `androidx.glance:glance-appwidget` / `glance-material3` | ios-native | **WidgetKit** (SwiftUI) extension. Share only the data layer. |
| `com.google.accompanist:accompanist-permissions` | needs-replacement | **moko-permissions** (`dev.icerock.moko:permissions(-compose)`) or expect/actual; iOS uses `UNUserNotificationCenter`/`CLLocationManager` auth. |
| Plugins: `com.android.application`, `kotlin.android`, `gms.google-services` | mixed | Android-module-scoped (keep). `shared` uses `kotlin.multiplatform` + `com.android.library`; iOS Firebase config = **`GoogleService-Info.plist`** in Xcode. |
| `kotlin.plugin.compose`, `kotlin.plugin.serialization`, `com.google.devtools.ksp` | keep | Keep; all KMP-capable (KSP2 for Room-KMP iOS). |
| `BuildConfig.*` (`ADMIN_UID`, `GEMINI_API_KEY`, `DEBUG`, `VERSION_NAME`) | replace | **BuildKonfig** (`com.codingfeline.buildkonfig`) or a common `expect val`/object. |

---

## 4. Sequenced migration roadmap (file-by-file)

> Notation: each file is referenced by its current path under `app/src/main/java/de/igbdsandzakkassel/vaktija/…`. "→ commonMain" means move the file into `shared/src/commonMain`. "→ androidMain" means it stays Android (in `shared/androidMain` or in `app`). "expect in commonMain / actual in androidMain+iosMain" denotes a seam.

### Phase 0 — Foundation Milestone (the 3–4 day Mac window)

**Goal:** prove the toolchain. On the **Intel** Mac, get a `shared` KMP module compiling an **iosX64** framework, hosted by a **minimal SwiftUI** app that displays **real Kassel prayer times computed by shared Kotlin** in the iPhone Simulator. Do **not** attempt full UI or features here.

**Do exactly this, in order:**
1. Create the `shared` module (`kotlin.multiplatform` + `com.android.library`), declare `androidTarget()` and `iosX64()/iosArm64()/iosSimulatorArm64()` with a static `Shared` framework.
2. Add to commonMain: `kotlinx-datetime`, `kotlinx-coroutines-core`, `adhan2`, `kotlinx-serialization-json`.
3. **Move a vertical slice** (the smallest end-to-end prayer-times path), converting `java.time → kotlinx-datetime` as you go:
   - `data/model/DailyTimes.kt` → commonMain (kotlinx-datetime swap).
   - `data/model/Prayer.kt` → commonMain (**replace `@StringRes labelRes` with a stable `String` key** — defer real localization).
   - `data/calendar/PrayerTimeCalculator.kt` → commonMain (drop Hilt, kotlinx-datetime, `TimeZone.currentSystemDefault()`).
   - `domain/PrayerScheduleCalculator.kt` → commonMain (kotlinx-datetime).
   - Add a trivial `PrayerTimesService` facade in commonMain that returns "today's times for Kassel" as plain strings — callable from Swift.
4. Generate the Xcode `iosApp` (SwiftUI). Wire the framework embed task (`embedAndSignAppleFrameworkForXcode`). In `ContentView`, call the shared facade and render the six prayer times in a `List`.
5. **Build & run `iosX64` in the iPhone Simulator.** This is the milestone.

**Deliverable:** a screenshot of real prayer times in the iPhone Simulator. Commit the `shared` module skeleton + the converted files. This is the knowledge you cannot reacquire without a Mac.

> Keep the Android app green throughout: the moved files are now consumed by `app` from `shared`. If the kotlinx-datetime swap risks the Android build in the time available, do the moves on a branch and don't merge to the Android release line until Phase 1.

### Phase 1 — De-Android the core, **in place, no Mac** (the Mac-less month)

All of this is plain Kotlin/Gradle, verifiable by the **Android build only**. Goal: when the next Mac window opens, almost nothing platform-agnostic remains to convert.

**1a. Project-wide `java.time → kotlinx-datetime`** (the single biggest cross-cutting change). Touches, per the maps:
`data/model/DailyTimes.kt`, `data/model/CommunityRules.kt`, `data/local/Mappers.kt`, `data/calendar/PrayerTimeCalculator.kt`, `data/repository/MonthCalendarRepository.kt`, `data/remote/VaktijaEuSource.kt`, `data/repository/OfflinePrayerTimesRepository.kt`, `data/repository/FirestoreCommunityRuleProvider.kt`, `data/repository/StubPrayerTimesRepository.kt`, `domain/PrayerScheduleCalculator.kt`, and UI: `ui/calendar/*`, `ui/dashboard/*`, `ui/ramadan/*`, `ui/news/NewsScreen.kt`, plus `service/alarm/AlarmScheduler.kt` and `service/widget/PrayerTimesWidgetReceiver.kt`.
> **Hijri caveat:** `ui/dashboard/DashboardViewModel.kt` uses `java.time.chrono.HijrahDate`, which has **no kotlinx-datetime equivalent**. Keep Hijri formatting behind an `expect fun hijriDate(...)` — Android actual uses `HijrahDate`; iOS actual will use `NSCalendar(.islamicUmmAlQura)`. Do the seam now; implement the iOS actual later.

**1b. Hilt → Koin.** Rewrite the four `di/` modules as Koin module *declarations* (keep the same interface→impl intent):
- `di/DataModule.kt` → common Koin module (cleanest; pure binds).
- `di/NetworkModule.kt` → common Koin module providing the `Json` (verbatim) and a **Ktor client** (15s connect / 20s read; `Logging` plugin gated by a common debug flag).
- `di/DatabaseModule.kt` → **expect/actual DB factory** (Android Context vs iOS path), Koin provider.
- `di/FirebaseModule.kt` → expect/actual (Android `getInstance()` vs GitLive iOS).
Then strip `@Inject/@Singleton/@HiltViewModel/@AndroidEntryPoint/@HiltWorker` from **every** annotated class and convert ViewModels to constructor injection resolved by Koin. On Android, `hiltViewModel()` call sites become `koinViewModel()`.

**1c. OkHttp → Ktor.** `data/remote/VaktijaEuSource.kt` and `data/translate/GeminiTranslator.kt` (port the per-call timeout to Ktor's `HttpTimeout`). Drop `TimeUnit`.

**1d. Decouple Android resources from domain/model types** (so they can later live in commonMain):
- `data/model/Prayer.kt`, `data/settings/ThemeMode.kt`, `data/settings/AlarmSettings.kt` (`AdhanSound`): replace `@StringRes labelRes:Int` with stable **string keys**; resolve labels in the UI layer (CMP `Res.string`). For `AdhanSound`, move `rawResName`→sound-file resolution to the platform playback layer.
- `core/locale/AppLanguage.kt`: drop `@StringRes/@DrawableRes`; map display-name/flag to **CMP resources**. Migrate the 8 flag PNGs (`flag_ba/de/ps/tr/al/gb/pk/ru`) and the `values-XX/` strings into the CMP resources bundle.

**1e. Replace `BuildConfig`** (`ADMIN_UID`, `GEMINI_API_KEY`, `DEBUG`) with **BuildKonfig** or a common `expect` flag. Unblocks `data/repository/AdminController.kt`, `data/translate/GeminiTranslator.kt`, `di/NetworkModule.kt`.

**1f. Pure-common moves** (no behavior change, can go to `shared/commonMain` now since they have no Android deps after 1a–1e):
`data/model/NewsItem.kt`, `data/remote/RemoteVaktijaSource.kt`, `data/repository/CommunityRuleProvider.kt`, `data/repository/NewsRepository.kt`, `data/repository/PrayerTimesRepository.kt`, `data/repository/StubCommunityRuleProvider.kt`, `data/repository/StubPrayerTimesRepository.kt`, `data/translate/CommunityGlossary.kt`, `ui/theme/Color.kt`, `ui/theme/Theme.kt`, `ui/dashboard/DashboardUiState.kt`, `ui/calendar/MonthCalendarUiState.kt`, `ui/ramadan/RamadanUiState.kt`, the `DhikrData` object (split out of `ui/dhikr/DhikrScreen.kt`), `core/locale/AppLanguage.kt`. Delete `core/.DS_Store` and git-ignore it.

**Outcome of Phase 1:** the Android app still builds and ships, but is now Koin-based, Ktor-based, kotlinx-datetime-based, resource-decoupled, and a large chunk physically lives in `shared/commonMain` — all **without a Mac**.

### Phase 2 — Persistence & settings into KMP

- **Room-KMP**: `data/local/DailyTimesEntity.kt`, `data/local/MonthDayTimesEntity.kt` (entities → commonMain), `data/local/PrayerTimesDao.kt`, `data/local/MonthTimesDao.kt` (DAOs → commonMain; `Flow` observe queries port to Room-KMP), `data/local/KasselDatabase.kt` (`@Database` → commonMain; **builder is expect/actual** — Android Context vs iOS file path), `data/local/Mappers.kt` (→ commonMain). Preserve DB name `kassel-vaktija.db` and `fallbackToDestructiveMigration`.
- **DataStore-multiplatform**: `data/settings/SettingsRepository.kt` → commonMain mapping logic; **expect/actual DataStore factory** (Android Context vs iOS file path).
- **KeyValueStore seam** (multiplatform-settings): `data/quran/QuranProgress.kt` (Compose state stays via CMP; SharedPreferences → `KeyValueStore`), plus the inline SharedPreferences in `ui/dhikr/DhikrScreen.kt`, `ui/tasbih/TasbihScreen.kt`, `ui/tracker/PrayerTrackerScreen.kt`, `ui/ramadan/RamadanScreen.kt` — all behind one common interface.
- **Asset reading seam**: `expect class AssetReader` (Android `AssetManager` vs iOS `NSBundle`/CMP resources) — consumed by `data/hadith/HadithRepository.kt` and `data/quran/QuranRepository.kt` (→ commonMain). Migrate the bundled JSON assets (`assets/hadith/<collection>/<lang>.json`, `quran/*.json`) into the shared resources.

> Room-KMP code-gen for iOS requires **KSP2** and a Mac for the iOS target build; you can author the DAOs/entities Mac-less (Android target compiles them), and finish the iOS driver in the next Mac window.

### Phase 3 — Firebase + networking shared (needs Mac to link iOS SDK)

Adopt **GitLive** (`dev.gitlive:firebase-firestore`, `firebase-auth`). Then:
- `data/repository/AdminController.kt` → commonMain (GitLive auth Flow; `ADMIN_UID` from BuildKonfig).
- `data/repository/FirestoreCommunityRuleProvider.kt` → commonMain (cache-first read, snapshot Flow, DEFAULT fallback; kotlinx-datetime formatting replaces `DateTimeFormatter`).
- `data/repository/FirestoreNewsRepository.kt` → commonMain (`android.util.Base64`→`kotlin.io.encoding.Base64`; `android.util.Log`→ KMP logger).
- iOS: add **`GoogleService-Info.plist`** in Xcode and link the Firebase iOS SDK (SPM/CocoaPods) that GitLive delegates to.
- **Deferred admin-only natives:** `data/media/NewsImageCompressor.kt` (expect `compress(): ByteArray`; iOS = UIImage/CoreGraphics/ImageIO) and `data/translate/NewsTranslator.kt` (ML Kit Android; iOS = Apple Translation, or omit and rely on the shared Gemini path). These only matter if iOS gets the **admin posting** flow — ship iOS **read-only** first and defer both.

### Phase 4 — Shared Compose UI on iOS

Convert ViewModels to KMP `androidx.lifecycle.ViewModel` + Koin (`Dispatchers.IO`→`Default`/injected), then bring screens to commonMain. Order by portability:
- **Easy:** `ui/components/PlaceholderContent.kt`, `ui/theme/Type.kt` (Inter via CMP `Res.font`), `ui/navigation/TopLevelDestination.kt`, `ui/library/LibraryScreen.kt`, `ui/components/LanguagePickerDialog.kt`, `ui/KasselApp.kt` (Navigation-Compose-KMP).
- **VMs + screens:** `ui/calendar/MonthCalendarViewModel.kt`+`MonthCalendarScreen.kt`, `ui/dashboard/DashboardViewModel.kt`+`DashboardScreen.kt` (Hijri seam from 1a; `Settings.Global` reduce-motion → `expect rememberReduceMotion()`), `ui/hadith/*`, `ui/quran/*`, `ui/ramadan/*`, `ui/dhikr/DhikrScreen.kt`, `ui/tracker/PrayerTrackerScreen.kt`.
- **Device-capability seams:**
  - `ui/qibla/QiblaScreen.kt`: `expect rememberCompassHeading()` (Android `SensorManager` rotation-vector vs iOS `CoreMotion`+`CoreLocation`); replace `Canvas.nativeCanvas.drawText` with Compose `drawText`/`TextMeasurer`.
  - `ui/tasbih/TasbihScreen.kt`: `expect hapticTick()` (Android `Vibrator` vs iOS `UIImpactFeedbackGenerator`).
  - `ui/news/NewsScreen.kt`/`NewsViewModel.kt`: `expect` image picker (Android `PickVisualMedia` vs iOS `PHPicker`); `Toast`→snackbar; **Coil 3** `AsyncImage`; `Uri`→common bytes/file handle. (Posting deferred per Phase 3.)
  - `ui/onboarding/LanguagePickerScreen.kt`/`OnboardingScreen.kt`: GIF flags via Coil 3 + asset-path seam; `AppCompatDelegate.getApplicationLocales` → common "is locale chosen"; the `else` branch routes to **native iOS permissions** instead of `OnboardingPermissions.kt`.
- **Locale seam:** `core/locale/LocaleController.kt` → expect/actual (Android `AppCompatDelegate`+`AppLocalesMetadataHolderService` auto-persist vs **iOS: NSUserDefaults `AppleLanguages` + explicit persistence + Compose `LayoutDirection` for RTL**). `core/device/DeviceType.kt` → expect (`isTelevisionDevice()`; **iOS actual returns `false`**).

### Phase 5 — iOS-native platform features (Swift)

Build in `iosApp` (no shared Kotlin equivalent):
- **Adhan alarm** → see §5 below. Lift `AlarmScheduler`'s window/Jumua math + `NewsNotificationChecker` dedup logic into commonMain; the Swift side schedules `UNNotificationRequest`s.
- **Background refresh** → `BGTaskScheduler` (`BGAppRefreshTask`) calls a common `RefreshUseCase` (extracted from `VaktijaRefreshWorker.doWork()` / `NewsCheckWorker`).
- **Push** → APNs via Firebase iOS SDK in `AppDelegate` + a `UNNotificationServiceExtension`. Reuse common watermark-gating from `NewsNotificationChecker`.
- **Widget** → WidgetKit `TimelineProvider` (next prayer + countdown via `Text(timerInterval:)`).
- **iOS Settings & onboarding** → native SwiftUI (notification authorization; **no** exact-alarm/battery/DND/widget-pin/Accompanist concepts). Share only theme + per-prayer toggles via the common settings VM.

### Phase 6 — Polish, RTL & 8-locale QA, App Store

RTL verification (Arabic/Urdu via Compose `LayoutDirection`), all 8 locales, language persistence across relaunch on iOS, `GoogleService-Info.plist`, **Apple Developer Program ($99/yr)**, TestFlight, App Store review. Apple TV (tvOS) **stays deferred** — CMP doesn't support it and there is no iOS-side use case for the mosque wall board.

---

## 5. iOS hard limits and per-feature handling

These are platform-level (true in every framework), from `launch-readiness.md` §F and confirmed by the service/UI maps.

| Feature | Android today | iOS reality | iOS approach |
|---|---|---|---|
| **Exact-time Adhan from a killed app** | `AlarmManager` exact alarm → `PrayerAlarmReceiver` → `AdhanForegroundService` plays the **full** Adhan via `MediaPlayer` with `USAGE_ALARM`, screen-off, silent-mode-piercing. | **Impossible.** iOS cannot run code at an exact moment from a killed app, and a background-delivered notification can only play a sound **≤ 30 seconds**. | Pre-schedule `UNUserNotificationCenter` **local notifications** per prayer with a **≤30s Adhan clip** as the notification sound (bundled). The **full** Adhan plays **only when the user taps** the notification and the app foregrounds (`AVAudioPlayer`/`AVAudioSession`). Re-schedule a rolling window (e.g. next 14 days × prayers) on each app open and via `BGAppRefreshTask`, because iOS can't "re-arm on each fire" like `AlarmScheduler` does. Be explicit with the owner: **no full-Adhan-while-locked on iOS.** |
| **Auto Do-Not-Disturb** | `DndController` toggles `NotificationManager.setInterruptionFilter` around prayer time; `recoverStrandedDnd` watermark. | **No API.** Focus modes are not app-togglable. | `DndController` iOS actual is a **no-op** (or a best-effort one-time "set up a Prayer Focus" suggestion). The auto-silence window + stranded-DND recovery become **Android-only**. Calling logic stays common; it simply does nothing on iOS. |
| **Home-screen widget** | Glance/RemoteViews (`PrayerTimesWidgetReceiver`) with live `Chronometer` countdown + `AlarmManager` self-refresh. | Glance has no iOS form; widgets are native. | **WidgetKit** SwiftUI extension with a `TimelineProvider` for next prayer; live countdown via `Text(timerInterval:)` (per-second updates are constrained on iOS widgets — show a counting interval, not a custom Chronometer). The `nextPrayer()` selection logic is shared Kotlin feeding the timeline. |
| **FCM push** | `PushMessagingService` (FCM) + `runBlocking`. | FCM client has no shared API. | **APNs** via the Firebase iOS SDK in a Swift `AppDelegate` + `UNNotificationServiceExtension`. Watermark-gating decision logic lifted into the common `NewsNotificationChecker`; only the messaging shell is native. |
| **Background polling** | `WorkManager` 15-min periodic + one-shot (`NewsCheckWorker`, `VaktijaRefreshWorker`). | No WorkManager; background execution is opportunistic. | `BGTaskScheduler` (`BGAppRefreshTask`) running the common `RefreshUseCase`. Cadence is **best-effort**, not guaranteed 15-min. The reliable alerting path is the **pre-scheduled local notifications**, not polling. |
| **Reschedule on boot/time change** | `RescheduleReceiver` re-arms alarms. | iOS local notifications **survive reboot**; no receiver needed. | Drop entirely on iOS. |
| **Permissions / onboarding** | `OnboardingPermissions.kt`: POST_NOTIFICATIONS, DND access, widget-pin, Settings intents. | Most have **no iOS analogue**. | Native SwiftUI flow: only `UNUserNotificationCenter` notification authorization (and `CoreLocation` for Qibla heading). No DND/exact-alarm/battery/widget-pin steps. |
| **Apple TV / mosque wall board** | `ui/tv/*` Android-TV board. | tvOS unsupported by CMP; no iOS use case. | **Deferred / dropped** from the iOS target. `isTelevisionDevice()` returns `false` on iOS, so all common code branching on TV silently takes the phone path (intended). |
| **Per-app language + RTL** | `AppCompatDelegate.setApplicationLocales` (+ auto-persist service). | Different mechanism; no auto-persist. | iOS actual writes `AppleLanguages` to `NSUserDefaults`, persists the choice explicitly, drives Compose `LayoutDirection` for Arabic/Urdu, and recomposes/restarts. **Verify language survives relaunch** — the Android auto-persistence has no iOS equivalent. |

---

## 6. Risks, open questions, and the Mac-less month

### Top risks
1. **java.time is everywhere.** The kotlinx-datetime migration (Phase 1a) is the single largest change and touches nearly every model, repository, ViewModel, and the scheduler/widget. It is **not** a name swap — `LocalDateTime.of`/`toLocalDate()`/`isAfter`/`YearMonth`/`DateTimeFormatter`/`HijrahDate` each need rewriting and re-verifying. Mitigate by doing it **first, Mac-less, with the Android build as the regression oracle.**
2. **Hijri calendar gap.** `HijrahDate` (DashboardViewModel) has no kotlinx-datetime equivalent → mandatory `expect`/`actual` (Android `HijrahDate`, iOS `NSCalendar` Umm al-Qura). Easy to forget; surface it as a seam in Phase 1.
3. **GitLive Firebase API drift.** GitLive's snapshot listeners, `Source.CACHE`, and `getLong/getString` accessors differ subtly from the Android SDK; the three Firestore/Auth files **will** need code changes even where they "mostly" port. Linking the iOS Firebase SDK (SPM/CocoaPods) is itself a Mac-only, fiddly step.
4. **Intel-Mac performance + lifespan.** Kotlin/Native + Xcode builds are slow on an Intel Mac; the 3–4 day window is tight. **Scope Phase 0 ruthlessly** to the prayer-times slice only. Also: only `iosX64` runs in the Simulator there — don't burn time fighting `iosSimulatorArm64`.
5. **Adhan UX regression is unavoidable.** The ≤30s-sound limit means iOS users get a fundamentally weaker Adhan than Android. This is a **product expectation** issue, not a bug — confirm the owner accepts it before investing.
6. **Cross-package leakage.** `core.locale.AppLanguage`, `core.locale.LocaleController`, `core.device.isTelevision()`, and `BuildConfig` are imported across data/service/ui. The `core` package must migrate **before or alongside** the packages that depend on it, or nothing downstream compiles cross-platform.
7. **Coil 3 GIF parity** for the waving-flag onboarding GIFs is unverified on iOS — may need static flags as a fallback.
8. **Admin-only natives (image compress + ML Kit translate)** are real work with no shared form. **Ship iOS read-only first** and defer both; this removes a large chunk of Phase 3/4 risk.

### Open questions (decide before/early)
- **iOS scope of v1:** read-only (recommended) vs. full admin posting? Read-only defers `NewsImageCompressor`, `NewsTranslator`, the image picker, and admin auth.
- **Room-KMP vs SQLDelight?** Recommend Room-KMP (lowest churn — DAOs port directly), accepting the KSP2/iOS-codegen Mac dependency.
- **DataStore-multiplatform vs multiplatform-settings?** Recommend DataStore for `SettingsRepository` (near drop-in) and multiplatform-settings only for the small per-screen flags.
- **Navigation:** stay on Navigation-Compose-KMP (lowest churn) vs. Decompose/Voyager.
- **`PrayerScheduleCalculator` TBD:** the unresolved comment about whether the countdown should also stop at Sunrise/Izlazak — resolve the **product** decision independently so the KMP rewrite doesn't silently change semantics.
- **Apple Developer account ownership:** personal vs the association (mirrors the Play account D-U-N-S decision in §B of launch-readiness).

### What the beginner can do during the Mac-less month (no Apple hardware required)
**Refactor (all verified by the Android build):**
- Phase 1 in full: java.time→kotlinx-datetime; Hilt→Koin; OkHttp→Ktor; decouple `@StringRes`/`R`/`BuildConfig` from `Prayer`/`ThemeMode`/`AlarmSettings`/`AppLanguage`; move all pure-common files into `shared/commonMain`.
- Author Phase 2 entities/DAOs and the expect/actual **declarations** (AssetReader, KeyValueStore, DataStore factory, DB factory, `rememberReduceMotion`, `rememberCompassHeading`, `hapticTick`, `hijriDate`) — the Android actuals compile and run; the iosMain actuals are stubbed for the next Mac window.
- Migrate flag PNGs, the Inter font, and `values-XX/` strings + the hadith/quran JSON into the **CMP resources** bundle.

**Study (so the next Mac window is productive):**
- **Swift + SwiftUI basics** (views, `List`, state) — needed for the app shell, settings, onboarding, widget.
- **`UNUserNotificationCenter`** (scheduling local notifications, ≤30s sounds, categories/actions, the delegate) — the heart of the iOS Adhan.
- **WidgetKit** `TimelineProvider` + `Text(timerInterval:)`.
- **`BGTaskScheduler`** (`BGAppRefreshTask`).
- **CMP iOS interop**: `ComposeUIViewController`, the framework-embed Gradle tasks, and how Swift calls Kotlin.
- **GitLive Firebase setup** (SPM/CocoaPods, `GoogleService-Info.plist`) and **Apple Developer enrollment** ($99/yr) paperwork.

**Net:** by the time a Mac is available again, the only work left genuinely *needs* a Mac — iOS actuals, the SwiftUI shell, notifications/widget/APNs, and App Store submission — because everything platform-agnostic was finished Mac-less against the Android build.