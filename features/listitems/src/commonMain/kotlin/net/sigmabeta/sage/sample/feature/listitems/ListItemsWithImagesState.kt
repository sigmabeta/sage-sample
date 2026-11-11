package net.sigmabeta.sage.sample.feature.listitems
import net.sigmabeta.sage.sample.strings.api.SampleStringId
import net.sigmabeta.sage.sample.models.ArtworkCorner
import net.sigmabeta.sage.sample.models.ContentArtworkSize
import net.sigmabeta.sage.sample.models.SampleItem

import net.sigmabeta.sage.appcomm.LCE
import net.sigmabeta.sage.components.EmptyStateListModel
import net.sigmabeta.sage.components.ErrorStateListModel
import net.sigmabeta.sage.components.ImageNameCaptionListModel
import net.sigmabeta.sage.components.ListModel
import net.sigmabeta.sage.components.LoadingItemListModel
import net.sigmabeta.sage.components.LoadingType
import net.sigmabeta.sage.components.TitleBarModel
import net.sigmabeta.sage.images.SourceInfo
import net.sigmabeta.sage.list.ListState
import net.sigmabeta.sage.list.PaginationType
import net.sigmabeta.sage.sample.bitmaps.GeneratedImageFetcher
import net.sigmabeta.sage.ui.Icon
import net.sigmabeta.sage.ui.StringProvider

/**
 * List Items with Images' content, declared as a SAGE [ListState].
 *
 * The rows carry the same generated [SampleItem] data as List Items, but each one renders as an
 * [ImageNameCaptionListModel]. Its [SourceInfo] is a `generated://` URL: `:core:bitmaps`'
 * Coil fetcher turns that URL into deterministic artwork, so the same item always shows the same
 * image without shipping any bitmap assets.
 *
 * The screen is a [PaginationType.Standard] window: [windowStart] is the absolute index of
 * [content]'s first row, and [loadingMore]/[loadingPrevious] add an inline boundary loader while a
 * page is in flight. Loaders only appear beside non-empty content, so an initial empty result still
 * renders the one localized [EmptyStateListModel] and a failed load still renders the one localized
 * [ErrorStateListModel] naming the operation.
 */
data class ListItemsWithImagesState(
    val content: LCE<List<SampleItem>> = LCE.Loading(
        LIST_ITEMS_WITH_IMAGES_OPERATION_NAME,
    ),
    /** Absolute index of [content]'s first row within the catalogue. */
    val windowStart: Int = 0,
    /** Whether a page may still exist below the current window. */
    val hasMoreAfter: Boolean = true,
    /** Whether the next page is in flight; renders a footer loader beside existing rows. */
    val loadingMore: Boolean = false,
    /** Whether the previous page is in flight; renders a header loader beside existing rows. */
    val loadingPrevious: Boolean = false,
) : ListState() {
    override val paginationType: PaginationType = PaginationType.Standard(
        loadingType = LoadingType.TEXT_CAPTION_IMAGE,
        pageSize = LIST_ITEMS_WITH_IMAGES_PAGE_SIZE,
    )

    override fun title(stringProvider: StringProvider) =
        TitleBarModel(
            title = stringProvider.getString(SampleStringId.LIST_ITEMS_WITH_IMAGES_TITLE),
            shouldShowBack = true,
        )

    override fun toListItems(stringProvider: StringProvider): List<ListModel> =
        when (val loaded = content) {
            is LCE.Error -> listOf(
                ErrorStateListModel(
                    failedOperationName = LIST_ITEMS_WITH_IMAGES_OPERATION_NAME,
                    errorString = stringProvider.getString(
                        SampleStringId.LIST_ITEMS_WITH_IMAGES_ERROR,
                    ),
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
                            ImageNameCaptionListModel(
                                dataId = item.id,
                                name = item.name,
                                caption = item.caption,
                                sourceInfo = imageSourceFor(item),
                                imagePlaceholder = Icon.Person,
                                clickAction = ListItemsWithImagesAction.ItemClicked(
                                    item = item,
                                    artwork = ContentArtworkSize(
                                        width = LIST_ITEMS_WITH_IMAGES_IMAGE_SIZE,
                                        height = LIST_ITEMS_WITH_IMAGES_IMAGE_SIZE,
                                        // The row's thumbnail is a circle.
                                        corner = ArtworkCorner.Circle,
                                    ),
                                ),
                            ),
                        )
                    }
                    if (loadingMore) add(pageLoader(LOAD_MORE_OPERATION))
                }
            }

            is LCE.Loading,
            LCE.Uninitialized -> loading(
                operationName = LIST_ITEMS_WITH_IMAGES_OPERATION_NAME,
                loadingType = LoadingType.TEXT_CAPTION_IMAGE,
                loadingItemCount = LOADING_ITEM_COUNT,
            )
        }

    private fun pageLoader(operationName: String) = LoadingItemListModel(
        loadingType = LoadingType.TEXT_CAPTION_IMAGE,
        loadOperationName = operationName,
        loadPositionOffset = 0,
    )

    private fun emptyState(stringProvider: StringProvider) = EmptyStateListModel(
        icon = Icon.Person,
        explanation = stringProvider.getString(SampleStringId.LIST_ITEMS_WITH_IMAGES_EMPTY),
        showCrossOut = false,
    )

    private companion object {
        const val LOADING_ITEM_COUNT = 10
    }
}

/**
 * The deterministic image request for [item].
 *
 * The URL's path is the seed and its query only selects the rendered size, so the artwork is
 * stable across app runs and independent of the image's display size. [seedPrefix] keeps one
 * screen's seeds distinct from any other `generated://` artwork in the app.
 */
internal fun imageSourceFor(
    item: SampleItem,
    seedPrefix: String = LIST_ITEMS_WITH_IMAGES_SEED_PREFIX,
    imageSize: Int = LIST_ITEMS_WITH_IMAGES_IMAGE_SIZE,
): SourceInfo = SourceInfo(
    GeneratedImageFetcher.urlFor(
        seed = "$seedPrefix/${item.id}",
        width = imageSize,
        height = imageSize,
    ),
)

/** API operation name for List Items with Images' LCE loading and error rows. */
const val LIST_ITEMS_WITH_IMAGES_OPERATION_NAME = "list-items-with-images"

/** Rows fetched per page by List Items with Images' standard paging window. */
const val LIST_ITEMS_WITH_IMAGES_PAGE_SIZE = 10

private const val LOAD_MORE_OPERATION = "$LIST_ITEMS_WITH_IMAGES_OPERATION_NAME.load_more"
private const val LOAD_PREVIOUS_OPERATION = "$LIST_ITEMS_WITH_IMAGES_OPERATION_NAME.load_previous"

private const val LIST_ITEMS_WITH_IMAGES_SEED_PREFIX = "list-items-with-images"

/**
 * The generated bitmap's pixel size. It is larger than the 56 dp row thumbnail so the image stays
 * sharp on high-density screens; the size is not part of the seed.
 */
private const val LIST_ITEMS_WITH_IMAGES_IMAGE_SIZE = 128
