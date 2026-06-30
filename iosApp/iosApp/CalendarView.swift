import SwiftUI
import Shared

// "Kalender" (spec section 3): month navigation, a 7-column header (date + 6 times incl. sunrise),
// one row per day, today highlighted green (alpha 0.14, bold green), auto-scrolls to today.
struct CalendarView: View {
    @State private var monthOffset = 0

    private var monthDate: Date {
        Calendar.current.date(byAdding: .month, value: monthOffset, to: Date()) ?? Date()
    }
    private var year: Int { Calendar.current.component(.year, from: monthDate) }
    private var month: Int { Calendar.current.component(.month, from: monthDate) }
    private var monthLabel: String {
        let f = DateFormatter(); f.locale = Locale(identifier: "de_DE"); f.dateFormat = "LLLL yyyy"
        return f.string(from: monthDate)
    }
    private var days: [CalendarDay] {
        CalendarDataKt.monthForDisplay(year: Int32(year), month: Int32(month))
    }
    private let columns = ["Morg.", "Aufg.", "Mitt.", "Nachm", "Abend", "Nacht"]

    var body: some View {
        VStack(spacing: 0) {
            // Month navigation header
            HStack {
                Button { monthOffset -= 1 } label: { Image(systemName: "chevron.left").font(.system(size: 18, weight: .semibold)) }
                Spacer()
                Text(monthLabel).font(.inter(22, .bold)).foregroundColor(.appPrimary)
                Spacer()
                Button { monthOffset += 1 } label: { Image(systemName: "chevron.right").font(.system(size: 18, weight: .semibold)) }
            }
            .tint(.appPrimary)
            .padding(.horizontal, 16).padding(.vertical, 10)

            // Column header (7 columns: blank date + 6 prayers)
            HStack(spacing: 0) {
                Text("").frame(width: 48)
                ForEach(columns, id: \.self) {
                    Text($0).font(.inter(11, .bold)).foregroundColor(.appSecondary).frame(maxWidth: .infinity)
                }
            }
            .padding(.horizontal, 8).padding(.bottom, 4)
            Divider()

            ScrollViewReader { proxy in
                ScrollView {
                    LazyVStack(spacing: 0) {
                        ForEach(days, id: \.day) { d in
                            dayRow(d).id(Int(d.day))
                            Divider().opacity(0.4)
                        }
                    }
                }
                .onAppear {
                    if let today = days.first(where: { $0.isToday }) {
                        withAnimation { proxy.scrollTo(Int(today.day), anchor: .center) }
                    }
                }
            }
        }
        .background(Color.appBackground.ignoresSafeArea())
    }

    private func dayRow(_ d: CalendarDay) -> some View {
        let textColor = d.isToday ? Color.appPrimary : Color.appOnSurface
        let weight: Font.Weight = d.isToday ? .bold : .regular
        return HStack(spacing: 0) {
            VStack(spacing: 0) {
                Text("\(d.day)").font(.inter(16, d.isToday ? .bold : .semibold)).foregroundColor(textColor)
                Text(d.weekday).font(.inter(10)).foregroundColor(d.isToday ? textColor : .appOnSurfaceVariant)
            }
            .frame(width: 48)
            Group {
                Text(d.fajr); Text(d.sunrise); Text(d.dhuhr); Text(d.asr); Text(d.maghrib); Text(d.isha)
            }
            .font(.inter(13, weight)).foregroundColor(textColor).monospacedDigit().frame(maxWidth: .infinity)
        }
        .padding(.horizontal, 8).padding(.vertical, 8)
        .background(d.isToday ? Color.appPrimary.opacity(0.14) : Color.clear)
    }
}
