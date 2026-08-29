import UIKit
import FirebaseCore
import FirebaseMessaging
import UserNotifications

// Firebase Cloud Messaging, the iOS half of what Android already does (see
// docs/ios/FIREBASE-HANDOFF.md §4). Two kinds of message arrive here:
//
//   • `onNewsCreated` — a normal notification payload. iOS displays it by itself; the only
//     thing we must get right is that APNs knows this device.
//   • `onConfigUpdated` — a data-only message telling us the community changed the prayer
//     times. It only reaches us when the server sends `content-available: 1`, which the
//     handover notes as still missing on the server side. The handler below is written so it
//     works the moment the server is fixed, and costs nothing until then.
//
// Both platforms listen on the single topic `announcements` — do not rename it, the Android
// app and the Cloud Function both hardcode it.
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
        // Subscribing needs a token first, which is why it lives here and not in didFinishLaunching.
        Messaging.messaging().subscribe(toTopic: "announcements") { error in
            if let error { NSLog("[Push] topic subscribe failed: \(error.localizedDescription)") }
        }
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
