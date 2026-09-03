import Foundation
import FirebaseCore
import FirebaseAuth
import FirebaseFirestore

// Community announcements, read from the SAME Firestore project as Android (`kassel-vaktija`).
// Die Feldnamen sind vom laufenden Backend vorgegeben — nichts hier umbenennen, die Android-App
// im Play Store schreibt dieselben Dokumente. Siehe docs/multi-gemeinde/FUER-DIE-IOS-APP.md.
//
// Seit dem Umbau auf zwanzig Gemeinden gibt es ZWEI Quellen:
//   communities/{id}/news  — die Beiträge dieser Gemeinde
//   broadcasts             — verbandsweite Mitteilungen des Hauptadministrators
// Beide erscheinen in einer gemeinsamen Liste, nach Datum sortiert.

/// Herz oder Daumen. Der gespeicherte Wert ist kleingeschrieben, so schreibt es Android.
enum Reaction: String {
    case like, dislike

    static func from(_ raw: String?) -> Reaction? {
        guard let raw else { return nil }
        return Reaction(rawValue: raw)
    }
}

/// One announcement. Mirrors the Kotlin `NewsItem`.
struct NewsItem: Identifiable {
    /// Die Firestore-Dokument-ID, wie auf Android. Bild und Löschen hängen daran.
    let id: String
    /// Verbandsweite Mitteilung statt Beitrag dieser Gemeinde — entscheidet über den Pfad.
    let isBroadcast: Bool
    let titleByLang: [String: String]
    let bodyByLang: [String: String]
    /// Language the admin actually wrote in — the fallback when the reader's language is missing.
    let sourceLang: String
    /// Epoch MILLIseconds (Android writes `System.currentTimeMillis()`), not seconds.
    let createdAt: Int64
    let hasImage: Bool
    let likeCount: Int
    let dislikeCount: Int
    /// Empfängerkreis einer verbandsweiten Mitteilung. **Leer heißt alle.**
    let audience: [String]

    /// Ob diese Mitteilung die gewählte Gemeinde erreicht.
    func reaches(_ communityId: String) -> Bool {
        audience.isEmpty || audience.contains(communityId)
    }

    func title(_ lang: String) -> String { Self.pick(titleByLang, lang, sourceLang) }
    func body(_ lang: String) -> String { Self.pick(bodyByLang, lang, sourceLang) }

    private static func pick(_ map: [String: String], _ lang: String, _ source: String) -> String {
        map[lang] ?? map[source] ?? map.values.first ?? ""
    }

    var date: Date? {
        createdAt > 0 ? Date(timeIntervalSince1970: Double(createdAt) / 1000) : nil
    }
}

@MainActor
final class NewsStore: ObservableObject {
    /// nil = still loading; [] = genuinely empty (or offline with an empty cache).
    @Published private(set) var items: [NewsItem]?
    /// Was dieses Gerät bei welchem Beitrag gewählt hat, für den gedrückten Zustand der Knöpfe.
    @Published private(set) var myReactions: [String: Reaction] = [:]
    /// Beiträge, deren eigene Reaktion schon nachgeschlagen wurde — auch die ohne Reaktion.
    /// Ohne diese Menge fragte jede Aktualisierung des Feeds erneut für JEDEN Beitrag nach.
    private var reactionsChecked: Set<String> = []

    private var newsListener: ListenerRegistration?
    private var broadcastListener: ListenerRegistration?
    /// Die beiden Quellen kommen getrennt an; gemischt wird erst beim Zusammenbauen der Liste.
    private var communityItems: [NewsItem] = []
    private var broadcastItems: [NewsItem] = []
    /// Flyers already fetched this session. They are only loaded when a card scrolls into view.
    private var imageCache: [String: Data] = [:]
    /// Wessen Mitteilungen gerade gehoert werden. Der Pfad steckt im Zuhoerer und laesst sich
    /// nicht nachtraeglich umbiegen — beim Wechsel muss er neu angelegt werden.
    private var listeningTo: String?
    private var communityObserver: NSObjectProtocol?

    init() {
        communityObserver = NotificationCenter.default.addObserver(
            forName: .communityDidChange, object: nil, queue: .main
        ) { [weak self] _ in
            Task { @MainActor in self?.restart() }
        }
    }

    deinit {
        newsListener?.remove()
        broadcastListener?.remove()
        if let communityObserver { NotificationCenter.default.removeObserver(communityObserver) }
    }

    /// Von vorn, fuer die neue Gemeinde.
    ///
    /// Die alten Beitraege werden weggeworfen, nicht stehen gelassen: Sie gehoeren einer anderen
    /// Gemeinde, und sie in der Liste zu belassen, bis die neuen eintreffen, waere schlimmer als
    /// ein kurzer Ladezustand.
    private func restart() {
        stop()
        communityItems = []
        broadcastItems = []
        imageCache = [:]
        myReactions = [:]
        reactionsChecked = []
        items = nil
        start()
    }

    /// Live feed, newest first. Firestore serves the local cache first, so this works offline.
    func start() {
        guard FirebaseApp.app() != nil else { return }
        // Haengt der Zuhoerer schon an DIESER Gemeinde, ist nichts zu tun.
        if newsListener != nil, listeningTo == Community.id { return }
        stop()
        listeningTo = Community.id
        newsListener = Community.news
            .order(by: "createdAt", descending: true)
            .addSnapshotListener { [weak self] snapshot, _ in
                guard let snapshot else { return }
                Task { @MainActor in
                    self?.communityItems = snapshot.documents.map { Self.item($0, isBroadcast: false) }
                    self?.merge()
                }
            }
        broadcastListener = Community.broadcasts
            .order(by: "createdAt", descending: true)
            .addSnapshotListener { [weak self] snapshot, _ in
                // Fehlt die Sammlung oder verbietet eine Regel den Zugriff, bleibt es einfach bei
                // den Beiträgen der Gemeinde — kein Grund, die Liste leer zu lassen.
                guard let snapshot else { return }
                Task { @MainActor in
                    self?.broadcastItems = snapshot.documents.map { Self.item($0, isBroadcast: true) }
                    self?.merge()
                }
            }
    }

    func stop() {
        newsListener?.remove()
        newsListener = nil
        broadcastListener?.remove()
        broadcastListener = nil
        listeningTo = nil
    }

    private func merge() {
        // Hier gefiltert statt in der Abfrage: „an alle" steht als leere Liste, und das lässt sich
        // mit keiner array-contains-Abfrage ausdrücken. Verbandsweite Mitteilungen sind wenige
        // genug, dass der Client entscheiden kann.
        let reaching = broadcastItems.filter { $0.reaches(Community.id) }
        items = (communityItems + reaching).sorted { $0.createdAt > $1.createdAt }
        Task { await loadMyReactions() }
    }

    private static func item(_ doc: QueryDocumentSnapshot, isBroadcast: Bool) -> NewsItem {
        let d = doc.data()
        return NewsItem(
            id: doc.documentID,
            isBroadcast: isBroadcast,
            titleByLang: d["title"] as? [String: String] ?? [:],
            bodyByLang: d["body"] as? [String: String] ?? [:],
            sourceLang: d["sourceLang"] as? String ?? "bs",
            createdAt: (d["createdAt"] as? NSNumber)?.int64Value ?? 0,
            hasImage: d["hasImage"] as? Bool ?? false,
            likeCount: (d["likeCount"] as? NSNumber)?.intValue ?? 0,
            dislikeCount: (d["dislikeCount"] as? NSNumber)?.intValue ?? 0,
            audience: d["audience"] as? [String] ?? []
        )
    }

    // MARK: Reaktionen

    /// Das Elterndokument, an dem die Summen hängen.
    private func parent(_ item: NewsItem) -> DocumentReference {
        item.isBroadcast ? Community.broadcasts.document(item.id) : Community.news.document(item.id)
    }

    /// Was dieses Gerät bei welchem Beitrag gewählt hat — EINMAL je Beitrag.
    ///
    /// Vorher lief das nach jeder Feed-Aktualisierung für die ganze Liste durch, also ein
    /// Firestore-Lesevorgang je Beitrag, immer wieder. Bei vierzig Mitteilungen und zwei Quellen
    /// waren das schnell hunderte Lesevorgänge je Sitzung, die alle gegen das Kontingent zählen.
    ///
    /// Der eigene Druck auf einen Knopf steht ohnehin sofort in `myReactions` (siehe `react`),
    /// nachzulesen ist also nur, was ein anderes Gerät desselben Nutzers getan hat.
    private func loadMyReactions() async {
        guard let uid = Auth.auth().currentUser?.uid, let items else { return }
        let pending = items.filter { !reactionsChecked.contains($0.id) }
        guard !pending.isEmpty else { return }
        var found: [String: Reaction] = [:]
        for item in pending {
            let ref = parent(item).collection("reactions").document(uid)
            if let snap = try? await ref.getDocument(),
               let value = Reaction.from(snap.data()?["value"] as? String) {
                found[item.id] = value
            }
        }
        for id in pending.map(\.id) { reactionsChecked.insert(id) }
        myReactions.merge(found) { _, new in new }
    }

    /// Setzt, wechselt oder nimmt eine Reaktion zurück — dieselbe Logik wie Android.
    ///
    /// Die Summen können von den Einzeleinträgen abweichen, wenn ein Schreibvorgang scheitert und
    /// der andere durchgeht. Das ist bewusst hingenommen: es ist ein Stimmungsbild, keine Abrechnung.
    /// Exakt wäre nur mit einer Cloud Function zu haben, und die braucht den kostenpflichtigen Tarif.
    func react(_ item: NewsItem, _ choice: Reaction) async {
        guard FirebaseApp.app() != nil, let uid = Auth.auth().currentUser?.uid else { return }
        let parentRef = parent(item)
        let myRef = parentRef.collection("reactions").document(uid)
        let previous = myReactions[item.id]

        var deltas: [String: Any] = [:]
        func bump(_ reaction: Reaction, _ by: Int) {
            let field = reaction == .like ? "likeCount" : "dislikeCount"
            deltas[field] = FieldValue.increment(Int64(by))
        }

        if previous == choice {
            // Derselbe Knopf noch einmal: Reaktion zurücknehmen.
            try? await myRef.delete()
            bump(choice, -1)
            myReactions[item.id] = nil
        } else {
            try? await myRef.setData(["value": choice.rawValue])
            if let previous { bump(previous, -1) }
            bump(choice, 1)
            myReactions[item.id] = choice
        }
        // Wie die übrigen Schreibvorgänge nicht abgewartet: Firestore rechnet lokal sofort, damit
        // die Zahl unter dem Finger springt, auch ohne Verbindung.
        parentRef.updateData(deltas) { _ in }
    }

    // MARK: Bild

    /// The flyer for an announcement: a Base64 JPEG next to the post. Returns nil when it can't be
    /// loaded (offline, or never uploaded) so the UI can drop the slot instead of spinning.
    func image(_ item: NewsItem) async -> Data? {
        if let cached = imageCache[item.id] { return cached }
        guard FirebaseApp.app() != nil else { return nil }
        let ref = item.isBroadcast
            ? Community.broadcastImages.document(item.id)
            : Community.newsImages.document(item.id)
        guard let doc = try? await ref.getDocument(),
              let base64 = doc.data()?["data"] as? String,
              let bytes = Data(base64Encoded: base64) else { return nil }
        imageCache[item.id] = bytes
        return bytes
    }
}
