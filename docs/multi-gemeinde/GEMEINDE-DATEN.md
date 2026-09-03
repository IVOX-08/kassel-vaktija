# Daten einer Gemeinde pflegen — ohne App-Update

Ab jetzt braucht keine Gemeinde mehr eine Codeänderung. Spendenlink, E-Mail, Telefon, Webseite und
der Imam stehen in der Datenbank; die App liest sie und zeigt sie.

**Wo:** [Firebase-Konsole](https://console.firebase.google.com) → Firestore Database →
Sammlung `communities` → das Dokument der Gemeinde.

Eine Änderung dort ist **sofort** in allen Telefonen dieser Gemeinde sichtbar. Kein neuer Build,
keine Prüfung durch Apple, kein Warten.

---

## Die Felder

| Feld | Beispiel | Wo es erscheint |
|---|---|---|
| `name` | `IGBD-Gemeinde Sandžak-Kassel` | Kopfzeile, Kontaktkarte, Widget |
| `address` | `Schwanenweg 13, 34123 Kassel` | Kopfzeile, Kontaktkarte, Kartenlink |
| `email` | `vorstand@…de` | Kontaktkarte — tippen öffnet die Mail-App |
| `phone` | `0561 1234567` | Kontaktkarte — tippen wählt |
| `website` | `https://www.…de` | Kontaktkarte — tippen öffnet den Browser |
| `donationUrl` | `https://www.paypal.com/donate?business=…` | **Spendenherz** auf der Startseite und in der Kontaktkarte |
| `imamName` | `Alen Golac` | Kontaktkarte |
| `imamPhone` | `0176 3037 2402` | Kontaktkarte — tippen wählt |
| `facebookUrl` / `instagramUrl` / `youtubeUrl` | | Die drei Symbole über den Mitteilungen |
| `logoUrl` | | Wappen in der Kopfzeile statt des Verbandszeichens |

Alle Felder sind vom Typ **string**.

**Ein Feld, das fehlt oder leer ist, wird nicht angezeigt.** Die Zeile bleibt einfach weg. Es gibt
also keine halbleeren Karten und keine Knöpfe, die ins Nichts führen.

---

## So trägst du etwas ein

1. Firestore Database → Sammlung **`communities`**
2. Links das Dokument der Gemeinde anklicken (die ID, z. B. `dzemat-sejhul-ekber-rosenheim`)
3. **Add field**
4. Field: `donationUrl` · Type: **string** · Value: die Adresse
5. **Add**

Fertig. Die Telefone der Gemeinde haben es innerhalb weniger Sekunden.

### Beim Spendenlink aufpassen

Er muss vollständig sein und mit `https://` anfangen:

```
https://www.paypal.com/donate?business=kasse@gemeinde.de
```

Nicht `paypal.me/…` ohne `https://` und nicht nur die E-Mail-Adresse — daraus kann die App keinen
Link machen, und das Herz führt dann nirgendwohin.

### Bei der Telefonnummer

Schreib sie so, wie ein Mensch sie liest: `0176 3037 2402`. Die App entfernt Leerzeichen und
Striche selbst, bevor sie wählt.

---

## Wenn eine neue Gemeinde dazukommt

1. Eintrag in `iosApp/Resources/communities/communities.json` **und** in der Android-Vorlage
   ergänzen — das ist die einzige Stelle, die noch einen Build braucht, weil dort die
   **Koordinaten** und das **vaktija.eu-Kürzel** stehen
2. In der App als Hauptadministrator: Einstellungen → Verwaltung → **Gemeinden importieren**
3. In Firestore Kontakt, Spendenlink und Imam nachtragen (siehe oben)
4. Status auf `active` setzen, damit sie in der Auswahl erscheint

Die Koordinaten sind der Grund für Schritt 1: Nach ihnen rechnet die App die Zeiten, wenn kein Netz
da ist, und den Monatskalender. Ohne sie stünde eine neue Gemeinde wieder auf Kassels Sonnenstand.

---

## Was mit einem erneuten Import passiert

Der Import schreibt **zusammenführend** (`merge`). Was du in Firestore eingetragen hast, bleibt
stehen — der Import schickt nur, was im App-Paket steht, und leere Felder schickt er gar nicht.

Zwei Ausnahmen, absichtlich:

- Der **Status** wird nur bei einer Gemeinde geschrieben, die die Datenbank noch nie gesehen hat.
  Sonst würde ein Routine-Import eine gesperrte Gemeinde wieder freischalten.
- **Ikamet und Džuma** werden nur gesetzt, wenn die Gemeinde noch keine eigenen hat.

---

## Stand heute

| | gefüllt |
|---|---|
| `address` | 81 von 81 |
| `phone` | 49 von 81 |
| `email` | 45 von 81 |
| `website` | 31 von 81 |
| `donationUrl` | **1 von 81** |
| `imamName` / `imamPhone` | **1 von 81** |

Die letzten beiden Zeilen sind die Arbeit, die vor dem Verband liegt: Jede Gemeinde muss ihren
Spendenlink und die Nummer ihres Imams schicken. Bis dahin bleiben die Zeilen in der App einfach
leer — falsch wäre schlimmer als leer.
