package net.sigmabeta.sage.sample.feature.search
import net.sigmabeta.sage.sample.models.ContentArtworkSize
import net.sigmabeta.sage.sample.models.ContentId

import net.sigmabeta.sage.appcomm.SageAction

/**
 * Actions the Search screen's result rows carry.
 *
 * Query entry and clearing use SAGE's shared [SageAction.SearchQueryEntered] and
 * [SageAction.SearchClearClicked]. A result tap is feature
 * vocabulary, so the sample defines it here; [ResultClicked.contentId] is the stable address of
 * the detail screen the row opens, and [ResultClicked.artwork] is the size of the thumbnail the
 * row showed, so the detail can start from that cached bitmap.
 */
sealed class SearchAction : SageAction() {
    data class ResultClicked(
        val contentId: ContentId,
        val artwork: ContentArtworkSize? = null,
    ) : SearchAction()

    /** A recent-search row: refill the field, then let the normal debounce submit it. */
    data class HistoryClicked(val query: String) : SearchAction()

    /** A recent-search row's remove button. */
    data class HistoryRemoved(val query: String) : SearchAction()
}
