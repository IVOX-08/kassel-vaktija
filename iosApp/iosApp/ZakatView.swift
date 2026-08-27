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
                        .font(.inter(12)).foregroundColor(.appOnSurfaceVariant)
                        .fixedSize(horizontal: false, vertical: true)
                }

                resultCard
            }
            .padding(16)
        }
        .background(Color.appBackground.ignoresSafeArea())
        .navigationTitle(L("library_zakat"))
        .navigationBarTitleDisplayMode(.inline)
    }

    private var resultCard: some View {
        VStack(alignment: .leading, spacing: 8) {
            line(L("zakat_total_assets"), assets)
            line(L("zakat_net"), net)
            Divider()
            if let due {
                HStack {
                    Text(L("zakat_due")).font(.inter(16, .bold)).foregroundColor(.appPrimary)
                    Spacer()
                    Text(Self.euro(due)).font(.inter(20, .bold)).foregroundColor(.brandGreen)
                }
                Text(L("zakat_above_nisab")).font(.inter(13)).foregroundColor(.brandGreen)
            } else if nisabValue <= 0 {
                Text(L("zakat_enter_nisab")).font(.inter(13)).foregroundColor(.appOnSurfaceVariant)
            } else {
                Text(L("zakat_below_nisab")).font(.inter(13)).foregroundColor(.appOnSurfaceVariant)
            }
            Text(L("zakat_disclaimer"))
                .font(.inter(12)).foregroundColor(.appOnSurfaceVariant)
                .fixedSize(horizontal: false, vertical: true)
                .padding(.top, 4)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(16)
        .background(Color.appSurface)
        .clipShape(RoundedRectangle(cornerRadius: Radius.smallCard, style: .continuous))
    }

    // MARK: Bausteine

    private func card<Content: View>(_ title: String,
                                     @ViewBuilder _ content: () -> Content) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(title).font(.inter(13, .semibold)).foregroundColor(.appSecondary)
            content()
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(16)
        .background(Color.appSurface)
        .clipShape(RoundedRectangle(cornerRadius: Radius.smallCard, style: .continuous))
    }

    private func field(_ label: String, _ value: Binding<String>) -> some View {
        HStack {
            Text(label).font(.inter(15)).foregroundColor(.appOnSurface)
            Spacer(minLength: 12)
            TextField("0", text: value)
                .keyboardType(.decimalPad)
                .multilineTextAlignment(.trailing)
                .font(.inter(15, .medium))
                .frame(maxWidth: 120)
        }
    }

    private func line(_ label: String, _ value: Double) -> some View {
        HStack {
            Text(label).font(.inter(14)).foregroundColor(.appOnSurfaceVariant)
            Spacer()
            Text(Self.euro(value)).font(.inter(15, .semibold)).foregroundColor(.appOnSurface)
        }
    }

    // MARK: Zahlen

    /// Nimmt Komma wie Punkt an — auf einer deutschen Tastatur tippt man „12,50", auf einer
    /// englischen „12.50", und beide meinen dasselbe.
    private static func amount(_ raw: String) -> Double {
        Double(raw.replacingOccurrences(of: ",", with: ".")
                  .trimmingCharacters(in: .whitespaces)) ?? 0
    }

    private static func euro(_ v: Double) -> String {
        let f = NumberFormatter()
        f.numberStyle = .currency
        f.currencyCode = "EUR"
        f.locale = Locale(identifier: Localization.shared.lang == "de" ? "de_DE" : "de_DE")
        return f.string(from: NSNumber(value: v)) ?? "0"
    }
}
