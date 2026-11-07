package net.sigmabeta.sage.sample.feature.search

import net.sigmabeta.sage.appcomm.SageAction
import net.sigmabeta.sage.sample.feature.contentdetail.ContentId

/**
 * Actions the Search screen's result rows carry.
 *
 * Query entry and clearing use SAGE's shared [SageAction.SearchQueryEntered] and
 * [SageAction.SearchClearClicked], matching the other SAGE apps. A result tap is feature
 * vocabulary, so the sample defines it here; [ResultClicked.contentId] is the stable address of
 * the detail screen the row opens.
 */
sealed class SearchAction : SageAction() {
    data class ResultClicked(val contentId: ContentId) : SearchAction()

    /** A recent-search row: refill the field, then let the normal debounce submit it. */
    data class HistoryClicked(val query: String) : SearchAction()

    /** A recent-search row's remove button. */
    data class HistoryRemoved(val query: String) : SearchAction()
}
