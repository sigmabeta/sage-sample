package net.sigmabeta.sage.sample.feature.listitems

import net.sigmabeta.sage.appcomm.LCE
import net.sigmabeta.sage.components.EmptyStateListModel
import net.sigmabeta.sage.components.ErrorStateListModel
import net.sigmabeta.sage.components.GridImageListModel
import net.sigmabeta.sage.components.ListModel
import net.sigmabeta.sage.components.LoadingItemListModel
import net.sigmabeta.sage.components.LoadingType
import net.sigmabeta.sage.components.TitleBarModel
import net.sigmabeta.sage.list.ColumnType
import net.sigmabeta.sage.list.ListState
import net.sigmabeta.sage.list.PaginationType
import net.sigmabeta.sage.ui.Icon
import net.sigmabeta.sage.ui.StringProvider

/**
 * Grid of Images' content, declared as a SAGE [ListState].
 *
 * It maps the same generated [SampleItem] data as the List Items screens to
 * [GridImageListModel]s. The [ColumnType.Regular] declaration is what makes the shared screen
 * choose SAGE's `GridScreen`: 160 dp cells give two columns on a compact window and more as it
 * widens.
 *
 * The grid is a [PaginationType.Standard] window: [windowStart] is the absolute index of
 * [content]'s first cell, and [loadingMore]/[loadingPrevious] add an inline boundary loader while a
 * page is in flight. Loaders only appear beside non-empty content, so an initial empty result still
 * renders the one localized [EmptyStateListModel] and a failed load still renders the one localized
 * [ErrorStateListModel] naming the operation.
 */
data class GridOfImagesState(
    val content: LCE<List<SampleItem>> = LCE.Loading(
        GRID_OF_IMAGES_OPERATION_NAME,
    ),
    /** Absolute index of [content]'s first cell within the catalogue. */
    val windowStart: Int = 0,
    /** Whether a page may still exist below the current window. */
    val hasMoreAfter: Boolean = true,
    /** Whether the next page is in flight; renders a footer loader beside existing cells. */
    val loadingMore: Boolean = false,
    /** Whether the previous page is in flight; renders a header loader beside existing cells. */
    val loadingPrevious: Boolean = false,
) : ListState() {
    override val columnType = ColumnType.Regular(GRID_OF_IMAGES_CELL_WIDTH_DP)

    override val paginationType: PaginationType = PaginationType.Standard(
        loadingType = LoadingType.SQUARE,
        pageSize = GRID_OF_IMAGES_PAGE_SIZE,
    )

    override fun title(stringProvider: StringProvider) =
        TitleBarModel(
            title = stringProvider.getString(ListItemsStringId.GRID_OF_IMAGES_TITLE),
            shouldShowBack = true,
        )

    override fun toListItems(stringProvider: StringProvider): List<ListModel> =
        when (val loaded = content) {
            is LCE.Error -> listOf(
                ErrorStateListModel(
                    failedOperationName = GRID_OF_IMAGES_OPERATION_NAME,
                    errorString = stringProvider.getString(ListItemsStringId.GRID_OF_IMAGES_ERROR),
                    error = loaded.error,
                ),
            )

            is LCE.Content -> if (loaded.data.isEmpty()) {
                listOf(emptyState(stringProvider))
            } else {
                buildList {
                    if (loadingPrevious) add(pageLoader(LOAD_PREVIOUS_OPERATION))
                    loaded.data.forEach { item ->
                        add(
                            GridImageListModel(
                                dataId = item.id,
                                name = item.name,
                                sourceInfo = imageSourceFor(
                                    item = item,
                                    seedPrefix = GRID_OF_IMAGES_SEED_PREFIX,
                                    imageSize = GRID_OF_IMAGES_IMAGE_SIZE,
                                ),
                                imagePlaceholder = Icon.Person,
                                clickAction = GridOfImagesAction.ItemClicked(item),
                            ),
                        )
                    }
                    if (loadingMore) add(pageLoader(LOAD_MORE_OPERATION))
                }
            }

            is LCE.Loading,
            LCE.Uninitialized -> loading(
                operationName = GRID_OF_IMAGES_OPERATION_NAME,
                loadingType = LoadingType.SQUARE,
                loadingItemCount = LOADING_ITEM_COUNT,
            )
        }

    private fun pageLoader(operationName: String) = LoadingItemListModel(
        loadingType = LoadingType.SQUARE,
        loadOperationName = operationName,
        loadPositionOffset = 0,
    )

    private fun emptyState(stringProvider: StringProvider) = EmptyStateListModel(
        icon = Icon.Album,
        explanation = stringProvider.getString(ListItemsStringId.GRID_OF_IMAGES_EMPTY),
        showCrossOut = false,
    )

    private companion object {
        const val LOADING_ITEM_COUNT = 10
    }
}

/** Cells fetched per page by Grid of Images' standard paging window. */
const val GRID_OF_IMAGES_PAGE_SIZE = 20

private const val LOAD_MORE_OPERATION = "$GRID_OF_IMAGES_OPERATION_NAME.load_more"
private const val LOAD_PREVIOUS_OPERATION = "$GRID_OF_IMAGES_OPERATION_NAME.load_previous"

private const val GRID_OF_IMAGES_SEED_PREFIX = "grid-of-images"

/**
 * The generated bitmap's pixel size. It is larger than a 160 dp grid cell so the image stays
 * sharp on high-density screens; the size is not part of the seed.
 */
private const val GRID_OF_IMAGES_IMAGE_SIZE = 256

/** The preferred grid cell width; SAGE divides the window's width class by this. */
private const val GRID_OF_IMAGES_CELL_WIDTH_DP = 160
