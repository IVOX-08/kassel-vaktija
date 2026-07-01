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
    // Community Iqamah rule (mirrors shared DashboardData): Fajr fixed 04:30; Dhuhr/Asr +10;
    // Maghrib +5; Isha +0; Sunrise none.
    static func rows(_ t: DayTimes) -> [PrayerRow] {
        [
            PrayerRow(name: "Fajr", adhan: DayTimes.hhmm(t.fajr), iqamah: DayTimes.hhmm(4 * 60 + 30)),
            PrayerRow(name: "Sunrise", adhan: DayTimes.hhmm(t.sunrise), iqamah: nil),
            PrayerRow(name: "Dhuhr", adhan: DayTimes.hhmm(t.dhuhr), iqamah: DayTimes.hhmm(t.dhuhr + 10)),
            PrayerRow(name: "Asr", adhan: DayTimes.hhmm(t.asr), iqamah: DayTimes.hhmm(t.asr + 10)),
            PrayerRow(name: "Maghrib", adhan: DayTimes.hhmm(t.maghrib), iqamah: DayTimes.hhmm(t.maghrib + 5)),
            PrayerRow(name: "Isha", adhan: DayTimes.hhmm(t.isha), iqamah: DayTimes.hhmm(t.isha)),
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
    static let url = URL(string: "https://vaktija.eu/kassel")!
    static let userAgent =
        "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36 KasselVaktija"

    static func fetchToday() async throws -> DayTimes {
        var req = URLRequest(url: url, timeoutInterval: 15)
        req.setValue(userAgent, forHTTPHeaderField: "User-Agent")
        req.setValue("bs,hr,sr", forHTTPHeaderField: "Accept-Language")
        let (data, _) = try await URLSession.shared.data(for: req)
        guard let html = String(data: data, encoding: .utf8) else { throw Err.empty }
        return try parse(html)
    }

    enum Err: Error { case empty, noJSONLD, structure }

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

    private static let cacheKey = "vaktija_today"
    private static let calibKey = "vaktija_calibration" // 6 ints, minutes

    init() {
        today = PrayerStore.cachedOfficial() ?? PrayerStore.localToday()
        official = PrayerStore.cachedOfficial() != nil
        calibration = PrayerStore.calibration()
    }

    func refresh() async {
        guard let off = try? await VaktijaEuSource.fetchToday() else { return }
        // Key by the device's current date (vaktija's startDate can lag around midnight).
        let dated = DayTimes(date: PrayerStore.isoToday(), fajr: off.fajr, sunrise: off.sunrise,
                             dhuhr: off.dhuhr, asr: off.asr, maghrib: off.maghrib, isha: off.isha)
        if let data = try? JSONEncoder().encode(dated) { UserDefaults.standard.set(data, forKey: PrayerStore.cacheKey) }
        // Calibration = official − local(raw) today, per prayer.
        let raw = PrayerStore.localToday()
        let calib = zip(dated.asArray, raw.asArray).map { $0 - $1 }
        UserDefaults.standard.set(calib, forKey: PrayerStore.calibKey)
        today = dated
        official = true
        calibration = calib
    }

    var rows: [PrayerRow] { PrayerModel.rows(today) }

    static func calibration() -> [Int] {
        (UserDefaults.standard.array(forKey: calibKey) as? [Int]) ?? [0, 0, 0, 0, 0, 0]
    }

    // MARK: helpers

    private static func isoToday() -> String {
        let f = DateFormatter(); f.calendar = Calendar(identifier: .gregorian)
        f.locale = Locale(identifier: "en_US_POSIX"); f.dateFormat = "yyyy-MM-dd"
        return f.string(from: Date())
    }

    private static func cachedOfficial() -> DayTimes? {
        guard let data = UserDefaults.standard.data(forKey: cacheKey),
              let t = try? JSONDecoder().decode(DayTimes.self, from: data),
              t.date == isoToday() else { return nil }
        return t
    }

    // Local adhan fallback via the shared Kotlin dashboard rows ("HH:MM" strings → minutes).
    static func localToday() -> DayTimes {
        let rows = DashboardDataKt.dashboardRowsForToday()
        func mins(_ name: String) -> Int {
            guard let r = rows.first(where: { $0.name == name }) else { return 0 }
            let p = r.adhan.split(separator: ":")
            return (Int(p.first ?? "0") ?? 0) * 60 + (Int(p.last ?? "0") ?? 0)
        }
        return DayTimes(date: isoToday(), fajr: mins("Fajr"), sunrise: mins("Sunrise"),
                        dhuhr: mins("Dhuhr"), asr: mins("Asr"), maghrib: mins("Maghrib"), isha: mins("Isha"))
    }
}
