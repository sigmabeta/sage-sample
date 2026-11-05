package net.sigmabeta.sage.sample.feature.browse

import net.sigmabeta.sage.appcomm.LCE
import net.sigmabeta.sage.appcomm.SageAction
import net.sigmabeta.sage.components.EmptyStateListModel
import net.sigmabeta.sage.components.ListModel
import net.sigmabeta.sage.components.SectionHeaderListModel
import net.sigmabeta.sage.components.SingleTextListModel
import net.sigmabeta.sage.components.TitleBarModel
import net.sigmabeta.sage.list.ListState
import net.sigmabeta.sage.ui.Icon
import net.sigmabeta.sage.ui.StringProvider

/**
 * Browse's content, declared as a SAGE [ListState].
 *
 * The loaded content is an [LCE]: SAGE's `withStandardErrorAndLoading` helper supplies the loading
 * presentation, and errors use the state's own `ErrorStateListModel` helper (SAGE's helper Error
 * branch throws). The three demo rows at the bottom let a user switch outcomes; they are chrome,
 * not loaded data, so they stay visible in every state.
 */
data class BrowseState(
    val content: LCE<List<BrowseItem>> = LCE.Loading(BROWSE_OPERATION_NAME),
) : ListState() {
    override fun title(stringProvider: StringProvider) =
        TitleBarModel(
            title = stringProvider.getString(BrowseStringId.BROWSE_TITLE),
            shouldShowBack = false,
        )

    override fun toListItems(stringProvider: StringProvider): List<ListModel> {
        val contentItems = when (val loaded = content) {
            // SAGE's withStandardErrorAndLoading Error branch calls stdlib error(...) and throws
            // (ListState.kt), so route errors through the class's own ErrorStateListModel helper.
            is LCE.Error -> error(BROWSE_OPERATION_NAME, loaded.error)

            else -> loaded.withStandardErrorAndLoading(
                loadingOperationNameOverride = BROWSE_OPERATION_NAME,
            ) {
                if (data.isEmpty()) {
                    listOf(
                        EmptyStateListModel(
                            icon = Icon.Browse,
                            explanation = stringProvider.getString(BrowseStringId.BROWSE_EMPTY),
                        ),
                    )
                } else {
                    data.map { item ->
                        SingleTextListModel(
                            name = item.name,
                            clickAction = SageAction.Noop,
                            dataId = item.id,
                        )
                    }
                }
            }
        }

        return contentItems + demoOutcomeRows(stringProvider)
    }

    private fun demoOutcomeRows(stringProvider: StringProvider): List<ListModel> =
        listOf(
            SectionHeaderListModel(
                title = stringProvider.getString(BrowseStringId.BROWSE_DEMO_HEADER),
            ),
            SingleTextListModel(
                name = stringProvider.getString(BrowseStringId.BROWSE_DEMO_CONTENT),
                clickAction = BrowseAction.LoadContent,
                dataId = DEMO_CONTENT_ROW_ID,
            ),
            SingleTextListModel(
                name = stringProvider.getString(BrowseStringId.BROWSE_DEMO_EMPTY),
                clickAction = BrowseAction.LoadEmpty,
                dataId = DEMO_EMPTY_ROW_ID,
            ),
            SingleTextListModel(
                name = stringProvider.getString(BrowseStringId.BROWSE_DEMO_ERROR),
                clickAction = BrowseAction.LoadError,
                dataId = DEMO_ERROR_ROW_ID,
            ),
        )

    private companion object {
        // Demo chrome ids live far from realistic item ids so they cannot collide with content.
        const val DEMO_CONTENT_ROW_ID = 1_000_001L
        const val DEMO_EMPTY_ROW_ID = 1_000_002L
        const val DEMO_ERROR_ROW_ID = 1_000_003L
    }
}
