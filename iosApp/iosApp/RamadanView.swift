import SwiftUI

// Ramadan (update prompt #1) — redesigned: day badge, progress ring (fasting/night window),
// Sehur/Iftar/Teravih times card, the Iftar dua (multilingual meaning), and a fasting counter.
// Uses the official vaktija.eu times via PrayerStore. UI labels are German for now; the Iftar dua
// meanings are the exact translations supplied by the community (never machine-translated).
struct RamadanView: View {
    @StateObject private var store = PrayerStore()
    @State private var now = Date()
    @State private var fastedToday = false
    private let ticker = Timer.publish(every: 1, on: .main, in: .common).autoconnect()

    // The Iftar dua meaning per language (from the community; Arabic shows only the original).
    private let duaMeaning: [String: String] = [
        "de": "O Allah, für Dich habe ich gefastet und mit Deiner Versorgung habe ich das Fasten gebrochen.",
        "bs": "Allahu moj, Tebi sam postio i Tvojom opskrbom se iftario.",
        "en": "O Allah, for You I have fasted and with Your provision I have broken my fast.",
        "tr": "Ey Allahım! Senin için oruç tuttum ve Senin rızkınla orucumu açtım.",
        "sq": "O Allah, për Ty agjërova dhe me furnizimin Tënd e çela agjërimin.",
        "ur": "اے اللہ! میں نے تیرے لیے روزہ رکھا اور تیرے رزق سے افطار کیا۔",
        "ru": "О Аллах, ради Тебя я постился и Твоим уделом разговелся.",
    ]

    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                dayBadge
                progressCard
                timesCard
                duaCard
                if isRamadan { fastingCounter }
            }
            .padding()
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color.appBackground.ignoresSafeArea())
        .navigationTitle(L("library_ramadan")).navigationBarTitleDisplayMode(.inline)
        .onReceive(ticker) { now = $0 }
        .task { await store.refresh() }
        .onAppear { fastedToday = UserDefaults.standard.bool(forKey: fastKey()) }
    }

    // a) Day badge
    private var dayBadge: some View {
        VStack(spacing: 4) {
            if isRamadan {
                Text("🌙 \(hijriDay). Ramadan \(hijriYear)")
                    .font(.inter(20, .bold)).foregroundColor(.brandGreen)
                Text("Ramadan Mubarak!").font(.inter(16, .semibold)).foregroundColor(.appSecondary)
            } else {
                Text("🌙 Ramadan beginnt in \(daysUntilRamadan) Tagen")
                    .font(.inter(18, .bold)).foregroundColor(.brandGreen)
                    .multilineTextAlignment(.center)
            }
        }
    }

    // b) Progress ring hero
    private var progressCard: some View {
        ZStack {
            Circle().stroke(Color.white.opacity(0.22), style: StrokeStyle(lineWidth: 14, lineCap: .round))
            Circle().trim(from: 0, to: progress)
                .stroke(Color.brandGoldLight, style: StrokeStyle(lineWidth: 14, lineCap: .round))
                .rotationEffect(.degrees(-90))
            VStack(spacing: 6) {
                Text(fasting ? "Bis zum Iftar" : "Bis zum Ende des Sehur")
                    .font(.inter(14, .semibold)).foregroundColor(.brandGoldLight)
                Text(countdown).font(.inter(40, .bold)).monospacedDigit().foregroundColor(.white)
            }
        }
        .frame(width: 240, height: 240)
        .padding(24)
        .frame(maxWidth: .infinity)
        .background(Color.brandGreen)
        .clipShape(RoundedRectangle(cornerRadius: Radius.hero))
    }

    // c) Sehur / Iftar / Teravih times card
    private var timesCard: some View {
        HStack(spacing: 0) {
            timeColumn("🌙", L("ramadan_sehur"), hhmm(store.today.fajr), hint: "(Imsak)")
            Divider().frame(height: 64)
            timeColumn("🌇", L("ramadan_iftar"), hhmm(store.today.maghrib))
            Divider().frame(height: 64)
            timeColumn("🕌", L("ramadan_teravija"), hhmm(store.today.isha))
        }
        .padding(.vertical, 16)
        .frame(maxWidth: .infinity)
        .background(Color.appSurface)
        .clipShape(RoundedRectangle(cornerRadius: Radius.smallCard))
    }

    private func timeColumn(_ emoji: String, _ label: String, _ time: String, hint: String? = nil) -> some View {
        VStack(spacing: 4) {
            Text(emoji).font(.system(size: 24))
            Text(label).font(.inter(13)).foregroundColor(.appOnSurfaceVariant)
            Text(time).font(.inter(18, .bold)).foregroundColor(.brandGreen).monospacedDigit()
            if let hint { Text(hint).font(.inter(11)).foregroundColor(.appOnSurfaceVariant) }
        }
        .frame(maxWidth: .infinity)
    }

    // d) Iftar dua card
    private var duaCard: some View {
        VStack(spacing: 10) {
            Text("Bittgebet beim Fastenbrechen").font(.inter(15, .bold)).foregroundColor(.brandGreen)
            Text("اللَّهُمَّ لَكَ صُمْتُ وَعَلَى رِزْقِكَ أَفْطَرْتُ")
                .font(.system(size: 24, weight: .bold)).foregroundColor(.appOnSurface)
                .multilineTextAlignment(.center)
                .environment(\.layoutDirection, .rightToLeft)
            if Localization.shared.lang != "ar" {
                Text("Allāhumma laka ṣumtu wa ʿalā rizqika afṭartu")
                    .font(.inter(14)).italic().foregroundColor(.appSecondary)
                    .multilineTextAlignment(.center)
                Text(duaMeaning[Localization.shared.lang] ?? duaMeaning["de"]!)
                    .font(.inter(14)).foregroundColor(.appOnSurfaceVariant)
                    .multilineTextAlignment(.center)
            }
        }
        .padding()
        .frame(maxWidth: .infinity)
        .background(Color.appSurface)
        .clipShape(RoundedRectangle(cornerRadius: Radius.smallCard))
    }

    // e) Fasting counter (Ramadan only)
    private var fastingCounter: some View {
        VStack(spacing: 10) {
            Text("\(fastedCount) von \(ramadanLength) Tagen gefastet")
                .font(.inter(16, .bold)).foregroundColor(.brandGreen)
            Toggle(L("ramadan_fasted_today"), isOn: Binding(
                get: { fastedToday },
                set: { fastedToday = $0; UserDefaults.standard.set($0, forKey: fastKey()) }
            )).font(.inter(15)).tint(.brandGreen)
        }
        .padding()
        .frame(maxWidth: .infinity)
        .background(Color.appSurface)
        .clipShape(RoundedRectangle(cornerRadius: Radius.smallCard))
    }

    // MARK: - Hijri / progress helpers

    private var islamic: Calendar { Calendar(identifier: .islamicUmmAlQura) }
    private var isRamadan: Bool { islamic.component(.month, from: now) == 9 }
    private var hijriDay: Int { islamic.component(.day, from: now) }
    private var hijriYear: Int { islamic.component(.year, from: now) }
    private var ramadanLength: Int { islamic.range(of: .day, in: .month, for: now)?.count ?? 30 }

    private var daysUntilRamadan: Int {
        let greg = Calendar.current
        for i in 0...400 {
            guard let d = greg.date(byAdding: .day, value: i, to: now) else { continue }
            if islamic.component(.month, from: d) == 9 && islamic.component(.day, from: d) == 1 { return i }
        }
        return 0
    }

    private var fastedCount: Int {
        // Count fasted-day flags within the current Ramadan.
        let greg = Calendar.current
        guard let firstOffset = (0...400).first(where: { i in
            guard let d = greg.date(byAdding: .day, value: -i, to: now) else { return false }
            return islamic.component(.month, from: d) == 9 && islamic.component(.day, from: d) == 1
        }) else { return 0 }
        guard let start = greg.date(byAdding: .day, value: -firstOffset, to: now) else { return 0 }
        var count = 0
        for i in 0..<ramadanLength {
            if let d = greg.date(byAdding: .day, value: i, to: start),
               UserDefaults.standard.bool(forKey: "f_\(isoKey(d))") { count += 1 }
        }
        return count
    }

    private var nowSec: Int {
        let c = Calendar.current.dateComponents([.hour, .minute, .second], from: now)
        return (c.hour ?? 0) * 3600 + (c.minute ?? 0) * 60 + (c.second ?? 0)
    }
    private var fasting: Bool { nowSec >= store.today.fajr * 60 && nowSec < store.today.maghrib * 60 }

    private var progress: CGFloat {
        let fajr = store.today.fajr * 60, maghrib = store.today.maghrib * 60
        if fasting {
            let total = max(1, maghrib - fajr)
            return CGFloat(nowSec - fajr) / CGFloat(total)
        }
        // Night window: Maghrib → next Fajr.
        let total = max(1, (fajr + 86_400) - maghrib)
        let elapsed = nowSec >= maghrib ? nowSec - maghrib : nowSec + (86_400 - maghrib)
        return CGFloat(elapsed) / CGFloat(total)
    }

    private var countdown: String {
        let fajr = store.today.fajr * 60, maghrib = store.today.maghrib * 60
        let remaining: Int
        if fasting { remaining = maghrib - nowSec }
        else if nowSec < fajr { remaining = fajr - nowSec }
        else { remaining = (fajr + 86_400) - nowSec }
        let r = max(0, remaining)
        return String(format: "%02d:%02d:%02d", r / 3600, (r % 3600) / 60, r % 60)
    }

    private func hhmm(_ m: Int) -> String { DayTimes.hhmm(m) }
    private func isoKey(_ d: Date) -> String {
        let f = DateFormatter(); f.calendar = Calendar(identifier: .gregorian)
        f.locale = Locale(identifier: "en_US_POSIX"); f.dateFormat = "yyyy-MM-dd"
        return f.string(from: d)
    }
    private func fastKey() -> String { "f_\(isoKey(now))" }
}
