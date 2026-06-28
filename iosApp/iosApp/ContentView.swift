import SwiftUI
import Shared

// Brand palette mirrored 1:1 from the Android app's ui/theme/Color.kt.
private extension Color {
    static let brandGreen = Color(red: 46 / 255, green: 125 / 255, blue: 50 / 255)        // #2E7D32
    static let brandGreenDark = Color(red: 27 / 255, green: 94 / 255, blue: 32 / 255)     // #1B5E20
    static let brandGold = Color(red: 184 / 255, green: 134 / 255, blue: 11 / 255)        // #B8860B
    static let brandGoldLight = Color(red: 212 / 255, green: 175 / 255, blue: 55 / 255)   // #D4AF37
    static let pageBackground = Color(red: 244 / 255, green: 244 / 255, blue: 244 / 255)  // #F4F4F4
    static let cardText = Color(red: 26 / 255, green: 26 / 255, blue: 26 / 255)           // #1A1A1A
}

// The iOS dashboard, styled to match the Android app: community logo header, a green "next prayer"
// hero card, and white prayer cards with a green accent (the active prayer is filled green/gold).
// All times come from the shared Kotlin module (PrayerTimesCalculator / nextPrayerNow).
struct ContentView: View {
    private let rows: [PrayerRow] = PrayerRowsKt.prayerRowsForToday()
    private let next: NextPrayerInfo = NextPrayerKt.nextPrayerNow()

    private var todayString: String {
        let f = DateFormatter()
        f.locale = Locale(identifier: "de_DE")
        f.dateFormat = "EEEE, d. MMMM yyyy"
        return f.string(from: Date())
    }

    var body: some View {
        ZStack {
            Color.pageBackground.ignoresSafeArea()
            ScrollView {
                VStack(spacing: 16) {
                    header
                    countdownCard
                    VStack(spacing: 12) {
                        ForEach(rows, id: \.name) { prayerCard($0) }
                    }
                    Text("Berechnet vom gemeinsamen Kotlin-Code")
                        .font(.caption)
                        .foregroundColor(.secondary)
                        .padding(.top, 4)
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 12)
            }
        }
    }

    private var header: some View {
        VStack(spacing: 6) {
            if let logo = UIImage(named: "logo_community") {
                Image(uiImage: logo)
                    .resizable()
                    .scaledToFit()
                    .frame(height: 104)
            }
            Text(todayString)
                .font(.subheadline)
                .foregroundColor(.secondary)
        }
        .padding(.top, 4)
    }

    // Green hero card: the upcoming prayer + minutes remaining.
    private var countdownCard: some View {
        VStack(spacing: 6) {
            Text("NÄCHSTES GEBET")
                .font(.caption).fontWeight(.semibold)
                .tracking(1.5)
                .foregroundColor(.brandGoldLight)
            Text("\(next.name) · \(next.time)")
                .font(.system(size: 34, weight: .bold))
                .foregroundColor(.white)
            Text("in \(next.inMinutes) Minuten")
                .font(.subheadline)
                .foregroundColor(.white.opacity(0.9))
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 22)
        .background(Color.brandGreen)
        .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
        .shadow(color: .black.opacity(0.18), radius: 8, y: 4)
    }

    // White prayer card with a green left accent; the active (next) prayer is filled green with gold.
    private func prayerCard(_ row: PrayerRow) -> some View {
        let active = row.name == next.name
        return HStack(spacing: 0) {
            RoundedRectangle(cornerRadius: 3)
                .fill(active ? Color.clear : Color.brandGreen)
                .frame(width: 5)
                .padding(.vertical, 14)
                .padding(.leading, 10)
            HStack {
                Text(row.name)
                    .font(.title3).fontWeight(.semibold)
                    .foregroundColor(active ? .brandGoldLight : .brandGreen)
                Spacer()
                Text(row.time)
                    .font(.system(size: 30, weight: .bold))
                    .foregroundColor(active ? .white : .cardText)
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 16)
        }
        .background(active ? Color.brandGreen : Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
        .shadow(color: .black.opacity(0.06), radius: 3, y: 1)
    }
}
