import WidgetKit
import SwiftUI
import Shared

// Home-screen widget (spec 7): community emblem + next mandatory prayer name, a live countdown, and
// the prayer's Adhan time. Next prayer comes from the same shared Kotlin source as the app.
struct PrayerEntry: TimelineEntry {
    let date: Date
    let name: String        // German prayer name
    let timeHHmm: String    // Adhan time "HH:mm"
    let target: Date        // moment of the next prayer (drives the live countdown)
}

struct Provider: TimelineProvider {
    func placeholder(in context: Context) -> PrayerEntry {
        PrayerEntry(date: Date(), name: "Abendgebet", timeHHmm: "21:15",
                    target: Date().addingTimeInterval(3600))
    }

    func getSnapshot(in context: Context, completion: @escaping (PrayerEntry) -> Void) {
        completion(currentEntry())
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<PrayerEntry>) -> Void) {
        let entry = currentEntry()
        // Reload just after this prayer passes so the widget rolls over to the next one.
        let reload = entry.target.addingTimeInterval(2)
        completion(Timeline(entries: [entry], policy: .after(reload)))
    }

    private func currentEntry() -> PrayerEntry {
        let n = NextPrayerKt.nextPrayerNow()
        return PrayerEntry(date: Date(), name: germanName(n.name), timeHHmm: n.time,
                           target: Date().addingTimeInterval(Double(n.inSeconds)))
    }

    private func germanName(_ s: String) -> String {
        switch s {
        case "Fajr": return "Morgengebet"
        case "Dhuhr": return "Mittagsgebet"
        case "Asr": return "Nachmittagsgebet"
        case "Maghrib": return "Abendgebet"
        case "Isha": return "Nachtgebet"
        default: return s
        }
    }
}

struct KasselWidgetEntryView: View {
    var entry: PrayerEntry
    private let amber = Color(red: 0xE0 / 255.0, green: 0xA9 / 255.0, blue: 0x3A / 255.0) // #E0A93A

    var body: some View {
        HStack(spacing: 14) {
            Image(uiImage: UIImage(named: "widget_crest") ?? UIImage())
                .resizable().scaledToFit()
                .frame(width: 96, height: 96)
                .background(Circle().fill(Color.white))
            VStack(alignment: .leading, spacing: 2) {
                Text(entry.name)
                    .font(.system(size: 20, weight: .bold)).foregroundColor(amber)
                    .lineLimit(1).shadow(radius: 2)
                Text(timerInterval: Date()...max(entry.target, Date().addingTimeInterval(1)), countsDown: true)
                    .font(.system(size: 48, weight: .bold)).foregroundColor(.white)
                    .lineLimit(1).minimumScaleFactor(0.5)
                Text("verbleibend  ·  \(entry.timeHHmm)")
                    .font(.system(size: 16)).foregroundColor(.white)
                    .padding(.top, 2)
            }
            Spacer(minLength: 0)
        }
        .padding(8)
        .widgetBackgroundClear()
    }
}

private extension View {
    // Transparent widget background (spec 7). iOS 17+ requires an explicit container background.
    @ViewBuilder func widgetBackgroundClear() -> some View {
        if #available(iOS 17.0, *) { containerBackground(.clear, for: .widget) } else { self }
    }
}

struct KasselWidget: Widget {
    let kind = "KasselWidget"
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: Provider()) { entry in
            KasselWidgetEntryView(entry: entry)
        }
        .configurationDisplayName("Kassel Vaktija")
        .description("Nächste Gebetszeit mit Countdown.")
        .supportedFamilies([.systemMedium, .systemSmall])
    }
}

@main
struct KasselWidgetBundle: WidgetBundle {
    var body: some Widget { KasselWidget() }
}
