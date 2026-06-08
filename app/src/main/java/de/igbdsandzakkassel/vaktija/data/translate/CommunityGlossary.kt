package de.igbdsandzakkassel.vaktija.data.translate

/**
 * Community / Islamic terminology that generic machine translation mishandles — it transliterates a
 * word like "Bajram" letter-by-letter (e.g. Arabic "باجرام") instead of using the real word for
 * "Eid". For each term we keep the correct word in every app language and, BEFORE translating, swap
 * the term in the source text for its correct TARGET-language form. The translator then keeps that
 * word (it is already in the target language) and only translates the surrounding text.
 *
 * Matching is case-insensitive on non-letter boundaries, longest alias first (so "Bajram-namaz"
 * wins over "Bajram"). To add a term, append a [Term] to [TERMS]. The non-bs/de/en forms were
 * produced and native-reviewed via a translation workflow.
 */
object CommunityGlossary {

    data class Term(val aliases: List<String>, val byLang: Map<String, String>)

    val TERMS: List<Term> = listOf(
        // Eid prayer (Salat al-Eid)
        Term(
            listOf("Bajram-namaz", "Bajram namaz", "Bajramski namaz", "Bajram-Gebet", "Bayram namazı"),
            mapOf(
                "bs" to "Bajram-namaz", "de" to "Bajram-Gebet", "en" to "Eid prayer",
                "ar" to "صلاة العيد", "tr" to "Bayram namazı", "sq" to "Namazi i Bajramit",
                "ur" to "نمازِ عید", "ru" to "праздничный намаз",
            ),
        ),
        // Eid al-Fitr (end of Ramadan)
        Term(
            listOf("Ramazanski bajram", "Ramazanski Bajram", "Ramazan-Bajram", "Ramazan Bajram"),
            mapOf(
                "bs" to "Ramazanski bajram", "de" to "Ramazanski Bajram", "en" to "Eid al-Fitr",
                "ar" to "عيد الفطر", "tr" to "Ramazan Bayramı", "sq" to "Fitër Bajrami",
                "ur" to "عیدالفطر", "ru" to "Ураза-байрам",
            ),
        ),
        // Eid al-Adha (festival of sacrifice)
        Term(
            listOf("Kurban-bajram", "Kurban Bajram", "Kurbanski bajram", "Kurban-Bajram"),
            mapOf(
                "bs" to "Kurban-bajram", "de" to "Kurban-Bajram", "en" to "Eid al-Adha",
                "ar" to "عيد الأضحى", "tr" to "Kurban Bayramı", "sq" to "Kurban Bajrami",
                "ur" to "عیدالاضحیٰ", "ru" to "Курбан-байрам",
            ),
        ),
        // Eid (the festival, general)
        Term(
            listOf("Bajram", "Bayram"),
            mapOf(
                "bs" to "Bajram", "de" to "Bajram", "en" to "Eid",
                "ar" to "العيد", "tr" to "Bayram", "sq" to "Bajram",
                "ur" to "عید", "ru" to "праздник Ид",
            ),
        ),
        // Tarawih (Ramadan night prayer)
        Term(
            listOf("Teravija-namaz", "Teravih-namaz", "Teravija", "Teravih", "Tarawih"),
            mapOf(
                "bs" to "Teravija", "de" to "Tarawih-Gebet", "en" to "Tarawih",
                "ar" to "صلاة التراويح", "tr" to "Teravih namazı", "sq" to "Teravia",
                "ur" to "تراویح", "ru" to "таравих",
            ),
        ),
        // Friday prayer (Jumu'ah)
        Term(
            listOf("Džuma-namaz", "Džuma namaz", "Dzuma-namaz", "Džuma", "Dzuma"),
            mapOf(
                "bs" to "Džuma-namaz", "de" to "Freitagsgebet", "en" to "Friday prayer",
                "ar" to "صلاة الجمعة", "tr" to "Cuma namazı", "sq" to "Namazi i xhumasë",
                "ur" to "نمازِ جمعہ", "ru" to "пятничный намаз",
            ),
        ),
        // Ramadan
        Term(
            listOf("Ramazan", "Ramadan"),
            mapOf(
                "bs" to "Ramazan", "de" to "Ramadan", "en" to "Ramadan",
                "ar" to "رمضان", "tr" to "Ramazan", "sq" to "Ramazan",
                "ur" to "رمضان", "ru" to "Рамадан",
            ),
        ),
        // Iftar (sunset meal)
        Term(
            listOf("Iftar"),
            mapOf(
                "bs" to "Iftar", "de" to "Iftar", "en" to "Iftar",
                "ar" to "الإفطار", "tr" to "İftar", "sq" to "Iftar",
                "ur" to "افطار", "ru" to "ифтар",
            ),
        ),
        // Suhoor (pre-dawn meal)
        Term(
            listOf("Sehur", "Sehari"),
            mapOf(
                "bs" to "Sehur", "de" to "Sehur", "en" to "Suhoor",
                "ar" to "السحور", "tr" to "Sahur", "sq" to "Syfyr",
                "ur" to "سحری", "ru" to "сухур",
            ),
        ),
        // Mawlid (Prophet's birth celebration)
        Term(
            listOf("Mevlud", "Mevlid"),
            mapOf(
                "bs" to "Mevlud", "de" to "Mevlud", "en" to "Mawlid",
                "ar" to "المولد النبوي", "tr" to "Mevlit", "sq" to "Mevlud",
                "ur" to "میلاد", "ru" to "мавлид",
            ),
        ),
        // Prayer (salah, generic) — keep last so phrases like "Bajram-namaz" match first
        Term(
            listOf("namaz"),
            mapOf(
                "bs" to "namaz", "de" to "Gebet", "en" to "prayer",
                "ar" to "الصلاة", "tr" to "namaz", "sq" to "namaz",
                "ur" to "نماز", "ru" to "намаз",
            ),
        ),
    )

    private val termByAlias: Map<String, Term> =
        TERMS.flatMap { term -> term.aliases.map { it.lowercase() to term } }.toMap()

    // One regex of all aliases, longest first so multi-word terms win over their sub-words.
    private val regex: Regex = Regex(
        "(?<!\\p{L})(?:" +
            termByAlias.keys.sortedByDescending { it.length }.joinToString("|") { Regex.escape(it) } +
            ")(?!\\p{L})",
        RegexOption.IGNORE_CASE,
    )

    /**
     * Returns [text] with any known community term replaced by its correct [lang] form, ready to be
     * machine-translated into [lang]. Unknown languages fall back to the matched text unchanged.
     */
    fun localize(text: String, lang: String): String {
        if (text.isBlank()) return text
        return regex.replace(text) { m ->
            termByAlias[m.value.lowercase()]?.byLang?.get(lang) ?: m.value
        }
    }
}
