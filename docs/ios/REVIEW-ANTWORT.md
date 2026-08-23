# Antwort an die App-Prüfung (Resolution Center)

Apple hat nicht abgelehnt, weil etwas kaputt ist — sie fordern Informationen nach
(Guideline 2.1, „App Completeness"). Unten die Antwort auf die sieben Punkte.

**Punkt 1 (Bildschirmaufnahme) muss der Vorstand selbst erstellen** — siehe unten.
Die Punkte 2–7 kopierst du in die Antwort im Resolution Center **und** zusätzlich
dauerhaft in das Feld „Anmerkungen" der App-Prüfungs-Informationen.

---

## Punkt 1 — Bildschirmaufnahme auf einem echten iPhone

Apple will ein Video vom echten Gerät, nicht vom Simulator.

**Vorbereitung**
1. Den Build über **TestFlight** auf ein echtes iPhone laden
   (App Store Connect → TestFlight → interner Tester → Einladung an die eigene Apple ID)
2. Auf dem iPhone: Einstellungen → Kontrollzentrum → **Bildschirmaufnahme** hinzufügen

**Aufnehmen** (Kontrollzentrum öffnen, Aufnahmeknopf drücken, dann):
1. App **frisch starten** — die Aufnahme muss mit dem Start beginnen
2. Sprachauswahl → **Deutsch** tippen
3. Durch die vier Intro-Folien blättern
4. Beim Berechtigungsassistenten **„Benachrichtigungen erlauben"** tippen und den
   Systemdialog mit **Erlauben** bestätigen — Apple will die Berechtigungsabfragen sehen
5. Startseite zeigen: Gebetszeiten, Countdown, Iqamah
6. Tab **Kalender** — durch einen Monat blättern
7. Tab **Mehr** → **Koran** → eine Sure öffnen, eine Seite blättern
8. Zurück → **Ramadan** zeigen
9. Zurück → **Qibla** öffnen (dabei erscheint die Standortabfrage — auch die zeigen)
10. Tab **Einstellungen** — Benachrichtigungen pro Gebet, Tonauswahl antippen
11. **Über uns** öffnen und **7 Mal auf das Wappen tippen**, damit der Admin-Anmeldebildschirm
    erscheint — danach abbrechen. So sieht der Prüfer, dass nichts verborgen wird.

Aufnahme beenden, Video in der Antwort anhängen. Zwei bis drei Minuten reichen.

---

## Punkt 2 — Getestete Geräte und Betriebssysteme

> **Trag hier ein, was tatsächlich getestet wurde — nichts erfinden.**
> Ergänze das Gerät, auf dem du die Aufnahme machst, mit seiner iOS-Version
> (Einstellungen → Allgemein → Info).

```
Tested on:
- iPhone [MODELL], iOS [VERSION] — physical device, via TestFlight
- iPhone 16 Pro Max and iPhone 16 Pro, iOS Simulator (Xcode) — development testing
```

---

## Punkte 3–7 — Text für die Antwort

```
3. FUNCTIONS AND TARGET AUDIENCE

Kassel Vaktija is a free prayer-time app for the IGBD Sandzak-Kassel mosque
community in Kassel, Germany, published by the registered non-profit association
that runs the mosque.

Problem it solves: members of the community need the prayer times as they are
actually announced by their own mosque. Generic prayer apps calculate times
astronomically and differ from the mosque's published schedule by several minutes,
which matters for congregational prayer. This app shows the mosque's own official
times, so the app and the notice board in the mosque agree.

Target audience: the roughly 1,000 members of this local community, many of whom
are Bosnian, Turkish, Albanian or Arabic speakers. The interface is available in
eight languages for that reason.

Core features: daily prayer times with a live countdown, a monthly calendar,
local reminders with the Adhan, a home-screen widget, the Quran with a page-based
reader, hadith collections, dhikr and tasbih counters, a prayer tracker, a Ramadan
screen and a Qibla compass. Announcements from the mosque board are shown in a
news tab.

4. SETUP AND ACCESS

No setup is required. There is no account, no login, no purchase and no paywall.
Every feature is available immediately after installing. On first launch the app
asks for a language and offers to enable notifications; both can be skipped.

The only exception is a small staff-only administration area used by the elected
board of the association to publish announcements and adjust the community's
prayer times. It is reached by opening "Mehr" -> "Über uns" and tapping the
community crest 7 times, which reveals a sign-in screen.

We are not able to provide credentials for that area. Access is restricted to a
single Firebase account belonging to the board and is enforced server-side by
Firestore security rules. That account can send announcements to every installed
device, so sharing its password would put the community's communication channel
at risk. The area unlocks no user-facing functionality, no content and no
purchases. If you need to inspect it, please tell us and we will arrange access.

5. EXTERNAL SERVICES

- vaktija.eu/kassel — the mosque's own published prayer-time schedule. Read-only,
  fetched over HTTPS and cached locally so the app works offline.
- Firebase Cloud Firestore (Google) — stores announcements written by the board.
  Public read, admin-only write, enforced by server-side security rules.
- Firebase Authentication (Google) — used only to sign in the single board account
  for the staff-only area. Regular users never authenticate.
- Firebase Cloud Messaging (Google) — delivers announcements from the board as push
  notifications. Prayer reminders themselves are scheduled locally on the device and
  do not depend on any server.
- Google Gemini API — used only inside the staff-only area, to translate an
  announcement written by the board into the app's eight languages at the moment it
  is posted. It is never invoked for regular users and processes no user data.

No payment processors, no advertising networks, no analytics or tracking SDKs.

6. REGIONAL DIFFERENCES

None. The app behaves identically in every region. The prayer times are those of
one specific mosque in Kassel, Germany, and are the same regardless of where the
device is located. The interface is available in Bosnian, German, Arabic, Turkish,
Albanian, English, Urdu and Russian; the language is chosen by the user and is not
derived from the region. Arabic and Urdu are laid out right-to-left.

7. REGULATED INDUSTRY / PROTECTED MATERIAL

The app does not operate in a regulated industry. It provides no medical, financial
or legal services and handles no payments.

It is published by the mosque community itself, for its own members, and the prayer
times it shows are that community's own published schedule.
```

---

## Anmerkung zu Punkt 7

Apple fragt dort ausdrücklich nach **geschütztem Material Dritter**. In den
Hadith-Daten steht als Quelle eine veröffentlichte Übersetzung
(„Riyâzü's-Sâlihîn Tercüme ve Şerhi", Erkam Yayınları) — siehe den offenen Punkt
in `STATUS.md`. Im Feld „Informationen zu den Inhaltsrechten" wurde **Nein**
angegeben; der obige Text ist dazu konsistent formuliert. Falls der Vorstand die
Rechtelage klärt und sie anders ausfällt, sollte die Angabe angepasst werden.
