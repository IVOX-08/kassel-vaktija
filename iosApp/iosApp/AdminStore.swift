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

    /// Saves the community rules the whole community reads (`config/community`).
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
            try await Firestore.firestore().collection("config").document("community")
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
        let doc = db.collection("news").document()
        do {
            try await doc.setData([
                "title": titleByLang,
                "body": bodyByLang,
                "sourceLang": sourceLang,
                "createdAt": Int64(Date().timeIntervalSince1970 * 1000),
                "hasImage": imageJPEG != nil,
            ])
            if let imageJPEG {
                try await db.collection("news_images").document(doc.documentID)
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
            try await db.collection("news").document(id).delete()
            try? await db.collection("news_images").document(id).delete()
            return true
        } catch {
            return false
        }
    }
}
