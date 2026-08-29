import SwiftUI
import UIKit

// Der Gebets-Tracker (Abschnitt 5.5).
//
// Die Liste zeigt fuer jedes der fuenf Gebete genau einen von fuenf Zustaenden, und daraus ergibt
// sich, was man tun kann: Nur ein offenes Fenster hat Knoepfe. Alles andere ist eine Feststellung —
// „Gebetet", „Verpasst" —, kein Angebot. So laesst sich der Bildschirm ohne Erklaerung lesen, und
// die Regel darunter beantwortet die einzige Frage, die dann noch offen ist: warum es fuer ein
// abgelaufenes Gebet keinen Knopf mehr gibt.
//
// Die Zustaende kommen aus PrayerTracker; diese Datei rechnet nichts selbst aus.

struct TrackerView: View {
    /// Die Uhr laeuft weiter, waehrend der Bildschirm offen ist: Ein Fenster kann sich waehrend
    /// des Hinsehens oeffnen oder schliessen, und eine Liste, die das erst beim naechsten Aufruf
    /// nachvollzieht, wuerde falsche Knoepfe anbieten.
    @State private var now = Date()
    /// Wird nach jeder Antwort hochgezaehlt. Die Antworten liegen in UserDefaults, nicht im
    /// View-State — ohne diesen Anstoss bliebe die Zeile stehen, wie sie war.
    @State private var version = 0

    private let clock = Timer.publish(every: 30, on: .main, in: .common).autoconnect()
    private let haptic = UINotificationFeedbackGenerator()

    private var streak: Int { PrayerTracker.streak(now: now) }

    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                streakCard
                todaySection
                ruleNote
            }
            .padding(16)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color.appBackground.ignoresSafeArea())
        .navigationTitle(L("library_tracker")).navigationBarTitleDisplayMode(.inline)
        .onReceive(clock) { now = $0 }
        // Die Antwort kann aus der Benachrichtigung gekommen sein, waehrend die App im Hintergrund
        // lag. Beim Zurueckkehren muss die Liste das zeigen.
        .onReceive(NotificationCenter.default.publisher(for: UIApplication.willEnterForegroundNotification)) { _ in
            now = Date(); version += 1
        }
    }

    // MARK: - Flamme und Geschenk

    private var streakCard: some View {
        VStack(spacing: 16) {
            HStack(spacing: 18) {
                ZStack {
                    Circle().stroke(Color.appPrimary.opacity(0.15), lineWidth: 8)
                    Circle()
                        .trim(from: 0, to: min(CGFloat(streak) / CGFloat(PrayerTracker.rewardDays), 1))
                        .stroke(Color.appPrimary, style: StrokeStyle(lineWidth: 8, lineCap: .round))
                        .rotationEffect(.degrees(-90))
                    Image(systemName: "flame.fill")
                        .font(.system(size: 30))
                        .foregroundColor(streak > 0 ? .appSecondary : .appOnSurfaceVariant.opacity(0.4))
                }
                .frame(width: 84, height: 84)
                .animation(.easeInOut(duration: 0.3), value: streak)

                VStack(alignment: .leading, spacing: 2) {
                    Text("\(streak)").font(.inter(40, .bold)).foregroundColor(.appPrimary).monospacedDigit()
                    Text(L("tracker_streak")).font(.inter(14)).foregroundColor(.appOnSurfaceVariant)
                }
                Spacer()
            }

            if streak >= PrayerTracker.rewardDays {
                // Erreicht: kein Balken mehr, sondern der Satz, auf den es hinauslief.
                HStack(spacing: 10) {
                    Image(systemName: "gift.fill").font(.system(size: 18)).foregroundColor(.appSecondary)
                    Text(L("tracker_reward_reached"))
                        .font(.inter(14, .semibold)).foregroundColor(.appOnSurface)
                        .fixedSize(horizontal: false, vertical: true)
                    Spacer(minLength: 0)
                }
                .padding(12)
                .background(Color.appSecondary.opacity(0.14))
                .clipShape(RoundedRectangle(cornerRadius: Radius.headerItem))
            } else {
                VStack(alignment: .leading, spacing: 8) {
                    Text(String(format: L("tracker_reward_progress"), streak, PrayerTracker.rewardDays))
                        .font(.inter(13)).foregroundColor(.appOnSurfaceVariant)
                        .fixedSize(horizontal: false, vertical: true)
                    GeometryReader { geo in
                        ZStack(alignment: .leading) {
                            Capsule().fill(Color.appPrimary.opacity(0.12))
                            Capsule().fill(Color.appPrimary)
                                .frame(width: geo.size.width * min(CGFloat(streak) / CGFloat(PrayerTracker.rewardDays), 1))
                        }
                    }
                    .frame(height: 8)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
            }
        }
        .padding(18)
        .background(Color.appSurface)
        .clipShape(RoundedRectangle(cornerRadius: Radius.prayerCard))
    }

    // MARK: - Heute

    private var todaySection: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(L("tracker_today")).font(.inter(17, .bold)).foregroundColor(.appOnSurface)
            VStack(spacing: 0) {
                ForEach(Array(TrackedPrayer.allCases.enumerated()), id: \.element) { index, prayer in
                    row(prayer)
                    if index < TrackedPrayer.allCases.count - 1 {
                        Divider().opacity(0.4).padding(.leading, 46)
                    }
                }
            }
            .padding(.horizontal, 16)
            .background(Color.appSurface)
            .clipShape(RoundedRectangle(cornerRadius: Radius.prayerCard))
        }
    }

    private func row(_ prayer: TrackedPrayer) -> some View {
        let state = PrayerTracker.state(prayer, at: now)
        return HStack(spacing: 12) {
            Image(systemName: icon(state)).font(.system(size: 20)).foregroundColor(tint(state)).frame(width: 24)
            VStack(alignment: .leading, spacing: 2) {
                Text(L(prayer.nameKey)).font(.inter(16, .medium)).foregroundColor(.appOnSurface)
                Text(subtitle(state)).font(.inter(12)).foregroundColor(tint(state))
            }
            Spacer(minLength: 8)
            // Knoepfe nur im offenen Fenster. Ausserhalb gibt es nichts zu entscheiden, und ein
            // grauer, wirkungsloser Knopf waere eine Einladung, es trotzdem zu versuchen.
            if case .open = state {
                HStack(spacing: 8) {
                    answerButton(L("action_yes"), filled: true) { record(prayer, .yes) }
                    answerButton(L("action_no"), filled: false) { record(prayer, .no) }
                }
            }
        }
        .padding(.vertical, 14)
    }

    private func answerButton(_ title: String, filled: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(title)
                .font(.inter(14, .semibold))
                .foregroundColor(filled ? .white : .appPrimary)
                .padding(.horizontal, 16).padding(.vertical, 8)
                .background(
                    Capsule().fill(filled ? Color.appPrimary : Color.appPrimary.opacity(0.10))
                )
        }
        .buttonStyle(.plain)
    }

    private func record(_ prayer: TrackedPrayer, _ answer: TrackerAnswer) {
        let counted = PrayerTracker.record(prayer, answer)
        haptic.notificationOccurred(counted ? .success : .warning)
        withAnimation(.easeInOut(duration: 0.2)) {
            now = Date()
            version += 1
        }
    }

    // MARK: - Die Regel

    private var ruleNote: some View {
        HStack(alignment: .top, spacing: 10) {
            Image(systemName: "info.circle").font(.system(size: 15)).foregroundColor(.appOnSurfaceVariant)
            Text(L("tracker_rule_explained"))
                .font(.inter(13)).foregroundColor(.appOnSurfaceVariant)
                .fixedSize(horizontal: false, vertical: true)
        }
        .padding(14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.appSurfaceVariant)
        .clipShape(RoundedRectangle(cornerRadius: Radius.smallCard))
    }

    // MARK: - Zustand in Text, Zeichen und Farbe

    private func subtitle(_ state: TrackerState) -> String {
        switch state {
        case .prayed: return L("tracker_state_prayed")
        case .notPrayed: return L("tracker_state_not_prayed")
        case .missed: return L("tracker_state_missed")
        case .open(let closesAt): return String(format: L("tracker_state_open"), Self.hhmm(closesAt))
        case .upcoming(let opensAt): return String(format: L("tracker_state_upcoming"), Self.hhmm(opensAt))
        }
    }

    private func icon(_ state: TrackerState) -> String {
        switch state {
        case .prayed: return "checkmark.circle.fill"
        case .notPrayed: return "xmark.circle.fill"
        case .missed: return "clock.badge.xmark"
        case .open: return "questionmark.circle.fill"
        case .upcoming: return "clock"
        }
    }

    /// „Verpasst" und „Nicht gebetet" bleiben gedeckt statt rot: Der Bildschirm soll den Tag
    /// festhalten, nicht schimpfen.
    private func tint(_ state: TrackerState) -> Color {
        switch state {
        case .prayed: return .appPrimary
        case .open: return .appSecondary
        case .notPrayed, .missed, .upcoming: return .appOnSurfaceVariant
        }
    }

    /// Feste 24-Stunden-Schreibweise wie im Rest der App — die Gebetszeiten stehen ueberall so.
    private static func hhmm(_ date: Date) -> String {
        let f = DateFormatter()
        f.calendar = Calendar(identifier: .gregorian)
        f.locale = Locale(identifier: "en_US_POSIX")
        f.dateFormat = "HH:mm"
        return f.string(from: date)
    }
}
