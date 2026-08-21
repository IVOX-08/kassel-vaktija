# App Store Connect — Eintrag „Kassel Vaktija"

Alles hier zum Kopieren. Sprache: Deutsch (Primärsprache des Eintrags).

## Untertitel (max. 30 Zeichen)

```
Gebetszeiten für Kassel
```
(23 Zeichen)

## Werbetext (max. 170 Zeichen, jederzeit änderbar)

```
Offizielle Gebetszeiten der IGBD-Gemeinde Sandžak-Kassel — mit Widget, Adhan-Erinnerung, Koran, Hadith und Ramadan-Kalender. In 8 Sprachen.
```
(137 Zeichen)

## Beschreibung (max. 4000 Zeichen)

```
Kassel Vaktija zeigt die offiziellen Gebetszeiten der IGBD-Gemeinde Sandžak-Kassel — zuverlässig, werbefrei und ohne Benutzerkonto.

GEBETSZEITEN
Die Zeiten stammen direkt von der Gemeinde und stimmen mit dem Aushang in der Moschee überein. Ein Countdown zeigt jederzeit, wie lange es bis zum nächsten Gebet dauert. Die Zeiten werden gespeichert und sind auch ohne Internet verfügbar.

ERINNERUNGEN
Für jedes der fünf Gebete lässt sich einzeln einstellen, ob und wann erinnert wird — auf Wunsch mit Vorwarnung von 5 bis 30 Minuten. Der Adhan erklingt im gewählten Ton; auf Wunsch schaltet die App zur Gebetszeit automatisch stumm.

WIDGET
Das Home-Bildschirm-Widget zeigt das nächste Gebet und den laufenden Countdown, ohne dass die App geöffnet werden muss.

KALENDER
Der Monatskalender zeigt alle Gebetszeiten auf einen Blick, abgeglichen mit den offiziellen Zeiten der Gemeinde.

KORAN
Der vollständige Koran im Mushaf-Layout mit seitenweisem Blättern von rechts nach links, wie im gedruckten Buch. Lesezeichen merken sich die Stelle.

RAMADAN
Fortschrittsring über den Monat, Sehur- und Iftar-Zeiten, Teravih, das Iftar-Bittgebet auf Arabisch mit Umschrift und Bedeutung sowie ein Zähler für die gefasteten Tage.

WEITERES
• Hadith-Sammlung
• Dhikr und digitale Tasbih
• Gebets-Tracker
• Qibla-Kompass
• Mitteilungen der Gemeinde

ACHT SPRACHEN
Bosnisch, Deutsch, Arabisch, Türkisch, Albanisch, Englisch, Urdu und Russisch — Arabisch und Urdu in korrekter Leserichtung von rechts nach links.

DATENSCHUTZ
Kein Benutzerkonto, keine Anmeldung, keine Werbung, kein Tracking. Der Standort wird ausschließlich auf dem Gerät für den Qibla-Kompass verwendet und nicht übertragen.

Herausgegeben von der IGBD-Gemeinde Sandžak-Kassel, Schwanenweg 13, 34123 Kassel.
```

## Keywords (max. 100 Zeichen, mit Komma getrennt, keine Leerzeichen)

```
namaz,vakat,ezan,adhan,gebet,moschee,islam,muslim,kuran,quran,ramadan,kibla,qibla,sandzak,bosna
```
(95 Zeichen)

## URLs

| Feld | Wert |
|---|---|
| Support-URL | `https://ivox-08.github.io/Kassel-Datenschutz/` |
| Marketing-URL | leer lassen |
| Datenschutzrichtlinien-URL | `https://ivox-08.github.io/Kassel-Datenschutz/` |

> Besser wäre eine eigene Support-Seite oder die Vereins-Website. Solange es die
> nicht gibt, ist die Datenschutzseite als Support-URL zulässig.

## Altersfreigabe

Alle Fragen mit **Nein** / **Keine** beantworten → Ergebnis **4+**.
Die App enthält keine Gewalt, keinen Alkohol, kein Glücksspiel, keine
nutzergenerierten Inhalte und keinen ungefilterten Web-Zugriff.

## App-Datenschutz (wichtig wegen Firebase)

Die App nutzt Firebase Messaging (Push), Firestore (Mitteilungen) und Auth
(nur Admin-Login). Anzugeben ist:

| Datentyp | Erfasst? | Zweck | Mit Identität verknüpft? | Tracking? |
|---|---|---|---|---|
| Kennungen → Geräte-ID (Push-Token) | Ja | App-Funktionalität | Nein | Nein |
| Kontaktinfo → E-Mail-Adresse | Ja, **nur Admins** | App-Funktionalität | Ja | Nein |
| Diagnose → Absturzdaten | **Nein** — Crashlytics ist nicht eingebunden | — | — | — |
| Standort | **Nein** — verlässt das Gerät nicht | — | — | — |

Auf die Frage nach **Tracking** überall **Nein** — die App verfolgt niemanden
über andere Apps oder Websites hinweg.

## Exportkonformität (Frage beim Build)

Die App nutzt nur HTTPS. Antwort: **Ja**, sie verwendet Verschlüsselung, aber
**nur die von Apple bereitgestellte Standardverschlüsselung** → damit ist sie
von der Ausfuhrgenehmigung befreit.

## Was Apple oft bemängelt — vorher prüfen

1. **Admin-Bereich.** Der Prüfer muss ihn erreichen können, sonst gilt die App
   als „unvollständige Funktionalität". Entweder Zugangsdaten im Feld
   *Anmerkungen für die App-Prüfung* hinterlegen, oder dort erklären, dass der
   Bereich ausschließlich dem Gemeindevorstand vorbehalten ist.
   Der Zugang ist über 7 Tipps auf „Über uns" versteckt — **das muss man
   dazuschreiben**, sonst findet der Prüfer ihn nicht.
2. **Religiöse Inhalte** sind unproblematisch, solange nichts zu Hass aufruft.
3. **Screenshots** müssen aus der echten App stammen, keine Montagen.

## Anmerkungen für die App-Prüfung (App Review Information → Notes)

Wörtlich in das Feld kopieren. Englisch, weil die Prüfer kein Deutsch lesen.
Kein Demo-Zugang — der Admin-Bereich ist dem Gemeindevorstand vorbehalten (Variante B).

```
Kassel Vaktija is a free prayer-time app for the IGBD Sandzak-Kassel mosque
community in Kassel, Germany. No account, no login, no purchases: every
feature you see on first launch is fully available.

STAFF-ONLY AREA
The app contains a small administration area used exclusively by the elected
board of the association to publish announcements and adjust the community's
prayer times. It is intentionally not advertised in the UI.

To view it: open "Mehr" -> "Über uns" and tap the community crest 7 times.
A sign-in screen appears.

We are not able to provide credentials for it. Access is restricted to a
single Firebase account belonging to the board, enforced server-side by
Firestore security rules. That account can post announcements to every
installed device, so sharing its password would put the community's
communication channel at risk.

The area contains no additional user-facing functionality — nothing is
unlocked, purchased or hidden behind it. Regular users are never asked to
sign in anywhere in the app.

If you do need to inspect it, please let us know and we will arrange access.

NOTIFICATIONS
Prayer reminders are scheduled locally on the device. Push (Firebase Cloud
Messaging) is used only for community announcements sent by the board.

PRAYER TIMES
Times are published by the mosque itself and fetched from vaktija.eu/kassel.
They are cached so the app works offline.

Contact: vorstand@igbdsandzakkassel.de
```

**Falls Apple doch einen Zugang verlangt** (kommt vor, Richtlinie 2.1): Antwort im
Resolution Center abwarten, dann im Vorstand entscheiden, ob ihr die Zugangsdaten
herausgebt. Ein zweites Admin-Konto anzulegen geht nicht ohne Weiteres — die Admin-UID
`1a7xqRgIYDR0RZqa3KghBlz98PK2` steht fest in den Firestore-Regeln.
