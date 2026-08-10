# Firebase handover — wiring the iOS app to the same backend as Android

Everything the iOS app needs in order to read the **same** announcements and community settings the
Android app reads, and to receive the **same** push notifications. Written for the Claude Code session
on the Mac.

Ground rule: **the backend is already live and must not be changed.** Firestore holds real data that
the Android app in the Play Store depends on. Match the shapes below exactly; do not "improve" field
names or restructure documents.

---

## 1. Project identity

| | |
|---|---|
| Project ID | `kassel-vaktija` |
| Project number | `876517152378` |
| Storage bucket | `kassel-vaktija.firebasestorage.app` (unused — see §4) |
| Android package | `de.igbdsandzakkassel.vaktija` |
| Admin UID | `1a7xqRgIYDR0RZqa3KghBlz98PK2` |

The admin UID is the single account allowed to write. It is enforced **server-side** by the Firestore
rules, so the in-app admin check only shows/hides buttons — it is not the security boundary.

## 2. What you must fetch yourself (cannot be copied from this repo)

`GoogleService-Info.plist` does not exist yet and cannot be derived from `google-services.json` —
Android and iOS get **different app IDs and API keys** within the same project.

1. Firebase console → project **kassel-vaktija** → *Add app* → **iOS**
2. Bundle ID: use the iOS app's real bundle identifier. Suggested: `de.igbdsandzakkassel.vaktija`
   (same as Android — allowed, they are separate platforms).
3. Download `GoogleService-Info.plist`, add it to the Xcode target.
4. Add the Firebase SDK via Swift Package Manager: `FirebaseFirestore`, `FirebaseAuth`,
   `FirebaseMessaging`.

Do **not** commit the plist — it is configuration for a live project, and `.gitignore` already
excludes the Android equivalent for the same reason.

## 3. Firestore data model — match exactly

Rules are in `docs/firestore/RULES.md`. All collections: **public read, admin-only write.**

### `news/{autoId}` — announcements

| Field | Type | Notes |
|---|---|---|
| `title` | map `{lang: String}` | per-language, e.g. `{"bs": "…", "de": "…"}` |
| `body` | map `{lang: String}` | same |
| `sourceLang` | String | language the admin actually wrote in; fall back to this when the reader's language is missing from the map |
| `createdAt` | Int64 | **milliseconds** since epoch (`System.currentTimeMillis()`), not seconds — divide by 1000 for `Date(timeIntervalSince1970:)` |
| `hasImage` | Bool | whether a `news_images` doc with the same id exists |

Language tags in the maps: `bs`, `de`, `ar`, `tr`, `sq`, `en`, `ur`, `ru`.

Read order: newest first by `createdAt` descending.

### `news_images/{sameIdAsNewsDoc}` — attached flyer

| Field | Type | Notes |
|---|---|---|
| `data` | String | Base64-encoded JPEG (`Data(base64Encoded:)`) |

Fetch **lazily and cache-first** — only when a card with `hasImage == true` scrolls into view. Images
live in Firestore rather than Cloud Storage so the project stays on the free plan; see RULES.md §"Why
images live in Firestore".

### `config/community` — admin-edited community settings (single document)

| Field | Type | Notes |
|---|---|---|
| `fajrIqamah` | String | `"HH:mm"` |
| `jumua` | String | `"HH:mm"` |
| `dhuhrOffsetMin` | Int | minutes added to the calculated prayer time |
| `asrOffsetMin` | Int | |
| `maghribOffsetMin` | Int | |
| `ishaOffsetMin` | Int | |
| `bajramDate` | String? | ISO `yyyy-MM-dd`; absent when no Eid prayer is announced |
| `bajramTime` | String? | `"HH:mm"`; absent likewise |
| `updatedAt` | Int64 | milliseconds; used to gate duplicate notifications |

Both `bajram*` fields are optional and appear/disappear together. When present, the app shows the Eid
prayer; when absent, it shows nothing (not a placeholder).

## 4. Push notifications (FCM) — the part that needs real work

Both platforms subscribe to the single topic **`announcements`**. On Android this happens at startup
in `KasselVaktijaApp.kt`; do the equivalent on iOS with
`Messaging.messaging().subscribe(toTopic: "announcements")`.

**Required before any push reaches an iPhone:**

1. An **APNs authentication key** (`.p8`) from the Apple Developer account, uploaded in Firebase
   console → *Project settings → Cloud Messaging → APNs Authentication Key*. Without it, iOS pushes
   silently fail. This needs the paid Apple Developer Program.
2. `UNUserNotificationCenter.current().requestAuthorization(...)` — iOS will not show anything
   without explicit permission.
3. `UIBackgroundModes` → `remote-notification` in Info.plist, for the data-only message below.

**Known gap in the server function.** `functions/index.js` has two triggers:

- `onNewsCreated` — sends a `notification:` payload. This **will** reach iOS once APNs is configured;
  no change needed.
- `onConfigUpdated` — sends a **data-only** message with only an `android:` block. iOS does **not**
  deliver data-only messages without an `apns` block carrying `content-available: 1` and
  `apns-priority: 5`. **As written, prayer-time changes will never reach iPhones.** This must be
  fixed on the server side; coordinate before editing so both platforms are tested together.

Also note from `docs/push/SETUP.md`: the Cloud Function may still be **undeployed** (it needs the
Firebase Blaze plan). Until it is, announcements arrive on both platforms only via poll-on-wake, not
instantly. Verify the current state in the console rather than assuming.

## 5. Gemini translation — **required**, the admin will post from the iPhone

Announcements are translated **once, on the posting device, at post time**
(`ui/news/NewsViewModel.kt`, `postNews`). The result is written to Firestore as the per-language maps
described in §3. Reading devices never translate — they just read their language out of the map.

That means a **reader-only** iOS build needs no Gemini at all. But the admin has decided he will also
post from the iPhone, so the iOS app **does** need it: an announcement posted from an unported iPhone
would reach the community in one language only.

A ready-to-use Swift port is in **`docs/ios/GeminiTranslator.swift`** — same models, same retry
behaviour, same prompt. **Keep the prompt in sync with the Kotlin original.** It encodes fixes that
came from real mistranslations (`džemat` → the mosque, not "the group"; no invented greetings or
calls to action). If the platforms drift, the same announcement gets translated differently
depending on which phone the imam happened to pick up.

One deliberate difference from the Kotlin version: the Swift port sends the key as an
`x-goog-api-key` **header** instead of a `?key=` query parameter, so it cannot leak into logs or
crash reports. Both are supported by the API.

**The key itself is not in this repo** (gitignored `gemini.properties` on the Windows machine); copy
it across by hand, not through GitHub. Note that a key shipped in an app binary is extractable on
both platforms — this is parity with the existing Android risk, not a new one. If it ever gets
abused, move translation behind a Cloud Function.

## 6. Checklist

- [ ] iOS app registered in Firebase console, `GoogleService-Info.plist` in the Xcode target
- [ ] Firebase SDKs added (Firestore, Auth, Messaging)
- [ ] APNs `.p8` key uploaded to Firebase
- [ ] Notification permission requested at first launch
- [ ] Subscribed to the `announcements` topic
- [ ] `news` / `news_images` / `config/community` read with the exact field names above
- [ ] `createdAt` / `updatedAt` treated as **milliseconds**
- [ ] `onConfigUpdated` given an `apns` block server-side (coordinate first)
