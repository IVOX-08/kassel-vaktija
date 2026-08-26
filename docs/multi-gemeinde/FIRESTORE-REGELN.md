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

    // Gemeindeverzeichnis: jeder darf lesen (auch wer noch keine Gemeinde gewählt hat).
    // Anlegen, ändern und abschalten darf NUR der Hauptadministrator.
    match /communities/{communityId} {
      allow read: if true;
      allow write: if isHead();
    }

    // Nachrichten, Bilder und Einstellungen einer Gemeinde:
    // lesen alle, schreiben nur der Admin GENAU DIESER Gemeinde (oder der Hauptadministrator).
    match /communities/{communityId}/news/{newsId} {
      allow read: if true;
      allow write: if administers(communityId);
    }
    match /communities/{communityId}/news_images/{imageId} {
      allow read: if true;
      allow write: if administers(communityId);
    }
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
      allow write: if isHead();
    }
    match /broadcast_images/{imageId} {
      allow read: if true;
      allow write: if isHead();
    }

    // Alter Aufbau aus der Ein-Gemeinde-Zeit. Bleibt lesbar, damit Installationen, die noch nicht
    // aktualisiert haben, weiterlaufen. Nach dem Umzug der Daten löschen.
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
| `communityId` | string | `sandzak-kassel` |

**Für dich als Hauptadministrator:**

| Feld | Typ | Wert |
|---|---|---|
| `role` | string | `head` |

*(kein `communityId` — der Hauptadministrator gilt für alle)*

---

## Deine Aufteilung

| Konto | Rolle | Wer hat es |
|---|---|---|
| bisheriger Zugang `1a7xqRgIYDR0RZqa3KghBlz98PK2` | `community` → `sandzak-kassel` | der Vorstand |
| neues Konto, das du anlegst | `head` | du |

⚠️ **Reihenfolge beachten:** Lege **zuerst** deinen Hauptadmin-Eintrag an, **danach** veröffentliche
die Regeln. Sonst darf niemand mehr in `admins` schreiben — auch du nicht — und du müsstest die
Regeln vorübergehend wieder lockern, um wieder hineinzukommen.

---

## Zugang entziehen

Wechselt ein Gemeindevorstand, **das Dokument in `admins` löschen**. Das Login bleibt bestehen,
verliert aber jedes Recht — die App meldet es beim nächsten Versuch ab. Das Passwort muss dafür
nicht geändert werden.
