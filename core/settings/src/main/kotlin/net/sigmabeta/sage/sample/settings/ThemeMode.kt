package net.sigmabeta.sage.sample.settings

/**
 * User-selectable app theme.
 *
 * [SYSTEM] follows the OS light/dark setting and is the default; [LIGHT] and [DARK] pin one color
 * scheme regardless of the system setting.
 */
enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM,
    ;

    /**
     * Whether this choice renders dark, given the current OS setting.
     *
     * [SYSTEM] is the only entry that reads [systemDark]; the others override it.
     */
    fun isDark(systemDark: Boolean): Boolean = when (this) {
        LIGHT -> false
        DARK -> true
        SYSTEM -> systemDark
    }

    companion object {
        val DEFAULT: ThemeMode = SYSTEM

        /** Parses a persisted [name] back into a [ThemeMode], falling back to [DEFAULT]. */
        fun fromStorageValue(value: String?): ThemeMode =
            entries.firstOrNull { it.name == value } ?: DEFAULT
    }
}
