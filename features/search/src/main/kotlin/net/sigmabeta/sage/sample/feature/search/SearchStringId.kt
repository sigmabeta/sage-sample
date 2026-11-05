package net.sigmabeta.sage.sample.feature.search

import net.sigmabeta.sage.ui.SageStringId

/**
 * Every string the Search feature renders.
 *
 * As with [net.sigmabeta.sage.sample.feature.welcome.WelcomeStringId], the app module owns the
 * id → `R.string` mapping; this module never touches Android resources.
 */
enum class SearchStringId : SageStringId {
    SEARCH_TITLE,
    SEARCH_TODO,
}
