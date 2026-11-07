package net.sigmabeta.sage.sample.ui.navigation

import androidx.annotation.StringRes
import net.sigmabeta.sage.nav.ArgType
import net.sigmabeta.sage.nav.RouteDescriptor
import net.sigmabeta.sage.sample.R
import net.sigmabeta.sage.ui.Icon

/**
 * The app's destinations, described with SAGE's navigation vocabulary.
 *
 * [isTopLevel] marks the destinations rendered as bottom-bar / rail items — Settings is reached
 * from the hamburger instead. It also supplies the top bar's pre-report fallback: once a screen
 * reports its SAGE `ListStateActual.title`, the reported `shouldShowBack` decides between the
 * hamburger and the back arrow.
 */
enum class SampleDestination(
    override val destName: String,
    @get:StringRes val labelRes: Int,
    val icon: Icon,
    val isTopLevel: Boolean,
) : RouteDescriptor {
    HOME("home", R.string.nav_home, Icon.Home, isTopLevel = true),
    BROWSE("browse", R.string.nav_browse, Icon.Browse, isTopLevel = true),
    SEARCH("search", R.string.nav_search, Icon.Search, isTopLevel = true),
    LIST_ITEMS("list-items", R.string.nav_list_items, Icon.Browse, isTopLevel = false),
    LIST_ITEMS_WITH_IMAGES(
        "list-items-with-images",
        R.string.nav_list_items_with_images,
        Icon.Browse,
        isTopLevel = false,
    ),
    GRID_OF_IMAGES(
        "grid-of-images",
        R.string.nav_grid_of_images,
        Icon.Browse,
        isTopLevel = false,
    ),
    CONTENT_DETAIL(
        "content-detail",
        R.string.nav_content_detail,
        Icon.Description,
        isTopLevel = false,
    ),
    SETTINGS("settings", R.string.nav_settings, Icon.Person, isTopLevel = false),
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
