import Foundation
import FirebaseCore
import FirebaseAuth
import FirebaseFirestore

// Admin access, matching Android exactly: ONE account (the board's) may write. The check below only
// shows or hides buttons — the real boundary is server-side in the Firestore rules, so a tampered
// client still cannot write. See docs/ios/FIREBASE-HANDOFF.md §1.
@MainActor
final class AdminStore: ObservableObject {
    static let shared = AdminStore()

    /// The board's account. Fixed by the deployed Firestore rules; not a secret.
    static let adminUID = "1a7xqRgIYDR0RZqa3KghBlz98PK2"

    @Published private(set) var isAdmin = false

    private var handle: AuthStateDidChangeListenerHandle?

    private init() {}

    /// Must NOT run from the singleton's initializer: touching `Auth.auth()` the first time hops
    /// back onto the main thread itself, which deadlocks a @MainActor init and hangs the whole UI
    /// on a white screen. Called from the first view that appears instead.
    func start() {
        guard FirebaseApp.app() != nil, handle == nil else { return }
        isAdmin = Auth.auth().currentUser?.uid == Self.adminUID
        // Firebase restores the session across launches, so a signed-in admin stays signed in.
        handle = Auth.auth().addStateDidChangeListener { [weak self] _, user in
            Task { @MainActor in self?.isAdmin = user?.uid == Self.adminUID }
            Self.ensureSignedIn()
        }
        Self.ensureSignedIn()
    }

    /// Sorgt dafür, dass das Gerät immer eine Firebase-Identität hat, und meldet es sonst anonym an
    /// — wie `SessionManager` auf Android.
    ///
    /// Grund sind die Reaktionen: Ein Herz muss jemandem gehören, sonst könnte dasselbe Gerät
    /// hundertmal tippen und nichts ließe sich zurücknehmen. Der Nutzer merkt davon nichts, es gibt
    /// keine Anmeldung zu sehen. Läuft auch nach dem Abmelden eines Admins wieder an, damit die
    /// Reaktionsknöpfe nicht ohne sichtbaren Grund tot sind.
    private static func ensureSignedIn() {
        guard FirebaseApp.app() != nil, Auth.auth().currentUser == nil else { return }
        Auth.auth().signInAnonymously { _, error in
            if let error {
                // Wahrscheinlichste Ursache: anonyme Anmeldung ist in der Firebase-Konsole nicht
                // freigeschaltet. Alles andere funktioniert weiter, nur die Reaktionen bleiben
                // stumm — eine Protokollzeile wert, kein Absturz und keine Meldung an den Nutzer.
                NSLog("[Auth] Anonyme Anmeldung fehlgeschlagen, Reaktionen nicht verfügbar: \(error.localizedDescription)")
            }
        }
    }

    /// nil = signed in as the board's admin. Otherwise the reason, so the sheet can say what really
    /// went wrong instead of a blanket "failed" (wrong password vs. no network vs. wrong account).
    func signIn(email: String, password: String) async -> String? {
        guard FirebaseApp.app() != nil else { return "Firebase not configured" }
        let result: AuthDataResult
        do {
            result = try await Auth.auth().signIn(withEmail: email, password: password)
        } catch {
            // The plain message collapses very different causes into one sentence. Report the code,
            // the domain and the server's own response so the real cause is visible.
            let e = error as NSError
            var parts = ["\(e.domain) code \(e.code)", e.localizedDescription]
            if let server = e.userInfo["FIRAuthErrorUserInfoDeserializedResponseKey"] {
                parts.append("Server: \(server)")
            }
            if let underlying = e.userInfo[NSUnderlyingErrorKey] as? NSError {
                parts.append("Intern: \(underlying.domain) \(underlying.code) \(underlying.localizedDescription)")
            }
            return parts.joined(separator: "\n")
        }
        guard result.user.uid == Self.adminUID else {
            // A valid account that isn't the board's gets no rights — don't leave it signed in.
            try? Auth.auth().signOut()
            return "Signed in as \(result.user.email ?? "?") (uid \(result.user.uid)) — not the admin account"
        }
        return nil
    }

    func signOut() {
        try? Auth.auth().signOut()
    }



    // MARK: - Writes (admin only; rejected server-side otherwise)

    /// Saves the community rules the whole community reads (`communities/{id}/config/rules`).
    func saveRule(_ rule: CommunityRule) async -> Bool {
        guard FirebaseApp.app() != nil else { return false }
        var data: [String: Any] = [
            "fajrIqamah": rule.fajrIqamah,
            "jumua": rule.jumua,
            "dhuhrOffsetMin": rule.dhuhrOffsetMin,
            "asrOffsetMin": rule.asrOffsetMin,
            "maghribOffsetMin": rule.maghribOffsetMin,
            "ishaOffsetMin": rule.ishaOffsetMin,
            "updatedAt": Int64(Date().timeIntervalSince1970 * 1000),
        ]
        // Both Eid fields appear and disappear together; deleting clears the banner everywhere.
        if let b = rule.bajram {
            data["bajramDate"] = b.date
            data["bajramTime"] = b.time
        } else {
            data["bajramDate"] = FieldValue.delete()
            data["bajramTime"] = FieldValue.delete()
        }
        do {
            try await Community.rules
                .setData(data, merge: true)
            return true
        } catch {
            return false
        }
    }

    /// Posts an announcement. `titleByLang`/`bodyByLang` are the translated maps; the image (if any)
    /// goes into a separate `news_images` document so the feed stays light.
    func postNews(titleByLang: [String: String], bodyByLang: [String: String],
                  sourceLang: String, imageJPEG: Data?) async -> Bool {
        guard FirebaseApp.app() != nil else { return false }
        let db = Firestore.firestore()
        let doc = Community.news.document()
        do {
            try await doc.setData([
                "title": titleByLang,
                "body": bodyByLang,
                "sourceLang": sourceLang,
                "createdAt": Int64(Date().timeIntervalSince1970 * 1000),
                "hasImage": imageJPEG != nil,
            ])
            if let imageJPEG {
                try await Community.newsImages.document(doc.documentID)
                    .setData(["data": imageJPEG.base64EncodedString()])
            }
            return true
        } catch {
            return false
        }
    }

    /// Deletes an announcement and its image. The image delete is best-effort: a leftover image
    /// document without its announcement is invisible to readers.
    func deleteNews(_ id: String) async -> Bool {
        guard FirebaseApp.app() != nil else { return false }
        let db = Firestore.firestore()
        do {
            try await Community.news.document(id).delete()
            try? await Community.newsImages.document(id).delete()
            return true
        } catch {
            return false
        }
    }
}
