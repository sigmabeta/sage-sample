package net.sigmabeta.sage.sample.feature.listitems

import net.sigmabeta.sage.appcomm.SageAction

/**
 * Actions the Grid of Images cells carry.
 *
 * It is a separate vocabulary from the List Items actions because the screens are separate
 * destinations: the app forwards the same tap, but each view model decides what it means.
 */
sealed class GridOfImagesAction : SageAction() {
    data class ItemClicked(val item: SampleItem) : GridOfImagesAction()
}
