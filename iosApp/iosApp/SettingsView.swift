import SwiftUI

// Settings: design (light/dark — working), notification toggles (stored, wiring later), language.
struct SettingsView: View {
    @AppStorage("appColorScheme") private var appColorScheme = "system"
    @AppStorage("prayerNotifications") private var prayerNotifications = true
    @AppStorage("announcements") private var announcements = true
    @AppStorage("language") private var language = "Deutsch"

    private let languages = ["Bosanski", "Deutsch", "العربية", "Türkçe", "Shqip", "English", "اردو", "Русский"]

    var body: some View {
        NavigationStack {
            Form {
                Section("Design") {
                    Picker("Erscheinungsbild", selection: $appColorScheme) {
                        Text("System").tag("system")
                        Text("Hell").tag("light")
                        Text("Dunkel").tag("dark")
                    }
                }
                Section("Benachrichtigungen") {
                    Toggle("Gebetsbenachrichtigung", isOn: $prayerNotifications)
                    Toggle("Mitteilungen", isOn: $announcements)
                }
                Section("Sprache") {
                    Picker("Sprache", selection: $language) {
                        ForEach(languages, id: \.self) { Text($0).tag($0) }
                    }
                }
                Section {
                    HStack {
                        Text("Version")
                        Spacer()
                        Text("iOS-Fundament").foregroundColor(.secondary)
                    }
                }
            }
            .navigationTitle("Einstellungen")
            .tint(.brandGreen)
        }
    }
}
