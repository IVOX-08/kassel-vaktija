import SwiftUI
import FirebaseFirestore

// Was der Hauptadministrator sieht: die Liste aller Gemeinden und ihren Zustand.
//
// Die drei Zustände sind bewusst abgestuft und in dieser Reihenfolge erklärt, wie auf Android:
//   Aktiv          — alles läuft normal
//   Eingeschränkt  — nicht in der Auswahl, kein Logo, keine Spenden, keine Mitteilungen,
//                    ABER die Gebetszeiten laufen weiter
//   Gesperrt       — die App zeigt nur noch einen Hinweis, rücknehmbar
//
// Die mittlere Stufe ist die wichtige: Eine Gemeinde, die nicht bezahlt hat, verliert ihre
// Sichtbarkeit im Verband — aber niemandem werden deshalb die Gebetszeiten abgedreht. Wer sich auf
// die App verlässt, um zum Gebet zu kommen, soll nicht zwischen Verband und Gemeinde geraten.
struct HeadAdminSection: View {
    @ObservedObject private var catalog = CommunityCatalog.shared
    @ObservedObject private var admin = AdminStore.shared

    @State private var query = ""
    @State private var pending: CommunityInfo?
    @State private var busy = false
    @State private var failed: String?
    /// Das Ergebnis des letzten Wartungslaufs — „81 Gemeinden geschrieben."
    @State private var maintenance: String?

    /// Ein Wartungsknopf: laeuft, meldet die Zahl, sperrt sich waehrenddessen.
    @ViewBuilder private func maintenanceButton(_ title: String, _ doneFormat: String,
                                                action: @escaping () async -> Int) -> some View {
        Button {
            busy = true
            Task {
                let count = await action()
                maintenance = String(format: doneFormat, count)
                busy = false
            }
        } label: {
            HStack {
                Text(title).font(.inter(15, .medium)).foregroundColor(.brandGreen)
                Spacer()
                if busy { ProgressView().controlSize(.small) }
            }
        }
        .buttonStyle(.plain)
        .disabled(busy)
    }

    private var matches: [CommunityInfo] {
        let q = query.trimmingCharacters(in: .whitespaces).lowercased()
        guard !q.isEmpty else { return catalog.all }
        return catalog.all.filter { c in
            c.name.lowercased().contains(q)
                || c.locations.contains { $0.name.lowercased().contains(q) }
        }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(L("admin_head_header"))
                .font(.inter(13, .semibold)).foregroundColor(.appSecondary)
                .padding(.leading, 4)
            SettingCard {
                Text(L("admin_manage_communities"))
                    .font(.inter(15, .semibold)).foregroundColor(.appOnSurface)
                searchField
                ForEach(matches) { community in
                    Button { pending = community } label: { row(community) }
                        .buttonStyle(.plain)
                }
                if let failed {
                    Text(failed).font(.inter(12)).foregroundColor(.qiblaRed)
                }
                #if DEBUG
                // NUR in der Entwicklungsfassung, genau wie auf Android: In einer
                // veroeffentlichten App koennte ein Fehlgriff das lebende Verzeichnis mit dem
                // ueberschreiben, was zufaellig einkompiliert war.
                Divider()
                maintenanceButton(L("admin_import_communities"), L("admin_import_done"),
                                  action: CommunityImport.run)
                maintenanceButton(L("admin_cleanup_communities"), L("admin_cleanup_done"),
                                  action: CommunityImport.removeSuperseded)
                if let maintenance {
                    Text(maintenance).font(.inter(12)).foregroundColor(.appOnSurfaceVariant)
                }
                #endif
                Divider()
                Button(L("admin_sign_out")) { admin.signOut() }
                    .font(.inter(15, .semibold)).foregroundColor(.brandGreen)
            }
        }
        .confirmationDialog(pending?.name ?? "", isPresented: .init(
            get: { pending != nil }, set: { if !$0 { pending = nil } }
        ), titleVisibility: .visible) {
            if let community = pending {
                Button(L("community_status_active")) { setStatus(community, "active") }
                Button(L("community_status_suspended")) { setStatus(community, "suspended") }
                Button(L("community_status_blocked"), role: .destructive) {
                    setStatus(community, "blocked")
                }
                Button(L("action_cancel"), role: .cancel) { pending = nil }
            }
        } message: {
            // Die Folgen jeder Stufe, damit niemand aus Versehen eine Gemeinde abschaltet.
            Text("\(L("community_status_active_hint"))\n\n\(L("community_status_suspended_hint"))\n\n\(L("community_status_blocked_hint"))")
        }
        .onAppear { catalog.start() }
    }

    private var searchField: some View {
        HStack(spacing: 8) {
            Image(systemName: "magnifyingglass").foregroundColor(.appOnSurfaceVariant)
            TextField(L("community_search_hint"), text: $query).font(.inter(14))
        }
        .padding(10)
        .background(Color.appBackground)
        .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
    }

    private func row(_ c: CommunityInfo) -> some View {
        HStack(spacing: 10) {
            VStack(alignment: .leading, spacing: 1) {
                Text(c.name).font(.inter(14, .medium)).foregroundColor(.appOnSurface)
                    .fixedSize(horizontal: false, vertical: true)
                    .multilineTextAlignment(.leading)
                if let place = c.primaryLocation {
                    Text(place.name).font(.inter(12)).foregroundColor(.appOnSurfaceVariant)
                }
            }
            Spacer()
            statusBadge(c)
        }
        .padding(.vertical, 6)
    }

    private func statusBadge(_ c: CommunityInfo) -> some View {
        let (text, color): (String, Color) =
            c.isBlocked ? (L("community_status_blocked"), .qiblaRed)
            : c.isSuspended ? (L("community_status_suspended"), .appSecondary)
            : (L("community_status_active"), .brandGreen)
        return Text(text)
            .font(.inter(11, .semibold)).foregroundColor(color)
            .padding(.horizontal, 8).padding(.vertical, 3)
            .background(color.opacity(0.12)).clipShape(Capsule())
    }

    /// Schreibt nur das eine Feld. Ein `setData` mit merge würde bei einem Tippfehler im Code den
    /// Rest des Dokuments überschreiben — die Adresse einer Gemeinde ist nichts, was ein
    /// Statuswechsel anfassen darf.
    private func setStatus(_ c: CommunityInfo, _ status: String) {
        guard admin.canBroadcast else { return }   // nur der Hauptadministrator
        busy = true
        failed = nil
        Firestore.firestore().collection("communities").document(c.id)
            .updateData(["status": status]) { error in
                Task { @MainActor in
                    busy = false
                    pending = nil
                    if let error { failed = error.localizedDescription }
                }
            }
    }
}
