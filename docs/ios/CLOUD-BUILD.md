# Cloud-Build — iOS ohne eigenen Mac bauen

## Warum das nötig ist

Apple akzeptiert nur noch Uploads, die mit dem **iOS-26-SDK** gebaut wurden. Das
steckt in Xcode 26, das macOS 26 braucht. Der Mac der Gemeinde ist ein
**MacBook Air 2020 mit Intel-Prozessor** — Apple bietet dafür kein macOS 26 mehr an
(geprüft mit `softwareupdate --list-full-installers`: neuestes Angebot ist Sequoia 15.7.9).

Auf diesem Gerät lässt sich also **keine hochladbare Version mehr bauen**. Der Workflow
`.github/workflows/ios-release.yml` baut stattdessen auf einem aktuellen Mac in der
GitHub-Cloud und lädt direkt zu TestFlight hoch.

## Einmalig einrichten: fünf Secrets

GitHub → **Settings** → **Secrets and variables** → **Actions** → **New repository secret**

| Name | Wert |
|---|---|
| `ASC_KEY_ID` | `R59B9UJ3RM` |
| `ASC_ISSUER_ID` | `f36921e5-2582-4c97-bb4c-f05c396b17e4` |
| `ASC_KEY_P8` | Base64 der Datei `AuthKey_R59B9UJ3RM.p8` |
| `GOOGLE_SERVICE_INFO_PLIST` | Base64 von `iosApp/iosApp/GoogleService-Info.plist` |
| `SECRETS_PLIST` | Base64 von `iosApp/iosApp/Secrets.plist` |

Die drei Base64-Werte legt jeweils dieser Befehl in die Zwischenablage — so steht das
Geheimnis nirgends im Klartext auf dem Bildschirm:

```bash
base64 -i ~/Downloads/AuthKey_R59B9UJ3RM.p8 | pbcopy
base64 -i iosApp/iosApp/GoogleService-Info.plist | pbcopy
base64 -i iosApp/iosApp/Secrets.plist | pbcopy
```

Nach jedem Befehl bei GitHub das Feld anlegen und **Cmd+V** drücken.

> Die Secrets trägt **der Vorstand selbst** ein. Zugangsdaten gehören nicht durch fremde
> Hände, auch nicht durch die eines Assistenten.

## Bauen

GitHub → **Actions** → **iOS Release** → **Run workflow** → Build-Nummer eintragen → starten.

Die Build-Nummer muss bei **jedem** Upload höher sein als beim letzten. Version 1.0 (1)
wurde nie erfolgreich hochgeladen, der erste Cloud-Build kann also `2` benutzen.

Der Lauf dauert etwa 30 Minuten. Danach erscheint der Build in App Store Connect unter
**TestFlight**, zuerst als „Wird verarbeitet".

## Kosten

| Repository | Mac-Minuten |
|---|---|
| **privat** | 2000 Freiminuten/Monat, Mac zählt **zehnfach** → ~200 echte Minuten → ~5 Builds |
| **öffentlich** | unbegrenzt kostenlos |

Öffentlich zu schalten spart Geld, macht aber den **gesamten Quellcode und die komplette
Historie für jeden lesbar**. Die Secrets bleiben geheim — der Code nicht. Und: Wer das
Repository währenddessen kopiert oder forkt, behält seine Kopie auch dann, wenn es später
wieder auf privat gestellt wird. **Einmal öffentlich ist nicht rückgängig zu machen.**

Immerhin ist die Voraussetzung erfüllt: In der Historie wurde **nie** eine Geheimdatei
eingecheckt (`google-services.json`, `GoogleService-Info.plist`, `Secrets.plist`,
Keystores, `.p8` — alle von Anfang an gitignored).

## Beim ersten Lauf wahrscheinliche Fehler

Der Workflow konnte lokal nicht getestet werden — dafür fehlt genau der Mac, um den es geht.
Zwei Stellen sind erfahrungsgemäß heikel:

1. **Android-SDK.** Der Gradle-Aufruf für das Kotlin-Framework konfiguriert womöglich das
   ganze Projekt inklusive `:app`. Ob der Cloud-Mac ein Android-SDK hat, ist offen.
2. **Automatisches Signieren.** Xcode muss dort Zertifikat und Profil frisch anlegen. Mit
   dem API-Schlüssel sollte das gehen, scheitert aber oft im ersten Anlauf.

Bei einem Fehlschlag: in **Actions** den Lauf öffnen, den roten Schritt aufklappen und die
Meldung weitergeben. Die Protokolle werden zusätzlich als Artefakt `build-logs` gesichert.

## Danach noch offen

- **Händlerstatus** (App Store Connect → *Geschäftliches*) — EU-Pflicht, Prüfung dauert
  Tage und **blockiert die Einreichung**
- **Altersfreigabe**, **Datenschutz-URL** (`https://ivox-08.github.io/Kassel-Datenschutz/`)
- **Anmerkungen für die App-Prüfung**: Der Admin-Bereich liegt hinter 7 Tipps auf
  „Über uns". Findet der Prüfer ihn nicht, gilt die App als unvollständig
- **Auf echtem Gerät gegenhören**, ob der 19-Sekunden-Adhan in der Benachrichtigung klingt
- **`onConfigUpdated` deployen** (Server-Fix liegt bereit, betrifft die Android-App)
