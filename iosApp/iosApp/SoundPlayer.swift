import AVFoundation

// Plays the notification-tone previews ("Adhan testen" / sound picker): adhan_short.mp3
// (Kurzer Adhan) and chime.wav (Signalton). Both sit at the bundle root rather than in an
// "audio" subfolder, because UNNotificationSound only looks there — see project.yml.
final class SoundPlayer {
    static let shared = SoundPlayer()
    private var player: AVAudioPlayer?

    func play(_ name: String, ext: String) {
        guard let url = Bundle.main.url(forResource: name, withExtension: ext) else { return }
        try? AVAudioSession.sharedInstance().setCategory(.playback, mode: .default)
        try? AVAudioSession.sharedInstance().setActive(true)
        player = try? AVAudioPlayer(contentsOf: url)
        player?.prepareToPlay()
        player?.play()
    }
}

/// Der Ton, mit dem eine Gebetszeit ankommt.
///
/// Die drei schlichten Toene sind fuer Leute, die den Adhan nicht laufen lassen koennen — in der
/// Arbeit, in der Schule, auf Station. Sie wollen trotzdem wissen, dass die Zeit da ist; was sie
/// nicht haben koennen, ist eine Rezitation, die alle hoeren.
enum NotifSound: String, CaseIterable {
    case adhan, chime, bell, gong, soft

    var label: String {
        switch self {
        case .adhan: return L("sound_short_adhan")
        case .chime: return L("sound_chime")
        case .bell: return L("sound_bell")
        case .gong: return L("sound_gong")
        case .soft: return L("sound_soft")
        }
    }

    var file: String {
        switch self {
        case .adhan: return "adhan_short"
        case .chime: return "chime"
        case .bell: return "tone_bell"
        case .gong: return "tone_gong"
        case .soft: return "tone_soft"
        }
    }

    var ext: String { self == .adhan ? "mp3" : "wav" }
}

/// Der Ton, mit dem eine Mitteilung der Gemeinde ankommt.
///
/// Eine EIGENE Wahl, nicht dieselbe wie beim Adhan: Beide werden in ganz verschiedenen Lagen
/// gehoert. Der Adhan ruft zum Gebet, eine Mitteilung sagt, dass die Moschee etwas geschrieben
/// hat. Wer den Adhan auf eine schlichte Glocke gestellt hat, will die Mitteilung vielleicht
/// trotzdem anders hoeren — oder umgekehrt.
enum NewsSound: String, CaseIterable {
    case standard, bell, soft

    var label: String {
        switch self {
        case .standard: return L("sound_announcement")
        case .bell: return L("sound_bell")
        case .soft: return L("sound_soft")
        }
    }

    var file: String {
        switch self {
        case .standard: return "announcement"
        case .bell: return "tone_bell"
        case .soft: return "tone_soft"
        }
    }

    var ext: String { self == .standard ? "mp3" : "wav" }
}
