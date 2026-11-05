package net.sigmabeta.sage.sample.feature.welcome

import net.sigmabeta.sage.appcomm.SageAction

/**
 * Actions the Welcome screen's rows carry.
 *
 * A feature owns its vocabulary: the app forwards a tap through [SageAction], and
 * [WelcomeViewModel] decides what it means. These are not init actions, so each one is logged as
 * a user action rather than a screen view.
 */
sealed class WelcomeAction : SageAction() {
    data object OpenBrowse : WelcomeAction()
}
