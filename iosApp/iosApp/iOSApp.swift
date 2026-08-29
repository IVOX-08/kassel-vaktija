import SwiftUI
import FirebaseCore

@main
struct iOSApp: App {
    // `store:` ausdruecklich: `.defaultAppStorage` unten wirkt nur auf die Ansichten DARUNTER,
    // nicht auf die App selbst. Ohne die Angabe laese die App hier den alten, eigenen Speicher —
    // waehrend das Onboarding sein „fertig" in die Gruppe schreibt. Ergebnis: Die Auswahl kaeme
    // bei jedem Start wieder.
    @AppStorage("appColorScheme", store: AppGroup.defaults) private var appColorScheme = "system"
    @AppStorage("onboarding_done", store: AppGroup.defaults) private var onboardingDone = false
    @StateObject private var loc = Localization.shared
    // Push needs the UIKit delegate callbacks (APNs token, silent push) — SwiftUI has no
    // equivalent. See PushService.swift.
    @UIApplicationDelegateAdaptor(AppDelegate.self) private var appDelegate

    init() {
        // Zuerst: den alten, app-eigenen Speicher in die App-Gruppe holen. Muss vor allem anderen
        // laufen — jede Zeile darunter liest schon aus der Gruppe, und ohne den Umzug stuenden
        // bestehende Nutzer nach dem Update wieder vor der Gemeindeauswahl.
        AppGroup.migrateOnce()
        // Same Firebase project as Android — announcements and community settings come from there.
        // Guarded so a missing GoogleService-Info.plist degrades to "no news" instead of a crash.
        if Bundle.main.url(forResource: "GoogleService-Info", withExtension: "plist") != nil {
            FirebaseApp.configure()
        }
        // Frisch geladene Zeiten muessen die geplanten Meldungen nachziehen. Der gemeinsame Code
        // kennt den Planer nicht (das Widget soll ihn nicht mitschleppen), deshalb wird er hier
        // eingehaengt — an EINER Stelle, damit Vorder- und Hintergrundweg nicht auseinanderlaufen.
        PrayerStore.onTimesLoaded = { times in
            await NotificationScheduler.reschedule(times: times)
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
            // Alle @AppStorage-Werte in den geteilten Speicher — sonst schreibt die App in ihren
            // eigenen und das Widget liest nebenan ins Leere.
            .defaultAppStorage(AppGroup.defaults)
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
