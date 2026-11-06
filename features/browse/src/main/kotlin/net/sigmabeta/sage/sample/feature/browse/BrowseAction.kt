package net.sigmabeta.sage.sample.feature.browse

import net.sigmabeta.sage.appcomm.SageAction

/**
 * Actions the Browse screen's rows carry beyond the repository content.
 *
 * A feature owns its vocabulary: the app forwards a tap through [SageAction], and
 * [BrowseViewModel] decides what it means. These are not init actions, so each one is logged as a
 * user action rather than a screen view.
 */
sealed class BrowseAction : SageAction() {
    data object OpenListItems : BrowseAction()
    data object OpenListItemsWithImages : BrowseAction()
    data object OpenGridOfImages : BrowseAction()
}
