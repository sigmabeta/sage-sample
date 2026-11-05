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
}
