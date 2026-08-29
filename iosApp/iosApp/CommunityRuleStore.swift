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

@MainActor
final class CommunityRuleStore: ObservableObject {
    static let shared = CommunityRuleStore()

    @Published private(set) var rule: CommunityRule

    private var listener: ListenerRegistration?

    private init() {
        rule = CommunityRule.cachedRule()
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
                CommunityRule.cache(parsed)
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

    private static func cached() -> CommunityRule? { CommunityRule.cached() }
}
