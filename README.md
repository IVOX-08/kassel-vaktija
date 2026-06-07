# Kassel Vaktija

Native Android prayer-times app for the **IGBD-Gemeinde Sandžak-Kassel**. Android-only project
(iOS is a separate, later project — no iOS code lives here).

- **applicationId:** `de.igbdsandzakkassel.vaktija`
- **minSdk:** 26 (Android 8.0) · **compileSdk / targetSdk:** 35
- **Stack:** Kotlin · Jetpack Compose + Material 3 · MVVM · Hilt · Room · DataStore ·
  WorkManager · AlarmManager · Glance · Retrofit/OkHttp/kotlinx.serialization · Jsoup (fallback) ·
  Firebase (Firestore/Auth/FCM/Storage — wired in Phase 1/4b)

---

## Build status by phase

- [x] **Phase 0 — Project setup** (this commit): builds & runs, themed single-Activity scaffold,
  bottom nav (5 tabs), 7-language localization with in-app switching, TV (leanback) routing,
  Hilt DI root, bundled Inter font, adaptive launcher icon.
- [x] **Phase 1 — Data layer**: vaktija.eu/kassel JSON-LD source + Room cache + daily WorkManager
  refresh (offline-first), behind `PrayerTimesRepository`. (Community overrides still default; Firebase in 4b.)
- [x] **Phase 2 — Dashboard UI**: emblem, countdown hero, prayer cards (Adhan/Iqamah with divider,
  next-prayer highlight), Džuma card. Light/dark, 8 languages, RTL.
- [x] **Phase 3 — Notifications & Adhan**: exact alarms (AlarmManager) per prayer + pre-warnings,
  foreground playback service (placeholder sound — real Adhans TBD, Open Item #4), reschedule on
  boot/time/locale, Settings with per-prayer toggles + pre-warning + permissions + test button.
- [x] **Feature batch**: light/dark/system **theme selector**; Dashboard **auto-scroll + one-shot
  pulse** to the next prayer; **Month Calendar** (full month via adhan2, calibrated to today's
  official value, cached in Room) — replaces the Zakat calculator; bottom nav reordered
  (Home · Calendar · News · Qibla · Settings); pre-warning options 0/5/10/15/30; emblem launcher icon.
- [ ] Phase 4 — Auto-silence / DND
- [ ] Phase 4b — Admin mode & Firebase backend
- [ ] Phase 5 — Widgets (Glance)
- [ ] Phase 6 — Qibla compass
- [ ] ~~Zakat calculator~~ → replaced by the Month Calendar (above)
- [ ] Phase 8 — News & Donations
- [ ] Phase 9 — Polish, accessibility, release prep

---

## Open items — decisions needed from the project owner

These are **not yet implemented** and are tracked in code with greppable tags
(`// TBD-community-rule:`, `// TBD-asset:`, `// TBD-decision:`).

1. **Admin unlock gesture** — proposal: long-press logo ~3 s, then 5 taps. Confirm/change.
2. **Initial seed values** — day-one Fajr, Fajr Iqamah, Džuma, Eid (next 12 months), Friday DND.
3. **Sound choice UI** — per-prayer by default, or behind an "Advanced" toggle?
4. **Default Adhan sounds** — which Adhans ship by default? Provide `.mp3`/`.wav` files.
5. ~~**Logo asset** — community coat-of-arms.~~ ✅ **Provided.** Light + dark variants at
   `res/drawable-nodpi/logo_community.png` and `res/drawable-night-nodpi/logo_community.png`;
   resolved automatically per system theme. (Launcher icon intentionally stays the simpler
   crescent-and-star mark — the full emblem is too detailed to read at app-icon sizes.)
6. **Donation details** — account holder, IBAN, BIC, donation-card wording.
7. **Firebase project** — project ID + admin UID (baked into `BuildConfig`), `google-services.json`.
8. **Translations** — Bosnian + German are translator-quality; `ar`, `tr`, `sq`, `ur`, `ru` are
   best-effort and **need native-speaker review before publishing** (marked in each `values-XX/`).
9. **Future admins** — only the developer has admin rights in v1; extending later is planned.

> Remaining greppable markers in the tree: `// TBD-decision:` for the not-yet-set `ADMIN_UID`,
> google-services wiring, and release signing; the four `ar/tr/sq/ur` translation-review notes;
> and one `// TBD-asset:` on the TV screen for the live clock/table that arrives in Phase 1/2.

---

## Local setup

This repo builds with the toolchain bundled in **Android Studio** — no separate JDK/Gradle needed.

1. Open the project in Android Studio (it uses Gradle **8.10.2** via the wrapper).
2. `local.properties` must point at your Android SDK (already set on the dev machine):
   ```
   sdk.dir=C:\\Users\\<you>\\AppData\\Local\\Android\\Sdk
   ```
3. Build from the command line with the **bundled JDK 21** as `JAVA_HOME`:
   ```powershell
   $env:JAVA_HOME = "C:\Program Files\Android\Android Studio<N>\jbr"
   .\gradlew.bat :app:assembleDebug
   ```
   The APK lands at `app/build/outputs/apk/debug/app-debug.apk`.

### Install & run on a real device

Notifications, exact alarms, DND, sensors and widgets behave differently on real phones than on
emulators — **test on hardware after each phase**, especially Samsung / Xiaomi / OnePlus / Huawei.

```powershell
$adb = "C:\Users\<you>\AppData\Local\Android\Sdk\platform-tools\adb.exe"
& $adb devices                                   # confirm your phone shows up (enable USB debugging)
& $adb install -r app\build\outputs\apk\debug\app-debug.apk
```

---

## Internationalization

- Primary language is **Bosnian** (`res/values/`). Translations live in `res/values-{de,ar,tr,sq,en,ur,ru}/`.
- In-app language switching uses the AndroidX per-app language API
  (`AppCompatDelegate.setApplicationLocales`); the selection persists via the
  `AppLocalesMetadataHolderService` declared in the manifest.
- Supported locales are listed in `res/xml/locales_config.xml`. **Adding a new language** = add a
  `values-XX/strings.xml`, a `<locale>` line there, an `AppLanguage` enum entry, and the tag to
  `resourceConfigurations` in `app/build.gradle.kts`.
- `android:supportsRtl="true"`; Arabic and Urdu render right-to-left.

## Fonts & icon

- **Inter** is bundled as a single variable font (`res/font/inter_variable.ttf`) and instanced to
  weights via `FontVariation`. Bundled rather than downloadable so it works offline and on devices
  **without Google Play Services** (Huawei, Fire TV).
- The launcher icon is an adaptive icon (green background + white crescent + gold star) with a
  monochrome layer for Android 13+ themed icons. This is intentionally distinct from the in-app
  emblem — the coat-of-arms has fine text/detail that doesn't read at app-icon sizes.
- The **community coat-of-arms** (`R.drawable.logo_community`) is shown on the Dashboard and TV
  screens. Light and dark variants live in `res/drawable-nodpi/` and `res/drawable-night-nodpi/`
  and are selected automatically by the system theme.

## Android TV / Fire TV

`UiModeManager.currentModeType == UI_MODE_TYPE_TELEVISION` routes to a TV-only Dashboard
(no bottom nav, no Qibla/Zakat/News/Settings; notifications & DND disabled on TV). The manifest
declares a `LEANBACK_LAUNCHER` entry and marks touchscreen/leanback features as not required.

---

## Permissions the app will request (and why)

Most are introduced in later phases; listed here so expectations are clear up front.

| Permission / access | Why | Phase |
|---|---|---|
| `INTERNET` | Fetch prayer times & community overrides; news images | 0/1 |
| `POST_NOTIFICATIONS` (13+) | Prayer-time & news notifications | 3 |
| `SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM` (12+) | Precise prayer-time alarms; falls back to inexact with a warning if denied | 3 |
| Foreground service (`mediaPlayback`) | Play the full Adhan reliably (notification sound would be cut off) | 3 |
| `ACCESS_NOTIFICATION_POLICY` + DND access (granted in system settings) | Auto-silence around prayer time; Android does **not** allow this silently | 4 |
| `ACCESS_FINE/COARSE_LOCATION` | Qibla bearing from current location (falls back to Kassel centre) | 6 |
| Battery-optimization whitelist (informational) | Keep alarms firing on aggressive OEMs | 3/9 |

## Honest platform constraints

- **DND / auto-silence** requires the user to grant *Do Not Disturb access* once in system settings.
  The app guides them there; it cannot be done silently.
- **Exact alarms** on Android 12+ need `SCHEDULE_EXACT_ALARM`; accuracy also depends on the user
  disabling battery optimization for the app.
- **OEM background killers** (Samsung, Xiaomi, Huawei, OnePlus, Oppo, Vivo) can still delay/kill
  alarms — the first-run flow (Phase 9) walks users through whitelisting.
- **Widgets**: Android has no dedicated lock-screen widget API (unlike iOS WidgetKit). Home-screen
  widgets may appear on the lock screen on some OEMs/launchers depending on user settings.

## Signing & release

Debug builds use the auto-generated debug keystore. A real signing config and R8/minification are
added in **Phase 9**; keystores and `keystore.properties` are git-ignored and must never be committed.

## Firebase

Not yet wired (Phase 1/4b). When ready: add `app/google-services.json`, uncomment the
`google-services` plugin in `app/build.gradle.kts` and the Firebase dependencies, and bake the
admin UID into `BuildConfig.ADMIN_UID`. The real admin protection is the **Firestore Security
Rule** on the server — the in-app unlock gesture is only obfuscation. `google-services.json` is
git-ignored by default (Open Item #7 — decide whether to commit it).
