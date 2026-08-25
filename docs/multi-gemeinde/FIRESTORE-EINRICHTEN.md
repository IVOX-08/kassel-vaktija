# Firestore für mehrere Gemeinden einrichten

Ich kann nicht in deine Datenbank schreiben — dafür bräuchte ich deine Zugangsdaten, und die gebe
ich nicht ein. Hier steht alles zum Selbermachen. Es dauert etwa zehn Minuten.

**Wichtig:** Solange die Sammlung `communities` leer ist, benutzt die App die eingebaute Liste
(Kassel + Rosenheim). Sobald du das erste Dokument anlegst, **gilt nur noch Firestore**.

---

## 1. Sammlung anlegen

Firebase-Konsole → **Firestore Database** → **Sammlung starten**

Sammlungs-ID: `communities`

## 2. Erstes Dokument: Kassel

Dokument-ID: **`sandzak-kassel`** (genau so — die App migriert bestehende Nutzer auf diese Kennung)

| Feld | Typ | Wert |
|---|---|---|
| `name` | string | `IGBD-Gemeinde Sandžak-Kassel` |
| `address` | string | `Schwanenweg 13, 34123 Kassel` |
| `donationUrl` | string | `https://www.paypal.com/donate?business=ikzsandzakkassel@gmail.com` |
| `status` | string | `active` |
| `locations` | array | drei Objekte, siehe unten |

**`locations`** — Typ *array*, darin drei *map*-Einträge:

```
[0]  id: "kassel"          name: "Kassel"
     vaktijaSlug: "kassel"        latitude: 51.3093   longitude: 9.5132
[1]  id: "hann-muenden"    name: "Hann. Münden"
     vaktijaSlug: "hann-munden"   latitude: 51.4194   longitude: 9.6524
[2]  id: "korbach"         name: "Korbach"
     vaktijaSlug: "brilon"        latitude: 51.2761   longitude: 8.8735
```

> `latitude` und `longitude` als **number** anlegen, nicht als string.
> Bei Korbach steht bewusst `brilon`: vaktija.eu veröffentlicht Korbach nicht, Brilon liegt 19 km
> entfernt — unter einer Minute Unterschied.

## 3. Zweites Dokument: Rosenheim

Dokument-ID: **`rosenheim`**

| Feld | Typ | Wert |
|---|---|---|
| `name` | string | `IGBD-Gemeinde Rosenheim` |
| `address` | string | `Burgfriedstraße 55, 83024 Rosenheim` |
| `logoUrl` | string | `https://igbd-rosenheim.de/wp-content/uploads/2024/01/cropped-Logo-IZ-59x59.png` |
| `status` | string | `active` |
| `locations` | array | ein Eintrag: `id: "rosenheim"`, `name: "Rosenheim"`, `vaktijaSlug: "rosenheim"`, `latitude: 47.8664`, `longitude: 12.1131` |

Rosenheim hat keinen Spendenlink veröffentlicht — `donationUrl` bleibt weg, dann blendet die App
den Spendenknopf aus.

---

## 4. Sicherheitsregeln erweitern

Firestore → **Regeln**. Der Katalog muss **für alle lesbar** sein (auch für Leute, die noch keine
Gemeinde gewählt haben), aber nur von dir beschreibbar:

```
match /communities/{communityId} {
  allow read: if true;
  allow write: if request.auth != null
               && request.auth.uid == "DEINE-HAUPTADMIN-UID";
}
```

Die bestehenden Regeln für `config` und `news` bleiben, bis die Adminrechte je Gemeinde umgebaut
sind — daran arbeite ich als Nächstes.

---

## 5. Prüfen

App neu starten → Auswahl öffnen. Es müssen **vier Standorte** erscheinen. Rosenheim auswählen:
Adresse, Logo und Zeiten müssen wechseln (Rosenheim liegt gut zehn Minuten vor Kassel).

---

## Statuswerte zum Abschalten

| Wert | Wirkung |
|---|---|
| `active` | normal |
| `suspended` | aus der Auswahl, kein Logo, keine Spenden, keine Nachrichten — **Gebetszeiten laufen weiter** |
| `blocked` | App zeigt nur noch einen Hinweis mit Weg zu einer anderen Gemeinde |

Fehlt das Feld, gilt die Gemeinde als **aktiv**. Zurücknehmen ist derselbe Handgriff.

---

## Was von jeder Gemeinde gebraucht wird

Für das Anschreiben, das der Vorstand verschickt:

1. **Offizieller Name** der Gemeinde
2. **Anschrift der Moschee** (Straße, Hausnummer, PLZ, Ort)
3. **Städte**, für die Gebetszeiten gelten sollen — falls mehrere Orte zur Gemeinde gehören
4. **Logo** als quadratisches Bild, **mindestens 512 × 512 Pixel**
   *(Rosenheims Logo auf der Webseite ist nur 59 × 59 und wird unscharf — bitte ausdrücklich eine
   ordentliche Größe erbitten.)*
5. **Spendenlink** (PayPal oder Ähnliches), falls vorhanden
6. **Iqamah-Zeiten** und **Džuma-Zeit**
7. **Ansprechpartner mit E-Mail** — daraus wird der Adminzugang
