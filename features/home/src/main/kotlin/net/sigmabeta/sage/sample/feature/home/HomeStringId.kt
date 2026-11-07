package net.sigmabeta.sage.sample.feature.home

import net.sigmabeta.sage.ui.SageStringId

/**
 * Every string the Home feature renders.
 *
 * As with the other sample features, the app module owns the id → `R.string` mapping; this
 * module never touches Android resources.
 */
enum class HomeStringId : SageStringId {
    HOME_TITLE,
    HOME_SECTION_BIG_IMAGE,
    HOME_SECTION_GRID_IMAGES,
    HOME_EMPTY,
    HOME_ALL_ERROR,
}
