import Foundation

// Die von der Gemeinde gesetzten Regeln — Ikamet, Džuma, Bajram.
//
// Der Datentyp und sein Zwischenspeicher stehen im gemeinsamen Code, der Firestore-Anschluss
// nicht: Das Widget und der Benachrichtigungs-Planer brauchen die Werte, koennen aber weder
// Firestore noch den Hauptthread nutzen. Der Zwischenspeicher wird bei jeder Aenderung
// mitgeschrieben, ist also derselbe Stand — nur einen Wimpernschlag aelter.

struct CommunityRule: Codable, Equatable {
    var fajrIqamah: String      // "HH:mm"
    var jumua: String           // "HH:mm"
    var dhuhrOffsetMin: Int
    var asrOffsetMin: Int
    var maghribOffsetMin: Int
    var ishaOffsetMin: Int
    /// Announced Eid prayer — both appear and disappear together, so treat them as one.
    var bajramDate: String?     // ISO yyyy-MM-dd
    var bajramTime: String?     // "HH:mm"
    var updatedAt: Int64

    /// What the app used before the board could edit anything; also the offline fallback.
    static let fallback = CommunityRule(
        fajrIqamah: "04:30", jumua: "13:30",
        dhuhrOffsetMin: 10, asrOffsetMin: 10, maghribOffsetMin: 5, ishaOffsetMin: 0,
        bajramDate: nil, bajramTime: nil, updatedAt: 0
    )

    var bajram: (date: String, time: String)? {
        guard let d = bajramDate, let t = bajramTime, !d.isEmpty, !t.isEmpty else { return nil }
        return (d, t)
    }
}

extension CommunityRule {
    private static let cacheKey = "community_rule"

    static func cache(_ rule: CommunityRule) {
        guard let encoded = try? JSONEncoder().encode(rule) else { return }
        AppGroup.defaults.set(encoded, forKey: cacheKey)
    }

    static func cached() -> CommunityRule? {
        guard let data = AppGroup.defaults.data(forKey: cacheKey) else { return nil }
        return try? JSONDecoder().decode(CommunityRule.self, from: data)
    }

    /// Die zuletzt gespeicherte Regel, ohne den Hauptthread.
    static func cachedRule() -> CommunityRule { cached() ?? .fallback }
}
