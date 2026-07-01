import SwiftUI

// The 8 languages the app ships (spec 6.6). Order = grid order. Arabic uses the Palestinian flag
// by community choice. Endonyms and flags are fixed literals — never translated.
struct AppLang: Identifiable {
    let tag: String       // bs, de, ar, tr, sq, en, ur, ru
    let endonym: String   // shown on the card (own-language name)
    let flagPng: String   // static thumbnail for the pill (flag_ba, …)
    var id: String { tag }
    var gif: String { tag }                       // waving-flag GIF file == tag (bs.gif, …)
    var isRTL: Bool { tag == "ar" || tag == "ur" }
}

let appLanguages: [AppLang] = [
    .init(tag: "bs", endonym: "Bosanski", flagPng: "flag_ba"),
    .init(tag: "de", endonym: "Deutsch",  flagPng: "flag_de"),
    .init(tag: "ar", endonym: "العربية",   flagPng: "flag_ps"),
    .init(tag: "tr", endonym: "Türkçe",   flagPng: "flag_tr"),
    .init(tag: "sq", endonym: "Shqip",    flagPng: "flag_al"),
    .init(tag: "en", endonym: "English",  flagPng: "flag_gb"),
    .init(tag: "ur", endonym: "اردو",      flagPng: "flag_pk"),
    .init(tag: "ru", endonym: "Русский",  flagPng: "flag_ru"),
]

func langForTag(_ tag: String) -> AppLang { appLanguages.first { $0.tag == tag } ?? appLanguages[0] }

func flagUIImage(_ name: String) -> UIImage? {
    guard let url = Bundle.main.url(forResource: name, withExtension: "png", subdirectory: "flags") else { return nil }
    return UIImage(contentsOfFile: url.path)
}

// Small static flag thumbnail for the "Sprache ändern" pill.
struct FlagThumb: View {
    let pngName: String
    var w: CGFloat = 24
    var h: CGFloat = 18
    var body: some View {
        Group {
            if let img = flagUIImage(pngName) { Image(uiImage: img).resizable() } else { Color.gray }
        }
        .frame(width: w, height: h)
        .clipShape(RoundedRectangle(cornerRadius: 3))
        .overlay(RoundedRectangle(cornerRadius: 3).stroke(Color.appOnSurfaceVariant.opacity(0.4), lineWidth: 1))
    }
}

private func lerpColor(_ a: Color, _ b: Color, _ t: Double) -> Color {
    let ua = UIColor(a), ub = UIColor(b)
    var r1: CGFloat = 0, g1: CGFloat = 0, b1: CGFloat = 0, a1: CGFloat = 0
    var r2: CGFloat = 0, g2: CGFloat = 0, b2: CGFloat = 0, a2: CGFloat = 0
    ua.getRed(&r1, green: &g1, blue: &b1, alpha: &a1)
    ub.getRed(&r2, green: &g2, blue: &b2, alpha: &a2)
    let tt = CGFloat(t)
    return Color(red: r1 + (r2 - r1) * tt, green: g1 + (g2 - g1) * tt, blue: b1 + (b2 - b1) * tt)
}

// The favourite feature (spec 6.6): full-screen animated picker — a slowly flowing green/teal
// gradient with a 2-column grid of staggered-entrance cards, each showing a waving-flag GIF.
struct LanguagePickerView: View {
    var showClose = true
    let onSelect: (AppLang) -> Void
    let onClose: () -> Void

    private let cols = [GridItem(.flexible(), spacing: 12), GridItem(.flexible(), spacing: 12)]

    var body: some View {
        ZStack(alignment: .topTrailing) {
            // Slowly flowing colour gradient (~9 s each way, mirrored → 18 s cycle).
            TimelineView(.animation) { tl in
                let secs = tl.date.timeIntervalSinceReferenceDate
                let phase = (secs / 9).truncatingRemainder(dividingBy: 2)
                let t = phase <= 1 ? phase : 2 - phase
                LinearGradient(
                    colors: [
                        lerpColor(.deepGreen, .teal, t),
                        lerpColor(.brandGreen, .deepGreen, t),
                        lerpColor(.teal, .brandGreen, t),
                    ],
                    startPoint: .top, endPoint: .bottom
                )
                .ignoresSafeArea()
            }

            VStack(spacing: 0) {
                Text("اللغة · Language")
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundColor(.brandGoldLight)
                    .padding(.top, 56)
                Text("Jezik · Sprache · Gjuha · زبان · Язык")
                    .font(.system(size: 13))
                    .foregroundColor(.white.opacity(0.85))
                    .multilineTextAlignment(.center)
                    .padding(.top, 4)
                    .padding(.bottom, 16)

                ScrollView {
                    LazyVGrid(columns: cols, spacing: 12) {
                        ForEach(Array(appLanguages.enumerated()), id: \.element.id) { index, lang in
                            LanguageCard(lang: lang, index: index) { onSelect(lang) }
                        }
                    }
                    .padding(.horizontal, 16)
                    .padding(.bottom, 24)
                }
            }

            if showClose {
                Button(action: onClose) {
                    Image(systemName: "xmark")
                        .font(.system(size: 18, weight: .semibold))
                        .foregroundColor(.white)
                        .padding(12)
                }
                .padding(8)
            }
        }
    }
}

private struct LanguageCard: View {
    let lang: AppLang
    let index: Int
    let onTap: () -> Void
    @State private var appeared = false

    var body: some View {
        VStack(spacing: 8) {
            Color.clear
                .aspectRatio(3.0 / 2.0, contentMode: .fit)
                .overlay(GIFView(name: lang.gif))
                .clipShape(RoundedRectangle(cornerRadius: 10))
                .overlay(RoundedRectangle(cornerRadius: 10).stroke(Color.white.opacity(0.4), lineWidth: 1))
            Text(lang.endonym)
                .font(.system(size: 16, weight: .semibold))
                .foregroundColor(.white)
                .multilineTextAlignment(.center)
        }
        .padding(10)
        .frame(maxWidth: .infinity)
        .background(Color.white.opacity(0.10))
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .shadow(radius: 6)
        .opacity(appeared ? 1 : 0)
        .offset(y: appeared ? 0 : 40)
        .contentShape(Rectangle())
        .onTapGesture(perform: onTap)
        .onAppear {
            withAnimation(.easeOut(duration: 0.42).delay(Double(index) * 0.055)) { appeared = true }
        }
    }
}
