import SwiftUI

// Die Zeichen für Instagram, Facebook und YouTube.
//
// Schlichte einfarbige Glyphen in den Farben der App — NICHT die farbigen Markenlogos und nicht
// als solche ausgegeben. Die echten Logos der drei Netze dürfen nicht nachgezeichnet werden, und
// eine schlechte Kopie wäre ohnehin schlechter als ein klares Zeichen.
//
// Die Pfaddaten sind Zeichen für Zeichen dieselben wie auf Android (`res/drawable/ic_*.xml`).
// Nachzuzeichnen hätte zwei Apps mit zwei verschiedenen Symbolen ergeben.
enum SocialIcon {
    /// Der abgerundete Rahmen, die Linse und der Punkt.
    static let instagram = [
        "M7,2h10c2.76,0 5,2.24 5,5v10c0,2.76 -2.24,5 -5,5H7c-2.76,0 -5,-2.24 -5,-5V7c0,-2.76 2.24,-5 5,-5zM7,4C5.35,4 4,5.35 4,7v10c0,1.65 1.35,3 3,3h10c1.65,0 3,-1.35 3,-3V7c0,-1.65 -1.35,-3 -3,-3H7z",
        "M12,7c2.76,0 5,2.24 5,5s-2.24,5 -5,5 -5,-2.24 -5,-5 2.24,-5 5,-5zM12,9c-1.65,0 -3,1.35 -3,3s1.35,3 3,3 3,-1.35 3,-3 -1.35,-3 -3,-3z",
    ]

    /// Die Buchstabenform im abgerundeten Rahmen.
    static let facebook = [
        "M13.5,22v-8h2.7l0.4,-3.1h-3.1V8.9c0,-0.9 0.25,-1.5 1.55,-1.5h1.65V4.6C16.4,4.55 15.5,4.5 14.45,4.5c-2.2,0 -3.7,1.35 -3.7,3.8v2.6H8v3.1h2.75v8z",
        "M7,2h10c2.76,0 5,2.24 5,5v10c0,2.76 -2.24,5 -5,5H7c-2.76,0 -5,-2.24 -5,-5V7c0,-2.76 2.24,-5 5,-5zM7,4C5.35,4 4,5.35 4,7v10c0,1.65 1.35,3 3,3h10c1.65,0 3,-1.35 3,-3V7c0,-1.65 -1.35,-3 -3,-3H7z",
    ]

    /// Der abgerundete Rahmen mit dem Abspiel-Dreieck.
    static let youtube = [
        "M21.6,7.2c-0.23,-0.86 -0.9,-1.53 -1.76,-1.76C18.25,5 12,5 12,5s-6.25,0 -7.84,0.44C3.3,5.67 2.63,6.34 2.4,7.2 2,8.79 2,12 2,12s0,3.21 0.4,4.8c0.23,0.86 0.9,1.53 1.76,1.76C5.75,19 12,19 12,19s6.25,0 7.84,-0.44c0.86,-0.23 1.53,-0.9 1.76,-1.76C22,15.21 22,12 22,12s0,-3.21 -0.4,-4.8zM4.33,7.7c0.1,-0.37 0.38,-0.65 0.75,-0.75C6.2,6.65 10.4,6.6 12,6.6s5.8,0.05 6.92,0.35c0.37,0.1 0.65,0.38 0.75,0.75 0.28,1.06 0.33,3.35 0.33,4.3s-0.05,3.24 -0.33,4.3c-0.1,0.37 -0.38,0.65 -0.75,0.75 -1.12,0.3 -5.32,0.35 -6.92,0.35s-5.8,-0.05 -6.92,-0.35c-0.37,-0.1 -0.65,-0.38 -0.75,-0.75C4.05,15.24 4,12.95 4,12s0.05,-3.24 0.33,-4.3z",
        "M10.25,9.15l4.75,2.85 -4.75,2.85z",
    ]
}

/// Zeichnet die Pfade eines 24×24-Zeichens in der gegebenen Fläche.
///
/// Der Punkt über der Instagram-Linse wird als Kreis gezeichnet statt als Bogen: Android schreibt
/// ihn als zwei Halbbögen, und ein Bogen-Befehl im Pfadleser wäre viel Aufwand für einen Punkt,
/// der geometrisch nichts anderes ist als ein Kreis.
struct SocialIconShape: Shape {
    let paths: [String]
    /// Mittelpunkt und Radius eines zusätzlichen Kreises, im 24er-Raster.
    var dot: (x: CGFloat, y: CGFloat, r: CGFloat)?

    func path(in rect: CGRect) -> Path {
        let scale = min(rect.width, rect.height) / 24
        var out = Path()
        for data in paths { out.addPath(SVGPath.parse(data)) }
        if let dot {
            out.addEllipse(in: CGRect(x: dot.x - dot.r, y: dot.y - dot.r,
                                      width: dot.r * 2, height: dot.r * 2))
        }
        return out.applying(CGAffineTransform(scaleX: scale, y: scale))
    }
}

/// Ein kleiner Leser für Pfaddaten, wie sie in SVG und in Androids Vektordateien stehen.
///
/// Nur die Befehle, die in diesen drei Zeichen vorkommen. Was er nicht kennt, überspringt er —
/// hier ist das kein Risiko: Die Pfade sind fest einkompiliert und ändern sich nie.
enum SVGPath {
    static func parse(_ data: String) -> Path {
        var path = Path()
        var current = CGPoint.zero
        var start = CGPoint.zero
        /// Der letzte Kontrollpunkt — `s` und `S` spiegeln ihn.
        var lastControl: CGPoint?
        var command: Character = "M"
        var numbers: [CGFloat] = []
        var index = data.startIndex

        func flush() {
            guard !numbers.isEmpty || command == "z" || command == "Z" else { return }
            var i = 0
            func next() -> CGFloat { defer { i += 1 }; return i < numbers.count ? numbers[i] : 0 }
            let relative = command.isLowercase

            while true {
                switch command {
                case "M", "m":
                    let p = CGPoint(x: next(), y: next())
                    current = relative ? CGPoint(x: current.x + p.x, y: current.y + p.y) : p
                    path.move(to: current)
                    start = current
                    // Weitere Paare nach einem M zählen als L.
                    command = relative ? "l" : "L"
                    lastControl = nil
                case "L", "l":
                    let p = CGPoint(x: next(), y: next())
                    current = relative ? CGPoint(x: current.x + p.x, y: current.y + p.y) : p
                    path.addLine(to: current)
                    lastControl = nil
                case "H", "h":
                    let x = next()
                    current = CGPoint(x: relative ? current.x + x : x, y: current.y)
                    path.addLine(to: current)
                    lastControl = nil
                case "V", "v":
                    let y = next()
                    current = CGPoint(x: current.x, y: relative ? current.y + y : y)
                    path.addLine(to: current)
                    lastControl = nil
                case "C", "c":
                    let base = relative ? current : .zero
                    let c1 = CGPoint(x: base.x + next(), y: base.y + next())
                    let c2 = CGPoint(x: base.x + next(), y: base.y + next())
                    let end = CGPoint(x: base.x + next(), y: base.y + next())
                    path.addCurve(to: end, control1: c1, control2: c2)
                    lastControl = c2
                    current = end
                case "S", "s":
                    let base = relative ? current : .zero
                    // Ohne vorherige Kurve ist der erste Kontrollpunkt der aktuelle Punkt.
                    let c1 = lastControl.map {
                        CGPoint(x: 2 * current.x - $0.x, y: 2 * current.y - $0.y)
                    } ?? current
                    let c2 = CGPoint(x: base.x + next(), y: base.y + next())
                    let end = CGPoint(x: base.x + next(), y: base.y + next())
                    path.addCurve(to: end, control1: c1, control2: c2)
                    lastControl = c2
                    current = end
                case "Z", "z":
                    path.closeSubpath()
                    current = start
                    lastControl = nil
                default:
                    return
                }
                // Ein Befehl darf mehrere Punktgruppen tragen; sind alle verbraucht, ist Schluss.
                if i >= numbers.count { break }
            }
            numbers.removeAll()
        }

        while index < data.endIndex {
            let c = data[index]
            if c.isLetter {
                flush()
                command = c
                index = data.index(after: index)
                if c == "z" || c == "Z" { flush() }
                continue
            }
            if c == "," || c == " " || c == "\n" {
                index = data.index(after: index)
                continue
            }
            // Eine Zahl: Vorzeichen, Ziffern, ein Punkt.
            var text = ""
            if c == "-" || c == "+" { text.append(c); index = data.index(after: index) }
            while index < data.endIndex, data[index].isNumber || data[index] == "." {
                text.append(data[index])
                index = data.index(after: index)
            }
            if let value = Double(text) { numbers.append(CGFloat(value)) }
        }
        flush()
        return path
    }
}
