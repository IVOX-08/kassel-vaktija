# Das Zeichen und die Farben

Verbindlich für **beide** Apps, Android wie iOS.

Grundlage ist die *Knjiga grafičkih standarda* der Islamska zajednica u Bosni i Hercegovini,
Version 1.0. Beide Originaldateien liegen in diesem Ordner:

| Datei | Inhalt |
|---|---|
| `Znak 1_0 pozitiv i negativ.pdf` | das Zeichen als Vektor, positiv und negativ |
| `Knjiga standarda - Znak i boje.pdf` | Kapitel 1 (Zeichen) und 2 (Farben) des Handbuchs |
| `play_store_icon_512.png` | 512 × 512 für die Play Console |

---

## Die Farben

Das sind keine Näherungen. Sie stehen so in Kapitel 2.1 und werden nicht verschoben.

| | Pantone | RGB | Hex |
|---|---|---|---|
| **IZ zelena** | 356 C | 0 · 131 · 72 | `#008348` |
| **IZ zlatna** | 871 C | 165 · 149 · 115 | `#A59573` |
| **Crna** | Black | 0 · 0 · 0 | `#000000` |

Das **Zeichen** wurde vorher aus einer Website-Grafik gebaut, deren Grün fast schwarz war. Jetzt
kommt es aus dem Vektor und trägt diese Farben exakt.

### ⚠️ Diese Farben gelten für das ZEICHEN — nicht für die Schrift der App

Das ist der wichtigste Satz auf dieser Seite.

Das Handbuch regelt das **Zeichen**. Die Beschriftung einer App regelt es nicht. IZ Zlatna ist ein
blasses, graustichiges Gold; als Schriftfarbe nimmt es der App das warme Gold, an dem die Tafel in
der Moschee aus der Entfernung erkannt wird.

| Verwendung | Farbe |
|---|---|
| das **Zeichen** | `#008348` / `#A59573` — die geschützten Farben, ohne Ausnahme |
| **Schrift** in der App und auf der TV-Tafel | `#B8860B`, hell `#D4AF37` — unverändert |

Die Schriftfarben stehen nicht im Handbuch und sollen es auch nicht. Sie sind seit der ersten
Fassung der App so und bleiben so.

**Das war ein Fehler auf der Android-Seite:** Die Markenfarben wurden aus dem Handbuch auf die ganze
App übertragen, auch auf die Schrift. Das ist zurückgenommen. Die iOS-Seite darf ihn nicht
wiederholen.

---

## Welche Fassung wo

Kapitel 2.2 erlaubt vier Farbvarianten. Wir benutzen zwei davon:

| Untergrund | Fassung | Datei (Android) |
|---|---|---|
| hell | **Standard** — grüner Halbmond, goldener Stern | `drawable-nodpi/logo_igbd.png` |
| schwarz | **Negativ CB** — das Zeichen ganz in Weiß | `drawable-night-nodpi/logo_igbd.png` |

Auf iOS entsprechend: das positive Bild im Light Appearance, das weiße im Dark Appearance.

⚠️ **Das Zeichen wird nicht umgefärbt.** Eine früher von Hand aufgehellte grüne Fassung für den
dunklen Modus war unzulässig — dafür gibt es die offizielle Negativ-Fassung.

---

## Regeln aus dem Handbuch, die uns betreffen

Aus Kapitel 1.3 und 4.0:

- **Freiraum:** rund um das Zeichen bleibt mindestens `x4` frei (`x` = die Höhe eines Sternarms).
  Nichts darf hineinragen.
- **Nicht erlaubt:** Schatten, Effekte, das Verschieben von Elementen im Zeichen, unproportionales
  Verkleinern einzelner Elemente, das Drehen des Zeichens.
- **Mindestgröße:** in Farbe nicht unter 4,2 mm Breite im Druck. Auf dem Bildschirm ist das kein
  Thema, im Aushang schon.

---

## Wer zeigt welches Zeichen

| Ort | Zeichen |
|---|---|
| IGBD-Gemeinde Sandžak-Kassel | ihr **eigenes** Wappen |
| jede andere Gemeinde | das **Verbandszeichen** mit dem Gemeindenamen darunter |
| App-Symbol, Begrüßung, gesperrte Gemeinde, TV-Ladebildschirm, Absender einer Rundnachricht | das **Verbandszeichen** |

Alle Zeichen werden **gleich groß** dargestellt (96 dp in der App). Ein kleineres Zeichen für die
anderen Gemeinden liest sich wie zweite Klasse.

---

## Bilder neu erzeugen

Alles in `res/mipmap-*`, `res/drawable-nodpi/logo_igbd.png` und
`res/drawable-night-nodpi/logo_igbd.png` ist erzeugt, nicht von Hand gebaut:

```
python tools/gen_brand_assets.py
```

Das Skript rendert den Vektor aus dem PDF und stellt das Weiß frei. Es rät die Form nicht nach.

Eine Feinheit, die beim ersten Versuch schiefging und die auf iOS genauso gilt: Man darf das Weiß
**nicht** über die Helligkeit freistellen. Das funktioniert für den dunklen Halbmond und ruiniert
den hellen Stern — der wird dabei halbdurchsichtig und verblasst über hellem Grund. Jeder Bildpunkt
muss stattdessen als Mischung aus Weiß und **einer der beiden** Markenfarben gelesen werden.
