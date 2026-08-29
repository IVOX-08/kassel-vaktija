import WidgetKit

// Das Widget neu zeichnen lassen.
//
// WidgetKit rechnet einen Zeitplan aus und haelt sich daran. Aendert sich etwas, worauf dieser
// Zeitplan aufbaut — die Sprache, die Gemeinde, die geladenen Gebetszeiten, eine Antwort im
// Tracker —, merkt das Widget davon von selbst NICHTS. Es zeigte deshalb bosnische Namen, obwohl
// die App auf Deutsch stand, und eine Flamme, die nicht mitzaehlte.
enum WidgetRefresh {
    static func now() {
        WidgetCenter.shared.reloadAllTimelines()
    }
}
