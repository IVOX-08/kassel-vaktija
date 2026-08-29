import SwiftUI
import CoreLocation
import Shared

// "Mehr" hub (spec section 5): a list of cards to every extra feature.
//
// Die Ziele sind Werte (MoreDestination), keine fertigen Ansichten. Nur so kann eine angetippte
// Benachrichtigung eine der Seiten aufschlagen, ohne dass jemand die Liste vorher gesehen hat —
// die Tracker-Frage muss beim Tracker landen, nicht auf der Startseite.
struct MoreView: View {
    @ObservedObject private var route = AppRoute.shared
    /// Der Weg im Reiter. Ein Array, kein einzelner Wert: Sonst liesse sich eine geoeffnete Seite
    /// nicht wieder verlassen, ohne den Reiter zu wechseln.
    @State private var path: [MoreDestination] = []

    // Lavender cards mirroring the Android "Mehr" hub (icon · title · chevron).
    var body: some View {
        NavigationStack(path: $path) {
            ScrollView {
                VStack(spacing: 12) {
                    ForEach(MoreDestination.allCases, id: \.self) { item in
                        NavigationLink(value: item) {
                            HStack(spacing: 16) {
                                Image(systemName: item.icon).font(.system(size: 22)).foregroundColor(.appPrimary).frame(width: 28)
                                Text(L(item.titleKey)).font(.inter(17, .medium)).foregroundColor(.appOnSurface)
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
            .navigationDestination(for: MoreDestination.self) { $0.view }
        }
        // Aus der Benachrichtigung. Der Wunsch wird danach geloescht, sonst springt der Reiter bei
        // jedem Hinsehen wieder auf dieselbe Seite zurueck.
        .onReceive(route.$pendingMore.compactMap { $0 }) { destination in
            path = [destination]
            route.pendingMore = nil
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

// MARK: - 5.5 Gebets-Tracker

// TrackerView liegt in einer eigenen Datei (TrackerView.swift): Seit die Gebete feste
// Zeitfenster haben, ist daraus ein eigener Bildschirm geworden.

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

    /// Ob nach dem eigenen Standort gerechnet wird oder nach der Moschee.
    ///
    /// Eine WAHL, kein Automatismus: Beide Zahlen sind richtig, nur für verschiedene Orte. Wer in
    /// der Moschee steht, will die Zahl der Moschee — die hängt dort an der Wand. Wer unterwegs
    /// ist, will die für den Ort, an dem er betet. Ohne die Zeile darunter wäre nicht erklärbar,
    /// warum die Zahl heute eine andere ist als gestern.
    @AppStorage("qibla_use_device") private var useDevice = false

    private var coordinate: CLLocationCoordinate2D? { useDevice ? model.coordinate : nil }

    private var qibla: Double {
        guard let c = coordinate else { return QiblaKt.qiblaDegrees() }
        return QiblaKt.qiblaDegrees(latitude: c.latitude, longitude: c.longitude)
    }

    /// Wohin die Nadel auf dem Zifferblatt zeigt.
    ///
    /// Die Qibla wird gegen GEOGRAFISCH Nord gerechnet, der Kompass meldet gegen MAGNETISCH Nord.
    /// Die Nadel muss deshalb mit dem drehen, was der Kompass sagt; die Gradzahl oben bleibt die
    /// wahre Richtung, weil das die Zahl ist, die für eine Stadt veröffentlicht wird.
    private var needle: Double { qibla - (model.heading ?? 0) }

    private var aligned: Bool {
        guard model.heading != nil else { return false }
        return abs(angleDiff(needle, 0)) < 5
    }

    private func angleDiff(_ a: Double, _ b: Double) -> Double {
        var d = (a - b).truncatingRemainder(dividingBy: 360)
        if d > 180 { d -= 360 }
        if d < -180 { d += 360 }
        return d
    }

    var body: some View {
        ScrollView {
            VStack(spacing: 0) {
                Text(L("nav_qibla"))
                    .font(.inter(30, .bold)).foregroundColor(.brandGreen)
                    .padding(.top, 28)
                Text("\(Int(qibla.rounded()))°")
                    .font(.inter(22, .bold)).foregroundColor(.brandGold)
                    .padding(.top, 6)
                Text(L(coordinate != nil ? "qibla_from_device" : "qibla_from_mosque"))
                    .font(.inter(15)).foregroundColor(.appOnSurfaceVariant)
                    .padding(.top, 2)

                Button {
                    useDevice.toggle()
                    if useDevice { model.start() }
                } label: {
                    Text(L(useDevice ? "qibla_use_mosque" : "qibla_use_device"))
                        .font(.inter(16, .semibold)).foregroundColor(.brandGreen)
                }
                .buttonStyle(.plain)
                .padding(.top, 18)

                dial.padding(.top, 20)

                Text(model.heading == nil ? L("qibla_no_sensor")
                                          : (aligned ? L("qibla_facing") : L("qibla_hint")))
                    .font(.inter(17, aligned ? .bold : .regular))
                    .foregroundColor(aligned ? .brandGreen : .appOnSurface)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 24).padding(.top, 24).padding(.bottom, 24)
            }
            .frame(maxWidth: .infinity)
        }
        .background(Color.appBackground.ignoresSafeArea())
        .navigationTitle(L("nav_qibla")).navigationBarTitleDisplayMode(.inline)
        .onAppear { model.start() }
        .onDisappear { model.stop() }
    }

    // MARK: - Das Zifferblatt

    /// Der feste Zeiger steht AUSSERHALB des Rings und dreht sich nie: Er zeigt, wohin das Handy
    /// schaut. Der Ring darunter dreht gegen den Kompass, die Nadel sitzt auf dem Ring. Damit ist
    /// die Aufgabe für den Nutzer eine einzige — die Nadel unter den Zeiger drehen.
    private var dial: some View {
        ZStack {
            Circle()
                .stroke(Color.appOnSurfaceVariant.opacity(0.35), lineWidth: 1.5)
                .padding(6)

            CompassTicks()
                .stroke(Color.appOnSurfaceVariant.opacity(0.55), lineWidth: 1.5)
                .padding(6)
                .rotationEffect(.degrees(-(model.heading ?? 0)))

            // Die Himmelsrichtungen drehen mit dem Ring, nicht mit dem Bildschirm. Nord ist rot,
            // wie auf jedem Kompass — und der einzige Anhaltspunkt, wenn man das Handy dreht.
            ForEach(CompassPoint.all, id: \.label) { point in
                Text(point.label)
                    .font(.inter(17, .semibold))
                    .foregroundColor(point.isNorth ? .qiblaRed : .appOnSurface)
                    .rotationEffect(.degrees(point.degrees))
                    .offset(y: -108)
                    .rotationEffect(.degrees(-point.degrees))
                    .rotationEffect(.degrees(point.degrees))
            }
            .rotationEffect(.degrees(-(model.heading ?? 0)))

            // Die Nadel: Strich vom Mittelpunkt zum Rand, am Ende der Punkt mit goldenem Ring.
            QiblaNeedle()
                .rotationEffect(.degrees(needle))

            Circle().fill(Color.appOnSurface).frame(width: 9, height: 9)

            VStack {
                Triangle()
                    .fill(aligned ? Color.brandGreen : Color.brandGold)
                    .frame(width: 22, height: 15)
                Spacer()
            }
        }
        .frame(width: 300, height: 300)
        .animation(.easeOut(duration: 0.15), value: model.heading ?? 0)
    }
}

/// Eine Himmelsrichtung auf dem Ring.
private struct CompassPoint {
    let label: String
    let degrees: Double
    var isNorth: Bool { degrees == 0 }

    static let all = [
        CompassPoint(label: "N", degrees: 0),
        CompassPoint(label: "E", degrees: 90),
        CompassPoint(label: "S", degrees: 180),
        CompassPoint(label: "W", degrees: 270),
    ]
}

/// Der Skalenring: alle 5 Grad ein Strich, alle 30 Grad ein längerer.
///
/// Ohne die Striche ist der Ring eine leere Scheibe, auf der sich nicht ablesen lässt, ob man sich
/// um zwei oder um zwanzig Grad gedreht hat.
private struct CompassTicks: Shape {
    func path(in rect: CGRect) -> Path {
        let center = CGPoint(x: rect.midX, y: rect.midY)
        let radius = min(rect.width, rect.height) / 2
        var path = Path()
        for degree in stride(from: 0, to: 360, by: 5) {
            let major = degree % 30 == 0
            let length: CGFloat = major ? 14 : 7
            let angle = (Double(degree) - 90) * .pi / 180
            let outer = CGPoint(x: center.x + cos(angle) * radius,
                                y: center.y + sin(angle) * radius)
            let inner = CGPoint(x: center.x + cos(angle) * (radius - length),
                                y: center.y + sin(angle) * (radius - length))
            path.move(to: inner)
            path.addLine(to: outer)
        }
        return path
    }
}

/// Der grüne Zeiger zur Kaaba, mit dem Punkt am äußeren Ende.
private struct QiblaNeedle: View {
    var body: some View {
        VStack(spacing: 0) {
            Circle()
                .fill(Color.brandGreen)
                .frame(width: 26, height: 26)
                .overlay(Circle().stroke(Color.brandGold, lineWidth: 3))
            Rectangle()
                .fill(Color.brandGreen)
                .frame(width: 5)
            Spacer(minLength: 0)
        }
        .frame(width: 300, height: 300)
        .padding(.top, 12)
        // Die untere Hälfte bleibt leer: ein Zeiger, der über den Mittelpunkt hinausragt, zeigt in
        // zwei Richtungen, und eine davon ist falsch.
        .mask(VStack(spacing: 0) { Rectangle(); Color.clear }.frame(width: 300, height: 300))
    }
}

struct Triangle: Shape {
    func path(in r: CGRect) -> Path {
        var p = Path(); p.move(to: CGPoint(x: r.midX, y: r.maxY))
        p.addLine(to: CGPoint(x: r.minX, y: r.minY)); p.addLine(to: CGPoint(x: r.maxX, y: r.minY)); p.closeSubpath()
        return p
    }
}
