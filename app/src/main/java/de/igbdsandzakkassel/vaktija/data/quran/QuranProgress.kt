package de.igbdsandzakkassel.vaktija.data.quran

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue

/**
 * Qur'an reading progress: the last-read position (for "continue reading") and page bookmarks.
 * Held as observable Compose state so the reader and the surah list stay in sync instantly, and
 * mirrored to SharedPreferences so it survives restarts. A position is identified by surah id + the
 * first ayah number on the page (stable even though pages are laid out dynamically).
 */
object QuranProgress {

    var lastSurah by mutableIntStateOf(0) // 0 = nothing read yet
        private set
    var lastAyah by mutableIntStateOf(1)
        private set

    /** Bookmarks as "surah:ayah" keys (observable). */
    val bookmarks = mutableStateListOf<String>()

    private var loaded = false
    private fun prefs(c: Context) = c.getSharedPreferences("quran_prefs", Context.MODE_PRIVATE)

    fun load(context: Context) {
        if (loaded) return
        loaded = true
        val p = prefs(context)
        lastSurah = p.getInt("last_surah", 0)
        lastAyah = p.getInt("last_ayah", 1)
        bookmarks.clear()
        bookmarks.addAll(p.getStringSet("bookmarks", emptySet()).orEmpty())
    }

    fun saveLast(context: Context, surah: Int, ayah: Int) {
        if (surah == lastSurah && ayah == lastAyah) return
        lastSurah = surah
        lastAyah = ayah
        prefs(context).edit().putInt("last_surah", surah).putInt("last_ayah", ayah).apply()
    }

    fun isBookmarked(surah: Int, ayah: Int): Boolean = "$surah:$ayah" in bookmarks

    fun toggleBookmark(context: Context, surah: Int, ayah: Int) {
        val key = "$surah:$ayah"
        if (!bookmarks.remove(key)) bookmarks.add(key)
        prefs(context).edit().putStringSet("bookmarks", bookmarks.toSet()).apply()
    }
}
