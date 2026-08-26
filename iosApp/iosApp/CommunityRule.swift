import Foundation
import FirebaseCore
import FirebaseFirestore

// The community-set rules the board edits from the admin area: Fajr Iqamah, Jumua, the per-prayer
// Iqamah offsets and the announced Eid prayer. Backed by the SAME Firestore document Android reads
// — seit dem Umbau auf zwanzig Gemeinden ist das `communities/{id}/config/rules` statt
// `config/community`. Feldnamen bleiben unverändert, siehe docs/multi-gemeinde/FUER-DIE-IOS-APP.md.
//
// Cached in UserDefaults so a cold start offline still shows the board's values rather than
// snapping back to the built-in defaults.
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

@MainActor
final class CommunityRuleStore: ObservableObject {
    static let shared = CommunityRuleStore()

    @Published private(set) var rule: CommunityRule

    private static let cacheKey = "community_rule"
    private var listener: ListenerRegistration?

    private init() {
        rule = Self.cached() ?? .fallback
    }

    deinit { listener?.remove() }

    /// Live document — the board's edit reaches open apps without a restart.
    func start() {
        guard listener == nil, FirebaseApp.app() != nil else { return }
        listener = Community.rules
            .addSnapshotListener { [weak self] snapshot, _ in
                guard let data = snapshot?.data() else { return }
                let parsed = Self.parse(data)
                self?.rule = parsed
                if let encoded = try? JSONEncoder().encode(parsed) {
                    UserDefaults.standard.set(encoded, forKey: Self.cacheKey)
                }
            }
    }

    private static func parse(_ d: [String: Any]) -> CommunityRule {
        let f = CommunityRule.fallback
        return CommunityRule(
            fajrIqamah: d["fajrIqamah"] as? String ?? f.fajrIqamah,
            jumua: d["jumua"] as? String ?? f.jumua,
            dhuhrOffsetMin: (d["dhuhrOffsetMin"] as? NSNumber)?.intValue ?? f.dhuhrOffsetMin,
            asrOffsetMin: (d["asrOffsetMin"] as? NSNumber)?.intValue ?? f.asrOffsetMin,
            maghribOffsetMin: (d["maghribOffsetMin"] as? NSNumber)?.intValue ?? f.maghribOffsetMin,
            ishaOffsetMin: (d["ishaOffsetMin"] as? NSNumber)?.intValue ?? f.ishaOffsetMin,
            bajramDate: d["bajramDate"] as? String,
            bajramTime: d["bajramTime"] as? String,
            updatedAt: (d["updatedAt"] as? NSNumber)?.int64Value ?? 0
        )
    }

    private static func cached() -> CommunityRule? {
        guard let data = UserDefaults.standard.data(forKey: cacheKey) else { return nil }
        return try? JSONDecoder().decode(CommunityRule.self, from: data)
    }
}
