package de.igbdsandzakkassel.vaktija.data.quran

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle

/**
 * The tajweed rules, as the bundled text marks them.
 *
 * The marking comes with the text; the app does not work the rules out for itself. Tajweed is an
 * exact discipline, and a colour placed by guesswork in a Qur'an is worse than no colour at all.
 *
 * Every rule carries TWO colours. The familiar ones are mixed for ink on white paper, and on a
 * black page a dark blue or a deep brown all but disappears — the rule is still marked, but nobody
 * can read it. The dark-page set keeps each rule recognisably the same hue and lifts it until it
 * carries against black.
 */
enum class TajweedRule(
    val code: Char,
    /** On a light page — the colours printed mushafs have used for decades. */
    val onLight: Color,
    /** The same hue, lifted to carry on a dark page. */
    val onDark: Color,
    val labelKey: String,
) {
    /** Hamzat wasl — the alif that is written but not pronounced when joined. */
    HAMZAT_WASL('h', Color(0xFF9AA0A6), Color(0xFF9AA0A6), "hamzat_wasl"),

    /** A letter written but not pronounced. */
    SILENT('s', Color(0xFF9AA0A6), Color(0xFF9AA0A6), "silent"),

    /** Lam of "al-" swallowed by a sun letter. */
    LAM_SHAMSIYYAH('l', Color(0xFF9AA0A6), Color(0xFF9AA0A6), "lam_shamsiyyah"),

    /** Madd of two counts. */
    MADD_NORMAL('n', Color(0xFF1E88E5), Color(0xFF64B5F6), "madd_normal"),

    /** Madd of two, four or six counts. */
    MADD_PERMISSIBLE('p', Color(0xFF3949AB), Color(0xFF9FA8DA), "madd_permissible"),

    /** Madd of four or five counts. */
    MADD_OBLIGATORY('o', Color(0xFF6A1B9A), Color(0xFFCE93D8), "madd_obligatory"),

    /** Madd of six counts. */
    MADD_NECESSARY('m', Color(0xFFC62828), Color(0xFFEF9A9A), "madd_necessary"),

    /** The echo on a qalqalah letter. */
    QALQALAH('q', Color(0xFF00897B), Color(0xFF4DB6AC), "qalqalah"),

    /** Nasal sound held for two counts. */
    GHUNNAH('g', Color(0xFFE65100), Color(0xFFFFB74D), "ghunnah"),

    /** Nun sakinah/tanwin hidden before certain letters. */
    IKHFA('f', Color(0xFF7B1FA2), Color(0xFFCE93D8), "ikhfa"),

    /** The same, at the lips (mim sakinah before ba). */
    IKHFA_SHAFAWI('c', Color(0xFF7B1FA2), Color(0xFFCE93D8), "ikhfa_shafawi"),

    /** Nun sakinah turned into a mim before ba. */
    IQLAB('w', Color(0xFF00838F), Color(0xFF4DD0E1), "iqlab"),

    /** Merged with a nasal sound. */
    IDGHAM_GHUNNAH('a', Color(0xFFAD1457), Color(0xFFF48FB1), "idgham_ghunnah"),

    /** Merged without one. */
    IDGHAM_NO_GHUNNAH('u', Color(0xFF8D6E63), Color(0xFFD7CCC8), "idgham_no_ghunnah"),

    /** Mim sakinah merged into a following mim. */
    IDGHAM_SHAFAWI('i', Color(0xFFAD1457), Color(0xFFF48FB1), "idgham_shafawi"),

    /** Merged with a letter from the same place of articulation. */
    IDGHAM_MUTAJANISAYN('d', Color(0xFF5D4037), Color(0xFFBCAAA4), "idgham_mutajanisayn"),

    /** Merged with a letter from a nearby place. */
    IDGHAM_MUTAQARIBAYN('b', Color(0xFF5D4037), Color(0xFFBCAAA4), "idgham_mutaqaribayn"),
    ;

    fun color(darkPage: Boolean): Color = if (darkPage) onDark else onLight

    companion object {
        private val BY_CODE = entries.associateBy { it.code }
        fun of(code: Char): TajweedRule? = BY_CODE[code]
    }
}

/**
 * Turns the marked text into coloured text.
 *
 * The marking looks like `بِسْمِ [h:1[ٱ]للَّهِ` — an opening `[`, the rule's letter, an optional
 * `:index`, another `[`, the letters the rule applies to, and a closing `]`. Anything the parser
 * does not recognise is emitted as plain text rather than dropped: a Qur'an must never lose a
 * letter to a parsing mistake.
 */
fun tajweedAnnotated(
    marked: String,
    baseColor: Color,
    darkPage: Boolean,
): AnnotatedString = buildAnnotatedString {
    var i = 0
    while (i < marked.length) {
        val open = marked.indexOf('[', i)
        if (open < 0) {
            appendPlain(marked.substring(i), baseColor)
            return@buildAnnotatedString
        }
        appendPlain(marked.substring(i, open), baseColor)

        val second = marked.indexOf('[', open + 1)
        val close = if (second >= 0) marked.indexOf(']', second + 1) else -1
        val header = if (second >= 0) marked.substring(open + 1, second) else ""
        val rule = header.firstOrNull()?.let { TajweedRule.of(it) }

        if (rule == null || close < 0) {
            // Not a marker we understand — keep the character and carry on.
            appendPlain(marked.substring(open, open + 1), baseColor)
            i = open + 1
            continue
        }
        withStyle(SpanStyle(color = rule.color(darkPage))) {
            append(marked.substring(second + 1, close))
        }
        i = close + 1
    }
}

private fun androidx.compose.ui.text.AnnotatedString.Builder.appendPlain(
    text: String,
    color: Color,
) {
    if (text.isEmpty()) return
    withStyle(SpanStyle(color = color)) { append(text) }
}
