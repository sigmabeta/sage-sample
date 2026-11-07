package net.sigmabeta.sage.sample.feature.listitems

import net.sigmabeta.sage.appcomm.LCE
import net.sigmabeta.sage.components.EmptyStateListModel
import net.sigmabeta.sage.components.ErrorStateListModel
import net.sigmabeta.sage.components.GridImageListModel
import net.sigmabeta.sage.components.ListModel
import net.sigmabeta.sage.components.LoadingType
import net.sigmabeta.sage.components.TitleBarModel
import net.sigmabeta.sage.list.ColumnType
import net.sigmabeta.sage.list.ListState
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
 * Loading rows use [LoadingType.SQUARE] with no section header, so the placeholder cells have the
 * same 1:1 shape as the [GridImageListModel] cells they are replaced by. An empty successful
 * result renders one localized [EmptyStateListModel], and a failed load renders one localized
 * [ErrorStateListModel] naming the operation; neither falls through to a blank grid.
 */
data class GridOfImagesState(
    val content: LCE<List<SampleItem>> = LCE.Loading(
        GRID_OF_IMAGES_OPERATION_NAME,
    ),
) : ListState() {
    override val columnType = ColumnType.Regular(GRID_OF_IMAGES_CELL_WIDTH_DP)

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
                loaded.data.map { item ->
                    GridImageListModel(
                        dataId = item.id,
                        name = item.name,
                        sourceInfo = imageSourceFor(
                            item = item,
                            seedPrefix = GRID_OF_IMAGES_SEED_PREFIX,
                            imageSize = GRID_OF_IMAGES_IMAGE_SIZE,
                        ),
                        imagePlaceholder = Icon.Person,
                        clickAction = GridOfImagesAction.ItemClicked(
                            gridOfImagesContentId(item.id),
                        ),
                    )
                }
            }

            is LCE.Loading,
            LCE.Uninitialized -> loading(
                operationName = GRID_OF_IMAGES_OPERATION_NAME,
                loadingType = LoadingType.SQUARE,
                loadingItemCount = LOADING_ITEM_COUNT,
            )
        }

    private fun emptyState(stringProvider: StringProvider) = EmptyStateListModel(
        icon = Icon.Album,
        explanation = stringProvider.getString(ListItemsStringId.GRID_OF_IMAGES_EMPTY),
        showCrossOut = false,
    )

    private companion object {
        const val LOADING_ITEM_COUNT = 10
    }
}

private const val GRID_OF_IMAGES_SEED_PREFIX = "grid-of-images"

/**
 * The generated bitmap's pixel size. It is larger than a 160 dp grid cell so the image stays
 * sharp on high-density screens; the size is not part of the seed.
 */
private const val GRID_OF_IMAGES_IMAGE_SIZE = 256

/** The preferred grid cell width; SAGE divides the window's width class by this. */
private const val GRID_OF_IMAGES_CELL_WIDTH_DP = 160
