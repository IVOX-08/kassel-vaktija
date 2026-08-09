# Kassel Vaktija — Play Store launch readiness

Status as of 2026-06-10. Based on a multi-agent policy audit (adversarially verified).
Artifacts already produced live in `docs/play/`:
- `play_icon_512.png` (512×512 store icon)
- `feature_graphic_1024x500.png`
- `store-listing.md` (title + short/full descriptions, DE/BS/EN)
- `privacy-policy.html` (bilingual DE/EN, ready to host)
- signed AAB at `app/build/outputs/bundle/release/app-release.aab`

---

## A. Hard blockers (must do before first production submission)

### A1. Foreground-service declaration **with demo video** — #1 rejection cause
The Adhan plays via a `mediaPlayback` foreground service. Apps on Android 14+ MUST declare each
FGS type in Play Console → **App content → Foreground service types**, including a **link to a
screen-recording**. Ready-to-paste answers:
- **Type:** Media playback
- **Functionality:** *"Plays the Adhan (Islamic call to prayer) audio when a scheduled prayer time
  arrives. The user opts in by enabling prayer-time notifications and choosing the Adhan sound."*
- **User impact if deferred/interrupted:** *"The Adhan would be silenced or play late, which defeats
  the core purpose of a prayer-times app — alerting the user at the exact prayer moment."*
- **Demo video (must record on a phone):** enable a prayer alert in Settings → let the alarm fire →
  show the Adhan playing with the ongoing media notification + **Stop** button visible. Upload as an
  unlisted YouTube/Drive link and paste it. *(The app already shows an ongoing notification with a
  Stop action — good, it supports the media-playback classification.)*

### A2. Content rating (IARC) questionnaire
Mandatory App-content step. Answer truthfully (no violence/sexual/gambling content; references
religious content) → will rate everyone/PEGI 3. Cannot publish without it.

### A3. Data safety form  (see section D for exact answers)

### A4. Privacy policy URL
`privacy-policy.html` must be hosted at a public, stable URL (no login). Paste that URL in **both**
the store listing and the Data-safety form. (Hosting decision — see "Decisions" below.)

---

## B. Account & testing track

- **Developer account:** Google Play Developer account, one-time **$25** fee.
  - A **personal** account created today must run **closed testing with ≥12 testers for 14 days**
    before it can request production access. For a mosque, recruiting 12 community testers is easy.
  - An **organisation** account (in the association's name) is exempt from that gate but needs a
    **D-U-N-S number** (free, can take 1–2 weeks to obtain). Cleaner long-term ("IGBD-Gemeinde…" as
    the developer name instead of a person).
- **Play App Signing:** enrol (default) — Google holds the upload/app-signing keys; our
  `keystore.properties` is the *upload* key. Keep it backed up.

## C. Permissions to revisit (review-scrutiny)

| Permission | Verdict | Action |
|---|---|---|
| `SCHEDULE_EXACT_ALARM` + `USE_EXACT_ALARM` | **OK** — app qualifies as alarm-clock class | Keep both. Frame the app as a prayer-time *alarm* in the listing + production-access form. No separate form. |
| `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | Needs A1 declaration | See A1. |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | **Flagged / restricted** | Decision: keep (reliability on Samsung/Xiaomi — owner's own S22) vs. remove (FGS + exact alarms are already Doze-exempt). If kept: keep it optional/opt-in (already a Settings button, not auto-triggered) and be ready to justify. |
| `USE_FULL_SCREEN_INTENT` | **Not currently declared** | Only needed if we want the Adhan to take over the lock screen like an alarm clock. Today it's a heads-up notification + FGS (no declaration needed). Decision below. |
| `ACCESS_NOTIFICATION_POLICY` (DND auto-silence) | OK | Keep opt-in + the in-app explanation; documented in privacy policy. |

## C2. Android TV (same bundle installs on phones + Google TV)
- Opt into the **Android TV form factor** in Play Console and pass the **TV app-quality review**
  (D-pad/leanback navigation, banner asset — `tv_banner.png` already done, no touchscreen required).
- This is a *separate* review track from the phone listing; can be enabled after the phone launch.

## C3. Target API level — **done**
- `compileSdk`/`targetSdk` are both **36** (Android 16), so the **31 Aug 2026** deadline for new
  apps/updates is already met. (Android TV only requires 35, and 36 satisfies that too.)
- Caveat: AGP 8.7.3 warns it was only tested up to `compileSdk 35`. The build is clean, but an AGP
  bump to a version that officially supports 36 is worth doing at the next dependency refresh.

---

## D. Data-safety form — exact answers

Overall: **No data collected, no data shared** (for advertising/analytics) — the app has no user
accounts, no location permission, no ads, no analytics SDK.
- *"Does your app collect or share any of the required user data types?"* → the only honest nuance:
  Google Firebase may process **device identifiers / approximate IP** at the network layer to deliver
  announcements. Per Google's rules, **ephemeral, server-side-only** technical data that you don't
  store can be excluded from collection — but if the form asks, declare it as **collected, not shared,
  not for ads, encrypted in transit, processed ephemerally**.
- **Encryption in transit:** Yes (HTTPS/Firebase).
- **Data deletion:** No account → nothing to delete server-side; on-device data cleared by uninstalling.
- Keep the form **consistent with `privacy-policy.html`** — Google cross-checks them.

---

## E. Size trim (optional, recommended)
- **Drop ML Kit** (`mlkit.translate`, `mlkit.language.id`): Gemini is the primary translator; ML Kit
  is only an offline fallback the single admin would rarely hit. Saves ~7 MB on **every** install
  (AAB 50.5 → ~23 MB). Requires removing the fallback code path + graceful "offline, try again" handling.
- **Enable R8/minify + resource shrinking:** another ~8–15 MB off, but needs keep-rules
  (Firebase models, Room, Hilt, serialization) **and a full on-device QA pass** before shipping.
- Per-device download is already ~29 MB (AAB ships one ABI), not 50.5 MB.

---

## F. iOS (separate track — needs owner go-ahead + hardware)
- **Recommended approach: Compose Multiplatform / KMP** — reuses ~60–75 % of the existing Kotlin
  (prayer math, networking, Room/DataStore, Firebase, most Compose UI). Swap Hilt→Koin, Retrofit→Ktor.
- **Hard platform limits (true in *every* framework, not a shortcut):**
  - Exact-time Adhan that plays from a killed app → **not possible**. iOS gives only scheduled local
    notifications; the Adhan becomes a **≤30-second notification sound**, full Adhan only on tap.
  - **Auto Do-Not-Disturb toggling → not possible** (no iOS API).
  - Widget (Glance) → rewrite natively in **WidgetKit** (SwiftUI).
  - Firebase, per-app language, RTL, 8 locales → fine.
  - Apple TV (tvOS) → CMP doesn't support it; defer.
- **Prerequisites:** a **Mac** (none today — dev machine is Windows), Xcode, **Apple Developer
  Program $99/yr**. Effort ≈ **8–14 weeks** for a phone-only iOS v1.
