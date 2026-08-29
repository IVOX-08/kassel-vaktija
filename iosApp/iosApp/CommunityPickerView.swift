import SwiftUI

// Die Gemeindeauswahl. Aufbau und Texte wie auf Android — die Apps sollen sich gleich anfühlen,
// sonst erklärt der Vorstand bei jeder Schulung zweimal dasselbe.
//
// Zweistufig und in DIESER Reihenfolge: erst die Gemeinde, dann der Ort. Nicht umgekehrt — in
// Berlin gibt es zwei Gemeinden, und eine Ortsliste vorweg könnte sie nicht auseinanderhalten.
//
// Gesucht wird über Name, Adresse und alle Orte. Wer die App bekommt, kennt oft nur seine Stadt,
// nicht den eingetragenen Vereinsnamen: „Siegen" muss „IGBD-Džemat BKC Siegen e.V." finden.
//
// Abgeschaltete und gesperrte Gemeinden stehen nicht in der Liste. Neue Gemeinden werden
// abgeschaltet angelegt, deshalb ist die Liste anfangs kurz — das ist Absicht, kein Fehler.
struct CommunityPickerView: View {
    @ObservedObject private var catalog = CommunityCatalog.shared
    @Environment(\.dismiss) private var dismiss

    @State private var query = ""
    /// Gesetzt, sobald eine Gemeinde mit mehreren Orten gewählt wurde.
    @State private var pendingLocations: CommunityInfo?

    /// Namen, die mehr als einmal vorkommen — im GANZEN Verzeichnis, nicht nur im Suchergebnis.
    /// Sonst waere dieselbe Zeile mal mit Ort und mal mit Strasse beschriftet, je nachdem, was
    /// gerade im Suchfeld steht.
    private var ambiguousNames: Set<String> {
        var seen = Set<String>(), twice = Set<String>()
        for c in catalog.selectable where !seen.insert(c.name).inserted { twice.insert(c.name) }
        return twice
    }

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
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    Text(L("community_picker_title"))
                        .font(.inter(30, .bold)).foregroundColor(.brandGreen)
                        .fixedSize(horizontal: false, vertical: true)
                    Text(L("community_picker_subtitle"))
                        .font(.inter(15)).foregroundColor(.appOnSurface)
                        .fixedSize(horizontal: false, vertical: true)

                    searchField

                    if catalog.selectable.isEmpty {
                        // Ohne Netz und ohne Verzeichnis ist die Liste leer — das ist etwas
                        // anderes als „nichts gefunden" und muss anders klingen.
                        notice(L("community_none_available"))
                    } else if matches.isEmpty {
                        notice(L("community_search_empty"))
                    } else {
                        VStack(spacing: 10) {
                            ForEach(matches) { community in
                                Button { pick(community) } label: { row(community) }
                                    .buttonStyle(.plain)
                            }
                        }
                    }
                }
                .padding(.horizontal, 20).padding(.top, 24).padding(.bottom, 32)
            }
            .background(Color.appBackground.ignoresSafeArea())
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(L("action_cancel")) { dismiss() }
                }
            }
            .sheet(item: $pendingLocations) { community in
                LocationPickerSheet(community: community) { location in
                    catalog.choose(community, location: location)
                    pendingLocations = nil
                    dismiss()
                }
                .presentationDetents([.medium, .large])
            }
        }
        .tint(.brandGreen)
        .onAppear { catalog.start() }
    }

    private var searchField: some View {
        HStack(spacing: 12) {
            Image(systemName: "magnifyingglass")
                .font(.system(size: 19)).foregroundColor(.appOnSurfaceVariant)
            TextField(L("community_search_hint"), text: $query)
                .font(.inter(17)).autocorrectionDisabled()
            if !query.isEmpty {
                Button { query = "" } label: {
                    Image(systemName: "xmark.circle.fill")
                        .foregroundColor(.appOnSurfaceVariant)
                }
                .accessibilityLabel(L("community_search_clear"))
            }
        }
        .padding(.horizontal, 16).padding(.vertical, 18)
        .overlay(RoundedRectangle(cornerRadius: 16, style: .continuous)
            .stroke(Color.appOnSurfaceVariant.opacity(0.45), lineWidth: 1))
    }

    private func notice(_ text: String) -> some View {
        Text(text)
            .font(.inter(15)).foregroundColor(.appOnSurfaceVariant)
            .fixedSize(horizontal: false, vertical: true)
            .padding(.top, 8)
    }

    private func row(_ c: CommunityInfo) -> some View {
        HStack(spacing: 16) {
            Image(systemName: "mappin.and.ellipse")
                .font(.system(size: 22)).foregroundColor(.brandGoldLight)
                .frame(width: 60, height: 60)
                .background(Color.brandGreen).clipShape(Circle())
            VStack(alignment: .leading, spacing: 4) {
                Text(c.name)
                    .font(.inter(19, .bold)).foregroundColor(.appOnSurface)
                    .fixedSize(horizontal: false, vertical: true)
                    .multilineTextAlignment(.leading)
                // Alle Orte, nicht nur der erste: die Zeile beantwortet die Frage „ist meine
                // Stadt dabei?", und die stellt sich vor dem Antippen.
                //
                // Ausnahme: Zwei Gemeinden heissen „Džemat Stuttgart" und liegen beide in
                // Stuttgart. Untereinander waeren sie Wort fuer Wort dieselbe Zeile. Dann
                // entscheidet die Strasse — das ist das Einzige, was sie unterscheidet.
                Text(subtitle(c))
                    .font(.inter(15)).foregroundColor(.appOnSurfaceVariant)
                    .lineLimit(1)
            }
            Spacer(minLength: 0)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.vertical, 10)
    }

    private func subtitle(_ c: CommunityInfo) -> String {
        if ambiguousNames.contains(c.name), let address = c.address, !address.isEmpty {
            return address
        }
        return c.locations.map(\.name).joined(separator: " · ")
    }

    private func pick(_ c: CommunityInfo) {
        // Zweiter Schritt nur, wenn es etwas zu wählen gibt.
        if c.locations.count > 1 {
            pendingLocations = c
        } else {
            catalog.choose(c, location: c.primaryLocation)
            dismiss()
        }
    }
}

/// Zweiter Schritt: der Ort innerhalb der Gemeinde. Betrifft die Gebetszeiten, nicht nur die
/// Anzeige — eine Filiale in der Nachbarstadt hat eigene Zeiten.
private struct LocationPickerSheet: View {
    let community: CommunityInfo
    let onPick: (CommunityLocation) -> Void
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text(community.name)
                .font(.inter(24, .bold)).foregroundColor(.appOnSurface)
                .fixedSize(horizontal: false, vertical: true)
            Text(L("community_pick_town"))
                .font(.inter(16)).foregroundColor(.appOnSurface)
                .fixedSize(horizontal: false, vertical: true)

            ScrollView {
                VStack(spacing: 10) {
                    ForEach(community.locations) { place in
                        Button { onPick(place) } label: {
                            HStack(spacing: 14) {
                                Image(systemName: "mappin.and.ellipse")
                                    .font(.system(size: 19)).foregroundColor(.brandGreen)
                                Text(place.name)
                                    .font(.inter(17, .medium)).foregroundColor(.appOnSurface)
                                Spacer()
                            }
                            .padding(.horizontal, 16).padding(.vertical, 16)
                            .frame(maxWidth: .infinity)
                            .background(Color.appBackground)
                            .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                        }
                        .buttonStyle(.plain)
                    }
                }
            }

            HStack {
                Spacer()
                Button(L("action_cancel")) { dismiss() }
                    .font(.inter(16, .semibold)).foregroundColor(.brandGreen)
            }
        }
        .padding(24)
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
        .background(Color.moreCard.ignoresSafeArea())
    }
}
