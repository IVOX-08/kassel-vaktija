package de.igbdsandzakkassel.vaktija.data.quran

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** The script the Qur'an is set in. */
enum class QuranScript {
    /**
     * The phone's own Arabic face — what the app has always shown, and what most readers here are
     * already used to.
     */
    SYSTEM,

    /**
     * Amiri Quran: the classical naskh cut used by Ottoman and Turkish printed mushafs. Asked for
     * by the board, because that is the script their members learned to read from.
     */
    OTTOMAN,
}

/**
 * How the reader is set up: script, size and whether the tajweed rules are coloured.
 *
 * A plain object with a SharedPreferences file behind it, matching [QuranProgress] next door.
 * These are three settings read on every frame of the reader and written when a button is pressed;
 * routing them through DataStore and a ViewModel would add machinery without adding anything.
 */
object QuranReaderPrefs {

    var script by mutableStateOf(QuranScript.SYSTEM)
        private set

    /** Multiplier on the base text size. Kept inside [MIN_SCALE]..[MAX_SCALE]. */
    var scale by mutableFloatStateOf(1f)
        private set

    var tajweed by mutableStateOf(false)
        private set

    private var loaded = false
    private fun prefs(context: Context) =
        context.getSharedPreferences("quran_reader", Context.MODE_PRIVATE)

    fun load(context: Context) {
        if (loaded) return
        loaded = true
        val p = prefs(context)
        script = runCatching { QuranScript.valueOf(p.getString(KEY_SCRIPT, "") ?: "") }
            .getOrDefault(QuranScript.SYSTEM)
        scale = p.getFloat(KEY_SCALE, 1f).coerceIn(MIN_SCALE, MAX_SCALE)
        tajweed = p.getBoolean(KEY_TAJWEED, false)
    }

    fun setScript(context: Context, value: QuranScript) {
        script = value
        prefs(context).edit().putString(KEY_SCRIPT, value.name).apply()
    }

    fun setTajweed(context: Context, value: Boolean) {
        tajweed = value
        prefs(context).edit().putBoolean(KEY_TAJWEED, value).apply()
    }

    /** Steps rather than free zoom: every step still has to lay out into whole pages. */
    fun zoom(context: Context, delta: Float) {
        scale = (scale + delta).coerceIn(MIN_SCALE, MAX_SCALE)
        prefs(context).edit().putFloat(KEY_SCALE, scale).apply()
    }

    const val MIN_SCALE = 0.7f
    const val MAX_SCALE = 1.8f
    const val STEP = 0.1f

    private const val KEY_SCRIPT = "script"
    private const val KEY_SCALE = "scale"
    private const val KEY_TAJWEED = "tajweed"
}
