import Foundation
import FirebaseCore
import FirebaseFirestore

// Das mitgelieferte Verzeichnis in die Datenbank schreiben.
//
// Einundachtzig Gemeinden mit verschachtelten Ortslisten von Hand in die Konsole zu tippen ist eine
// Stunde fehleranfaellige Arbeit — und die Daten stecken schon in der App.
//
// NUR IN DER ENTWICKLUNGSFASSUNG, genau wie auf Android. In einer veroeffentlichten App koennte
// ein Fehlgriff das lebende Verzeichnis mit dem ueberschreiben, was zufaellig einkompiliert war.
enum CommunityImport {

    /// Schreibt das Verzeichnis. Gibt zurueck, wie viele Dokumente geschrieben wurden.
    static func run() async -> Int {
        guard FirebaseApp.app() != nil else { return 0 }
        let db = Firestore.firestore()
        var written = 0

        for community in bundled() {
            var data: [String: Any] = [
                "name": community.name,
                "locations": community.locations.map { location in
                    [
                        "id": location.id,
                        "name": location.name,
                        "vaktijaSlug": location.vaktijaSlug,
                        "latitude": location.latitude,
                        "longitude": location.longitude,
                    ] as [String: Any]
                },
            ]
            if let v = community.address { data["address"] = v }
            if let v = community.facebookUrl { data["facebookUrl"] = v }
            if let v = community.instagramUrl { data["instagramUrl"] = v }
            if let v = community.youtubeUrl { data["youtubeUrl"] = v }
            if let v = community.donationUrl { data["donationUrl"] = v }
            if let v = community.logoUrl { data["logoUrl"] = v }
            // Kontakt und Imam. Diese fuenf Felder sind der Grund, warum eine neue Gemeinde
            // kuenftig ohne App-Update auskommt: Der Vorstand traegt sie in Firestore ein, und die
            // Kontaktkarte zeigt sie. `merge: true` sorgt dafuer, dass ein spaeter dort
            // eingetragener Wert von einem erneuten Import nicht ueberschrieben wird — der Import
            // schickt nur, was im Paket steht, und das Paket kennt keine leeren Felder.
            if let v = community.phone { data["phone"] = v }
            if let v = community.email { data["email"] = v }
            if let v = community.website { data["website"] = v }
            if let v = community.imamName { data["imamName"] = v }
            if let v = community.imamPhone { data["imamPhone"] = v }

            let document = db.collection("communities").document(community.id)
            guard let snapshot = try? await document.getDocument() else { continue }

            // Der Status wird NUR fuer eine Gemeinde geschrieben, die die Datenbank noch nie
            // gesehen hat. Ihn bei jedem Import mitzuschicken schaltete eine vom
            // Hauptadministrator gesperrte Gemeinde wieder ein — sie stuende wieder in der Auswahl
            // und veroeffentlichte Gebetszeiten in ihrem Namen, ohne dass irgendetwas auf dem
            // Bildschirm zeigte, dass ein Routine-Import das getan hat.
            //
            // Und `merge`, damit ein Dokument, das inzwischen ein Logo, einen Spendenlink oder
            // einen Status bekommen hat, nicht auf den Auslieferungsstand zurueckfaellt.
            if !snapshot.exists { data["status"] = community.status ?? "suspended" }

            guard (try? await document.setData(data, merge: true)) != nil else { continue }
            await seedRules(document, community)
            written += 1
        }
        return written
    }

    /// Ikamet und Dzuma der Gemeinde — aber nur, wenn sie noch keine hat.
    ///
    /// Ein erneuter Import darf die Eintraege eines Admins niemals rueckgaengig machen: Eine
    /// Gemeinde, die inzwischen ihre echte Dzuma gesetzt hat, wuerde sonst still auf Kassels 15:00
    /// zurueckgeschoben — und niemand saehe es, bis Leute zur falschen Zeit vor der Tuer stehen.
    private static func seedRules(_ document: DocumentReference, _ community: CommunityInfo) async {
        let ref = document.collection("config").document("rules")
        guard let snapshot = try? await ref.getDocument(), !snapshot.exists else { return }
        // Einzeln zusammengesetzt statt als ein grosses Literal: Swift braucht sonst zu lange,
        // um die Typen eines gemischten Woerterbuchs mit sieben `??` herzuleiten.
        var rules: [String: Any] = [:]
        rules["fajrIqamah"] = community.fajrIqamah ?? "04:30"
        rules["jumua"] = community.jumua ?? "13:30"
        rules["dhuhrOffsetMin"] = community.dhuhrOffsetMin ?? 10
        rules["asrOffsetMin"] = community.asrOffsetMin ?? 10
        rules["maghribOffsetMin"] = community.maghribOffsetMin ?? 5
        rules["ishaOffsetMin"] = community.ishaOffsetMin ?? 0
        rules["updatedAt"] = Int64(Date().timeIntervalSince1970 * 1000)
        try? await ref.setData(rules)
    }

    /// Entfernt die Dokumente, die das ERSTE Verzeichnis hinterlassen hat.
    ///
    /// Jenes stammte aus den Impressen der Gemeinden und gab ihnen Kennungen wie `hagen-e-v`; das
    /// jetzige stammt aus IGBDs Register und nennt dieselbe Gemeinde `dzemat-hagen`. Beide Importe
    /// zusammen haben neunzehn Gemeinden doppelt angelegt — unsichtbar, solange alles
    /// abgeschaltet ist, und zwei gleich aussehende Zeilen in dem Moment, in dem eine
    /// freigeschaltet wird.
    ///
    /// Die Kennungen stehen als feste Liste da und nicht als Regel „loesche alles, was nicht im
    /// Verzeichnis steht": Eine spaeter direkt in der Konsole angelegte Gemeinde stuende dort
    /// ebenfalls nicht, und ein Aufraeumen darf so eine niemals loeschen koennen.
    static func removeSuperseded() async -> Int {
        guard FirebaseApp.app() != nil else { return 0 }
        let db = Firestore.firestore()
        var deleted = 0
        for id in supersededIDs {
            let ref = db.collection("communities").document(id)
            guard let snapshot = try? await ref.getDocument(), snapshot.exists else { continue }
            guard (try? await ref.delete()) != nil else { continue }
            deleted += 1
        }
        return deleted
    }

    private static let supersededIDs = [
        "boeblingen-sindelfingen-e-v",
        "bosniakisch-deutsche-gemeinde-karlsruhe-e-v",
        "duisburg-e-v",
        "dzemat-bkc-siegen-e-v",
        "dzemat-essen-e-v",
        "dzemat-ikre-berlin-e-v",
        "gemeinde-bosnischer-moslems-e-v-dzemat-bremen",
        "hagen-e-v",
        "hannover-e-v",
        "islamische-gemeinschaft-bih-aachen",
        "mainz-e-v",
        "mannheim-e-v",
        "nuernberg-e-v",
        "oberhausen-e-v",
        "offenburg-e-v",
        "rosenheim-e-v",
        "ulm-e-v",
        "witten-e-v",
        "wuppertal-e-v",
    ]

    private static func bundled() -> [CommunityInfo] {
        guard let url = Bundle.main.url(forResource: "communities", withExtension: "json"),
              let data = try? Data(contentsOf: url),
              let list = try? JSONDecoder().decode([CommunityInfo].self, from: data) else { return [] }
        return list
    }
}
