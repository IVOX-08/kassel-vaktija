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
enum NotificationScheduler {

    /// Cancels and re-schedules everything from the current settings + times. Safe to call often.
    static func reschedule(times: DayTimes) async {
        let center = UNUserNotificationCenter.current()
        let settings = await center.notificationSettings()
        guard settings.authorizationStatus == .authorized else { return }

        center.removeAllPendingNotificationRequests()
        let d = UserDefaults.standard
        guard d.object(forKey: "notif_master") == nil || d.bool(forKey: "notif_master") else { return }

        let sound = notifSound()
        // Schedule today's remaining prayers and the same set for the next 6 days, so notifications
        // keep firing even if the app isn't opened (iOS allows up to 64 pending requests).
        for dayOffset in 0..<7 {
            for (key, nameKey, minutes) in prayerList(times) {
                let enabled = d.object(forKey: "pn_\(key)") == nil || d.bool(forKey: "pn_\(key)")
                guard enabled else { continue }
                let warn = d.integer(forKey: "pw_\(key)")

                // Adhan itself
                schedule(id: "adhan_\(key)_\(dayOffset)",
                         title: String(format: L("notif_adhan_title"), L(nameKey)),
                         body: L("notif_adhan_text"),
                         minutes: minutes, dayOffset: dayOffset, sound: sound)

                // Optional pre-warning
                if warn > 0 {
                    schedule(id: "warn_\(key)_\(dayOffset)",
                             title: String(format: L("notif_prewarn_title"), L(nameKey), warn),
                             body: L("notif_adhan_text"),
                             minutes: minutes - warn, dayOffset: dayOffset, sound: sound)
                }
            }
        }
    }

    // MARK: - helpers

    private static func prayerList(_ t: DayTimes) -> [(String, String, Int)] {
        [
            ("fajr", "prayer_fajr", t.fajr),
            ("dhuhr", "prayer_dhuhr", t.dhuhr),
            ("asr", "prayer_asr", t.asr),
            ("maghrib", "prayer_maghrib", t.maghrib),
            ("isha", "prayer_isha", t.isha),
        ]
    }

    private static func notifSound() -> UNNotificationSound {
        let raw = UserDefaults.standard.string(forKey: "notif_sound") ?? NotifSound.adhan.rawValue
        let s = NotifSound(rawValue: raw) ?? .adhan
        // Bundled under the "audio" folder; iOS looks the name up in the bundle.
        return UNNotificationSound(named: UNNotificationSoundName("\(s.file).\(s.ext)"))
    }

    private static func schedule(id: String, title: String, body: String,
                                 minutes: Int, dayOffset: Int, sound: UNNotificationSound) {
        let cal = Calendar.current
        guard let base = cal.date(byAdding: .day, value: dayOffset, to: Date()) else { return }
        var comps = cal.dateComponents([.year, .month, .day], from: base)
        comps.hour = (minutes / 60) % 24
        comps.minute = minutes % 60
        guard let fireDate = cal.date(from: comps), fireDate > Date() else { return }

        let content = UNMutableNotificationContent()
        content.title = title
        content.body = body
        content.sound = sound
        // Time-sensitive so it still surfaces during Focus modes where the user allows it.
        if #available(iOS 15.0, *) { content.interruptionLevel = .timeSensitive }

        let trigger = UNCalendarNotificationTrigger(
            dateMatching: cal.dateComponents([.year, .month, .day, .hour, .minute], from: fireDate),
            repeats: false
        )
        UNUserNotificationCenter.current().add(
            UNNotificationRequest(identifier: id, content: content, trigger: trigger)
        )
    }
}
