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
    private static let latKey = "selected_latitude"
    private static let lngKey = "selected_longitude"

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

    /// Die Koordinaten des gewaehlten Ortes.
    ///
    /// Liegen aus demselben Grund hier wie das Kuerzel: Die Gebetszeiten werden auch im
    /// Hintergrund gerechnet — im Widget, beim geplanten Adhan, beim Tracker-Fenster von morgen —
    /// und dort gibt es weder Firestore noch den Hauptthread. Bis hierher rechnete jede dieser
    /// Stellen fuer Kassel, egal welche Gemeinde gewaehlt war.
    ///
    /// Kassel bleibt der Rueckfall, solange nie gewaehlt wurde.
    static var latitude: Double {
        let v = AppGroup.defaults.double(forKey: latKey)
        return v == 0 ? kasselLatitude : v
    }

    static var longitude: Double {
        let v = AppGroup.defaults.double(forKey: lngKey)
        return v == 0 ? kasselLongitude : v
    }

    static let kasselLatitude = 51.3127
    static let kasselLongitude = 9.4797

    /// Der Name der Gemeinde. Nur fuer das Widget mitgeschrieben: Es hat keinen Zugriff auf
    /// das Verzeichnis (das haengt an Firestore), muss aber den Namen unter das
    /// Verbandszeichen setzen koennen.
    static var communityName: String? {
        AppGroup.defaults.string(forKey: nameKey)
    }

    static func set(community: String, location: String?, vaktijaSlug: String?, name: String? = nil,
                    latitude: Double? = nil, longitude: Double? = nil) {
        AppGroup.defaults.set(community, forKey: communityKey)
        AppGroup.defaults.set(location, forKey: locationKey)
        if let vaktijaSlug { AppGroup.defaults.set(vaktijaSlug, forKey: slugKey) }
        if let name { AppGroup.defaults.set(name, forKey: nameKey) }
        // 0/0 liegt im Golf von Guinea. Ein Ort ohne Koordinaten darf die vorhandenen nicht
        // ueberschreiben — dann lieber die alten behalten als in den Atlantik zeigen.
        if let latitude, let longitude, latitude != 0, longitude != 0 {
            AppGroup.defaults.set(latitude, forKey: latKey)
            AppGroup.defaults.set(longitude, forKey: lngKey)
        }
    }

    /// Ob der Nutzer je selbst gewählt hat — sonst zeigt das Onboarding die Auswahl.
    static var hasChosen: Bool {
        AppGroup.defaults.string(forKey: communityKey) != nil
    }
}
