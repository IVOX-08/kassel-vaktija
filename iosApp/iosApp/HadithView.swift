import SwiftUI

// "Hadith" (spec 5.2): pick a collection, then read each hadith — number, Arabic matn (RTL),
// a divider, the translation in the app language (English fallback), and the translator credit.
// Uses the exact bundled JSON assets (never machine-translated).

private struct HadithFile: Decodable {
    let metadata: Meta
    let hadiths: [Entry]
    struct Meta: Decodable { let name: String?; let translator: String? }
    struct Entry: Decodable { let hadithnumber: Int; let text: String }
}

private enum HadithLoader {
    static func load(_ collection: String, _ lang: String) -> HadithFile? {
        guard let url = Bundle.main.url(forResource: lang, withExtension: "json", subdirectory: "hadith/\(collection)"),
              let data = try? Data(contentsOf: url) else { return nil }
        return try? JSONDecoder().decode(HadithFile.self, from: data)
    }
    static func map(_ file: HadithFile?) -> [Int: String] {
        Dictionary((file?.hadiths ?? []).map { ($0.hadithnumber, $0.text) }, uniquingKeysWith: { a, _ in a })
    }
}

struct HadithView: View {
    var body: some View {
        List {
            NavigationLink(destination: HadithReader(collection: "nawawi40", title: L("hadith_nawawi"))) {
                Text(L("hadith_nawawi")).font(.inter(16)).foregroundColor(.appOnSurface)
            }
            NavigationLink(destination: HadithReader(collection: "riyadussalihin", title: L("hadith_riyad"))) {
                Text(L("hadith_riyad")).font(.inter(16)).foregroundColor(.appOnSurface)
            }
        }
        .navigationTitle(L("library_hadith")).navigationBarTitleDisplayMode(.inline)
    }
}

struct HadithReader: View {
    let collection: String
    let title: String

    // The translation follows the selected app language; English is the fallback for a collection
    // that happens to be missing it. (All 8 languages ship, so the fallback should never show.)
    private var lang: String { Localization.shared.lang }
    private var arFile: HadithFile? { HadithLoader.load(collection, "ar") }
    private var translation: [Int: String] { HadithLoader.map(HadithLoader.load(collection, lang)) }
    private var fallback: [Int: String] { HadithLoader.map(HadithLoader.load(collection, "en")) }
    private var credit: String? { HadithLoader.load(collection, lang)?.metadata.translator }

    var body: some View {
        ScrollView {
            LazyVStack(spacing: 14) {
                ForEach((arFile?.hadiths ?? []).filter { $0.hadithnumber >= 1 }, id: \.hadithnumber) { h in
                    VStack(alignment: .leading, spacing: 8) {
                        Text("\(h.hadithnumber).").font(.inter(15, .bold)).foregroundColor(.appPrimary)
                        Text(h.text)
                            .font(.system(size: 21))
                            .foregroundColor(.appOnSurface)
                            .lineSpacing(8)
                            .frame(maxWidth: .infinity, alignment: .trailing)
                            .multilineTextAlignment(.trailing)
                            .environment(\.layoutDirection, .rightToLeft)
                        if let t = translation[h.hadithnumber] ?? fallback[h.hadithnumber] {
                            Divider()
                            Text(t).font(.inter(15)).foregroundColor(.appOnSurfaceVariant)
                        }
                    }
                    .padding()
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(Color.appSurface)
                    .clipShape(RoundedRectangle(cornerRadius: Radius.smallCard))
                }
                if let credit = credit {
                    Text(String(format: L("hadith_translation_credit"), credit)).font(.inter(11)).foregroundColor(.appOnSurfaceVariant).padding(.vertical, 8)
                }
            }
            .padding()
        }
        .background(Color.appBackground.ignoresSafeArea())
        .navigationTitle(title).navigationBarTitleDisplayMode(.inline)
    }
}
