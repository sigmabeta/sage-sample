package net.sigmabeta.sage.sample.feature.search

import net.sigmabeta.sage.ui.SageStringId

/**
 * Every string the Search feature renders.
 *
 * As with [net.sigmabeta.sage.sample.feature.home.HomeStringId], the app module owns the
 * id → `R.string` mapping; this module never touches Android resources.
 */
enum class SearchStringId : SageStringId {
    SEARCH_TITLE,
    SEARCH_HINT,
    SEARCH_CLEAR_DESCRIPTION,
    SEARCH_BACK_DESCRIPTION,
    SEARCH_PROMPT,
    SEARCH_NO_RESULTS,
    SEARCH_RESULT_CLICKED,
    SEARCH_SECTION_LIST,
    SEARCH_SECTION_GRID,
}
