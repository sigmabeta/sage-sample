package net.sigmabeta.sage.sample.nav

import net.sigmabeta.sage.appcomm.SageEvent
import net.sigmabeta.sage.sample.models.ContentArtworkSize
import net.sigmabeta.sage.sample.models.ContentId
import net.sigmabeta.sage.sample.models.contentDetailArtworkFromRoute
import net.sigmabeta.sage.sample.models.contentDetailIdFromRoute

/**
 * What a feature's navigation intent means for the app's single back stack.
 *
 * The translation is a pure function ([navActionFor]) so the app half of the feature-navigation
 * contract is testable without a `Navigator` or Compose.
 */
sealed interface SampleNavAction {
    /** Select the destination's tab — the top-level tab switch. */
    data class ShowTopLevel(val destination: SampleDestination) : SampleNavAction

    /**
     * Push onto the active tab's inner Navigator, the same way the hamburger opens Settings.
     *
     * [contentId] is set only for the parameterized Content detail destination; a `null` value
     * there means the route had no usable ID and the screen renders its not-found state.
     * [artworkSize] is the size the originating row requested its artwork at, when the route
     * carried one: the detail uses it to reuse that cached image instead of waiting for its own.
     */
    data class Push(
        val destination: SampleDestination,
        val contentId: ContentId? = null,
        val artworkSize: ContentArtworkSize? = null,
    ) : SampleNavAction

    /** Back: apply the shared [backActionFor] policy rather than popping a navigator directly. */
    data object Pop : SampleNavAction
}

/**
 * Translates a feature [SageEvent] into a navigation operation.
 *
 * Routes resolve through [SampleDestination.fromRoute]. A top-level destination selects its tab
 * (`ShowTopLevel`); every other destination is pushed onto the active tab, so a feature can reach
 * Settings without discarding the screen the user came from. Events the app does not translate,
 * and unknown routes, return `null` and are ignored rather than crashing a screen.
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
        SampleNavAction.Push(
            destination = destination,
            contentId = if (destination == SampleDestination.CONTENT_DETAIL) {
                contentDetailIdFromRoute(route)
            } else {
                null
            },
            artworkSize = if (destination == SampleDestination.CONTENT_DETAIL) {
                contentDetailArtworkFromRoute(route)
            } else {
                null
            },
        )
    }
}
