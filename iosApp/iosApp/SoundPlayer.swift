import AVFoundation

// Plays the notification-tone previews ("Adhan testen" / sound picker). Files live in the bundled
// "audio" folder: adhan_short.mp3 (Kurzer Adhan) and chime.wav (Signalton).
final class SoundPlayer {
    static let shared = SoundPlayer()
    private var player: AVAudioPlayer?

    func play(_ name: String, ext: String) {
        guard let url = Bundle.main.url(forResource: name, withExtension: ext, subdirectory: "audio") else { return }
        try? AVAudioSession.sharedInstance().setCategory(.playback, mode: .default)
        try? AVAudioSession.sharedInstance().setActive(true)
        player = try? AVAudioPlayer(contentsOf: url)
        player?.prepareToPlay()
        player?.play()
    }
}

// The two selectable notification tones (spec 6.2). Value stored in @AppStorage.
enum NotifSound: String, CaseIterable {
    case adhan, chime
    var label: String { self == .adhan ? "Kurzer Adhan" : "Signalton" }
    var file: String { self == .adhan ? "adhan_short" : "chime" }
    var ext: String { self == .adhan ? "mp3" : "wav" }
}
