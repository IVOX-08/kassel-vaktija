import SwiftUI
import CoreLocation
import Shared

// "Mehr" hub (spec section 5): a list of cards to every extra feature.
struct MoreView: View {
    private struct Item: Identifiable {
        let id = UUID(); let title: String; let icon: String; let dest: AnyView
    }
    private var items: [Item] {
        [
            Item(title: "Koran", icon: "book.fill", dest: AnyView(PlaceholderView(title: "Koran", icon: "book.fill", note: "Der arabische Koran (Uthmani) wird hier eingebunden — die 115 JSON-Dateien liegen schon im Projekt."))),
            Item(title: "Hadith", icon: "text.book.closed.fill", dest: AnyView(PlaceholderView(title: "Hadith", icon: "text.book.closed.fill", note: "40 Hadithe an-Nawawi + Riyad us-Salihin (echte Übersetzungen, 8 Sprachen) — Assets liegen bereit."))),
            Item(title: "Dhikr", icon: "heart.text.square.fill", dest: AnyView(PlaceholderView(title: "Dhikr", icon: "heart.text.square.fill", note: "24 Adhkar (Arabisch + Transliteration + Bedeutung) — werden aus dem Code portiert."))),
            Item(title: "Tasbih", icon: "circle.circle.fill", dest: AnyView(TasbihView())),
            Item(title: "Gebets-Tracker", icon: "checkmark.circle.fill", dest: AnyView(TrackerView())),
            Item(title: "Ramadan", icon: "moon.stars.fill", dest: AnyView(RamadanView())),
            Item(title: "Qibla", icon: "location.north.line.fill", dest: AnyView(QiblaView())),
        ]
    }

    var body: some View {
        NavigationStack {
            List(items) { item in
                NavigationLink(destination: item.dest) {
                    Label {
                        Text(item.title).font(.inter(17)).foregroundColor(.appOnSurface)
                    } icon: {
                        Image(systemName: item.icon).foregroundColor(.appPrimary)
                    }.padding(.vertical, 4)
                }
            }
            .navigationTitle("Mehr")
        }
    }
}

// MARK: - Placeholder (content-heavy screens still to wire)

struct PlaceholderView: View {
    let title: String; let icon: String; let note: String
    var body: some View {
        VStack(spacing: 16) {
            Image(systemName: icon).font(.system(size: 52)).foregroundColor(.appPrimary)
            Text(title).font(.inter(22, .bold)).foregroundColor(.appOnSurface)
            Text(note).font(.inter(14)).foregroundColor(.appOnSurfaceVariant).multilineTextAlignment(.center).padding(.horizontal, 32)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color.appBackground.ignoresSafeArea())
        .navigationTitle(title).navigationBarTitleDisplayMode(.inline)
    }
}

// MARK: - 5.4 Tasbih (targets 33/99/∞, progress ring, haptics)

struct TasbihView: View {
    @AppStorage("tasbih_count") private var count = 0
    @AppStorage("tasbih_target") private var target = 33
    private let targets = [33, 99, 0] // 0 = ∞
    private var inRound: Int { target == 0 ? count : count % target }
    private var rounds: Int { target == 0 ? 0 : count / target }
    private let tapHaptic = UIImpactFeedbackGenerator(style: .light)
    private let roundHaptic = UINotificationFeedbackGenerator()

    var body: some View {
        VStack(spacing: 20) {
            HStack(spacing: 10) {
                ForEach(targets, id: \.self) { t in
                    Button { target = t; count = 0 } label: {
                        Text(t == 0 ? "∞" : "\(t)")
                            .font(.inter(15, .semibold))
                            .padding(.horizontal, 18).padding(.vertical, 8)
                            .background(target == t ? Color.brandGreen : Color.appSurface)
                            .foregroundColor(target == t ? .white : .appPrimary)
                            .clipShape(Capsule())
                    }
                }
            }.padding(.top, 12)
            Spacer()
            Button {
                count += 1; tapHaptic.impactOccurred()
                if target != 0 && count % target == 0 { roundHaptic.notificationOccurred(.success) }
            } label: {
                ZStack {
                    Circle().fill(Color.brandGreen.opacity(0.08))
                    Circle().stroke(Color.brandGreen.opacity(0.15), lineWidth: 10)
                    Circle().trim(from: 0, to: target == 0 ? 0 : CGFloat(inRound) / CGFloat(target))
                        .stroke(Color.brandGreen, style: StrokeStyle(lineWidth: 10, lineCap: .round))
                        .rotationEffect(.degrees(-90))
                    VStack(spacing: 4) {
                        Text("\(count)").font(.inter(64, .bold)).foregroundColor(.brandGreen).monospacedDigit()
                        Text(target == 0 ? "\(inRound)" : "\(inRound) / \(target)").font(.inter(15)).foregroundColor(.appOnSurfaceVariant)
                    }
                }.frame(width: 260, height: 260)
            }.buttonStyle(.plain)
            Text("Runden: \(rounds)").font(.inter(15)).foregroundColor(.appOnSurfaceVariant)
            Text("Tippen zum Zählen").font(.inter(13)).foregroundColor(.appOnSurfaceVariant)
            Spacer()
            Button("Zurücksetzen") { count = 0 }.font(.inter(15, .semibold)).foregroundColor(.brandGreen)
        }
        .padding()
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color.appBackground.ignoresSafeArea())
        .navigationTitle("Tasbih").navigationBarTitleDisplayMode(.inline)
    }
}

// MARK: - 5.5 Gebets-Tracker (5-bit mask per day + streak)

struct TrackerView: View {
    private let names = ["Morgengebet", "Mittagsgebet", "Nachmittagsgebet", "Abendgebet", "Nachtgebet"]
    @State private var mask = TrackerStore.maskFor(TrackerStore.today())
    private var doneCount: Int { (0..<5).filter { mask & (1 << $0) != 0 }.count }
    private var streak: Int { TrackerStore.streak() }

    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                // Streak card
                HStack {
                    Text("🔥").font(.system(size: 40))
                    VStack(alignment: .leading) {
                        Text("\(streak)").font(.inter(32, .bold)).foregroundColor(.brandGreen)
                        Text("Tage in Folge").font(.inter(13)).foregroundColor(.appOnSurfaceVariant)
                    }
                    Spacer()
                    Text("\(doneCount) / 5").font(.inter(24, .bold)).foregroundColor(doneCount == 5 ? .brandGold : .appOnSurfaceVariant)
                }
                .padding().background(Color.brandGreen.opacity(0.12)).clipShape(RoundedRectangle(cornerRadius: Radius.smallCard))

                Text("Heute").font(.inter(17, .bold)).foregroundColor(.appOnSurface).frame(maxWidth: .infinity, alignment: .leading)

                VStack(spacing: 0) {
                    ForEach(0..<5, id: \.self) { i in
                        Button { toggle(i) } label: {
                            HStack {
                                Image(systemName: mask & (1 << i) != 0 ? "checkmark.circle.fill" : "circle")
                                    .foregroundColor(mask & (1 << i) != 0 ? .brandGreen : .appOnSurfaceVariant)
                                Text(names[i]).font(.inter(16)).foregroundColor(.appOnSurface)
                                Spacer()
                            }.padding(.vertical, 12)
                        }
                        if i < 4 { Divider() }
                    }
                }.padding(.horizontal, 16).background(Color.appSurface).clipShape(RoundedRectangle(cornerRadius: Radius.smallCard))
            }.padding()
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color.appBackground.ignoresSafeArea())
        .navigationTitle("Gebets-Tracker").navigationBarTitleDisplayMode(.inline)
    }

    private func toggle(_ i: Int) {
        mask ^= (1 << i)
        TrackerStore.setMask(mask, for: TrackerStore.today())
    }
}

enum TrackerStore {
    static func today() -> String { key(for: Date()) }
    private static func key(for date: Date) -> String {
        let f = DateFormatter(); f.dateFormat = "yyyy-MM-dd"; return "d_" + f.string(from: date)
    }
    static func maskFor(_ k: String) -> Int { UserDefaults.standard.integer(forKey: k) }
    static func setMask(_ m: Int, for k: String) { UserDefaults.standard.set(m, forKey: k) }
    static func streak() -> Int {
        var n = 0
        let cal = Calendar.current
        var day = Date()
        // if today isn't complete, start counting from yesterday
        if maskFor(key(for: day)) != 31 { day = cal.date(byAdding: .day, value: -1, to: day)! }
        while maskFor(key(for: day)) == 31 {
            n += 1
            day = cal.date(byAdding: .day, value: -1, to: day)!
        }
        return n
    }
}

// MARK: - 5.6 Ramadan (countdown to iftar / sehur + 3 rows + fasted toggle)

struct RamadanView: View {
    private let rows = DashboardDataKt.dashboardRowsForToday()
    @AppStorage("fasted_today") private var fasted = false
    @State private var now = Date()
    private let ticker = Timer.publish(every: 1, on: .main, in: .common).autoconnect()

    private func time(_ name: String) -> String { rows.first { $0.name == name }?.adhan ?? "--:--" }
    private func date(_ hhmm: String, addDays: Int = 0) -> Date {
        let parts = hhmm.split(separator: ":").compactMap { Int($0) }
        var c = Calendar.current.dateComponents([.year, .month, .day], from: Date())
        c.hour = parts.first ?? 0; c.minute = parts.count > 1 ? parts[1] : 0
        let base = Calendar.current.date(from: c) ?? Date()
        return Calendar.current.date(byAdding: .day, value: addDays, to: base) ?? base
    }
    private var fasting: Bool { now >= date(time("Fajr")) && now < date(time("Maghrib")) }
    private var label: String { fasting ? "Bis zum Iftar" : "Bis Sehur-Ende" }
    private var targetDate: Date {
        if fasting { return date(time("Maghrib")) }
        return now < date(time("Fajr")) ? date(time("Fajr")) : date(time("Fajr"), addDays: 1)
    }
    private var countdown: String {
        let r = max(0, Int(targetDate.timeIntervalSince(now)))
        return String(format: "%02d:%02d:%02d", r / 3600, (r % 3600) / 60, r % 60)
    }

    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                VStack(spacing: 6) {
                    Text(label).font(.inter(16, .semibold)).foregroundColor(.brandGoldLight)
                    Text(countdown).font(.inter(52, .bold)).monospacedDigit().foregroundColor(.white)
                }
                .frame(maxWidth: .infinity).padding(.vertical, 20)
                .background(Color.brandGreen).clipShape(RoundedRectangle(cornerRadius: Radius.hero))

                infoRow("Iftar", time("Maghrib"))
                infoRow("Sehur", time("Fajr"))
                infoRow("Tarawih", time("Isha"))

                Toggle("Heute gefastet", isOn: $fasted).font(.inter(16)).tint(.brandGreen)
                    .padding().background(Color.appSurface).clipShape(RoundedRectangle(cornerRadius: Radius.smallCard))
            }.padding()
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color.appBackground.ignoresSafeArea())
        .navigationTitle("Ramadan").navigationBarTitleDisplayMode(.inline)
        .onReceive(ticker) { now = $0 }
    }

    private func infoRow(_ title: String, _ value: String) -> some View {
        HStack {
            Text(title).font(.inter(16)).foregroundColor(.appOnSurface)
            Spacer()
            Text(value).font(.inter(20, .bold)).foregroundColor(.brandGreen).monospacedDigit()
        }.padding().background(Color.appSurface).clipShape(RoundedRectangle(cornerRadius: Radius.smallCard))
    }
}

// MARK: - 5.7 Qibla (fixed bearing from shared + live compass)

final class HeadingModel: NSObject, ObservableObject, CLLocationManagerDelegate {
    @Published var heading: Double?
    private let manager = CLLocationManager()
    override init() { super.init(); manager.delegate = self }
    func start() { if CLLocationManager.headingAvailable() { manager.startUpdatingHeading() } }
    func stop() { manager.stopUpdatingHeading() }
    func locationManager(_ m: CLLocationManager, didUpdateHeading newHeading: CLHeading) {
        heading = newHeading.magneticHeading
    }
}

struct QiblaView: View {
    @StateObject private var model = HeadingModel()
    private let qibla = QiblaKt.qiblaDegrees()
    private var aligned: Bool { guard let h = model.heading else { return false }; return abs(angleDiff(qibla, h)) < 5 }
    private func angleDiff(_ a: Double, _ b: Double) -> Double { var d = (a - b).truncatingRemainder(dividingBy: 360); if d > 180 { d -= 360 }; if d < -180 { d += 360 }; return d }

    var body: some View {
        VStack(spacing: 16) {
            Text("Qibla").font(.inter(22, .bold)).foregroundColor(.appPrimary)
            Text("\(Int(qibla.rounded()))°").font(.inter(17)).foregroundColor(.appSecondary)
            ZStack {
                Circle().stroke(Color.appOnSurfaceVariant.opacity(0.3), lineWidth: 2)
                // North marker (red)
                VStack { Text("N").font(.inter(14, .bold)).foregroundColor(.qiblaRed); Spacer() }
                // Qibla marker (green line + gold dot)
                VStack {
                    Circle().fill(Color.brandGoldLight).frame(width: 16, height: 16)
                        .overlay(Circle().stroke(Color.brandGold, lineWidth: 2))
                    Rectangle().fill(Color.brandGreen).frame(width: 4, height: 120)
                    Spacer()
                }
                .rotationEffect(.degrees(qibla - (model.heading ?? 0)))
                // Fixed top pointer
                VStack {
                    Triangle().fill(aligned ? Color.brandGreen : Color.brandGold).frame(width: 22, height: 16)
                    Spacer()
                }.offset(y: -22)
            }
            .frame(width: 300, height: 300)
            .rotationEffect(.degrees(-(model.heading ?? 0)))   // dial counter-rotates with the device

            if model.heading != nil {
                Text(aligned ? "Du schaust Richtung Qibla" : "Halte das Handy flach und drehe dich, bis der Pfeil nach oben zeigt.")
                    .font(.inter(15, aligned ? .bold : .regular))
                    .foregroundColor(aligned ? .brandGreen : .appOnSurfaceVariant)
                    .multilineTextAlignment(.center)
            } else {
                Text("Kompass-Sensor auf diesem Gerät nicht verfügbar.")
                    .font(.inter(14)).foregroundColor(.appOnSurfaceVariant).multilineTextAlignment(.center)
            }
        }
        .padding()
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color.appBackground.ignoresSafeArea())
        .navigationTitle("Qibla").navigationBarTitleDisplayMode(.inline)
        .onAppear { model.start() }
        .onDisappear { model.stop() }
    }
}

struct Triangle: Shape {
    func path(in r: CGRect) -> Path {
        var p = Path(); p.move(to: CGPoint(x: r.midX, y: r.maxY))
        p.addLine(to: CGPoint(x: r.minX, y: r.minY)); p.addLine(to: CGPoint(x: r.maxX, y: r.minY)); p.closeSubpath()
        return p
    }
}
