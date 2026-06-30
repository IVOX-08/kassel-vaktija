import SwiftUI

// "Nachrichten" (spec section 4). Firebase wiring is a later step; for now the empty state per spec.
struct NewsView: View {
    var body: some View {
        NavigationStack {
            VStack {
                Spacer()
                Text("Noch keine Mitteilungen.")
                    .font(.inter(17))
                    .foregroundColor(.appOnSurfaceVariant)
                Spacer()
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .background(Color.appBackground.ignoresSafeArea())
            .navigationTitle("Nachrichten")
        }
    }
}
