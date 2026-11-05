package net.sigmabeta.sage.sample.feature.listitems

import net.sigmabeta.sage.ui.SageStringId

/**
 * Every string the List Items feature renders.
 *
 * As with [net.sigmabeta.sage.sample.feature.welcome.WelcomeStringId], the app module owns the
 * id → `R.string` mapping; this module never touches Android resources.
 */
enum class ListItemsStringId : SageStringId {
    LIST_ITEMS_TITLE,
    LIST_ITEMS_ITEM_CLICKED,
}
