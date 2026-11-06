package net.sigmabeta.sage.sample.jvm.ui.navigation

import net.sigmabeta.sage.nav.ArgType
import net.sigmabeta.sage.nav.RouteDescriptor
import net.sigmabeta.sage.sample.jvm.SampleStrings
import net.sigmabeta.sage.ui.Icon

/**
 * The app's destinations, described with SAGE's navigation vocabulary.
 *
 * [isTopLevel] marks the destinations rendered as bottom-bar / rail items — Settings is reached
 * from the hamburger instead. It also supplies the top bar's pre-report fallback: once a screen
 * reports its SAGE `ListStateActual.title`, the reported `shouldShowBack` decides between the
 * hamburger and the back arrow.
 *
 * The desktop app has no Android resources, so [label] is a plain string; the Android app keeps
 * the same labels as `R.string` ids.
 */
enum class SampleDestination(
    override val destName: String,
    val label: String,
    val icon: Icon,
    val isTopLevel: Boolean,
) : RouteDescriptor {
    HOME("home", SampleStrings.NAV_HOME, Icon.Home, isTopLevel = true),
    BROWSE("browse", SampleStrings.NAV_BROWSE, Icon.Browse, isTopLevel = true),
    SEARCH("search", SampleStrings.NAV_SEARCH, Icon.Search, isTopLevel = true),
    LIST_ITEMS("list-items", SampleStrings.NAV_LIST_ITEMS, Icon.Browse, isTopLevel = false),
    LIST_ITEMS_WITH_IMAGES(
        "list-items-with-images",
        SampleStrings.NAV_LIST_ITEMS_WITH_IMAGES,
        Icon.Browse,
        isTopLevel = false,
    ),
    SETTINGS("settings", SampleStrings.NAV_SETTINGS, Icon.Person, isTopLevel = false),
    ;

    override val argType: ArgType = ArgType.NONE

    companion object {
        /** Destinations rendered as bottom-bar / rail items, in display order. */
        val topLevel: List<SampleDestination> = entries.filter { it.isTopLevel }

        /**
         * Resolves a feature-emitted route (the `destination` of a SAGE
         * [net.sigmabeta.sage.appcomm.SageEvent.NavigateTo]) to a destination.
         *
         * This is what makes [RouteDescriptor.destName] load-bearing: features name routes as
         * strings, and this is the single app-side lookup. Unknown routes return `null` so
         * callers can ignore them instead of crashing.
         */
        fun fromRoute(route: String): SampleDestination? =
            entries.firstOrNull { it.destName == route }
    }
}
