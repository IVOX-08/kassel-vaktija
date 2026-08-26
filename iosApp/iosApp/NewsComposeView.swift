import SwiftUI
import PhotosUI

// Admin-only "post an announcement" sheet (spec section 9). Matches the Android flow: write the
// title and body in ANY of the 8 languages, optionally attach a flyer, then Gemini translates once
// here and the 8-language result goes to Firestore. Readers never translate.
//
// A failed translation must never block the imam: the post still goes out in the language it was
// written in, with a warning.
struct NewsComposeView: View {
    /// Verbandsweite Mitteilung des Hauptadministrators statt Beitrag dieser Gemeinde.
    var broadcast = false

    @Environment(\.dismiss) private var dismiss

    @State private var title = ""
    @State private var body_ = ""
    @State private var pickerItem: PhotosPickerItem?
    @State private var imageData: Data?
    @State private var busy = false
    @State private var warning: String?

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    TextField(L("news_title_label"), text: $title)
                        .font(.inter(16))
                    TextEditor(text: $body_)
                        .font(.inter(15))
                        .frame(minHeight: 140)
                }
                Section {
                    if let imageData, let image = UIImage(data: imageData) {
                        Image(uiImage: image)
                            .resizable().scaledToFit()
                            .clipShape(RoundedRectangle(cornerRadius: 10))
                        Button(L("news_remove_image"), role: .destructive) {
                            self.imageData = nil
                            pickerItem = nil
                        }
                    } else {
                        PhotosPicker(selection: $pickerItem, matching: .images) {
                            Label(L("news_add_image"), systemImage: "photo.badge.plus")
                        }
                    }
                }
                if let warning {
                    Section { Text(warning).font(.inter(13)).foregroundColor(.qiblaRed) }
                }
            }
            .navigationTitle(L("news_add"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(L("action_cancel")) { dismiss() }.disabled(busy)
                }
                ToolbarItem(placement: .confirmationAction) {
                    if busy {
                        ProgressView()
                    } else {
                        Button(L("news_post")) { Task { await post() } }
                            .disabled(title.isEmpty && body_.isEmpty)
                    }
                }
            }
            .overlay {
                if busy {
                    // Translation takes several seconds — say what is happening rather than freezing.
                    VStack(spacing: 10) {
                        ProgressView()
                        Text(L("news_translating")).font(.inter(13)).foregroundColor(.appOnSurfaceVariant)
                    }
                    .padding(20)
                    .background(Color.appSurface)
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                    .shadow(radius: 8)
                }
            }
        }
        .tint(.brandGreen)
        // Single-parameter onChange: the two-parameter form needs iOS 17, we ship iOS 16.
        .onChange(of: pickerItem) { item in
            Task { @MainActor in
                let data = await NewsComposeView.jpeg(from: item)
                imageData = data
            }
        }
    }

    private func post() async {
        busy = true
        warning = nil
        let sourceLang = Localization.shared.lang

        // Translate once, here. Nil result = unconfigured or every model failed.
        let result = await GeminiTranslator.shared.translateToAll(
            title: title, body: body_, fallbackLang: sourceLang
        )
        var titles = result?.titleByLang ?? [:]
        var bodies = result?.bodyByLang ?? [:]
        let source = result?.sourceLang ?? sourceLang
        // Always carry the admin's exact wording under its own language.
        titles[source] = title
        if !body_.isEmpty { bodies[source] = body_ }

        let ok = await AdminStore.shared.postNews(
            titleByLang: titles, bodyByLang: bodies, sourceLang: source, imageJPEG: imageData,
            broadcast: broadcast
        )
        busy = false

        guard ok else { warning = L("news_post_failed"); return }
        if result == nil || !(result?.failed.isEmpty ?? true) {
            warning = L("news_translate_partial")
            // The post is already live; give the warning a moment to be read.
            try? await Task.sleep(for: .seconds(3))
        }
        dismiss()
    }

    /// Re-encodes the picked photo as JPEG and keeps it well under Firestore's 1 MB document limit
    /// (Base64 adds ~33%), matching the Android compression step.
    private static func jpeg(from item: PhotosPickerItem?) async -> Data? {
        guard let item,
              let raw = try? await item.loadTransferable(type: Data.self),
              let image = UIImage(data: raw) else { return nil }
        let maxEdge: CGFloat = 1280
        let scale = min(1, maxEdge / max(image.size.width, image.size.height))
        let target = CGSize(width: image.size.width * scale, height: image.size.height * scale)
        let resized = UIGraphicsImageRenderer(size: target).image { _ in
            image.draw(in: CGRect(origin: .zero, size: target))
        }
        // Step the quality down until it fits; give up rather than post something that will be rejected.
        for quality in [0.7, 0.5, 0.35, 0.2] {
            if let data = resized.jpegData(compressionQuality: quality), data.count < 700_000 {
                return data
            }
        }
        return nil
    }
}
