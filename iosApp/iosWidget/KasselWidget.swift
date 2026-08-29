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
    /// Das Gebet, dessen Fenster gerade offen und noch unbeantwortet ist — sonst `nil`.
    /// Nur dann stehen Ja und Nein im Widget.
    let asking: TrackedPrayer?
}

struct Provider: TimelineProvider {
    func placeholder(in context: Context) -> PrayerEntry {
        PrayerEntry(date: Date(), nameKey: "prayer_maghrib", timeHHmm: "21:15",
                    target: Date().addingTimeInterval(3600), asking: nil)
    }

    func getSnapshot(in context: Context, completion: @escaping (PrayerEntry) -> Void) {
        completion(currentEntry())
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<PrayerEntry>) -> Void) {
        let entry = currentEntry()
        // Neu zeichnen, sobald dieses Gebet vorbei ist — ODER sobald sich ein Tracker-Fenster
        // oeffnet oder schliesst. Ohne den zweiten Fall erschiene die Frage erst beim naechsten
        // Gebetsruf, also lange nach dem Ikamet, zu dem gefragt werden soll.
        let reload = min(entry.target, Self.nextWindowChange() ?? entry.target)
        completion(Timeline(entries: [entry], policy: .after(reload.addingTimeInterval(2))))
    }

    /// Der naechste Zeitpunkt, an dem sich ein Fenster oeffnet oder schliesst.
    private static func nextWindowChange() -> Date? {
        let now = Date()
        var moments: [Date] = []
        for prayer in TrackedPrayer.allCases {
            guard let w = PrayerTracker.window(prayer) else { continue }
            if w.open > now { moments.append(w.open) }
            if w.close > now { moments.append(w.close) }
        }
        return moments.min()
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
                               target: at, asking: Self.openQuestion())
        }

        let n = NextPrayerKt.nextPrayerNow()
        return PrayerEntry(date: now, nameKey: Self.key(n.name), timeHHmm: n.time,
                           target: now.addingTimeInterval(Double(n.inSeconds)),
                           asking: Self.openQuestion())
    }

    /// Welches Gebet gerade gefragt werden darf: Fenster offen und noch keine Antwort.
    private static func openQuestion() -> TrackedPrayer? {
        TrackedPrayer.allCases.first {
            if case .open = PrayerTracker.state($0) { return true }
            return false
        }
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
    @Environment(\.colorScheme) private var scheme
    private let amber = Color(red: 0xE0 / 255.0, green: 0xA9 / 255.0, blue: 0x3A / 255.0) // #E0A93A

    /// Kassel trägt sein eigenes Wappen, jede andere Gemeinde das Zeichen des Verbands — dieselbe
    /// Regel wie auf der Startseite. Kassels Wappen über Nürnbergs Gebetszeiten wäre schlicht
    /// falsch.
    private var isHome: Bool {
        CommunitySelection.communityId == CommunitySelection.fallbackCommunityId
    }

    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            VStack(spacing: 6) {
                Image(uiImage: UIImage(named: isHome ? "widget_crest"
                                                     : (scheme == .dark ? "logo_igbd_dark" : "logo_igbd")) ?? UIImage())
                    .resizable().scaledToFit()
                    .frame(width: 64, height: 64)
                // Flamme und Tagesstand direkt unter dem Wappen — die einzige Zahl, die man
                // mehrmals am Tag sehen will, ohne die App zu öffnen.
                HStack(spacing: 4) {
                    Text("🔥").font(.system(size: 14))
                    Text("\(PrayerTracker.streak())")
                        .font(.system(size: 14, weight: .bold))
                    Text("·").foregroundStyle(.secondary)
                    Text("\(PrayerTracker.answeredCount()) / \(TrackedPrayer.allCases.count)")
                        .font(.system(size: 14, weight: .medium))
                }
                .foregroundStyle(.primary)
                if !isHome, let name = CommunitySelection.communityName, !name.isEmpty {
                    Text(name)
                        .font(.system(size: 9, weight: .semibold)).foregroundColor(amber)
                        .multilineTextAlignment(.center).lineLimit(2)
                        .frame(maxWidth: 72)
                }
            }

            VStack(alignment: .leading, spacing: 0) {
                // Steht ein Fenster offen, gehoert dem Widget die Frage — der Countdown laeuft
                // ohnehin weiter und ist in dem Moment nicht das Wichtigere.
                if let prayer = entry.asking {
                    question(prayer)
                } else {
                    countdown
                }
            }
            Spacer(minLength: 0)
        }
        .padding(10)
        .environment(\.layoutDirection, Localization.shared.layoutDirection)
        .widgetBackgroundClear()
    }

    /// Die Frage mit Ja und Nein.
    ///
    /// Die Knoepfe loesen eine Absicht aus (AnswerPrayerIntent); ein Widget kann keinen eigenen
    /// Code ausfuehren. Die Antwort wird dort noch einmal gegen das Fenster geprueft — ein Widget
    /// kann lange unangetastet auf dem Bildschirm stehen.
    @ViewBuilder private func question(_ prayer: TrackedPrayer) -> some View {
        Text(String(format: L("tracker_ask_title"), L(prayer.nameKey)))
            .font(.system(size: 17, weight: .bold))
            .foregroundStyle(.primary)
            .lineLimit(2).minimumScaleFactor(0.8)
            .fixedSize(horizontal: false, vertical: true)
        HStack(spacing: 8) {
            answerButton(L("action_yes"), prayer: prayer, answer: .yes, filled: true)
            answerButton(L("action_no"), prayer: prayer, answer: .no, filled: false)
        }
        .padding(.top, 8)
        Text(L(entry.nameKey) + "  ·  " + entry.timeHHmm)
            .font(.system(size: 13)).foregroundStyle(.secondary)
            .padding(.top, 6)
    }

    private func answerButton(_ title: String, prayer: TrackedPrayer, answer: TrackerAnswer,
                              filled: Bool) -> some View {
        Button(intent: AnswerPrayerIntent(prayer: prayer, answer: answer)) {
            Text(title)
                .font(.system(size: 15, weight: .semibold))
                .foregroundColor(filled ? .white : .primary)
                .padding(.horizontal, 18).padding(.vertical, 7)
                .background(
                    Capsule().fill(filled ? Color(red: 0, green: 0x83 / 255.0, blue: 0x48 / 255.0)
                                          : Color.primary.opacity(0.12))
                )
        }
        .buttonStyle(.plain)
    }

    @ViewBuilder private var countdown: some View {
        Group {
            Text(L(entry.nameKey))
                .font(.system(size: 19, weight: .bold)).foregroundColor(amber)
                .lineLimit(1)
                // .primary statt Weiß: Das Widget steht auf einem hellen Untergrund, sobald das
                // Telefon im Hellmodus läuft — weiße Schrift war dort schlicht unsichtbar, und
                // genau deshalb fehlten Countdown und Uhrzeit ganz.
                Text(timerInterval: Date()...max(entry.target, Date().addingTimeInterval(1)), countsDown: true)
                    .font(.system(size: 44, weight: .bold))
                    .foregroundStyle(.primary)
                    .lineLimit(1).minimumScaleFactor(0.5)
            Text("\(L("widget_remaining"))  ·  \(entry.timeHHmm)")
                .font(.system(size: 15))
                .foregroundStyle(.secondary)
                .padding(.top, 2)
        }
    }
}

private extension View {
    // Durchsichtiger Hintergrund wie auf Android. iOS verlangt eine ausdrueckliche Angabe —
    // ohne sie setzt das System einen weissen Hintergrund, und weisse Schrift darauf ist
    // unsichtbar. Genau das hatte das Widget: Countdown und Uhrzeit fehlten scheinbar ganz.
    func widgetBackgroundClear() -> some View {
        containerBackground(for: .widget) { Color.clear }
    }
}

struct KasselWidget: Widget {
    let kind = "KasselWidget"
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: Provider()) { entry in
            KasselWidgetEntryView(entry: entry)
        }
        .configurationDisplayName("IGBD Vaktija")
        .description("Nächste Gebetszeit mit Countdown.")
        .supportedFamilies([.systemMedium, .systemSmall])
    }
}

@main
struct KasselWidgetBundle: WidgetBundle {
    var body: some Widget { KasselWidget() }
}
