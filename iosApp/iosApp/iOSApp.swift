import SwiftUI

@main
struct iOSApp: App {
    @AppStorage("appColorScheme") private var appColorScheme = "system"
    @AppStorage("onboarding_done") private var onboardingDone = false

    var body: some Scene {
        WindowGroup {
            MainTabView()
                .preferredColorScheme(resolvedScheme)
                .fullScreenCover(isPresented: .init(
                    get: { !onboardingDone },
                    set: { shown in if !shown { onboardingDone = true } }
                )) {
                    OnboardingView(onDone: { onboardingDone = true })
                }
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
