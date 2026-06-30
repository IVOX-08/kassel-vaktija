import SwiftUI

// The app's bottom tab bar, mirroring the Android navigation:
// Gebetszeiten · Kalender · Nachrichten · Mehr · Einstellungen.
struct MainTabView: View {
    var body: some View {
        TabView {
            ContentView()
                .tabItem { Label("Gebetszeiten", systemImage: "moon.stars.fill") }
            CalendarView()
                .tabItem { Label("Kalender", systemImage: "calendar") }
            NewsView()
                .tabItem { Label("Nachrichten", systemImage: "bell.fill") }
            MoreView()
                .tabItem { Label("Mehr", systemImage: "square.grid.2x2.fill") }
            SettingsView()
                .tabItem { Label("Einstellungen", systemImage: "gearshape.fill") }
        }
        .tint(.brandGreen)
    }
}
