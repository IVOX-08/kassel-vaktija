import SwiftUI

// Links zu YouTube, Instagram und Facebook in einer Mitteilung.
//
// Warum überhaupt Links und keine hochgeladenen Videos: Ein Video von zwanzig Minuten in 4K wiegt
// rund 3 GB. Das bei jedem Zuschauer herunterzuladen kostet die Gemeinde dreistellig — je
// Mitteilung — und frisst beim Empfänger das Monatsvolumen. Der Link kostet nichts, hat keine
// Längengrenze, und die Plattform schickt von selbst die Qualität, die ins Netz des Zuschauers
// passt.
//
// Der Vorstand muss nichts Zusätzliches ausfüllen: Er schreibt den Link einfach in den Text, die
// App findet ihn. Damit ändert sich am gespeicherten Eintrag NICHTS — die Android-App liest
// dieselbe Mitteilung weiter, dort steht der Link als Text und lässt sich antippen oder kopieren.
enum SocialLink: Equatable {
    /// Die Kennung des Videos, nicht die ganze Adresse: Aus ihr kommen Vorschaubild und Abspieler.
    case youtube(id: String, url: URL)
    case instagram(URL)
    case facebook(URL)

    var url: URL {
        switch self {
        case .youtube(_, let url), .instagram(let url), .facebook(let url): return url
        }
    }

    /// Der Name der Plattform. Absichtlich kein übersetzter Text — „YouTube" heißt in allen acht
    /// Sprachen YouTube.
    var platform: String {
        switch self {
        case .youtube: return "YouTube"
        case .instagram: return "Instagram"
        case .facebook: return "Facebook"
        }
    }

    /// Das Vorschaubild.
    ///
    /// Nur YouTube liefert eines ohne Anmeldung. Instagram und Facebook verlangen dafür seit 2020
    /// ein Entwicklerkonto samt Zugriffsschlüssel — den würde diese App bei jedem Bild mitschicken
    /// müssen. Dort steht deshalb eine schlichte Karte, kein geratenes Bild.
    var thumbnail: URL? {
        guard case .youtube(let id, _) = self else { return nil }
        return URL(string: "https://img.youtube.com/vi/\(id)/hqdefault.jpg")
    }

    // MARK: Erkennen

    /// Sucht den ERSTEN erkannten Link im Text.
    ///
    /// Nur diese drei Plattformen bekommen eine Karte. Alles andere bleibt schlichter Text — eine
    /// Vorschaukarte für eine beliebige fremde Adresse wäre eine Einladung, sie ungeprüft
    /// anzutippen.
    static func first(in text: String) -> SocialLink? {
        guard let detector = try? NSDataDetector(types: NSTextCheckingResult.CheckingType.link.rawValue)
        else { return nil }
        let range = NSRange(text.startIndex..., in: text)
        for match in detector.matches(in: text, range: range) {
            if let url = match.url, let link = SocialLink(url) { return link }
        }
        return nil
    }

    init?(_ url: URL) {
        guard let host = url.host?.lowercased().replacingOccurrences(of: "www.", with: "") else { return nil }
        switch host {
        case "youtube.com", "m.youtube.com", "youtu.be", "youtube-nocookie.com":
            guard let id = SocialLink.youtubeID(url) else { return nil }
            self = .youtube(id: id, url: url)
        case "instagram.com":
            self = .instagram(url)
        case "facebook.com", "fb.com", "fb.watch", "m.facebook.com":
            self = .facebook(url)
        default:
            return nil
        }
    }

    /// Die elf Zeichen, die ein YouTube-Video bezeichnen.
    ///
    /// YouTube verteilt dieselbe Kennung über vier Adressformen — geteilt wird mal die eine, mal
    /// die andere, je nachdem, wo jemand auf „Teilen" gedrückt hat.
    private static func youtubeID(_ url: URL) -> String? {
        let host = url.host?.lowercased() ?? ""
        // youtu.be/ID
        if host.contains("youtu.be") {
            let id = url.lastPathComponent
            return id.isEmpty || id == "/" ? nil : id
        }
        let path = url.path
        // youtube.com/shorts/ID, /embed/ID, /live/ID
        for prefix in ["/shorts/", "/embed/", "/live/", "/v/"] where path.hasPrefix(prefix) {
            let id = String(path.dropFirst(prefix.count)).split(separator: "/").first.map(String.init)
            return (id?.isEmpty ?? true) ? nil : id
        }
        // youtube.com/watch?v=ID
        return URLComponents(url: url, resolvingAgainstBaseURL: false)?
            .queryItems?.first { $0.name == "v" }?.value
    }
}
