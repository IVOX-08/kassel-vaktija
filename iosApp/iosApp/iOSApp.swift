import SwiftUI
import FirebaseCore

@main
struct iOSApp: App {
    @AppStorage("appColorScheme") private var appColorScheme = "system"
    @AppStorage("onboarding_done") private var onboardingDone = false
    @StateObject private var loc = Localization.shared
    // Push needs the UIKit delegate callbacks (APNs token, silent push) — SwiftUI has no
    // equivalent. See PushService.swift.
    @UIApplicationDelegateAdaptor(AppDelegate.self) private var appDelegate

    init() {
        // Same Firebase project as Android — announcements and community settings come from there.
        // Guarded so a missing GoogleService-Info.plist degrades to "no news" instead of a crash.
        if Bundle.main.url(forResource: "GoogleService-Info", withExtension: "plist") != nil {
            FirebaseApp.configure()
        }
        // Show Adhan/prayer notifications even while the app is open.
        NotificationPresenter.shared.install()
        // Green navigation-bar titles throughout (matches the Android branded headers).
        let green = UIColor(Color.appPrimary)
        let standard = UINavigationBarAppearance()
        standard.configureWithDefaultBackground()
        standard.largeTitleTextAttributes = [.foregroundColor: green]
        standard.titleTextAttributes = [.foregroundColor: green]
        let edge = UINavigationBarAppearance()
        edge.configureWithTransparentBackground()
        edge.largeTitleTextAttributes = [.foregroundColor: green]
        edge.titleTextAttributes = [.foregroundColor: green]
        UINavigationBar.appearance().standardAppearance = standard
        UINavigationBar.appearance().scrollEdgeAppearance = edge
        UINavigationBar.appearance().compactAppearance = standard
    }

    var body: some Scene {
        WindowGroup {
            Group {
                if onboardingDone {
                    MainTabView()
                } else {
                    OnboardingView(onDone: { onboardingDone = true })
                }
            }
            .environmentObject(loc)
            .environment(\.layoutDirection, loc.layoutDirection)
            .id(loc.lang) // rebuild the whole tree when the language switches (incl. RTL)
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
