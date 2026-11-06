package net.sigmabeta.sage.sample.feature.listitems

import net.sigmabeta.sage.appcomm.LCE
import net.sigmabeta.sage.components.ImageNameCaptionListModel
import net.sigmabeta.sage.components.ListModel
import net.sigmabeta.sage.components.TitleBarModel
import net.sigmabeta.sage.images.SourceInfo
import net.sigmabeta.sage.list.ListState
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
 */
data class ListItemsWithImagesState(
    val content: LCE<List<SampleItem>> = LCE.Loading(
        LIST_ITEMS_WITH_IMAGES_OPERATION_NAME,
    ),
) : ListState() {
    override fun title(stringProvider: StringProvider) =
        TitleBarModel(
            title = stringProvider.getString(ListItemsStringId.LIST_ITEMS_WITH_IMAGES_TITLE),
            shouldShowBack = true,
        )

    override fun toListItems(stringProvider: StringProvider): List<ListModel> =
        when (val loaded = content) {
            is LCE.Error -> error(LIST_ITEMS_WITH_IMAGES_OPERATION_NAME, loaded.error)

            else -> loaded.withStandardErrorAndLoading(
                loadingOperationNameOverride = LIST_ITEMS_WITH_IMAGES_OPERATION_NAME,
            ) {
                data.map { item ->
                    ImageNameCaptionListModel(
                        dataId = item.id,
                        name = item.name,
                        caption = item.caption,
                        sourceInfo = imageSourceFor(item),
                        imagePlaceholder = Icon.Person,
                        clickAction = ListItemsWithImagesAction.ItemClicked(item),
                    )
                }
            }
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

private const val LIST_ITEMS_WITH_IMAGES_SEED_PREFIX = "list-items-with-images"

/**
 * The generated bitmap's pixel size. It is larger than the 56 dp row thumbnail so the image stays
 * sharp on high-density screens; the size is not part of the seed.
 */
private const val LIST_ITEMS_WITH_IMAGES_IMAGE_SIZE = 128
