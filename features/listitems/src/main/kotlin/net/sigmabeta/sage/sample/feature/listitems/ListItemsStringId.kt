package net.sigmabeta.sage.sample.feature.listitems

import net.sigmabeta.sage.ui.SageStringId

/**
 * Every string the List Items feature renders.
 *
 * As with [net.sigmabeta.sage.sample.feature.home.HomeStringId], the app module owns the
 * id → `R.string` mapping; this module never touches Android resources.
 */
enum class ListItemsStringId : SageStringId {
    LIST_ITEMS_TITLE,
    LIST_ITEMS_ITEM_CLICKED,
    LIST_ITEMS_EMPTY,
    LIST_ITEMS_ERROR,
    LIST_ITEMS_WITH_IMAGES_TITLE,
    LIST_ITEMS_WITH_IMAGES_ITEM_CLICKED,
    LIST_ITEMS_WITH_IMAGES_EMPTY,
    LIST_ITEMS_WITH_IMAGES_ERROR,
    GRID_OF_IMAGES_TITLE,
    GRID_OF_IMAGES_ITEM_CLICKED,
    GRID_OF_IMAGES_EMPTY,
    GRID_OF_IMAGES_ERROR,
}
