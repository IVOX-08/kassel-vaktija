# Was auf beiden Seiten gleich sein muss

Dieses Dokument ist die Prüfliste für die iOS-Seite. Es beschreibt **Verhalten**, nicht Code.

## Warum es das gibt

Die iOS-App kann nie 1:1 derselbe Code sein wie die Android-App — die eine ist Kotlin mit Compose,
die andere Swift. Wer den Android-Code liest und „genauso" baut, trifft dabei hunderte kleine
Entscheidungen neu: eine Zeile weiter oben, ein anderer Standardwert, eine Frage anders formuliert.
Jede für sich harmlos, zusammen zwei verschiedene Apps.

Deshalb steht hier, **was** gelten muss. Wo etwas hier nicht steht, ist es frei. Wo es hier steht,
ist es verbindlich — und wenn die iOS-Seite einen guten Grund hat, davon abzuweichen, gehört das
besprochen und hier geändert, nicht still anders gebaut.

**Regel für die Mac-Seite: was hier steht, wird geprüft und abgehakt. Was nicht geht, wird gemeldet
— nicht ersetzt.**

---

## 1 · Reihenfolge in den Einstellungen

Von oben nach unten:

1. Administrator (nur wenn angemeldet)
2. Darstellung
3. Gebetsbenachrichtigungen
4. Mitteilungen
5. Berechtigungen
6. **Gemeinde**
7. **Sprache**
8. App
9. Über uns

Gemeinde und Sprache stehen unten und in dieser Reihenfolge. Beides wird einmal gewählt und nie
wieder angefasst; der oberste Platz gehört dem, was man oft braucht. Ganz oben wäre die Gemeinde
ein Antipp-Risiko — und wer versehentlich wechselt, sieht ab dann die Zeiten einer fremden Stadt.

**Es gibt keinen Hauptschalter für Benachrichtigungen.** Er schaltete mit einem Tipp alles ab und
wer ihn einmal gedrückt hatte, fand selten zurück. Die einzelnen Schalter bleiben.

---

## 2 · Das Gemeindezeichen

| Gemeinde | Zeichen | Name darunter |
|---|---|---|
| IGBD-Gemeinde Sandžak-Kassel | eigenes Wappen | nein |
| jede andere | IGBD-Verbandslogo | ja |

**Gleich groß**, 96 dp Höhe, in der App und im Widget. Ein kleineres Zeichen für die anderen liest
sich wie zweite Klasse.

Die Markenfarben, die erlaubten Fassungen des Zeichens und die Regeln aus dem Handbuch der
Islamska zajednica stehen in **`docs/marke/README.md`**. Dort liegen auch die Originaldateien.
Kurz:

| Wofür | Hex |
|---|---|
| das **Zeichen** — IZ zelena (Pantone 356 C) | `#008348` |
| das **Zeichen** — IZ zlatna (Pantone 871 C) | `#A59573` |
| **Schrift** der App, Gold | `#B8860B` |
| **Schrift** der App, helles Gold | `#D4AF37` |
| App-Grün | `#008348` |

⚠️ **Die Markenfarben gelten für das Zeichen, nicht für die Schrift.** Die Schrift behält das warme
Gold, das die App immer hatte. Auf der Android-Seite wurde das einmal verwechselt und die ganze App
umgefärbt — die Tafel in der Moschee verlor damit das Gold, an dem sie aus der Entfernung erkannt
wird. Ist zurückgenommen; nicht wiederholen.

Auf hellem Grund die positive Fassung des Zeichens, auf schwarzem die offizielle Negativ-Fassung
(weiß). Das Zeichen wird **nicht** umgefärbt.

**Falle beim Zeichen auf dem Fernseher:** Die TV-Tafel malt immer eine helle Seite, unabhängig
davon, was der Fernseher selbst für Tag/Nacht eingestellt hat. Wer dort das
theme-abhängige Zeichen anfordert, bekommt auf einem Gerät im Nachtmodus die weiße Fassung — weiß
auf weißer Karte. Der Fernseher braucht eine feste positive Fassung. Umgekehrt wird die markierte
Zeile der Auswahl IZ-grün: dort gehört die weiße Fassung hin, sonst verschwindet der Halbmond.

---

## 2b · Aussehen und Aufbau

**Der Name der App ist „IGBD Vaktija".** Die Paketkennung `de.igbdsandzakkassel.vaktija` bleibt,
wie sie ist — sie ist die Identität im Store und lässt sich nach der Veröffentlichung nicht mehr
ändern. Auf iOS gilt dasselbe für die Bundle-ID.

**Karten sind weiß auf hellgrauer Seite**, mit einer Haarlinie. Nichts darf einen Lila- oder
Rosastich haben. Auf Android kam der daher, dass Material jede nicht gesetzte Farbrolle aus seiner
eigenen Palette füllt und `Card` auf `surfaceContainerLow` zeichnet — eine Rolle, die niemand
gesetzt hatte. Wenn iOS eine ähnliche Automatik hat, gilt dasselbe: nichts der Voreinstellung
überlassen.

**Zeilen für Gemeinde und Sprache:** rundes Symbol links, der **Wert** als grüne Überschrift, das
Etikett als graue Zeile darunter, Pfeil rechts. Der Blick fällt auf „Kassel" und „Deutsch", nicht
auf die Wörter „Gemeinde" und „Sprache".

**Social-Links** — Instagram, Facebook, YouTube — stehen neben der Überschrift der Mitteilungen,
nicht am Fuß der Liste. Jede Gemeinde trägt die Konten des Verbands, bis sie eigene schickt;
Kassel hat eigene. Die Symbole sind schlichte einfarbige Zeichen in den Farben der App, keine
nachgemalten Markenlogos.

**Die TV-Auswahl hat ein Suchfeld.** Einundachtzig Gemeinden sind zu viele, um mit dem Steuerkreuz
daran vorbeizulaufen.

---

## 3 · Gebetstracker

**Das Fenster ist die ganze Funktion.** Ohne es ist die Flamme wertlos.

| Gebet | Fenster öffnet | Fenster schließt |
|---|---|---|
| Fajr | Ikamet | **Sonnenaufgang** |
| Dhuhr (Fr: Džuma) | Ikamet bzw. Džuma-Zeit | Asr-Adhan |
| Asr | Ikamet | Maghrib-Adhan |
| Maghrib | Ikamet | Isha-Adhan |
| Isha | Ikamet | Fajr-Adhan des Folgetags |

- Nach dem Fenster lässt sich **nichts** nachtragen. Ein spätes „Ja" wird verworfen.
- Die Antwort wird **beim Empfang noch einmal** gegen das Fenster geprüft, nicht nur beim Anzeigen.
  Eine Benachrichtigung kann stundenlang liegen bleiben.
- Ein Tag zählt nur, wenn **alle fünf** mit Ja beantwortet sind.
- Ein „Nein" oder ein abgelaufenes Fenster setzt die Flamme auf **0**.
- Ziel: **30 Tage** → Geschenk der Gemeinde. Der Fortschritt wird angezeigt.
- Alles bleibt auf dem Gerät. Nichts wird übertragen.

Gefragt wird zum Ikamet, mit **Ja** und **Nein** in der Benachrichtigung und im Widget.

---

## 4 · Benachrichtigungen

- **Alle Kanäle sind „wichtig"** (auf iOS: `.timeSensitive` bzw. kritische Darstellung).
- **Kein „Stopp"-Knopf** — außer beim aufgenommenen Adhan. Der läuft über eine Minute, und wer in
  der Arbeit oder Schule ist, braucht einen Abbruch. Die kurzen Töne sind nach zwei Sekunden vorbei;
  ein Stopp darunter wäre ein Knopf, den niemand rechtzeitig erreicht.
- **Antippen öffnet den Bildschirm, um den es geht:** die Tracker-Frage öffnet den Tracker, eine
  Mitteilung öffnet die Mitteilungen. Nicht die Startseite.

### Töne

| Zweck | Auswahl |
|---|---|
| Adhan | Kurzer Adhan · Signalton · Glocke · Gong · Leiser Ton |
| Mitteilungen | Standardton · Glocke · Leiser Ton |

Zwei getrennte Einstellungen. Beide werden in verschiedenen Lagen gehört und sollen ohne Hinsehen
unterscheidbar sein.

---

## 5 · Sonnenaufgang

- Spielt den Adhan **wie die übrigen Zeiten**.
- Bleibt **standardmäßig ausgeschaltet** — ein ungefragter Ruf bei Tagesanbruch weckt Leute, die
  ihn nie wollten.
- Schaltet das Telefon **nicht** stumm: dort ist keine Gemeinschaft, für die man leise sein müsste.
- Vorwarnung: **0 / 10 / 20 / 30 / 40 / 50 / 60** Minuten.

---

## 6 · Qibla

Die Richtung zur Kaaba wird gegen **geografisch Nord** berechnet, der Kompass meldet gegen
**magnetisch Nord**. Beides direkt zu vergleichen ist falsch — über Deutschland 3 bis 5 Grad, mehr
je weiter östlich oder nördlich.

- Die **Nadel** rechnet mit der magnetischen Deklination.
- Die **angezeigte Gradzahl** bleibt die wahre Richtung (Kassel ≈ 130°), weil das die Zahl ist, die
  für eine Stadt veröffentlicht wird.

---

## 7 · Koran

- Zwei Schriften: die Systemschrift und **Amiri Quran** (osmanisch/türkisch, SIL OFL).
- **Zoom in Stufen**, 0,7 bis 1,8. Der Seitenumbruch misst mit **genau dem Stil**, in dem gezeichnet
  wird — sonst brechen die Seiten falsch.
- **Tedschwid-Farben** kommen MIT dem Text. Die App errät keine Regel. Wo eine Ajah keine markierte
  Fassung hat, wird der schlichte Text gezeigt — nie geraten eingefärbt.
- Querformat: kompakter Titelblock, Zeilenabstand 1,45 statt 1,95.

---

## 8 · Zakat

- Alles wird von Hand eingegeben. **Kein Goldpreis aus dem Netz.**
- Unter dem Nisab wird **0** angezeigt, kein kleiner Betrag.
- Eingabefelder nehmen „1.234,50" und „1234.50".
- Hinweis: Rechenhilfe, keine Fatwa.

---

## 9 · Gemeinden

- Quelle ist IGBDs eigenes Verzeichnis (`igbd.org/bs/dzemati`), 81 Gemeinden.
- Eine **neue** Gemeinde wird **abgeschaltet** angelegt.
- Der Import schreibt den Status **nur** für Dokumente, die es noch nicht gibt — sonst macht ein
  Routine-Import eine gesperrte Gemeinde wieder sichtbar.
- Bei **gleichem Namen** zeigt die Auswahl die Straße statt der Orte (zweimal „Džemat Stuttgart").
- Erst Gemeinde wählen, dann Ort — nicht umgekehrt. In Berlin gibt es zwei Gemeinden.

---

## 10 · Texte

Acht Sprachen: **bs (Standard), de, en, ar, tr, sq, ur, ru.**

Neue Texte werden **nicht neu erfunden**. Sie stehen in
`app/src/main/res/values*/strings.xml` und werden von dort übernommen:

```
git show origin/android:app/src/main/res/values-de/strings.xml | grep tracker_
```

---

## Wie eine Änderung übernommen wird

1. `git fetch origin` und den Zweig `android` lesen.
2. Die Commit-Botschaften lesen — dort steht bei jeder Entscheidung **warum**, und das Warum ist
   das, was übernommen werden muss.
3. Gegen dieses Dokument prüfen, Punkt für Punkt.
4. **Abweichungen melden, nicht ersetzen.** Wenn etwas auf iOS nicht geht oder dort anders gehört,
   ist das eine Nachricht an die Android-Seite und eine Änderung an diesem Dokument — keine stille
   Eigenlösung.
