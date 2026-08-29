import SwiftUI
import WebKit

// Die Karte unter dem Text einer Mitteilung, wenn ein Video- oder Beitragslink darin steht.
//
// YouTube spielt IN der App: Vorschaubild antippen, das Video läuft, ohne dass jemand die App
// verlässt. Instagram und Facebook öffnen ihre eigene App — beide zeigen fremden Besuchern
// inzwischen eine Anmeldewand, ein eingebauter Abspieler zeigte dort nur ein Anmeldefenster.
struct SocialLinkCard: View {
    let link: SocialLink
    @State private var playing = false
    @Environment(\.openURL) private var openURL

    var body: some View {
        Button {
            if case .youtube = link { playing = true } else { openURL(link.url) }
        } label: {
            VStack(alignment: .leading, spacing: 0) {
                if let thumbnail = link.thumbnail {
                    AsyncImage(url: thumbnail) { phase in
                        switch phase {
                        case .success(let image):
                            image.resizable().aspectRatio(16 / 9, contentMode: .fill)
                        default:
                            // Beim Laden und beim Fehlschlag dieselbe Fläche: Ohne feste Höhe
                            // springt die ganze Liste, sobald ein Bild ankommt.
                            Rectangle().fill(Color.appSurfaceVariant).aspectRatio(16 / 9, contentMode: .fill)
                        }
                    }
                    .frame(maxWidth: .infinity)
                    .clipped()
                    .overlay(playBadge)
                }
                HStack(spacing: 10) {
                    Image(systemName: icon).font(.system(size: 18)).foregroundColor(.brandGreen)
                    VStack(alignment: .leading, spacing: 1) {
                        Text(link.platform).font(.inter(15, .semibold)).foregroundColor(.appOnSurface)
                        // Die vollständige Adresse steht dabei. Wer einem Link folgt, soll vorher
                        // sehen, wohin er führt.
                        Text(link.url.absoluteString)
                            .font(.inter(12)).foregroundColor(.appOnSurfaceVariant)
                            .lineLimit(1).truncationMode(.middle)
                    }
                    Spacer(minLength: 0)
                    Image(systemName: "chevron.right")
                        .font(.system(size: 13, weight: .semibold)).foregroundColor(.appOnSurfaceVariant)
                }
                .padding(12)
            }
            .background(Color.appSurfaceVariant)
            .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
        }
        .buttonStyle(.plain)
        .fullScreenCover(isPresented: $playing) {
            if case .youtube(let id, _) = link { YouTubePlayer(videoID: id) }
        }
    }

    private var icon: String {
        switch link {
        case .youtube: return "play.rectangle.fill"
        case .instagram: return "camera.fill"
        case .facebook: return "person.2.fill"
        }
    }

    private var playBadge: some View {
        Image(systemName: "play.circle.fill")
            .font(.system(size: 54))
            .foregroundStyle(.white, Color.black.opacity(0.45))
            .shadow(radius: 6)
    }
}

/// YouTube in der App.
///
/// Über den eingebauten Abspieler von YouTube — nicht über einen eigenen. Ein eigener müsste die
/// Videodatei selbst holen, und das verbietet YouTube; ausserdem käme dann immer die volle
/// Auflösung an, statt der, die ins Netz des Zuschauers passt.
private struct YouTubePlayer: UIViewControllerRepresentable {
    let videoID: String

    func makeUIViewController(context: Context) -> UIViewController { PlayerController(videoID: videoID) }
    func updateUIViewController(_ controller: UIViewController, context: Context) {}

    private final class PlayerController: UIViewController {
        private let videoID: String
        init(videoID: String) { self.videoID = videoID; super.init(nibName: nil, bundle: nil) }
        required init?(coder: NSCoder) { fatalError() }

        override func viewDidLoad() {
            super.viewDidLoad()
            view.backgroundColor = .black

            let config = WKWebViewConfiguration()
            // Ohne das öffnet iOS das Video im Vollbild des Systems und die App verschwindet
            // dahinter — genau das, was der eingebaute Abspieler vermeiden soll.
            config.allowsInlineMediaPlayback = true
            config.mediaTypesRequiringUserActionForPlayback = []

            let web = WKWebView(frame: .zero, configuration: config)
            web.backgroundColor = .black
            web.isOpaque = false
            web.scrollView.isScrollEnabled = false
            web.translatesAutoresizingMaskIntoConstraints = false
            view.addSubview(web)

            let close = UIButton(type: .system)
            close.setImage(UIImage(systemName: "xmark.circle.fill"), for: .normal)
            close.tintColor = .white
            close.addTarget(self, action: #selector(dismissSelf), for: .touchUpInside)
            close.translatesAutoresizingMaskIntoConstraints = false
            view.addSubview(close)

            NSLayoutConstraint.activate([
                web.leadingAnchor.constraint(equalTo: view.leadingAnchor),
                web.trailingAnchor.constraint(equalTo: view.trailingAnchor),
                web.centerYAnchor.constraint(equalTo: view.centerYAnchor),
                web.heightAnchor.constraint(equalTo: web.widthAnchor, multiplier: 9.0 / 16.0),
                close.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor, constant: 12),
                close.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -16),
                close.widthAnchor.constraint(equalToConstant: 34),
                close.heightAnchor.constraint(equalToConstant: 34),
            ])

            // `playsinline` und `rel=0`: im Rahmen bleiben, und danach keine fremden Videos
            // vorschlagen — die Gemeinde hat einen Beitrag geteilt, keine Empfehlungsliste.
            let src = "https://www.youtube-nocookie.com/embed/\(videoID)?playsinline=1&rel=0&autoplay=1"
            if let url = URL(string: src) { web.load(URLRequest(url: url)) }
        }

        @objc private func dismissSelf() { dismiss(animated: true) }
    }
}
