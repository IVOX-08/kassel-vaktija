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

    var isActive: Bool { status == nil || status == "active" }
    var isSuspended: Bool { status == "suspended" }
    var isBlocked: Bool { status == "blocked" }

    /// Ort für Kopfzeile und Zeitquelle. Mehrere Standorte kommen vor (Gemeinde mit Filiale).
    var primaryLocation: CommunityLocation? { locations.first }
}

/// Die gewählte Gemeinde. Bewusst über UserDefaults und nicht über den ObservableObject-Speicher
/// erreichbar, weil Code ausserhalb der Oberfläche sie braucht — Firestore-Pfade, die Zeitquelle
/// und die geplanten Benachrichtigungen.
enum CommunitySelection {
    private static let communityKey = "selected_community_id"
    private static let locationKey = "selected_location_id"
    private static let slugKey = "selected_vaktija_slug"

    /// Kassel bleibt der Ausgangspunkt: die App war bis hierher Kassels App, und ein Bestandsnutzer
    /// soll nach dem Update nicht vor einer Auswahl stehen, die er nie getroffen hat.
    static let fallbackCommunityId = "igbd-gemeinde-sandzak-kassel"

    static var communityId: String {
        UserDefaults.standard.string(forKey: communityKey) ?? fallbackCommunityId
    }

    static var locationId: String? {
        UserDefaults.standard.string(forKey: locationKey)
    }

    /// Das Kürzel liegt bewusst mit in den Voreinstellungen: die Zeitquelle wird auch aus dem
    /// Hintergrund geholt (Silent Push, geplante Benachrichtigungen), und der Katalog hängt am
    /// Hauptthread. So kommt jeder Aufrufer ohne Umweg an den richtigen Ort.
    static var vaktijaSlug: String {
        UserDefaults.standard.string(forKey: slugKey) ?? "kassel"
    }

    static func set(community: String, location: String?, vaktijaSlug: String?) {
        UserDefaults.standard.set(community, forKey: communityKey)
        UserDefaults.standard.set(location, forKey: locationKey)
        if let vaktijaSlug { UserDefaults.standard.set(vaktijaSlug, forKey: slugKey) }
    }

    /// Ob der Nutzer je selbst gewählt hat — sonst zeigt das Onboarding die Auswahl.
    static var hasChosen: Bool {
        UserDefaults.standard.string(forKey: communityKey) != nil
    }
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
                    self?.all = live.sorted { $0.name < $1.name }
                    self?.applySelection()
                }
            }
    }

    func choose(_ community: CommunityInfo, location: CommunityLocation?) {
        let place = location ?? community.primaryLocation
        CommunitySelection.set(community: community.id,
                               location: place?.id,
                               vaktijaSlug: place?.vaktijaSlug)
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
                                   location: place.id, vaktijaSlug: place.vaktijaSlug)
        }
    }

    // MARK: Quellen

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
            logoUrl: d["logoUrl"] as? String
        )
    }
}
