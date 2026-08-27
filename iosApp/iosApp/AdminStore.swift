import Foundation
import FirebaseCore
import FirebaseAuth
import FirebaseFirestore

// Was das angemeldete Konto darf — eins zu eins wie Androids `AdminRole`.
//
// Die Rechte sind bewusst KEINE Leiter mit dem Hauptadministrator oben. Es sind zwei verschiedene
// Aufgaben: eine Gemeinde führt ihr eigenes Gebetsleben, der Betreiber führt das Programm. Der
// Hauptadministrator erreicht jede Gemeinde mit einer Mitteilung und kann eine abschalten, aber er
// rührt niemandes Iqamah an — das ist die religiöse Entscheidung der Gemeinde, und ein Fremder,
// der sie ändert, läge falsch, selbst wenn es praktisch wäre.
enum AdminRole: Equatable {
    /// Abgemeldet, oder ein Konto ohne Eintrag unter `admins/{uid}`.
    case none
    /// Führt genau eine Gemeinde: deren Gebetszeiten und deren Mitteilungen. Sonst nichts.
    case community(String)
    /// Führt das Programm. Mitteilungen an alle, entscheidet über Zugänge — verwaltet aber
    /// niemandes Gebetszeiten.
    case head

    /// Darf Iqamah und Jumu'ah DIESER Gemeinde ändern. Nur deren eigener Admin, nie der Hauptadmin.
    func canEditTimes(_ communityId: String) -> Bool {
        if case .community(let own) = self { return own == communityId }
        return false
    }

    /// Darf Mitteilungen DIESER Gemeinde schreiben und löschen. Ebenfalls nur ihr eigener Admin.
    func canPostNews(_ communityId: String) -> Bool { canEditTimes(communityId) }

    /// Darf an alle Gemeinden gleichzeitig verkünden.
    var canBroadcast: Bool { self == .head }

    /// Ob dieses Konto überhaupt Verwaltungsrechte hat — steuert den Abschnitt in den Einstellungen.
    var isAdmin: Bool { self != .none }

    /// Alles Unbekannte bedeutet kein Zugang.
    static func from(role: String?, communityId: String?) -> AdminRole {
        switch role?.lowercased() {
        case "head": return .head
        case "community":
            guard let id = communityId, !id.isEmpty else { return .none }
            return .community(id)
        default: return .none
        }
    }
}

/// Was bei einem Anmeldeversuch herausgekommen ist.
enum AdminSignInOutcome {
    case success(AdminRole)
    /// Gültiges Admin-Konto, aber für eine andere Gemeinde. Die Sitzung bleibt bestehen.
    case wrongCommunity(own: String, attempted: String)
    /// Angemeldet, aber ohne Eintrag unter `admins/{uid}`. Wieder abgemeldet.
    case noRights
    /// Falsches Passwort, keine Verbindung und so weiter.
    case failed(String)
}

// Admin-Zugang. Die Rolle steht in Firestore unter `admins/{uid}`, nicht im Code: Gemeinden kommen
// und gehen, und ihre Vorstände wechseln mit jedem Ausschuss — einen Zugang zu vergeben muss eine
// Änderung in der Konsole sein, keine neue App-Version. Die Sicherheitsregeln lesen dasselbe
// Dokument, der Server setzt also genau das durch, was die Oberfläche zeigt.
//
// Vorher stand hier EINE feste Kennung. Seit dem Umbau auf zwanzig Gemeinden (26.08.2026) wäre das
// falsch: der Vorstand hat eine eigene Kennung, und das Besitzer-Konto ist `head` und darf gerade
// NICHT die Zeiten einer Gemeinde ändern.
@MainActor
final class AdminStore: ObservableObject {
    static let shared = AdminStore()

    @Published private(set) var role: AdminRole = .none

    /// Bequemlichkeiten für die Oberfläche — diese App zeigt immer auf Kassel.
    var isAdmin: Bool { role.isAdmin }
    var canEditTimes: Bool { role.canEditTimes(Community.id) }
    var canPostNews: Bool { role.canPostNews(Community.id) }
    var canBroadcast: Bool { role.canBroadcast }

    private var handle: AuthStateDidChangeListenerHandle?
    private var roleListener: ListenerRegistration?

    private init() {}

    /// Must NOT run from the singleton's initializer: touching `Auth.auth()` the first time hops
    /// back onto the main thread itself, which deadlocks a @MainActor init and hangs the whole UI
    /// on a white screen. Called from the first view that appears instead.
    func start() {
        guard FirebaseApp.app() != nil, handle == nil else { return }
        // Firebase restores the session across launches, so a signed-in admin stays signed in.
        handle = Auth.auth().addStateDidChangeListener { [weak self] _, user in
            Task { @MainActor in self?.watchRole(of: user?.uid) }
            Self.ensureSignedIn()
        }
        watchRole(of: Auth.auth().currentUser?.uid)
        Self.ensureSignedIn()
    }

    /// Liest die Rolle live aus `admins/{uid}` — ändert der Hauptadministrator einen Zugang in der
    /// Konsole, wirkt das ohne Neustart und ohne neue App-Version.
    private func watchRole(of uid: String?) {
        roleListener?.remove()
        roleListener = nil
        guard let uid else {
            role = .none
            return
        }
        roleListener = Firestore.firestore().collection("admins").document(uid)
            .addSnapshotListener { [weak self] snapshot, _ in
                let data = snapshot?.data()
                let parsed = AdminRole.from(role: data?["role"] as? String,
                                            communityId: data?["communityId"] as? String)
                Task { @MainActor in self?.role = parsed }
            }
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

    /// Meldet an und sagt, was herausgekommen ist.
    ///
    /// „Falsche Gemeinde" ist bewusst etwas anderes als „falsches Passwort": Das Konto ist echt, es
    /// verwaltet nur woanders. Das klar zu sagen erspart eine Runde ratloser Passwort-Zurücksetzung
    /// — und es ist der Moment, den der Hauptadministrator gemeldet bekommen wollte, denn dass die
    /// Zugangsdaten eines Admins bei einer fremden Gemeinde probiert werden, ist genau das.
    func signIn(email: String, password: String) async -> AdminSignInOutcome {
        guard FirebaseApp.app() != nil else { return .failed("Firebase not configured") }
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
            return .failed(parts.joined(separator: "\n"))
        }
        let uid = result.user.uid
        let doc = try? await Firestore.firestore().collection("admins").document(uid).getDocument()
        let signedInRole = AdminRole.from(role: doc?.data()?["role"] as? String,
                                          communityId: doc?.data()?["communityId"] as? String)
        switch signedInRole {
        case .none:
            // Echtes Konto, aber ohne Verwaltungsrechte — nicht angemeldet stehen lassen.
            try? Auth.auth().signOut()
            return .noRights
        case .community(let own) where own != Community.id:
            // Sitzung BLEIBT: Das Konto ist ein gültiger Admin, nur nicht hier.
            Self.recordWrongCommunityLogin(uid: uid, own: own, attempted: Community.id)
            return .wrongCommunity(own: own, attempted: Community.id)
        default:
            return .success(signedInRole)
        }
    }

    /// Meldet einen Anmeldeversuch bei einer fremden Gemeinde.
    ///
    /// Keine Sicherheitsmaßnahme — wer böse will, schreibt sie einfach nicht. Es ist ein Vermerk für
    /// den ehrlichen Irrtum und eine Spur für den unehrlichen. Absichtlich nicht abgewartet:
    /// Protokollieren darf eine Anmeldung nie aufhalten.
    private static func recordWrongCommunityLogin(uid: String, own: String, attempted: String) {
        Firestore.firestore().collection("admin_alerts").document().setData([
            "type": "wrong_community",
            "uid": uid,
            "ownCommunityId": own,
            "attemptedCommunityId": attempted,
            "createdAt": Int64(Date().timeIntervalSince1970 * 1000),
        ]) { _ in }
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
    /// goes into a separate images document so the feed stays light.
    ///
    /// `broadcast` schreibt stattdessen nach `broadcasts` — die Mitteilung des Hauptadministrators
    /// an alle Gemeinden. Die Regeln lassen das nur für `role: 'head'` zu.
    /// `audience` gilt nur für verbandsweite Mitteilungen: leer heißt alle Gemeinden.
    func postNews(titleByLang: [String: String], bodyByLang: [String: String],
                  sourceLang: String, imageJPEG: Data?, broadcast: Bool = false,
                  audience: [String] = []) async -> Bool {
        guard FirebaseApp.app() != nil else { return false }
        let collection = broadcast ? Community.broadcasts : Community.news
        let images = broadcast ? Community.broadcastImages : Community.newsImages
        let doc = collection.document()
        do {
            // Feldnamen und Vorbelegungen wie auf Android — beide Apps lesen dieselben Dokumente.
            var data: [String: Any] = [
                "title": titleByLang,
                "body": bodyByLang,
                "sourceLang": sourceLang,
                "createdAt": Int64(Date().timeIntervalSince1970 * 1000),
                "hasImage": imageJPEG != nil,
                "audience": broadcast ? audience : [],
                "likeCount": 0,
                "dislikeCount": 0,
            ]
            if !broadcast { data["communityId"] = Community.id }
            try await doc.setData(data)
            if let imageJPEG {
                try await images.document(doc.documentID)
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
        do {
            try await Community.news.document(id).delete()
            try? await Community.newsImages.document(id).delete()
            return true
        } catch {
            return false
        }
    }
}
