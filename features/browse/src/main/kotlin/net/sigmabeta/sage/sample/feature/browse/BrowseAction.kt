package net.sigmabeta.sage.sample.feature.browse

import net.sigmabeta.sage.appcomm.SageAction

/**
 * Browse's feature-specific actions.
 *
 * They drive the demo outcome rows appended to [BrowseState]; SAGE's shared actions still arrive
 * through the same [SageAction] seam.
 */
sealed class BrowseAction : SageAction() {
    data object LoadContent : BrowseAction()
    data object LoadEmpty : BrowseAction()
    data object LoadError : BrowseAction()
}
