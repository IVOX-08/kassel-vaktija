# Kassel Vaktija — iOS-Stand (Übergabe für neuen Chat)

Stand: 9. August 2026 · Repo: `IVOX-08/kassel-vaktija` (privat, alles gepusht)

## Projekt
iOS-Version der fertigen Android-App „Kassel Vaktija" (Gebetszeiten-App der
IGBD-Gemeinde Sandžak-Kassel). Ziel: **1:1 wie Android**, exakt nach den beiden
Spezifikationen:
- `~/Downloads/iOS-1zu1-Prompt.txt` (Haupt-Spec, alle Screens)
- `~/Downloads/iOS-Update-Prompt.txt` (Nachtrag: Ramadan, Benachrichtigungen, Sprache)

## Umgebung (geliehener Intel-Mac)
- Xcode 16.4 unter `/Applications/Xcode-16.4.0.app` → immer
  `export DEVELOPER_DIR=/Applications/Xcode-16.4.0.app/Contents/Developer`
- Simulator: **KasselSim**, UDID `EB35B669-0E6A-4419-8AD7-9B7947BCF65F`, iOS 18.6
- Bundle-ID: `de.igbdsandzakkassel.vaktija.ios`
- Intel → immer mit `ARCHS=x86_64` bauen
- Projekt wird von **XcodeGen** aus `iosApp/project.yml` erzeugt
  (`.xcodeproj` ist gitignored → nach neuen Dateien `xcodegen generate` laufen lassen)

### Build + Start (bewährter Befehl)
```bash
export DEVELOPER_DIR=/Applications/Xcode-16.4.0.app/Contents/Developer
cd ~/Documents/kassel-vaktija/iosApp && xcodegen generate
DEV=EB35B669-0E6A-4419-8AD7-9B7947BCF65F
xcodebuild -project iosApp.xcodeproj -scheme iosApp -configuration Debug \
  -derivedDataPath build -destination "id=$DEV" ARCHS=x86_64 \
  ONLY_ACTIVE_ARCH=YES CODE_SIGNING_ALLOWED=NO build
xcrun simctl install $DEV build/Build/Products/Debug-iphonesimulator/iosApp.app
xcrun simctl launch $DEV de.igbdsandzakkassel.vaktija.ios
```

## Fertig und im Simulator geprüft
- **Startseite** — offizielle Zeiten von `https://vaktija.eu/kassel` (JSON-LD),
  offline-first mit Cache, Iqamah-Regel (Fajr 04:30 fix, Dhuhr/Asr +10,
  Maghrib +5, Isha +0), Live-Countdown
- **Kalender** — lokal per adhan2 gerechnet + **Kalibrier-Offset**
  (offiziell heute − lokal heute) → Monat deckt sich mit vaktija.eu
- **Mehr** — Lavendel-Karten wie Android; Koran (paginierter Mushaf-Reader,
  kein Scrollen, RTL-Blättern, Lesezeichen), Hadith, Dhikr (zentriert),
  Tasbih, Gebets-Tracker, Ramadan, Qibla
- **Einstellungen** — Design-Chips, Gebetsbenachrichtigungen (Master, Ton,
  Lautlos-Hinweis, 5 Pro-Gebet-Karten mit Vorwarnung 0/5/10/15/30),
  Auto-Stummschaltung, Mitteilungen, Sprache, Über-uns + 7-Tipp-Admin-Tor
- **Sprache (8)** — bs (Standard), de, ar, tr, sq, en, ur, ru; animierte
  Sprachauswahl mit **wehenden GIF-Flaggen**; Wechsel wirkt sofort, **bleibt
  nach Neustart**, **RTL** für Arabisch/Urdu (getestet de→ar→de)
- **Onboarding** — Sprache → 4 Intro-Folien → 3-Schritt-Berechtigungsassistent
- **Widget** — WidgetKit-Extension, nächstes Gebet + Live-Countdown
- **Ramadan-Screen (neu)** — Tag-Abzeichen, goldener Fortschrittsring +
  Countdown, Sehur/Iftar/Teravih-Karte, Iftar-Bittgebet (arabisch +
  Umschrift + Bedeutung), Fasten-Zähler; **alle UI-Labels in allen 8 Sprachen**
  (Schlüssel `ramadan_day_badge`, `ramadan_mubarak`, `ramadan_starts_in`,
  `ramadan_dua_title`, `ramadan_fasted_count`) — geprüft in de, bs und ar (RTL)
- **Benachrichtigungen** — geplant für 7 Tage, Texte in der **gewählten
  App-Sprache**, gewählter Ton, time-sensitive (Vibration bei lautlos),
  erscheinen auch bei offener App. Bewiesen: zugestellte Nachricht
  „Vrijeme za Akšam" (bosnisch) mit `adhan_short.mp3`
- **Inhalte bereinigt** — `<br>`/`\n`-Reste aus Hadith-JSON entfernt

## Veröffentlichung — Stand 21.08.2026

### Erledigt
- **Apple Developer Program** — Company-Account `IGBD - Gemeinde Sandzak-Kassel e.V.`,
  Team-ID `RM3FWBH4T7`, über die **Gebührenbefreiung** (keine 99 € gezahlt)
- **App Store Connect** — App „Kassel Vaktija", Version 1.0, Bundle-ID
  `de.igbdsandzakkassel.vaktija.ios`
- **Signing** — Debug bleibt Ad-hoc (Simulator/Keychain), Release nutzt automatisches
  Signing. `CODE_SIGN_IDENTITY` darf bei Automatic **nicht** gesetzt werden, sonst
  "conflicting provisioning settings". Ein Gerät muss im Team registriert sein, sonst
  scheitert das Archiv an "team has no devices" (registriert: `iPhone von Alen`)
- **Archiv + .ipa** gebaut, Distribution-signiert, `aps-environment = production`
- **Datenschutz-URL** live: https://ivox-08.github.io/Kassel-Datenschutz/
- **Screenshots** — 5 Stück, 1320×2868, in `docs/ios/screenshots/`
- **Store-Texte** — `docs/ios/STORE-LISTING.md`

### Zwei behobene Fehler
1. **Adhan-Ton klang nie in Benachrichtigungen.** Die Datei lag unter
   `<App>/audio/adhan_short.mp3`; `UNNotificationSound` sucht aber nur im Bundle-Root
   (oder `Library/Sounds`) und fällt sonst kommentarlos auf den Standardton zurück.
   `Resources/audio` ist deshalb **keine Ordner-Referenz** mehr, und `SoundPlayer`
   lädt ohne `subdirectory:`. **Auf echtem Gerät noch nicht gegengehört.**
2. **Push war gar nicht angebunden.** `FirebaseMessaging` war eingebunden, aber kein
   Code nutzte es. Neu: `PushService.swift` (APNs-Token an FCM, Topic `announcements`,
   Silent-Push-Handler) plus `@UIApplicationDelegateAdaptor` in `iOSApp.swift`.

### Offen — braucht den Login des Vorstands
1. **APNs-Schlüssel** (.p8) erzeugen und in Firebase hochladen (Team-ID `RM3FWBH4T7`).
   **Ohne ihn erreicht kein Push ein iPhone.**
2. **Upload der .ipa** — entweder App-Store-Connect-API-Schlüssel bereitstellen, oder
   selbst über Xcode → Organizer → Distribute App hochladen
3. **Formulare in App Store Connect** — App-Datenschutz, Altersfreigabe,
   Datenschutz-URL, **Händlerstatus** (EU-Pflicht, Prüfung dauert Tage, blockiert
   die Einreichung)
4. **Anmerkungen für die App-Prüfung** — der Admin-Bereich liegt hinter 7 Tipps auf
   „Über uns". Findet der Prüfer ihn nicht, gilt die App als unvollständig

### Offen — Server, nicht deployed
`functions/index.js` → `onConfigUpdated` hat jetzt einen `apns`-Block mit
`content-available: 1`. Ohne ihn erreichen **Gebetszeit-Änderungen nie ein iPhone**.
Die Änderung ist eingetragen, aber **nicht deployed** — betrifft die laufende
Android-App, deshalb vorher mit dem Android-Team abstimmen.

### Nicht mehr nötig
Eine **D-U-N-S-Nummer** war nicht erforderlich — der Company-Account bestand bereits.

## Bekannte Macken dieses Macs
- **Ursache der Simulator-Aussetzer: Hitze.** Vom Nutzer herausgefunden — der Mac
  ist alt, und nach mehreren langen Builds hintereinander wird er so warm, dass
  der Simulator keine Klicks mehr annimmt (Screenshots und `simctl` gehen oft
  noch). **Erst abkühlen lassen**, dann läuft es wieder; ein Neustart hilft vor
  allem deshalb, weil er eine Pause erzwingt. Praktische Folge: Builds nicht
  ohne Pause aneinanderreihen, und bei ausbleibenden Klicks warten statt
  Prozesse zu killen.
- **CoreSimulator hängt sich regelmäßig auf**: `simctl` antwortet nicht mehr,
  Klicks kommen nicht an, oder das Gerät verschwindet. Prozess-Kills helfen
  meist nicht → abkühlen lassen, notfalls **Mac neu starten**.
- `simctl spawn` hängt fast immer → nicht verwenden.
- Screenshots: `xcrun simctl io $DEV screenshot datei.png` funktioniert auch,
  wenn die computer-use-Screenshots klemmen.
- Lange Befehle immer im Hintergrund starten, sonst Timeout.

## Sicherheit / Aufräumen am Ende
Vor Rückgabe des Macs: `gh auth logout`, Apple-ID abmelden, Projekt +
Werkzeuge löschen. Secrets (`gemini.properties`, `keystore.properties`,
`local.properties`, `google-services.json`) sind gitignored und dürfen
**nicht** committet werden.
