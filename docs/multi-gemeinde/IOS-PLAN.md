# iOS auf den Android-Stand bringen — Plan

Grundlage: die Bildschirmfotos der Android-App (hell und dunkel) vom 26.08.2026 und die
45 Commits im Zweig `android`. Ziel ist Gleichstand, nicht Ähnlichkeit.

---

## Der eigentliche Befund

Kassel ist in der iOS-App nicht *eingestellt*, sondern *eingebacken*. Sieben Stellen:

| Stelle | fest verdrahtet |
|---|---|
| `PrayerData.swift:63` | `https://vaktija.eu/kassel` — **die Zeitquelle** |
| `ContentView.swift:48` | „Schwanenweg 13, 34123 Kassel" |
| `ContentView.swift:16` | Karten-Link |
| `SettingsView.swift:254` | Karten-Link (zweite Kopie) |
| `ContentView.swift:67` | Logo als festes Bild im App-Paket |
| `Community.swift` | die Kennung |
| `shared/Qibla.kt` | Kassels Koordinaten (seit 26.08. überschreibbar) |

**Die Zeitquelle ist die gefährliche.** Ein Auswahlbildschirm ohne sie ergäbe: richtiger
Gemeindename, Kassels Gebetszeiten. Ein stiller Fehler — nichts sieht kaputt aus, niemand
meldet ihn, und gebetet wird trotzdem falsch. Deshalb wird die Auswahl **zusammen mit** der
Zeitquelle umgestellt oder gar nicht.

---

## Reihenfolge

### 1. Fundament — die Gemeinde wird eine Auswahl statt einer Konstante
- `communities.json` aus `docs/multi-gemeinde/` als Ressource ins App-Paket, damit die Auswahl
  beim ersten Start ohne Netz funktioniert (wie Androids `CommunityCatalog`)
- `CommunitySelectionStore`: gewählte Gemeinde + Standort, in UserDefaults
- **Alle sieben Stellen oben** ziehen aus dieser Auswahl statt aus Konstanten
- Auswahlbildschirm: Suche nach Stadt oder Gemeinde, zweistufig wie Android
  (erst Gemeinde, dann Standort, falls mehrere)
- Beim ersten Start Teil des Onboardings, danach in den Einstellungen unter „Gemeinde"

**Ohne diesen Schritt sind 2–4 nicht sinnvoll baubar.**

### 2. Kopf und Mitteilungen an Android angleichen
- Startseite: Ort und **Logo der gewählten Gemeinde** statt Kassels festem Bild
- Nachrichtenkarte: **Absenderzeile mit Logo** oben („Gesendet von IGBD" bzw. vom Gemeindenamen)
  — ersetzt das kleine Abzeichen unten rechts, das ich zuerst gebaut hatte
- Admin-Abschnitt: Überschrift benennt die Gemeinde
  („Administrator: …" / „Hauptadministrator (alle Gemeinden)")

### 3. Hauptadministrator
- „Gemeinden verwalten": Liste mit Suche und Statusabzeichen
- Statuswechsel je Gemeinde: **Aktiv / Eingeschränkt / Gesperrt**, mit denselben
  Erklärungstexten wie Android
- Gesperrte Gemeinde: die App zeigt nur noch einen Hinweis
- Eingeschränkt: nicht in der Auswahl, kein Logo, keine Spenden, keine Mitteilungen —
  **Gebetszeiten laufen weiter**

### 4. Rundnachricht gezielt
- „Wer bekommt die Mitteilung?" — Mehrfachauswahl mit Suche, „Alle" und „Alle Gemeinden"
- Nachtrag zu meinem ersten Wurf: den Empfängerkreis hatte ich vergessen

### 5. Kleinere Lücken
- **Zakat-Rechner** unter „Mehr"
- **Sonnenaufgang-Erinnerung** in den Benachrichtigungen, mit dem Hinweis, dass dann die Zeit
  für das Morgengebet endet
- **Kontaktkarte** in den Einstellungen („App oder Website gewünscht?")
- **Wöchentliche Erinnerung** (Dhikr & Hadith) prüfen und angleichen

---

## Was dabei zu beachten ist

- **`config/community` nicht anfassen** — die Play-Version 1.1.3 liest noch davon
- **Niemals `--force` pushen** — die Historien sind seit `faaa96c` getrennt
- Der Katalog im App-Paket ist eine **Kopie**; die Wahrheit steht in Firestore. Beim Start wird
  aus `communities/` nachgeladen, damit neue Gemeinden ohne App-Update erscheinen
- 84 statt 20 Gemeinden ändern an diesem Plan nichts — nur der Katalog wächst
