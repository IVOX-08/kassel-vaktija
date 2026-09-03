import UIKit
import FirebaseCore
import FirebaseMessaging
import UserNotifications

// Firebase Cloud Messaging, die iOS-Seite dessen, was Android schon tut (siehe
// docs/ios/FIREBASE-HANDOFF.md §4). Zwei Arten von Meldung kommen hier an:
//
//   • Mitteilungen — eine normale Meldung mit Text. iOS zeigt sie selbst an; richtig sein muss
//     nur, dass APNs dieses Geraet kennt.
//   • Zeitaenderungen — eine reine Datenmeldung. Sie erreicht uns nur, wenn der Server
//     `content-available: 1` mitschickt (siehe functions/index.js).
//
// DIE THEMEN: Bis hierher hing jedes Geraet am einen Thema `announcements`. Damit haette jeder
// Nutzer in Deutschland jede Mitteilung jeder der 81 Gemeinden bekommen. Jetzt sind es drei,
// und sie tragen Gemeinde und Sprache im Namen — siehe PushTopics unten.
final class AppDelegate: NSObject, UIApplicationDelegate, MessagingDelegate {

    func application(_ application: UIApplication,
                     didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil) -> Bool {
        // iOSApp.init() configures Firebase, but only when GoogleService-Info.plist is present.
        // Without it Messaging would crash, so degrade to "no push" the same way the rest does.
        guard FirebaseApp.app() != nil else { return true }
        Messaging.messaging().delegate = self
        // Safe to call unconditionally: this asks APNs for a device token and never prompts the
        // user. The alert permission is a separate thing, handled by NotificationScheduler.
        application.registerForRemoteNotifications()
        PushTopics.observeChanges()
        return true
    }

    // MARK: Ausrichtung

    /// Welche Ausrichtungen gerade erlaubt sind.
    ///
    /// Die App ist fuer das Hochformat gestaltet: gedrehte Gebetszeiten, ein gedrehter Kalender und
    /// ein gedrehter Qibla-Kompass waeren schlechter, nicht besser. Der Koran-Leser ist die
    /// Ausnahme — quer gehalten passt deutlich mehr in eine Zeile.
    static var allowedOrientations: UIInterfaceOrientationMask = .portrait

    func application(_ application: UIApplication,
                     supportedInterfaceOrientationsFor window: UIWindow?) -> UIInterfaceOrientationMask {
        AppDelegate.allowedOrientations
    }

    /// Gibt das Querformat frei bzw. nimmt es zurueck — und dreht beim Zurueckgehen selbst wieder
    /// auf Hochformat. Ohne das bliebe die naechste Seite quer stehen, obwohl sie dafuer nicht
    /// gebaut ist.
    @MainActor
    static func allowLandscape(_ allow: Bool) {
        allowedOrientations = allow ? [.portrait, .landscapeLeft, .landscapeRight] : .portrait
        guard !allow,
              let scene = UIApplication.shared.connectedScenes.first as? UIWindowScene
        else { return }
        scene.requestGeometryUpdate(.iOS(interfaceOrientations: .portrait))
    }

    // MARK: APNs token

    func application(_ application: UIApplication,
                     didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
        // FCM cannot talk to this device until it has the APNs token.
        Messaging.messaging().apnsToken = deviceToken
    }

    func application(_ application: UIApplication,
                     didFailToRegisterForRemoteNotificationsWithError error: Error) {
        // Not fatal: prayer reminders are scheduled locally and keep working without push.
        NSLog("[Push] APNs registration failed: \(error.localizedDescription)")
    }

    // MARK: FCM token

    func messaging(_ messaging: Messaging, didReceiveRegistrationToken fcmToken: String?) {
        guard fcmToken != nil else { return }
        // Anmelden geht erst mit Token, deshalb steht es hier und nicht in didFinishLaunching.
        PushTopics.resync()
    }

    // MARK: silent push — prayer times changed

    func application(_ application: UIApplication,
                     didReceiveRemoteNotification userInfo: [AnyHashable: Any],
                     fetchCompletionHandler completionHandler: @escaping (UIBackgroundFetchResult) -> Void) {
        Task { @MainActor in
            // refresh() re-fetches the official times, rewrites the cache and the calibration, and
            // reschedules the seven days of local prayer notifications. Reusing it keeps the
            // background path and the foreground path from drifting apart.
            await PrayerStore().refresh()
            completionHandler(.newData)
        }
    }
}

/// Woran dieses Geraet haengt.
///
/// Drei Themen, und sie muessen sich aendern, wenn sich die Gemeinde oder die Sprache aendert:
///
///   c_<gemeinde>            Datenmeldung: die Zeiten dieser Gemeinde haben sich geaendert
///   c_<gemeinde>_<sprache>  Mitteilungen dieser Gemeinde, im richtigen Wortlaut
///   b_<sprache>             verbandsweite Mitteilungen des Hauptadministrators
///
/// Die Sprache steckt im Namen, weil der Text schon beim Verfassen in alle acht Sprachen
/// uebersetzt wird. So steht die Mitteilung gleich in der Leiste richtig da und nicht erst,
/// nachdem jemand die App geoeffnet hat.
///
/// Was einmal angemeldet wurde, bleibt es bei Firebase — bis es ausdruecklich abgemeldet wird.
/// Deshalb merkt sich diese Stelle, woran sie das Geraet gehaengt hat: Ohne das bekaeme jemand,
/// der zweimal die Gemeinde wechselt, die Mitteilungen von drei Gemeinden.
enum PushTopics {
    private static let storeKey = "push_topics"

    private static var desired: [String] {
        let community = CommunitySelection.communityId
        let lang = Localization.shared.lang
        return ["c_\(community)", "c_\(community)_\(lang)", "b_\(lang)"]
    }

    static func observeChanges() {
        for name in [Notification.Name.communityDidChange, .appLanguageDidChange] {
            NotificationCenter.default.addObserver(forName: name, object: nil, queue: .main) { _ in
                resync()
            }
        }
    }

    static func resync() {
        guard FirebaseApp.app() != nil else { return }
        let want = Set(desired)
        let have = Set(AppGroup.defaults.stringArray(forKey: storeKey) ?? [])
        guard want != have else { return }

        for topic in have.subtracting(want) {
            Messaging.messaging().unsubscribe(fromTopic: topic) { error in
                if let error { NSLog("[Push] Abmelden von \(topic) fehlgeschlagen: \(error.localizedDescription)") }
            }
        }
        for topic in want.subtracting(have) {
            Messaging.messaging().subscribe(toTopic: topic) { error in
                if let error { NSLog("[Push] Anmelden an \(topic) fehlgeschlagen: \(error.localizedDescription)") }
            }
        }
        // Auch dann merken, wenn eine einzelne Anmeldung scheitert: Firebase versucht es von
        // selbst weiter, und ein zweiter Durchlauf wuerde sonst dieselben Themen doppelt melden.
        AppGroup.defaults.set(Array(want), forKey: storeKey)
    }
}
