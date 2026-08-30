# Antwort an die App-Prüfung (Resolution Center)

Apple hat **nichts an der App bemängelt**. Sie fordern Informationen nach — Richtlinie 2.1,
„Information Needed". Bei einer ersten Einreichung ist das Routine.

Zu tun sind drei Dinge:

1. **Ein Demo-Konto anlegen** (sonst kommt die nächste Ablehnung sicher — siehe unten)
2. **Eine Bildschirmaufnahme auf einem echten iPhone** — die kann nur der Vorstand machen
3. **Den Text unten** in die Antwort im Resolution Center kopieren **und** dauerhaft in
   App Store Connect → App-Prüfungs-Informationen → **Anmerkungen**

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

## Punkte 3–7 — Text für Apple (Englisch, zum Kopieren)

```
3. FUNCTIONS AND TARGET AUDIENCE

IGBD Vaktija is a free prayer-time app for the member communities of the IGBD
(Islamische Gemeinschaft der Bosniaken in Deutschland), the federation of Bosnian
Muslim communities in Germany. It is published by the registered non-profit
association IGBD - Gemeinde Sandzak-Kassel e.V.

Problem it solves: each community announces its own prayer (Iqamah) times, which
differ from astronomically calculated times and change through the year. Members
previously had to check a noticeboard in the mosque or ask. The app shows the
times their own community has published, so nobody arrives late or prays at the
wrong time.

Users pick their community once from a list of 81 (or the town within it), and
from then on see that community's times, address and announcements.

Features:
- Daily prayer times with a live countdown, plus the community's Iqamah times
- Notifications per prayer, with an optional advance reminder
- Hijri calendar with a month view
- Announcements from the community board
- Qur'an (Arabic, with optional tajweed colouring and a second Arabic typeface)
- Hadith collection, dhikr, digital tasbih counter
- Prayer tracker with a daily streak
- Ramadan overview, Zakat calculator, Qibla compass
- Home-screen widget with the next prayer

Target audience: members of these mosque communities in Germany — Bosnian,
German, Albanian, Turkish, Arabic, Urdu, Russian and English speaking. All eight
languages are included.

The app is completely free. There are no purchases, no subscriptions, no
advertising and no analytics.


4. HOW TO SET UP AND ACCESS THE MAIN FEATURES

No account is needed. On first launch the app asks for a language, then for the
community. Choose "IGBD-Gemeinde Sandzak-Kassel" and the town "Kassel" to see a
fully populated community. Everything else is reachable from the five tabs at the
bottom; the "More" tab holds Qur'an, hadith, dhikr, tasbih, prayer tracker,
Ramadan, Zakat and Qibla.

ADMIN ACCESS (optional, not needed to use the app)

Community board members can sign in to edit their own community's Iqamah times
and to post announcements. The button is at the bottom of the Settings screen.

  Demo account (community administrator):
  E-mail:   [E-MAIL DES PRUEF-KONTOS]
  Password: [PASSWORT]

This account administers one community only. There is a second, higher role for
the federation's head administrator; it is reached by tapping the version number
at the very bottom of Settings seven times. We are not providing credentials for
that role because it can send a message to all 81 communities at once. It grants
no additional user-facing screens beyond community management.

There is no paid content, so no purchase or subscription flow exists.


5. EXTERNAL SERVICES USED

- vaktija.eu — the published prayer-time source used by these communities. Read
  only, no account, no personal data sent.
- Google Firebase (Firestore, Authentication, Cloud Messaging) — stores the
  community directory, the Iqamah times each community sets, and its
  announcements; delivers announcement notifications. Ordinary users are signed
  in anonymously; only board members have an e-mail account.
- Google Gemini API — used ONCE by an administrator when posting an announcement,
  to translate it into the app's eight languages. The translated text is stored
  with the announcement. Readers never contact this service, and no user data is
  sent to it — only the announcement text the administrator just typed.
- YouTube — if an announcement contains a YouTube link, the video plays inside
  the app in the standard embedded player. No account, no data collection by us.
- PayPal — the "Donate" button opens the community's public donation page in the
  browser. No payment is processed inside the app.

Permissions requested:
- Notifications — for prayer times and community announcements.
- Location (when in use) — ONLY for the Qibla compass, to compute the direction
  to the Kaaba from where the user stands. The location is used on the device,
  is never stored and never leaves the phone. If the user declines, the compass
  falls back to the mosque's address.

USER-GENERATED CONTENT

Announcements can only be written by a community's board, who are authenticated
with an account the association issues. Ordinary users cannot post text, images
or comments. They can only react to an announcement with a like or a dislike,
which is a single anonymous counter and shows no user identity. There is
therefore no user-to-user content and nothing for users to report or block.


6. REGIONAL DIFFERENCES

None. The app behaves identically in every region and every App Store territory.
The content is the same everywhere; only the chosen community determines which
prayer times and announcements are shown, and that is a user choice, not a
regional one. All eight languages are available everywhere.


7. RIGHTS TO THIRD-PARTY MATERIAL

- The app is published by IGBD - Gemeinde Sandzak-Kassel e.V., a member community
  of the IGBD, and uses the federation's name and emblem with its authorisation.
- The Qur'anic text is the Uthmani text from the public alquran.cloud dataset,
  with tajweed markup from the open quran-tajweed project.
- The Arabic typeface is Amiri Quran, licensed under the SIL Open Font License.
- The recorded call to prayer is used with the permission of the muezzin who
  recorded it for the community.
- The community emblem belongs to the publishing association itself.

The app is not part of a regulated industry. It provides no medical, financial or
legal advice. The Zakat calculator is explicitly labelled inside the app as a
calculation aid and not a religious ruling, and it asks the user to consult their
imam.
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
