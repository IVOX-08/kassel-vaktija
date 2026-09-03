import Foundation
import UserNotifications

// Schedules local notifications for each enabled prayer (update prompt #2 + #4):
// - Texts are built in the language the user CHOSE in the app (never the system language), read
//   from the persisted app_lang via L() — the Android bug was reading an empty language in the
//   background and falling back to Bosnian.
// - Each prayer can be switched off individually and can carry a pre-warning (0/5/10/15/30 min).
// - Sound: "Kurzer Adhan" (adhan_short.mp3) or "Signalton" (chime.wav). iOS plays the notification
//   sound and vibrates on its own when the phone is on silent/vibrate, so the Adhan is still felt
//   (there is no Android-style DND override on iOS).
/// Without a delegate opting in, iOS silently drops notifications while the app is in the
/// foreground — the Adhan would not be heard by someone who has the app open. This shows the
/// banner and plays the chosen sound in that case too.
final class NotificationPresenter: NSObject, UNUserNotificationCenterDelegate {
    static let shared = NotificationPresenter()

    /// Kennung der Tracker-Frage. Die Antwortknöpfe hängen an der Kategorie, nicht an der
    /// einzelnen Meldung.
    static let trackerCategory = "tracker_ask"

    func install() {
        let center = UNUserNotificationCenter.current()
        center.delegate = self
        let yes = UNNotificationAction(identifier: "tracker_yes", title: L("action_yes"))
        let no = UNNotificationAction(identifier: "tracker_no", title: L("action_no"))
        center.setNotificationCategories([
            UNNotificationCategory(identifier: Self.trackerCategory,
                                   actions: [yes, no], intentIdentifiers: [])
        ])
    }

    /// Nimmt die Antwort auf die Tracker-Frage entgegen.
    ///
    /// Hier wird das Fenster ein ZWEITES Mal geprüft — im Speicher, nicht in der Anzeige. Eine
    /// Benachrichtigung kann stundenlang in der Leiste liegen; ohne diese Prüfung würde ein „Ja"
    /// auf die Fajr-Frage um die Mittagszeit zählen.
    func userNotificationCenter(_ center: UNUserNotificationCenter,
                                didReceive response: UNNotificationResponse,
                                withCompletionHandler completionHandler: @escaping () -> Void) {
        defer { completionHandler() }
        let info = response.notification.request.content.userInfo

        // Eine Mitteilung (Cloud Function: data.type = "news" oder "broadcasts") gehört in die
        // Mitteilungen, nicht auf die Startseite. Wer dort landet, sucht die Meldung, die er
        // gerade angetippt hat.
        if let type = info["type"] as? String, type == "news" || type == "broadcasts" {
            Task { @MainActor in AppRoute.shared.openNews() }
            return
        }

        guard let raw = info["prayer"] as? String, let prayer = TrackedPrayer(rawValue: raw) else { return }
        switch response.actionIdentifier {
        case "tracker_yes": PrayerTracker.record(prayer, .yes)
        case "tracker_no": PrayerTracker.record(prayer, .no)
        default:
            // Angetippt statt beantwortet: Dann muss der Tracker aufgehen. Das Fenster läuft, und
            // wer sich erst durch „Mehr" und eine Liste tippen muss, findet es geschlossen vor.
            Task { @MainActor in AppRoute.shared.openTracker() }
        }
    }

    func userNotificationCenter(_ center: UNUserNotificationCenter,
                                willPresent notification: UNNotification,
                                withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void) {
        if #available(iOS 14.0, *) { completionHandler([.banner, .list, .sound]) }
        else { completionHandler([.alert, .sound]) }
    }
}

enum NotificationScheduler {

    /// Asks for permission the first time (status .notDetermined). Without this, a user who skipped
    /// the onboarding assistant would never be asked and no prayer notification would ever fire.
    /// Returns true when notifications may be scheduled.
    @discardableResult
    static func ensureAuthorization() async -> Bool {
        let center = UNUserNotificationCenter.current()
        let status = await center.notificationSettings().authorizationStatus
        switch status {
        case .authorized, .provisional, .ephemeral:
            return true
        case .notDetermined:
            return (try? await center.requestAuthorization(options: [.alert, .sound, .badge])) ?? false
        default:
            return false // denied — the Settings screen offers a button to fix it
        }
    }

    /// iOS nimmt pro App hoechstens 64 wartende Meldungen an. Was darueber hinaus eingereicht
    /// wird, verwirft das System stillschweigend — in der Reihenfolge des Einreichens, nicht nach
    /// Uhrzeit. Sieben Tage mal sechs Gebete sind allein schon 42, mit Vorwarnungen 84, dazu fuenf
    /// Tracker-Fragen taeglich: ohne Deckel fielen ausgerechnet die spaeteren Gebetsrufe weg.
    private static let maxPending = 64

    /// Eine geplante Meldung, bevor sie beim System landet. Erst wenn alle beisammen sind, laesst
    /// sich nach Zeitpunkt sortieren und am Limit abschneiden.
    private struct Planned {
        let id: String
        let fire: Date
        let content: UNNotificationContent
    }

    /// Cancels and re-schedules everything from the current settings + times. Safe to call often.
    static func reschedule(times: DayTimes) async {
        let center = UNUserNotificationCenter.current()
        guard await ensureAuthorization() else { return }

        center.removeAllPendingNotificationRequests()
        // Kein Hauptschalter mehr — jedes Gebet hat seinen eigenen. Ein einziger Schalter, der
        // alles abdreht, hat zu viele Leute stumm zurückgelassen.

        let cal = Calendar.current
        var planned: [Planned] = []
        // Die naechste Woche im Voraus, damit die Meldungen auch dann kommen, wenn die App
        // tagelang nicht geoeffnet wird.
        for dayOffset in 0..<7 {
            guard let day = cal.date(byAdding: .day, value: dayOffset, to: Date()) else { continue }
            // Heute die offiziellen Zeiten, die gerade geladen wurden; jeder weitere Tag seine
            // eigenen. Vorher wurden sieben Tage lang die heutigen Uhrzeiten wiederholt — im
            // Fruehjahr laeuft der Sonnenaufgang darin um mehr als zwanzig Minuten davon.
            let dayTimes = dayOffset == 0 ? times : PrayerStore.times(on: day)
            planned += adhanPlan(dayTimes, day: day, dayOffset: dayOffset)
            planned += trackerPlan(day: day, dayOffset: dayOffset)
            planned += jumuaPlan(day: day, dayOffset: dayOffset)
        }

        planned = planned.compactMap(respectingJumuaSilence)

        for item in planned.sorted(by: { $0.fire < $1.fire }).prefix(maxPending) {
            let trigger = UNCalendarNotificationTrigger(
                dateMatching: cal.dateComponents([.year, .month, .day, .hour, .minute], from: item.fire),
                repeats: false)
            // `add` ist im asynchronen Zusammenhang die werfende Fassung. Ein Fehlschlag betrifft
            // genau diese eine Meldung; die uebrigen sollen trotzdem gestellt werden.
            try? await center.add(
                UNNotificationRequest(identifier: item.id, content: item.content, trigger: trigger))
        }
    }

    /// Gebetsruf und optionale Vorwarnung eines Tages.
    private static func adhanPlan(_ times: DayTimes, day: Date, dayOffset: Int) -> [Planned] {
        let d = AppGroup.defaults
        let sound = notifSound()
        var out: [Planned] = []
        for (key, nameKey, minutes) in prayerList(times) {
            let enabled = d.object(forKey: "pn_\(key)") as? Bool ?? enabledByDefault(key)
            guard enabled else { continue }
            let warn = d.integer(forKey: "pw_\(key)")

            // Der Sonnenaufgang ist kein Gebetsruf: dort endet die Zeit für das Morgengebet.
            // „Zeit für Sonnenaufgang" wäre schlicht falsch.
            let isSunrise = key == "sunrise"
            out += plan(id: "adhan_\(key)_\(dayOffset)",
                        title: isSunrise ? L("notif_sunrise_title")
                                         : String(format: L("notif_adhan_title"), L(nameKey)),
                        body: isSunrise ? L("notif_sunrise_text") : L("notif_adhan_text"),
                        minutes: minutes, day: day, sound: sound)

            // Optional pre-warning
            if warn > 0 {
                out += plan(id: "warn_\(key)_\(dayOffset)",
                            title: isSunrise
                                ? String(format: L("notif_sunrise_prewarn_title"), warn)
                                : String(format: L("notif_prewarn_title"), L(nameKey), warn),
                            body: isSunrise ? L("notif_sunrise_text") : L("notif_adhan_text"),
                            minutes: minutes - warn, day: day, sound: sound)
            }
        }
        return out
    }

    /// Fragt zum Ikamet jedes Gebets: „Hast du … gebetet?" mit Ja und Nein direkt in der Meldung.
    ///
    /// Eine Frage, die erst das Öffnen der App verlangt, wird beantwortet, wenn das Fenster schon
    /// zu ist — deshalb die Knöpfe in der Benachrichtigung.
    ///
    /// Bewusst OHNE Ton. Fünf tönende Meldungen am Tag wären der schnellste Weg, die App
    /// stummzuschalten — und dann wäre auch der Adhan weg.
    private static func trackerPlan(day: Date, dayOffset: Int) -> [Planned] {
        guard AppGroup.defaults.object(forKey: trackerAskKey) == nil
                || AppGroup.defaults.bool(forKey: trackerAskKey) else { return [] }
        var out: [Planned] = []
        for prayer in TrackedPrayer.allCases {
            guard let w = PrayerTracker.window(prayer, on: day), w.open > Date() else { continue }
            let content = UNMutableNotificationContent()
            content.title = String(format: L("tracker_ask_title"), L(prayer.nameKey))
            content.body = L("tracker_ask_text")
            content.categoryIdentifier = NotificationPresenter.trackerCategory
            content.userInfo = ["prayer": prayer.rawValue]
            content.sound = nil
            if #available(iOS 15.0, *) { content.interruptionLevel = .timeSensitive }
            out.append(Planned(id: "ask_\(prayer.rawValue)_\(dayOffset)", fire: w.open, content: content))
        }
        return out
    }

    /// Womit eine Zeile beginnt, solange der Nutzer sie nie angefasst hat.
    ///
    /// Der Sonnenaufgang beginnt AUS. Er ist kein Gebetsruf, sondern das Ende der Zeit fuer das
    /// Morgengebet, und ein ungefragter Ruf bei Tagesanbruch weckt Leute, die ihn nie wollten.
    /// Muss mit `PrayerNotifCard.defaultsOff` uebereinstimmen — sonst zeigt der Schalter „an",
    /// waehrend nichts geplant wird.
    static func enabledByDefault(_ key: String) -> Bool { key != "sunrise" }

    // MARK: - Freitagsgebet

    /// Erinnert 30 Minuten vor der Dzuma.
    ///
    /// Das Freitagsgebet ist das einzige mit einer festen Uhrzeit, die die Gemeinde selbst setzt,
    /// und das einzige, zu dem man sich auf den Weg machen muss. Eine halbe Stunde ist die Zeit,
    /// die man dafuer braucht — der Gebetsruf selbst kommt zu spaet, um noch loszugehen.
    ///
    /// Kein neuer Text: Titel und Zeile sind dieselben wie bei jeder anderen Vorwarnung.
    private static let jumuaWarnMinutes = 30

    private static func jumuaPlan(day: Date, dayOffset: Int) -> [Planned] {
        guard Calendar.current.component(.weekday, from: day) == 6,
              let jumua = hhmm(CommunityRule.cachedRule().jumua),
              let fire = at(jumua - jumuaWarnMinutes, on: day), fire > Date()
        else { return [] }

        let content = UNMutableNotificationContent()
        content.title = String(format: L("notif_prewarn_title"), L("prayer_jumua"), jumuaWarnMinutes)
        content.body = L("notif_adhan_text")
        content.sound = notifSound()
        if #available(iOS 15.0, *) { content.interruptionLevel = .timeSensitive }
        return [Planned(id: "jumua_\(dayOffset)", fire: fire, content: content)]
    }

    /// Von 10 Minuten VOR dem Freitagsgebet bis 40 Minuten NACH seinem Beginn — eine Stunde und
    /// zehn Minuten — sagt die App nichts.
    ///
    /// Das ist die Zeit der Chutba und des Gebets. Ein Telefon, das dort klingelt, stoert nicht
    /// den Besitzer, sondern die ganze Reihe.
    ///
    /// WICHTIG: iOS laesst keine App das Telefon stummschalten oder „Nicht stoeren" einschalten —
    /// es gibt dafuer keine Schnittstelle. Still ist also NUR diese App. Anrufe und andere Apps
    /// klingeln weiter; dafuer muss das Telefon selbst leise gestellt werden.
    private static func jumuaQuiet(on day: Date) -> (start: Date, end: Date)? {
        guard Calendar.current.component(.weekday, from: day) == 6,
              let jumua = hhmm(CommunityRule.cachedRule().jumua),
              let start = at(jumua - 10, on: day), let end = at(jumua + 40, on: day)
        else { return nil }
        return (start, end)
    }

    /// Wirft eine Meldung weg, die in die Stille faellt — mit einer Ausnahme.
    ///
    /// Die Tracker-Frage wird NICHT verworfen, sondern ans Ende der Stille geschoben. Ihr Fenster
    /// laeuft bis zum Ikindija-Ruf und ist dann noch offen; wer sie nie bekaeme, verloere jeden
    /// Freitag seine Flamme, ohne etwas falsch gemacht zu haben.
    private static func respectingJumuaSilence(_ item: Planned) -> Planned? {
        guard let quiet = jumuaQuiet(on: item.fire),
              item.fire >= quiet.start, item.fire <= quiet.end else { return item }
        guard item.content.categoryIdentifier == NotificationPresenter.trackerCategory else { return nil }
        return Planned(id: item.id, fire: quiet.end, content: item.content)
    }

    /// "HH:mm" in Minuten seit Mitternacht.
    private static func hhmm(_ s: String) -> Int? {
        let p = s.split(separator: ":")
        guard p.count == 2, let h = Int(p[0]), let m = Int(p[1]) else { return nil }
        return h * 60 + m
    }

    /// Minuten seit Mitternacht als Zeitpunkt an einem Tag.
    private static func at(_ minutes: Int, on day: Date) -> Date? {
        let cal = Calendar.current
        var c = cal.dateComponents([.year, .month, .day], from: day)
        c.hour = (minutes / 60) % 24
        c.minute = minutes % 60
        return cal.date(from: c)
    }

    /// Schalter fuer die Tracker-Fragen. Wer den Tracker nicht nutzt, soll nicht fuenfmal am Tag
    /// gefragt werden.
    static let trackerAskKey = "tracker_ask_enabled"

    // MARK: - helpers

    private static func prayerList(_ t: DayTimes) -> [(String, String, Int)] {
        [
            ("fajr", "prayer_fajr", t.fajr),
            ("sunrise", "prayer_sunrise", t.sunrise),
            ("dhuhr", "prayer_dhuhr", t.dhuhr),
            ("asr", "prayer_asr", t.asr),
            ("maghrib", "prayer_maghrib", t.maghrib),
            ("isha", "prayer_isha", t.isha),
        ]
    }

    private static func notifSound() -> UNNotificationSound {
        let raw = AppGroup.defaults.string(forKey: "notif_sound") ?? NotifSound.adhan.rawValue
        let s = NotifSound(rawValue: raw) ?? .adhan
        // Bundled under the "audio" folder; iOS looks the name up in the bundle.
        return UNNotificationSound(named: UNNotificationSoundName("\(s.file).\(s.ext)"))
    }

    /// Baut eine Meldung fuer eine Uhrzeit an einem Tag — oder nichts, wenn der Zeitpunkt schon
    /// vorbei ist.
    private static func plan(id: String, title: String, body: String,
                             minutes: Int, day: Date, sound: UNNotificationSound) -> [Planned] {
        let cal = Calendar.current
        var comps = cal.dateComponents([.year, .month, .day], from: day)
        // Auf den Tag zurechtgebogen. Eine Vorwarnung kann rechnerisch vor Mitternacht fallen
        // (Gebetszeit minus Vorwarnzeit), und negative Minuten landeten dann am VORTAG — also in
        // der Vergangenheit und damit im Papierkorb, ohne dass etwas darauf hinwies.
        let m = ((minutes % 1440) + 1440) % 1440
        comps.hour = m / 60
        comps.minute = m % 60
        guard let fire = cal.date(from: comps), fire > Date() else { return [] }

        let content = UNMutableNotificationContent()
        content.title = title
        content.body = body
        content.sound = sound
        // Time-sensitive so it still surfaces during Focus modes where the user allows it.
        if #available(iOS 15.0, *) { content.interruptionLevel = .timeSensitive }
        return [Planned(id: id, fire: fire, content: content)]
    }
}
