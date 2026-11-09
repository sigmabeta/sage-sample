package net.sigmabeta.sage.sample.feature.listitems
import net.sigmabeta.sage.sample.strings.api.SampleStringId
import net.sigmabeta.sage.sample.models.SampleItem

import net.sigmabeta.sage.appcomm.LCE
import net.sigmabeta.sage.components.EmptyStateListModel
import net.sigmabeta.sage.components.ErrorStateListModel
import net.sigmabeta.sage.components.ListModel
import net.sigmabeta.sage.components.LoadingType
import net.sigmabeta.sage.components.NameCaptionListModel
import net.sigmabeta.sage.components.TitleBarModel
import net.sigmabeta.sage.list.ListState
import net.sigmabeta.sage.ui.Icon
import net.sigmabeta.sage.ui.StringProvider
import net.sigmabeta.sage.sample.repository.LIST_ITEMS_OPERATION_NAME

/**
 * List Items' content, declared as a SAGE [ListState].
 *
 * The loaded content is an [LCE]. Loading uses [LoadingType.TEXT_CAPTION] placeholder rows with
 * the same text-and-caption shape as the [NameCaptionListModel] rows they are replaced by. A
 * successful but empty result renders one localized [EmptyStateListModel], and a failed load
 * renders one localized [ErrorStateListModel] naming the operation; neither falls through to a
 * blank list. The error row is built directly because SAGE's `error` helper hardcodes English
 * copy.
 */
data class ListItemsState(
    val content: LCE<List<SampleItem>> = LCE.Loading(LIST_ITEMS_OPERATION_NAME),
) : ListState() {
    override fun title(stringProvider: StringProvider) =
        TitleBarModel(
            title = stringProvider.getString(SampleStringId.LIST_ITEMS_TITLE),
            shouldShowBack = true,
        )

    override fun toListItems(stringProvider: StringProvider): List<ListModel> =
        when (val loaded = content) {
            is LCE.Error -> listOf(
                ErrorStateListModel(
                    failedOperationName = LIST_ITEMS_OPERATION_NAME,
                    errorString = stringProvider.getString(SampleStringId.LIST_ITEMS_ERROR),
                    error = loaded.error,
                ),
            )

            is LCE.Content -> if (loaded.data.isEmpty()) {
                listOf(emptyState(stringProvider))
            } else {
                loaded.data.map { item ->
                    NameCaptionListModel(
                        dataId = item.id,
                        name = item.name,
                        caption = item.caption,
                        clickAction = ListItemsAction.ItemClicked(item),
                    )
                }
            }

            is LCE.Loading,
            LCE.Uninitialized -> loading(
                operationName = LIST_ITEMS_OPERATION_NAME,
                loadingType = LoadingType.TEXT_CAPTION,
                loadingItemCount = LOADING_ITEM_COUNT,
            )
        }

    private fun emptyState(stringProvider: StringProvider) = EmptyStateListModel(
        icon = Icon.Person,
        explanation = stringProvider.getString(SampleStringId.LIST_ITEMS_EMPTY),
        showCrossOut = false,
    )

    private companion object {
        const val LOADING_ITEM_COUNT = 10
    }
}
