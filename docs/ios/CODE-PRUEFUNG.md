# Code-Prüfung iOS — 03.09.2026

Vollständige Durchsicht des iOS-Codes nach dem Vorbild der Android-Prüfung.

Geprüft: 7540 Zeilen Swift (47 Dateien), 728 Zeilen gemeinsames Kotlin, `functions/index.js`,
`communities.json` (81 Gemeinden), acht Sprachdateien.

**39 Befunde.** Nichts davon ist erfunden — jeder Punkt nennt Datei und Zeile.

> **Das Ziel dieser Prüfung:** In Zukunft sollen für eine Gemeinde nur noch Spendenlink,
> E-Mail und Imam-Nummer eingetragen werden — als Daten, ohne dass jemand Code anfasst.
> Abschnitt A sagt, was dem heute im Weg steht.

---

## Was nachweislich in Ordnung ist

Bevor die Fehlerliste kommt — diese Dinge habe ich geprüft und sie stimmen:

- **Alle 72 Kürzel für vaktija.eu antworten.** Ich habe jedes einzelne abgefragt: alle liefern
  HTTP 200 und eine auswertbare Zeitentabelle. Keine Gemeinde hängt an einer toten Quelle.
- **Alle 85 Orte haben Koordinaten.** Kein Ort steht auf 0/0.
- **Alle Klingeltöne sind unter 30 Sekunden** (Adhan 19,1 s). Über 30 s würde iOS still den
  Standardton spielen.
- **Der Zakat-Rechner liest Beträge richtig.** Der Android-Fehler (`1234.50` als 123450) ist hier
  nicht drin, sondern bewusst anders gelöst — siehe `ZakatView.swift:175–190`.
- **Die Umzugsliste für den geteilten Speicher** deckt alle Schlüssel ab bis auf einen (Punkt 18).

---

## A · Fremde Daten für 80 von 81 Gemeinden

Diese Gruppe blockiert das Ziel. Die Daten sind da — sie kommen nur nicht an.

### 1. Kontaktkarte fest auf Kassel
`SettingsView.swift:356–372` (Karte ab `:366`)

Name, Adresse, E-Mail, Spendenlink und Imam stehen als feste Zeichenketten im Code:

```
Text("IGBD-Gemeinde Sandžak-Kassel")
row(… "Schwanenweg 13\n34123 Kassel")
row(… "vorstand@igbdsandzakkassel.de")
row(… "\(L("about_imam")): Alen Golac\n0176 3037 2402")
```

Wer in Berlin seine Gemeinde wählt, bekommt Kassels Adresse, Kassels Vorstand und Kassels Imam.

### 2. Spendenknopf auf der Startseite geht immer nach Kassel
`ContentView.swift:17` und `:60`

```
private let donateURL = URL(string: "https://www.paypal.com/donate/?business=ikzsandzakkassel@gmail.com…")
```

Die Adresse daneben folgt korrekt der Auswahl (`mapsURLForSelection`), das Herz nicht. Geld einer
fremden Gemeinde landet bei Kassel.

### 3. Telefon, E-Mail und Webseite werden gelesen und weggeworfen
`CommunityCatalog.swift:20–45`

In `communities.json` stehen bereits:

| Feld | vorhanden bei |
|---|---|
| `phone` | 49 von 81 |
| `email` | 45 von 81 |
| `website` | 31 von 81 |

`CommunityInfo` kennt diese drei Felder nicht. Swift überliest unbekannte Felder stillschweigend —
die Daten sind im App-Paket und kommen nie an. **Das ist der kürzeste Weg zu deinem Ziel:** drei
Zeilen in der Struktur, und die Kontaktkarte kann sie zeigen.

### 4. Spendenlink nur bei einer Gemeinde
`communities.json`

`donationUrl` ist bei **1 von 81** gefüllt (Kassel). Ein Feld für den Imam gibt es überhaupt nicht.

### 5. Gebetszeiten werden für Kassel gerechnet — für alle
`PrayerTimesCalculator.kt:25` und `:63–64`

```
private val coordinates: Coordinates = Coordinates(KASSEL_LAT, KASSEL_LNG)
```

`dashboardRowsForToday()` und `monthForDisplay()` rufen den Rechner ohne Koordinaten auf, also
immer mit Kassel. Betroffen ist alles, was nicht die tagesaktuelle Abfrage bei vaktija.eu ist:

- die Zeiten, solange kein Netz da ist
- der **Monatskalender**
- die Benachrichtigungen für Tag 1 bis 6 im Voraus
- die Fenster des Gebets-Trackers für kommende Tage

Der Ausgleich („Kalibrierung": offiziell minus gerechnet) mildert das, ist aber ein fester Zuschlag
aus **einem** Tag. Zwischen Hamburg und München liegen gut 5 Breitengrade; über einen Monat läuft
die Kurve auseinander, nicht nur der Startwert.

Die Koordinaten jeder Gemeinde liegen vor. Sie werden nur nicht durchgereicht.

### 6. Qibla „ab der Moschee" zeigt ab Kassel
`MoreView.swift:212` → `Qibla.kt:29`

Ohne Standortfreigabe fällt die App auf `qiblaDegrees()` zurück — und das ist fest Kassel.
Darunter steht aber „ab der Adresse deiner Moschee". Innerhalb Deutschlands sind das unter vier
Grad Abweichung, die Zeile ist trotzdem falsch.

### 7. Der Übersetzer hält jede Gemeinde für Kassel
`GeminiTranslator.swift:176`

Im Auftrag an das Sprachmodell steht fest `mosque "IGBD-Gemeinde Sandžak-Kassel" in Kassel,
Germany`. Ein Vorstand in Stuttgart schreibt eine Mitteilung, und das Modell übersetzt sie im
Glauben, es gehe um Kassel.

---

## B · Der Gemeindewechsel greift nicht durch

Wer die Gemeinde wechselt, wechselt weniger, als er denkt. Alles hier braucht einen Neustart.

### 8. Mitteilungen bleiben bei der alten Gemeinde
`NewsRepository.swift:82`

```
guard newsListener == nil, FirebaseApp.app() != nil else { return }
```

Der Zuhörer wird einmal auf `communities/{alte-id}/news` gesetzt und nie erneuert. `stop()` ist
geschrieben, wird aber **nirgends aufgerufen**.

### 9. Ikamet, Džuma und Bajram bleiben bei der alten Gemeinde
`CommunityRuleStore.swift:29`

Derselbe Bau, dieselbe Folge: `guard listener == nil`. Nach dem Wechsel zeigt die App die
Ikamet-Zeiten der vorherigen Gemeinde — und plant die Benachrichtigungen danach.

### 10. Der Zwischenspeicher der Zeiten kennt keine Gemeinde
`PrayerData.swift:132–133`

`vaktija_today` und `vaktija_calibration` sind je **ein** Schlüssel für alle. Geprüft wird nur das
Datum, nicht die Gemeinde. Direkt nach dem Wechsel — und ohne Netz den ganzen Tag — stehen die
offiziellen Zeiten der alten Gemeinde unter dem neuen Namen.

Dazu lädt `ContentView` nicht neu: `.task { await store.refresh() }` läuft einmal beim Erscheinen.

### 11. Gemeinden können aus der Liste verschwinden
`CommunityCatalog.swift:130`

```
return live.map { remote in … }
```

Das Ergebnis ist genau die Firestore-Liste. Eine Gemeinde, die im App-Paket steht, aber noch nicht
importiert wurde, ist beim Start eine Sekunde lang da und danach weg. Der Kommentar darüber
verspricht das Gegenteil („Was dort FEHLT, kommt weiter aus dem Paket") — das gilt für Felder,
nicht für ganze Gemeinden.

### 12. Der Kopfbereich stimmt nur durch Zufall
`ContentView.swift:75–147`

`headerAddress`, `communityEmblem` und der Name lesen `CommunityCatalog.shared` direkt.
`ContentView` beobachtet den Katalog aber nicht (`@ObservedObject` gibt es nur für die Regeln).
Dass die Kopfzeile trotzdem umspringt, liegt am Sekundentakt für den Countdown, der die Ansicht
ohnehin neu zeichnet. Nimmt jemand den Takt heraus, bleibt die alte Adresse stehen.

---

## C · Push-Mitteilungen erreichen niemanden

Das ist der schwerste Befund, und er betrifft **beide Apps**. Ich habe `functions/index.js` auf
`master` und auf `origin/android` verglichen: identisch.

### 13. Der Auslöser für Mitteilungen kann nicht feuern
`functions/index.js:46`

```
exports.onNewsCreated = onDocumentCreated("news/{id}", …)
```

Geschrieben wird seit dem Umbau auf mehrere Gemeinden nach `communities/{id}/news`
(`AdminStore.swift:240`) beziehungsweise `broadcasts`. Der Pfad `news/{id}` existiert nicht mehr.
**Seit dem Umbau geht zu keiner Mitteilung eine Push-Meldung raus** — weder auf Android noch auf
iOS. Die Mitteilung steht in der App, aber niemand erfährt davon.

### 14. Derselbe Fehler bei den Gebetszeiten
`functions/index.js:57`

`onDocumentUpdated("config/community", …)` — geschrieben wird nach
`communities/{id}/config/rules`. Ändert ein Vorstand die Ikamet, wacht kein Telefon auf.

### 15. Ein einziges Thema für alle 81 Gemeinden
`functions/index.js:30` und `PushService.swift:77`

Jedes Gerät hängt an `announcements`. Selbst mit richtigen Pfaden bekäme jeder Nutzer in
Deutschland jede Mitteilung jeder Gemeinde. Nötig wären Themen je Gemeinde, etwa
`news_<gemeinde-id>`.

### 16. Der leise Ton ist weiterhin nicht ausgerollt
`functions/index.js:41`

Steht im Code, ist nicht bereitgestellt. Ohne `firebase deploy` wirkt er nicht — und wegen Punkt
13 im Moment ohnehin nicht.

### 17. Rückfalltitel nur auf Deutsch
`functions/index.js:48`

`"Neue Mitteilung"` erreicht auch das arabische und das türkische Telefon.

---

## D · Verlorene und tote Einstellungen

### 18. Die Ramadan-Aufzeichnung geht beim Update verloren
`RamadanView.swift:170` gegen `SharedDefaults.swift:41–57`

Die abgehakten Fastentage liegen unter `f_2026-03-14`. Die Umzugsliste in die App-Gruppe kennt
`pn_`, `pw_`, `q_`, `tracker_`, `selected_`, `automute_`, `vaktija_` — **`f_` fehlt**. Wer die App
schon hat, verliert beim Update auf die Version mit Widget seine gesamte Ramadan-Aufzeichnung.

### 19. Der Schalter „Ton im Lautlos-Modus" tut nichts
`SettingsView.swift:10` und `:142`

`notif_silent` wird geschrieben und an genau **keiner** Stelle gelesen. iOS lässt eine App den
Lautlos-Schalter nicht übergehen. Das ist derselbe Fall wie die Auto-Stummschaltung, die wir
herausgenommen haben: ein Versprechen, das die Oberfläche nicht halten kann.

### 20. Ein Ton für Mitteilungen, den es nicht gibt
`SharedDefaults.swift:54`, `lang/*.strings`

`news_sound` steht in der Umzugsliste, `settings_news_sound` ist in **acht Sprachen** übersetzt —
es gibt weder eine Einstellung dafür noch jemanden, der sie liest. Übrig geblieben, als der Ton
fest auf „leise" gesetzt wurde.

### 21. Niemand erfährt, wenn die Zeiten gerechnet statt offiziell sind
`PrayerData.swift:128`

`PrayerStore.official` wird gepflegt, aber von keiner Ansicht gelesen. Wenn vaktija.eu nicht
erreichbar ist, zeigt die App gerechnete Zeiten — ohne Hinweis. Und wegen Punkt 5 sind das
Kassels Zeiten.

---

## E · Echte Fehler

### 22. Isha nach Mitternacht lässt sich nicht bestätigen
`PrayerTracker.swift:138`

```
static func record(_ prayer:, _ value:, at now: Date = Date(), on day: Date = Date())
```

Das Fenster für Isha reicht bis zum Fajr des **Folgetags**. Wer um 00:30 auf „Ja" tippt, ruft
`record` mit `day = heute` auf — das ist inzwischen der neue Tag. Geprüft wird dann gegen das
Isha-Fenster des neuen Abends, und das ist noch nicht offen: **die Antwort wird still verworfen.**
Der Tag zählt als unvollständig, die Flamme reißt.

Im Sommer liegt Isha gegen 23:30 und das Fenster offen bis etwa 03:00 — dieser Fall tritt regelmäßig ein.

### 23. „Adhan testen" stoppt Musik und Podcasts dauerhaft
`SoundPlayer.swift:12–13`

```
try? AVAudioSession.sharedInstance().setCategory(.playback …)
try? AVAudioSession.sharedInstance().setActive(true)
```

Die Sitzung wird aktiviert und nie wieder abgegeben. Wer beim Einstellen Musik hört, hört sie nach
dem Probeton nicht mehr — bis die App beendet wird.

### 24. Ein falsch getipptes Kürzel bringt die App zum Absturz
`PrayerData.swift:70`

```
URL(string: "https://vaktija.eu/\(CommunitySelection.vaktijaSlug)")!
```

Das Kürzel kommt auch aus Firestore. Trägt jemand dort ein Leerzeichen ein, gibt `URL(string:)`
nichts zurück, und das Ausrufezeichen beendet die App — bei jedem Start aufs Neue. Die 72 Kürzel
im Paket sind sauber; die Stelle ist trotzdem ungesichert.

### 25. Verbandsweite Mitteilungen sind nicht löschbar
`AdminStore.swift:272` und `NewsView.swift:57`

`deleteNews` löscht immer unter `communities/{id}/news`. Der Löschknopf ist bei verbandsweiten
Mitteilungen deshalb ausgeblendet. Der Hauptadministrator kann an 81 Gemeinden senden und es
danach nicht mehr zurücknehmen.

### 26. Die veröffentlichte Android-Version bekommt keine Ikamet-Änderung mehr
`AdminStore.swift:222`

Geschrieben wird nur nach `communities/{id}/config/rules`. Android 1.1.3 im Play Store liest
`config/community` — das Dokument wird von keiner der beiden Apps mehr beschrieben. Wer noch die
alte Version hat, sieht eingefrorene Ikamet-Zeiten. **Frage an die Android-Seite:** ist das so
gewollt, oder muss doppelt geschrieben werden, bis alle aktualisiert haben?

### 27. Vorwarnung kurz nach Mitternacht (ungefährlich, aber ungesichert)
`NotificationScheduler.swift:306`

Bei `minutes - warn < 0` entstehen negative Minuten, und die Meldung landet am Vortag — also in
der Vergangenheit und damit im Papierkorb. In Deutschland liegt kein Gebet vor 00:30, der Fall
tritt also nicht ein. Er ist nur nicht abgefangen.

---

## F · Geschwindigkeit und Kosten

### 28. Der Ramadan-Bildschirm rechnet im Sekundentakt
`RamadanView.swift:11` und `:159–171`

Die Ansicht zeichnet sich jede Sekunde neu (Countdown). `fastedCount` sucht dabei jedes Mal in
einer Schleife über **bis zu 400 Tage** den Ramadan-Anfang, mit zwei islamischen
Kalenderumrechnungen je Durchlauf, und liest danach bis zu 30 Werte aus dem Speicher. Das sind
grob 800 Umrechnungen pro Sekunde, solange der Bildschirm offen ist.

### 29. Ein Firestore-Lesevorgang je Beitrag, bei jeder Aktualisierung
`NewsRepository.swift:146`

`loadMyReactions()` läuft nach **jedem** `merge()` und holt für jeden Beitrag ein eigenes
Dokument. Bei 40 Mitteilungen und zwei Quellen sind das schnell hunderte Lesevorgänge pro Sitzung
— das zählt gegen das Firestore-Kontingent.

### 30. Neuer Datumsformatierer bei jedem Aufruf
`PrayerTracker.swift:119`

`dayKey` legt jedes Mal einen `DateFormatter` an. Beim Zählen der Flamme geschieht das fünfmal pro
Tag der Serie.

---

## G · Sicherheit

### 31. Der Gemini-Schlüssel liegt in der App
`GeminiTranslator.swift:64–69`

Der Schlüssel kommt aus `Secrets.plist` im App-Paket. Wer die App aus dem Store lädt, kann ihn
auslesen und auf Rechnung der Gemeinde übersetzen lassen. Android macht es genauso, es ist also
kein neuer Fehler — aber es bleibt einer. Sauber wäre: übersetzen in einer Cloud Function, der
Schlüssel bleibt auf dem Server.

---

## H · Aufräumen

### 32. Toter Kartenlink
`ContentView.swift:16` — `mapsURL` wird seit dem Umbau nicht mehr benutzt.

### 33. Prüfdaten in der ausgelieferten Gemeindeliste
`communities.json` — bei Kassel steht ein `_check`-Objekt mit Geokodierungs-Prüfdaten
(`geocoded_as`, `town_distance_km`, `sample_times`). Gehört nicht ins App-Paket.

### 34. Drei Texte fehlen in fünf Sprachen
`ar`, `ru`, `sq`, `tr`, `ur` fehlen `news_translating`, `news_translate_partial`,
`news_post_failed`. Die App fällt auf **Bosnisch** zurück — ein türkischer Vorstand sieht beim
Absenden bosnischen Text.

### 35. 17 Übersetzungen ohne Verwendung
Nur in `bs.strings`: `app_name`, `community_address`, `header_subtitle`, `lang_*` und
`tv_prayer_*`. Die `tv_`-Zeilen stammen vom Fernseher-Bildschirm, den es auf iOS nicht gibt.

### 36. Widget-Beschreibung nur auf Deutsch
`KasselWidget.swift:237–238` — `configurationDisplayName` und `description` stehen fest auf
Deutsch. In der Widget-Galerie sieht das auch, wer die App auf Arabisch benutzt.

### 37. Das Widget ist für eine Größe freigegeben, für die es nicht gebaut ist
`KasselWidget.swift:239` — `supportedFamilies([.systemMedium, .systemSmall])`. Der Aufbau ist
waagerecht: 64 Punkte Wappen, Flamme, Gemeindename und daneben ein 44-Punkt-Countdown. Im kleinen
Widget (155 × 155) wird das eng. **Bitte einmal ansehen** und die kleine Größe notfalls streichen.

### 38. Ein Kommentar, der nicht mehr stimmt
`Community.swift:6–8` — „Diese App zeigt weiterhin fest auf Kassel — eine Gemeindeauswahl wie auf
Android gibt es hier noch nicht." Gibt es seit dem 26.08.

### 39. Doppelte Kürzel für dieselbe Stadt
`communities.json` — `muenchen` und `munchen` zeigen beide auf München (identische Zeiten,
beide antworten), ebenso `frankfurt` und `frankfurt-am-main`. Kein Fehler, aber zwei Schreibweisen
für einen Ort laden zum nächsten Tippfehler ein.

---

## Vorschlag für die Reihenfolge

1. **C (13–17)** — Push. Betrifft beide Apps und alle 81 Gemeinden; ohne das erfährt niemand von
   einer Mitteilung.
2. **A (1–5)** — fremde Kontaktdaten und Kassels Gebetszeiten. Das ist auch der Weg zu deinem
   Ziel: Felder in die Struktur, Daten in die JSON, fertig.
3. **B (8–11)** — der Gemeindewechsel.
4. **E (22–23)** — die zwei Fehler, die man täglich merkt.
5. **D (18)** — vor dem nächsten Update, sonst sind die Fastentage weg.
6. Der Rest.

Nichts davon ist am 03.09.2026 geändert worden. Diese Datei beschreibt den Stand, nicht die Lösung.
