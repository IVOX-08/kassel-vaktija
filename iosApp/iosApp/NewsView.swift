import SwiftUI

// Community announcements. On Android these come from Firebase; the iOS wiring is a later step,
// so this is a styled placeholder for now.
struct NewsView: View {
    var body: some View {
        NavigationStack {
            VStack(spacing: 16) {
                Image(systemName: "bell.badge.fill")
                    .font(.system(size: 52))
                    .foregroundColor(.brandGreen)
                Text("Gemeinde-Nachrichten")
                    .font(.title2).bold()
                Text("Hier erscheinen die Mitteilungen der Gemeinde.\nDie Verbindung zu Firebase folgt als nächster Schritt.")
                    .multilineTextAlignment(.center)
                    .foregroundColor(.secondary)
                    .padding(.horizontal, 32)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .background(Color.pageBackground.ignoresSafeArea())
            .navigationTitle("Nachrichten")
        }
    }
}
