package net.sigmabeta.sage.sample.feature.welcome

import net.sigmabeta.sage.ui.SageStringId

/**
 * Every string the Welcome feature renders.
 *
 * Features never touch `R.string`: they expose an id enum like this one and read text through
 * SAGE's [net.sigmabeta.sage.ui.StringProvider]. The app module owns the id → resource mapping
 * (see `SampleBindings`), so this module stays platform-agnostic and unit-testable on the JVM.
 */
enum class WelcomeStringId : SageStringId {
    APP_NAME,
    WELCOME_TITLE,
    WELCOME_ABOUT_HEADER,
    WELCOME_ABOUT_BODY_COMPOSITE_BUILD,
    WELCOME_ABOUT_BODY_LIST_STATE,
}
