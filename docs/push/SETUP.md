# Push-Mitteilungen — Einrichtung

> **Stand 04.09.2026 — wichtig:** Seit dem Umbau auf mehrere Gemeinden ging zu **keiner**
> Mitteilung eine Push-Meldung raus. Die Auslöser horchten auf `news/{id}` und `config/community`;
> geschrieben wird seitdem nach `communities/{id}/news`, `broadcasts` und
> `communities/{id}/config/rules`. Beide Pfade existierten nicht mehr, also feuerte nichts — auf
> beiden Plattformen, ohne Fehlermeldung. `functions/index.js` ist deshalb neu geschrieben.
>
> **Der Code ist fertig, aber noch nicht bereitgestellt.** Bis jemand `firebase deploy` ausführt,
> bleibt es dabei, dass niemand von einer neuen Mitteilung erfährt.

---

## Die Themen

Jedes Gerät hängt an genau drei Themen. Sie tragen Gemeinde und Sprache im Namen:

| Thema | Inhalt |
|---|---|
| `c_<gemeinde>` | Datenmeldung: die Gebetszeiten dieser Gemeinde haben sich geändert |
| `c_<gemeinde>_<sprache>` | Mitteilungen dieser Gemeinde, im richtigen Wortlaut |
| `b_<sprache>` | verbandsweite Mitteilungen des Hauptadministrators |

Vorher hing jedes Gerät am einen Thema `announcements`. Damit hätte jeder Nutzer in Deutschland
jede Mitteilung jeder der 81 Gemeinden bekommen.

Die **Sprache steht im Namen**, weil der Text schon beim Verfassen in alle acht Sprachen übersetzt
und mitgespeichert wird. Die Meldung steht dadurch gleich in der Leiste richtig da — nicht erst,
nachdem jemand die App geöffnet hat. Acht Sendungen je Mitteilung, das ist der ganze Aufwand.

Das alte Thema `announcements` bekommt weiterhin die Mitteilungen **der Kasseler Gemeinde**, damit
die veröffentlichte Android-Version 1.1.3 nicht stumm bleibt. Die Zeile in `functions/index.js`
darf weg, sobald niemand mehr auf 1.1.3 ist.

---

## Bereitstellen

Braucht den **Blaze-Tarif** (pay-as-you-go). Bei diesem Aufkommen ist er praktisch kostenlos — das
monatliche Freikontingent liegt weit über ein paar Mitteilungen —, aber er verlangt ein
Zahlungsmittel im Konto.

1. [Firebase-Konsole](https://console.firebase.google.com) → Projekt `kassel-vaktija` →
   **Upgrade** auf **Blaze** (Budgetwarnung setzen, z. B. 5 €)
2. Auf dem Mac: `npm install -g firebase-tools`, dann `firebase login`
3. Im Projektordner: `firebase use kassel-vaktija`, dann `cd functions && npm install`
4. Bereitstellen:

```bash
firebase deploy --only functions
```

5. Prüfen: eine Mitteilung in der App veröffentlichen → jedes Gerät dieser Gemeinde bekommt sofort
   eine Meldung, in seiner eigenen Sprache.

### Beim ersten Bereitstellen nach dem Umbau

Firebase fragt, ob die alten Funktionen **`onConfigUpdated`** gelöscht werden sollen. **Ja.** Sie
horcht auf einen Pfad, den es nicht mehr gibt. Neu dazu kommen `onBroadcastCreated` und
`onRulesUpdated`.

---

## Wenn nichts ankommt

| Beobachtung | Ursache |
|---|---|
| Gar keine Meldung, auf keinem Gerät | Nicht bereitgestellt, oder Blaze nicht aktiv |
| Android bekommt, iPhone nicht | Der APNs-Schlüssel (`.p8`) fehlt im Firebase-Projekt |
| Meldung kommt, aber mit dem Standardton | `tone_soft.wav` liegt nicht im App-Paket — der Name muss genau stimmen |
| Meldung in der falschen Sprache | Das Gerät hängt noch am alten Thema; ein Sprachwechsel in der App meldet es um |

Die Protokolle stehen in der Firebase-Konsole unter **Functions → Logs**. Eine fehlgeschlagene
Sendung wird dort mit dem Thema genannt, das sie nicht erreicht hat.
