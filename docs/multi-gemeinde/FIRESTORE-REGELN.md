# Sicherheitsregeln für mehrere Gemeinden

Diese Regeln laufen auf Googles Servern, nicht in der App. Was hier steht, lässt sich **nicht
umgehen** — auch nicht von jemandem, der die App auseinandernimmt. Was die App an Knöpfen zeigt,
ist nur Bequemlichkeit; die Absicherung ist hier.

Firebase-Konsole → **Firestore Database** → **Regeln** → einfügen → **Veröffentlichen**

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {

    // Rolle des angemeldeten Kontos nachschlagen
    function adminDoc() {
      return get(/databases/$(database)/documents/admins/$(request.auth.uid)).data;
    }
    function signedIn() {
      return request.auth != null;
    }
    function isHead() {
      return signedIn()
             && exists(/databases/$(database)/documents/admins/$(request.auth.uid))
             && adminDoc().role == 'head';
    }
    function administers(communityId) {
      return isHead()
             || (signedIn()
                 && exists(/databases/$(database)/documents/admins/$(request.auth.uid))
                 && adminDoc().role == 'community'
                 && adminDoc().communityId == communityId);
    }

    // Eine Änderung, die AUSSCHLIESSLICH die beiden Reaktionszähler anfasst.
    // Damit darf jeder Leser das Herz drücken, ohne den Beitrag selbst ändern zu können:
    // Text, Bild und Empfänger bleiben für alle ausser dem Admin unantastbar.
    function onlyReactionCounts() {
      return signedIn()
             && request.resource.data.diff(resource.data).affectedKeys()
                  .hasOnly(['likeCount', 'dislikeCount']);
    }

    // Gemeindeverzeichnis: jeder darf lesen (auch wer noch keine Gemeinde gewählt hat).
    // Anlegen, ändern und abschalten darf NUR der Hauptadministrator.
    match /communities/{communityId} {
      allow read: if true;
      allow write: if isHead();
    }

    // Nachrichten einer Gemeinde: lesen alle, schreiben nur der Admin GENAU DIESER Gemeinde
    // (oder der Hauptadministrator). Ausgenommen die Reaktionszähler — siehe oben.
    match /communities/{communityId}/news/{newsId} {
      allow read: if true;
      allow create, delete: if administers(communityId);
      allow update: if administers(communityId) || onlyReactionCounts();
    }

    // Wer wie reagiert hat. Jeder darf nur SEINEN EIGENEN Eintrag setzen und zurücknehmen —
    // die Dokument-ID ist die Kennung des Geräts, und die muss mit dem Absender übereinstimmen.
    match /communities/{communityId}/news/{newsId}/reactions/{uid} {
      allow read: if true;
      allow write: if signedIn() && request.auth.uid == uid;
    }

    match /communities/{communityId}/news_images/{imageId} {
      allow read: if true;
      allow write: if administers(communityId);
    }

    // Iqamah und Jumu'ah der Gemeinde.
    match /communities/{communityId}/config/{docId} {
      allow read: if true;
      allow write: if administers(communityId);
    }

    // Rollenverzeichnis: jeder Angemeldete darf SEINEN EIGENEN Eintrag lesen (die App braucht das,
    // um zu wissen, was sie anzeigen darf). Vergeben und entziehen darf nur der Hauptadministrator.
    match /admins/{uid} {
      allow read: if signedIn() && request.auth.uid == uid;
      allow write: if isHead();
    }

    // Gemeldete Anmeldeversuche bei einer fremden Gemeinde.
    // Schreiben darf jeder angemeldete Admin — das Gerät, das den Versuch macht, ist das einzige,
    // das beide Seiten kennt. Lesen und löschen darf nur der Hauptadministrator.
    match /admin_alerts/{alertId} {
      allow create: if signedIn();
      allow read, delete: if isHead();
      allow update: if false;
    }

    // Verbandsweite Mitteilungen des Hauptadministrators an alle Nutzer.
    match /broadcasts/{docId} {
      allow read: if true;
      allow create, delete: if isHead();
      allow update: if isHead() || onlyReactionCounts();
    }
    match /broadcasts/{docId}/reactions/{uid} {
      allow read: if true;
      allow write: if signedIn() && request.auth.uid == uid;
    }
    match /broadcast_images/{imageId} {
      allow read: if true;
      allow write: if isHead();
    }

    // Veranstaltungen. Wird von der iOS-App geschrieben; die Android-App liest sie noch nicht.
    // Schreibrecht wie bisher: nur der Hauptadministrator.
    match /events/{eventId} {
      allow read: if true;
      allow write: if isHead();
    }

    // Alter Aufbau aus der Ein-Gemeinde-Zeit. Bleibt lesbar, damit Installationen, die noch nicht
    // aktualisiert haben, weiterlaufen. Erst löschen, wenn niemand mehr die alte Version benutzt.
    match /config/{docId} {
      allow read: if true;
      allow write: if isHead();
    }
    match /news/{newsId} {
      allow read: if true;
      allow write: if isHead();
    }
  }
}
```

---

## Was diese Regeln gegenüber der alten ändern

Die bisherige Regel liess **nur eine einzige Kennung** schreiben — die des Besitzers. Das war
sicher, aber zu eng:

- Der Vorstandszugang der Gemeinde Kassel konnte **nichts** bearbeiten, obwohl er die Rolle hat.
- Jeder künftige Gemeindeadmin wäre ebenso ausgesperrt gewesen.
- **Herz und Daumen funktionierten nicht.** Jeder Tipp wurde stillschweigend abgelehnt, ohne dass
  in der App etwas darauf hingedeutet hätte.

Gleichzeitig bleibt alles gesperrt, was gesperrt sein muss: kein Nicht-Admin kann eine Gemeinde
anlegen, eine Gebetszeit ändern oder einen Beitrag schreiben.

⚠️ **Lesen bleibt für alle offen.** Das ist Absicht — Gebetszeiten sollen alle sehen können, auch
wer die App gar nicht benutzt. Es heisst aber auch: Beiträge einer Gemeinde sind für jeden abrufbar,
der die Adresse der Datenbank kennt. Sag keiner Gemeinde zu, ihre Beiträge seien vertraulich.

---

## Zugänge anlegen

Ein Konto besteht aus **zwei Teilen**: dem Login und dem Rolleneintrag. Ohne den zweiten kommt
niemand rein — auch nicht mit gültigem Passwort.

### 1. Login erzeugen

**Authentication → Users → Add user** → E-Mail und Passwort der Gemeinde eintragen → **UID kopieren**

### 2. Rolle eintragen

**Firestore → Sammlung `admins`** → neues Dokument, **Dokument-ID = die kopierte UID**

**Für eine Gemeinde:**

| Feld | Typ | Wert |
|---|---|---|
| `role` | string | `community` |
| `communityId` | string | die Kennung aus dem Verzeichnis, z. B. `igbd-gemeinde-sandzak-kassel` |

**Für den Hauptadministrator:**

| Feld | Typ | Wert |
|---|---|---|
| `role` | string | `head` |

*(kein `communityId` — der Hauptadministrator gilt für alle)*

---

## Stand der Konten

| Konto | Rolle | Wer hat es |
|---|---|---|
| `1a7xqRgIYDR0RZqa3KghBlz98PK2` | `head` | der Besitzer |
| `Vpalzb0gitTTLUQDFYPLcbIbFXG3` | `community` → `igbd-gemeinde-sandzak-kassel` | der Vorstand Kassel |

Beide sind angelegt und geprüft. Für jede weitere Gemeinde kommt ein Konto nach demselben Muster
dazu — aber erst, wenn die Gemeinde zugesagt hat.

---

## Zugang entziehen

Wechselt ein Gemeindevorstand, **das Dokument in `admins` löschen**. Das Login bleibt bestehen,
verliert aber jedes Recht — die App meldet es beim nächsten Versuch ab. Das Passwort muss dafür
nicht geändert werden.
