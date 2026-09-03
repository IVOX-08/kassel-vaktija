import SwiftUI
import UserNotifications

// Einstellungen (spec 6): a vertically scrolling column, padding 16, spacing 12. Section headings in
// primary/bold, each option group a card. All labels come from the selected app language.
struct SettingsView: View {
    @AppStorage("appColorScheme") private var theme = "system"
    // 6.2 prayer notifications
    @AppStorage("notif_sound") private var soundRaw = NotifSound.adhan.rawValue
    @AppStorage("notif_silent") private var playInSilent = false
    /// Die Frage des Trackers nach jedem Gebet. Standardmaessig an, aber abschaltbar: Wer den
    /// Tracker nicht nutzt, wuerde sonst fuenfmal taeglich gefragt — und schaltete am Ende alle
    /// Meldungen ab, auch den Adhan.
    @AppStorage(NotificationScheduler.trackerAskKey) private var trackerAsk = true
    // 6.3 auto-mute
    // 6.4 announcements
    @AppStorage("msg_notif") private var msgNotif = true
    @AppStorage("weekly_reminder") private var weekly = true

    @ObservedObject private var admin = AdminStore.shared
    @ObservedObject private var catalog = CommunityCatalog.shared
    @State private var showCommunityPicker = false
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
                    // Reihenfolge wie auf Android — die Apps sollen sich gleich anfuehlen.
                    // Only visible once the board's account is signed in (via the 7-tap gate below).
                    // Nur der Admin DIESER Gemeinde. Der Hauptadministrator sieht den Editor
                    // bewusst nicht — die Iqamah ist die Entscheidung der Gemeinde, nicht seine.
                    if admin.canEditTimes { AdminSection() }
                    // Der Hauptadministrator sieht statt des Zeiten-Editors die Verwaltung.
                    if admin.canBroadcast { HeadAdminSection() }
                    designSection
                    prayerNotifSection
                    announcementsSection
                    if !notifGranted { permissionsSection }
                    // Gemeinde und Sprache stehen zusammen unten: beides wird einmal im Leben der
                    // Installation gewählt und danach nie wieder angefasst. Der oberste Platz
                    // gehört dem, was man oft braucht — und ein Fehlgriff bei der Gemeinde hiesse,
                    // dass das Telefon still die Gebetszeiten einer anderen Stadt anzeigt.
                    communitySection
                    languageSection
                    DeveloperCard()
                    AboutCard()
                }
                .padding(16)
            }
            .background(Color.appBackground.ignoresSafeArea())
            .navigationTitle(L("nav_settings"))
            .navigationBarTitleDisplayMode(.inline)
        }
        .tint(.brandGreen)
        .sheet(isPresented: $showCommunityPicker) { CommunityPickerView() }
        .onAppear { refreshAuth(); admin.start(); catalog.start() }
        .onChange(of: soundRaw) { _ in rearm() }
        .onChange(of: trackerAsk) { _ in rearm() }
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
    /// Die eigene Gemeinde. An ihr haengt alles — Gebetszeiten, Mitteilungen, Adresse und Wappen —,
    /// und genau deshalb steht sie unten: Wer sie versehentlich wechselt, sieht ab dann still die
    /// Zeiten einer fremden Stadt.
    private var communitySection: some View {
        VStack(alignment: .leading, spacing: 8) {
            SettingHeader(L("settings_community_header"))
            SettingCard {
                Button { showCommunityPicker = true } label: {
                    HStack(spacing: 12) {
                        Image(systemName: "mappin.and.ellipse")
                            .font(.system(size: 17)).foregroundColor(.brandGreen)
                            .frame(width: 34, height: 34)
                            .background(Color.brandGreen.opacity(0.12)).clipShape(Circle())
                        VStack(alignment: .leading, spacing: 1) {
                            Text(catalog.selectedLocation?.name ?? L("community_none_selected"))
                                .font(.inter(16, .semibold)).foregroundColor(.appPrimary)
                            Text(catalog.selected?.name ?? L("action_change_community"))
                                .font(.inter(12)).foregroundColor(.appOnSurfaceVariant)
                                .fixedSize(horizontal: false, vertical: true)
                                .multilineTextAlignment(.leading)
                        }
                        Spacer()
                        Image(systemName: "chevron.right")
                            .font(.system(size: 13)).foregroundColor(.appOnSurfaceVariant)
                    }
                }
            }
        }
    }

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
                // Kein Hauptschalter mehr: Er schaltete mit einem Tipp alles ab, und wer ihn
                // einmal gedrückt hatte, fand selten zurück. Die einzelnen Schalter bleiben.
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
                Divider()
                VStack(alignment: .leading, spacing: 4) {
                    Toggle(L("settings_tracker_ask"), isOn: $trackerAsk)
                        .tint(.brandGreen).font(.inter(15, .medium))
                    Text(L("settings_tracker_ask_hint"))
                        .font(.inter(12)).foregroundColor(.appOnSurfaceVariant)
                }
            }
            Group {
                ForEach(SettingsView.prayers, id: \.0) { key, nameKey in
                    PrayerNotifCard(title: L(nameKey), key: key,
                                    hint: key == "sunrise" ? L("settings_sunrise_hint") : nil,
                                    onChange: rearm)
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
                    HStack(spacing: 12) {
                        FlagCircle(pngName: langForTag(Localization.shared.lang).flagPng)
                        VStack(alignment: .leading, spacing: 1) {
                            // Der eigene Name der Sprache, nicht übersetzt — wer versehentlich auf
                            // Urdu gestellt hat, erkennt „Deutsch" wieder, „جرمن" nicht.
                            Text(langForTag(Localization.shared.lang).endonym)
                                .font(.inter(16, .semibold)).foregroundColor(.appPrimary)
                            Text(L("action_change_language"))
                                .font(.inter(12)).foregroundColor(.appOnSurfaceVariant)
                        }
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
        ("fajr", "prayer_fajr"),
        // Kein Gebet, sondern das Ende der Zeit für das Morgengebet — steht deshalb direkt
        // dahinter, wie auf Android.
        ("sunrise", "prayer_sunrise"),
        ("dhuhr", "prayer_dhuhr"), ("asr", "prayer_asr"),
        ("maghrib", "prayer_maghrib"), ("isha", "prayer_isha"),
    ]
}

private func minutesLabel(_ v: Int) -> String { String(format: L("settings_minutes"), v) }

// One prayer's notification card: a switch (default on) and — when on — a pre-warning pill (0/5/10/15/30).
private struct PrayerNotifCard: View {
    let title: String
    /// Erklärt die Zeile, wenn sie nicht selbsterklärend ist — beim Sonnenaufgang muss dastehen,
    /// dass dann die Zeit für das Morgengebet endet, sonst wirkt die Erinnerung willkürlich.
    let hint: String?
    let onChange: () -> Void
    @AppStorage private var enabled: Bool
    @AppStorage private var warn: Int

    /// Der Sonnenaufgang ist die eine Zeile, die anders vorbelegt ist und andere Vorwarnzeiten
    /// anbietet — deshalb steht die Ausnahme hier und nicht an fuenf Stellen im Code.
    private let warnValues: [Int]

    init(title: String, key: String, hint: String? = nil, onChange: @escaping () -> Void) {
        self.title = title
        self.hint = hint
        self.onChange = onChange
        // Der Sonnenaufgang ist standardmaessig AUS. Ein ungefragter Ruf bei Tagesanbruch weckt
        // Leute, die ihn nie wollten — und wer einmal so geweckt wurde, schaltet die
        // Benachrichtigungen ganz ab und verliert damit auch den Adhan.
        _enabled = AppStorage(wrappedValue: !PrayerNotifCard.defaultsOff.contains(key), "pn_\(key)")
        _warn = AppStorage(wrappedValue: 0, "pw_\(key)")
        // Beim Sonnenaufgang ist die Vorwarnung keine Erinnerung, sondern die Restzeit fuer das
        // Morgengebet: Eine Stunde vorher ist die sinnvolle Obergrenze, fuenf Minuten waeren zu
        // knapp, um noch aufzustehen und zu beten.
        self.warnValues = key == "sunrise" ? [0, 10, 20, 30, 40, 50, 60]
                                           : [0, 5, 10, 15, 20, 25, 30]
    }

    /// Zeilen, die ausgeschaltet beginnen. Muss mit dem Zeitplan uebereinstimmen —
    /// siehe `NotificationScheduler.enabledByDefault`.
    static let defaultsOff: Set<String> = ["sunrise"]

    var body: some View {
        SettingCard {
            // Name, Vorwarnung und Schalter in EINER Zeile.
            //
            // Vorher brauchte jedes Gebet zwei Zeilen und eine eigene Karte; sechs Gebete fuellten
            // damit zwei Bildschirme. Wer eine Vorwarnung setzen will, vergleicht sie mit den
            // anderen — und dafuer muessen sie zusammen sichtbar sein.
            HStack(spacing: 10) {
                Text(title).font(.inter(16, .medium)).foregroundColor(.appOnSurface)
                    .lineLimit(2).fixedSize(horizontal: false, vertical: true)
                Spacer(minLength: 4)
                if enabled {
                    Menu {
                        ForEach(warnValues, id: \.self) { v in
                            Button(minutesLabel(v)) { warn = v }
                        }
                    } label: { PillLabel(minutesLabel(warn)) }
                }
                Toggle("", isOn: $enabled).labelsHidden().tint(.brandGreen)
            }
            if let hint {
                Text(hint).font(.inter(13)).foregroundColor(.appOnSurfaceVariant)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
        .onChange(of: enabled) { _ in onChange() }
        .onChange(of: warn) { _ in onChange() }
    }
}

/// Kontakt zum Entwickler — bewusst eine EIGENE Karte über „Über uns".
///
/// Vorher standen die beiden Zeilen mitten zwischen Adresse, Spenden und Imam der Gemeinde. Wer
/// die Gemeinde erreichen wollte, landete bei der Telefonnummer des Entwicklers. Zwei Absender,
/// zwei Karten.
private struct DeveloperCard: View {
    @Environment(\.openURL) private var openURL

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            SettingHeader(L("settings_app_header"))
            SettingCard {
                row("phone.fill", "\(L("about_dev_promo"))\n0176 6188 7123") { open("tel:017661887123") }
                row("envelope", "\(L("about_dev_email"))\nmuhamedgolac311@gmail.com") { open("mailto:muhamedgolac311@gmail.com") }
            }
        }
    }

    private func open(_ s: String) { if let u = URL(string: s) { openURL(u) } }

    private func row(_ icon: String, _ text: String, _ action: @escaping () -> Void) -> some View {
        Button(action: action) {
            HStack(alignment: .top, spacing: 10) {
                Image(systemName: icon).font(.system(size: 15)).foregroundColor(.brandGreen).frame(width: 22)
                Text(text).font(.inter(15)).foregroundColor(.appOnSurface)
                    .fixedSize(horizontal: false, vertical: true)
                    .multilineTextAlignment(.leading)
                Spacer()
            }
        }
        .buttonStyle(.plain)
    }
}

// "About the community" card (spec 6.7): tappable rows + version line with the hidden 7-tap admin gate.
private struct AboutCard: View {
    @Environment(\.openURL) private var openURL
    @State private var versionTaps = 0
    @State private var showCommunityLogin = false
    @State private var showHeadLogin = false

    /// Alles in dieser Karte kommt aus dem Verzeichnis, nichts mehr aus dem Quelltext.
    ///
    /// Vorher standen Name, Adresse, E-Mail, Spendenlink und der Imam als feste Zeichenketten
    /// hier — Kassels Daten, gezeigt bei allen einundachtzig Gemeinden. Wer in Berlin seine
    /// Gemeinde gewaehlt hatte, rief bei einem Anruf auf den Imam Kassels Imam an, und eine
    /// Spende ging an Kassels Konto.
    ///
    /// Eine neue Gemeinde braucht deshalb keinen Codeeingriff mehr: Spendenlink, E-Mail und
    /// Nummer des Imams gehoeren in `communities/{id}` — siehe docs/multi-gemeinde/GEMEINDE-DATEN.md.
    @ObservedObject private var catalog = CommunityCatalog.shared

    private var community: CommunityInfo? { catalog.selected }

    private var version: String {
        (Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String) ?? "1.0"
    }

    /// Zeigt nur, was wirklich hinterlegt ist. Eine leere Zeile „E-Mail:" ohne Adresse ist
    /// schlechter als gar keine — sie sieht aus wie ein Fehler der App.
    private func filled(_ value: String?) -> String? {
        guard let v = value?.trimmingCharacters(in: .whitespacesAndNewlines), !v.isEmpty else { return nil }
        return v
    }

    /// Kartenlink auf die Adresse der Gemeinde.
    private var mapsURL: String? {
        guard let query = filled(community?.address) ?? filled(catalog.selectedLocation?.name),
              let encoded = query.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed)
        else { return nil }
        return "https://www.google.com/maps/search/?api=1&query=\(encoded)"
    }

    /// Aus „0176 3037 2402" wird „tel:017630372402" — Leerzeichen und Striche muss man
    /// wegnehmen, sonst oeffnet iOS den Waehler gar nicht.
    private func telURL(_ number: String) -> String {
        "tel:" + number.filter { $0.isNumber || $0 == "+" }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            SettingHeader(L("settings_about_header"))
            SettingCard {
                Text(community?.name ?? L("community_none_selected"))
                    .font(.inter(16, .bold)).foregroundColor(.appPrimary)
                    .fixedSize(horizontal: false, vertical: true)
                Divider()
                if let address = filled(community?.address) {
                    row("mappin.and.ellipse", address.replacingOccurrences(of: ", ", with: "\n")) {
                        if let u = mapsURL { open(u) }
                    }
                }
                if let email = filled(community?.email) {
                    row("envelope.fill", email) { open("mailto:\(email)") }
                }
                if let phone = filled(community?.phone) {
                    row("phone.fill", phone) { open(telURL(phone)) }
                }
                if let website = filled(community?.website) {
                    row("globe", website) { open(website) }
                }
                if let donate = filled(community?.donationUrl) {
                    row("heart.fill", L("action_donate")) { open(donate) }
                }
                if let imam = filled(community?.imamName) {
                    let number = filled(community?.imamPhone)
                    row("person.fill", "\(L("about_imam")): \(imam)" + (number.map { "\n" + $0 } ?? "")) {
                        if let number { open(telURL(number)) }
                    }
                }
            }

            // Sichtbar für alle Gemeinden: hier meldet sich der Vorstand einer Gemeinde an.
            // Ein Vorstand, der die App zum ersten Mal bekommt, kann nicht wissen, dass er
            // irgendwo siebenmal tippen müsste.
            Button { showCommunityLogin = true } label: {
                HStack(spacing: 8) {
                    Image(systemName: "lock.fill").font(.system(size: 14))
                    Text(L("admin_login_community"))
                        .font(.inter(15, .medium))
                }
                .foregroundColor(.appOnSurface)
                .frame(maxWidth: .infinity).padding(.vertical, 14)
                .background(Color.moreCard)
                .clipShape(RoundedRectangle(cornerRadius: Radius.smallCard, style: .continuous))
                .overlay(RoundedRectangle(cornerRadius: Radius.smallCard, style: .continuous)
                    .stroke(Color.cardOutline, lineWidth: 1))
            }
            .padding(.top, 4)

            // Der Zugang des Hauptadministrators bleibt versteckt: sieben Tipps auf die
            // Versionsnummer. Er steht allein und zentriert, wie auf Android.
            Text("v\(version)")
                .font(.inter(12)).foregroundColor(.appOnSurfaceVariant)
                .frame(maxWidth: .infinity, alignment: .center)
                .contentShape(Rectangle())
                .padding(.top, 8)
                .onTapGesture {
                    versionTaps += 1
                    if versionTaps >= 7 { showHeadLogin = true; versionTaps = 0 }
                }
        }
        .sheet(isPresented: $showCommunityLogin) { AdminLoginSheet(headAdmin: false) }
        .sheet(isPresented: $showHeadLogin) { AdminLoginSheet(headAdmin: true) }
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

// Nicht mehr privat: HeadAdminView nutzt dieselben Bausteine, damit die
// Verwaltung nicht anders aussieht als der Rest der Einstellungen.
struct SettingHeader: View {
    let text: String
    init(_ text: String) { self.text = text }
    var body: some View {
        Text(text).font(.inter(14, .bold)).foregroundColor(.appPrimary)
    }
}

struct SettingCard<Content: View>: View {
    @ViewBuilder let content: Content
    var body: some View {
        VStack(alignment: .leading, spacing: 10) { content }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(14)
            // Weiss mit Haarlinie. Ohne die Linie verschwimmt die Karte auf dem hellen Grau.
            .background(Color.moreCard)
            .clipShape(RoundedRectangle(cornerRadius: Radius.smallCard))
            .overlay(RoundedRectangle(cornerRadius: Radius.smallCard)
                .stroke(Color.cardOutline, lineWidth: 1))
    }
}

private struct PillLabel: View {
    let text: String
    init(_ text: String) { self.text = text }
    var body: some View {
        HStack(spacing: 4) {
            Text(text).font(.inter(14, .medium)).lineLimit(1)
            Image(systemName: "chevron.down").font(.system(size: 11, weight: .semibold))
        }
        .foregroundColor(.brandGreen)
        .padding(.horizontal, 12).padding(.vertical, 7)
        .overlay(Capsule().stroke(Color.appOnSurfaceVariant.opacity(0.5), lineWidth: 1))
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
