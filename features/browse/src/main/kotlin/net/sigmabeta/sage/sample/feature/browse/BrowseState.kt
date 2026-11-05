package net.sigmabeta.sage.sample.feature.browse

import net.sigmabeta.sage.appcomm.SageAction
import net.sigmabeta.sage.components.ListModel
import net.sigmabeta.sage.components.SingleTextListModel
import net.sigmabeta.sage.components.TitleBarModel
import net.sigmabeta.sage.list.ListState
import net.sigmabeta.sage.ui.StringProvider

/**
 * Browse's content, declared as a SAGE [ListState].
 *
 * TODO: replace the placeholder row with the real browse content once it exists.
 */
object BrowseState : ListState() {
    override fun title(stringProvider: StringProvider) =
        TitleBarModel(
            title = stringProvider.getString(BrowseStringId.BROWSE_TITLE),
            shouldShowBack = false,
        )

    override fun toListItems(stringProvider: StringProvider): List<ListModel> =
        listOf(
            SingleTextListModel(
                name = stringProvider.getString(BrowseStringId.BROWSE_TODO),
                clickAction = SageAction.Noop,
                dataId = 1L,
            ),
        )
}
