import SwiftUI
import Shared

// "Gebetszeiten" (Startseite) — built to match the Android DashboardScreen (spec section 2):
// address / logo / donate header, Gregorian + Hijri date, a green hero with a live HH:MM:SS
// countdown, and prayer cards with Adhan + a divider + Iqamah. Inter font + exact brand colors.
struct ContentView: View {
    private let rows: [DashboardRow] = DashboardDataKt.dashboardRowsForToday()
    private let next: NextPrayerInfo = NextPrayerKt.nextPrayerNow()
    private let started = Date()

    @State private var now = Date()
    @Environment(\.colorScheme) private var scheme
    private let ticker = Timer.publish(every: 1, on: .main, in: .common).autoconnect()

    private let mapsURL = URL(string: "https://www.google.com/maps/search/?api=1&query=Schwanenweg+13%2C+34123+Kassel")
    private let donateURL = URL(string: "https://www.paypal.com/donate/?business=ikzsandzakkassel@gmail.com&currency_code=EUR")

    var body: some View {
        ZStack {
            Color.appBackground.ignoresSafeArea()
            ScrollView {
                VStack(spacing: 14) {
                    header
                    countdownCard.padding(.horizontal, 12)
                    VStack(spacing: 12) { ForEach(rows, id: \.name) { prayerCard($0) } }
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 10)
            }
        }
        .onReceive(ticker) { now = $0 }
    }

    // MARK: Header

    private var header: some View {
        VStack(spacing: 6) {
            HStack(alignment: .top, spacing: 6) {
                linkBlock(url: mapsURL) {
                    VStack(spacing: 3) {
                        Image(systemName: "mappin.and.ellipse").font(.system(size: 22)).foregroundColor(.appPrimary)
                        Text("Schwanenweg 13\n34123 Kassel").font(.inter(11, .medium)).foregroundColor(.appPrimary).multilineTextAlignment(.center)
                    }.frame(maxWidth: .infinity)
                }
                Image(uiImage: logoImage).resizable().scaledToFit().frame(height: 96)
                    .blendMode(scheme == .dark ? .normal : .multiply) // white emblem box blends into #F4F4F4
                linkBlock(url: donateURL) {
                    VStack(spacing: 3) {
                        Image(systemName: "heart.fill").font(.system(size: 30)).foregroundColor(.appPrimary)
                        Text(L("action_donate")).font(.inter(16, .bold)).foregroundColor(.appPrimary)
                    }.frame(maxWidth: .infinity)
                }
            }
            Text(gregorian).font(.inter(15)).foregroundColor(.appOnSurfaceVariant)
            Text(hijri).font(.inter(13)).foregroundColor(.appOnSurfaceVariant.opacity(0.7))
        }
        .padding(.top, 4)
    }

    private var logoImage: UIImage {
        let name = scheme == .dark ? "logo_community_dark" : "logo_community"
        return UIImage(named: name) ?? UIImage(named: "logo_community") ?? UIImage()
    }

    @ViewBuilder private func linkBlock<C: View>(url: URL?, @ViewBuilder _ content: () -> C) -> some View {
        if let url = url { Link(destination: url) { content() } } else { content() }
    }

    // MARK: Green hero with live countdown

    private var countdownCard: some View {
        VStack(spacing: 6) {
            Text(L("dashboard_next_prayer_in") + ":").font(.inter(16, .semibold)).foregroundColor(.brandGoldLight)
            Text(countdown).font(.inter(48, .bold)).monospacedDigit().foregroundColor(.white)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 18)
        .background(Color.brandGreen)
        .clipShape(RoundedRectangle(cornerRadius: Radius.hero, style: .continuous))
        .shadow(color: .black.opacity(0.18), radius: 6, y: 3)
    }

    // MARK: Prayer card (Adhan + divider + Iqamah)

    private func prayerCard(_ row: DashboardRow) -> some View {
        let active = row.name == next.name
        let nameColor = active ? Color.brandGoldLight : Color.appSecondary
        let adhanColor = active ? Color.white : Color.appPrimary
        let iqamahLabelColor = active ? Color.white.opacity(0.85) : Color.appPrimary
        let iqamahTimeColor = active ? Color.brandGoldLight : Color.appSecondary
        let dividerColor = active ? Color.white.opacity(0.35) : Color.appOnSurfaceVariant.opacity(0.3)
        return HStack(spacing: 0) {
            RoundedRectangle(cornerRadius: Radius.accentBar)
                .fill(active ? Color.clear : Color.appPrimary)
                .frame(width: 5)
                .padding(.vertical, 12)
                .padding(.leading, 10)
            VStack(spacing: 8) {
                HStack {
                    Text(germanName(row.name)).font(.inter(17, .semibold)).foregroundColor(nameColor)
                    Spacer()
                    Text(row.adhan).font(.inter(34, .bold)).foregroundColor(adhanColor)
                }
                if let iqamah = row.iqamah {
                    Rectangle().fill(dividerColor).frame(height: 1)
                    HStack {
                        Text(L("label_iqamah")).font(.inter(13, .medium)).foregroundColor(iqamahLabelColor)
                        Spacer()
                        Text(iqamah).font(.inter(22, .bold)).foregroundColor(iqamahTimeColor)
                    }
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 14)
        }
        .background(active ? Color.brandGreen : Color.appSurface)
        .clipShape(RoundedRectangle(cornerRadius: Radius.prayerCard, style: .continuous))
        .shadow(color: .black.opacity(0.06), radius: 3, y: 1)
    }

    // Localized prayer names (spec section 2 table) — from the selected app language.
    private func germanName(_ n: String) -> String {
        switch n {
        case "Fajr": return L("prayer_fajr")
        case "Sunrise": return L("prayer_sunrise")
        case "Dhuhr": return L("prayer_dhuhr")
        case "Asr": return L("prayer_asr")
        case "Maghrib": return L("prayer_maghrib")
        case "Isha": return L("prayer_isha")
        default: return n
        }
    }

    // MARK: Derived strings

    private var countdown: String {
        let target = started.addingTimeInterval(Double(next.inSeconds))
        let r = max(0, Int(target.timeIntervalSince(now)))
        return String(format: "%02d:%02d:%02d", r / 3600, (r % 3600) / 60, r % 60)
    }
    private var gregorian: String {
        let f = DateFormatter(); f.locale = Locale(identifier: Localization.shared.lang); f.dateFormat = "EEEE, d. MMMM yyyy"
        return f.string(from: Date())
    }
    private var hijri: String {
        let f = DateFormatter(); f.calendar = Calendar(identifier: .islamicUmmAlQura)
        f.locale = Locale(identifier: Localization.shared.lang); f.dateFormat = "d. MMMM yyyy G"
        return f.string(from: Date())
    }
}
