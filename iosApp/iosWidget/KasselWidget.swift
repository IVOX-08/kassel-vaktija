import WidgetKit
import SwiftUI
import Shared

// Home-screen widget (spec 7): Gemeindezeichen, das nächste Pflichtgebet, ein laufender
// Countdown und die Adhan-Zeit.
//
// Das Widget läuft in einem eigenen Prozess. Alles, was es weiß, kommt aus der App-Gruppe
// (SharedCode/SharedDefaults.swift) — vorher stand hier fest Kassel, fest Deutsch und fest die
// selbst gerechneten Zeiten, während die App längst die offiziellen Zeiten der gewählten
// Gemeinde anzeigte. Auf dem Startbildschirm stand also etwas anderes als in der App.
struct PrayerEntry: TimelineEntry {
    let date: Date
    /// Schlüssel statt fertiger Text: Übersetzt wird beim Zeichnen, in der Sprache, die dann gilt.
    let nameKey: String
    let timeHHmm: String
    let target: Date        // moment of the next prayer (drives the live countdown)
}

struct Provider: TimelineProvider {
    func placeholder(in context: Context) -> PrayerEntry {
        PrayerEntry(date: Date(), nameKey: "prayer_maghrib", timeHHmm: "21:15",
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

    /// Das nächste Gebet aus DEN GELADENEN Zeiten der gewählten Gemeinde.
    ///
    /// Die geteilte Kotlin-Rechnung bleibt als Rückfall — sie rechnet für Kassel und lag beim
    /// Morgengebet um Stunden daneben, taugt also nur, solange noch nie etwas geladen wurde.
    private func currentEntry() -> PrayerEntry {
        let now = Date()
        let cal = Calendar.current
        let today = PrayerStore.times(on: now)
        let tomorrow = PrayerStore.times(on: cal.date(byAdding: .day, value: 1, to: now) ?? now)

        // Der Sonnenaufgang ist kein Gebet und steht deshalb nicht in der Liste.
        let candidates: [(String, Int, Int)] = [
            ("prayer_fajr", today.fajr, 0),
            ("prayer_dhuhr", today.dhuhr, 0),
            ("prayer_asr", today.asr, 0),
            ("prayer_maghrib", today.maghrib, 0),
            ("prayer_isha", today.isha, 0),
            // Nach dem Nachtgebet der Morgen des Folgetags — sonst stünde das Widget nachts leer.
            ("prayer_fajr", tomorrow.fajr, 1),
        ]

        for (key, minutes, dayOffset) in candidates {
            guard let base = cal.date(byAdding: .day, value: dayOffset, to: now) else { continue }
            var c = cal.dateComponents([.year, .month, .day], from: base)
            c.hour = minutes / 60
            c.minute = minutes % 60
            guard let at = cal.date(from: c), at > now else { continue }
            return PrayerEntry(date: now, nameKey: key,
                               timeHHmm: String(format: "%02d:%02d", minutes / 60, minutes % 60),
                               target: at)
        }

        let n = NextPrayerKt.nextPrayerNow()
        return PrayerEntry(date: now, nameKey: Self.key(n.name), timeHHmm: n.time,
                           target: now.addingTimeInterval(Double(n.inSeconds)))
    }

    private static func key(_ s: String) -> String {
        switch s {
        case "Fajr": return "prayer_fajr"
        case "Dhuhr": return "prayer_dhuhr"
        case "Asr": return "prayer_asr"
        case "Maghrib": return "prayer_maghrib"
        case "Isha": return "prayer_isha"
        default: return s
        }
    }
}

struct KasselWidgetEntryView: View {
    var entry: PrayerEntry
    private let amber = Color(red: 0xE0 / 255.0, green: 0xA9 / 255.0, blue: 0x3A / 255.0) // #E0A93A

    /// Kassel trägt sein eigenes Wappen, jede andere Gemeinde das Zeichen des Verbands — dieselbe
    /// Regel wie auf der Startseite. Kassels Wappen über Nürnbergs Gebetszeiten wäre schlicht
    /// falsch.
    private var isHome: Bool {
        CommunitySelection.communityId == CommunitySelection.fallbackCommunityId
    }

    var body: some View {
        HStack(alignment: .top, spacing: 14) {
            // Weißer Kreis unter beiden Zeichen: Das Widget steht auf dem Hintergrundbild des
            // Nutzers, und ein grünes Wappen auf dunklem Foto verschwindet.
            Image(uiImage: UIImage(named: isHome ? "widget_crest" : "logo_igbd") ?? UIImage())
                .resizable().scaledToFit()
                .padding(isHome ? 0 : 8)
                // Gleich groß für alle. Ein kleineres Zeichen für die anderen läse sich wie
                // zweite Klasse.
                .frame(width: 96, height: 96)
                .background(Circle().fill(Color.white))

            VStack(alignment: .leading, spacing: 0) {
                Text(L(entry.nameKey))
                    .font(.system(size: 20, weight: .bold)).foregroundColor(amber)
                    .lineLimit(1).shadow(radius: 2)
                Text(timerInterval: Date()...max(entry.target, Date().addingTimeInterval(1)), countsDown: true)
                    .font(.system(size: 48, weight: .bold)).foregroundColor(.white)
                    .lineLimit(1).minimumScaleFactor(0.5)
                Text("\(L("widget_remaining"))  ·  \(entry.timeHHmm)")
                    .font(.system(size: 16)).foregroundColor(.white)
                    .padding(.top, 2)
            }
            Spacer(minLength: 0)
        }
        .overlay(alignment: .bottomLeading) { tracker }
        .padding(8)
        .environment(\.layoutDirection, Localization.shared.layoutDirection)
        .widgetBackgroundClear()
    }

    /// Flamme und Tagesstand — die einzige Zahl, die man mehrmals am Tag sehen will, ohne die App
    /// zu öffnen. Links unter dem Wappen, damit sie den Countdown nicht bedrängt.
    ///
    /// Antworten lässt sich hier NICHT: Knöpfe in einem Widget gibt es erst ab iOS 17, und die App
    /// läuft ab iOS 16. Gefragt wird in der Benachrichtigung, dort sind Ja und Nein.
    private var tracker: some View {
        HStack(spacing: 6) {
            Text("🔥").font(.system(size: 17))
            Text("\(PrayerTracker.streak())")
                .font(.system(size: 17, weight: .bold)).foregroundColor(.white)
            Text("·").font(.system(size: 17)).foregroundColor(.white.opacity(0.7))
            Text("\(PrayerTracker.answeredCount()) / \(TrackedPrayer.allCases.count)")
                .font(.system(size: 17, weight: .medium)).foregroundColor(.white)
        }
        .shadow(radius: 2)
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
