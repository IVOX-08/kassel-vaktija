import SwiftUI

// Zakat-Rechner, eins zu eins wie auf Android.
//
// Bewusst ein Rechner und nichts weiter: Er addiert, was der Nutzer einträgt, zieht Schulden ab,
// vergleicht mit dem Nisab, den ER angibt, und nimmt 2,5 %. Der Goldpreis wird NICHT aus dem Netz
// geholt — ein falscher Nisab erzeugt still eine falsche Pflicht, und die richtige Quelle dafür ist
// der Imam der Gemeinde, nicht ein Dienst, den diese App zufällig erreicht.
//
// Alles wird von Hand eingetragen, aus demselben Grund: ein Rechner, der das Vermögen von jemandem
// errät, wäre schlimmer als keiner.
struct ZakatView: View {
    @State private var cash = ""
    @State private var bank = ""
    @State private var gold = ""
    @State private var business = ""
    @State private var receivable = ""
    @State private var debts = ""
    @State private var nisab = ""

    private var assets: Double {
        [cash, bank, gold, business, receivable].map(Self.amount).reduce(0, +)
    }
    private var net: Double { assets - Self.amount(debts) }
    private var nisabValue: Double { Self.amount(nisab) }
    /// Unter dem Nisab fällt gar nichts an. Trotzdem eine kleine Zahl zu zeigen lüde dazu ein,
    /// etwas zu zahlen, das nicht geschuldet ist.
    private var due: Double? {
        guard nisabValue > 0, net >= nisabValue else { return nil }
        return net * 0.025
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 12) {
                Text(L("zakat_intro"))
                    .font(.inter(14)).foregroundColor(.appOnSurfaceVariant)
                    .fixedSize(horizontal: false, vertical: true)

                card(L("zakat_assets")) {
                    field(L("zakat_cash"), $cash)
                    field(L("zakat_bank"), $bank)
                    field(L("zakat_gold"), $gold)
                    field(L("zakat_business"), $business)
                    field(L("zakat_receivable"), $receivable)
                }

                card(L("zakat_deductions")) {
                    field(L("zakat_debts"), $debts)
                }

                card(L("zakat_nisab")) {
                    field(L("zakat_nisab_value"), $nisab)
                    Text(L("zakat_nisab_hint"))
                        .font(.inter(13)).foregroundColor(.appOnSurfaceVariant)
                        .fixedSize(horizontal: false, vertical: true)
                }

                resultCard

                // Der Hinweis steht AUSSERHALB der Karte, wie auf Android: Er gehört nicht zur
                // Rechnung, sondern sagt, was diese Rechnung nicht ist.
                Text(L("zakat_disclaimer"))
                    .font(.inter(13)).foregroundColor(.appOnSurfaceVariant)
                    .fixedSize(horizontal: false, vertical: true)
                    .padding(.horizontal, 4).padding(.top, 4)
            }
            .padding(16)
        }
        .background(Color.appBackground.ignoresSafeArea())
        .navigationTitle(L("library_zakat"))
        .navigationBarTitleDisplayMode(.inline)
    }

    private var resultCard: some View {
        VStack(alignment: .leading, spacing: 10) {
            line(L("zakat_total_assets"), assets)
            line(L("zakat_net"), net)
            Divider()
            // Die Zeile steht IMMER da, auch mit 0,00 €. Sie erst erscheinen zu lassen, wenn
            // etwas faellig ist, laesst den Rechner unfertig aussehen — und „0" ist hier eine
            // Antwort, keine fehlende Angabe.
            HStack(alignment: .firstTextBaseline) {
                Text(L("zakat_due")).font(.inter(17, .bold)).foregroundColor(.appOnSurface)
                Spacer()
                Text(Self.euro(due ?? 0)).font(.inter(24, .bold)).foregroundColor(.brandGreen)
            }
            if due != nil {
                Text(L("zakat_above_nisab")).font(.inter(13)).foregroundColor(.brandGreen)
                    .fixedSize(horizontal: false, vertical: true)
            } else if nisabValue <= 0 {
                Text(L("zakat_enter_nisab")).font(.inter(13)).foregroundColor(.appOnSurfaceVariant)
                    .fixedSize(horizontal: false, vertical: true)
            } else {
                Text(L("zakat_below_nisab")).font(.inter(13)).foregroundColor(.appOnSurfaceVariant)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(18)
        .background(Color.moreCard)
        .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
        .overlay(RoundedRectangle(cornerRadius: 18, style: .continuous)
            .stroke(Color.cardOutline, lineWidth: 1))
    }

    // MARK: Bausteine

    private func card<Content: View>(_ title: String,
                                     @ViewBuilder _ content: () -> Content) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(title).font(.inter(15, .bold)).foregroundColor(.brandGreen)
            content()
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(18)
        .background(Color.moreCard)
        .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
        .overlay(RoundedRectangle(cornerRadius: 18, style: .continuous)
            .stroke(Color.cardOutline, lineWidth: 1))
    }

    /// Ein Eingabefeld: umrandeter Kasten über die ganze Breite, wie auf Android.
    ///
    /// Solange nichts drinsteht, steht der Name IM Kasten. Sobald etwas drinsteht, rutscht er
    /// klein nach oben — sonst wüsste man bei sechs Kästen untereinander nicht mehr, welche Zahl
    /// wozu gehört.
    ///
    /// Gefiltert wird beim Tippen, nicht erst beim Rechnen — sonst könnte im Feld etwas stehen,
    /// das die Rechnung still als null liest. Komma ist erlaubt, so schreibt man Beträge hier.
    private func field(_ label: String, _ value: Binding<String>) -> some View {
        let filtered = Binding<String>(
            get: { value.wrappedValue },
            set: { value.wrappedValue = $0.filter { $0.isNumber || $0 == "." || $0 == "," } }
        )
        let empty = value.wrappedValue.isEmpty
        return VStack(alignment: .leading, spacing: 2) {
            if !empty {
                Text(label).font(.inter(12)).foregroundColor(.brandGreen)
            }
            HStack(spacing: 6) {
                TextField(label, text: filtered)
                    .keyboardType(.decimalPad)
                    .font(.inter(17))
                    .foregroundColor(.appOnSurface)
                if !empty {
                    Text("€").font(.inter(17)).foregroundColor(.appOnSurfaceVariant)
                }
            }
        }
        .padding(.horizontal, 16)
        .frame(height: 62)
        .frame(maxWidth: .infinity, alignment: .leading)
        .overlay(
            RoundedRectangle(cornerRadius: 6, style: .continuous)
                .stroke(Color.appOnSurfaceVariant.opacity(0.7), lineWidth: 1)
        )
    }

    private func line(_ label: String, _ value: Double) -> some View {
        HStack {
            Text(label).font(.inter(16)).foregroundColor(.appOnSurface)
            Spacer()
            Text(Self.euro(value)).font(.inter(17, .semibold)).foregroundColor(.appOnSurface)
        }
    }

    // MARK: Zahlen

    /// Nimmt „1.234,50" wie „1234.50" an — die Leute tippen, was ihre Tastatur anbietet.
    ///
    /// Regel: Das ZULETZT stehende Trennzeichen ist das Dezimaltrennzeichen, alle davor sind
    /// Tausendertrennzeichen. Damit stimmen „1.234,50", „1,234.50", „1234.50" und „1234,50".
    ///
    /// Bewusst NICHT wie die Android-Vorlage: die entfernt erst alle Punkte und liest „1234.50"
    /// deshalb als 123450 — hundertfach zu viel, und damit eine hundertfach zu hohe Zakat, ohne
    /// dass irgendetwas kaputt aussieht. Gemeldet, damit es dort behoben wird.
    ///
    /// Bleibt eine Unschärfe: Ein alleinstehendes „1.234" wird als 1,234 gelesen, nicht als 1234.
    /// Auf dem Ziffernblock tippt kaum jemand Tausenderpunkte, und hier lieber zu klein als
    /// tausendfach zu groß.
    private static func amount(_ raw: String) -> Double {
        let t = raw.trimmingCharacters(in: .whitespaces)
        guard let lastSeparator = t.lastIndex(where: { $0 == "." || $0 == "," }) else {
            return Double(t) ?? 0
        }
        let whole = t[t.startIndex..<lastSeparator].filter(\.isNumber)
        let fraction = t[t.index(after: lastSeparator)...].filter(\.isNumber)
        return Double("\(whole).\(fraction)") ?? 0
    }

    private static func euro(_ v: Double) -> String {
        let f = NumberFormatter()
        f.numberStyle = .currency
        f.currencyCode = "EUR"
        // Fest deutsch wie Locale.GERMANY auf Android: die Beträge sind Euro in Deutschland,
        // unabhängig davon, in welcher Sprache jemand die App liest.
        f.locale = Locale(identifier: "de_DE")
        return f.string(from: NSNumber(value: v)) ?? "0"
    }
}
