import SwiftUI

// Brand palette (from the Android app's ui/theme/Color.kt) + adaptive light/dark surfaces.
extension Color {
    static let brandGreen = Color(red: 46 / 255, green: 125 / 255, blue: 50 / 255)        // #2E7D32
    static let brandGreenDark = Color(red: 27 / 255, green: 94 / 255, blue: 32 / 255)     // #1B5E20
    static let brandGold = Color(red: 184 / 255, green: 134 / 255, blue: 11 / 255)        // #B8860B
    static let brandGoldLight = Color(red: 212 / 255, green: 175 / 255, blue: 55 / 255)   // #D4AF37

    // Mirror Android: light = grey page + white cards · dark = black page + dark cards (auto-adapting).
    static let pageBackground = Color(.systemGroupedBackground)
    static let cardBackground = Color(.secondarySystemGroupedBackground)
    static let primaryText = Color(.label)
}
