import SwiftUI

// Die Gemeindeauswahl — wie auf Android zweistufig: erst die Gemeinde, dann der Ort, falls eine
// Gemeinde mehrere hat.
//
// Gesucht wird über Name UND Ort. Wer die App bekommt, kennt oft nur seine Stadt, nicht den
// eingetragenen Vereinsnamen ("IGBD-Džemat BKC Siegen e.V." findet man über "Siegen").
//
// Gesperrte und eingeschränkte Gemeinden stehen NICHT in der Liste: eine Gemeinde, die nicht
// teilnimmt, soll auch nicht wählbar sein. Ihre Gebetszeiten laufen für Bestandsnutzer weiter,
// das entscheidet der Hauptadministrator, nicht diese Ansicht.
struct CommunityPickerView: View {
    @ObservedObject private var catalog = CommunityCatalog.shared
    @Environment(\.dismiss) private var dismiss

    @State private var query = ""
    /// Gesetzt, sobald eine Gemeinde mit mehreren Orten gewählt wurde.
    @State private var pendingLocations: CommunityInfo?

    private var matches: [CommunityInfo] {
        let q = query.trimmingCharacters(in: .whitespaces).lowercased()
        guard !q.isEmpty else { return catalog.selectable }
        return catalog.selectable.filter { c in
            c.name.lowercased().contains(q)
                || (c.address ?? "").lowercased().contains(q)
                || c.locations.contains { $0.name.lowercased().contains(q) }
        }
    }

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                searchField
                if matches.isEmpty {
                    Spacer()
                    Text(L("community_none_found"))
                        .font(.inter(15)).foregroundColor(.appOnSurfaceVariant)
                        .multilineTextAlignment(.center).padding(.horizontal, 32)
                    Spacer()
                } else {
                    ScrollView {
                        LazyVStack(spacing: 10) {
                            ForEach(matches) { community in
                                Button { pick(community) } label: { row(community) }
                                    .buttonStyle(.plain)
                            }
                        }
                        .padding(.horizontal, 16).padding(.vertical, 12)
                    }
                }
            }
            .background(Color.appBackground.ignoresSafeArea())
            .navigationTitle(L("community_picker_title"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(L("action_cancel")) { dismiss() }
                }
            }
            .sheet(item: $pendingLocations) { community in
                LocationPickerView(community: community) { location in
                    catalog.choose(community, location: location)
                    pendingLocations = nil
                    dismiss()
                }
            }
        }
        .tint(.brandGreen)
        .onAppear { catalog.start() }
    }

    private var searchField: some View {
        HStack(spacing: 8) {
            Image(systemName: "magnifyingglass").foregroundColor(.appOnSurfaceVariant)
            TextField(L("community_search_hint"), text: $query)
                .font(.inter(15))
                .autocorrectionDisabled()
        }
        .padding(12)
        .background(Color.appSurface)
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
        .overlay(RoundedRectangle(cornerRadius: 12, style: .continuous)
            .stroke(Color.appOnSurfaceVariant.opacity(0.3), lineWidth: 1))
        .padding(.horizontal, 16).padding(.top, 12)
    }

    private func row(_ c: CommunityInfo) -> some View {
        HStack(spacing: 12) {
            Image(systemName: "mappin.and.ellipse")
                .font(.system(size: 17)).foregroundColor(.brandGreen).frame(width: 26)
            VStack(alignment: .leading, spacing: 2) {
                Text(c.name).font(.inter(15, .semibold)).foregroundColor(.appOnSurface)
                    .fixedSize(horizontal: false, vertical: true)
                    .multilineTextAlignment(.leading)
                if let place = c.primaryLocation {
                    Text(place.name).font(.inter(13)).foregroundColor(.appOnSurfaceVariant)
                }
            }
            Spacer()
            if c.id == catalog.selected?.id {
                Image(systemName: "checkmark").font(.system(size: 15, weight: .semibold))
                    .foregroundColor(.brandGreen)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(14)
        .background(Color.appSurface)
        .clipShape(RoundedRectangle(cornerRadius: Radius.smallCard, style: .continuous))
    }

    private func pick(_ c: CommunityInfo) {
        // Zweiter Schritt nur, wenn es überhaupt etwas zu wählen gibt.
        if c.locations.count > 1 {
            pendingLocations = c
        } else {
            catalog.choose(c, location: c.primaryLocation)
            dismiss()
        }
    }
}

/// Zweiter Schritt: der Ort innerhalb einer Gemeinde. Betrifft die Gebetszeiten, nicht nur die
/// Anzeige — eine Filiale in der Nachbarstadt hat eigene Zeiten.
private struct LocationPickerView: View {
    let community: CommunityInfo
    let onPick: (CommunityLocation) -> Void
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            ScrollView {
                LazyVStack(spacing: 10) {
                    ForEach(community.locations) { place in
                        Button { onPick(place) } label: {
                            HStack {
                                Text(place.name).font(.inter(15, .medium)).foregroundColor(.appOnSurface)
                                Spacer()
                                Image(systemName: "chevron.right")
                                    .font(.system(size: 13)).foregroundColor(.appOnSurfaceVariant)
                            }
                            .padding(14)
                            .frame(maxWidth: .infinity)
                            .background(Color.appSurface)
                            .clipShape(RoundedRectangle(cornerRadius: Radius.smallCard, style: .continuous))
                        }
                        .buttonStyle(.plain)
                    }
                }
                .padding(16)
            }
            .background(Color.appBackground.ignoresSafeArea())
            .navigationTitle(community.name)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(L("action_cancel")) { dismiss() }
                }
            }
        }
        .tint(.brandGreen)
    }
}
