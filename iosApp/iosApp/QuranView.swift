import SwiftUI

// "Koran" (spec 5.1): the surah list + an Arabic-only Mushaf-style reader, from the bundled Uthmani
// JSON (quran/index.json + quran/<id>.json). Arabic only — no translation, by design.
// (This first version scrolls; the true page-fitting RTL pager is a later refinement.)

private struct Surah: Decodable, Identifiable {
    let id: Int
    let name: String
    let transliteration: String
    let total_verses: Int
}

private struct Ayah: Decodable { let n: Int; let t: String }
private struct SurahContent: Decodable { let ayahs: [Ayah] }

private enum QuranLoader {
    static func index() -> [Surah] {
        guard let url = Bundle.main.url(forResource: "index", withExtension: "json", subdirectory: "quran"),
              let d = try? Data(contentsOf: url),
              let s = try? JSONDecoder().decode([Surah].self, from: d) else { return [] }
        return s
    }
    static func ayahs(_ id: Int) -> [Ayah] {
        guard let url = Bundle.main.url(forResource: "\(id)", withExtension: "json", subdirectory: "quran"),
              let d = try? Data(contentsOf: url),
              let s = try? JSONDecoder().decode(SurahContent.self, from: d) else { return [] }
        return s.ayahs
    }
}

struct QuranView: View {
    private let surahs = QuranLoader.index()
    var body: some View {
        List(surahs) { s in
            NavigationLink(destination: SurahReader(id: s.id, name: s.name, transliteration: s.transliteration)) {
                HStack(spacing: 12) {
                    Text("\(s.id)")
                        .font(.inter(14, .semibold)).foregroundColor(.brandGreen)
                        .frame(width: 40, height: 40)
                        .background(Color.brandGreen.opacity(0.14)).clipShape(Circle())
                    VStack(alignment: .leading, spacing: 2) {
                        Text(s.transliteration).font(.inter(16, .semibold)).foregroundColor(.appOnSurface)
                        Text("\(s.total_verses) Verse").font(.inter(12)).foregroundColor(.appOnSurfaceVariant)
                    }
                    Spacer()
                    Text(s.name).font(.system(size: 22, weight: .bold)).foregroundColor(.brandGreen)
                }
            }
        }
        .listStyle(.plain)
        .navigationTitle("Koran").navigationBarTitleDisplayMode(.inline)
    }
}

private struct SurahReader: View {
    let id: Int
    let name: String
    let transliteration: String

    private var ayahs: [Ayah] { QuranLoader.ayahs(id) }

    var body: some View {
        ScrollView {
            VStack(spacing: 18) {
                Text(name)
                    .font(.system(size: 30, weight: .bold)).foregroundColor(.brandGoldLight)
                    .frame(maxWidth: .infinity).padding(.vertical, 16)
                    .background(Color.brandGreen).clipShape(RoundedRectangle(cornerRadius: Radius.languageCard))

                if id != 1 && id != 9 {
                    Text("بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ")
                        .font(.system(size: 24, weight: .bold)).foregroundColor(.brandGreen)
                }

                Text(flowingText)
                    .font(.system(size: 25))
                    .foregroundColor(.appOnSurface)
                    .lineSpacing(16)
                    .frame(maxWidth: .infinity, alignment: .trailing)
                    .multilineTextAlignment(.trailing)
                    .environment(\.layoutDirection, .rightToLeft)
            }
            .padding(18)
        }
        .background(Color.appBackground.ignoresSafeArea())
        .navigationTitle(transliteration).navigationBarTitleDisplayMode(.inline)
    }

    // The surah as one flowing block, each ayah followed by its number in a ﴿ ﴾ ornament.
    private var flowingText: String {
        ayahs.map { "\($0.t) ﴿\(arabicDigits($0.n))﴾" }.joined(separator: "  ")
    }
    private func arabicDigits(_ n: Int) -> String {
        let map: [Character: Character] = ["0": "٠", "1": "١", "2": "٢", "3": "٣", "4": "٤", "5": "٥", "6": "٦", "7": "٧", "8": "٨", "9": "٩"]
        return String(String(n).map { map[$0] ?? $0 })
    }
}
