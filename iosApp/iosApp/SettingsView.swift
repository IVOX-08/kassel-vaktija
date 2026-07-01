import SwiftUI
import UserNotifications

// Einstellungen (spec 6): a vertically scrolling column, padding 16, spacing 12. Section headings in
// primary/bold, each option group a card. Order: Design → Gebetsbenachrichtigungen →
// Auto-Stummschaltung → Mitteilungen → Berechtigungen (bedingt) → Sprache → Über uns.
struct SettingsView: View {
    @AppStorage("appColorScheme") private var theme = "system"
    // 6.2 prayer notifications
    @AppStorage("notif_master") private var master = true
    @AppStorage("notif_sound") private var soundRaw = NotifSound.adhan.rawValue
    @AppStorage("notif_silent") private var playInSilent = false
    // 6.3 auto-mute
    @AppStorage("automute_on") private var autoMute = false
    @AppStorage("automute_before") private var muteBefore = 5
    @AppStorage("automute_after") private var muteAfter = 10
    // 6.4 announcements
    @AppStorage("msg_notif") private var msgNotif = true
    @AppStorage("weekly_reminder") private var weekly = true
    // 6.6 language
    @AppStorage("app_lang") private var lang = "bs"

    @State private var showLangPicker = false
    @State private var notifGranted = true

    private var sound: NotifSound { NotifSound(rawValue: soundRaw) ?? .adhan }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 12) {
                    designSection
                    prayerNotifSection
                    autoMuteSection
                    announcementsSection
                    if !notifGranted { permissionsSection }
                    languageSection
                    AboutCard()
                }
                .padding(16)
            }
            .background(Color.appBackground.ignoresSafeArea())
            .navigationTitle("Einstellungen")
            .navigationBarTitleDisplayMode(.inline)
        }
        .tint(.brandGreen)
        .onAppear(perform: refreshAuth)
        .fullScreenCover(isPresented: $showLangPicker) {
            LanguagePickerView(
                showClose: true,
                onSelect: { lang = $0.tag; showLangPicker = false },
                onClose: { showLangPicker = false }
            )
        }
    }

    // 6.1 Design
    private var designSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            SettingHeader("Design")
            SettingCard {
                ChipRow(options: [("system", "System"), ("light", "Hell"), ("dark", "Dunkel")],
                        selection: $theme)
            }
        }
    }

    // 6.2 Gebetsbenachrichtigungen
    private var prayerNotifSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            SettingHeader("Gebetsbenachrichtigungen")
            SettingCard {
                Toggle("Benachrichtigungen aktivieren", isOn: $master)
                    .tint(.brandGreen).font(.inter(15, .medium))
                if master {
                    Divider()
                    HStack {
                        Text("Benachrichtigungston").font(.inter(15))
                        Spacer()
                        Menu {
                            ForEach(NotifSound.allCases, id: \.self) { s in
                                Button(s.label) { soundRaw = s.rawValue }
                            }
                        } label: { PillLabel(sound.label) }
                    }
                    Divider()
                    VStack(alignment: .leading, spacing: 4) {
                        Toggle("Adhan auch im Lautlos-Modus abspielen", isOn: $playInSilent)
                            .tint(.brandGreen).font(.inter(15, .medium))
                        Text("Aus: Ist das Handy stumm oder auf Vibration, kommt nur eine stille Benachrichtigung.")
                            .font(.inter(12)).foregroundColor(.appOnSurfaceVariant)
                    }
                }
            }
            if master {
                ForEach(SettingsView.prayers, id: \.0) { key, name in
                    PrayerNotifCard(title: name, key: key)
                }
                Button {
                    SoundPlayer.shared.play(sound.file, ext: sound.ext)
                } label: {
                    Text("Adhan testen").font(.inter(15, .semibold)).foregroundColor(.white)
                        .frame(maxWidth: .infinity).padding(.vertical, 12)
                        .background(Color.brandGreen).clipShape(RoundedRectangle(cornerRadius: 12))
                }
            }
        }
    }

    // 6.3 Auto-Stummschaltung
    private var autoMuteSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            SettingHeader("Auto-Stummschaltung")
            SettingCard {
                Toggle("Während des Gebets stummschalten", isOn: $autoMute)
                    .tint(.brandGreen).font(.inter(15, .medium))
                if autoMute {
                    Divider()
                    Text("Vor dem Adhan").font(.inter(13)).foregroundColor(.appOnSurfaceVariant)
                    ChipRow(options: SettingsView.muteMins, intSelection: $muteBefore)
                    Text("Nach dem Adhan").font(.inter(13)).foregroundColor(.appOnSurfaceVariant)
                    ChipRow(options: SettingsView.muteMins, intSelection: $muteAfter)
                }
            }
        }
    }

    // 6.4 Mitteilungen
    private var announcementsSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            SettingHeader("Mitteilungen")
            SettingCard {
                Toggle("Mitteilungs-Benachrichtigungen", isOn: $msgNotif)
                    .tint(.brandGreen).font(.inter(15, .medium))
                Divider()
                Toggle("Wöchentliche Erinnerung (Dhikr & Hadith)", isOn: $weekly)
                    .tint(.brandGreen).font(.inter(15, .medium))
            }
        }
    }

    // 6.5 Berechtigungen (only while notifications aren't granted)
    private var permissionsSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            SettingHeader("Berechtigungen")
            Button(action: requestNotif) {
                Text("Benachrichtigungen erlauben").font(.inter(15, .semibold)).foregroundColor(.white)
                    .frame(maxWidth: .infinity).padding(.vertical, 12)
                    .background(Color.brandGreen).clipShape(RoundedRectangle(cornerRadius: 12))
            }
        }
    }

    // 6.6 Sprache
    private var languageSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            SettingHeader("Sprache")
            SettingCard {
                Button { showLangPicker = true } label: {
                    HStack(spacing: 8) {
                        FlagThumb(pngName: langForTag(lang).flagPng)
                        Text("Sprache ändern").font(.inter(15, .medium)).foregroundColor(.appPrimary)
                        Spacer()
                        Image(systemName: "chevron.right").font(.system(size: 13)).foregroundColor(.appOnSurfaceVariant)
                    }
                }
            }
        }
    }

    private func refreshAuth() {
        UNUserNotificationCenter.current().getNotificationSettings { s in
            DispatchQueue.main.async { notifGranted = s.authorizationStatus == .authorized }
        }
    }

    private func requestNotif() {
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound, .badge]) { _, _ in
            refreshAuth()
        }
    }

    static let prayers: [(String, String)] = [
        ("fajr", "Morgengebet"), ("dhuhr", "Mittagsgebet"), ("asr", "Nachmittagsgebet"),
        ("maghrib", "Abendgebet"), ("isha", "Nachtgebet"),
    ]
    static let muteMins: [(String, String)] = [("5", "5"), ("10", "10"), ("15", "15"), ("20", "20"), ("30", "30")]
}

// One prayer's notification card: a switch (default on) and — when on — a pre-warning pill (0/5/10/15/30).
private struct PrayerNotifCard: View {
    let title: String
    @AppStorage private var enabled: Bool
    @AppStorage private var warn: Int

    init(title: String, key: String) {
        self.title = title
        _enabled = AppStorage(wrappedValue: true, "pn_\(key)")
        _warn = AppStorage(wrappedValue: 0, "pw_\(key)")
    }

    private let warnValues = [0, 5, 10, 15, 30]

    var body: some View {
        SettingCard {
            Toggle(title, isOn: $enabled).tint(.brandGreen).font(.inter(15, .medium))
            if enabled {
                Divider()
                HStack {
                    Text("Vorwarnung").font(.inter(15))
                    Spacer()
                    Menu {
                        ForEach(warnValues, id: \.self) { v in
                            Button("\(v) Min.") { warn = v }
                        }
                    } label: { PillLabel("\(warn) Min.") }
                }
            }
        }
    }
}

// "About the community" card (spec 6.7): tappable rows + version line with the hidden 7-tap admin gate.
private struct AboutCard: View {
    @Environment(\.openURL) private var openURL
    @State private var versionTaps = 0
    @State private var showAdmin = false

    private let maps = "https://www.google.com/maps/search/?api=1&query=Schwanenweg+13%2C+34123+Kassel"
    private let donate = "https://www.paypal.com/donate/?business=ikzsandzakkassel@gmail.com&currency_code=EUR"

    private var version: String {
        (Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String) ?? "1.0"
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            SettingHeader("Über uns")
            SettingCard {
                Text("IGBD-Gemeinde Sandžak-Kassel")
                    .font(.inter(16, .bold)).foregroundColor(.appPrimary)
                Divider()
                row("mappin.and.ellipse", "Schwanenweg 13\n34123 Kassel") { open(maps) }
                row("envelope.fill", "vorstand@igbdsandzakkassel.de") { open("mailto:vorstand@igbdsandzakkassel.de") }
                row("heart.fill", "Spenden") { open(donate) }
                row("person.fill", "Imam: Alen Golac\n0176 3037 2402") { open("tel:017630372402") }
                row("phone.fill", "App oder Website gewünscht? Anrufen:\n0176 6188 7123") { open("tel:017661887123") }
                row("envelope", "oder E-Mail schreiben:\nmuhamedgolac311@gmail.com") { open("mailto:muhamedgolac311@gmail.com") }
                Divider()
                Text("v\(version)")
                    .font(.inter(12)).foregroundColor(.appOnSurfaceVariant)
                    .onTapGesture {
                        versionTaps += 1
                        if versionTaps >= 7 { showAdmin = true; versionTaps = 0 }
                    }
            }
        }
        .alert("Admin-Login", isPresented: $showAdmin) {
            Button("OK", role: .cancel) {}
        } message: {
            Text("Der Admin-Bereich benötigt die Firebase-Konfiguration vom Vorstand und wird danach freigeschaltet.")
        }
    }

    private func row(_ icon: String, _ text: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            HStack(alignment: .top, spacing: 12) {
                Image(systemName: icon).font(.system(size: 18)).foregroundColor(.appPrimary).frame(width: 24)
                Text(text).font(.inter(14)).foregroundColor(.appOnSurface)
                    .fixedSize(horizontal: false, vertical: true)
                Spacer()
            }
        }
    }

    private func open(_ s: String) { if let u = URL(string: s) { openURL(u) } }
}

// MARK: - Shared building blocks

private struct SettingHeader: View {
    let text: String
    init(_ text: String) { self.text = text }
    var body: some View {
        Text(text).font(.inter(14, .bold)).foregroundColor(.appPrimary)
    }
}

private struct SettingCard<Content: View>: View {
    @ViewBuilder let content: Content
    var body: some View {
        VStack(alignment: .leading, spacing: 10) { content }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(14)
            .background(Color.appSurface)
            .clipShape(RoundedRectangle(cornerRadius: Radius.smallCard))
    }
}

private struct PillLabel: View {
    let text: String
    init(_ text: String) { self.text = text }
    var body: some View {
        HStack(spacing: 4) {
            Text(text).font(.inter(14, .medium))
            Image(systemName: "chevron.up.chevron.down").font(.system(size: 11))
        }
        .foregroundColor(.appPrimary)
        .padding(.horizontal, 12).padding(.vertical, 6)
        .background(Color.brandGreen.opacity(0.12))
        .clipShape(Capsule())
    }
}

// A row of filter chips. Either string-keyed (Design) or int-valued (minutes).
private struct ChipRow: View {
    let options: [(String, String)]
    var selection: Binding<String>?
    var intSelection: Binding<Int>?

    init(options: [(String, String)], selection: Binding<String>) {
        self.options = options; self.selection = selection; self.intSelection = nil
    }
    init(options: [(String, String)], intSelection: Binding<Int>) {
        self.options = options; self.selection = nil; self.intSelection = intSelection
    }

    var body: some View {
        HStack(spacing: 8) {
            ForEach(options, id: \.0) { value, label in
                let active = isActive(value)
                Text(label)
                    .font(.inter(14, active ? .semibold : .regular))
                    .foregroundColor(active ? .white : .appOnSurface)
                    .padding(.horizontal, 14).padding(.vertical, 8)
                    .background(active ? Color.brandGreen : Color.appOnSurface.opacity(0.08))
                    .clipShape(Capsule())
                    .contentShape(Capsule())
                    .onTapGesture { select(value) }
            }
            Spacer(minLength: 0)
        }
    }

    private func isActive(_ v: String) -> Bool {
        if let s = selection { return s.wrappedValue == v }
        if let i = intSelection { return String(i.wrappedValue) == v }
        return false
    }
    private func select(_ v: String) {
        if let s = selection { s.wrappedValue = v }
        if let i = intSelection, let n = Int(v) { i.wrappedValue = n }
    }
}
