import SwiftUI
import Shared

// "Kalender" (spec section 3): month navigation, a 7-column header (date + 6 times incl. sunrise),
// one row per day, today highlighted green (alpha 0.14, bold green), auto-scrolls to today.
struct CalendarView: View {
    @State private var monthOffset = 0
    @State private var flash = false
    @StateObject private var store = PrayerStore()
    private let pulseStep = 0.45

    private var monthDate: Date {
        Calendar.current.date(byAdding: .month, value: monthOffset, to: Date()) ?? Date()
    }
    private var year: Int { Calendar.current.component(.year, from: monthDate) }
    private var month: Int { Calendar.current.component(.month, from: monthDate) }
    private var monthLabel: String {
        let f = DateFormatter(); f.locale = Locale(identifier: Localization.shared.lang); f.dateFormat = "LLLL yyyy"
        return f.string(from: monthDate)
    }
    // Per-prayer calibration (official − local), applied so the month lines up with vaktija.eu.
    private func calib(_ i: Int) -> Int { store.calibration.indices.contains(i) ? store.calibration[i] : 0 }
    private func adj(_ hhmm: String, _ offsetMin: Int) -> String {
        let p = hhmm.split(separator: ":")
        guard p.count == 2, let h = Int(p[0]), let m = Int(p[1]) else { return hhmm }
        let total = (((h * 60 + m + offsetMin) % 1440) + 1440) % 1440
        return String(format: "%02d:%02d", total / 60, total % 60)
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
                // Nur sichtbar, wenn man woanders ist — im laufenden Monat wäre der Knopf
                // wirkungslos und würde die Überschrift nur aus der Mitte schieben.
                if monthOffset != 0 {
                    Button(L("calendar_today")) { monthOffset = 0 }
                        .font(.inter(15, .semibold)).foregroundColor(.brandGreen)
                }
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
                .onAppear { jumpToToday(proxy) }
            }
        }
        .background(Color.appBackground.ignoresSafeArea())
        .task { await store.refresh() }
    }

    /// Opening the tab jumps to today, pulses its row a few times, and leaves it highlighted.
    private func jumpToToday(_ proxy: ScrollViewProxy) {
        guard let today = days.first(where: { $0.isToday }) else { return }
        withAnimation { proxy.scrollTo(Int(today.day), anchor: .center) }
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.35) {
            withAnimation(.easeInOut(duration: pulseStep).repeatCount(4, autoreverses: true)) { flash = true }
            DispatchQueue.main.asyncAfter(deadline: .now() + pulseStep * 4) { flash = false }
        }
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
                Text(adj(d.fajr, calib(0))); Text(adj(d.sunrise, calib(1))); Text(adj(d.dhuhr, calib(2)))
                Text(adj(d.asr, calib(3))); Text(adj(d.maghrib, calib(4))); Text(adj(d.isha, calib(5)))
            }
            .font(.inter(13, weight)).foregroundColor(textColor).monospacedDigit().frame(maxWidth: .infinity)
        }
        .padding(.horizontal, 8).padding(.vertical, 8)
        .background(d.isToday ? Color.appPrimary.opacity(flash ? 0.42 : 0.14) : Color.clear)
    }
}
