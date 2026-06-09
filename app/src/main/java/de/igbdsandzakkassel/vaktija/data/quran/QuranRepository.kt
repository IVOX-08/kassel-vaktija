package de.igbdsandzakkassel.vaktija.data.quran

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/** Surah metadata for the chapter list. */
@Serializable
data class SurahMeta(
    val id: Int,
    val name: String,            // Arabic name, e.g. الفاتحة
    val transliteration: String, // e.g. Al-Fatihah
    val type: String,            // "meccan" | "medinan"
    @SerialName("total_verses") val totalVerses: Int,
)

/** A single ayah: number within the surah + Uthmani Arabic text + its Mushaf page (1..604). */
data class Ayah(val number: Int, val text: String, val page: Int)

@Serializable
private data class SurahFile(val ayahs: List<AyahEntry> = emptyList())

@Serializable
private data class AyahEntry(val n: Int = 0, val t: String = "", val p: Int = 0)

/**
 * The Qur'an in Arabic only (Uthmani text, public domain), bundled as JSON assets:
 * `quran/index.json` (114 surah metadata) and `quran/<id>.json` (the surah's ayahs). No
 * translations are shipped — by design, to avoid translation/ʿaqīdah disputes.
 */
@Singleton
class QuranRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val json = Json { ignoreUnknownKeys = true }

    fun surahs(): List<SurahMeta> = runCatching {
        context.assets.open("quran/index.json").use {
            json.decodeFromString<List<SurahMeta>>(it.readBytes().decodeToString())
        }
    }.getOrDefault(emptyList())

    fun ayahs(surahId: Int): List<Ayah> = runCatching {
        context.assets.open("quran/$surahId.json").use {
            json.decodeFromString<SurahFile>(it.readBytes().decodeToString())
                .ayahs.map { a -> Ayah(a.n, a.t, a.p) }
        }
    }.getOrDefault(emptyList())
}
