package de.igbdsandzakkassel.vaktija.data.hadith

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/** One hadith ready for display: the Arabic original + the translation in the chosen language. */
data class HadithItem(
    val number: Int,
    val arabic: String,
    val translation: String,
)

@Serializable
private data class HadithEdition(val hadiths: List<HadithEntry> = emptyList())

@Serializable
private data class HadithEntry(val hadithnumber: Int = 0, val text: String = "")

/**
 * Loads hadith collections bundled as JSON assets (assets/hadith/<collection>/<lang>.json). The
 * Arabic file (ar.json) is the source of truth for numbering; the translation file for the chosen
 * language is joined by hadith number, falling back to English when a language is not yet available.
 * Returns an empty list if the collection isn't bundled yet (e.g. Riyad as-Salihin — coming).
 */
@Singleton
class HadithRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val json = Json { ignoreUnknownKeys = true }

    fun load(collection: String, lang: String): List<HadithItem> {
        val arabic = parse(collection, "ar") ?: return emptyList()
        // For Arabic readers the matn IS the content — don't repeat it as a "translation".
        val translation = if (lang == "ar") emptyList()
        else parse(collection, lang) ?: parse(collection, "en").orEmpty()
        val byNumber = translation.associateBy { it.hadithnumber }
        return arabic
            .filter { it.hadithnumber > 0 && it.text.isNotBlank() }
            .map { entry ->
                HadithItem(
                    number = entry.hadithnumber,
                    arabic = entry.text,
                    translation = byNumber[entry.hadithnumber]?.text.orEmpty(),
                )
            }
    }

    private fun parse(collection: String, lang: String): List<HadithEntry>? = runCatching {
        context.assets.open("hadith/$collection/$lang.json").use { stream ->
            json.decodeFromString<HadithEdition>(stream.readBytes().decodeToString()).hadiths
        }
    }.getOrNull()
}
