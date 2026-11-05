package net.sigmabeta.sage.sample.jvm.ui.navigation

import net.sigmabeta.sage.appcomm.SageEvent

/**
 * What a feature's navigation intent means for the app's single back stack.
 *
 * The translation is a pure function ([navActionFor]) so the app half of the feature-navigation
 * contract is testable without a `Navigator` or Compose.
 */
sealed interface SampleNavAction {
    /** Reset the stack to `[Home, destination]`, the tab-switch semantics. */
    data class ShowTopLevel(val destination: SampleDestination) : SampleNavAction

    /** Push the destination once, the same way the hamburger opens Settings. */
    data class Push(val destination: SampleDestination) : SampleNavAction

    /** Pop the current screen. */
    data object Pop : SampleNavAction
}

/**
 * Translates a feature [SageEvent] into a back-stack operation.
 *
 * Routes resolve through [SampleDestination.fromRoute]. A top-level destination resets the stack
 * (`ShowTopLevel`); every other destination is pushed, so a feature can reach Settings without
 * discarding the screen the user came from. Events the app does not translate, and unknown routes,
 * return `null` and are ignored rather than crashing a screen.
 */
fun navActionFor(event: SageEvent): SampleNavAction? = when (event) {
    is SageEvent.NavigateTo -> navActionForRoute(event.destination)
    is SageEvent.NavigateSingleTopLevel -> navActionForRoute(event.destination)
    is SageEvent.NavigateBack -> SampleNavAction.Pop
    else -> null
}

private fun navActionForRoute(route: String): SampleNavAction? {
    val destination = SampleDestination.fromRoute(route) ?: return null
    return if (destination.isTopLevel) {
        SampleNavAction.ShowTopLevel(destination)
    } else {
        SampleNavAction.Push(destination)
    }
}
