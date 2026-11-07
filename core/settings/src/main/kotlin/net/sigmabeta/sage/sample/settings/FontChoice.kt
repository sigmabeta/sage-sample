package net.sigmabeta.sage.sample.settings

/**
 * The app-wide typeface.
 *
 * [STANDARD] keeps the platform's default family; [INTER] uses the bundled Inter family. The
 * preference module stays Compose-free, so each app maps a choice to a Compose `FontFamily` where
 * it owns the font resources.
 */
enum class FontChoice {
    STANDARD,
    INTER,
    ;

    companion object {
        val DEFAULT: FontChoice = STANDARD

        /** Parses a persisted [name] back into a [FontChoice], falling back to [DEFAULT]. */
        fun fromStorageValue(value: String?): FontChoice =
            entries.firstOrNull { it.name == value } ?: DEFAULT
    }
}
