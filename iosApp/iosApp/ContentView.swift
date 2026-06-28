import SwiftUI
import Shared

// The iOS foundation screen: a "next prayer" header + today's Kassel prayer times, all computed by
// the shared Kotlin module (PrayerTimesCalculator, nextPrayerNow, prayerRowsForToday).
struct ContentView: View {
    private let rows: [PrayerRow] = PrayerRowsKt.prayerRowsForToday()
    private let next: NextPrayerInfo = NextPrayerKt.nextPrayerNow()
    private let platform: String = PlatformKt.platformName()

    var body: some View {
        NavigationView {
            List {
                Section {
                    VStack(alignment: .leading, spacing: 4) {
                        Text("Nächstes Gebet")
                            .font(.caption)
                            .foregroundColor(.secondary)
                        Text("\(next.name) · \(next.time)")
                            .font(.title2).bold()
                        Text("in \(next.inMinutes) Min")
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                    }
                    .padding(.vertical, 4)
                }

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
