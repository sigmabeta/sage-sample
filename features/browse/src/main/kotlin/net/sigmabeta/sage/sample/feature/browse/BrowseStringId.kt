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
    BROWSE_OPEN_LIST_ITEMS,
    BROWSE_OPEN_LIST_ITEMS_WITH_IMAGES,
    BROWSE_DEMO_ITEM_LISTS,
    BROWSE_DEMO_ITEM_METRO,
    BROWSE_DEMO_ITEM_VOYAGER,
}
