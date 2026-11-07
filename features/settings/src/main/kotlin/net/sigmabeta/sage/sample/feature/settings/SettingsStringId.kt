package net.sigmabeta.sage.sample.feature.settings

import net.sigmabeta.sage.ui.SageStringId

/**
 * Every string the Settings feature renders.
 *
 * As with [net.sigmabeta.sage.sample.feature.home.HomeStringId], the app module owns the
 * id → `R.string` mapping; this module never touches Android resources.
 */
enum class SettingsStringId : SageStringId {
    SETTINGS_TITLE,

    SETTINGS_SECTION_APPEARANCE,
    SETTINGS_LABEL_THEME,
    SETTINGS_THEME_LIGHT,
    SETTINGS_THEME_DARK,
    SETTINGS_THEME_SYSTEM,
    SETTINGS_LABEL_FONT,
    SETTINGS_FONT_STANDARD,
    SETTINGS_FONT_INTER,

    SETTINGS_SECTION_SIMULATION,
    SETTINGS_LABEL_SIMULATE_DELAY,
    SETTINGS_DELAY_NONE,
    SETTINGS_DELAY_ONE_SECOND,
    SETTINGS_DELAY_TWO_SECONDS,
    SETTINGS_DELAY_FIVE_SECONDS,
    SETTINGS_LABEL_HOME_SHOWS_SAME_CONTENT,

    SETTINGS_SECTION_ABOUT,
    SETTINGS_LABEL_APP_VERSION,
    SETTINGS_LABEL_BUILD_DATE,
}
