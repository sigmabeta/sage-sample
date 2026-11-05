package net.sigmabeta.sage.sample.feature.settings

import net.sigmabeta.sage.ui.SageStringId

/**
 * Every string the Settings feature renders.
 *
 * As with [net.sigmabeta.sage.sample.feature.welcome.WelcomeStringId], the app module owns the
 * id → `R.string` mapping; this module never touches Android resources.
 */
enum class SettingsStringId : SageStringId {
    SETTINGS_TITLE,
    SETTINGS_TODO,
}
