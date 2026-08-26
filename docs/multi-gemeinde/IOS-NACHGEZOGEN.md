# iOS ist nachgezogen — Antwort auf FUER-DIE-IOS-APP.md

Stand: 26. August 2026. Die iOS-App folgt jetzt dem Aufbau mit zwanzig Gemeinden.

## Was umgestellt wurde

| Vorher | Jetzt |
|---|---|
| `config/community` | `communities/igbd-gemeinde-sandzak-kassel/config/rules` |
| `news/{id}` | `communities/{id}/news/{newsId}` |
| `news_images/{id}` | `communities/{id}/news_images/{id}` |

Alle Pfade stehen jetzt in **einer** Datei (`iosApp/iosApp/Community.swift`) statt als
Zeichenketten in vier. Beim nächsten Umzug ist nur diese eine anzufassen.

## Was dazugekommen ist

- **Verbandsweite Mitteilungen.** Die App liest `broadcasts` und mischt sie nach Datum unter die
  Beiträge der Gemeinde. Ein Abzeichen („Verband" / „Savez" / …, in allen acht Sprachen) zeigt,
  woher eine Mitteilung kommt — sonst wäre nicht erklärbar, warum etwas auftaucht, das der eigene
  Vorstand nie geschrieben hat.
- **Reaktionen.** Herz und Daumen, mit demselben Format wie Android: `reactions/{uid}` mit dem Feld
  `value: "like" | "dislike"`, Summen als `likeCount` / `dislikeCount` per `increment`. Derselbe
  Knopf noch einmal nimmt die Reaktion zurück.
- **Anonyme Anmeldung** beim Start, wie `SessionManager` auf Android. Ohne sie gehört ein Herz
  niemandem und ließe sich nicht zurücknehmen.

## Korrektur einer falschen Anweisung

Eine frühere Fassung dieses Dokuments forderte, `admins/1a7xqRgIYDR0RZqa3KghBlz98PK2` auf
`role: "community"` zu setzen. **Das war falsch und hätte Schaden angerichtet.**

Diese Kennung gehört dem Besitzer und trägt `role: "head"`. Sie herabzustufen hätte ihm
Gemeindeverwaltung, Rundnachrichten und das Recht genommen, Zugänge zu vergeben — also auch das
Recht, den Fehler rückgängig zu machen. Der Schluss kam daher, dass die alte iOS-Regel nur diese
eine Kennung schreiben ließ; daraus wurde fälschlich ein Gemeinde-Konto abgeleitet.

Ebenso falsch war die Annahme, der Vorstand könne vom iPhone aus nicht mehr posten. Das Konto ist
nicht blockiert, es hat mehr Rechte als vorher.

**An bestehenden Einträgen ist nichts zu ändern.** Der Vorstand hat mit
`Vpalzb0gitTTLUQDFYPLcbIbFXG3` bereits eine eigene Kennung.

## Welches Konto die iOS-App benutzt

Keins fest — **das war das eigentliche Problem.** Die App hatte `1a7xq…` im Code stehen und meldete
jedes andere Konto sofort wieder ab. Der Vorstand hätte sich mit seiner eigenen Kennung **nicht**
anmelden können, obwohl der Server ihn längst zulässt.

Das ist jetzt behoben: Die App liest die Rolle aus `admins/{uid}`, genau wie die Android-App seit
„Drop the hard-coded ADMIN_UID". Ein neuer Eintrag wird also nicht gebraucht — jedes Konto, das die
Android-Seite anlegt, funktioniert auf iOS ohne App-Änderung.

## events

Die iOS-App schreibt **nichts** nach `events`. Die einzigen Treffer im Code sind `eventSchedule`
aus dem JSON-LD von vaktija.eu — etwas völlig anderes. Kein Handlungsbedarf.

## Bewusst nicht gebaut

- **Gemeindeauswahl.** Die iOS-App zeigt weiterhin fest auf Kassel. Laut Übergabedokument in
  Ordnung; sie liest jetzt nur den richtigen Pfad.
- **`events`.** Die iOS-App schreibt keine Veranstaltungen. Die Android-App liest sie ohnehin noch
  nicht — wenn das kommen soll, sollten beide Seiten es zusammen planen.

## Nicht auf echten Daten geprüft

Der Code kompiliert und die App startet. **Ob Reaktionen und verbandsweite Mitteilungen wirklich
durchgehen, ist nicht getestet** — dazu müssten die Regeln greifen und echte Beiträge vorliegen.
Bitte auf der Android-Seite gegenprüfen, sobald die Admin-Kennung eingetragen ist.
