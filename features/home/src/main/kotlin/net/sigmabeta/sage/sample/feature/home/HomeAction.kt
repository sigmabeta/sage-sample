package net.sigmabeta.sage.sample.feature.home

import net.sigmabeta.sage.appcomm.SageAction

/**
 * Actions Home rows carry.
 *
 * A feature owns its vocabulary: a module builds its rows with one of these, and
 * [HomeViewModel] decides what it means. These are not init actions, so each one is logged as a
 * user action rather than a screen view.
 */
sealed class HomeAction : SageAction() {
    /** A module's image was tapped; [title] names the demo item. */
    data class ItemClicked(val title: String) : HomeAction()
}
