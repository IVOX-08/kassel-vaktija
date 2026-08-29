# iOS gegen PARITAET.md — Stand 29.08.2026

Geprüft gegen `origin/android:docs/multi-gemeinde/PARITAET.md`, Punkt für Punkt.
Abweichungen sind **gemeldet, nicht ersetzt**.

| # | Punkt | Stand |
|---|---|---|
| 1 | Reihenfolge in den Einstellungen | erfüllt |
| 2 | Gemeindezeichen | erfüllt |
| 3 | Gebetstracker | erfüllt bis auf die Knöpfe im Widget — siehe A |
| 4 | Benachrichtigungen | Töne und Antippen erfüllt; „Stopp" siehe B, Ton für Mitteilungen siehe C |
| 5 | Sonnenaufgang | erfüllt bis auf die Auto-Stummschaltung — siehe D |
| 6 | Qibla | erfüllt |
| 7 | Koran | erfüllt |
| 8 | Zakat | erfüllt, mit einer Abweichung beim Einlesen der Zahlen — siehe E |
| 9 | Gemeinden | erfüllt bis auf den Import — siehe F |
| 10 | Texte | erfüllt |

---

## Zu den einzelnen Punkten

**1 · Reihenfolge.** Administrator (nur angemeldet) · Darstellung · Gebetsbenachrichtigungen ·
Mitteilungen · Berechtigungen · Gemeinde · Sprache · App · Über uns. Kein Hauptschalter.
Berechtigungen erscheinen nur, wenn sie fehlen — sonst wäre es eine Zeile ohne Inhalt.

**2 · Gemeindezeichen.** Kassel behält sein Wappen ohne Namen. Jede andere Gemeinde bekommt das
IGBD-Verbandszeichen mit ihrem Namen darunter, **96 hoch wie Kassels Wappen** — in der App und im
Widget. Vorher stand dort ein Mondsymbol, das nichts darüber sagte, wessen Zeiten auf dem Schirm
stehen.

**3 · Gebetstracker.** Fenster vom Ikamet bis zum nächsten Adhan, Fajr bis Sonnenaufgang, Isha bis
zum Fajr des Folgetags. Nichts lässt sich nachtragen. Die Antwort aus der Benachrichtigung wird
beim Empfang **noch einmal** gegen das Fenster geprüft. Ein Tag zählt nur mit fünf Ja. Ziel 30 Tage
mit Fortschrittsanzeige. Alles bleibt auf dem Gerät.

**4 · Benachrichtigungen.** Alle Kanäle `.timeSensitive`. Antippen öffnet den richtigen Bildschirm:
Tracker-Frage → Tracker, Mitteilung → Mitteilungen. Adhan-Töne: Kurzer Adhan · Signalton · Glocke ·
Gong · Leiser Ton.

**5 · Sonnenaufgang.** Spielt den Adhan wie die übrigen Zeiten, ist **standardmäßig aus**,
Vorwarnung 0/10/20/30/40/50/60.

**6 · Qibla.** Die Nadel rechnet gegen den Kompass (magnetisch), die angezeigte Gradzahl bleibt die
wahre Richtung.

**7 · Koran.** Zwei Schriften (System und Amiri Quran, SIL OFL). Zoom 0,7–1,8 in Schritten von 0,1.
Der Umbruch misst mit genau der Schrift, Größe und dem Zeilenabstand, in dem gezeichnet wird —
Tedschwid eingeschlossen, weil die markierte Fassung manche Buchstaben anders schreibt und deshalb
nicht gleich lang ist. Tedschwid-Farben kommen mit dem Text; wo keine markierte Fassung vorliegt,
steht der schlichte Text. Querformat: kompakter Titelblock, Zeilenabstand 1,45 statt 1,95 — und nur
hier darf sich der Bildschirm überhaupt drehen.

**9 · Gemeinden.** 81 Gemeinden, Melsungen und Beverungen als Orte der Kasseler Gemeinde.
Abgeschaltete und gesperrte stehen nicht in der Auswahl. Bei gleichem Namen zeigt die Zeile die
Straße statt der Orte. Erst Gemeinde, dann Ort.

---

## Abweichungen — zur Entscheidung

### A · Ja/Nein-Knöpfe im Widget: braucht iOS 17

Knöpfe in einem iOS-Widget gibt es erst ab **iOS 17** (AppIntents). Die App steht auf **iOS 16**.
Entweder die Mindestversion steigt auf iOS 17 — dann fallen iPhone 8, X und ältere weg —, oder das
Widget zeigt den Stand des Tages an und beantwortet wird in der Meldung und in der App.
**Das ist keine Entscheidung, die hier fallen sollte.**

### B · „Stopp" beim Adhan: auf iOS nicht möglich

iOS spielt den Ton einer Benachrichtigung selbst ab. Eine App kann ihn nicht abbrechen — auch nicht
über einen Knopf in der Meldung, und auch nicht, indem sie die Meldung entfernt. Der kurze Adhan
dauert 19 Sekunden; iOS schneidet ohnehin bei 30 ab.

### C · Ton für Mitteilungen: die Auswahl steht, wirkt aber noch nicht

Die Einstellung ist gebaut (Standardton · Glocke · Leiser Ton). Auf iOS bestimmt aber der **Absender**
den Ton einer Push-Meldung, nicht das Gerät. Damit die Wahl wirkt, braucht es eines von beiden:

1. eine **Notification Service Extension** in der App, die den Ton vor dem Anzeigen austauscht, oder
2. die Cloud Function schickt den Ton mit (`apns.payload.aps.sound`) — dann gilt für alle derselbe.

Weg 1 ist der richtige, kostet aber ein weiteres Ziel im Projekt samt Signierung. **Nicht gebaut,
bis das entschieden ist.**

### D · Auto-Stummschaltung: der Schalter tut nichts

In den Einstellungen steht „Automatisch stummschalten" mit Zeiten davor und danach. Auf iOS kann
keine App das Telefon stummschalten oder „Nicht stören" einschalten — es gibt dafür keine
Schnittstelle. Der Schalter wird gespeichert und sonst nirgends gelesen.

Das ist gefährlicher als eine fehlende Funktion: Wer sich darauf verlässt, dessen Telefon klingelt
im Gebet. **Vorschlag: den Abschnitt entfernen.** Nicht getan, weil es eine sichtbare Änderung an
der Oberfläche ist.

### E · Zakat: das Einlesen der Zahlen weicht bewusst ab

Android liest in `ZakatScreen.kt` mit `toAmount()` alle Ziffern und wirft Punkt und Komma weg.
Aus **„1234.50" werden 123450** — der hundertfache Betrag, und damit die hundertfache Zakat.
Die iOS-Fassung nimmt das letzte Trennzeichen als Komma. **Das ist ein Fehler in der
veröffentlichten Android-App**, kein Unterschied im Geschmack.

### F · „Gemeinden in die Datenbank übertragen": nicht gebaut

PARITAET verlangt, dass der Import den Status **nur für neue Dokumente** schreibt, damit ein
Routine-Import keine gesperrte Gemeinde wieder sichtbar macht. Der Knopf selbst ist auf iOS nicht
gebaut — 81 Dokumente aus der App heraus zu schreiben ist ein Eingriff, der einmal und von einer
Seite kommen sollte, nicht von zwei.

### G · Fehler in den arabischen Texten der Android-Seite

`values-ar/strings.xml`, `sound_gong` steht dort als **„غوнغ"** — mitten im arabischen Wort stehen
zwei kyrillische Buchstaben (н und г). Auf iOS steht „غونغ". Bitte auf der Android-Seite
nachziehen.

---

## Der Koran-Leser: zwei Fehler, gefunden und behoben

Beide zeigten die Markierungen der Tedschwid-Daten mitten im Korantext an. Sie stehen hier, weil
die Android-Seite den zweiten ebenfalls hat.

**1 · Swift klebt die Klammer an den arabischen Buchstaben.** Swift zählt in „Buchstaben", nicht in
Zeichen. Eine `[`, auf die unmittelbar ein arabisches Beizeichen folgt — `[o[َآ]` mit einer Fatha —
gilt als EIN Buchstabe. Eine Suche nach `[` findet diese Klammer dann nicht mehr, die Markierung
bleibt stehen und wird gedruckt. Gelesen wird jetzt auf der Ebene der Unicode-Zeichen.

**2 · Markierungen stecken ineinander.** In 2:190 steht `[o[ُوٓ[s[اْ]‌ۚ]` — eine Madd-Regel, und
darin ein stummer Buchstabe. Wer die erste schließende Klammer für das Ende hält, färbt die innere
Markierung als Text ein. Betrifft 34 Ajahs. **Die Android-Fassung (`Tajweed.kt`, `tajweedAnnotated`)
macht denselben Fehler** — sie sucht mit `indexOf(']', second + 1)` ebenfalls die erste statt der
passenden Klammer. Bitte dort nachziehen.

**Geprüft:** alle 114 Suren, 6236 markierte Ajahs. Keine Klammer bleibt übrig, kein Zeichen geht
verloren.

**Eine Ajah bekommt keine Farben: 32:3.** Dort stehen in den Daten Klammern, die keine Regel sind
(`ٱفْتَرَ[ٮٰ]هُ`). Sie zu drucken hieße, Klammern in den Korantext zu setzen; sie wegzuwerfen hieße,
den Buchstaben zu ändern. Also gilt dort der schlichte Text — dieselbe Regel wie für eine Ajah ohne
markierte Fassung.

**Zum Wissen, nicht zum Ändern:** Die markierte und die schlichte Fassung sind zwei verschiedene
Schreibweisen desselben Textes. In 578 Ajahs unterscheiden sie sich in der Schreibung — etwa
`ٱلْأَخِرَةِ` gegenüber `ٱلْءَاخِرَةِ`. Das kommt aus den Quelldaten und ist auf Android identisch.
Tedschwid ist deshalb **standardmäßig aus**.

---

## Freitagsgebet (neu, steht noch nicht in PARITAET)

- Die Karte steht **am Freitag oben**, an jedem anderen Tag **unten**.
- **30 Minuten vorher** eine Erinnerung. Kein neuer Text: Titel und Zeile sind dieselben wie bei
  jeder anderen Vorwarnung.
- **Ruhe von 10 Minuten vor bis 40 Minuten nach dem Beginn** — eine Stunde und zehn Minuten. In
  dieser Zeit stellt die App keine Meldung.
- Die Tracker-Frage wird dabei **nicht verworfen, sondern ans Ende der Stille geschoben**. Ihr
  Fenster läuft bis zum Ikindija-Ruf und ist dann noch offen; wer sie nie bekäme, verlöre jeden
  Freitag seine Flamme, ohne etwas falsch gemacht zu haben.

**Grenze:** iOS lässt keine App das Telefon stummschalten oder „Nicht stören" einschalten. Still
ist also nur DIESE App. Anrufe und andere Apps klingeln weiter. Siehe auch D.

---

## Einstellungen und Widget (nach den Bildern vom 29.08.)

**Einstellungen kompakt.** Name, Vorwarnung und Schalter stehen jetzt in EINER Zeile, die Karten
sind lavendel wie auf Android. Vorher brauchte jedes Gebet zwei Zeilen; sechs Gebete füllten damit
zwei Bildschirme, und wer Vorwarnzeiten vergleichen will, muss sie zusammen sehen.

**Widget.** Wappen links im weißen Kreis, Gebetsname in Gold, Countdown, „verbleibend · HH:mm" —
und darunter **Flamme, Serie und Tagesstand** (`🔥 3 · 2 / 5`). Für jede andere Gemeinde steht dort
das IGBD-Verbandszeichen statt Kassels Wappen, gleich groß.

Antworten lässt sich im Widget weiterhin nicht — siehe A.

---

## Was einmal von Hand eingerichtet werden muss

**App-Gruppe im Apple-Developer-Portal.** Das Widget lief bisher in seinem eigenen Sandkasten und
sah weder die gewählte Gemeinde noch die geladenen Zeiten noch die Sprache — es zeigte jedem
Kassel. Beides teilt sich jetzt einen Speicher, und der braucht eine App-Gruppe:

1. Certificates, Identifiers & Profiles → Identifiers → **App Groups** → `group.de.igbdsandzakkassel.vaktija`
2. Bei **beiden** App-IDs anhaken: `de.igbdsandzakkassel.vaktija.ios` und `…ios.widget`

Ohne diesen Schritt schlägt das Signieren fehl — mit einer Meldung, die genau diese Gruppe nennt.
