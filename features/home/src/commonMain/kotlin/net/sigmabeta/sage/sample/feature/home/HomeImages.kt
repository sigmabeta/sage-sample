package net.sigmabeta.sage.sample.feature.home
import net.sigmabeta.sage.sample.models.HomeItem

import net.sigmabeta.sage.images.SourceInfo
import net.sigmabeta.sage.sample.bitmaps.GeneratedImageFetcher

/**
 * Builds a `generated://` image source for one Home item.
 *
 * [moduleId] seeds the artwork together with the item id, so two instances of the same module
 * show different art even though their catalogs use the same positional ids. [width] and
 * [height] only pick the rendered resolution.
 */
internal fun homeImageSource(
    item: HomeItem,
    moduleId: String,
    width: Int,
    height: Int,
): SourceInfo = SourceInfo(
    GeneratedImageFetcher.urlFor(
        seed = "$HOME_IMAGE_SEED_PREFIX/$moduleId/${item.id}",
        width = width,
        height = height,
    ),
)

internal const val HOME_IMAGE_SEED_PREFIX = "home"
