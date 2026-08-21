import SwiftUI
import UserNotifications

// Einstellungen (spec 6): a vertically scrolling column, padding 16, spacing 12. Section headings in
// primary/bold, each option group a card. All labels come from the selected app language.
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

    @ObservedObject private var admin = AdminStore.shared
    @State private var showLangPicker = false
    @State private var notifGranted = true
    @StateObject private var store = PrayerStore()

    private var sound: NotifSound { NotifSound(rawValue: soundRaw) ?? .adhan }

    /// Any change to the notification settings (or the language) must re-arm the scheduled
    /// notifications so they fire with the new texts, sound and pre-warn times.
    private func rearm() {
        Task { await NotificationScheduler.reschedule(times: store.today) }
    }

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
                    // Only visible once the board's account is signed in (via the 7-tap gate below).
                    if admin.isAdmin { AdminSection() }
                    AboutCard()
                }
                .padding(16)
            }
            .background(Color.appBackground.ignoresSafeArea())
            .navigationTitle(L("nav_settings"))
            .navigationBarTitleDisplayMode(.inline)
        }
        .tint(.brandGreen)
        .onAppear { refreshAuth(); admin.start() }
        .onChange(of: master) { _ in rearm() }
        .onChange(of: soundRaw) { _ in rearm() }
        .fullScreenCover(isPresented: $showLangPicker) {
            LanguagePickerView(
                showClose: true,
                onSelect: { lang in
                    Localization.shared.set(lang.tag)
                    showLangPicker = false
                    rearm() // notifications must speak the newly chosen language
                },
                onClose: { showLangPicker = false }
            )
        }
    }

    // 6.1 Design
    private var designSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            SettingHeader(L("settings_theme_header"))
            SettingCard {
                ChipRow(options: [("system", L("theme_system")), ("light", L("theme_light")), ("dark", L("theme_dark"))],
                        selection: $theme)
            }
        }
    }

    // 6.2 Gebetsbenachrichtigungen
    private var prayerNotifSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            SettingHeader(L("settings_notifications_header"))
            SettingCard {
                Toggle(L("settings_master_toggle"), isOn: $master)
                    .tint(.brandGreen).font(.inter(15, .medium))
                if master {
                    Divider()
                    HStack {
                        Text(L("settings_notification_sound")).font(.inter(15))
                        Spacer()
                        Menu {
                            ForEach(NotifSound.allCases, id: \.self) { s in
                                Button(s.label) { soundRaw = s.rawValue }
                            }
                        } label: { PillLabel(sound.label) }
                    }
                    Divider()
                    VStack(alignment: .leading, spacing: 4) {
                        Toggle(L("settings_play_when_silent"), isOn: $playInSilent)
                            .tint(.brandGreen).font(.inter(15, .medium))
                        Text(L("settings_play_when_silent_hint"))
                            .font(.inter(12)).foregroundColor(.appOnSurfaceVariant)
                    }
                }
            }
            if master {
                ForEach(SettingsView.prayers, id: \.0) { key, nameKey in
                    PrayerNotifCard(title: L(nameKey), key: key, onChange: rearm)
                }
                Button {
                    SoundPlayer.shared.play(sound.file, ext: sound.ext)
                } label: {
                    Text(L("settings_test_adhan")).font(.inter(15, .semibold)).foregroundColor(.white)
                        .frame(maxWidth: .infinity).padding(.vertical, 12)
                        .background(Color.brandGreen).clipShape(RoundedRectangle(cornerRadius: 12))
                }
            }
        }
    }

    // 6.3 Auto-Stummschaltung
    private var autoMuteSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            SettingHeader(L("settings_autosilence"))
            SettingCard {
                Toggle(L("settings_autosilence"), isOn: $autoMute)
                    .tint(.brandGreen).font(.inter(15, .medium))
                if autoMute {
                    Divider()
                    Text(L("settings_silence_before")).font(.inter(13)).foregroundColor(.appOnSurfaceVariant)
                    ChipRow(options: SettingsView.muteMins, intSelection: $muteBefore)
                    Text(L("settings_silence_after")).font(.inter(13)).foregroundColor(.appOnSurfaceVariant)
                    ChipRow(options: SettingsView.muteMins, intSelection: $muteAfter)
                }
            }
        }
    }

    // 6.4 Mitteilungen
    private var announcementsSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            SettingHeader(L("settings_news_header"))
            SettingCard {
                Toggle(L("settings_news_notifications"), isOn: $msgNotif)
                    .tint(.brandGreen).font(.inter(15, .medium))
                Divider()
                Toggle(L("settings_weekly_reminder"), isOn: $weekly)
                    .tint(.brandGreen).font(.inter(15, .medium))
            }
        }
    }

    // 6.5 Berechtigungen (only while notifications aren't granted)
    private var permissionsSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            SettingHeader(L("settings_permissions_header"))
            Button(action: requestNotif) {
                Text(L("settings_perm_notifications")).font(.inter(15, .semibold)).foregroundColor(.white)
                    .frame(maxWidth: .infinity).padding(.vertical, 12)
                    .background(Color.brandGreen).clipShape(RoundedRectangle(cornerRadius: 12))
            }
        }
    }

    // 6.6 Sprache
    private var languageSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            SettingHeader(L("language_picker_title"))
            SettingCard {
                Button { showLangPicker = true } label: {
                    HStack(spacing: 8) {
                        FlagThumb(pngName: langForTag(Localization.shared.lang).flagPng)
                        Text(L("action_change_language")).font(.inter(15, .medium)).foregroundColor(.appPrimary)
                        Spacer()
                        Image(systemName: "chevron.right").font(.system(size: 13)).foregroundColor(.appOnSurfaceVariant)
                    }
                }
            }
        }
    }

    private func refreshAuth() {
        UNUserNotificationCenter.current().getNotificationSettings { s in
            // Only a real denial should surface the "allow notifications" button; .notDetermined is
            // handled by ensureAuthorization() asking the system prompt.
            let granted = s.authorizationStatus != .denied
            DispatchQueue.main.async { notifGranted = granted }
        }
    }

    private func requestNotif() {
        // Reaching this button means the user denied notifications, and iOS won't prompt again —
        // send them to the app's page in the system settings.
        if let u = URL(string: UIApplication.openSettingsURLString) { UIApplication.shared.open(u) }
    }

    // prayer key (for @AppStorage) + localization key for the name
    static let prayers: [(String, String)] = [
        ("fajr", "prayer_fajr"), ("dhuhr", "prayer_dhuhr"), ("asr", "prayer_asr"),
        ("maghrib", "prayer_maghrib"), ("isha", "prayer_isha"),
    ]
    static let muteMins: [(String, String)] = [("5", "5"), ("10", "10"), ("15", "15"), ("20", "20"), ("30", "30")]
}

private func minutesLabel(_ v: Int) -> String { String(format: L("settings_minutes"), v) }

// One prayer's notification card: a switch (default on) and — when on — a pre-warning pill (0/5/10/15/30).
private struct PrayerNotifCard: View {
    let title: String
    let onChange: () -> Void
    @AppStorage private var enabled: Bool
    @AppStorage private var warn: Int

    init(title: String, key: String, onChange: @escaping () -> Void) {
        self.title = title
        self.onChange = onChange
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
                    Text(L("settings_prewarn")).font(.inter(15))
                    Spacer()
                    Menu {
                        ForEach(warnValues, id: \.self) { v in
                            Button(minutesLabel(v)) { warn = v }
                        }
                    } label: { PillLabel(minutesLabel(warn)) }
                }
            }
        }
        .onChange(of: enabled) { _ in onChange() }
        .onChange(of: warn) { _ in onChange() }
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
            SettingHeader(L("settings_about_header"))
            SettingCard {
                Text("IGBD-Gemeinde Sandžak-Kassel")
                    .font(.inter(16, .bold)).foregroundColor(.appPrimary)
                Divider()
                row("mappin.and.ellipse", "Schwanenweg 13\n34123 Kassel") { open(maps) }
                row("envelope.fill", "vorstand@igbdsandzakkassel.de") { open("mailto:vorstand@igbdsandzakkassel.de") }
                row("heart.fill", L("action_donate")) { open(donate) }
                row("person.fill", "\(L("about_imam")): Alen Golac\n0176 3037 2402") { open("tel:017630372402") }
                row("phone.fill", "\(L("about_dev_promo"))\n0176 6188 7123") { open("tel:017661887123") }
                row("envelope", "\(L("about_dev_email"))\nmuhamedgolac311@gmail.com") { open("mailto:muhamedgolac311@gmail.com") }
                Divider()
                Text("v\(version)")
                    .font(.inter(12)).foregroundColor(.appOnSurfaceVariant)
                    .onTapGesture {
                        versionTaps += 1
                        if versionTaps >= 7 { showAdmin = true; versionTaps = 0 }
                    }
            }
        }
        .sheet(isPresented: $showAdmin) { AdminLoginSheet() }
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
