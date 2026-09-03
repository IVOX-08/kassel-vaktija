import AVFoundation

// Plays the notification-tone previews ("Adhan testen" / sound picker): adhan_short.mp3
// (Kurzer Adhan) and chime.wav (Signalton). Both sit at the bundle root rather than in an
// "audio" subfolder, because UNNotificationSound only looks there — see project.yml.
final class SoundPlayer: NSObject, AVAudioPlayerDelegate {
    static let shared = SoundPlayer()
    private var player: AVAudioPlayer?

    func play(_ name: String, ext: String) {
        guard let url = Bundle.main.url(forResource: name, withExtension: ext) else { return }
        try? AVAudioSession.sharedInstance().setCategory(.playback, mode: .default)
        try? AVAudioSession.sharedInstance().setActive(true)
        player = try? AVAudioPlayer(contentsOf: url)
        player?.delegate = self
        player?.prepareToPlay()
        player?.play()
    }

    /// Die Sitzung wieder abgeben, sobald der Ton durch ist.
    ///
    /// Vorher wurde sie aktiviert und nie zurueckgegeben: Wer beim Einstellen Musik oder einen
    /// Podcast hoerte, hoerte nach dem Probeton nichts mehr — bis die App beendet war. Mit
    /// `notifyOthersOnDeactivation` sagt iOS der anderen App, dass sie weitermachen darf.
    func audioPlayerDidFinishPlaying(_ player: AVAudioPlayer, successfully flag: Bool) {
        release()
    }

    func audioPlayerDecodeErrorDidOccur(_ player: AVAudioPlayer, error: Error?) {
        release()
    }

    private func release() {
        player = nil
        try? AVAudioSession.sharedInstance().setActive(false, options: .notifyOthersOnDeactivation)
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

/// Der Ton, mit dem eine Mitteilung der Gemeinde ankommt: der LEISE TON, fuer alle gleich.
///
/// Keine Einstellung, weil es auf iOS keine geben kann: Apple laesst den ABSENDER den Ton einer
/// Push-Meldung bestimmen, nicht das Telefon. Ein Schalter in den Einstellungen haette etwas
/// versprochen, das nie gewirkt haette.
///
/// Gesetzt wird er in der Cloud Function (`functions/index.js`, `apns.payload.aps.sound`). Der
/// Name muss genau auf diese Datei zeigen — findet iOS sie nicht, klingelt still der Standardton.
///
/// Auf Android bleibt die Wahl bestehen: dort haengt der Ton am Kanal, und das Telefon entscheidet.
enum NewsSound {
    static let file = "tone_soft"
    static let ext = "wav"
}
