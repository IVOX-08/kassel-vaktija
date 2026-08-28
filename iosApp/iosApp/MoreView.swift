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
            Item(title: L("library_quran"), icon: "book.fill", dest: AnyView(QuranView())),
            Item(title: L("library_hadith"), icon: "text.quote", dest: AnyView(HadithView())),
            Item(title: L("library_dhikr"), icon: "figure.mind.and.body", dest: AnyView(DhikrView())),
            Item(title: L("library_tasbih"), icon: "target", dest: AnyView(TasbihView())),
            Item(title: L("library_tracker"), icon: "checkmark.circle", dest: AnyView(TrackerView())),
            Item(title: L("library_ramadan"), icon: "moon.fill", dest: AnyView(RamadanView())),
            Item(title: L("library_zakat"), icon: "plusminus.circle", dest: AnyView(ZakatView())),
            Item(title: L("nav_qibla"), icon: "safari", dest: AnyView(QiblaView())),
        ]
    }

    // Lavender cards mirroring the Android "Mehr" hub (icon · title · chevron).
    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 12) {
                    ForEach(items) { item in
                        NavigationLink(destination: item.dest) {
                            HStack(spacing: 16) {
                                Image(systemName: item.icon).font(.system(size: 22)).foregroundColor(.appPrimary).frame(width: 28)
                                Text(item.title).font(.inter(17, .medium)).foregroundColor(.appOnSurface)
                                Spacer()
                                Image(systemName: "chevron.right").font(.system(size: 14, weight: .semibold)).foregroundColor(.appOnSurfaceVariant)
                            }
                            .padding(.horizontal, 18).padding(.vertical, 18)
                            .background(Color.moreCard)
                            .clipShape(RoundedRectangle(cornerRadius: 16))
                        }
                    }
                }
                .padding(16)
            }
            .background(Color.appBackground.ignoresSafeArea())
            .navigationTitle(L("nav_more"))
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
            Text(String(format: L("tasbih_rounds"), rounds)).font(.inter(15)).foregroundColor(.appOnSurfaceVariant)
            Text(L("tasbih_hint")).font(.inter(13)).foregroundColor(.appOnSurfaceVariant)
            Spacer()
            Button(L("tasbih_reset")) { count = 0 }.font(.inter(15, .semibold)).foregroundColor(.brandGreen)
        }
        .padding()
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color.appBackground.ignoresSafeArea())
        .navigationTitle(L("library_tasbih")).navigationBarTitleDisplayMode(.inline)
    }
}

// MARK: - 5.5 Gebets-Tracker (5-bit mask per day + streak)

struct TrackerView: View {
    // The five daily prayers in the selected app language (sunrise is not tracked).
    private var names: [String] {
        ["prayer_fajr", "prayer_dhuhr", "prayer_asr", "prayer_maghrib", "prayer_isha"].map { L($0) }
    }
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
                        Text(L("tracker_streak")).font(.inter(13)).foregroundColor(.appOnSurfaceVariant)
                    }
                    Spacer()
                    Text("\(doneCount) / 5").font(.inter(24, .bold)).foregroundColor(doneCount == 5 ? .brandGold : .appOnSurfaceVariant)
                }
                .padding().background(Color.brandGreen.opacity(0.12)).clipShape(RoundedRectangle(cornerRadius: Radius.smallCard))

                Text(L("tracker_today")).font(.inter(17, .bold)).foregroundColor(.appOnSurface).frame(maxWidth: .infinity, alignment: .leading)

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
        .navigationTitle(L("library_tracker")).navigationBarTitleDisplayMode(.inline)
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

// RamadanView moved to its own file (RamadanView.swift) with the redesigned layout.

// MARK: - 5.7 Qibla (fixed bearing from shared + live compass)

/// Kompassdrehung des Geräts und — falls erlaubt — seine ungefähre Position.
///
/// Der Standort ist bewusst grob und wird nur im Vordergrund geholt, solange der Kompass offen ist.
/// Die Richtung zur Kaaba ändert sich über einen Kilometer um weit weniger als ein hundertstel
/// Grad; eine stadtgenaue Ortung ist also genauso gut wie eine metergenaue — und deutlich leichter
/// zu rechtfertigen. Die Position verlässt das Gerät nicht.
final class HeadingModel: NSObject, ObservableObject, CLLocationManagerDelegate {
    @Published var heading: Double?
    /// Ungefähre Position des Geräts, oder nil ohne Berechtigung bzw. ohne Ortung.
    @Published var coordinate: CLLocationCoordinate2D?

    private let manager = CLLocationManager()

    override init() {
        super.init()
        manager.delegate = self
        // Stadtgenau reicht — alles Feinere kostet Akku und wäre nicht zu begründen.
        manager.desiredAccuracy = kCLLocationAccuracyKilometer
    }

    func start() {
        if CLLocationManager.headingAvailable() { manager.startUpdatingHeading() }
        // Die zuletzt bekannte Ortung liegt meist schon vor und kommt sofort, auch im Gebäude.
        coordinate = manager.location?.coordinate
        switch manager.authorizationStatus {
        case .notDetermined:
            manager.requestWhenInUseAuthorization()
        case .authorizedWhenInUse, .authorizedAlways:
            manager.startUpdatingLocation()
        default:
            // Abgelehnt: die Richtung der Moschee bleibt, die Ansicht sagt es dem Nutzer.
            break
        }
    }

    func stop() {
        manager.stopUpdatingHeading()
        manager.stopUpdatingLocation()
    }

    func locationManager(_ m: CLLocationManager, didUpdateHeading newHeading: CLHeading) {
        // trueHeading, nicht magneticHeading: Die Richtung zur Kaaba wird gegen GEOGRAFISCH Nord
        // gerechnet, der Kompass misst gegen MAGNETISCH Nord. Beides direkt zu vergleichen ist
        // falsch — über Deutschland 3 bis 5 Grad, weiter östlich mehr. Genau die Abweichung, die
        // beim Ausprobieren aufgefallen ist.
        //
        // trueHeading ist erst gültig, wenn der Standort bekannt ist (sonst -1); bis dahin bleibt
        // die magnetische Messung besser als gar keine Nadel.
        heading = newHeading.trueHeading >= 0 ? newHeading.trueHeading : newHeading.magneticHeading
    }

    func locationManager(_ m: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        guard let last = locations.last else { return }
        coordinate = last.coordinate
    }

    func locationManagerDidChangeAuthorization(_ m: CLLocationManager) {
        // Erlaubt der Nutzer erst im Dialog, soll die Richtung ohne Neuöffnen stimmen.
        if m.authorizationStatus == .authorizedWhenInUse || m.authorizationStatus == .authorizedAlways {
            m.startUpdatingLocation()
            coordinate = m.location?.coordinate
        }
    }
}

struct QiblaView: View {
    @StateObject private var model = HeadingModel()
    /// Vom eigenen Standort, sobald einer vorliegt — sonst von der Moschee. Der feste Wert war
    /// innerhalb Deutschlands knapp daneben, aber ab Sarajevo oder Istanbul deutlich falsch.
    private var qibla: Double {
        guard let c = model.coordinate else { return QiblaKt.qiblaDegrees() }
        return QiblaKt.qiblaDegrees(latitude: c.latitude, longitude: c.longitude)
    }
    private var usingDeviceLocation: Bool { model.coordinate != nil }
    private var aligned: Bool { guard let h = model.heading else { return false }; return abs(angleDiff(qibla, h)) < 5 }
    private func angleDiff(_ a: Double, _ b: Double) -> Double { var d = (a - b).truncatingRemainder(dividingBy: 360); if d > 180 { d -= 360 }; if d < -180 { d += 360 }; return d }

    var body: some View {
        VStack(spacing: 16) {
            Text(L("nav_qibla")).font(.inter(22, .bold)).foregroundColor(.appPrimary)
            Text("\(Int(qibla.rounded()))°").font(.inter(17)).foregroundColor(.appSecondary)
            // Welche der beiden Quellen gerade gilt — sonst wäre nicht erklärbar, warum die Zahl
            // unterwegs eine andere ist als zu Hause.
            Text(L(usingDeviceLocation ? "qibla_from_device" : "qibla_from_mosque"))
                .font(.inter(12)).foregroundColor(.appOnSurfaceVariant)
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
                Text(L("qibla_no_sensor"))
                    .font(.inter(14)).foregroundColor(.appOnSurfaceVariant).multilineTextAlignment(.center)
            }
        }
        .padding()
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color.appBackground.ignoresSafeArea())
        .navigationTitle(L("nav_qibla")).navigationBarTitleDisplayMode(.inline)
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
