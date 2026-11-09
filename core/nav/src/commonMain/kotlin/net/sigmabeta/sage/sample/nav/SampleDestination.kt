package net.sigmabeta.sage.sample.nav

import net.sigmabeta.sage.nav.ArgType
import net.sigmabeta.sage.nav.RouteDescriptor
import net.sigmabeta.sage.sample.models.CONTENT_DETAIL_ROUTE
import net.sigmabeta.sage.sample.strings.api.SampleStringId
import net.sigmabeta.sage.ui.Icon

/**
 * The app's destinations, described with SAGE's navigation vocabulary.
 *
 * [isTopLevel] marks the destinations rendered as bottom-bar / rail items — Settings is reached
 * from the hamburger instead. It also supplies the top bar's pre-report fallback: once a screen
 * reports its SAGE `ListStateActual.title`, the reported `shouldShowBack` decides between the
 * hamburger and the back arrow.
 *
 * The label is a [SampleStringId], not an Android resource: the shared strings module owns the
 * text and both apps resolve it through `SampleStringId.text()`.
 */
enum class SampleDestination(
    override val destName: String,
    val label: SampleStringId,
    val icon: Icon,
    val isTopLevel: Boolean,
) : RouteDescriptor {
    HOME("home", SampleStringId.NAV_HOME, Icon.Home, isTopLevel = true),
    BROWSE("browse", SampleStringId.NAV_BROWSE, Icon.Browse, isTopLevel = true),
    SEARCH("search", SampleStringId.NAV_SEARCH, Icon.Search, isTopLevel = true),
    LIST_ITEMS("list-items", SampleStringId.NAV_LIST_ITEMS, Icon.Browse, isTopLevel = false),
    LIST_ITEMS_WITH_IMAGES(
        "list-items-with-images",
        SampleStringId.NAV_LIST_ITEMS_WITH_IMAGES,
        Icon.Browse,
        isTopLevel = false,
    ),
    GRID_OF_IMAGES(
        "grid-of-images",
        SampleStringId.NAV_GRID_OF_IMAGES,
        Icon.Browse,
        isTopLevel = false,
    ),
    CONTENT_DETAIL(
        "content-detail",
        SampleStringId.NAV_CONTENT_DETAIL,
        Icon.Description,
        isTopLevel = false,
    ),
    SETTINGS("settings", SampleStringId.NAV_SETTINGS, Icon.Person, isTopLevel = false),
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
        fun fromRoute(route: String): SampleDestination? = when {
            // Content detail is parameterized: every route under its prefix is the destination,
            // and the route codec decides whether the carried ID is usable. A bare or malformed
            // route still opens the explicit not-found screen instead of being ignored.
            route == CONTENT_DETAIL_ROUTE || route.startsWith("$CONTENT_DETAIL_ROUTE/") ->
                CONTENT_DETAIL

            else -> entries.firstOrNull { it.destName == route }
        }
    }
}
