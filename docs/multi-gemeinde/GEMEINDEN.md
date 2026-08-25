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

## ⚠️ Was noch nicht stimmt

**Die Namen sind Platzhalter.** Ich habe „IGBD-Gemeinde \<Stadt\>" eingesetzt. Die echten Namen
weichen ab — Bremen heißt z. B. „Gemeinde bosnischer Moslems e.V.", Rosenheim „Bosnischer
Kulturverein e.V.". **Vor der Veröffentlichung durch die echten Namen ersetzen.**

**Logos fehlen komplett.** Kommt mit dem Anschreiben.

**Iqamah- und Džuma-Zeiten fehlen.** Die kann niemand außer der Gemeinde selbst liefern.

**Es fehlen Gemeinden.** 19 von rund 76. Nicht gefunden wurden alle ohne eigene Webseite und alle
mit anderem Adressmuster.

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
