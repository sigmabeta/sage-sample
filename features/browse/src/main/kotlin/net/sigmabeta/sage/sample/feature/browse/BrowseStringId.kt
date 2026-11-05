package net.sigmabeta.sage.sample.feature.browse

import net.sigmabeta.sage.ui.SageStringId

/**
 * Every string the Browse feature renders.
 *
 * As with [net.sigmabeta.sage.sample.feature.welcome.WelcomeStringId], the app module owns the
 * id → `R.string` mapping; this module never touches Android resources.
 */
enum class BrowseStringId : SageStringId {
    BROWSE_TITLE,
    BROWSE_EMPTY,
    BROWSE_DEMO_HEADER,
    BROWSE_DEMO_CONTENT,
    BROWSE_DEMO_EMPTY,
    BROWSE_DEMO_ERROR,
}
