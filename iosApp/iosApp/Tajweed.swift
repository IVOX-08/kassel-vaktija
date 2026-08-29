import SwiftUI

// Die Tedschwid-Regeln, so wie der mitgelieferte Text sie markiert.
//
// Die Markierung kommt MIT dem Text; die App rechnet keine Regel selbst aus. Tedschwid ist eine
// genaue Wissenschaft, und eine geratene Farbe im Koran ist schlimmer als gar keine.
//
// Die Farben sind die, die gedruckte Mushafs seit Jahrzehnten verwenden — wer aus einem farbigen
// Mushaf gelernt hat, erkennt sie ohne Legende, und genau darum geht es beim Einfärben.
enum TajweedRule: Character, CaseIterable {
    /// Hamzat wasl — das Alif, das geschrieben, aber beim Verbinden nicht gesprochen wird.
    case hamzatWasl = "h"
    /// Ein Buchstabe, der geschrieben, aber nicht gesprochen wird.
    case silent = "s"
    /// Das Lam von „al-", das ein Sonnenbuchstabe verschluckt.
    case lamShamsiyyah = "l"
    /// Madd von zwei Zeiten.
    case maddNormal = "n"
    /// Madd von zwei, vier oder sechs Zeiten.
    case maddPermissible = "p"
    /// Madd von vier oder fünf Zeiten.
    case maddObligatory = "o"
    /// Madd von sechs Zeiten.
    case maddNecessary = "m"
    /// Der Nachklang auf einem Qalqalah-Buchstaben.
    case qalqalah = "q"
    /// Nasal, zwei Zeiten gehalten.
    case ghunnah = "g"
    /// Nun sakinah/Tanwin vor bestimmten Buchstaben verborgen.
    case ikhfa = "f"
    /// Dasselbe an den Lippen (Mim sakinah vor Ba).
    case ikhfaShafawi = "c"
    /// Nun sakinah wird vor Ba zu einem Mim.
    case iqlab = "w"
    /// Verschmolzen mit Nasal.
    case idghamGhunnah = "a"
    /// Verschmolzen ohne Nasal.
    case idghamNoGhunnah = "u"
    /// Mim sakinah in ein folgendes Mim verschmolzen.
    case idghamShafawi = "i"
    /// Verschmolzen mit einem Buchstaben derselben Artikulationsstelle.
    case idghamMutajanisayn = "d"
    /// Verschmolzen mit einem Buchstaben einer nahen Stelle.
    case idghamMutaqaribayn = "b"

    var color: Color {
        switch self {
        case .hamzatWasl, .silent, .lamShamsiyyah: return Color(red: 0x9A / 255, green: 0xA0 / 255, blue: 0xA6 / 255)
        case .maddNormal: return Color(red: 0x1E / 255, green: 0x88 / 255, blue: 0xE5 / 255)
        case .maddPermissible: return Color(red: 0x39 / 255, green: 0x49 / 255, blue: 0xAB / 255)
        case .maddObligatory: return Color(red: 0x6A / 255, green: 0x1B / 255, blue: 0x9A / 255)
        case .maddNecessary: return Color(red: 0xC6 / 255, green: 0x28 / 255, blue: 0x28 / 255)
        case .qalqalah: return Color(red: 0x00 / 255, green: 0x89 / 255, blue: 0x7B / 255)
        case .ghunnah: return Color(red: 0xE6 / 255, green: 0x51 / 255, blue: 0x00 / 255)
        case .ikhfa, .ikhfaShafawi: return Color(red: 0x7B / 255, green: 0x1F / 255, blue: 0xA2 / 255)
        case .iqlab: return Color(red: 0x00 / 255, green: 0x83 / 255, blue: 0x8F / 255)
        case .idghamGhunnah, .idghamShafawi: return Color(red: 0xAD / 255, green: 0x14 / 255, blue: 0x57 / 255)
        case .idghamNoGhunnah: return Color(red: 0x8D / 255, green: 0x6E / 255, blue: 0x63 / 255)
        case .idghamMutajanisayn, .idghamMutaqaribayn: return Color(red: 0x5D / 255, green: 0x40 / 255, blue: 0x37 / 255)
        }
    }
}

enum Tajweed {
    /// Macht aus dem markierten Text farbigen Text.
    ///
    /// Die Markierung sieht so aus: `بِسْمِ [h:1[ٱ]للَّهِ` — eine öffnende `[`, der Buchstabe der
    /// Regel, ein optionales `:index`, noch eine `[`, die betroffenen Buchstaben und eine
    /// schließende `]`.
    ///
    /// Zwei Dinge sind hier kein Feinschliff, sondern die ganze Sache:
    ///
    /// **Gelesen wird auf der Ebene der Unicode-Zeichen**, nicht der Swift-Buchstaben. Swift fasst
    /// eine `[`, auf die unmittelbar ein arabisches Beizeichen folgt (`[` + Fatha in `[o[َآ]`), zu
    /// EINEM Buchstaben zusammen. Eine Suche nach `[` findet diese Klammer dann nicht mehr — die
    /// Markierung bleibt stehen und landet mitten im Korantext auf dem Bildschirm.
    ///
    /// **Markierungen stecken ineinander.** In 2:190 steht `[o[ُوٓ[s[اْ]‌ۚ]`: eine Madd-Regel, und
    /// darin ein stummer Buchstabe. Wer die erste schließende Klammer für das Ende hält, färbt die
    /// innere Markierung als Text ein und lässt ihre Zeichen stehen. Deshalb wird die passende
    /// Klammer gesucht und der Inhalt erneut gelesen — die innere Regel gewinnt für ihre
    /// Buchstaben, die äußere gilt für den Rest.
    ///
    /// Was nicht erkannt wird, bleibt als schlichter Text stehen statt verworfen zu werden: Ein
    /// Koran darf niemals einen Buchstaben an einen Lesefehler verlieren.
    static func attributed(_ marked: String, base: Color) -> AttributedString {
        let scalars = Array(marked.unicodeScalars)
        var out = AttributedString()

        func text(_ range: Range<Int>) -> String {
            var view = String.UnicodeScalarView()
            for index in range { view.append(scalars[index]) }
            return String(view)
        }

        func append(_ range: Range<Int>, _ color: Color) {
            guard !range.isEmpty else { return }
            var piece = AttributedString(text(range))
            piece.foregroundColor = color
            out.append(piece)
        }

        /// Liest die Markierung, die bei `start` beginnt: welche Regel, welcher Inhalt, und wo es
        /// danach weitergeht. `nil`, wenn dort keine gültige Markierung steht.
        func marker(at start: Int) -> (rule: TajweedRule, content: Range<Int>, next: Int)? {
            guard start < scalars.count, scalars[start] == "[" else { return nil }
            var head = start + 1
            while head < scalars.count, scalars[head] != "[" { head += 1 }
            guard head < scalars.count, head > start + 1,
                  let rule = TajweedRule(rawValue: Character(scalars[start + 1]))
            else { return nil }

            // Die PASSENDE schließende Klammer: verschachtelte Markierungen werden übersprungen.
            var i = head + 1
            while i < scalars.count {
                if scalars[i] == "]" { return (rule, (head + 1)..<i, i + 1) }
                if scalars[i] == "[", let inner = marker(at: i) { i = inner.next; continue }
                i += 1
            }
            return nil
        }

        /// Gibt einen Abschnitt aus: schlichter Text in `base`, Markierungen in ihrer Farbe.
        func render(_ range: Range<Int>, base: Color) {
            var i = range.lowerBound
            var plainStart = i
            while i < range.upperBound {
                guard scalars[i] == "[", let m = marker(at: i), m.next <= range.upperBound else {
                    i += 1
                    continue
                }
                append(plainStart..<i, base)
                render(m.content, base: m.rule.color)
                i = m.next
                plainStart = i
            }
            append(plainStart..<range.upperBound, base)
        }

        render(0..<scalars.count, base: base)
        return out
    }

    /// Derselbe Text OHNE Markierungen — für das Ausmessen der Seiten und als schlichte Fassung.
    static func plain(_ marked: String) -> String {
        String(attributed(marked, base: .primary).characters)
    }

    /// Ob sich die markierte Fassung restlos lesen ließ.
    ///
    /// In 32:3 stehen in den Daten Klammern, die keine Regel sind: `ٱفْتَرَ[ٮٰ]هُ`. Sie stehen zu
    /// lassen hieße, Klammern mitten in den Korantext zu setzen; sie wegzuwerfen hieße, den
    /// Buchstaben zu verändern. Beides ist falsch — also gilt dort der schlichte Text, dieselbe
    /// Regel wie für eine Ajah ganz ohne markierte Fassung.
    static func isUsable(_ marked: String) -> Bool {
        let read = plain(marked)
        return !read.contains("[") && !read.contains("]")
    }
}
