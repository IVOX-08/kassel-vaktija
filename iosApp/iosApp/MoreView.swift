import SwiftUI
import Shared

// "Mehr" tab: a menu to every extra feature (mirrors the Android "More" section).
struct MoreView: View {
    private struct Item: Identifiable {
        let id = UUID()
        let title: String
        let icon: String
        let destination: AnyView
    }

    private var items: [Item] {
        [
            Item(title: "Koran", icon: "book.fill", destination: AnyView(PlaceholderView(title: "Koran", icon: "book.fill", note: "Der arabische Koran wird hier eingebunden (eigener Datensatz)."))),
            Item(title: "Hadithe", icon: "text.book.closed.fill", destination: AnyView(PlaceholderView(title: "Hadithe", icon: "text.book.closed.fill", note: "40 Hadithe an-Nawawi + Riyad us-Salihin (Datensatz folgt)."))),
            Item(title: "Zikr", icon: "heart.text.square.fill", destination: AnyView(PlaceholderView(title: "Zikr", icon: "heart.text.square.fill", note: "Tägliche Bittgebete / Adhkar (Datensatz folgt)."))),
            Item(title: "Tasbih", icon: "circle.circle.fill", destination: AnyView(TasbihView())),
            Item(title: "Gebets-Tracker", icon: "checkmark.circle.fill", destination: AnyView(TrackerView())),
            Item(title: "Ramadan", icon: "moon.stars.fill", destination: AnyView(RamadanView())),
            Item(title: "Qibla", icon: "location.north.line.fill", destination: AnyView(QiblaView())),
        ]
    }

    var body: some View {
        NavigationStack {
            List(items) { item in
                NavigationLink(destination: item.destination) {
                    Label {
                        Text(item.title).foregroundColor(.primaryText)
                    } icon: {
                        Image(systemName: item.icon).foregroundColor(.brandGreen)
                    }
                    .padding(.vertical, 4)
                }
            }
            .navigationTitle("Mehr")
        }
    }
}

// MARK: - Placeholder for content-heavy screens (Koran / Hadithe / Zikr)

struct PlaceholderView: View {
    let title: String
    let icon: String
    let note: String

    var body: some View {
        VStack(spacing: 16) {
            Image(systemName: icon).font(.system(size: 52)).foregroundColor(.brandGreen)
            Text(title).font(.title2).bold()
            Text(note).multilineTextAlignment(.center).foregroundColor(.secondary).padding(.horizontal, 32)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color.pageBackground.ignoresSafeArea())
        .navigationTitle(title)
        .navigationBarTitleDisplayMode(.inline)
    }
}

// MARK: - Tasbih (working tap counter)

struct TasbihView: View {
    @State private var count = 0

    var body: some View {
        VStack(spacing: 28) {
            Text("\(count)")
                .font(.system(size: 84, weight: .bold))
                .foregroundColor(.brandGreen)
                .monospacedDigit()
            Button { count += 1 } label: {
                Circle()
                    .fill(Color.brandGreen)
                    .frame(width: 200, height: 200)
                    .overlay(Text("Tippen").font(.title2).foregroundColor(.white))
                    .shadow(color: .black.opacity(0.2), radius: 8, y: 4)
            }
            Button("Zurücksetzen") { count = 0 }
                .foregroundColor(.brandGreen)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color.pageBackground.ignoresSafeArea())
        .navigationTitle("Tasbih")
        .navigationBarTitleDisplayMode(.inline)
    }
}

// MARK: - Prayer tracker (tap to mark today's prayers)

struct TrackerView: View {
    private let prayers = ["Fajr", "Dhuhr", "Asr", "Maghrib", "Isha"]
    @State private var done: Set<String> = []

    var body: some View {
        List {
            Section {
                ForEach(prayers, id: \.self) { p in
                    Button {
                        if done.contains(p) { done.remove(p) } else { done.insert(p) }
                    } label: {
                        HStack {
                            Image(systemName: done.contains(p) ? "checkmark.circle.fill" : "circle")
                                .foregroundColor(done.contains(p) ? .brandGreen : .secondary)
                            Text(p).foregroundColor(.primaryText)
                            Spacer()
                        }
                    }
                }
            } header: {
                Text("Heute gebetet (\(done.count)/5)")
            }
        }
        .navigationTitle("Gebets-Tracker")
        .navigationBarTitleDisplayMode(.inline)
    }
}

// MARK: - Ramadan (Suhoor / Iftar from today's shared times)

struct RamadanView: View {
    private let rows = DashboardDataKt.dashboardRowsForToday()
    private var suhoor: String { rows.first { $0.name == "Fajr" }?.adhan ?? "--:--" }
    private var iftar: String { rows.first { $0.name == "Maghrib" }?.adhan ?? "--:--" }

    var body: some View {
        VStack(spacing: 20) {
            ramadanCard(title: "Suhoor (Ende)", time: suhoor, icon: "moon.fill")
            ramadanCard(title: "Iftar", time: iftar, icon: "sunset.fill")
        }
        .padding()
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
        .background(Color.pageBackground.ignoresSafeArea())
        .navigationTitle("Ramadan")
        .navigationBarTitleDisplayMode(.inline)
    }

    private func ramadanCard(title: String, time: String, icon: String) -> some View {
        HStack {
            Image(systemName: icon).font(.title).foregroundColor(.white)
            VStack(alignment: .leading) {
                Text(title).foregroundColor(.brandGoldLight).font(.subheadline).fontWeight(.semibold)
                Text(time).foregroundColor(.white).font(.system(size: 36, weight: .bold))
            }
            Spacer()
        }
        .padding(20)
        .background(Color.brandGreen)
        .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
    }
}

// MARK: - Qibla (bearing from shared math + a static compass)

struct QiblaView: View {
    private let degrees = QiblaKt.qiblaDegrees()

    var body: some View {
        VStack(spacing: 24) {
            Text("\(Int(degrees.rounded()))° von Norden")
                .font(.title2).bold().foregroundColor(.primaryText)
            ZStack {
                Circle().stroke(Color.brandGreen, lineWidth: 3).frame(width: 230, height: 230)
                Text("N").offset(y: -100).foregroundColor(.secondary)
                Text("S").offset(y: 100).foregroundColor(.secondary)
                Text("O").offset(x: 100).foregroundColor(.secondary)
                Text("W").offset(x: -100).foregroundColor(.secondary)
                Image(systemName: "location.north.fill")
                    .font(.system(size: 64))
                    .foregroundColor(.brandGreen)
                    .rotationEffect(.degrees(degrees))
            }
            Text("Richtung nach Mekka 🕋").foregroundColor(.secondary)
            Text("Live-Kompass folgt — der Simulator hat keinen Kompass-Sensor.")
                .font(.caption).foregroundColor(.secondary).multilineTextAlignment(.center).padding(.horizontal)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color.pageBackground.ignoresSafeArea())
        .navigationTitle("Qibla")
        .navigationBarTitleDisplayMode(.inline)
    }
}
