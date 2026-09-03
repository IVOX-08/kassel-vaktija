import Foundation
import FirebaseFirestore

// Am 26.08.2026 hat die Android-Seite die Datenbank von EINER Gemeinde auf ZWANZIG umgebaut
// (docs/multi-gemeinde/FUER-DIE-IOS-APP.md). Alles, was vorher flach lag, hängt jetzt unter
// `communities/{id}`. Diese App hat die Gemeindeauswahl inzwischen ebenfalls; welche Gemeinde
// gerade gilt, steht in CommunitySelection und damit auch im Widget und im Hintergrund.
//
// Warum an einer Stelle: vorher standen dieselben Pfade als Zeichenketten in vier Dateien. Beim
// nächsten Umzug wäre wieder jede einzeln zu finden.
enum Community {
    /// Die aktuell gewählte Gemeinde. War bis zum 26.08.2026 eine Konstante auf Kassel — bei
    /// einundachtzig Gemeinden ist sie eine Auswahl des Nutzers.
    static var id: String { CommunitySelection.communityId }

    private static var db: Firestore { Firestore.firestore() }

    static var root: DocumentReference {
        db.collection("communities").document(id)
    }

    /// Iqamah, Jumu'ah und die Bajram-Ankündigung dieser Gemeinde.
    /// Früher `config/community` — das Dokument existiert weiter, wird aber nur noch von der
    /// veröffentlichten Android-Version 1.1.3 gelesen und darf nicht gelöscht werden.
    static var rules: DocumentReference {
        root.collection("config").document("rules")
    }

    static var news: CollectionReference { root.collection("news") }
    static var newsImages: CollectionReference { root.collection("news_images") }

    /// Verbandsweite Mitteilungen des Hauptadministrators — dieselben für alle zwanzig Gemeinden.
    static var broadcasts: CollectionReference { db.collection("broadcasts") }
    static var broadcastImages: CollectionReference { db.collection("broadcast_images") }
}
