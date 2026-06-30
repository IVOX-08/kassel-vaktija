import SwiftUI

// "Koran" (spec 5.1): surah list + a PAGINATED Mushaf reader — each page fills with as many complete
// ayahs as fit, page-turning right-to-left (no scrolling), bookmark top-right, saved resume point.
// Arabic only, justified, wrapped, from the bundled Uthmani JSON.

private struct Surah: Decodable, Identifiable {
    let id: Int; let name: String; let transliteration: String; let total_verses: Int
}
private struct Ayah: Decodable { let n: Int; let t: String }
private struct SurahContent: Decodable { let ayahs: [Ayah] }

private enum QuranLoader {
    static func index() -> [Surah] { load("index", [Surah].self) ?? [] }
    static func ayahs(_ id: Int) -> [Ayah] { load("\(id)", SurahContent.self)?.ayahs ?? [] }
    private static func load<T: Decodable>(_ name: String, _ type: T.Type) -> T? {
        guard let url = Bundle.main.url(forResource: name, withExtension: "json", subdirectory: "quran"),
              let d = try? Data(contentsOf: url) else { return nil }
        return try? JSONDecoder().decode(T.self, from: d)
    }
}

private enum QuranStore {
    static func resume(_ s: Int) -> Int { UserDefaults.standard.integer(forKey: "q_resume_\(s)") }
    static func setResume(_ s: Int, _ p: Int) { UserDefaults.standard.set(p, forKey: "q_resume_\(s)") }
    static func marks() -> Set<String> { Set(UserDefaults.standard.stringArray(forKey: "q_bm") ?? []) }
    static func isMarked(_ k: String) -> Bool { marks().contains(k) }
    static func toggle(_ k: String) {
        var b = marks(); if b.contains(k) { b.remove(k) } else { b.insert(k) }
        UserDefaults.standard.set(Array(b), forKey: "q_bm")
    }
}

private let readerFontSize: CGFloat = 26
private func arabicParagraph() -> NSMutableParagraphStyle {
    let p = NSMutableParagraphStyle()
    p.alignment = .justified; p.baseWritingDirection = .rightToLeft; p.lineSpacing = 12
    return p
}
private func arabicAttributes() -> [NSAttributedString.Key: Any] {
    [.font: UIFont.systemFont(ofSize: readerFontSize), .paragraphStyle: arabicParagraph()]
}
private func arabicDigits(_ n: Int) -> String {
    let m: [Character: Character] = ["0": "٠", "1": "١", "2": "٢", "3": "٣", "4": "٤", "5": "٥", "6": "٦", "7": "٧", "8": "٨", "9": "٩"]
    return String(String(n).map { m[$0] ?? $0 })
}

// Justified, wrapping, non-scrolling Arabic — width is fixed so the text container actually wraps.
struct ArabicText: UIViewRepresentable {
    let text: String
    let width: CGFloat
    func makeUIView(context: Context) -> UITextView {
        let tv = UITextView()
        tv.isEditable = false; tv.isScrollEnabled = false; tv.backgroundColor = .clear
        tv.textContainerInset = .zero; tv.textContainer.lineFragmentPadding = 0
        return tv
    }
    func updateUIView(_ tv: UITextView, context: Context) {
        tv.textContainer.size = CGSize(width: width, height: .greatestFiniteMagnitude)
        let attrs = arabicAttributes().merging([.foregroundColor: UIColor.label]) { a, _ in a }
        tv.attributedText = NSAttributedString(string: text, attributes: attrs)
    }
}

struct QuranView: View {
    private let surahs = QuranLoader.index()
    var body: some View {
        List(surahs) { s in
            NavigationLink(destination: SurahReader(id: s.id, name: s.name, transliteration: s.transliteration)) {
                HStack(spacing: 12) {
                    Text("\(s.id)").font(.inter(14, .semibold)).foregroundColor(.brandGreen)
                        .frame(width: 40, height: 40).background(Color.brandGreen.opacity(0.14)).clipShape(Circle())
                    VStack(alignment: .leading, spacing: 2) {
                        Text(s.transliteration).font(.inter(16, .semibold)).foregroundColor(.appOnSurface)
                        Text("\(s.total_verses) Verse").font(.inter(12)).foregroundColor(.appOnSurfaceVariant)
                    }
                    Spacer()
                    Text(s.name).font(.system(size: 22, weight: .bold)).foregroundColor(.brandGreen)
                }
            }
        }
        .listStyle(.plain).navigationTitle("Koran").navigationBarTitleDisplayMode(.inline)
    }
}

private struct SurahReader: View {
    let id: Int; let name: String; let transliteration: String
    private let ayahs: [Ayah]
    @State private var pages: [[Ayah]] = []
    @State private var current: Int
    @State private var marked = false

    init(id: Int, name: String, transliteration: String) {
        self.id = id; self.name = name; self.transliteration = transliteration
        self.ayahs = QuranLoader.ayahs(id)
        _current = State(initialValue: QuranStore.resume(id))
    }

    var body: some View {
        GeometryReader { geo in
            let contentWidth = geo.size.width - 36
            ZStack {
                Color.appBackground.ignoresSafeArea()
                if pages.isEmpty {
                    ProgressView().onAppear {
                        pages = paginate(width: contentWidth, height: geo.size.height - 80,
                                         firstExtra: (id == 1 || id == 9) ? 70 : 120)
                        if current >= pages.count { current = max(0, pages.count - 1) }
                        marked = QuranStore.isMarked("\(id):\(current)")
                    }
                } else {
                    TabView(selection: $current) {
                        ForEach(pages.indices, id: \.self) { i in page(i, width: contentWidth).tag(i) }
                    }
                    .tabViewStyle(.page(indexDisplayMode: .never))
                    .environment(\.layoutDirection, .rightToLeft)
                    .onChange(of: current) { v in
                        QuranStore.setResume(id, v); marked = QuranStore.isMarked("\(id):\(v)")
                    }
                }
            }
        }
        .navigationTitle(transliteration).navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Button { QuranStore.toggle("\(id):\(current)"); marked.toggle() } label: {
                    Image(systemName: marked ? "bookmark.fill" : "bookmark").foregroundColor(.brandGold)
                }
            }
        }
    }

    private func page(_ i: Int, width: CGFloat) -> some View {
        VStack(spacing: 12) {
            if i == 0 {
                Text(name).font(.system(size: 30, weight: .bold)).foregroundColor(.brandGoldLight)
                    .frame(maxWidth: .infinity).padding(.vertical, 14)
                    .background(Color.brandGreen).clipShape(RoundedRectangle(cornerRadius: 16))
                if id != 1 && id != 9 {
                    Text("بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ")
                        .font(.system(size: 22, weight: .bold)).foregroundColor(.brandGreen)
                }
            }
            ArabicText(text: flowing(pages[i]), width: width)
                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topTrailing)
            Text(arabicDigits(i + 1)).font(.system(size: 16)).foregroundColor(.appOnSurfaceVariant)
        }
        .padding(.horizontal, 18).padding(.vertical, 6)
    }

    private func flowing(_ a: [Ayah]) -> String {
        a.map { "\($0.t) ﴿\(arabicDigits($0.n))﴾" }.joined(separator: "  ")
    }

    // Greedily pack complete ayahs onto each page until the next one would overflow the page height.
    private func paginate(width: CGFloat, height: CGFloat, firstExtra: CGFloat) -> [[Ayah]] {
        let attrs = arabicAttributes()
        func textHeight(_ t: String) -> CGFloat {
            (t as NSString).boundingRect(
                with: CGSize(width: width, height: .greatestFiniteMagnitude),
                options: [.usesLineFragmentOrigin, .usesFontLeading], attributes: attrs, context: nil
            ).height
        }
        var result: [[Ayah]] = []; var cur: [Ayah] = []
        var limit = height - firstExtra
        for a in ayahs {
            let candidate = cur + [a]
            let text = candidate.map { "\($0.t) ﴿\(arabicDigits($0.n))﴾" }.joined(separator: "  ")
            if !cur.isEmpty && textHeight(text) > limit {
                result.append(cur); cur = [a]; limit = height
            } else {
                cur = candidate
            }
        }
        if !cur.isEmpty { result.append(cur) }
        return result
    }
}
