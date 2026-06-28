# iOS foundation — status & how to continue

_Last updated: 2026-06-28 · Repo: https://github.com/IVOX-08/kassel-vaktija (private)._

## Kurzfassung (Deutsch)

Das iOS / Kotlin-Multiplatform-**Fundament steht und ist auf GitHub gesichert**:

- Das gemeinsame Kotlin-Modul `:shared` baut ein echtes **iOS-Framework** (Gebetszeiten-Rechner,
  geteilt mit der Android-App).
- Die **iPhone-App** (SwiftUI, Ordner `iosApp/`) **baut und linkt** sauber gegen dieses Framework
  (`BUILD SUCCEEDED`).
- **Bekannte Grenze:** Der iOS-**Simulator startet auf dem geliehenen Intel-Mac nicht**
  (`launchd_sim ... could not bind to session` — ein macOS/Intel-Problem, **nicht** der Code).
  Auf einem Apple-Silicon-Mac (dein nächster Mac) läuft die App im Simulator.
- Der detaillierte Weiterbau-Plan steht in [`migration-plan.md`](migration-plan.md).

## What works (verified on the borrowed Mac)

- **Toolchain:** JDK 17 (Temurin), Xcode 16.4, Gradle 8.10.2, Kotlin 2.0.21.
- **`:shared` KMP module** with source sets `commonMain` / `jvmMain` / `iosMain`
  (targets: jvm, iosX64, iosArm64, iosSimulatorArm64):
  - `PrayerTimesCalculator`, `DailyPrayerTimes`, `prayerRowsForToday()`, `platformName()`
    (ported off java.time onto kotlinx-datetime so it runs on iOS).
  - `./gradlew :shared:jvmTest` → **green** (computes real Kassel prayer times).
  - `./gradlew :shared:linkDebugFrameworkIosX64` → builds **Shared.framework**.
- **`iosApp/`** SwiftUI app (`ContentView.swift` does `import Shared` and lists today's times):
  - `xcodebuild ... -sdk iphonesimulator build` → **BUILD SUCCEEDED** (links Shared.framework).
- The build includes `:app` only when an Android SDK is present, so `:shared` builds on a Mac
  without the Android SDK (see `settings.gradle.kts`).

## Known blocker

- The iOS **Simulator will not boot** on this borrowed **Intel** Mac (macOS 15.7.7 + Xcode 16.4 +
  iOS 18.6 runtime): `Unable to boot ... launchd_sim may have crashed ... could not bind to
  session`. The runtime ships x86_64, and the app builds & links fine — only the live run in the
  simulator is blocked. Expected to work on an Apple-Silicon Mac.

## How to continue on any Mac

```bash
# 1. Get the project
git clone https://github.com/IVOX-08/kassel-vaktija.git
cd kassel-vaktija

# 2. Build + test the shared logic (needs only a JDK, no Xcode/Android SDK)
./gradlew :shared:jvmTest

# 3. Build the iOS framework (needs Xcode)
./gradlew :shared:linkDebugFrameworkIosX64

# 4. Generate + open the iOS app, then press Run on an iPhone simulator
cd iosApp
xcodegen generate
open iosApp.xcodeproj
```

Feature-by-feature roadmap (Hilt→Koin, Retrofit→Ktor, notifications, widget, etc.):
[`migration-plan.md`](migration-plan.md).

## Before returning the borrowed Mac (cleanup checklist)

- `gh auth logout` — sign out of GitHub, then delete the project folder.
- Sign out of the Apple ID used by `xcodes` (stored in the login keychain) if desired.
- Optional: remove the dev tools (`brew uninstall xcodegen gh aria2 temurin@17`, delete
  `/Applications/Xcode-16.4.0.app`), and clear `~/Downloads`.
