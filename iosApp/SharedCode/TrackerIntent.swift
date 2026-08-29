import AppIntents

// Die Antwort auf die Tracker-Frage, direkt aus dem Widget.
//
// Ein Widget kann keinen eigenen Code ausführen; es kann nur eine Absicht auslösen, die das System
// dann startet. Deshalb liegt die Antwort hier und nicht in der Ansicht — und deshalb braucht das
// Ganze iOS 17.
//
// Gefragt wird zum Ikamet. Wer erst die App öffnen muss, beantwortet die Frage, wenn das Fenster
// schon zu ist.
struct AnswerPrayerIntent: AppIntent {
    static var title: LocalizedStringResource = "Gebet beantworten"

    /// Als Zeichenkette, nicht als eigener Typ: Eine Absicht muss sich vom System speichern und
    /// später wieder herstellen lassen, und dafür sind die einfachen Typen der sichere Weg.
    @Parameter(title: "Gebet") var prayer: String
    @Parameter(title: "Antwort") var answer: String

    init() {}

    init(prayer: TrackedPrayer, answer: TrackerAnswer) {
        self.prayer = prayer.rawValue
        self.answer = answer.rawValue
    }

    func perform() async throws -> some IntentResult {
        guard let prayer = TrackedPrayer(rawValue: prayer),
              let answer = TrackerAnswer(rawValue: answer)
        else { return .result() }
        // `record` prüft das Fenster ein zweites Mal und stößt danach das Widget an. Ein Widget
        // kann lange unangetastet auf dem Bildschirm stehen; ohne diese Prüfung würde ein „Ja"
        // gezählt, dessen Fenster längst zu ist.
        PrayerTracker.record(prayer, answer)
        return .result()
    }
}
