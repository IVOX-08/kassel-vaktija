import Foundation

// Der gemeinsame Speicher von App und Widget.
//
// Das Widget läuft in einem EIGENEN Prozess mit eigenem Sandkasten. `UserDefaults.standard` ist
// dort ein anderer, leerer Speicher — deshalb zeigte das Widget bisher jedem Nutzer Kassels
// Wappen und Kassels Gebetszeiten, egal welche Gemeinde er gewählt hatte. Wer in Nürnberg lebt,
// bekam auf dem Startbildschirm die Zeiten einer Stadt, in der er nicht ist.
//
// Eine App-Gruppe ist der einzige Weg, dass beide Seiten denselben Speicher sehen.
//
// EINRICHTUNG: Die Gruppe muss einmal im Apple-Developer-Portal angelegt werden
// (Certificates, Identifiers & Profiles → Identifiers → App Groups → `group.de.igbdsandzakkassel.vaktija`)
// und bei beiden App-IDs angehakt sein. Ohne sie schlägt das Signieren fehl — mit einer
// Fehlermeldung, die genau diese Gruppe nennt.
enum AppGroup {
    static let id = "group.de.igbdsandzakkassel.vaktija"

    /// Der geteilte Speicher — oder der eigene, falls die Gruppe (noch) nicht eingerichtet ist.
    ///
    /// Geprüft wird am CONTAINER, nicht am Rückgabewert von `UserDefaults(suiteName:)`. Der ist
    /// nämlich auch dann nicht `nil`, wenn die App das Gruppen-Recht gar nicht hat — sie schriebe
    /// dann in eine eigene Datei, die zufällig so heißt wie die Gruppe. Das Widget läse nebenan
    /// ins Leere, und beim späteren Nachrüsten der Gruppe wäre alles Gespeicherte auf einmal weg,
    /// weil der Umzug schon als erledigt gilt.
    ///
    /// Ohne Gruppe läuft die App also genau wie vorher weiter. Nur das Widget bleibt dann auf dem
    /// alten Stand — kein Datenverlust, kein halber Zustand.
    static let defaults: UserDefaults = {
        guard FileManager.default.containerURL(forSecurityApplicationGroupIdentifier: id) != nil,
              let shared = UserDefaults(suiteName: id)
        else { return .standard }
        return shared
    }()

    /// Die Schluessel, die dieser App gehoeren.
    ///
    /// Nur diese werden umgezogen. `dictionaryRepresentation()` liefert AUCH die hunderte
    /// Systemeintraege des Geraets (Sprachen, Tastaturen, Barrierefreiheit) — die alle beim ersten
    /// Start in die Gruppe zu schreiben, kostet spuerbar Zeit vor dem ersten Bild und fuellt den
    /// gemeinsamen Speicher mit Dingen, die niemand liest.
    private static let ownPrefixes = [
        "pn_",          // Gebet an/aus
        "pw_",          // Vorwarnung
        "q_",           // Koran: Lesezeichen, Schrift, Zoom, Tedschwid, Leseposition
        "tracker_",     // Antworten des Gebetstrackers, je Tag
        "selected_",    // gewaehlte Gemeinde, Ort, Kuerzel, Name
        "automute_",
        "vaktija_",     // geladene Zeiten und Kalibrierung
    ]

    private static let ownKeys: Set<String> = [
        "appColorScheme", "app_lang", "onboarding_done", "ob_phase",
        "notif_sound", "news_sound", "notif_silent", "msg_notif", "weekly_reminder",
        "community_rule", "dhikr_ptr", "tasbih_count", "tasbih_target", "qibla_use_device",
    ]

    private static func isOwn(_ key: String) -> Bool {
        ownKeys.contains(key) || ownPrefixes.contains { key.hasPrefix($0) }
    }

    /// Holt einmalig die eigenen Werte aus dem alten, app-eigenen Speicher herueber.
    ///
    /// Ohne diesen Schritt stuende jeder, der die App schon hat, nach dem Update wieder vor der
    /// Gemeindeauswahl — mit zurueckgesetzten Benachrichtigungen und verlorener Flamme.
    static func migrateOnce() {
        let flag = "migrated_to_app_group"
        guard defaults != .standard, !defaults.bool(forKey: flag) else { return }
        for (key, value) in UserDefaults.standard.dictionaryRepresentation() where isOwn(key) {
            // Nur was noch nicht drueben steht: Ein zweiter Lauf darf frische Werte nicht mit
            // alten ueberschreiben.
            guard defaults.object(forKey: key) == nil else { continue }
            defaults.set(value, forKey: key)
        }
        defaults.set(true, forKey: flag)
    }
}
