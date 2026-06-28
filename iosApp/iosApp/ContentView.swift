import SwiftUI
import Shared

// The iOS foundation screen: shows today's Kassel prayer times, computed entirely by the shared
// Kotlin module (PrayerTimesCalculator → prayerRowsForToday). Proves the Kotlin↔Swift bridge works.
struct ContentView: View {
    private let rows: [PrayerRow] = PrayerRowsKt.prayerRowsForToday()
    private let platform: String = PlatformKt.platformName()

    var body: some View {
        NavigationView {
            List {
                Section {
                    ForEach(rows, id: \.name) { row in
                        HStack {
                            Text(row.name)
                            Spacer()
                            Text(row.time)
                                .monospacedDigit()
                                .foregroundColor(.secondary)
                        }
                    }
                } header: {
                    Text("Kassel — heute")
                } footer: {
                    Text("Berechnet von gemeinsamem Kotlin-Code · läuft auf \(platform)")
                }
            }
            .navigationTitle("Vaktija")
        }
    }
}
