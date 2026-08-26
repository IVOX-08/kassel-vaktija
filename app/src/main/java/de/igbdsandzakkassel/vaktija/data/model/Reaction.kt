package de.igbdsandzakkassel.vaktija.data.model

/** How a reader answered an announcement. */
enum class Reaction {
    LIKE,
    DISLIKE,
    ;

    companion object {
        /** Anything unrecognised counts as no reaction rather than guessing at one. */
        fun from(raw: String?): Reaction? =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) }
    }
}
