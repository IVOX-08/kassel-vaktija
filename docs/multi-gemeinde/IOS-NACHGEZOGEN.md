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

## Was die Android-Seite noch tun muss

⚠️ **Die Admin-Kennung der iOS-App braucht einen Eintrag unter `admins/{uid}`:**

```
admins/1a7xqRgIYDR0RZqa3KghBlz98PK2
  role: "community"
  communityId: "igbd-gemeinde-sandzak-kassel"
```

Ohne diesen Eintrag bekommt der Vorstand beim Posten vom iPhone `PERMISSION_DENIED` — die Regeln
sind seit dem 26. August scharf, die iOS-App hatte vorher als einzige Kennung Schreibrecht.

⚠️ **Anonyme Anmeldung muss in der Firebase-Konsole aktiviert sein**
(Authentication → Sign-in method). Ist sie es nicht, funktioniert alles weiter — nur die
Reaktionsknöpfe bleiben wirkungslos.

## Bewusst nicht gebaut

- **Gemeindeauswahl.** Die iOS-App zeigt weiterhin fest auf Kassel. Laut Übergabedokument in
  Ordnung; sie liest jetzt nur den richtigen Pfad.
- **`events`.** Die iOS-App schreibt keine Veranstaltungen. Die Android-App liest sie ohnehin noch
  nicht — wenn das kommen soll, sollten beide Seiten es zusammen planen.

## Nicht auf echten Daten geprüft

Der Code kompiliert und die App startet. **Ob Reaktionen und verbandsweite Mitteilungen wirklich
durchgehen, ist nicht getestet** — dazu müssten die Regeln greifen und echte Beiträge vorliegen.
Bitte auf der Android-Seite gegenprüfen, sobald die Admin-Kennung eingetragen ist.
