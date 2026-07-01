import SwiftUI

@main
struct iOSApp: App {
    @AppStorage("appColorScheme") private var appColorScheme = "system"
    @AppStorage("onboarding_done") private var onboardingDone = false

    var body: some Scene {
        WindowGroup {
            Group {
                if onboardingDone {
                    MainTabView()
                } else {
                    OnboardingView(onDone: { onboardingDone = true })
                }
            }
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
