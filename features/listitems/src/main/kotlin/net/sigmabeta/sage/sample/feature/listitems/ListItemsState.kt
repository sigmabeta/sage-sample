package net.sigmabeta.sage.sample.feature.listitems

import net.sigmabeta.sage.appcomm.LCE
import net.sigmabeta.sage.components.ListModel
import net.sigmabeta.sage.components.LoadingType
import net.sigmabeta.sage.components.NameCaptionListModel
import net.sigmabeta.sage.components.TitleBarModel
import net.sigmabeta.sage.list.ListState
import net.sigmabeta.sage.ui.StringProvider

/**
 * List Items' content, declared as a SAGE [ListState].
 *
 * The loaded content is an [LCE]: SAGE's `withStandardErrorAndLoading` helper supplies the loading
 * presentation, and errors use the state's own `ErrorStateListModel` helper. The loading rows use
 * [LoadingType.TEXT_CAPTION] and no section header, so the placeholder has the same text-and-caption
 * shape as the [NameCaptionListModel] rows it is replaced by.
 */
data class ListItemsState(
    val content: LCE<List<SampleItem>> = LCE.Loading(LIST_ITEMS_OPERATION_NAME),
) : ListState() {
    override fun title(stringProvider: StringProvider) =
        TitleBarModel(
            title = stringProvider.getString(ListItemsStringId.LIST_ITEMS_TITLE),
            shouldShowBack = true,
        )

    override fun toListItems(stringProvider: StringProvider): List<ListModel> =
        when (val loaded = content) {
            is LCE.Error -> error(LIST_ITEMS_OPERATION_NAME, loaded.error)

            else -> loaded.withStandardErrorAndLoading(
                loadingType = LoadingType.TEXT_CAPTION,
                loadingWithHeader = false,
                loadingOperationNameOverride = LIST_ITEMS_OPERATION_NAME,
            ) {
                data.map { item ->
                    NameCaptionListModel(
                        dataId = item.id,
                        name = item.name,
                        caption = item.caption,
                        clickAction = ListItemsAction.ItemClicked(item),
                    )
                }
            }
        }
}
