import SwiftUI

// „Wer bekommt die Mitteilung?" — der Empfängerkreis einer verbandsweiten Mitteilung.
//
// Nichts angekreuzt heißt ALLE Gemeinden, und genau so wird es gespeichert: als leere Liste.
// Das ist keine Bequemlichkeit, sondern der Vertrag mit der Android-App und mit allen Mitteilungen,
// die schon in der Datenbank liegen — dort steht bei „an alle" ebenfalls eine leere Liste. Wer
// stattdessen alle Kennungen einzeln einträgt, erreicht keine Gemeinde, die später dazukommt.
struct AudiencePickerView: View {
    /// Vorbelegung und Ergebnis. Leer = alle.
    @Binding var selection: Set<String>
    let onDone: () -> Void

    @ObservedObject private var catalog = CommunityCatalog.shared
    @Environment(\.dismiss) private var dismiss
    @State private var query = ""

    private var matches: [CommunityInfo] {
        let q = query.trimmingCharacters(in: .whitespaces).lowercased()
        guard !q.isEmpty else { return catalog.selectable }
        return catalog.selectable.filter { c in
            c.name.lowercased().contains(q)
                || c.locations.contains { $0.name.lowercased().contains(q) }
        }
    }

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                searchField
                ScrollView {
                    LazyVStack(spacing: 8) {
                        ForEach(matches) { community in
                            Button { toggle(community.id) } label: { row(community) }
                                .buttonStyle(.plain)
                        }
                    }
                    .padding(.horizontal, 16).padding(.vertical, 12)
                }
                footer
            }
            .background(Color.appBackground.ignoresSafeArea())
            .navigationTitle(L("broadcast_audience_title"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(L("action_cancel")) { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    // „Alle" leert die Auswahl — die leere Liste IST „an alle".
                    Button(L("broadcast_audience_all")) { selection.removeAll() }
                }
            }
        }
        .tint(.brandGreen)
        .onAppear { catalog.start() }
    }

    private var searchField: some View {
        HStack(spacing: 8) {
            Image(systemName: "magnifyingglass").foregroundColor(.appOnSurfaceVariant)
            TextField(L("community_search_hint"), text: $query).font(.inter(15))
        }
        .padding(12)
        .background(Color.appSurface)
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
        .padding(.horizontal, 16).padding(.top, 12)
    }

    private var footer: some View {
        VStack(spacing: 8) {
            // Sagt in Worten, was gerade gilt — ein leerer Haken-Zustand ist sonst zweideutig.
            Text(selection.isEmpty
                 ? L("broadcast_audience_everyone")
                 : String(format: L("broadcast_audience_count"), selection.count))
                .font(.inter(13)).foregroundColor(.appOnSurfaceVariant)
            Button {
                onDone()
                dismiss()
            } label: {
                Text(L("action_confirm"))
                    .font(.inter(15, .semibold)).foregroundColor(.white)
                    .frame(maxWidth: .infinity).padding(.vertical, 12)
                    .background(Color.brandGreen)
                    .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
            }
        }
        .padding(16)
        .background(Color.appSurface)
    }

    private func row(_ c: CommunityInfo) -> some View {
        HStack(spacing: 12) {
            Image(systemName: selection.contains(c.id) ? "checkmark.square.fill" : "square")
                .font(.system(size: 19))
                .foregroundColor(selection.contains(c.id) ? .brandGreen : .appOnSurfaceVariant)
            VStack(alignment: .leading, spacing: 1) {
                Text(c.name).font(.inter(14, .medium)).foregroundColor(.appOnSurface)
                    .fixedSize(horizontal: false, vertical: true)
                    .multilineTextAlignment(.leading)
                if let place = c.primaryLocation {
                    Text(place.name).font(.inter(12)).foregroundColor(.appOnSurfaceVariant)
                }
            }
            Spacer()
        }
        .padding(12)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.appSurface)
        .clipShape(RoundedRectangle(cornerRadius: Radius.smallCard, style: .continuous))
    }

    private func toggle(_ id: String) {
        if selection.contains(id) { selection.remove(id) } else { selection.insert(id) }
    }
}
