import Foundation
import Shared

// Official prayer times for Kassel come from https://vaktija.eu/kassel (the community's source),
// mirroring the Android app. The local adhan2 computation (shared Kotlin) is only an offline
// fallback and — for the month calendar — a base that gets calibrated to today's official value.
// (The raw high-latitude adhan Fajr is wildly off, e.g. 01:25 vs the official 03:20 — so the
// official times matter a lot here.)

// A day's six adhan times as minutes since midnight.
struct DayTimes: Codable, Equatable {
    let date: String // yyyy-MM-dd
    var fajr, sunrise, dhuhr, asr, maghrib, isha: Int

    var asArray: [Int] { [fajr, sunrise, dhuhr, asr, maghrib, isha] }
    static func hhmm(_ minutes: Int) -> String {
        let m = ((minutes % 1440) + 1440) % 1440
        return String(format: "%02d:%02d", m / 60, m % 60)
    }
}

struct PrayerRow: Identifiable {
    let name: String       // canonical key: Fajr/Sunrise/Dhuhr/Asr/Maghrib/Isha
    let adhan: String      // "HH:mm"
    let iqamah: String?    // "HH:mm" or nil (Sunrise)
    var id: String { name }
}

enum PrayerModel {
    // Iqamah times follow the community rule the board edits (Firestore `config/community`):
    // a fixed Fajr Iqamah, per-prayer offsets for the rest, no Iqamah for sunrise. Falls back to
    // the values the app shipped with when the document hasn't loaded yet.
    static func rows(_ t: DayTimes, rule: CommunityRule = CommunityRule.fallback) -> [PrayerRow] {
        [
            PrayerRow(name: "Fajr", adhan: DayTimes.hhmm(t.fajr), iqamah: rule.fajrIqamah),
            PrayerRow(name: "Sunrise", adhan: DayTimes.hhmm(t.sunrise), iqamah: nil),
            PrayerRow(name: "Dhuhr", adhan: DayTimes.hhmm(t.dhuhr), iqamah: DayTimes.hhmm(t.dhuhr + rule.dhuhrOffsetMin)),
            PrayerRow(name: "Asr", adhan: DayTimes.hhmm(t.asr), iqamah: DayTimes.hhmm(t.asr + rule.asrOffsetMin)),
            PrayerRow(name: "Maghrib", adhan: DayTimes.hhmm(t.maghrib), iqamah: DayTimes.hhmm(t.maghrib + rule.maghribOffsetMin)),
            PrayerRow(name: "Isha", adhan: DayTimes.hhmm(t.isha), iqamah: DayTimes.hhmm(t.isha + rule.ishaOffsetMin)),
        ]
    }

    // The next of the five daily prayers (Sunrise excluded), with seconds remaining. After Isha it
    // wraps to tomorrow's Fajr (approximated by today's Fajr + 24h).
    static func next(_ t: DayTimes, now: Date = Date()) -> (name: String, time: String, inSeconds: Int) {
        let cal = Calendar.current
        let comps = cal.dateComponents([.hour, .minute, .second], from: now)
        let nowSec = (comps.hour ?? 0) * 3600 + (comps.minute ?? 0) * 60 + (comps.second ?? 0)
        let daily: [(String, Int)] = [
            ("Fajr", t.fajr), ("Dhuhr", t.dhuhr), ("Asr", t.asr), ("Maghrib", t.maghrib), ("Isha", t.isha),
        ]
        for (name, minutes) in daily where minutes * 60 > nowSec {
            return (name, DayTimes.hhmm(minutes), minutes * 60 - nowSec)
        }
        // After Isha → tomorrow's Fajr.
        return ("Fajr", DayTimes.hhmm(t.fajr), t.fajr * 60 + 86_400 - nowSec)
    }
}

// Fetches + parses the vaktija.eu JSON-LD.
enum VaktijaEuSource {
    /// Der Ort der GEWÄHLTEN Gemeinde, nicht mehr fest Kassel.
    ///
    /// Das ist die heikelste Stelle des Umbaus auf zwanzig Gemeinden: Bliebe hier `kassel`
    /// stehen, während die Auswahl eine andere Gemeinde zeigt, sähe der Nutzer den richtigen
    /// Gemeindenamen über Kassels Gebetszeiten. Nichts wirkt kaputt, niemand meldet es, und
    /// gebetet wird trotzdem zur falschen Zeit.
    /// Kein Ausrufezeichen: Das Kuerzel kommt auch aus Firestore. Traegt dort jemand ein
    /// Leerzeichen ein, gaebe `URL(string:)` nichts zurueck — und ein erzwungenes Auspacken
    /// beendete die App bei JEDEM Start aufs Neue, ohne Weg zurueck.
    static var url: URL? {
        let slug = CommunitySelection.vaktijaSlug
            .trimmingCharacters(in: .whitespacesAndNewlines)
            .addingPercentEncoding(withAllowedCharacters: .urlPathAllowed) ?? ""
        guard !slug.isEmpty else { return nil }
        return URL(string: "https://vaktija.eu/\(slug)")
    }
    static let userAgent =
        "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36 KasselVaktija"

    static func fetchToday() async throws -> DayTimes {
        guard let url else { throw Err.badSlug }
        var req = URLRequest(url: url, timeoutInterval: 15)
        req.setValue(userAgent, forHTTPHeaderField: "User-Agent")
        req.setValue("bs,hr,sr", forHTTPHeaderField: "Accept-Language")
        let (data, _) = try await URLSession.shared.data(for: req)
        guard let html = String(data: data, encoding: .utf8) else { throw Err.empty }
        return try parse(html)
    }

    enum Err: Error { case empty, noJSONLD, structure, badSlug }

    static func parse(_ html: String) throws -> DayTimes {
        // Extract the application/ld+json <script> block.
        guard let range = html.range(of: #"<script[^>]*application/ld\+json[^>]*>"#, options: .regularExpression),
              let end = html.range(of: "</script>", range: range.upperBound..<html.endIndex) else { throw Err.noJSONLD }
        let jsonText = String(html[range.upperBound..<end.lowerBound])
        guard let jd = jsonText.data(using: .utf8),
              let root = try JSONSerialization.jsonObject(with: jd) as? [String: Any],
              let graph = root["@graph"] as? [[String: Any]] else { throw Err.structure }
        guard let dataset = graph.first(where: { ($0["@type"] as? String) == "Dataset" }),
              let schedule = dataset["mainEntity"] as? [String: Any],
              let startDate = schedule["startDate"] as? String,
              let events = schedule["eventSchedule"] as? [[String: Any]] else { throw Err.structure }

        // name (Bosnian, normalised) -> minutes
        var byName: [String: Int] = [:]
        for e in events {
            guard let name = (e["name"] as? String)?.lowercased().trimmingCharacters(in: .whitespaces),
                  let time = e["startTime"] as? String else { continue }
            let parts = time.split(separator: ":")
            if parts.count == 2, let h = Int(parts[0]), let m = Int(parts[1]) { byName[name] = h * 60 + m }
        }
        func time(_ keys: [String]) throws -> Int {
            for k in keys { if let v = byName.first(where: { $0.key.contains(k) })?.value { return v } }
            throw Err.structure
        }
        return DayTimes(
            date: startDate,
            fajr: try time(["sabah", "imsak", "zora", "fajr"]),
            sunrise: try time(["izlazak", "sunrise"]),
            dhuhr: try time(["podne", "dhuhr", "zuhr"]),
            asr: try time(["ikindija", "asr"]),
            maghrib: try time(["akšam", "aksam", "maghrib"]),
            isha: try time(["jacija", "isha", "jacaja"])
        )
    }
}

// Offline-first store: the UI reads `today`; the network only refreshes the cache. Also derives the
// per-prayer calibration offset (official − local) so the month calendar can line up with vaktija.
@MainActor
final class PrayerStore: ObservableObject {
    @Published private(set) var today: DayTimes
    @Published private(set) var official = false
    @Published private(set) var calibration: [Int]

    // `nonisolated`: Beide Schluessel werden auch aus dem Hintergrund gelesen (times(on:)).
    //
    // Die GEMEINDE steht im Schluessel. Vorher war es je ein Schluessel fuer alle, und geprueft
    // wurde nur das Datum: Wer die Gemeinde wechselte, sah bis zum naechsten geglueckten Abruf
    // die offiziellen Zeiten der alten Gemeinde unter dem neuen Namen — ohne Netz den ganzen Tag.
    nonisolated private static var cacheKey: String { "vaktija_today_\(CommunitySelection.communityId)" }
    nonisolated private static var calibKey: String { "vaktija_calibration_\(CommunitySelection.communityId)" }

    private var communityObserver: NSObjectProtocol?

    init() {
        today = PrayerStore.cachedOfficial() ?? PrayerStore.localToday()
        official = PrayerStore.cachedOfficial() != nil
        calibration = PrayerStore.calibration()
        // Nach einem Gemeindewechsel stehen sonst bis zum naechsten Start die Zeiten der alten
        // Stadt auf der Startseite: `refresh()` laeuft nur einmal, wenn die Ansicht erscheint.
        communityObserver = NotificationCenter.default.addObserver(
            forName: .communityDidChange, object: nil, queue: .main
        ) { [weak self] _ in
            Task { @MainActor in await self?.reloadForNewCommunity() }
        }
    }

    deinit {
        if let communityObserver { NotificationCenter.default.removeObserver(communityObserver) }
    }

    /// Sofort auf den Stand der neuen Gemeinde, dann nachladen.
    ///
    /// Der Zwischenspeicher traegt die Gemeinde im Schluessel, also liegt hier entweder ihr
    /// gespeicherter Stand oder — beim ersten Mal — die oertliche Rechnung. Beides ist richtiger
    /// als die Zeiten der Gemeinde, die man gerade verlassen hat.
    private func reloadForNewCommunity() async {
        let cached = PrayerStore.cachedOfficial()
        today = cached ?? PrayerStore.localToday()
        official = cached != nil
        calibration = PrayerStore.calibration()
        await refresh()
    }

    func refresh() async {
        guard let off = try? await VaktijaEuSource.fetchToday() else { return }
        // Key by the device's current date (vaktija's startDate can lag around midnight).
        let dated = DayTimes(date: PrayerStore.isoToday(), fajr: off.fajr, sunrise: off.sunrise,
                             dhuhr: off.dhuhr, asr: off.asr, maghrib: off.maghrib, isha: off.isha)
        if let data = try? JSONEncoder().encode(dated) { AppGroup.defaults.set(data, forKey: PrayerStore.cacheKey) }
        // Calibration = official − local(raw) today, per prayer.
        let raw = PrayerStore.localToday()
        let calib = zip(dated.asArray, raw.asArray).map { $0 - $1 }
        AppGroup.defaults.set(calib, forKey: PrayerStore.calibKey)
        today = dated
        official = true
        calibration = calib
        // Keep the scheduled prayer notifications in step with the official times.
        // Frische Zeiten gehoeren auch ins Widget.
        WidgetRefresh.now()
        await PrayerStore.onTimesLoaded?(dated)
    }

    /// Was nach frisch geladenen Zeiten zu tun ist. Die App haengt hier den
    /// Benachrichtigungs-Planer ein (siehe iOSApp.swift); im Widget bleibt es leer — dort gibt es
    /// nichts zu planen. Ohne diesen Haken zoege der gemeinsame Code den ganzen Planer samt
    /// Firestore ins Widget.
    static var onTimesLoaded: ((DayTimes) async -> Void)?

    nonisolated static func calibration() -> [Int] {
        (AppGroup.defaults.array(forKey: calibKey) as? [Int]) ?? [0, 0, 0, 0, 0, 0]
    }

    // MARK: helpers

    nonisolated static func iso(_ date: Date) -> String {
        let f = DateFormatter(); f.calendar = Calendar(identifier: .gregorian)
        f.locale = Locale(identifier: "en_US_POSIX"); f.dateFormat = "yyyy-MM-dd"
        return f.string(from: date)
    }

    nonisolated private static func isoToday() -> String { iso(Date()) }

    // Nicht mehr privat: Der Gebetstracker braucht dieselben Zeiten fuer seine Fenster. Zwei
    // Quellen fuer dieselben Zahlen waeren die sicherste Art, sie auseinanderlaufen zu lassen.
    //
    // `nonisolated`, weil auch der Benachrichtigungs-Planer sie liest. Der laeuft nicht auf dem
    // Hauptthread, und diese Funktionen ruehren nichts an, was dort liegt: UserDefaults und die
    // Rechnung aus dem geteilten Kotlin-Modul.
    nonisolated static func cachedOfficial() -> DayTimes? {
        guard let data = AppGroup.defaults.data(forKey: cacheKey),
              let t = try? JSONDecoder().decode(DayTimes.self, from: data),
              t.date == isoToday() else { return nil }
        return t
    }

    // Local adhan fallback via the shared Kotlin dashboard rows ("HH:MM" strings → minutes).
    //
    // Mit den Koordinaten der GEWAEHLTEN Gemeinde. Ohne sie rechnete diese Zeile fuer jede der
    // einundachtzig Gemeinden Kassel — und das ist der Wert, der ohne Netz auf dem Schirm steht.
    nonisolated static func localToday() -> DayTimes {
        let rows = DashboardDataKt.dashboardRowsForToday(
            latitude: CommunitySelection.latitude, longitude: CommunitySelection.longitude)
        func mins(_ name: String) -> Int {
            guard let r = rows.first(where: { $0.name == name }) else { return 0 }
            let p = r.adhan.split(separator: ":")
            return (Int(p.first ?? "0") ?? 0) * 60 + (Int(p.last ?? "0") ?? 0)
        }
        return DayTimes(date: isoToday(), fajr: mins("Fajr"), sunrise: mins("Sunrise"),
                        dhuhr: mins("Dhuhr"), asr: mins("Asr"), maghrib: mins("Maghrib"), isha: mins("Isha"))
    }

    /// Die Zeiten eines beliebigen Tages.
    ///
    /// Fuer heute stehen die offiziellen Zeiten von vaktija.eu im Zwischenspeicher. Fuer jeden
    /// anderen Tag gibt es im Geraet keine offizielle Quelle, also wird lokal gerechnet und die
    /// heutige Abweichung (offiziell − lokal) je Gebet aufgeschlagen — dieselbe Rechnung, die der
    /// Kalender anzeigt. Zwei Bildschirme mit unterschiedlichen Zahlen fuer denselben Tag waeren
    /// schlimmer als ein paar Minuten Ungenauigkeit.
    ///
    /// Ohne diesen Weg wuerden Benachrichtigungen und Tracker-Fenster eine Woche lang die Zeiten
    /// von heute wiederholen — im Fruehjahr sind das am siebten Tag gut zwanzig Minuten daneben.
    nonisolated static func times(on day: Date) -> DayTimes {
        let key = iso(day)
        if key == isoToday() { return cachedOfficial() ?? localToday() }
        guard let d = localDay(day) else { return cachedOfficial() ?? localToday() }
        let c = calibration()
        func m(_ hhmm: String, _ i: Int) -> Int {
            let p = hhmm.split(separator: ":")
            let raw = (Int(p.first ?? "0") ?? 0) * 60 + (Int(p.last ?? "0") ?? 0)
            return raw + (c.indices.contains(i) ? c[i] : 0)
        }
        return DayTimes(date: key, fajr: m(d.fajr, 0), sunrise: m(d.sunrise, 1), dhuhr: m(d.dhuhr, 2),
                        asr: m(d.asr, 3), maghrib: m(d.maghrib, 4), isha: m(d.isha, 5))
    }

    // Der Monat wird gepuffert: Der Planer fragt fuer sieben Tage je fuenf Gebete nach Zeiten, und
    // ohne Puffer wuerde derselbe Monat dutzendfach neu gerechnet.
    nonisolated(unsafe) private static var monthCache: [String: [CalendarDay]] = [:]
    nonisolated private static let monthLock = NSLock()

    /// Ein ganzer Monat, gepuffert.
    ///
    /// Der Kalender rief die Kotlin-Rechnung bisher unmittelbar auf — bei JEDEM Neuzeichnen der
    /// Ansicht, also fuer dreissig Tage neu. Hier teilt er sich den Puffer mit den geplanten
    /// Meldungen und dem Tracker.
    nonisolated static func month(year: Int, month: Int) -> [CalendarDay] {
        cachedMonth(year: year, month: month)
    }

    nonisolated private static func cachedMonth(year: Int, month: Int) -> [CalendarDay] {
        // Die Gemeinde gehoert in den Schluessel: Sonst liefert der Puffer nach einem Wechsel
        // weiter den Monat der alten Stadt.
        let key = "\(year)-\(month)-\(CommunitySelection.communityId)"
        monthLock.lock()
        defer { monthLock.unlock() }
        if let cached = monthCache[key] { return cached }
        let days = CalendarDataKt.monthForDisplay(
            year: Int32(year), month: Int32(month),
            latitude: CommunitySelection.latitude, longitude: CommunitySelection.longitude)
        monthCache[key] = days
        return days
    }

    nonisolated private static func localDay(_ day: Date) -> CalendarDay? {
        let cal = Calendar.current
        let days = cachedMonth(year: cal.component(.year, from: day),
                               month: cal.component(.month, from: day))
        let dayOfMonth = cal.component(.day, from: day)
        return days.first { Int($0.day) == dayOfMonth }
    }
}
