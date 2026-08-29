import SwiftUI

// The app's bottom tab bar, mirroring the Android navigation:
// Gebetszeiten · Kalender · Nachrichten · Mehr · Einstellungen.
//
// Die Auswahl haengt an AppRoute, nicht am Reiter selbst: Eine angetippte Benachrichtigung muss
// den Bildschirm oeffnen koennen, um den es geht — die Tracker-Frage den Tracker, eine Mitteilung
// die Mitteilungen. Ohne die gemeinsame Steuerung landet beides auf der Startseite.
struct MainTabView: View {
    @ObservedObject private var route = AppRoute.shared

    var body: some View {
        TabView(selection: $route.tab) {
            ContentView()
                .tabItem { Label(L("nav_dashboard"), systemImage: "moon.stars.fill") }
                .tag(AppRoute.Tab.prayer)
            CalendarView()
                .tabItem { Label(L("nav_calendar"), systemImage: "calendar") }
                .tag(AppRoute.Tab.calendar)
            NewsView()
                .tabItem { Label(L("nav_news"), systemImage: "bell.fill") }
                .tag(AppRoute.Tab.news)
            MoreView()
                .tabItem { Label(L("nav_more"), systemImage: "square.grid.2x2.fill") }
                .tag(AppRoute.Tab.more)
            SettingsView()
                .tabItem { Label(L("nav_settings"), systemImage: "gearshape.fill") }
                .tag(AppRoute.Tab.settings)
        }
        .tint(.brandGreen)
    }
}
