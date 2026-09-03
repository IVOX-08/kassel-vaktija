import SwiftUI
import Shared

// "Gebetszeiten" (Startseite) — built to match the Android DashboardScreen (spec section 2):
// address / logo / donate header, Gregorian + Hijri date, a green hero with a live HH:MM:SS
// countdown, and prayer cards with Adhan + a divider + Iqamah. Inter font + exact brand colors.
struct ContentView: View {
    @StateObject private var store = PrayerStore()
    // Iqamah/Jumua/Eid come from the board's Firestore document; observed so an edit lands live.
    @ObservedObject private var community = CommunityRuleStore.shared
    /// Ausdrücklich beobachtet. Adresse, Wappen und Name lasen den Katalog bisher nur direkt —
    /// dass die Kopfzeile nach einem Wechsel trotzdem umsprang, lag allein am Sekundentakt für
    /// den Countdown, der die Ansicht ohnehin neu zeichnet. Ein Zufall, kein Zusammenhang.
    @ObservedObject private var catalog = CommunityCatalog.shared

    @State private var now = Date()
    @Environment(\.colorScheme) private var scheme
    private let ticker = Timer.publish(every: 1, on: .main, in: .common).autoconnect()


    var body: some View {
        ZStack {
            Color.appBackground.ignoresSafeArea()
            ScrollView {
                VStack(spacing: 14) {
                    header
                    // Die App hatte den Unterschied die ganze Zeit gewusst und nie gezeigt:
                    // `official` wurde gepflegt und von keiner Ansicht gelesen. Wenn vaktija.eu
                    // nicht erreichbar ist, stehen gerechnete Zeiten auf dem Schirm — und die
                    // koennen in unseren Breiten um Minuten danebenliegen. Wer das nicht weiss,
                    // haelt sie fuer die Zeiten seiner Moschee.
                    if !store.official {
                        Text(L("cache_stale_warning"))
                            .font(.inter(12)).foregroundColor(.appOnSurfaceVariant)
                            .multilineTextAlignment(.center)
                            .frame(maxWidth: .infinity)
                            .padding(.horizontal, 16)
                    }
                    countdownCard.padding(.horizontal, 12)
                    // The announced Eid prayer is the community's most-asked question, so it leads.
                    if let bajram = activeBajram { bajramCard(bajram) }
                    // Das Freitagsgebet steht AM FREITAG oben und an jedem anderen Tag unten.
                    //
                    // An sechs von sieben Tagen ist es eine Information für später und darf den
                    // Zeiten des heutigen Tages nicht den Platz wegnehmen. Am Freitag ist es das
                    // Einzige, weswegen die meisten die App an diesem Tag öffnen.
                    if isFriday { jumuaCard }
                    VStack(spacing: 12) { ForEach(rows) { prayerCard($0) } }
                    if !isFriday { jumuaCard }
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 10)
            }
        }
        .onReceive(ticker) { now = $0 }
        .task { await store.refresh() }
        .onAppear { community.start(); catalog.start() }
    }

    // MARK: Header

    private var header: some View {
        VStack(spacing: 6) {
            HStack(alignment: .top, spacing: 6) {
                linkBlock(url: mapsURLForSelection) {
                    VStack(spacing: 3) {
                        Image(systemName: "mappin.and.ellipse").font(.system(size: 22)).foregroundColor(.appPrimary)
                        Text(headerAddress).font(.inter(11, .medium)).foregroundColor(.appPrimary).multilineTextAlignment(.center)
                    }.frame(maxWidth: .infinity)
                }
                // Kassels Wappen ist genau 96 hoch; beim Verbandszeichen kommt der Gemeindename
                // darunter, deshalb nur eine Mindesthöhe statt einer festen.
                communityEmblem.frame(minHeight: 96)
                linkBlock(url: donateURLForSelection) {
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

    /// Adresse der GEWÄHLTEN Gemeinde. Ohne Adresse im Verzeichnis bleibt der Ortsname stehen —
    /// besser als eine fremde Adresse unter dem eigenen Gemeindenamen.
    private var headerAddress: String {
        if let address = catalog.selected?.address, !address.isEmpty {
            // "Schwanenweg 13, 34123 Kassel" -> zwei Zeilen, wie bisher gesetzt.
            return address.replacingOccurrences(of: ", ", with: "\n")
        }
        return catalog.selectedLocation?.name ?? ""
    }

    /// Der Spendenlink der GEWÄHLTEN Gemeinde.
    ///
    /// Hier stand Kassels PayPal-Konto fest im Code — bei allen einundachtzig Gemeinden. Die
    /// Adresse daneben folgte längst der Auswahl, das Herz nicht: Wer in Nürnberg spendete,
    /// spendete nach Kassel.
    ///
    /// Ohne hinterlegten Link bleibt das Herz stehen, führt aber nirgendwohin (`linkBlock` macht
    /// dann keinen Link daraus). Ein Knopf, der zur falschen Kasse führt, wäre schlimmer als
    /// einer, der wartet, bis die Gemeinde ihren Link schickt.
    private var donateURLForSelection: URL? {
        guard let raw = catalog.selected?.donationUrl,
              !raw.trimmingCharacters(in: .whitespaces).isEmpty else { return nil }
        return URL(string: raw)
    }

    /// Karten-Link auf die gewählte Gemeinde statt fest auf Kassel.
    private var mapsURLForSelection: URL? {
        let query = catalog.selected?.address ?? catalog.selectedLocation?.name ?? ""
        guard !query.isEmpty,
              let encoded = query.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed)
        else { return nil }
        return URL(string: "https://www.google.com/maps/search/?api=1&query=\(encoded)")
    }

    /// Das Zeichen der gewählten Gemeinde.
    ///
    /// Kassel behält sein eigenes Wappen. Jede andere Gemeinde trägt das Verbandszeichen der IGBD
    /// mit ihrem NAMEN darunter — sie gehören alle dazu, und keine hat ein eigenes Logo
    /// hinterlegt. Ein Mond-Symbol sagte nichts darüber, wessen Zeiten auf dem Schirm stehen;
    /// der Name sagt es.
    ///
    /// Kassels Wappen wird NICHT verliehen: ein fremdes Wappen über den eigenen Gebetszeiten
    /// wäre schlimmer als gar keins.
    @ViewBuilder private var communityEmblem: some View {
        if isHomeCommunity {
            Image(uiImage: logoImage).resizable().scaledToFit()
                .blendMode(scheme == .dark ? .normal : .multiply) // white emblem box blends into #F4F4F4
        } else if let url = communityLogoURL {
            AsyncImage(url: url) { phase in
                switch phase {
                case .success(let image): image.resizable().scaledToFit()
                // Beim Laden und beim Fehlschlag dasselbe Zeichen — ein leerer Platz liesse die
                // Kopfzeile bei jedem Start springen.
                default: federationEmblem
                }
            }
        } else {
            federationEmblem
        }
    }

    /// Verbandszeichen plus Gemeindename.
    ///
    /// Das Zeichen bekommt dieselbe Höhe wie Kassels Wappen. Es in eine feste Box mit dem Namen
    /// zu quetschen, machte es sichtbar kleiner als das der Heimatgemeinde — das liest sich wie
    /// zweite Klasse.
    private var federationEmblem: some View {
        VStack(spacing: 3) {
            Image(uiImage: UIImage(named: scheme == .dark ? "logo_igbd_dark" : "logo_igbd") ?? UIImage())
                .resizable().scaledToFit()
                .frame(height: 96)
            Text(catalog.selected?.name ?? "")
                .font(.inter(11, .semibold)).foregroundColor(.brandGreen)
                .multilineTextAlignment(.center)
                // Zwei Zeilen: „Islamski kulturni centar Bošnjaka u Berlinu" passt nicht auf eine,
                // und einen Gemeindenamen auf drei Punkte zu kürzen ist keine gute Begrüßung.
                .lineLimit(2)
                .fixedSize(horizontal: false, vertical: true)
        }
    }

    /// Kassel ist die Heimatgemeinde dieser App — nur sie hat ihr Wappen im Paket.
    private var isHomeCommunity: Bool {
        CommunitySelection.communityId == CommunitySelection.fallbackCommunityId
    }

    private var communityLogoURL: URL? {
        guard let raw = catalog.selected?.logoUrl, !raw.isEmpty else { return nil }
        return URL(string: raw)
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
            Text(germanName(nextInfo.name)).font(.inter(20, .semibold)).foregroundColor(.white)
            Text(nextInfo.time).font(.inter(24, .bold)).monospacedDigit().foregroundColor(.brandGoldLight)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 18)
        .background(Color.brandGreen)
        .clipShape(RoundedRectangle(cornerRadius: Radius.hero, style: .continuous))
        .shadow(color: .black.opacity(0.18), radius: 6, y: 3)
    }

    // MARK: Friday / Eid (mirrors the Android dashboard)

    private var isFriday: Bool { Calendar.current.component(.weekday, from: now) == 6 }

    /// On Friday the Dhuhr row becomes Jumua: the community's Jumua time, and no Iqamah line.
    private var rows: [PrayerRow] {
        let base = store.rows
        guard isFriday else { return base }
        return base.map { row in
            row.name == "Dhuhr"
                ? PrayerRow(name: "Jumua", adhan: community.rule.jumua, iqamah: nil)
                : row
        }
    }

    /// The announced Eid prayer, hidden again once its day has passed.
    private var activeBajram: (date: String, time: String)? {
        guard let b = community.rule.bajram else { return nil }
        let f = DateFormatter()
        f.calendar = Calendar(identifier: .gregorian)
        f.locale = Locale(identifier: "en_US_POSIX")
        f.dateFormat = "yyyy-MM-dd"
        guard let day = f.date(from: b.date) else { return nil }
        return Calendar.current.isDateInToday(day) || day > now ? b : nil
    }

    private func bajramCard(_ b: (date: String, time: String)) -> some View {
        HStack {
            VStack(alignment: .leading, spacing: 2) {
                Text("🌙 " + L("bajram_prayer")).font(.inter(17, .bold)).foregroundColor(.brandGoldLight)
                Text(longDate(b.date)).font(.inter(13)).foregroundColor(.appOnSurfaceVariant)
            }
            Spacer()
            Text(b.time).font(.inter(30, .bold)).foregroundColor(.appPrimary).monospacedDigit()
        }
        .padding(16)
        .background(Color.appSurface)
        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
        .overlay(RoundedRectangle(cornerRadius: 14, style: .continuous).stroke(Color.brandGoldLight, lineWidth: 2))
    }

    private var jumuaCard: some View {
        HStack {
            Text(L("prayer_jumua")).font(.inter(17, .bold)).foregroundColor(.brandGoldLight)
            Spacer()
            Text(community.rule.jumua).font(.inter(30, .bold)).foregroundColor(.appPrimary).monospacedDigit()
        }
        .padding(16)
        .background(Color.appSurface)
        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
        .overlay(RoundedRectangle(cornerRadius: 14, style: .continuous).stroke(Color.appPrimary, lineWidth: 2))
    }

    private func longDate(_ iso: String) -> String {
        let parser = DateFormatter()
        parser.calendar = Calendar(identifier: .gregorian)
        parser.locale = Locale(identifier: "en_US_POSIX")
        parser.dateFormat = "yyyy-MM-dd"
        guard let d = parser.date(from: iso) else { return iso }
        let out = DateFormatter()
        out.locale = Locale(identifier: Localization.shared.lang)
        out.dateFormat = "EEEE, d. MMMM"
        return out.string(from: d)
    }

    // MARK: Prayer card (Adhan + divider + Iqamah)

    private func prayerCard(_ row: PrayerRow) -> some View {
        let active = row.name == nextInfo.name
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
        case "Jumua": return L("prayer_jumua")
        default: return n
        }
    }

    // MARK: Derived strings

    /// On Friday the midday prayer IS Jumua, at the community's time — so the countdown must use it
    /// rather than the calculated Dhuhr, and name it accordingly.
    private var nextInfo: (name: String, time: String, inSeconds: Int) {
        var times = store.today
        if isFriday, let jumua = minutes(community.rule.jumua) { times.dhuhr = jumua }
        let next = PrayerModel.next(times, now: now)
        return isFriday && next.name == "Dhuhr" ? ("Jumua", next.time, next.inSeconds) : next
    }

    private func minutes(_ hhmm: String) -> Int? {
        let p = hhmm.split(separator: ":")
        guard p.count == 2, let h = Int(p[0]), let m = Int(p[1]) else { return nil }
        return h * 60 + m
    }
    private var countdown: String {
        let r = max(0, nextInfo.inSeconds)
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
