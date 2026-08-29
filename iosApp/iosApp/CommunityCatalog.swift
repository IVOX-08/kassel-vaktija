import Foundation
import FirebaseCore
import FirebaseFirestore

// Das Verzeichnis der Gemeinden und die Auswahl des Nutzers.
//
// Warum eine Kopie im App-Paket: beim allerersten Start ist noch nichts aus Firestore geladen,
// und ohne Verzeichnis gäbe es nichts auszuwählen — der Nutzer stünde vor einer leeren Liste.
// Die Wahrheit steht trotzdem in Firestore: beim Start wird `communities/` nachgeladen, damit
// eine neu aufgenommene Gemeinde ohne App-Update erscheint und ein Statuswechsel sofort wirkt.

struct CommunityLocation: Codable, Identifiable, Equatable {
    let id: String
    let name: String
    /// Kürzel für vaktija.eu — DIE Gebetszeitquelle dieses Ortes.
    let vaktijaSlug: String
    let latitude: Double
    let longitude: Double
}

struct CommunityInfo: Codable, Identifiable, Equatable {
    let id: String
    let name: String
    let address: String?
    /// fehlt oder "active" = sichtbar · "suspended" = nicht in der Auswahl, Zeiten laufen weiter
    /// · "blocked" = die App zeigt nur noch einen Hinweis
    let status: String?
    let locations: [CommunityLocation]
    let donationUrl: String?
    let logoUrl: String?
    /// Die Konten der Gemeinde in den sozialen Netzen. Jede Gemeinde traegt die des Verbands,
    /// bis sie eigene schickt; Kassel hat eigene.
    let facebookUrl: String?
    let instagramUrl: String?
    let youtubeUrl: String?

    /// Ikamet und Dzuma, mit denen eine Gemeinde ANFAENGT. Nur fuer den Import: Sobald die
    /// Gemeinde ihre eigenen Zeiten gesetzt hat, gilt das Dokument in Firestore, nicht das hier.
    let fajrIqamah: String?
    let jumua: String?
    let dhuhrOffsetMin: Int?
    let asrOffsetMin: Int?
    let maghribOffsetMin: Int?
    let ishaOffsetMin: Int?

    var isActive: Bool { status == nil || status == "active" }
    var isSuspended: Bool { status == "suspended" }
    var isBlocked: Bool { status == "blocked" }

    /// Ort für Kopfzeile und Zeitquelle. Mehrere Standorte kommen vor (Gemeinde mit Filiale).
    var primaryLocation: CommunityLocation? { locations.first }
}

@MainActor
final class CommunityCatalog: ObservableObject {
    static let shared = CommunityCatalog()

    /// Alle bekannten Gemeinden, gesperrte eingeschlossen — die Verwaltung des Hauptadministrators
    /// muss auch die sehen, die gerade nicht in der Auswahl stehen.
    @Published private(set) var all: [CommunityInfo] = []

    /// Was in der Auswahl erscheinen darf.
    var selectable: [CommunityInfo] { all.filter(\.isActive) }

    @Published private(set) var selected: CommunityInfo?
    @Published private(set) var selectedLocation: CommunityLocation?

    private var listener: ListenerRegistration?

    private init() {
        all = Self.bundled()
        applySelection()
    }

    deinit { listener?.remove() }

    /// Firestore ist die Wahrheit; die Datei im Paket überbrückt nur den ersten Start.
    func start() {
        guard listener == nil, FirebaseApp.app() != nil else { return }
        listener = Firestore.firestore().collection("communities")
            .addSnapshotListener { [weak self] snapshot, _ in
                guard let docs = snapshot?.documents, !docs.isEmpty else { return }
                let live = docs.compactMap(Self.parse)
                Task { @MainActor in
                    self?.all = Self.merged(live).sorted { $0.name < $1.name }
                    self?.applySelection()
                }
            }
    }

    func choose(_ community: CommunityInfo, location: CommunityLocation?) {
        let place = location ?? community.primaryLocation
        CommunitySelection.set(community: community.id,
                               location: place?.id,
                               vaktijaSlug: place?.vaktijaSlug,
                               name: community.name)
        // Wappen, Zeiten und Name im Widget haengen an dieser Wahl.
        WidgetRefresh.now()
        applySelection()
    }

    private func applySelection() {
        let id = CommunitySelection.communityId
        let found = all.first { $0.id == id } ?? all.first { $0.id == CommunitySelection.fallbackCommunityId }
        selected = found
        let locId = CommunitySelection.locationId
        let place = found?.locations.first { $0.id == locId } ?? found?.primaryLocation
        selectedLocation = place
        // Auch beim Nachladen aus Firestore mitschreiben: ändert eine Gemeinde ihr Kürzel,
        // soll die Zeitquelle ohne erneutes Auswählen stimmen.
        if let place {
            CommunitySelection.set(community: found?.id ?? CommunitySelection.communityId,
                                   location: place.id, vaktijaSlug: place.vaktijaSlug,
                                   name: found?.name)
        }
    }

    // MARK: Quellen

    /// Legt die Werte aus Firestore ueber die mitgelieferten.
    ///
    /// Firestore gewinnt fuer alles, was dort steht — eine Gemeinde, die ihre Adresse oder ihre
    /// Konten aendert, soll das sofort sehen. Was dort FEHLT, kommt weiter aus dem Paket.
    ///
    /// Ohne diesen Schritt verschwanden die Konten der sozialen Netze, sobald Firestore antwortete:
    /// Die Dokumente dort tragen die Felder gar nicht, und die Liste wurde vorher komplett
    /// ersetzt. In der App hiess das: beim Start drei Symbole, eine Sekunde spaeter keine mehr.
    private static func merged(_ live: [CommunityInfo]) -> [CommunityInfo] {
        let seed = Dictionary(bundled().map { ($0.id, $0) }, uniquingKeysWith: { a, _ in a })
        return live.map { remote in
            guard let base = seed[remote.id] else { return remote }
            return CommunityInfo(
                id: remote.id,
                name: remote.name,
                address: remote.address ?? base.address,
                status: remote.status ?? base.status,
                locations: remote.locations.isEmpty ? base.locations : remote.locations,
                donationUrl: remote.donationUrl ?? base.donationUrl,
                logoUrl: remote.logoUrl ?? base.logoUrl,
                facebookUrl: remote.facebookUrl ?? base.facebookUrl,
                instagramUrl: remote.instagramUrl ?? base.instagramUrl,
                youtubeUrl: remote.youtubeUrl ?? base.youtubeUrl,
                fajrIqamah: base.fajrIqamah,
                jumua: base.jumua,
                dhuhrOffsetMin: base.dhuhrOffsetMin,
                asrOffsetMin: base.asrOffsetMin,
                maghribOffsetMin: base.maghribOffsetMin,
                ishaOffsetMin: base.ishaOffsetMin
            )
        }
    }

    private static func bundled() -> [CommunityInfo] {
        guard let url = Bundle.main.url(forResource: "communities", withExtension: "json"),
              let data = try? Data(contentsOf: url),
              let list = try? JSONDecoder().decode([CommunityInfo].self, from: data) else { return [] }
        return list.sorted { $0.name < $1.name }
    }

    private static func parse(_ doc: QueryDocumentSnapshot) -> CommunityInfo? {
        let d = doc.data()
        guard let name = d["name"] as? String else { return nil }
        let locations = (d["locations"] as? [[String: Any]] ?? []).compactMap { l -> CommunityLocation? in
            guard let id = l["id"] as? String, let name = l["name"] as? String,
                  let slug = l["vaktijaSlug"] as? String else { return nil }
            return CommunityLocation(
                id: id, name: name, vaktijaSlug: slug,
                latitude: (l["latitude"] as? NSNumber)?.doubleValue ?? 0,
                longitude: (l["longitude"] as? NSNumber)?.doubleValue ?? 0
            )
        }
        return CommunityInfo(
            id: doc.documentID,
            name: name,
            address: d["address"] as? String,
            status: d["status"] as? String,
            locations: locations,
            donationUrl: d["donationUrl"] as? String,
            logoUrl: d["logoUrl"] as? String,
            facebookUrl: d["facebookUrl"] as? String,
            instagramUrl: d["instagramUrl"] as? String,
            youtubeUrl: d["youtubeUrl"] as? String,
            // Nicht aus Firestore: die gelebten Regeln stehen in `config/rules` und werden von
            // CommunityRuleStore gelesen. Hier stehen nur die Startwerte aus dem Paket.
            fajrIqamah: nil, jumua: nil,
            dhuhrOffsetMin: nil, asrOffsetMin: nil, maghribOffsetMin: nil, ishaOffsetMin: nil
        )
    }
}
