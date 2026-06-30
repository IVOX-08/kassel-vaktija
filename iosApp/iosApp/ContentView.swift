import SwiftUI
import Shared

// The "Gebetszeiten" tab — matches the Android DashboardScreen: address / logo / donate header,
// Gregorian + Hijri date, a green hero with a live HH:MM:SS countdown, and white prayer cards showing
// Adhan + Iqamah (the active prayer filled green/gold). Times come from the shared Kotlin module.
struct ContentView: View {
    private let rows: [DashboardRow] = DashboardDataKt.dashboardRowsForToday()
    private let next: NextPrayerInfo = NextPrayerKt.nextPrayerNow()
    private let started = Date()

    @State private var now = Date()
    private let ticker = Timer.publish(every: 1, on: .main, in: .common).autoconnect()

    private let mapsURL = URL(string: "https://www.google.com/maps/search/?api=1&query=Schwanenweg+13%2C+34123+Kassel")
    private let donateURL = URL(string: "https://www.paypal.com/donate/?business=ikzsandzakkassel@gmail.com&currency_code=EUR")

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
        .onReceive(ticker) { now = $0 }
    }

    // MARK: Header — address (left) · logo (center) · donate (right) + dates

    private var header: some View {
        VStack(spacing: 6) {
            HStack(alignment: .top, spacing: 8) {
                headerItem(systemImage: "mappin.and.ellipse", title: "Schwanenweg 13\n34123 Kassel",
                           emphasized: false, url: mapsURL)
                if let logo = UIImage(named: "logo_community") {
                    Image(uiImage: logo).resizable().scaledToFit().frame(height: 92)
                }
                headerItem(systemImage: "heart.fill", title: "Spenden", emphasized: true, url: donateURL)
            }
            Text(gregorianString).font(.subheadline).foregroundColor(.secondary)
            Text(hijriString).font(.caption).foregroundColor(Color.secondary.opacity(0.75))
        }
        .padding(.top, 4)
    }

    private func headerItem(systemImage: String, title: String, emphasized: Bool, url: URL?) -> some View {
        let content = VStack(spacing: 4) {
            Image(systemName: systemImage)
                .font(.system(size: emphasized ? 26 : 20))
                .foregroundColor(.brandGreen)
            Text(title)
                .font(emphasized ? .subheadline : .caption2)
                .fontWeight(emphasized ? .bold : .regular)
                .foregroundColor(emphasized ? .brandGreen : .secondary)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity)
        return Group {
            if let url = url { Link(destination: url) { content } } else { content }
        }
    }

    // MARK: Green hero with live countdown

    private var countdownCard: some View {
        VStack(spacing: 6) {
            Text("NÄCHSTES GEBET IN")
                .font(.subheadline).fontWeight(.semibold).tracking(1.0)
                .foregroundColor(.brandGoldLight)
            Text(countdown)
                .font(.system(size: 46, weight: .bold, design: .rounded))
                .monospacedDigit()
                .foregroundColor(.white)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 20)
        .background(Color.brandGreen)
        .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
        .shadow(color: .black.opacity(0.18), radius: 8, y: 4)
    }

    // MARK: Prayer card — Adhan + Iqamah

    private func prayerCard(_ row: DashboardRow) -> some View {
        let active = row.name == next.name
        let nameColor = active ? Color.brandGoldLight : Color.brandGreen
        let timeColor = active ? Color.white : Color.primaryText
        let subColor = active ? Color.white.opacity(0.85) : Color.brandGreen
        return HStack(spacing: 0) {
            RoundedRectangle(cornerRadius: 3)
                .fill(active ? Color.clear : Color.brandGreen)
                .frame(width: 5)
                .padding(.vertical, 12)
                .padding(.leading, 10)
            VStack(spacing: 8) {
                HStack {
                    Text(row.name).font(.title3).fontWeight(.semibold).foregroundColor(nameColor)
                    Spacer()
                    Text(row.adhan).font(.system(size: 30, weight: .bold)).foregroundColor(timeColor)
                }
                if let iqamah = row.iqamah {
                    Rectangle()
                        .fill((active ? Color.white : Color.gray).opacity(0.25))
                        .frame(height: 1)
                    HStack {
                        Text("IQAMAH").font(.caption).fontWeight(.medium).foregroundColor(subColor)
                        Spacer()
                        Text(iqamah).font(.system(size: 20, weight: .bold)).foregroundColor(subColor)
                    }
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 14)
        }
        .background(active ? Color.brandGreen : Color.cardBackground)
        .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
        .shadow(color: .black.opacity(0.06), radius: 3, y: 1)
    }

    // MARK: Derived strings

    private var countdown: String {
        let target = started.addingTimeInterval(Double(next.inSeconds))
        let remaining = max(0, Int(target.timeIntervalSince(now)))
        return String(format: "%02d:%02d:%02d", remaining / 3600, (remaining % 3600) / 60, remaining % 60)
    }

    private var gregorianString: String {
        let f = DateFormatter()
        f.locale = Locale(identifier: "de_DE")
        f.dateFormat = "EEEE, d. MMMM yyyy"
        return f.string(from: Date())
    }

    private var hijriString: String {
        let f = DateFormatter()
        f.calendar = Calendar(identifier: .islamicUmmAlQura)
        f.locale = Locale(identifier: "de_DE")
        f.dateFormat = "d. MMMM yyyy"
        return f.string(from: Date()) + " n. H."
    }
}
