package net.sigmabeta.sage.sample.feature.listitems

import net.sigmabeta.sage.appcomm.LCE
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
 * same 1:1 shape as the [GridImageListModel] cells they are replaced by.
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
            is LCE.Error -> error(GRID_OF_IMAGES_OPERATION_NAME, loaded.error)

            else -> loaded.withStandardErrorAndLoading(
                loadingType = LoadingType.SQUARE,
                loadingWithHeader = false,
                loadingOperationNameOverride = GRID_OF_IMAGES_OPERATION_NAME,
            ) {
                data.map { item ->
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
                    )
                }
            }
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
