import Foundation
import UserNotifications

// Der Gebetstracker.
//
// Der alte war eine Liste zum Abhaken, jederzeit und in jede Richtung. Damit war die Flamme
// wertlos: man konnte abends fünf Haken setzen.
//
// Jetzt hat jedes Gebet ein FENSTER — vom Ikamet bis zum nächsten Adhan. Fajr endet am
// Sonnenaufgang, nicht am Mittag, und das ist die Regel, die das Ganze ehrlich macht: Wer Fajr
// bestätigen will, muss vor Sonnenaufgang aufstehen. Nach dem Fenster lässt sich nichts nachtragen.
//
// Alles bleibt auf dem Gerät. Nichts wird übertragen.

enum TrackedPrayer: String, CaseIterable {
    case fajr, dhuhr, asr, maghrib, isha

    var nameKey: String {
        switch self {
        case .fajr: return "prayer_fajr"
        case .dhuhr: return "prayer_dhuhr"
        case .asr: return "prayer_asr"
        case .maghrib: return "prayer_maghrib"
        case .isha: return "prayer_isha"
        }
    }
}

enum TrackerAnswer: String {
    case yes, no
}

/// Wo ein Gebet gerade steht — steuert Text und Knöpfe in der Ansicht.
enum TrackerState: Equatable {
    case upcoming(opensAt: Date)
    case open(closesAt: Date)
    case prayed
    case notPrayed
    /// Fenster zu, ohne Antwort. Zählt wie „nicht gebetet", heißt aber anders: Der Unterschied
    /// zwischen „ich habe Nein gesagt" und „ich war zu spät" ist für den Nutzer wichtig.
    case missed
}

enum PrayerTracker {

    // MARK: Fenster

    /// Öffnet zum Ikamet, schließt beim nächsten Adhan — bei Fajr am Sonnenaufgang.
    ///
    /// Isha schließt erst beim Fajr des FOLGETAGS. Das ist der einzige Fall, der über Mitternacht
    /// reicht, und ohne diese Ausnahme könnte man das Nachtgebet nie bestätigen.
    static func window(_ prayer: TrackedPrayer, on day: Date = Date()) -> (open: Date, close: Date)? {
        let times = PrayerStore.times(on: day)
        let rule = CommunityRule.cachedRule()
        let cal = Calendar.current
        let isFriday = cal.component(.weekday, from: day) == 6

        func at(_ minutes: Int, dayOffset: Int = 0) -> Date? {
            guard let base = cal.date(byAdding: .day, value: dayOffset, to: day) else { return nil }
            var c = cal.dateComponents([.year, .month, .day], from: base)
            c.hour = minutes / 60
            c.minute = minutes % 60
            return cal.date(from: c)
        }

        func hhmm(_ s: String) -> Int? {
            let p = s.split(separator: ":")
            guard p.count == 2, let h = Int(p[0]), let m = Int(p[1]) else { return nil }
            return h * 60 + m
        }

        let pair: (Date, Date)?
        switch prayer {
        case .fajr:
            // Ikamet ist beim Morgengebet eine feste Uhrzeit, kein Zuschlag. Im Winter liegt diese
            // feste Zeit VOR dem Gebetsruf — dann beginnt das Fenster beim Ruf. Vor der Zeit
            // gebetet ist nicht gebetet, und eine Regel, die das zulaesst, waere keine.
            let open = max(hhmm(rule.fajrIqamah) ?? times.fajr, times.fajr)
            pair = zip2(at(open), at(times.sunrise))
        case .dhuhr:
            // Freitags gilt die Dzuma-Zeit statt Ikamet — mit derselben Untergrenze.
            let open = isFriday ? max(hhmm(rule.jumua) ?? times.dhuhr, times.dhuhr)
                                : times.dhuhr + rule.dhuhrOffsetMin
            pair = zip2(at(open), at(times.asr))
        case .asr:
            pair = zip2(at(times.asr + rule.asrOffsetMin), at(times.maghrib))
        case .maghrib:
            pair = zip2(at(times.maghrib + rule.maghribOffsetMin), at(times.isha))
        case .isha:
            // Der einzige Fall ueber Mitternacht: Isha schliesst beim Fajr des FOLGETAGS — und der
            // steht in dessen Zeiten, nicht in denen von heute.
            let tomorrow = cal.date(byAdding: .day, value: 1, to: day) ?? day
            pair = zip2(at(times.isha + rule.ishaOffsetMin),
                        at(PrayerStore.times(on: tomorrow).fajr, dayOffset: 1))
        }

        // Ein Fenster, das schliesst bevor es oeffnet, gibt es nicht. Statt einer unsinnigen Zeile
        // in der Liste ("bis 04:12", wenn es um 06:15 aufmacht) lieber gar keine.
        guard let (open, close) = pair, open < close else { return nil }
        return (open, close)
    }

    /// Zwei Optionals zu einem Paar — spart die Wiederholung von `guard let` in jedem Zweig.
    private static func zip2(_ a: Date?, _ b: Date?) -> (Date, Date)? {
        guard let a, let b else { return nil }
        return (a, b)
    }

    // MARK: Antworten

    private static func key(_ prayer: TrackedPrayer, _ day: Date) -> String {
        "tracker_\(dayKey(day))_\(prayer.rawValue)"
    }

    /// Der Schluessel eines Tages im Speicher.
    ///
    /// Fest auf gregorianisch und POSIX: Ohne das schreibt ein Geraet mit arabischer Sprache
    /// „٢٠٢٦-٠٨-٢٩" und faende die eigenen Antworten nach einem Sprachwechsel nicht wieder.
    static func dayKey(_ date: Date) -> String {
        let f = DateFormatter()
        f.calendar = Calendar(identifier: .gregorian)
        f.locale = Locale(identifier: "en_US_POSIX")
        f.dateFormat = "yyyy-MM-dd"
        return f.string(from: date)
    }

    static func answer(_ prayer: TrackedPrayer, on day: Date = Date()) -> TrackerAnswer? {
        guard let raw = AppGroup.defaults.string(forKey: key(prayer, day)) else { return nil }
        return TrackerAnswer(rawValue: raw)
    }

    /// Nimmt eine Antwort an — aber nur INNERHALB des Fensters.
    ///
    /// Die Prüfung passiert hier und nicht nur in der Ansicht, weil eine Benachrichtigung stunden-
    /// lang in der Leiste liegen bleiben kann. Ohne sie würde ein „Ja" auf die Fajr-Frage um die
    /// Mittagszeit zählen. Gibt zurück, ob die Antwort gezählt hat.
    @discardableResult
    static func record(_ prayer: TrackedPrayer, _ value: TrackerAnswer,
                       at now: Date = Date(), on day: Date = Date()) -> Bool {
        guard let w = window(prayer, on: day), now >= w.open, now <= w.close else { return false }
        AppGroup.defaults.set(value.rawValue, forKey: key(prayer, day))
        return true
    }

    static func state(_ prayer: TrackedPrayer, at now: Date = Date(), on day: Date = Date()) -> TrackerState {
        if let a = answer(prayer, on: day) { return a == .yes ? .prayed : .notPrayed }
        guard let w = window(prayer, on: day) else { return .missed }
        if now < w.open { return .upcoming(opensAt: w.open) }
        if now > w.close { return .missed }
        return .open(closesAt: w.close)
    }

    // MARK: Flamme

    static let rewardDays = 30

    /// Ein Tag zählt nur, wenn ALLE fünf mit Ja beantwortet sind. Ein Nein oder ein abgelaufenes
    /// Fenster beendet die Serie — deshalb wird rückwärts gezählt, bis der erste unvollständige
    /// Tag kommt.
    ///
    /// Der heutige Tag zählt noch nicht mit, solange er nicht vollständig ist: Sonst stünde die
    /// Flamme morgens auf 0 und abends wieder auf ihrem Wert, was aussähe wie ein Fehler.
    static func streak(now: Date = Date()) -> Int {
        let cal = Calendar.current
        var count = 0
        var day = complete(on: now) ? now : cal.date(byAdding: .day, value: -1, to: now) ?? now
        while complete(on: day) {
            count += 1
            guard let previous = cal.date(byAdding: .day, value: -1, to: day) else { break }
            day = previous
        }
        return count
    }

    static func complete(on day: Date) -> Bool {
        TrackedPrayer.allCases.allSatisfy { answer($0, on: day) == .yes }
    }

    static func answeredCount(on day: Date = Date()) -> Int {
        TrackedPrayer.allCases.filter { answer($0, on: day) == .yes }.count
    }
}
