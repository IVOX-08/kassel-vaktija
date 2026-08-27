import SwiftUI

// The admin area (spec section 9), reached by tapping the version number 7× in Settings → About.
// Mirrors the Android SettingsScreen admin section: sign in with the board's account, then edit the
// community rules every phone reads. Writes are enforced server-side by the Firestore rules.

struct AdminLoginSheet: View {
    @Environment(\.dismiss) private var dismiss
    @ObservedObject private var admin = AdminStore.shared

    @State private var email = ""
    @State private var password = ""
    @State private var errorText: String?
    @State private var busy = false

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    TextField(L("admin_email"), text: $email)
                        .textContentType(.emailAddress)
                        .keyboardType(.emailAddress)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                    SecureField(L("admin_password"), text: $password)
                        .textContentType(.password)
                } footer: {
                    if let errorText {
                        VStack(alignment: .leading, spacing: 4) {
                            Text(L("admin_login_failed")).foregroundColor(.qiblaRed)
                            // The underlying reason — needed to tell a wrong password apart from a
                            // network or configuration problem.
                            Text(errorText).font(.inter(11)).foregroundColor(.appOnSurfaceVariant)
                                .textSelection(.enabled)
                        }
                    }
                }
                Section {
                    Button {
                        Task { await signIn() }
                    } label: {
                        HStack {
                            Spacer()
                            if busy { ProgressView() } else { Text(L("admin_sign_in")).font(.inter(16, .semibold)) }
                            Spacer()
                        }
                    }
                    .disabled(busy || email.isEmpty || password.isEmpty)
                }
            }
            .navigationTitle(L("admin_login_title"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(L("action_cancel")) { dismiss() }
                }
            }
        }
        .tint(.brandGreen)
    }

    private func signIn() async {
        busy = true
        errorText = nil
        // Trailing spaces from autocomplete/paste are a common cause of "but I typed it right".
        let outcome = await admin.signIn(
            email: email.trimmingCharacters(in: .whitespacesAndNewlines),
            password: password
        )
        busy = false
        switch outcome {
        case .success:
            dismiss()
        case .wrongCommunity(let own, _):
            // Das Konto ist echt, verwaltet aber eine andere Gemeinde. Angemeldet bleibt es
            // trotzdem — der Weg zurück zur eigenen Gemeinde darf keine neue Anmeldung kosten.
            errorText = String(format: L("admin_wrong_community"), own)
        case .noRights:
            errorText = L("admin_no_rights")
        case .failed(let reason):
            errorText = reason
        }
    }

}

/// Editor for the community rules — the same fields the Android admin edits, same ±5 min steppers.
struct AdminSection: View {
    @ObservedObject private var admin = AdminStore.shared
    @ObservedObject private var community = CommunityRuleStore.shared

    @State private var draft = CommunityRule.fallback
    @State private var loaded = false
    @State private var saved = false
    @State private var busy = false

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            // Benennt die Gemeinde, wie auf Android. Wer mehrere verwaltet oder zwischen ihnen
            // wechselt, muss sehen, wessen Zeiten er gerade ändert.
            Text(String(format: L("admin_section_header_named"),
                        CommunityCatalog.shared.selected?.name ?? ""))
                .font(.inter(13, .semibold)).foregroundColor(.appSecondary)
                .padding(.leading, 4)
            VStack(alignment: .leading, spacing: 10) {
                stepper("\(L("prayer_fajr")) \(L("label_iqamah"))", value: draft.fajrIqamah) {
                    draft.fajrIqamah = shift(draft.fajrIqamah, by: $0)
                }
                stepper(L("prayer_jumua"), value: draft.jumua) {
                    draft.jumua = shift(draft.jumua, by: $0)
                }
                Divider()
                offset(L("prayer_dhuhr"), value: $draft.dhuhrOffsetMin)
                offset(L("prayer_asr"), value: $draft.asrOffsetMin)
                offset(L("prayer_maghrib"), value: $draft.maghribOffsetMin)
                offset(L("prayer_isha"), value: $draft.ishaOffsetMin)
                Divider()
                bajramEditor
                Divider()
                HStack(spacing: 10) {
                    Button {
                        Task { await save() }
                    } label: {
                        HStack {
                            Spacer()
                            if busy { ProgressView() } else { Text(saved ? L("admin_saved") : L("admin_save")) }
                            Spacer()
                        }
                        .font(.inter(15, .semibold))
                        .padding(.vertical, 10)
                        .background(Color.brandGreen)
                        .foregroundColor(.white)
                        .clipShape(RoundedRectangle(cornerRadius: 10))
                    }
                    .disabled(busy)
                    Button(L("admin_sign_out")) { admin.signOut() }
                        .font(.inter(15, .semibold)).foregroundColor(.brandGreen)
                        .padding(.vertical, 10).padding(.horizontal, 14)
                        .overlay(RoundedRectangle(cornerRadius: 10).stroke(Color.brandGreen, lineWidth: 1))
                }
            }
            .padding(14)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(Color.appSurface)
            .clipShape(RoundedRectangle(cornerRadius: Radius.smallCard))
        }
        // Start from what the community currently has; later live updates must not overwrite an
        // edit in progress, so this only seeds once.
        .onAppear {
            if !loaded { draft = community.rule; loaded = true }
        }
    }

    // MARK: Eid announcement

    @ViewBuilder private var bajramEditor: some View {
        if draft.bajram == nil {
            Button {
                let tomorrow = Calendar.current.date(byAdding: .day, value: 1, to: Date()) ?? Date()
                draft.bajramDate = Self.iso(tomorrow)
                draft.bajramTime = "07:00"
            } label: {
                Text("🌙 " + L("admin_bajram_set"))
                    .font(.inter(14, .semibold)).foregroundColor(.brandGreen)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 10)
                    .overlay(RoundedRectangle(cornerRadius: 10).stroke(Color.brandGreen, lineWidth: 1))
            }
        } else {
            stepperRow(L("bajram_prayer"), value: shortDate(draft.bajramDate ?? "")) { step in
                guard let d = draft.bajramDate, let date = Self.parse(d),
                      let moved = Calendar.current.date(byAdding: .day, value: step > 0 ? 1 : -1, to: date)
                else { return }
                draft.bajramDate = Self.iso(moved)
            }
            stepperRow("", value: draft.bajramTime ?? "") { step in
                draft.bajramTime = shift(draft.bajramTime ?? "07:00", by: step)
            }
            Button {
                draft.bajramDate = nil
                draft.bajramTime = nil
            } label: {
                Text(L("admin_bajram_remove"))
                    .font(.inter(14, .semibold)).foregroundColor(.qiblaRed)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 10)
                    .overlay(RoundedRectangle(cornerRadius: 10).stroke(Color.qiblaRed, lineWidth: 1))
            }
        }
    }

    // MARK: Rows

    private func stepper(_ label: String, value: String, onStep: @escaping (Int) -> Void) -> some View {
        stepperRow(label, value: value, onStep: onStep)
    }

    /// A prayer's Iqamah offset in minutes after the Adhan, clamped to 0…60 like Android.
    private func offset(_ label: String, value: Binding<Int>) -> some View {
        stepperRow(label, value: "+\(value.wrappedValue)") { step in
            value.wrappedValue = min(60, max(0, value.wrappedValue + (step > 0 ? 1 : -1)))
        }
    }

    private func stepperRow(_ label: String, value: String, onStep: @escaping (Int) -> Void) -> some View {
        HStack {
            Text(label).font(.inter(15)).foregroundColor(.appOnSurface)
            Spacer()
            Button { onStep(-1); saved = false } label: {
                Image(systemName: "minus.circle").font(.system(size: 22)).foregroundColor(.brandGreen)
            }
            Text(value).font(.inter(15, .bold)).foregroundColor(.appOnSurface)
                .frame(width: 64).monospacedDigit()
            Button { onStep(1); saved = false } label: {
                Image(systemName: "plus.circle").font(.system(size: 22)).foregroundColor(.brandGreen)
            }
        }
        .buttonStyle(.plain)
    }

    // MARK: Helpers

    private func save() async {
        busy = true
        var toSave = draft
        toSave.updatedAt = Int64(Date().timeIntervalSince1970 * 1000)
        saved = await admin.saveRule(toSave)
        busy = false
    }

    /// Moves an "HH:mm" value by ±5 minutes, wrapping within the day.
    private func shift(_ hhmm: String, by step: Int) -> String {
        let parts = hhmm.split(separator: ":")
        guard parts.count == 2, let h = Int(parts[0]), let m = Int(parts[1]) else { return hhmm }
        let total = (((h * 60 + m + (step > 0 ? 5 : -5)) % 1440) + 1440) % 1440
        return String(format: "%02d:%02d", total / 60, total % 60)
    }

    private func shortDate(_ iso: String) -> String {
        guard let d = Self.parse(iso) else { return iso }
        let f = DateFormatter()
        f.locale = Locale(identifier: Localization.shared.lang)
        f.dateFormat = "dd.MM."
        return f.string(from: d)
    }

    private static func parse(_ iso: String) -> Date? {
        let f = DateFormatter()
        f.calendar = Calendar(identifier: .gregorian)
        f.locale = Locale(identifier: "en_US_POSIX")
        f.dateFormat = "yyyy-MM-dd"
        return f.date(from: iso)
    }

    private static func iso(_ d: Date) -> String {
        let f = DateFormatter()
        f.calendar = Calendar(identifier: .gregorian)
        f.locale = Locale(identifier: "en_US_POSIX")
        f.dateFormat = "yyyy-MM-dd"
        return f.string(from: d)
    }
}
