import SwiftUI

@main
struct iOSApp: App {
    @AppStorage("appColorScheme") private var appColorScheme = "system"

    var body: some Scene {
        WindowGroup {
            MainTabView()
                .preferredColorScheme(resolvedScheme)
        }
    }

    private var resolvedScheme: ColorScheme? {
        switch appColorScheme {
        case "light": return .light
        case "dark": return .dark
        default: return nil // follow the system
        }
    }
}
