package net.sigmabeta.sage.sample.nav

/**
 * What a Back request means for the tabbed shell.
 *
 * This is the app's one Back vocabulary: the top-bar arrow, Android system Back, and a feature's
 * `SageEvent.NavigateBack` all funnel through [backActionFor] and are applied by the same handler in
 * `SampleNavHost`.
 */
sealed interface SampleBackAction {
    /** Pop the active tab's inner stack, back to its root if only one pushed screen remains. */
    data object Pop : SampleBackAction

    /** Leave the current tab root for another top-level tab, clearing the outgoing stack. */
    data class ShowTab(val destination: SampleDestination) : SampleBackAction

    /** Nothing left in the tabbed shell: let the host's default Back behaviour run. */
    data object Outer : SampleBackAction
}

/**
 * The single Back policy shared by every affordance.
 *
 * Chipbox's Library tab is the model: a pushed screen always pops *inside* the active tab, Search's
 * root is the only tab root that sends Back Home, and the remaining roots fall through to the host
 * (Android finishes the activity; desktop has nothing above the shell). Only top-level destinations
 * are active tabs, so the non-top-level fallback is [SampleBackAction.Outer].
 */
fun backActionFor(tab: SampleDestination, canPop: Boolean): SampleBackAction = when {
    canPop -> SampleBackAction.Pop
    tab == SampleDestination.SEARCH -> SampleBackAction.ShowTab(SampleDestination.HOME)
    else -> SampleBackAction.Outer
}
