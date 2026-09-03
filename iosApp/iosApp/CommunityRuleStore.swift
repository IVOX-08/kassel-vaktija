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
    /// Wessen Regeln gerade gehoert werden — siehe NewsStore, gleicher Grund.
    private var listeningTo: String?
    private var communityObserver: NSObjectProtocol?

    private init() {
        rule = CommunityRule.cachedRule()
        communityObserver = NotificationCenter.default.addObserver(
            forName: .communityDidChange, object: nil, queue: .main
        ) { [weak self] _ in
            Task { @MainActor in self?.restart() }
        }
    }

    deinit { listener?.remove() }

    /// Von vorn, fuer die neue Gemeinde.
    ///
    /// Bis die neuen Regeln da sind, gelten die Auslieferungswerte — NICHT die der alten
    /// Gemeinde. Ikamet-Zeiten einer fremden Stadt unter dem eigenen Namen sind schlimmer als
    /// die allgemeinen Vorgaben, und der Zwischenspeicher gehoerte ebenfalls der alten Gemeinde.
    private func restart() {
        listener?.remove()
        listener = nil
        listeningTo = nil
        rule = .fallback
        CommunityRule.cache(.fallback)
        start()
    }

    /// Live document — the board's edit reaches open apps without a restart.
    func start() {
        guard FirebaseApp.app() != nil else { return }
        if listener != nil, listeningTo == Community.id { return }
        listener?.remove()
        listeningTo = Community.id
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
