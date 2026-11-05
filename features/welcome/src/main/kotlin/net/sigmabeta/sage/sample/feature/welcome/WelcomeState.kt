package net.sigmabeta.sage.sample.feature.welcome

import net.sigmabeta.sage.appcomm.SageAction
import net.sigmabeta.sage.components.ListModel
import net.sigmabeta.sage.components.SectionHeaderListModel
import net.sigmabeta.sage.components.SingleTextListModel
import net.sigmabeta.sage.components.TitleBarModel
import net.sigmabeta.sage.list.ListState
import net.sigmabeta.sage.ui.StringProvider

/**
 * The Welcome screen's content, declared as a SAGE [ListState].
 *
 * SAGE renders this into an immutable `ListStateActual` (applying de-duping and error
 * fallbacks); the app only supplies a composable for each [ListModel].
 */
object WelcomeState : ListState() {
    override fun title(stringProvider: StringProvider) =
        TitleBarModel(
            title = stringProvider.getString(WelcomeStringId.WELCOME_TITLE),
            shouldShowBack = false,
        )

    override fun toListItems(stringProvider: StringProvider): List<ListModel> =
        listOf(
            SingleTextListModel(
                name = stringProvider.getString(WelcomeStringId.WELCOME_OPEN_BROWSE),
                clickAction = WelcomeAction.OpenBrowse,
                dataId = OPEN_BROWSE_DATA_ID,
            ),
            SectionHeaderListModel(
                title = stringProvider.getString(WelcomeStringId.WELCOME_ABOUT_HEADER),
            ),
            SingleTextListModel(
                name = stringProvider.getString(WelcomeStringId.WELCOME_ABOUT_BODY_COMPOSITE_BUILD),
                clickAction = SageAction.Noop,
                dataId = 1L,
            ),
            SingleTextListModel(
                name = stringProvider.getString(WelcomeStringId.WELCOME_ABOUT_BODY_LIST_STATE),
                clickAction = SageAction.Noop,
                dataId = 2L,
            ),
        )

    /**
     * Stable model id for the Browse row.
     *
     * The id identifies the row across state rebuilds; it is part of the screen's contract, so
     * tests pin it rather than deriving it from the label.
     */
    const val OPEN_BROWSE_DATA_ID = 3L
}
