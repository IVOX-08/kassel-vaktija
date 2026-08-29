import SwiftUI
import UserNotifications

// First-launch onboarding (spec 8): Language → 4 intro slides → 3-step permission assistant.
// Only completing (or running through) the assistant sets `onboarding_done`. The phase is persisted
// so selecting a language (which rebuilds the whole tree for the new language + RTL) doesn't reset it.
struct OnboardingView: View {
    let onDone: () -> Void
    @AppStorage("ob_phase") private var phase = 0 // 0 = language, 1 = intro, 2 = permissions

    var body: some View {
        switch phase {
        case 0:
            LanguagePickerView(
                showClose: false,
                onSelect: { phase = 1; Localization.shared.set($0.tag) },
                onClose: {}
            )
        case 1:
            IntroSlides(onFinish: { phase = 2 })
        default:
            PermissionAssistant(onDone: { phase = 0; onDone() })
        }
    }
}

// MARK: - Step 2: intro slides

private struct IntroSlide {
    let emblem: Bool          // slide 1 shows the community emblem; the rest show an SF Symbol
    let icon: String
    let titleKey: String
    let textKey: String
}

private let introSlides: [IntroSlide] = [
    .init(emblem: true, icon: "", titleKey: "onb_welcome_title", textKey: "onb_welcome_body"),
    .init(emblem: false, icon: "bell.fill", titleKey: "onb_notif_title", textKey: "onb_notif_body"),
    .init(emblem: false, icon: "book.fill", titleKey: "onb_content_title", textKey: "onb_content_body"),
    .init(emblem: false, icon: "safari.fill", titleKey: "onb_more_title", textKey: "onb_more_body"),
]

private struct IntroSlides: View {
    let onFinish: () -> Void
    @Environment(\.colorScheme) private var scheme
    @State private var page = 0

    /// Das Verbandszeichen, nicht Kassels Wappen.
    ///
    /// Hier ist noch keine Gemeinde gewaehlt — die Begruessung gehoert dem Verband, nicht einer
    /// seiner einundachtzig Gemeinden. Im Dunkelmodus die offizielle Negativ-Fassung (weiss,
    /// Kapitel 2.2.1); das Zeichen wird NICHT umgefaerbt.
    private var logo: UIImage {
        let name = scheme == .dark ? "logo_igbd_dark" : "logo_igbd"
        return UIImage(named: name) ?? UIImage(named: "logo_igbd") ?? UIImage()
    }

    var body: some View {
        ZStack {
            Color.appBackground.ignoresSafeArea()
            VStack(spacing: 0) {
                HStack {
                    Spacer()
                    Button(L("onb_skip")) { onFinish() }
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
                    Text(page == introSlides.count - 1 ? L("onb_start") : L("onb_next"))
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
            Text(L(s.titleKey)).font(.inter(24, .bold)).foregroundColor(.appPrimary)
            Text(L(s.textKey)).font(.inter(17)).foregroundColor(.appOnSurfaceVariant)
                .multilineTextAlignment(.center).padding(.horizontal, 32)
            Spacer()
        }
    }
}

// MARK: - Step 3: permission assistant (3 steps)

private struct PermissionStep {
    let icon: String
    let titleKey: String
    let textKey: String
    let actionKey: String
}

private let permissionSteps: [PermissionStep] = [
    .init(icon: "bell.badge.fill", titleKey: "onb_perm_notif_title", textKey: "onb_perm_notif_body", actionKey: "settings_perm_notifications"),
    .init(icon: "moon.fill", titleKey: "onb_perm_dnd_title", textKey: "onb_perm_dnd_body", actionKey: "settings_perm_dnd"),
    .init(icon: "rectangle.stack.badge.plus", titleKey: "onb_perm_widget_title", textKey: "onb_perm_widget_body", actionKey: "onb_perm_widget_action"),
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
                Text(L(s.titleKey)).font(.inter(24, .bold)).foregroundColor(.appPrimary).padding(.top, 24)
                Text(L(s.textKey)).font(.inter(17)).foregroundColor(.appOnSurface)
                    .multilineTextAlignment(.center).padding(.horizontal, 32).padding(.top, 12)

                if step == 0 && notifGranted {
                    Label(L("settings_perm_granted"), systemImage: "checkmark.circle.fill")
                        .font(.inter(16, .semibold)).foregroundColor(.brandGreen).padding(.top, 20)
                } else {
                    Button(action: primaryAction) {
                        Text(L(s.actionKey)).font(.inter(15, .semibold)).foregroundColor(.white)
                            .frame(maxWidth: .infinity).padding(.vertical, 12)
                            .background(Color.brandGreen).clipShape(RoundedRectangle(cornerRadius: 12))
                    }
                    .padding(.horizontal, 24).padding(.top, 20)
                }
                Spacer()

                HStack {
                    Button(L("onb_skip")) { onDone() }
                        .font(.inter(15, .medium)).foregroundColor(.appOnSurfaceVariant)
                    Spacer()
                    Button {
                        if step == permissionSteps.count - 1 { onDone() } else { step += 1 }
                    } label: {
                        Text(step == permissionSteps.count - 1 ? L("onb_finish") : L("onb_next"))
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
