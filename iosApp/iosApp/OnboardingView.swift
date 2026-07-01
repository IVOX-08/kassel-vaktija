import SwiftUI
import UserNotifications

// First-launch onboarding (spec 8): Language → 4 intro slides → 3-step permission assistant.
// Only completing (or running through) the assistant sets `onboarding_done`.
struct OnboardingView: View {
    let onDone: () -> Void
    @AppStorage("app_lang") private var lang = "bs"
    @State private var phase = 0 // 0 = language, 1 = intro, 2 = permissions

    var body: some View {
        switch phase {
        case 0:
            LanguagePickerView(
                showClose: false,
                onSelect: { lang = $0.tag; phase = 1 },
                onClose: {}
            )
        case 1:
            IntroSlides(onFinish: { phase = 2 })
        default:
            PermissionAssistant(onDone: onDone)
        }
    }
}

// MARK: - Step 2: intro slides

private struct IntroSlide {
    let emblem: Bool          // slide 1 shows the community emblem; the rest show an SF Symbol
    let icon: String
    let title: String
    let text: String
}

private let introSlides: [IntroSlide] = [
    .init(emblem: true, icon: "", title: "Willkommen",
          text: "Die App der IGBD-Gemeinde Sandžak-Kassel — Gebetszeiten, Koran, Hadithe und mehr, in Ihrer Sprache."),
    .init(emblem: false, icon: "bell.fill", title: "Gebet & Adhan",
          text: "Genaue Gebetszeiten mit Benachrichtigungen. Wählen Sie pro Gebet den Adhan oder einen kurzen Ton und passen Sie die Benachrichtigungen an — in den Einstellungen."),
    .init(emblem: false, icon: "book.fill", title: "Koran, Hadith & Dhikr",
          text: "Der vollständige Koran auf Arabisch, die 40 Hadithe von an-Nawawi und eine Auswahl aus Riyad as-Salihin sowie Dhikr — auf Arabisch und in Ihrer Sprache."),
    .init(emblem: false, icon: "safari.fill", title: "Und mehr",
          text: "Qibla, Kalender, Gemeinde-Nachrichten und ein Startbildschirm-Widget. Möge Allah es annehmen."),
]

private struct IntroSlides: View {
    let onFinish: () -> Void
    @Environment(\.colorScheme) private var scheme
    @State private var page = 0

    private var logo: UIImage {
        let name = scheme == .dark ? "logo_community_dark" : "logo_community"
        return UIImage(named: name) ?? UIImage(named: "logo_community") ?? UIImage()
    }

    var body: some View {
        ZStack {
            Color.appBackground.ignoresSafeArea()
            VStack(spacing: 0) {
                HStack {
                    Spacer()
                    Button("Überspringen") { onFinish() }
                        .font(.inter(15, .medium)).foregroundColor(.appOnSurfaceVariant)
                        .padding(16)
                }
                TabView(selection: $page) {
                    ForEach(introSlides.indices, id: \.self) { i in
                        slide(introSlides[i]).tag(i)
                    }
                }
                .tabViewStyle(.page(indexDisplayMode: .never))

                HStack(spacing: 8) {
                    ForEach(introSlides.indices, id: \.self) { i in
                        Capsule()
                            .fill(i == page ? Color.brandGreen : Color.appOnSurfaceVariant.opacity(0.3))
                            .frame(width: i == page ? 22 : 8, height: 8)
                            .animation(.easeInOut, value: page)
                    }
                }
                .padding(.vertical, 16)

                Button {
                    if page == introSlides.count - 1 { onFinish() } else { withAnimation { page += 1 } }
                } label: {
                    Text(page == introSlides.count - 1 ? "Los geht's" : "Weiter")
                        .font(.inter(16, .semibold)).foregroundColor(.white)
                        .frame(maxWidth: .infinity).padding(.vertical, 14)
                        .background(Color.brandGreen).clipShape(RoundedRectangle(cornerRadius: 14))
                }
                .padding(.horizontal, 24).padding(.bottom, 24)
            }
        }
    }

    private func slide(_ s: IntroSlide) -> some View {
        VStack(spacing: 24) {
            Spacer()
            if s.emblem {
                Image(uiImage: logo).resizable().scaledToFit().frame(height: 150)
                    .blendMode(scheme == .dark ? .normal : .multiply)
            } else {
                Image(systemName: s.icon).font(.system(size: 96)).foregroundColor(.brandGreen)
            }
            Text(s.title).font(.inter(24, .bold)).foregroundColor(.appPrimary)
            Text(s.text).font(.inter(17)).foregroundColor(.appOnSurfaceVariant)
                .multilineTextAlignment(.center).padding(.horizontal, 32)
            Spacer()
        }
    }
}

// MARK: - Step 3: permission assistant (3 steps)

private struct PermissionStep {
    let icon: String
    let title: String
    let text: String
    let action: String
}

private let permissionSteps: [PermissionStep] = [
    .init(icon: "bell.badge.fill", title: "Benachrichtigungen",
          text: "Möchtest du an die Gebetszeiten (Adhan) und an Mitteilungen der Gemeinde erinnert werden? Dafür braucht die App die Erlaubnis für Benachrichtigungen.",
          action: "Benachrichtigungen erlauben"),
    .init(icon: "moon.fill", title: "„Nicht stören\"",
          text: "Damit der Adhan auch dann erklingt, wenn dein Handy auf lautlos steht, erlaube den Zugriff auf „Nicht stören\". Auf dem iPhone wird das über eine geplante stille Benachrichtigung gelöst.",
          action: "In den iOS-Einstellungen öffnen"),
    .init(icon: "rectangle.stack.badge.plus", title: "Widget",
          text: "Möchtest du die nächste Gebetszeit direkt auf dem Startbildschirm sehen? Halte den Startbildschirm gedrückt und füge „Kassel Vaktija\" über das +-Symbol hinzu.",
          action: "Verstanden"),
]

private struct PermissionAssistant: View {
    let onDone: () -> Void
    @Environment(\.openURL) private var openURL
    @State private var step = 0
    @State private var notifGranted = false

    var body: some View {
        ZStack {
            Color.appBackground.ignoresSafeArea()
            VStack(spacing: 0) {
                HStack(spacing: 8) {
                    ForEach(permissionSteps.indices, id: \.self) { i in
                        Circle().fill(i == step ? Color.brandGreen : Color.appOnSurfaceVariant.opacity(0.3))
                            .frame(width: 10, height: 10)
                    }
                }
                .padding(.top, 24)

                Spacer()
                let s = permissionSteps[step]
                Image(systemName: s.icon).font(.system(size: 88)).foregroundColor(.brandGreen)
                Text(s.title).font(.inter(24, .bold)).foregroundColor(.appPrimary).padding(.top, 24)
                Text(s.text).font(.inter(17)).foregroundColor(.appOnSurface)
                    .multilineTextAlignment(.center).padding(.horizontal, 32).padding(.top, 12)

                if step == 0 && notifGranted {
                    Label("Erlaubt", systemImage: "checkmark.circle.fill")
                        .font(.inter(16, .semibold)).foregroundColor(.brandGreen).padding(.top, 20)
                } else {
                    Button(action: primaryAction) {
                        Text(s.action).font(.inter(15, .semibold)).foregroundColor(.white)
                            .frame(maxWidth: .infinity).padding(.vertical, 12)
                            .background(Color.brandGreen).clipShape(RoundedRectangle(cornerRadius: 12))
                    }
                    .padding(.horizontal, 24).padding(.top, 20)
                }
                Spacer()

                HStack {
                    Button("Überspringen") { onDone() }
                        .font(.inter(15, .medium)).foregroundColor(.appOnSurfaceVariant)
                    Spacer()
                    Button {
                        if step == permissionSteps.count - 1 { onDone() } else { step += 1 }
                    } label: {
                        Text(step == permissionSteps.count - 1 ? "Fertig" : "Weiter")
                            .font(.inter(16, .semibold)).foregroundColor(.brandGreen)
                    }
                }
                .padding(24)
            }
        }
        .onAppear(perform: refreshAuth)
    }

    private func primaryAction() {
        switch step {
        case 0:
            UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound, .badge]) { _, _ in
                refreshAuth()
            }
        case 1:
            if let u = URL(string: UIApplication.openSettingsURLString) { openURL(u) }
        default:
            step = min(step + 1, permissionSteps.count - 1)
        }
    }

    private func refreshAuth() {
        UNUserNotificationCenter.current().getNotificationSettings { s in
            DispatchQueue.main.async { notifGranted = s.authorizationStatus == .authorized }
        }
    }
}
