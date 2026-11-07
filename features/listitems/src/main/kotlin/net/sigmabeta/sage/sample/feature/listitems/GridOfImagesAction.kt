package net.sigmabeta.sage.sample.feature.listitems

import net.sigmabeta.sage.appcomm.SageAction
import net.sigmabeta.sage.sample.feature.contentdetail.ContentId

/**
 * Actions the Grid of Images cells carry.
 *
 * It is a separate vocabulary from the List Items actions because the screens are separate
 * destinations: the app forwards the same tap, but each view model decides what it means.
 * [ItemClicked.contentId] is the stable detail address the cell opens.
 */
sealed class GridOfImagesAction : SageAction() {
    data class ItemClicked(val contentId: ContentId) : GridOfImagesAction()
}

/** The stable content ID for the grid cell showing [itemId]. */
internal fun gridOfImagesContentId(itemId: Long): ContentId =
    ContentId("grid-of-images:$itemId")
