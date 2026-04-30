package net.sigmabeta.sage.sample.feature.welcome

import net.sigmabeta.sage.appcomm.VglsAction
import net.sigmabeta.sage.components.ListModel
import net.sigmabeta.sage.components.SectionHeaderListModel
import net.sigmabeta.sage.components.SingleTextListModel
import net.sigmabeta.sage.components.TitleBarModel
import net.sigmabeta.sage.list.ListState
import net.sigmabeta.sage.ui.StringProvider

data class WelcomeState(
    val placeholder: Unit = Unit,
) : ListState() {
    override fun title(stringProvider: StringProvider) = TitleBarModel(
        title = "SAGE Sample",
        shouldShowBack = false,
    )

    override fun toListItems(stringProvider: StringProvider): List<ListModel> = listOf(
        SectionHeaderListModel(title = "About"),
        SingleTextListModel(
            name = "This is a sample app demonstrating SAGE consumed via Gradle composite build.",
            clickAction = VglsAction.Noop,
            dataId = 1L,
        ),
        SingleTextListModel(
            name = "Each feature is a pure JVM module that produces a ListState rendered by SAGE's ListScreen.",
            clickAction = VglsAction.Noop,
            dataId = 2L,
        ),
    )
}
