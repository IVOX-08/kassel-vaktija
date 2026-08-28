package de.igbdsandzakkassel.vaktija.data.settings

import androidx.annotation.StringRes
import de.igbdsandzakkassel.vaktija.R

/**
 * The tone a community announcement arrives with.
 *
 * A separate choice from the Adhan's, because the two are heard in different situations: the Adhan
 * calls to prayer, an announcement says the mosque has posted something. Someone who set the Adhan
 * to a plain bell may still want the announcement to sound different, or the other way round.
 *
 * Android locks a channel's sound once the channel exists, so each choice gets its own channel and
 * switching means posting on a different one. That is the only way a tone change can take effect
 * without asking people to reinstall the app.
 */
enum class NewsSound(
    @param:StringRes val labelRes: Int,
    val rawResName: String,
) {
    DEFAULT(R.string.sound_announcement, "announcement"),
    BELL(R.string.sound_bell, "tone_bell"),
    SOFT(R.string.sound_soft, "tone_soft"),
    ;

    /** Channel id for this tone. Versioned so a changed sound file gets a fresh channel. */
    val channelId: String get() = "news_v3_" + name.lowercase()

    companion object {
        val DEFAULT_SOUND = DEFAULT

        fun fromName(name: String?): NewsSound =
            entries.firstOrNull { it.name == name } ?: DEFAULT_SOUND
    }
}
