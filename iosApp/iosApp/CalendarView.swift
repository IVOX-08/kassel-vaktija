import SwiftUI
import Shared

// Month calendar of prayer times — powered by the shared Kotlin monthForDisplay().
struct CalendarView: View {
    @State private var monthOffset = 0

    private var monthDate: Date {
        Calendar.current.date(byAdding: .month, value: monthOffset, to: Date()) ?? Date()
    }
    private var year: Int { Calendar.current.component(.year, from: monthDate) }
    private var month: Int { Calendar.current.component(.month, from: monthDate) }
    private var monthLabel: String {
        let f = DateFormatter(); f.locale = Locale(identifier: "de_DE"); f.dateFormat = "MMMM yyyy"
        return f.string(from: monthDate)
    }
    private var days: [CalendarDay] {
        CalendarDataKt.monthForDisplay(year: Int32(year), month: Int32(month))
    }

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                HStack {
                    Button { monthOffset -= 1 } label: { Image(systemName: "chevron.left") }
                    Spacer()
                    Text(monthLabel).font(.headline)
                    Spacer()
                    Button { monthOffset += 1 } label: { Image(systemName: "chevron.right") }
                }
                .tint(.brandGreen)
                .padding(.horizontal).padding(.vertical, 10)

                List {
                    HStack {
                        Text("Tag").frame(width: 52, alignment: .leading)
                        ForEach(["Fajr", "Dhuhr", "Asr", "Magrb", "Isha"], id: \.self) {
                            Text($0).frame(maxWidth: .infinity)
                        }
                    }
                    .font(.caption2).foregroundColor(.secondary)

                    ForEach(days, id: \.day) { d in
                        HStack {
                            Text("\(d.weekday) \(d.day)")
                                .font(.subheadline)
                                .fontWeight(d.isToday ? .bold : .regular)
                                .foregroundColor(d.isToday ? .brandGreen : .primaryText)
                                .frame(width: 52, alignment: .leading)
                            Group {
                                Text(d.fajr); Text(d.dhuhr); Text(d.asr); Text(d.maghrib); Text(d.isha)
                            }
                            .font(.caption).monospacedDigit().frame(maxWidth: .infinity)
                        }
                        .listRowBackground(d.isToday ? Color.brandGreen.opacity(0.14) : Color.cardBackground)
                    }
                }
                .listStyle(.plain)
            }
            .navigationTitle("Kalender")
            .navigationBarTitleDisplayMode(.inline)
        }
    }
}
