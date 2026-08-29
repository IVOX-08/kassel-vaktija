import SwiftUI

// Runtime language switch (spec 6.6 + update prompt #3): the chosen app language overrides the
// system language, applies immediately (incl. RTL for ar/ur), and persists across restarts.
// Strings come from the bundled Resources/lang/<tag>.strings (converted from the Android resources),
// with Bosnian (the default language) as the fallback for any key a translation is missing.
final class Localization: ObservableObject {
    static let shared = Localization()

    @Published private(set) var lang: String
    private var table: [String: String] = [:]
    private var fallback: [String: String] = [:]

    private init() {
        lang = AppGroup.defaults.string(forKey: "app_lang") ?? "bs"
        fallback = Localization.read("bs")
        table = Localization.read(lang)
    }

    /// Switch language: persist, reload the table, and publish so the UI rebuilds.
    func set(_ newLang: String) {
        guard newLang != lang else { return }
        AppGroup.defaults.set(newLang, forKey: "app_lang")
        // Das Widget spricht sonst weiter die alte Sprache — es merkt einen Wechsel nicht selbst.
        WidgetRefresh.now()
        table = Localization.read(newLang)
        lang = newLang // @Published — triggers the root rebuild (.id(lang))
    }

    func string(_ key: String) -> String { table[key] ?? fallback[key] ?? key }

    var isRTL: Bool { lang == "ar" || lang == "ur" }
    var layoutDirection: LayoutDirection { isRTL ? .rightToLeft : .leftToRight }

    private static func read(_ tag: String) -> [String: String] {
        guard let url = Bundle.main.url(forResource: tag, withExtension: "strings", subdirectory: "lang"),
              let dict = NSDictionary(contentsOf: url) as? [String: String] else { return [:] }
        return dict
    }
}

/// Shorthand for a localized string in the currently selected app language.
func L(_ key: String) -> String { Localization.shared.string(key) }
