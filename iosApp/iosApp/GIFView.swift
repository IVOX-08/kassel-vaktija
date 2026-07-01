import SwiftUI
import ImageIO
import UIKit

// SwiftUI has no native animated-GIF view, so we decode the GIF frames with ImageIO and hand them
// to a UIImageView as an animated UIImage. Used for the waving-flag GIFs in the language picker.
struct GIFView: UIViewRepresentable {
    let name: String // file name without extension, bundled under the "flags" folder

    func makeUIView(context: Context) -> UIImageView {
        let iv = UIImageView()
        iv.contentMode = .scaleAspectFill
        iv.clipsToBounds = true
        // Let SwiftUI's frame drive the size instead of the image's intrinsic size.
        iv.setContentHuggingPriority(.defaultLow, for: .horizontal)
        iv.setContentHuggingPriority(.defaultLow, for: .vertical)
        iv.setContentCompressionResistancePriority(.defaultLow, for: .horizontal)
        iv.setContentCompressionResistancePriority(.defaultLow, for: .vertical)
        iv.image = GIFView.animatedImage(named: name)
        return iv
    }

    func updateUIView(_ uiView: UIImageView, context: Context) {}

    static func animatedImage(named: String) -> UIImage? {
        guard let url = Bundle.main.url(forResource: named, withExtension: "gif", subdirectory: "flags"),
              let data = try? Data(contentsOf: url),
              let src = CGImageSourceCreateWithData(data as CFData, nil) else { return nil }
        let count = CGImageSourceGetCount(src)
        var frames: [UIImage] = []
        var duration = 0.0
        for i in 0..<count {
            guard let cg = CGImageSourceCreateImageAtIndex(src, i, nil) else { continue }
            frames.append(UIImage(cgImage: cg))
            duration += frameDelay(src, i)
        }
        guard !frames.isEmpty else { return nil }
        return UIImage.animatedImage(with: frames, duration: duration)
    }

    private static func frameDelay(_ src: CGImageSource, _ i: Int) -> Double {
        guard let props = CGImageSourceCopyPropertiesAtIndex(src, i, nil) as? [CFString: Any],
              let gif = props[kCGImagePropertyGIFDictionary] as? [CFString: Any] else { return 0.1 }
        let unclamped = gif[kCGImagePropertyGIFUnclampedDelayTime] as? Double
        let clamped = gif[kCGImagePropertyGIFDelayTime] as? Double
        let d = unclamped ?? clamped ?? 0.1
        return d < 0.02 ? 0.1 : d
    }
}
