package net.sigmabeta.sage.sample.feature.listitems

import net.sigmabeta.sage.appcomm.SageAction

/**
 * Actions the List Items screen's rows carry.
 *
 * A feature owns its vocabulary: the app forwards a tap through [SageAction], and
 * [ListItemsViewModel] decides what it means. These are not init actions, so each one is logged
 * as a user action rather than a screen view.
 */
sealed class ListItemsAction : SageAction() {
    data class ItemClicked(val item: SampleItem) : ListItemsAction()
}
