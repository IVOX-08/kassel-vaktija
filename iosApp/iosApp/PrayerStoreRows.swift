import Foundation

// Die Gebetszeilen der Startseite.
//
// Steht in der App und nicht im gemeinsamen Code: Die Zeile braucht die LIVE-Regel der Gemeinde
// aus Firestore, damit eine geaenderte Ikamet sofort auf dem Schirm steht. Das Widget kommt mit
// dem Zwischenspeicher aus und soll Firestore gar nicht erst mitschleppen.
extension PrayerStore {
    var rows: [PrayerRow] { PrayerModel.rows(today, rule: CommunityRuleStore.shared.rule) }
}
