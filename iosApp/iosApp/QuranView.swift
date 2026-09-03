import SwiftUI

// "Koran" (spec 5.1): surah list + a PAGINATED Mushaf reader — each page fills with as many complete
// ayahs as fit, page-turning right-to-left (no scrolling), bookmark top-right, saved resume point.
// Arabic only, wrapped (native SwiftUI Text), from the bundled Uthmani JSON.

private struct Surah: Decodable, Identifiable {
    let id: Int; let name: String; let transliteration: String; let total_verses: Int
}
private struct Ayah: Decodable {
    let n: Int
    let t: String
    /// Die MARKIERTE Fassung derselben Ajah (siehe Tajweed.swift) — aber nur, wenn sie sich
    /// restlos lesen ließ. Sonst `nil`, und dann wird der schlichte Text gezeigt: nie geraten
    /// eingefärbt, nie eine Klammer im Korantext.
    ///
    /// Einmal beim Laden geprüft, nicht bei jedem Seitenumbruch: Der Umbruch misst dieselbe Ajah
    /// dutzendfach.
    let marked: String?

    private enum CodingKeys: String, CodingKey { case n, t, tj }

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        n = try c.decode(Int.self, forKey: .n)
        t = try c.decode(String.self, forKey: .t)
        let raw = try c.decodeIfPresent(String.self, forKey: .tj)
        marked = raw.flatMap { $0.isEmpty || !Tajweed.isUsable($0) ? nil : $0 }
    }
}
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
    static func resume(_ s: Int) -> Int { AppGroup.defaults.integer(forKey: "q_resume_\(s)") }
    static func setResume(_ s: Int, _ p: Int) { AppGroup.defaults.set(p, forKey: "q_resume_\(s)") }
    static func marks() -> Set<String> { Set(AppGroup.defaults.stringArray(forKey: "q_bm") ?? []) }
    static func isMarked(_ k: String) -> Bool { marks().contains(k) }
    static func toggle(_ k: String) {
        var b = marks(); if b.contains(k) { b.remove(k) } else { b.insert(k) }
        AppGroup.defaults.set(Array(b), forKey: "q_bm")
    }
}

/// Grosse, gut lesbare arabische Schrift, damit die Harakat und die Tedschwid-Zeichen deutlich
/// zu sehen sind — danach hat die Gemeinde ausdruecklich gefragt. Die Seiten werden auf genau
/// diese Groesse umbrochen, ohne Scrollen (siehe `paginate`).
private let readerBaseFontSize: CGFloat = 25

/// Schrift, Groesse und Tedschwid.
///
/// Steht im Leser und nicht in den Einstellungen: Das sind Entscheidungen, die beim Lesen fallen —
/// der Text ist eine Spur zu klein, oder die Regeln sollen fuer diese Sitzung sichtbar sein. Wer
/// dafuer die Sure verlassen muss, aendert es schlicht nicht.
final class QuranReaderPrefs: ObservableObject {
    static let shared = QuranReaderPrefs()

    static let minScale: CGFloat = 0.7
    static let maxScale: CGFloat = 1.8
    static let step: CGFloat = 0.1

    @Published var ottoman: Bool { didSet { AppGroup.defaults.set(ottoman, forKey: "q_ottoman") } }
    @Published var tajweed: Bool { didSet { AppGroup.defaults.set(tajweed, forKey: "q_tajweed") } }
    @Published var scale: CGFloat { didSet { AppGroup.defaults.set(Double(scale), forKey: "q_scale") } }

    private init() {
        ottoman = AppGroup.defaults.bool(forKey: "q_ottoman")
        tajweed = AppGroup.defaults.bool(forKey: "q_tajweed")
        let stored = AppGroup.defaults.object(forKey: "q_scale") as? Double
        scale = min(max(CGFloat(stored ?? 1), QuranReaderPrefs.minScale), QuranReaderPrefs.maxScale)
    }

    func zoom(_ delta: CGFloat) {
        scale = min(max(scale + delta, QuranReaderPrefs.minScale), QuranReaderPrefs.maxScale)
    }
}

/// Amiri Quran — der klassische Nasch-Schnitt osmanischer und tuerkischer Druck-Mushafs, unter der
/// SIL Open Font Licence mitgeliefert. Er steht neben der Systemschrift, weil beide sehr
/// unterschiedlich gelesen werden: Wer aus einem tuerkischen Mushaf gelernt hat, findet hier die
/// Buchstabenformen, die er kennt.
private let ottomanFontName = "Amiri Quran"

private func arabicDigits(_ n: Int) -> String {
    let m: [Character: Character] = ["0": "٠", "1": "١", "2": "٢", "3": "٣", "4": "٤", "5": "٥", "6": "٦", "7": "٧", "8": "٨", "9": "٩"]
    return String(String(n).map { m[$0] ?? $0 })
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
                        Text(String(format: L("quran_verses"), s.total_verses))
                            .font(.inter(12)).foregroundColor(.appOnSurfaceVariant)
                    }
                    Spacer()
                    Text(s.name).font(.system(size: 22, weight: .bold)).foregroundColor(.brandGreen)
                }
            }
        }
        .listStyle(.plain).navigationTitle(L("library_quran")).navigationBarTitleDisplayMode(.inline)
    }
}

private struct SurahReader: View {
    let id: Int; let name: String; let transliteration: String
    /// Die tatsaechliche Helligkeit der Seite — nicht die Systemeinstellung. Die App hat ihren
    /// eigenen Hell/Dunkel-Schalter, und die beiden koennen sich widersprechen.
    @Environment(\.colorScheme) private var scheme
    private let ayahs: [Ayah]
    @ObservedObject private var prefs = QuranReaderPrefs.shared
    @State private var pages: [[Ayah]] = []
    @State private var current: Int
    @State private var marked = false

    init(id: Int, name: String, transliteration: String) {
        self.id = id; self.name = name; self.transliteration = transliteration
        self.ayahs = QuranLoader.ayahs(id)
        _current = State(initialValue: QuranStore.resume(id))
    }

    var body: some View {
        VStack(spacing: 0) {
            controls
            Divider()
            GeometryReader { geo in
                // Querformat ist kaum 400 Punkte hoch. Dort muss der Titelblock schrumpfen und die
                // Zeilen enger stehen, sonst passen zwischen Bedienleiste und Seitenzahl knapp zwei
                // Zeilen — und Lesen im Querformat wird langsamer statt schneller.
                let compact = geo.size.height < 520
                let layout = Layout(scale: prefs.scale, ottoman: prefs.ottoman,
                                    tajweed: prefs.tajweed, compact: compact)
                let contentWidth = geo.size.width - 36
                let availableHeight = geo.size.height - (compact ? 34 : 64)
                ZStack {
                    Color.appBackground.ignoresSafeArea()
                    if pages.isEmpty {
                        ProgressView()
                    } else {
                        TabView(selection: $current) {
                            ForEach(pages.indices, id: \.self) { i in
                                page(i, layout: layout, compact: compact).tag(i)
                            }
                        }
                        .tabViewStyle(.page(indexDisplayMode: .never))
                        .environment(\.layoutDirection, .rightToLeft)
                        .onChange(of: current) { _, v in
                            QuranStore.setResume(id, v); marked = QuranStore.isMarked("\(id):\(v)")
                        }
                    }
                }
                // Neu umbrechen, sobald sich irgendetwas am Aussehen aendert. Schrift, Groesse,
                // Zeilenabstand und Tedschwid gehen ALLE in die Messung ein — bliebe einer davon
                // aussen vor, braechen die Seiten an falschen Stellen.
                .task(id: RelayoutKey(scale: prefs.scale, ottoman: prefs.ottoman,
                                      tajweed: prefs.tajweed, width: contentWidth,
                                      height: availableHeight)) {
                    pages = paginate(width: contentWidth, height: availableHeight,
                                     firstExtra: compact ? 74 : ((id == 1 || id == 9) ? 90 : 150),
                                     layout: layout)
                    if current >= pages.count { current = max(0, pages.count - 1) }
                    marked = QuranStore.isMarked("\(id):\(current)")
                }
            }
        }
        .background(Color.appBackground.ignoresSafeArea())
        // Nur hier darf sich der Bildschirm drehen. Beim Verlassen wird das Hochformat wieder
        // erzwungen — sonst stuende die naechste Seite quer, ohne dafuer gebaut zu sein.
        .onAppear { AppDelegate.allowLandscape(true) }
        .onDisappear { AppDelegate.allowLandscape(false) }
        .navigationTitle(transliteration).navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Button { QuranStore.toggle("\(id):\(current)"); marked.toggle() } label: {
                    Image(systemName: marked ? "bookmark.fill" : "bookmark").foregroundColor(.brandGold)
                }
            }
        }
    }

    /// Was in die Messung UND in die Zeichnung geht — beides aus einer Quelle, damit sie nicht
    /// auseinanderlaufen koennen.
    private struct Layout {
        let font: UIFont
        let lineSpacing: CGFloat
        let tajweed: Bool

        init(scale: CGFloat, ottoman: Bool, tajweed: Bool, compact: Bool) {
            let size = readerBaseFontSize * scale
            self.font = (ottoman ? UIFont(name: ottomanFontName, size: size) : nil)
                ?? UIFont.systemFont(ofSize: size)
            // 1,95 Zeilenabstand gibt den Harakat im Hochformat Luft. Im Querformat ist das fast
            // die ganze Seite, deshalb dort 1,45.
            let multiple = compact ? 1.45 : 1.95
            // SwiftUI und UIKit rechnen `lineSpacing` als ZUSATZ zwischen den Zeilen, Android als
            // Gesamthoehe. Ohne diesen Abzug stuenden die Zeilen um die Zeilenhoehe zu weit
            // auseinander und jede Seite haette ein Drittel weniger Text.
            self.lineSpacing = max(0, size * multiple - self.font.lineHeight)
            self.tajweed = tajweed
        }
    }

    /// Alles, was einen neuen Umbruch erzwingt. Als eigener Wert, damit `.task(id:)` ihn vergleichen kann.
    private struct RelayoutKey: Equatable {
        let scale: CGFloat; let ottoman: Bool; let tajweed: Bool
        let width: CGFloat; let height: CGFloat
    }

    // MARK: - Bedienleiste

    private var controls: some View {
        HStack(spacing: 6) {
            // Die Chips scrollen in ihrem EIGENEN Bereich.
            //
            // Als eine einzige Reihe gebaut, schoben sie die Zoom-Knoepfe bei laengeren
            // Beschriftungen ueber den rechten Rand — man konnte den Text verkleinern, aber nie
            // wieder vergroessern, weil der Plus-Knopf hinter dem Bildschirmrand lag. Genau das
            // ist auf Android passiert.
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 6) {
                    chip(L("quran_script_ottoman"), on: prefs.ottoman) { prefs.ottoman.toggle() }
                    chip(L("quran_tajweed"), on: prefs.tajweed) { prefs.tajweed.toggle() }
                }
            }
            // Die Zoom-Knoepfe stehen fest daneben und koennen nicht verdraengt werden.
            zoomButton("minus", enabled: prefs.scale > QuranReaderPrefs.minScale,
                       label: L("quran_smaller")) { prefs.zoom(-QuranReaderPrefs.step) }
            zoomButton("plus", enabled: prefs.scale < QuranReaderPrefs.maxScale,
                       label: L("quran_larger")) { prefs.zoom(QuranReaderPrefs.step) }
        }
        .padding(.leading, 12).padding(.trailing, 8).padding(.vertical, 6)
    }

    private func chip(_ title: String, on: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(title)
                .font(.inter(13, .semibold))
                .foregroundColor(on ? .white : .appOnSurface)
                .padding(.horizontal, 12).padding(.vertical, 7)
                .background(Capsule().fill(on ? Color.brandGreen : Color.appSurfaceVariant))
        }
        .buttonStyle(.plain)
    }

    private func zoomButton(_ icon: String, enabled: Bool, label: String,
                            action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Image(systemName: icon).font(.system(size: 16, weight: .semibold))
                .foregroundColor(enabled ? .brandGreen : .appOnSurfaceVariant.opacity(0.4))
                .frame(width: 34, height: 34)
        }
        .buttonStyle(.plain)
        .disabled(!enabled)
        .accessibilityLabel(label)
    }

    // MARK: - Seite

    private func page(_ i: Int, layout: Layout, compact: Bool) -> some View {
        VStack(spacing: compact ? 6 : 12) {
            if i == 0 {
                Text(name)
                    .font(.system(size: compact ? 20 : 30, weight: .bold))
                    .foregroundColor(.brandGoldLight)
                    .frame(maxWidth: .infinity).padding(.vertical, compact ? 6 : 14)
                    .background(Color.brandGreen).clipShape(RoundedRectangle(cornerRadius: 16))
                if id != 1 && id != 9 {
                    Text("بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ")
                        .font(.system(size: compact ? 17 : 22, weight: .bold)).foregroundColor(.brandGreen)
                }
            }
            Text(flowingAttributed(pages[i], layout: layout))
                .font(Font(layout.font))
                .lineSpacing(layout.lineSpacing)
                .foregroundColor(.appOnSurface)
                .fixedSize(horizontal: false, vertical: true)
                .frame(maxWidth: .infinity, alignment: .topTrailing)
                .multilineTextAlignment(.trailing)
                .environment(\.layoutDirection, .rightToLeft)
            Spacer(minLength: 0)
            Text(arabicDigits(i + 1)).font(.system(size: 16)).foregroundColor(.appOnSurfaceVariant)
        }
        .padding(.horizontal, 18).padding(.vertical, 8)
    }

    /// Der Text einer Ajah, so wie er gezeichnet wird.
    ///
    /// Mit Tedschwid ist es die markierte Fassung — sie schreibt manche Buchstaben anders (`ـٰ`
    /// statt `ٰ`) und ist deshalb NICHT gleich lang wie die schlichte. Wo keine markierte Fassung
    /// vorliegt, steht der schlichte Text: nie geraten eingefaerbt.
    private static func text(_ a: Ayah, tajweed: Bool) -> String {
        guard tajweed, let marked = a.marked else { return a.t }
        return Tajweed.plain(marked)
    }

    /// Derselbe Text wie `text`, Zeichen fuer Zeichen — nur farbig. Damit bleibt die Messung gueltig.
    private func flowingAttributed(_ a: [Ayah], layout: Layout) -> AttributedString {
        var out = AttributedString()
        for (idx, ayah) in a.enumerated() {
            if idx > 0 { out.append(AttributedString("  ")) }
            if layout.tajweed, let marked = ayah.marked {
                out.append(Tajweed.attributed(marked, base: .appOnSurface,
                                              onDarkPage: scheme == .dark))
            } else {
                out.append(AttributedString(ayah.t))
            }
            out.append(AttributedString(" "))
            var marker = AttributedString("﴿\(arabicDigits(ayah.n))﴾")
            // Auf dunklem Grund das helle Gold; das tiefe Gold liest sich dort als Braun.
            marker.foregroundColor = scheme == .dark ? .brandGoldLight : .brandGold
            out.append(marker)
        }
        return out
    }

    /// Greedily pack complete ayahs onto each page until the next one would overflow.
    ///
    /// Gemessen wird mit GENAU dem Stil, in dem auch gezeichnet wird — eine andere Schrift oder
    /// Groesse an dieser Stelle braeche die Seiten an falschen Stellen, und jede Seite liefe
    /// entweder ueber oder endete zu frueh.
    private func paginate(width: CGFloat, height: CGFloat, firstExtra: CGFloat,
                          layout: Layout) -> [[Ayah]] {
        guard width > 0, height > 0 else { return [] }
        let para = NSMutableParagraphStyle()
        para.baseWritingDirection = .rightToLeft
        para.lineSpacing = layout.lineSpacing
        let attrs: [NSAttributedString.Key: Any] = [.font: layout.font, .paragraphStyle: para]
        let sizer = UILabel()
        sizer.numberOfLines = 0
        func textHeight(_ t: String) -> CGFloat {
            sizer.attributedText = NSAttributedString(string: t, attributes: attrs)
            return sizer.sizeThatFits(CGSize(width: width, height: .greatestFiniteMagnitude)).height
        }
        // Einmal je Ajah, nicht einmal je Messung: Der gierige Umbruch misst dieselbe Ajah sonst
        // dutzendfach, und bei Al-Baqarah mit 286 Ajahs wird daraus eine spuerbare Wartezeit.
        let lines = ayahs.map { "\(Self.text($0, tajweed: layout.tajweed)) ﴿\(arabicDigits($0.n))﴾" }

        var result: [[Ayah]] = []; var cur: [Ayah] = []
        var currentLines: [String] = []
        var limit = height - firstExtra
        for (index, a) in ayahs.enumerated() {
            let text = (currentLines + [lines[index]]).joined(separator: "  ")
            // Small safety margin so we under-fill slightly rather than truncate the last ayah.
            if !cur.isEmpty && textHeight(text) * 1.06 > limit {
                result.append(cur)
                cur = [a]; currentLines = [lines[index]]
                limit = height
            } else {
                cur.append(a); currentLines.append(lines[index])
            }
        }
        if !cur.isEmpty { result.append(cur) }
        return result
    }
}
