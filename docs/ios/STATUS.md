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
  Umschrift + Bedeutung), Fasten-Zähler
- **Benachrichtigungen** — geplant für 7 Tage, Texte in der **gewählten
  App-Sprache**, gewählter Ton, time-sensitive (Vibration bei lautlos),
  erscheinen auch bei offener App. Bewiesen: zugestellte Nachricht
  „Vrijeme za Akšam" (bosnisch) mit `adhan_short.mp3`
- **Inhalte bereinigt** — `<br>`/`\n`-Reste aus Hadith-JSON entfernt

## Offen
1. **Ramadan-UI-Labels in den 7 anderen Sprachen** (aktuell nur Deutsch;
   das Iftar-Bittgebet ist bereits mehrsprachig vom Nutzer geliefert).
   Betrifft: „Ramadan beginnt in X Tagen", „X von Y Tagen gefastet",
   „Bittgebet beim Fastenbrechen", „Bis zum Ende des Sehur", „Ramadan Mubarak"
2. **Nachrichten + Admin** — brauchen `GoogleService-Info.plist` (Firebase)
   vom Vorstand. Admin-Spec steht in Abschnitt 9 der Haupt-Spec
   (fester UID `1a7xqRgIYDR0RZqa3KghBlz98PK2`, Zeiten-Editor, Mitteilungs-Editor)
3. **App-Icon + Store-Vorbereitung** (Screenshots, Texte, Altersfreigabe)

## Veröffentlichung — was der Nutzer besorgen muss
1. **Apple Developer Program, 99 €/Jahr** (Vorstand freigeben lassen)
2. **D-U-N-S-Nummer** für den Verein → dauert 1–2 Wochen, **zuerst beantragen**
   (nötig für Organisations-Account, sonst erscheint der Privatname)
3. **Datenschutzerklärung mit öffentlicher URL** (Pflicht bei Apple)
4. **Firebase-Datei** vom Vorstand
5. Zum Hochladen wird ein Mac mit Xcode gebraucht

## Bekannte Macken dieses Macs
- **CoreSimulator hängt sich regelmäßig auf**: `simctl` antwortet nicht mehr,
  Klicks kommen nicht an, oder das Gerät verschwindet. Prozess-Kills helfen
  meist nicht → **Mac neu starten**. Danach läuft es wieder.
- `simctl spawn` hängt fast immer → nicht verwenden.
- Screenshots: `xcrun simctl io $DEV screenshot datei.png` funktioniert auch,
  wenn die computer-use-Screenshots klemmen.
- Lange Befehle immer im Hintergrund starten, sonst Timeout.

## Sicherheit / Aufräumen am Ende
Vor Rückgabe des Macs: `gh auth logout`, Apple-ID abmelden, Projekt +
Werkzeuge löschen. Secrets (`gemini.properties`, `keystore.properties`,
`local.properties`, `google-services.json`) sind gitignored und dürfen
**nicht** committet werden.
