package net.sigmabeta.sage.sample.feature.browse
import net.sigmabeta.sage.sample.strings.api.SampleStringId

import net.sigmabeta.sage.components.ListModel
import net.sigmabeta.sage.components.SingleTextListModel
import net.sigmabeta.sage.components.TitleBarModel
import net.sigmabeta.sage.list.ListState
import net.sigmabeta.sage.ui.StringProvider

/**
 * Browse's content, declared as a SAGE [ListState].
 *
 * Browse is a static destination list: it exists to open the List Items and Content detail
 * screens and has no asynchronous content of its own. Keeping it load-free means the destinations
 * render immediately, with no loading placeholder or inactive demo rows to wait through or skip
 * past.
 */
data object BrowseState : ListState() {
    override fun title(stringProvider: StringProvider) =
        TitleBarModel(
            title = stringProvider.getString(SampleStringId.BROWSE_TITLE),
            shouldShowBack = false,
        )

    override fun toListItems(stringProvider: StringProvider): List<ListModel> =
        listOf(
            openListItemsRow(stringProvider),
            openListItemsWithImagesRow(stringProvider),
            openGridOfImagesRow(stringProvider),
            openContentDetailRow(stringProvider),
        )

    /** The app-side List Items destination, reached through [BrowseAction.OpenListItems]. */
    private fun openListItemsRow(stringProvider: StringProvider): ListModel =
        SingleTextListModel(
            name = stringProvider.getString(SampleStringId.BROWSE_OPEN_LIST_ITEMS),
            clickAction = BrowseAction.OpenListItems,
            dataId = OPEN_LIST_ITEMS_DATA_ID,
        )

    /** The image-bearing variant of List Items. */
    private fun openListItemsWithImagesRow(stringProvider: StringProvider): ListModel =
        SingleTextListModel(
            name = stringProvider.getString(SampleStringId.BROWSE_OPEN_LIST_ITEMS_WITH_IMAGES),
            clickAction = BrowseAction.OpenListItemsWithImages,
            dataId = OPEN_LIST_ITEMS_WITH_IMAGES_DATA_ID,
        )

    /** The 50-image grid. */
    private fun openGridOfImagesRow(stringProvider: StringProvider): ListModel =
        SingleTextListModel(
            name = stringProvider.getString(SampleStringId.BROWSE_OPEN_GRID_OF_IMAGES),
            clickAction = BrowseAction.OpenGridOfImages,
            dataId = OPEN_GRID_OF_IMAGES_DATA_ID,
        )

    /** The Chipbox-style detail screen over generated content. */
    private fun openContentDetailRow(stringProvider: StringProvider): ListModel =
        SingleTextListModel(
            name = stringProvider.getString(SampleStringId.BROWSE_OPEN_CONTENT_DETAIL),
            clickAction = BrowseAction.OpenContentDetail,
            dataId = OPEN_CONTENT_DETAIL_DATA_ID,
        )

    /** Stable model id for the List Items row; it is part of the screen's contract. */
    const val OPEN_LIST_ITEMS_DATA_ID = 100L

    /** Stable model id for the List Items with Images row. */
    const val OPEN_LIST_ITEMS_WITH_IMAGES_DATA_ID = 101L

    /** Stable model id for the Grid of Images row. */
    const val OPEN_GRID_OF_IMAGES_DATA_ID = 102L

    /** Stable model id for the Content detail row. */
    const val OPEN_CONTENT_DETAIL_DATA_ID = 103L
}
