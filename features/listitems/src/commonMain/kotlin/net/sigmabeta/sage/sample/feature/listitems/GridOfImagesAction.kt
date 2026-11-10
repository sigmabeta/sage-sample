package net.sigmabeta.sage.sample.feature.listitems
import net.sigmabeta.sage.sample.models.ContentArtworkSize
import net.sigmabeta.sage.sample.models.ContentId

import net.sigmabeta.sage.appcomm.SageAction

/**
 * Actions the Grid of Images cells carry.
 *
 * It is a separate vocabulary from the List Items actions because the screens are separate
 * destinations: the app forwards the same tap, but each view model decides what it means.
 * [ItemClicked.contentId] is the stable detail address the cell opens, and
 * [ItemClicked.artwork] is the cell's own image size, so the detail can reuse the cached bitmap
 * it was showing.
 */
sealed class GridOfImagesAction : SageAction() {
    data class ItemClicked(
        val contentId: ContentId,
        val artwork: ContentArtworkSize? = null,
    ) : GridOfImagesAction()
}

/** The stable content ID for the grid cell showing [itemId]. */
internal fun gridOfImagesContentId(itemId: Long): ContentId =
    ContentId("grid-of-images:$itemId")
