import SwiftUI

// Wohin ein Antippen führt.
//
// Eine Meldung, die nur „die App" öffnet, macht aus der Antwort eine Suche: Die Tracker-Frage hat
// ein Fenster, und wer erst zum Reiter „Mehr" und dort durch eine Liste muss, findet es geschlossen
// vor. Eine Mitteilung dasselbe — sie steht in „Nachrichten", nicht auf der Startseite.
//
// Deshalb steht der Zielbildschirm an EINER Stelle und wird von der Benachrichtigung gesetzt,
// nicht von jedem Bildschirm einzeln geraten.
@MainActor
final class AppRoute: ObservableObject {
    static let shared = AppRoute()

    enum Tab: Int, Hashable { case prayer, calendar, news, more, settings }

    @Published var tab: Tab = .prayer

    /// Was in „Mehr" aufgeschlagen werden soll. Wird nach dem Öffnen wieder geleert, sonst
    /// springt der Reiter beim nächsten Hinsehen erneut auf dieselbe Seite.
    @Published var pendingMore: MoreDestination?

    func openTracker() {
        tab = .more
        pendingMore = .tracker
    }

    func openNews() {
        tab = .news
    }
}

/// Die Seiten hinter dem Reiter „Mehr". Als Wert statt als fertige Ansicht, damit eine
/// Benachrichtigung eine davon aufschlagen kann, ohne die Liste vorher gesehen zu haben.
enum MoreDestination: String, Hashable, CaseIterable {
    case quran, hadith, dhikr, tasbih, tracker, ramadan, zakat, qibla

    var titleKey: String {
        switch self {
        case .quran: return "library_quran"
        case .hadith: return "library_hadith"
        case .dhikr: return "library_dhikr"
        case .tasbih: return "library_tasbih"
        case .tracker: return "library_tracker"
        case .ramadan: return "library_ramadan"
        case .zakat: return "library_zakat"
        case .qibla: return "nav_qibla"
        }
    }

    var icon: String {
        switch self {
        case .quran: return "book.fill"
        case .hadith: return "text.quote"
        case .dhikr: return "figure.mind.and.body"
        case .tasbih: return "target"
        case .tracker: return "checkmark.circle"
        case .ramadan: return "moon.fill"
        case .zakat: return "plusminus.circle"
        case .qibla: return "safari"
        }
    }

    @ViewBuilder var view: some View {
        switch self {
        case .quran: QuranView()
        case .hadith: HadithView()
        case .dhikr: DhikrView()
        case .tasbih: TasbihView()
        case .tracker: TrackerView()
        case .ramadan: RamadanView()
        case .zakat: ZakatView()
        case .qibla: QiblaView()
        }
    }
}
