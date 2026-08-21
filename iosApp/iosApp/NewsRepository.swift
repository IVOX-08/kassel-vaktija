import Foundation
import FirebaseCore
import FirebaseFirestore

// Community announcements, read from the SAME Firestore project as Android (`kassel-vaktija`).
// The field shapes are fixed by the live backend — see docs/ios/FIREBASE-HANDOFF.md. Do not rename
// anything here: the Android app in the Play Store writes these documents.

/// One announcement. Mirrors the Kotlin `NewsItem`.
struct NewsItem: Identifiable {
    let id: String
    let titleByLang: [String: String]
    let bodyByLang: [String: String]
    /// Language the admin actually wrote in — the fallback when the reader's language is missing.
    let sourceLang: String
    /// Epoch MILLIseconds (Android writes `System.currentTimeMillis()`), not seconds.
    let createdAt: Int64
    let hasImage: Bool

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

    private var listener: ListenerRegistration?
    /// Flyers already fetched this session. They are only loaded when a card scrolls into view.
    private var imageCache: [String: Data] = [:]

    deinit { listener?.remove() }

    /// Live feed, newest first. Firestore serves the local cache first, so this works offline.
    func start() {
        guard listener == nil, FirebaseApp.app() != nil else { return }
        listener = Firestore.firestore().collection("news")
            .order(by: "createdAt", descending: true)
            .addSnapshotListener { [weak self] snapshot, _ in
                guard let snapshot else { return }
                self?.items = snapshot.documents.map(Self.item)
            }
    }

    func stop() {
        listener?.remove()
        listener = nil
    }

    private static func item(_ doc: QueryDocumentSnapshot) -> NewsItem {
        let d = doc.data()
        return NewsItem(
            id: doc.documentID,
            titleByLang: d["title"] as? [String: String] ?? [:],
            bodyByLang: d["body"] as? [String: String] ?? [:],
            sourceLang: d["sourceLang"] as? String ?? "bs",
            createdAt: (d["createdAt"] as? NSNumber)?.int64Value ?? 0,
            hasImage: d["hasImage"] as? Bool ?? false
        )
    }

    /// The flyer for an announcement: a Base64 JPEG in `news_images/{sameId}`. Returns nil when it
    /// can't be loaded (offline, or never uploaded) so the UI can drop the slot instead of spinning.
    func image(_ id: String) async -> Data? {
        if let cached = imageCache[id] { return cached }
        guard FirebaseApp.app() != nil else { return nil }
        guard let doc = try? await Firestore.firestore().collection("news_images").document(id).getDocument(),
              let base64 = doc.data()?["data"] as? String,
              let bytes = Data(base64Encoded: base64) else { return nil }
        imageCache[id] = bytes
        return bytes
    }
}
