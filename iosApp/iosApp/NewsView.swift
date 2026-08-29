import SwiftUI

// "Nachrichten" (spec section 4): the community announcements the admin posts from Android (and
// later from the iPhone). Each item is shown in the reader's app language, falling back to the
// language it was written in. Posting/deleting is admin-only and comes with the admin area.
struct NewsView: View {
    @StateObject private var store = NewsStore()
    @ObservedObject private var admin = AdminStore.shared
    @State private var showBroadcast = false
    @State private var viewerImage: Data?
    @State private var showCompose = false
    @State private var pendingDelete: NewsItem?

    /// Instagram, Facebook und YouTube der gewaehlten Gemeinde.
    ///
    /// Schlichte einfarbige Zeichen in den Farben der App — KEINE nachgemalten Markenlogos. Die
    /// echten Logos der drei Netze duerfen nicht nachgezeichnet werden, und eine schlechte Kopie
    /// waere ohnehin schlechter als ein klares Symbol.
    @ViewBuilder private var socialLinks: some View {
        let community = CommunityCatalog.shared.selected
        HStack(spacing: 14) {
            socialLink(community?.instagramUrl, "camera")
            socialLink(community?.facebookUrl, "person.2")
            socialLink(community?.youtubeUrl, "play.rectangle")
        }
    }

    @ViewBuilder private func socialLink(_ raw: String?, _ icon: String) -> some View {
        if let raw, !raw.isEmpty, let url = URL(string: raw) {
            Link(destination: url) {
                Image(systemName: icon)
                    .font(.system(size: 17, weight: .medium))
                    .foregroundColor(.brandGreen)
            }
        }
    }

    private var deleteDialog: Binding<Bool> {
        Binding(get: { pendingDelete != nil }, set: { if !$0 { pendingDelete = nil } })
    }

    var body: some View {
        NavigationStack {
            Group {
                switch store.items {
                case .none:
                    ProgressView().tint(.brandGreen)
                case .some(let list) where list.isEmpty:
                    Text(L("news_empty"))
                        .font(.inter(17)).foregroundColor(.appOnSurfaceVariant)
                        .multilineTextAlignment(.center).padding(.horizontal, 32)
                case .some(let list):
                    ScrollView {
                        LazyVStack(spacing: 12) {
                            ForEach(list) { item in
                                NewsCard(item: item, canDelete: admin.canPostNews && !item.isBroadcast,
                                         myReaction: store.myReactions[item.id],
                                         loadImage: store.image,
                                         onImageTap: { viewerImage = $0 },
                                         onReact: { choice in
                                             Task { await store.react(item, choice) }
                                         },
                                         onDelete: { pendingDelete = item })
                            }
                        }
                        .padding(.horizontal, 16).padding(.vertical, 12)
                    }
                }
            }
            .safeAreaInset(edge: .top) {
                if admin.canBroadcast {
                    // Der Hauptadministrator schreibt an ALLE Gemeinden, nicht an diese eine.
                    // Er darf hier bewusst keinen Gemeindebeitrag verfassen.
                    Button { showBroadcast = true } label: {
                        Label(L("news_add_broadcast"), systemImage: "megaphone")
                            .font(.inter(15, .semibold)).foregroundColor(.white)
                            .frame(maxWidth: .infinity).padding(.vertical, 12)
                            .background(Color.brandGreen)
                            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                    }
                    .padding(.horizontal, 16).padding(.bottom, 8)
                }
                if admin.canPostNews {
                    Button { showCompose = true } label: {
                        Label(L("news_add"), systemImage: "plus")
                            .font(.inter(15, .semibold)).foregroundColor(.white)
                            .frame(maxWidth: .infinity).padding(.vertical, 12)
                            .background(Color.brandGreen)
                            .clipShape(RoundedRectangle(cornerRadius: 10))
                    }
                    .padding(.horizontal, 16).padding(.bottom, 4)
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .background(Color.appBackground.ignoresSafeArea())
            .navigationTitle(L("nav_news"))
            // Die Konten der Gemeinde stehen NEBEN der Ueberschrift, nicht am Fuss der Liste:
            // dort ist jemand, der gerade eine Mitteilung gelesen hat und mehr will.
            .toolbar { ToolbarItem(placement: .navigationBarTrailing) { socialLinks } }
        }
        .onAppear { store.start(); admin.start() }
        .fullScreenCover(item: $viewerImage) { data in
            FullScreenImageViewer(data: data) { viewerImage = nil }
        }
        .sheet(isPresented: $showCompose) { NewsComposeView() }
        .sheet(isPresented: $showBroadcast) { NewsComposeView(broadcast: true) }
        .confirmationDialog(L("news_delete_confirm"), isPresented: deleteDialog, titleVisibility: .visible) {
            Button(L("news_delete"), role: .destructive) {
                if let id = pendingDelete?.id {
                    Task { _ = await AdminStore.shared.deleteNews(id) }
                }
                pendingDelete = nil
            }
            Button(L("action_cancel"), role: .cancel) { pendingDelete = nil }
        }
    }
}

private struct NewsCard: View {
    let item: NewsItem
    let canDelete: Bool
    let myReaction: Reaction?
    let loadImage: (NewsItem) async -> Data?
    let onImageTap: (Data) -> Void
    let onReact: (Reaction) -> Void
    let onDelete: () -> Void

    @Environment(\.colorScheme) private var scheme
    @State private var flyer: Data?
    @State private var flyerLoaded = false

    private var lang: String { Localization.shared.lang }

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            senderRow
            HStack(alignment: .top) {
                Text(item.title(lang)).font(.inter(17, .bold)).foregroundColor(.brandGreen)
                if canDelete {
                    Spacer()
                    Button(action: onDelete) {
                        Image(systemName: "trash").font(.system(size: 16)).foregroundColor(.qiblaRed)
                    }
                    .buttonStyle(.plain)
                    .accessibilityLabel(L("news_delete"))
                }
            }
            if let date = item.date {
                Text(dateText(date)).font(.inter(12)).foregroundColor(.appOnSurfaceVariant)
            }
            let body = item.body(lang)
            if !body.isEmpty {
                Text(body).font(.inter(15)).foregroundColor(.appOnSurface)
                    .fixedSize(horizontal: false, vertical: true)
            }
            // Ein Video- oder Beitragslink im Text bekommt eine Karte. Der Vorstand muss dafür
            // nichts Zusätzliches ausfüllen — er schreibt den Link einfach mit hinein.
            if let link = SocialLink.first(in: body) {
                SocialLinkCard(link: link)
            }
            if item.hasImage {
                flyerSlot
            }
            reactionRow
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(16)
        .background(Color.appSurface)
        .clipShape(RoundedRectangle(cornerRadius: Radius.smallCard, style: .continuous))
        .shadow(color: .black.opacity(0.06), radius: 3, y: 1)
    }

    /// Wer die Mitteilung geschickt hat, mit Wappen — wie auf Android.
    ///
    /// Ohne diese Zeile wäre nicht erklärbar, warum in der Liste der eigenen Gemeinde plötzlich
    /// etwas steht, das der eigene Vorstand nie geschrieben hat.
    private var senderRow: some View {
        HStack(spacing: 8) {
            // Eine Rundnachricht kommt vom VERBAND und traegt sein Zeichen. Vorher stand hier
            // ein graues Haus-Symbol; auf Android stand sogar Kassels Wappen — jede Mitteilung des
            // Verbands kam damit an, als haette Kassel sie geschickt.
            if item.isBroadcast {
                Image(uiImage: UIImage(named: scheme == .dark ? "logo_igbd_dark" : "logo_igbd") ?? UIImage())
                    .resizable().scaledToFit()
                    .frame(width: 26, height: 26)
            } else {
                Image(systemName: "moon.stars.fill")
                    .font(.system(size: 14)).foregroundColor(.brandGreen)
                    .frame(width: 26, height: 26)
                    .background(Color.brandGreen.opacity(0.12)).clipShape(Circle())
            }
            Text(item.isBroadcast
                 ? L("news_sent_by_union")
                 : String(format: L("news_sent_by"), CommunityCatalog.shared.selected?.name ?? ""))
                .font(.inter(12)).foregroundColor(.appOnSurfaceVariant)
                .lineLimit(2)
            Spacer()
        }
    }

    /// Herz und Daumen wie auf Android: derselbe Knopf noch einmal nimmt die Reaktion zurück.
    /// Die Zahl bewegt sich sofort, weil Firestore die Erhöhung lokal rechnet.
    private var reactionRow: some View {
        HStack(spacing: 16) {
            reactionButton(.like, filled: "heart.fill", hollow: "heart", count: item.likeCount)
            reactionButton(.dislike, filled: "hand.thumbsdown.fill", hollow: "hand.thumbsdown",
                           count: item.dislikeCount)
            Spacer()
        }
        .padding(.top, 2)
    }

    private func reactionButton(_ choice: Reaction, filled: String, hollow: String,
                                count: Int) -> some View {
        let chosen = myReaction == choice
        return Button { onReact(choice) } label: {
            HStack(spacing: 5) {
                Image(systemName: chosen ? filled : hollow).font(.system(size: 15))
                if count > 0 { Text("\(count)").font(.inter(13)) }
            }
            .foregroundColor(chosen ? .brandGreen : .appOnSurfaceVariant)
        }
        .buttonStyle(.plain)
    }

    // Fetched only once this card is on screen. If it can't be loaded the slot disappears rather
    // than spinning forever — same behaviour as Android.
    @ViewBuilder private var flyerSlot: some View {
        if let flyer, let image = UIImage(data: flyer) {
            Image(uiImage: image)
                .resizable().scaledToFit()
                .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                .onTapGesture { onImageTap(flyer) }
        } else if !flyerLoaded {
            RoundedRectangle(cornerRadius: 12, style: .continuous)
                .fill(Color.appOnSurfaceVariant.opacity(0.15))
                .aspectRatio(16.0 / 9.0, contentMode: .fit)
                .overlay(ProgressView().tint(.brandGreen))
                .task {
                    flyer = await loadImage(item)
                    flyerLoaded = true
                }
        }
    }

    private func dateText(_ d: Date) -> String {
        let f = DateFormatter()
        f.locale = Locale(identifier: lang)
        f.dateFormat = "d. MMMM yyyy, HH:mm"
        return f.string(from: d)
    }
}

/// Full-screen flyer with pinch-to-zoom, dismissed by tapping the close button or double-tapping.
private struct FullScreenImageViewer: View {
    let data: Data
    let onDismiss: () -> Void

    @State private var scale: CGFloat = 1
    @State private var offset: CGSize = .zero

    var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()
            if let image = UIImage(data: data) {
                Image(uiImage: image)
                    .resizable().scaledToFit()
                    .scaleEffect(scale)
                    .offset(offset)
                    .gesture(
                        MagnificationGesture()
                            .onChanged { scale = max(1, min(5, $0)) }
                            .onEnded { _ in if scale <= 1 { withAnimation { offset = .zero } } }
                    )
                    .simultaneousGesture(
                        DragGesture()
                            .onChanged { if scale > 1 { offset = $0.translation } }
                    )
                    .onTapGesture(count: 2) {
                        withAnimation { scale = scale > 1 ? 1 : 2; offset = .zero }
                    }
            }
            VStack {
                HStack {
                    Spacer()
                    Button(action: onDismiss) {
                        Image(systemName: "xmark")
                            .font(.system(size: 18, weight: .bold)).foregroundColor(.white)
                            .padding(12).background(Circle().fill(Color.black.opacity(0.5)))
                    }
                    .padding(16)
                }
                Spacer()
            }
        }
    }
}

// Lets `Data` drive .fullScreenCover(item:).
extension Data: Identifiable {
    public var id: Int { hashValue }
}
