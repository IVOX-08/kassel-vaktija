# Antwort an die App-Prüfung (Resolution Center)

Apple hat **nichts an der App bemängelt**. Sie fordern Informationen nach — Richtlinie 2.1,
„Information Needed". Bei einer ersten Einreichung ist das Routine.

Zu tun sind drei Dinge:

1. **Ein Demo-Konto anlegen** (sonst kommt die nächste Ablehnung sicher — siehe unten)
2. **Eine Bildschirmaufnahme auf einem echten iPhone** — die kann nur der Vorstand machen
3. **Den Text unten** in die Antwort im Resolution Center kopieren **und** dauerhaft in
   App Store Connect → App-Prüfungs-Informationen → **Anmerkungen**

> **Das Antwortfeld nimmt höchstens 4000 Zeichen.** Der Text unten ist darauf gekürzt (3999).
> Die erste, ausführlichere Fassung war 5729 Zeichen lang und ließ sich nicht absenden.

---

## Zuerst: das Demo-Konto

Die App hat einen sichtbaren Knopf „Als Gemeinde-Administrator anmelden". **Der Prüfer wird ihn
finden.** Ohne Zugangsdaten wird die App wegen Richtlinie 2.1 erneut abgelehnt — diesmal zu Recht.

⚠️ **Gib Apple NICHT das echte Vorstandskonto.** Damit könnte ein Prüfer Gebetszeiten ändern oder
eine Mitteilung an alle Mitglieder schicken.

**Stattdessen ein eigenes Prüf-Konto:**

1. Firebase-Konsole → Authentication → Users → **Add user**
   E-Mail z. B. `appreview@igbdsandzakkassel.de`, ein Passwort vergeben
2. Die UID der neuen Person kopieren
3. Firestore → Sammlung `admins` → neues Dokument mit dieser UID:
   ```
   role:        "community"
   communityId: "igbd-gemeinde-sandzak-kassel"
   ```

Damit sieht der Prüfer den Admin-Bereich, kann aber nur die Kasseler Gemeinde bearbeiten und keine
Rundnachricht an alle 81 Gemeinden schicken.

**Nach der Freigabe das Konto in Firebase löschen.**

---

## Punkt 1 — Bildschirmaufnahme auf einem echten iPhone

Vom **echten Gerät**, nicht vom Simulator, mit der neuesten iOS-Version.

**Vorbereitung**
- Build über TestFlight aufs iPhone laden
- iPhone: Einstellungen → Kontrollzentrum → **Bildschirmaufnahme** hinzufügen
- Die App **löschen und neu installieren**, damit die Aufnahme wirklich beim ersten Start beginnt

**Aufnehmen** — Kontrollzentrum, Aufnahmeknopf, dann der Reihe nach:

1. App starten (die Aufnahme muss den Start zeigen)
2. Sprache **Deutsch** wählen
3. Durch die Intro-Folien blättern
4. **Benachrichtigungen erlauben** — den Systemdialog mit **Erlauben** bestätigen
5. **Gemeinde auswählen**: „Kassel" suchen, IGBD-Gemeinde Sandžak-Kassel wählen, dann den Ort
6. Startseite: Gebetszeiten, Countdown, Iqamah
7. Reiter **Kalender** — einen Monat blättern
8. Reiter **Nachrichten** — eine Mitteilung antippen, die Reaktions-Knöpfe zeigen
9. Reiter **Mehr → Koran** — eine Sure öffnen, blättern, **Tadschwid** und **Osmanische Schrift**
   antippen, einmal zoomen
10. Zurück → **Qibla** — hier kommt die **Standortabfrage**, mit **Erlauben** bestätigen und den
    Kompass zeigen
11. Zurück → **Zakat** — zwei Zahlen eintippen, damit die Rechnung sichtbar ist
12. Zurück → **Gebets-Tracker** — Flamme und Fortschritt zeigen
13. Reiter **Einstellungen** — durchscrollen, einen Gebets-Schalter und eine Tonauswahl antippen
14. Ganz unten: **„Als Gemeinde-Administrator anmelden"** antippen, mit dem **Prüf-Konto anmelden**,
    den Admin-Bereich kurz zeigen, wieder abmelden

Drei bis vier Minuten. Apple will sehen, dass nichts verborgen ist — deshalb Schritt 14 nicht
weglassen.

---

## Punkt 2 — Getestete Geräte

> **Trag ein, was wirklich getestet wurde. Nichts erfinden.**
> Die iOS-Version steht unter Einstellungen → Allgemein → Info.

```
Tested on:
- iPhone [MODELL], iOS [VERSION] — physical device, installed via TestFlight
- iPhone 16 Pro, iOS 18.6 — Simulator (development testing)
```

---

## Punkte 1–7 — Text für Apple (Englisch, zum Kopieren)

⚠️ **Das Antwortfeld nimmt höchstens 4000 Zeichen.** Dieser Text hat 3996.
Wenn du etwas ergänzt, musst du an anderer Stelle kürzen.

Punkt 1 ist die Bildschirmaufnahme — kein Text, sondern die Datei unter „Datei anhängen".
Die Zeile steht trotzdem im Text, damit der Prüfer nicht denkt, der Punkt sei übersprungen.

Vor dem Absenden: `[PASSWORT]` in Punkt 4 ersetzen. Die Geräte in Punkt 2 sind eingetragen.

```
1. SCREEN RECORDING
Attached to this reply. Recorded on a physical iPhone, starting from launch.

2. TESTED ON
- iPhone 17 Pro Max, iOS 26.6.1 - physical device via TestFlight
- iPhone 16 Pro, iOS 18.6 - Simulator (development)

3. FUNCTIONS AND TARGET AUDIENCE
IGBD Vaktija is a free prayer-time app for the member communities of the IGBD, the federation of Bosnian Muslim communities in Germany. Published by the non-profit association IGBD - Gemeinde Sandzak-Kassel e.V.

Every community announces its own Iqamah times, which differ from calculated times and change through the year; members previously had to check the mosque noticeboard.

Users pick their community once from a list of 81. Features: prayer times with countdown and Iqamah, per-prayer notifications, Hijri calendar, announcements, Qur'an in Arabic with optional tajweed colouring, hadith, dhikr, tasbih, prayer tracker, Ramadan overview, Zakat calculator, Qibla compass, home-screen widget.

Audience: members of these mosque communities in Germany. Eight languages: Bosnian, German, English, Arabic, Turkish, Albanian, Urdu, Russian. No advertising, no analytics.

4. SETUP AND ACCESS
No account needed. On first launch the app asks for a language, then a community. Choose "IGBD-Gemeinde Sandzak-Kassel" and the town "Kassel" for a fully populated community. The "More" tab holds Qur'an, hadith, dhikr, tasbih, tracker, Ramadan, Zakat, Qibla.

ADMIN ACCESS (optional, not needed to use the app). Board members sign in to edit their community's Iqamah times and post announcements. Button at the bottom of Settings.
  E-mail: appreview@igbdsandzakkassel.de
  Password: [PASSWORT]
This account administers one community only. A higher role exists for the head administrator (tap the version number in Settings seven times). We do not provide credentials for it: it can message all 81 communities at once and adds no user-facing screens.

No paid content, so no purchase or subscription flow exists.

5. EXTERNAL SERVICES
- vaktija.eu - the prayer-time source these communities publish. Read only, no account, no personal data sent.
- Google Firebase (Firestore, Auth, Cloud Messaging) - stores the community directory, Iqamah times and announcements; delivers notifications. Ordinary users are signed in anonymously; only board members have an account.
- Google Gemini API - used ONCE by an administrator when posting an announcement, to translate it into the eight languages. Readers never contact it. Only the text just typed is sent; no user data.
- YouTube - a link in an announcement plays in the standard embedded player.
- PayPal - the Donate button opens the community's donation page in the browser. No payment happens in the app.

Permissions: Notifications, for prayer times and announcements. Location (when in use) ONLY for the Qibla compass, to compute the direction to the Kaaba from where the user stands; used on the device, never stored or transmitted. If declined, the compass uses the mosque's address.

USER-GENERATED CONTENT: only a community's board can write announcements, using an account the association issues. Users cannot post text, images or comments. They can only react with an anonymous like or dislike. Nothing to report or block.

6. REGIONAL DIFFERENCES
None. The app is identical in every region. The chosen community decides which times and announcements appear - a user choice, not a regional one.

7. RIGHTS TO THIRD-PARTY MATERIAL
- Published by IGBD - Gemeinde Sandzak-Kassel e.V., a member community of the IGBD, using the federation's name and emblem with its authorisation.
- Qur'anic text: Uthmani from the public alquran.cloud dataset; tajweed markup from the open quran-tajweed project.
- Typeface: Amiri Quran, SIL Open Font License.
- The call to prayer was recorded by the community's own muezzin.
- The emblem belongs to the association.

Not a regulated industry. No medical or legal advice. The Zakat calculator is labelled in the app as a calculation aid, not a ruling.
```

---

## Punkt 7 — vom Vorstand bestätigt (30.08.2026)

- **Name und Zeichen der IGBD:** Die Kasseler Gemeinde ist Mitglied der IGBD und darf beides
  führen. Kein offenes Thema.
- **Die Adhan-Aufnahme** stammt vom eigenen Muezzin der Gemeinde. Sie ist nicht heruntergeladen.
- **Das Wappen** gehört dem Herausgeber selbst.

Damit ist der Text zu Punkt 7 so, wie er an Apple gehen kann.

**Die Screenshots im Store** (Richtlinie 2.3.3): Sie müssen die App im Gebrauch zeigen — nicht
den Startbildschirm, nicht die Sprachauswahl. Also Gebetszeiten, Kalender, Koran, Qibla.
