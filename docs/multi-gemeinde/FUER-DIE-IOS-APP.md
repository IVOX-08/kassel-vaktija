# Für die iOS-Seite: was sich in der Datenbank geändert hat

Dieses Dokument richtet sich an die iOS-App (Mac-Seite). Die Android-App wurde von **einer**
Gemeinde auf **zwanzig** umgebaut. Die Firestore-Datenbank ist dieselbe für beide Apps — die
Änderungen unten betreffen die iOS-App also unmittelbar, auch ohne dass dort eine Zeile geändert
wurde.

Stand: 26. August 2026. Projekt `kassel-vaktija`.

---

## 1. Die Gebetszeiten sind umgezogen

**Vorher** — ein einziges Dokument für alle:

```
config/community
  fajrIqamah, jumua, dhuhrOffsetMin, asrOffsetMin, maghribOffsetMin, ishaOffsetMin, updatedAt
```

**Jetzt** — eines pro Gemeinde:

```
communities/{communityId}/config/rules
  (dieselben Felder)
```

Der Grund: mit zwanzig Gemeinden hätten sich alle Kassels Iqamah und Jumu'ah geteilt. Eine
Gemeinde, deren Jumu'ah als 15:00 angekündigt wird, schickt ihre Leute in einen leeren Gebetsraum.

⚠️ **`config/community` bleibt bestehen und darf nicht gelöscht werden.** Die im Play Store
veröffentlichte Android-Version 1.1.3 liest noch von dort. Erst wenn niemand mehr die alte Version
benutzt, darf es weg. Wer die Zeiten dort ändert, ändert sie NICHT für die neue Version — und
umgekehrt. Bis zur nächsten Veröffentlichung müssen Kassels Zeiten an beiden Stellen gepflegt
werden, wenn sie sich ändern.

---

## 2. Der Aufbau der Datenbank

```
communities/{id}                     Verzeichnis: name, address, email, locations[],
                                     donationUrl?, logoUrl?, imamName?, imamPhone?, status?
communities/{id}/config/rules        Iqamah + Jumu'ah dieser Gemeinde
communities/{id}/news/{newsId}       Beiträge dieser Gemeinde (+ likeCount, dislikeCount)
communities/{id}/news/{id}/reactions/{uid}   wer wie reagiert hat
communities/{id}/news_images/{id}    Bilder zu Beiträgen

broadcasts/{id}                      verbandsweite Mitteilungen des Hauptadministrators
broadcasts/{id}/reactions/{uid}
broadcast_images/{id}

admins/{uid}                         role: 'head' | 'community', communityId (nur bei 'community')
admin_alerts/{id}                    gemeldete Anmeldeversuche bei fremder Gemeinde

events/{id}                          Veranstaltungen — von der iOS-App geschrieben
config/community                     ALT, nur noch für Version 1.1.3
news/{id}                            ALT, dito
```

`status` fehlt oder `"active"` = die Gemeinde ist sichtbar. `"suspended"` = nicht in der Auswahl,
Gebetszeiten bleiben. `"blocked"` = ganz weg.

Die zwanzig Kennungen stehen in `docs/multi-gemeinde/communities.json`, die Kennung von Kassel ist
`igbd-gemeinde-sandzak-kassel`.

---

## 3. Sicherheitsregeln sind seit heute scharf

Vorher durfte **nur eine einzige Kennung** schreiben. Jetzt gilt:

| Sammlung | lesen | schreiben |
|---|---|---|
| `communities/*`, `config/rules`, `news`, `broadcasts`, `events` | alle | siehe unten |
| `admins`, `admin_alerts` | nur der Hauptadministrator (bzw. der eigene Eintrag) | Hauptadministrator |

Geschrieben werden darf nur mit einem Konto, das einen Eintrag unter `admins/{uid}` hat:

- `role: 'head'` — darf überall schreiben
- `role: 'community'` + `communityId` — darf nur bei **dieser** Gemeinde schreiben

⚠️ **Für die iOS-App wichtig:** `events` darf jetzt **nur der Hauptadministrator** schreiben. Wenn
die iOS-App Veranstaltungen mit einem anderen Konto anlegt, bekommt sie ab sofort
`PERMISSION_DENIED`. Dann braucht dieses Konto einen Eintrag unter `admins/{uid}` — sag Bescheid,
welche UID die iOS-App benutzt.

Der komplette Regelsatz steht in `FIRESTORE-REGELN.md`.

---

## 4. Reaktionen

Herz und Daumen liegen als `reactions/{uid}` unter dem Beitrag; die Summen stehen als
`likeCount` / `dislikeCount` am Beitrag selbst und werden mit `FieldValue.increment` bewegt.

Ein normaler Leser darf am Beitrag **ausschließlich** diese beiden Felder ändern — jede andere
Änderung wird abgelehnt. Wer als iOS-Client einen Zähler hochsetzt, muss also einen `update`
schicken, der nichts anderes anfasst.

Dafür muss das Gerät angemeldet sein. Anonyme Anmeldung ist im Projekt aktiviert; die Android-App
meldet jedes Gerät beim Start still anonym an, damit eine Reaktion einem Absender gehört und wieder
zurückgenommen werden kann.

---

## 5. Was die iOS-App noch nicht hat

Die Android-App kennt `events` nicht — die Sammlung wird dort weder gelesen noch geschrieben. Wenn
Veranstaltungen auf beiden Seiten erscheinen sollen, muss das auf Android noch gebaut werden.

Umgekehrt kennt die iOS-App die Gemeindeauswahl noch nicht. Solange sie fest auf Kassel zeigt,
funktioniert sie weiter — sie muss dann nur `communities/igbd-gemeinde-sandzak-kassel/config/rules`
lesen statt `config/community`.

---

## 6. Zwei getrennte Historien

⚠️ Der Zweig `android` (dieser PC) und die iOS-Linie (Mac) sind seit `faaa96c` getrennt gewachsen.
**Niemals mit `--force` überschreiben.** Wer zusammenführen will, macht das über einen Merge, den
beide Seiten vorher besprechen.
