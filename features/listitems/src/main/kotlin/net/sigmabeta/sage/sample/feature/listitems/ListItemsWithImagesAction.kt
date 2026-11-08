package net.sigmabeta.sage.sample.feature.listitems
import net.sigmabeta.sage.sample.models.SampleItem

import net.sigmabeta.sage.appcomm.SageAction

/**
 * Actions the List Items with Images screen's rows carry.
 *
 * It is a separate vocabulary from [ListItemsAction] because the two screens are separate
 * destinations: the app forwards the same tap, but each view model decides what it means.
 */
sealed class ListItemsWithImagesAction : SageAction() {
    data class ItemClicked(val item: SampleItem) : ListItemsWithImagesAction()
}
