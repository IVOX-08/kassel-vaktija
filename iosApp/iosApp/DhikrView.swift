import SwiftUI
import Shared

// "Dhikr" (spec 5.3): the 25 remembrances (Arabic + transliteration + meaning), reused verbatim from
// the shared Kotlin data. Rotating order — the start point advances each visit.
struct DhikrView: View {
    @AppStorage("dhikr_ptr") private var ptr = 0

    private var items: [DhikrItem] {
        let all = DhikrDataKt.dhikrList(lang: "de")
        guard !all.isEmpty else { return [] }
        let p = ((ptr % all.count) + all.count) % all.count
        return Array(all[p...] + all[..<p])
    }

    var body: some View {
        ScrollView {
            LazyVStack(spacing: 12) {
                ForEach(Array(items.enumerated()), id: \.offset) { _, d in
                    VStack(alignment: .leading, spacing: 6) {
                        Text(d.arabic)
                            .font(.system(size: 26, weight: .bold)).foregroundColor(.appOnSurface)
                            .lineSpacing(12)
                            .frame(maxWidth: .infinity, alignment: .trailing)
                            .multilineTextAlignment(.trailing)
                            .environment(\.layoutDirection, .rightToLeft)
                        Text(d.transliteration).font(.inter(15)).italic().foregroundColor(.appPrimary)
                        Text(d.meaning).font(.inter(15)).foregroundColor(.appOnSurfaceVariant)
                    }
                    .padding()
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(Color.appSurface)
                    .clipShape(RoundedRectangle(cornerRadius: Radius.smallCard))
                }
            }
            .padding()
        }
        .background(Color.appBackground.ignoresSafeArea())
        .navigationTitle("Dhikr").navigationBarTitleDisplayMode(.inline)
        .onAppear { ptr += 5 }
    }
}
