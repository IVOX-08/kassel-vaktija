import SwiftUI

// The app's bottom tab bar, mirroring the Android navigation:
// Gebetszeiten · Kalender · Nachrichten · Mehr · Einstellungen.
struct MainTabView: View {
    var body: some View {
        TabView {
            ContentView()
                .tabItem { Label(L("nav_dashboard"), systemImage: "moon.stars.fill") }
            CalendarView()
                .tabItem { Label(L("nav_calendar"), systemImage: "calendar") }
            NewsView()
                .tabItem { Label(L("nav_news"), systemImage: "bell.fill") }
            MoreView()
                .tabItem { Label(L("nav_more"), systemImage: "square.grid.2x2.fill") }
            SettingsView()
                .tabItem { Label(L("nav_settings"), systemImage: "gearshape.fill") }
        }
        .tint(.brandGreen)
    }
}
