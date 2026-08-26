# Gefundene Gemeinden — Stand und Vorbehalte

`communities.json` enthält **19 Gemeinden**, jede mit geprüften Koordinaten und einem Ort, der
nachweislich Gebetszeiten liefert. Erzeugt mit `tools/add_community.py`.

## Woher die Daten stammen

**Nicht vom Verband.** Die Gemeindeliste auf `igbd.org/de/dzemati` ist praktisch leer — sie zeigt
genau **einen** Eintrag (Aachen), und die Regionalfilter (Medžlis Hannover, Frankfurt, …) liefern
null. Als Quelle fällt sie aus.

Stattdessen: Die meisten Gemeinden haben eigene Webseiten nach festem Muster (`dzemat-<stadt>.de`,
`igbd-<stadt>.de`). Von 39 antwortenden Domains ließ sich bei 18 eine Anschrift aus Impressum oder
Startseite auslesen. Drei davon habe ich von Hand nachkorrigiert, weil das Muster den Straßennamen
verstümmelt hatte.

## Namen

Die Namen stammen aus den Impressen der Gemeindeseiten, also die eingetragenen Vereinsnamen —
nicht bloß Stadtnamen. Sie unterscheiden sich deutlich voneinander:

- „Gemeinde bosnischer Moslems e.V. — Džemat Bremen"
- „Bosniakisch-Deutsche Gemeinde Karlsruhe e.V."
- „IGBD-Džemat BKC Siegen e.V."
- „Džemat IKRE Berlin e.V."
- „IGBD-Gemeinde Böblingen/Sindelfingen e.V." (eine Gemeinde für zwei Städte)

Das ist genau der Grund, warum Stadtnamen nicht reichen: In Berlin gibt es mehrere Gemeinden, und
in der Auswahl unterscheiden sie sich nur am Namen.

**Trotzdem gegenlesen lassen.** Ein Impressum ist keine Selbstbezeichnung — manche Gemeinde nennt
sich im Alltag anders, als sie im Vereinsregister steht. Bei der Rückmeldung auf das Anschreiben
sollte jede Gemeinde ihren Anzeigenamen bestätigen.

## Iqamah und Džuma

Jeder Eintrag bekommt **Kassels Werte als Ausgangspunkt** (Fajr-Iqamah 05:15, Džuma 15:00,
Versatz +10/+10/+5/+0). Das ist eine Vorgabe, keine Behauptung — jede Gemeinde bestätigt oder
korrigiert sie in der Rückmeldung und kann sie danach selbst in der App ändern.

## ⚠️ Was noch fehlt

**Logos** und **Spendenlinks** — bewusst leer gelassen, die lassen sich nicht erraten.

**Gemeinden.** 20 von rund 76. Nicht gefunden wurden alle ohne eigene Webseite und alle mit
anderem Adressmuster.

## Was verlässlich ist

- **Koordinaten** — aus der Anschrift geokodiert, also die Moschee und nicht die Stadtmitte
- **Gebetszeiten-Ort** — für jeden Eintrag wurde geprüft, dass die Seite wirklich Zeiten liefert

## Ein Fund am Rande

vaktija.eu führt **13 Orte mit den Koordinaten 0,0**, darunter **Offenburg**. Das ist nicht nur
kosmetisch: Die dort veröffentlichten Zeiten passen nicht zum echten Ort — Offenburgs Sonnenuntergang
lag *vor* dem von Karlsruhe, 60 km nordöstlich, was geografisch unmöglich ist. Das Werkzeug sortiert
solche Orte deshalb aus; Offenburg nutzt jetzt Höhberg, 6,8 km entfernt, dessen Zeiten stimmig sind.

**Wer künftig Orte von Hand einträgt, sollte das im Blick behalten** — ein Ort mit plausibel
aussehenden, aber falschen Zeiten fällt sonst niemandem auf.

## Weitere Gemeinde hinzufügen

```
python tools/add_community.py "Echter Name e.V." "Straße 1, 12345 Stadt"
```

Oder mehrere auf einmal, eine Zeile je Gemeinde, Name und Anschrift durch einen Tabulator getrennt:

```
python tools/add_community.py --file gemeinden.tsv
```

Das Werkzeug schreibt nach `communities.json` und prüft dabei jeden Eintrag. Achte auf die
Ausgabe: `ueber Name` heißt, der Ort wurde über den Ortsnamen gefunden; `ueber Nähe` heißt, der
Ort selbst fehlt bei vaktija.eu und es wird der nächstgelegene verwendet — bei mehr als etwa
15 km lohnt ein Blick, ob das noch vertretbar ist.
