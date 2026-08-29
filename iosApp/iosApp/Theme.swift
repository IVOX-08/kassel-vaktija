import SwiftUI
import UIKit

// MARK: - Colors (EXACT hex values from the spec / Android ui/theme/Color.kt)

extension UIColor {
    convenience init(rgb: UInt) {
        self.init(
            red: CGFloat((rgb >> 16) & 0xFF) / 255,
            green: CGFloat((rgb >> 8) & 0xFF) / 255,
            blue: CGFloat(rgb & 0xFF) / 255,
            alpha: 1
        )
    }
}

private func adaptive(light: UInt, dark: UInt) -> Color {
    Color(UIColor { $0.userInterfaceStyle == .dark ? UIColor(rgb: dark) : UIColor(rgb: light) })
}

extension Color {
    init(rgb: UInt) { self.init(UIColor(rgb: rgb)) }

    // Fixed brand palette
    // IZ zelena, Pantone 356 C — die geschuetzte Farbe aus Kapitel 2.1 des Handbuchs der
    // Islamska zajednica. Vorher stand hier Materials Gruen 800: sah aehnlich aus, war falsch.
    static let brandGreen = Color(rgb: 0x008348)
    static let brandGreenDark = Color(rgb: 0x00612F)
    static let brandGreenLight = Color(rgb: 0x4CB783)

    // Das GOLD DER SCHRIFT bleibt, wie es immer war.
    //
    // Es steht nicht im Markenhandbuch und soll es auch nicht: Das Handbuch regelt das ZEICHEN,
    // nicht die Beschriftung einer App. IZ Zlatna (#A59573) ist ein blasses, graustichiges Gold —
    // als Schriftfarbe naehme es der App das warme Gold, an dem die Tafel in der Moschee aus der
    // Entfernung erkannt wird. Auf der Android-Seite wurde das einmal verwechselt und
    // zurueckgenommen; hier nicht wiederholen.
    static let brandGold = Color(rgb: 0xB8860B)
    static let brandGoldLight = Color(rgb: 0xD4AF37)
    static let qiblaRed = Color(rgb: 0xD32F2F)
    static let deepGreen = Color(rgb: 0x0B3D2E)
    static let teal = Color(rgb: 0x0E6B5C)

    // Material-3-like semantic colors — light/dark adaptive (spec section 1)
    static let appPrimary = adaptive(light: 0x2E7D32, dark: 0x66BB6A)            // green brightens in dark
    static let appSecondary = adaptive(light: 0xB8860B, dark: 0xD4AF37)          // gold brightens in dark
    static let appBackground = adaptive(light: 0xF4F4F4, dark: 0x000000)         // true black in dark (OLED)
    static let appSurface = adaptive(light: 0xFFFFFF, dark: 0x1E1E1E)
    static let appSurfaceVariant = adaptive(light: 0xF1F4F1, dark: 0x2A2F2A)
    static let appOnSurface = adaptive(light: 0x1A1A1A, dark: 0xECECEC)
    static let appOnSurfaceVariant = adaptive(light: 0x5C615C, dark: 0xB6BBB6)
    /// Die Karten der App: WEISS auf hellgrauer Seite, mit einer Haarlinie.
    ///
    /// Vorher lavendel (#E7E0EC). Der Ton stammte aus Materials eigener Grundpalette, weil auf
    /// Android eine Farbrolle nie gesetzt war — auf einem Bildschirm standen dadurch drei Karten
    /// in drei Toenen, einer davon deutlich rosa. Das ist dort behoben; nichts in der App darf
    /// einen Lila- oder Rosastich haben.
    static let moreCard = adaptive(light: 0xFFFFFF, dark: 0x181D19)
    /// Die Haarlinie um die Karten. Ohne sie verschwimmt Weiss auf hellem Grau.
    static let cardOutline = adaptive(light: 0xE4E9E4, dark: 0x272E27)

    // Back-compat aliases used by the existing screens
    static let pageBackground = appBackground
    static let cardBackground = appSurface
    static let primaryText = appOnSurface
}

// MARK: - Inter font (bundled variable font; the spec requires Inter, not the system font)

enum AppFont {
    /// The bundled Inter family name, discovered at runtime — variable-font family names vary
    /// ("Inter", "Inter Variable", "Inter18pt"…), so we find whichever registered name contains "Inter".
    static let interFamily: String = UIFont.familyNames
        .first { $0.range(of: "Inter", options: .caseInsensitive) != nil } ?? "Inter"

    static func font(_ size: CGFloat, _ weight: Font.Weight = .regular) -> Font {
        Font.custom(interFamily, size: size).weight(weight)
    }
}

extension Font {
    /// Inter at the given point size + weight (400/500/600/700 map to regular/medium/semibold/bold).
    static func inter(_ size: CGFloat, _ weight: Font.Weight = .regular) -> Font {
        AppFont.font(size, weight)
    }
}

// MARK: - Corner radii (per component, from the spec section 1)

enum Radius {
    static let hero: CGFloat = 24
    static let prayerCard: CGFloat = 18
    static let smallCard: CGFloat = 14
    static let headerItem: CGFloat = 12
    static let accentBar: CGFloat = 3
    static let quranCard: CGFloat = 20
    static let newsImage: CGFloat = 12
    static let languageCard: CGFloat = 16
}
