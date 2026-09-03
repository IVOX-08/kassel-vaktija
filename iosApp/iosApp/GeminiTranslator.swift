//
//  GeminiTranslator.swift
//  Kassel Vaktija — iOS
//
//  1:1 port of app/src/main/java/.../data/translate/GeminiTranslator.kt.
//  Drop into the iOS target and call from the admin's "post announcement" flow.
//
//  Why this exists: announcements are translated ONCE, on the posting device, and the 8-language
//  result is written to Firestore. Readers never translate. So this file is only needed because the
//  admin now also wants to post from an iPhone — a reader-only build does not need it.
//
//  KEEP THE PROMPT IN SYNC with the Kotlin original. It encodes hard-won fixes: "džemat" must become
//  the mosque (not "the group"), and the model must never invent greetings or calls to action. If the
//  two platforms drift apart, the same announcement gets translated differently depending on which
//  phone the imam happened to use.
//

import Foundation

/// The 8 languages the app ships. Must match AppLanguage in the Kotlin source.
enum AppLanguage: String, CaseIterable {
    case bs, de, ar, tr, sq, en, ur, ru
    static let `default`: AppLanguage = .bs
}

struct TranslationResult {
    let sourceLang: String
    let titleByLang: [String: String]
    let bodyByLang: [String: String]
    /// App languages the model returned no title for — surface these to the admin as a warning.
    let failed: [String]
}

actor GeminiTranslator {

    // Free-tier models, tried in order. Measured 2026-08 against a real announcement containing the
    // vocabulary that trips translators up (mevlud, sadaka, abdesthana, akšam-namaz, džemat):
    // 3.6-flash 7.5-10s and the most natural output; flash-latest ~8s and equally faithful;
    // 2.5-flash was SLOWEST (12.7s) and stiffest, so it is last resort. Pro is unusable — it
    // returns 429 immediately on the free tier.
    private static let models = ["gemini-3.6-flash", "gemini-flash-latest", "gemini-2.5-flash"]
    private static let attemptsPerModel = 2
    private static let retryDelay: Duration = .milliseconds(800)
    private static let base = "https://generativelanguage.googleapis.com/v1beta/models"

    private let apiKey: String
    private let session: URLSession

    init(apiKey: String) {
        self.apiKey = apiKey
        let config = URLSessionConfiguration.default
        // Gemini can be slow to start streaming; give it room so a working-but-busy model isn't
        // killed prematurely, which would drop the post to an untranslated one.
        config.timeoutIntervalForRequest = 40
        config.timeoutIntervalForResource = 45
        self.session = URLSession(configuration: config)
    }

    var isConfigured: Bool { !apiKey.isEmpty }

    /// The shared instance, keyed from the gitignored `Secrets.plist` (mirrors Android's
    /// gemini.properties). Missing key → `isConfigured` is false and posting still works, just
    /// without translation.
    static let shared = GeminiTranslator(apiKey: GeminiTranslator.keyFromBundle())

    private static func keyFromBundle() -> String {
        guard let url = Bundle.main.url(forResource: "Secrets", withExtension: "plist"),
              let dict = NSDictionary(contentsOf: url) as? [String: Any],
              let key = dict["GEMINI_API_KEY"] as? String else { return "" }
        return key
    }

    /// Returns nil when unconfigured or when every model attempt failed. The caller must still allow
    /// the post to go through (source language only) — a failed translation must never block the imam.
    func translateToAll(title: String, body: String, fallbackLang: String) async -> TranslationResult? {
        guard isConfigured, !(title.isEmpty && body.isEmpty) else { return nil }

        for model in Self.models {
            for attempt in 0..<Self.attemptsPerModel {
                if let result = try? await request(model: model, title: title, body: body,
                                                   fallbackLang: fallbackLang) {
                    return result
                }
                if attempt < Self.attemptsPerModel - 1 {
                    try? await Task.sleep(for: Self.retryDelay)
                }
            }
        }
        return nil
    }

    // MARK: - Networking

    private func request(model: String, title: String, body: String,
                         fallbackLang: String) async throws -> TranslationResult {
        let payload: [String: Any] = [
            "contents": [["parts": [["text": Self.buildPrompt(title: title, body: body)]]]],
            "generationConfig": ["temperature": 0.2, "responseMimeType": "application/json"],
        ]

        var request = URLRequest(url: URL(string: "\(Self.base)/\(model):generateContent")!)
        request.httpMethod = "POST"
        request.setValue("application/json; charset=utf-8", forHTTPHeaderField: "Content-Type")
        // Header rather than a query parameter: keys in URLs end up in logs and crash reports.
        request.setValue(apiKey, forHTTPHeaderField: "x-goog-api-key")
        request.httpBody = try JSONSerialization.data(withJSONObject: payload)

        let (data, response) = try await session.data(for: request)
        guard let http = response as? HTTPURLResponse, (200..<300).contains(http.statusCode) else {
            throw TranslatorError.http((response as? HTTPURLResponse)?.statusCode ?? -1)
        }

        // The model's JSON answer sits in candidates[0].content.parts. Take the first part with
        // non-blank text — "thinking" models can prepend a thought part.
        let root = try JSONSerialization.jsonObject(with: data) as? [String: Any]
        let parts = (root?["candidates"] as? [[String: Any]])?.first
            .flatMap { $0["content"] as? [String: Any] }
            .flatMap { $0["parts"] as? [[String: Any]] }
        guard let answer = parts?.compactMap({ $0["text"] as? String })
            .first(where: { !$0.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty })
        else { throw TranslatorError.emptyResponse }

        guard let parsed = try JSONSerialization.jsonObject(with: Data(Self.stripFences(answer).utf8))
                as? [String: Any]
        else { throw TranslatorError.emptyResponse }

        let validTags = Set(AppLanguage.allCases.map(\.rawValue))
        let source = (parsed["sourceLang"] as? String)?
            .lowercased()
            .components(separatedBy: "-").first
            .flatMap { validTags.contains($0) ? $0 : nil }
            ?? fallbackLang

        var titleByLang = Self.readLangMap(parsed["title"], validTags: validTags)
        var bodyByLang = Self.readLangMap(parsed["body"], validTags: validTags)

        // Guarantee the admin's exact original text under its own language — never a round-trip.
        titleByLang[source] = title
        if !body.isEmpty { bodyByLang[source] = body }

        let failed = title.isEmpty ? [] : validTags.filter {
            $0 != source && (titleByLang[$0]?.isEmpty ?? true)
        }.sorted()

        return TranslationResult(sourceLang: source, titleByLang: titleByLang,
                                 bodyByLang: bodyByLang, failed: failed)
    }

    enum TranslatorError: Error { case http(Int), emptyResponse }

    // MARK: - Parsing helpers

    private static func readLangMap(_ value: Any?, validTags: Set<String>) -> [String: String] {
        guard let dict = value as? [String: Any] else { return [:] }
        var out: [String: String] = [:]
        for (key, raw) in dict {
            let tag = key.lowercased()
            if validTags.contains(tag), let text = raw as? String, !text.isEmpty { out[tag] = text }
        }
        return out
    }

    private static func stripFences(_ text: String) -> String {
        var s = text.trimmingCharacters(in: .whitespacesAndNewlines)
        if s.hasPrefix("```json") { s.removeFirst("```json".count) }
        else if s.hasPrefix("```") { s.removeFirst(3) }
        if s.hasSuffix("```") { s.removeLast(3) }
        return s.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    // MARK: - Prompt (keep byte-identical in meaning to the Kotlin version)

    private static func buildPrompt(title: String, body: String) -> String {
        """
        You are a professional translator for the mobile app of the Bosniak (Sandžak) Muslim community
        mosque "\(CommunitySelection.communityName ?? "IGBD")" in Germany. You translate short community
        announcements (prayer/event notices) for ordinary members.

        Translate the announcement below into ALL of these languages and return STRICT JSON.
        Use these exact keys: bs=Bosnian, de=German, ar=Modern Standard Arabic, tr=Turkish,
        sq=Albanian, en=English, ur=Urdu, ru=Russian.

        Rules:
        - Faithful, natural, fluent translation. Preserve meaning, tone, dates, times, numbers, names
          and line breaks. Do NOT add or omit information.
        - Never add a phrase that is not in the source — no extra politeness formulas, greetings,
          blessings or calls to action ("we await your contributions", "may Allah reward you").
          If the source does not say it, it must not appear in the translation.
        - Use the CORRECT Islamic/religious term in each language; never transliterate religious terms
          literally. Examples: Bajram/Bayram = Eid (ar: العيد; the Eid prayer = صلاة العيد);
          namaz / Gebet = the ritual prayer (ar: الصلاة); Džuma = Jumuʿah (ar: صلاة الجمعة);
          Teravija = Tarawih (ar: صلاة التراويح); iftar (ar: الإفطار); sehur (ar: السحور);
          Ramazan = Ramadan (ar: رمضان); Mevlud = Mawlid; sadaka = sadaqah; abdest = wudu;
          džemat = the mosque congregation — when it means the building say "the mosque"
          (ar: المسجد, tr: cami), never a literal "the group/community" (ar: الجماعة).
        - Arabic must be correct, natural Modern Standard Arabic — not a word-for-word gloss.
        - Detect the source language (one of the keys above) and report it as "sourceLang".

        Return ONLY a JSON object, no markdown, with exactly this shape:
        {"sourceLang":"<key>","title":{"bs":"","de":"","ar":"","tr":"","sq":"","en":"","ur":"","ru":""},"body":{"bs":"","de":"","ar":"","tr":"","sq":"","en":"","ur":"","ru":""}}

        TITLE:
        \(title)

        BODY:
        \(body)
        """
    }
}

//
//  WHERE THE API KEY COMES FROM
//
//  Do NOT hardcode it and do NOT commit it. Mirror the Android setup: a gitignored config file read
//  at build time. On iOS: add `GEMINI_API_KEY` to a gitignored .xcconfig, surface it through
//  Info.plist, and read it with Bundle.main.object(forInfoDictionaryKey:).
//
//  Be clear-eyed about what this does and does not buy: a key shipped inside an app binary is
//  extractable by anyone who downloads the app, on iOS exactly as on Android. This is the same
//  exposure the Android build already accepts, so it is parity, not a new risk — but if the key ever
//  leaks and gets abused, the durable fix is to move translation behind a Cloud Function so the key
//  lives only on the server. Worth doing if the app grows beyond one admin.
//
