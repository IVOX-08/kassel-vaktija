# Für die Android-Seite — Stand 04.09.2026

Aus der Code-Prüfung der iOS-App (`docs/ios/CODE-PRUEFUNG.md`, 39 Befunde) betrifft ein Teil
**beide** Apps oder den gemeinsamen Code. Das hier ist die Liste zum Nachziehen, wichtigstes
zuerst.

---

## 1. DRINGEND: Push-Themen, bevor die Functions bereitgestellt werden

`functions/index.js` ist neu geschrieben. Die alten Auslöser konnten seit dem Umbau auf mehrere
Gemeinden **nicht mehr feuern** — sie horchten auf `news/{id}` und `config/community`, geschrieben
wird nach `communities/{id}/news`, `broadcasts` und `communities/{id}/config/rules`. Seitdem ging
zu keiner Mitteilung eine Push-Meldung raus, auf beiden Plattformen.

Gleichzeitig hängt jedes Gerät bisher am einen Thema `announcements`. Das ist jetzt anders:

```
c_<gemeinde>            Datenmeldung: die Zeiten dieser Gemeinde haben sich geändert
c_<gemeinde>_<sprache>  Mitteilungen dieser Gemeinde, im richtigen Wortlaut
b_<sprache>             verbandsweite Mitteilungen
```

**Was Android tun muss:** sich an dieselben drei Themen anmelden und beim Wechsel von Gemeinde
oder Sprache umhängen. Die iOS-Seite macht das in `PushService.swift` → `PushTopics`; sie merkt
sich, woran sie hängt, weil eine Anmeldung bei Firebase sonst für immer bestehen bleibt.

> **Reihenfolge:** Erst Android umstellen, dann bereitstellen. Nach dem Bereitstellen bekommen
> Android-Geräte, die nur an `announcements` hängen, **nur noch die Mitteilungen der Kasseler
> Gemeinde** — die geht absichtlich weiter an das alte Thema, damit die veröffentlichte Version
> 1.1.3 nicht stumm bleibt. Alle anderen Gemeinden bekämen dort nichts.

---

## 2. Die Koordinaten der Gemeinde in die Rechnung geben

`PrayerTimesCalculator` stand fest auf Kassel. Die vier gemeinsamen Funktionen nehmen jetzt
`latitude`/`longitude`:

```kotlin
dashboardRowsForToday(latitude, longitude)
monthForDisplay(year, month, latitude, longitude)
nextPrayerNow(latitude, longitude)
prayerRowsForToday(latitude, longitude)
```

**Der Standardwert ist weiterhin Kassel**, damit euer Build ohne Änderung übersetzt — aber wer ihn
stehen lässt, behält den Fehler. Betroffen ist alles außer der tagesaktuellen Abfrage bei
vaktija.eu: die Zeiten ohne Netz, der **Monatskalender**, geplante Meldungen für die kommenden
Tage, die Fenster des Trackers.

Drei Tests in `CoordinatesReachTheCalculationTest` belegen, dass die Koordinaten ankommen: Hamburg
und München dürfen nicht dieselbe Fajr liefern.

---

## 3. Zwei Fehler im gemeinsamen Kotlin bzw. in eurer Fassung

### Tedschwid: verschachtelte Marken
`Tajweed.kt` nimmt die **erste** schließende Klammer als Ende einer Marke. In 34 Ajetes stecken
Marken ineinander (`[o[ُوٓ[s[اْ]‌ۚ]`, z. B. 2:190) — dort wird die innere Marke als Text
eingefärbt. Die iOS-Fassung (`Tajweed.swift`) sucht rekursiv die **passende** Klammer.

Zweiter Punkt derselben Datei: Auf Swift war `[` gefolgt von einem arabischen Kombinationszeichen
**ein** Zeichen, weshalb die Marke gar nicht gefunden wurde. Ob Kotlin das auch trifft, hängt
davon ab, ob ihr über `Char` oder über Codepunkte lauft — bitte einmal nachsehen.

### Zakat: Beträge mit Nachkommastelle
`toAmount()` entfernt erst alle Punkte und liest `"1234.50"` deshalb als **123450** — hundertfach
zu viel, und damit eine hundertfach zu hohe Zakat, ohne dass etwas kaputt aussieht. Der Weg auf
iOS steht in `ZakatView.swift:175–190`: am **letzten** Trennzeichen teilen, davor die Tausender
wegwerfen, dahinter die Nachkommastellen.

---

## 4. Kleinigkeiten zum Abgleich

- **Der Übersetzer-Auftrag** nannte fest „IGBD-Gemeinde Sandžak-Kassel in Kassel, Germany". Ein
  Vorstand in Stuttgart schrieb, und das Modell übersetzte im Glauben, es gehe um Kassel. Auf iOS
  steht dort jetzt der Name der gewählten Gemeinde. Der Kommentar im Code verlangt, dass beide
  Fassungen gleich bleiben — deshalb hier gemeldet.
- **Ikamet und die alte Fassung:** Seit dem Umbau schreibt niemand mehr nach `config/community`,
  das Android 1.1.3 liest. Deren Nutzer sahen eingefrorene Zeiten. iOS schreibt es für die
  Heimatgemeinde wieder mit (`AdminStore.saveRule`). Prüft, ob eure Seite dasselbe tut.
- **Verbandsweite Mitteilungen** waren auf iOS nicht löschbar (der Löschpfad kannte nur die
  Gemeinde). Falls das bei euch genauso ist: derselbe Fehler.
- **Neue Felder im Verzeichnis:** `phone`, `email`, `website`, `imamName`, `imamPhone`. Sie lagen
  teilweise schon in `communities.json` und wurden nirgends gelesen. Siehe
  [GEMEINDE-DATEN.md](GEMEINDE-DATEN.md).
- **Zwei Kürzel je Stadt** (`munchen`/`muenchen`, `frankfurt`/`frankfurt-am-main`) sind auf je
  eines vereinheitlicht.

---

## 5. Offen, für beide Seiten gemeinsam

**Der Gemini-Schlüssel liegt in beiden App-Paketen** und lässt sich auslesen. Die Lösung ist eine
Cloud Function, die ihn hält; die Apps schicken nur noch den Text hin. Das geht nur zusammen: Der
alte Schlüssel muss ersetzt werden, und solange eine der beiden Apps ihn weiter mitliefert, ändert
sich nichts an der Gefahr.

Vorschlag: nach der Freigabe im App Store — Cloud Function schreiben, beide Apps umstellen, dann
den alten Schlüssel sperren.
