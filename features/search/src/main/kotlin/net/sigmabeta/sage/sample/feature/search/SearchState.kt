package net.sigmabeta.sage.sample.feature.search

import net.sigmabeta.sage.appcomm.SageAction
import net.sigmabeta.sage.components.ListModel
import net.sigmabeta.sage.components.SingleTextListModel
import net.sigmabeta.sage.components.TitleBarModel
import net.sigmabeta.sage.list.ListState
import net.sigmabeta.sage.ui.StringProvider

/**
 * Search's content, declared as a SAGE [ListState].
 *
 * TODO: add the query field and result rows once search exists. SAGE already ships a
 * `SearchResultListModel` and `SageAction.SearchQueryEntered` for that work.
 */
object SearchState : ListState() {
    override fun title(stringProvider: StringProvider) =
        TitleBarModel(
            title = stringProvider.getString(SearchStringId.SEARCH_TITLE),
            shouldShowBack = false,
        )

    override fun toListItems(stringProvider: StringProvider): List<ListModel> =
        listOf(
            SingleTextListModel(
                name = stringProvider.getString(SearchStringId.SEARCH_TODO),
                clickAction = SageAction.Noop,
                dataId = 1L,
            ),
        )
}
