import Foundation

// Welche Gemeinde gewaehlt ist — geteilt zwischen App und Widget.
//
// Steht im gemeinsamen Code, weil das Widget dieselbe Antwort braucht: Es zeigt das Wappen und die
// Gebetszeiten der gewaehlten Gemeinde, und bis hierher zeigte es jedem Kassel.

/// Die gewählte Gemeinde. Bewusst über UserDefaults und nicht über den ObservableObject-Speicher
/// erreichbar, weil Code ausserhalb der Oberfläche sie braucht — Firestore-Pfade, die Zeitquelle
/// und die geplanten Benachrichtigungen.
enum CommunitySelection {
    private static let communityKey = "selected_community_id"
    private static let locationKey = "selected_location_id"
    private static let slugKey = "selected_vaktija_slug"
    private static let nameKey = "selected_community_name"

    /// Kassel bleibt der Ausgangspunkt: die App war bis hierher Kassels App, und ein Bestandsnutzer
    /// soll nach dem Update nicht vor einer Auswahl stehen, die er nie getroffen hat.
    static let fallbackCommunityId = "igbd-gemeinde-sandzak-kassel"

    static var communityId: String {
        AppGroup.defaults.string(forKey: communityKey) ?? fallbackCommunityId
    }

    static var locationId: String? {
        AppGroup.defaults.string(forKey: locationKey)
    }

    /// Das Kürzel liegt bewusst mit in den Voreinstellungen: die Zeitquelle wird auch aus dem
    /// Hintergrund geholt (Silent Push, geplante Benachrichtigungen), und der Katalog hängt am
    /// Hauptthread. So kommt jeder Aufrufer ohne Umweg an den richtigen Ort.
    static var vaktijaSlug: String {
        AppGroup.defaults.string(forKey: slugKey) ?? "kassel"
    }

    /// Der Name der Gemeinde. Nur fuer das Widget mitgeschrieben: Es hat keinen Zugriff auf
    /// das Verzeichnis (das haengt an Firestore), muss aber den Namen unter das
    /// Verbandszeichen setzen koennen.
    static var communityName: String? {
        AppGroup.defaults.string(forKey: nameKey)
    }

    static func set(community: String, location: String?, vaktijaSlug: String?, name: String? = nil) {
        AppGroup.defaults.set(community, forKey: communityKey)
        AppGroup.defaults.set(location, forKey: locationKey)
        if let vaktijaSlug { AppGroup.defaults.set(vaktijaSlug, forKey: slugKey) }
        if let name { AppGroup.defaults.set(name, forKey: nameKey) }
    }

    /// Ob der Nutzer je selbst gewählt hat — sonst zeigt das Onboarding die Auswahl.
    static var hasChosen: Bool {
        AppGroup.defaults.string(forKey: communityKey) != nil
    }
}
