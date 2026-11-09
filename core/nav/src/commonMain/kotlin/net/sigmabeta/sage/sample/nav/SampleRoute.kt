package net.sigmabeta.sage.sample.nav

import net.sigmabeta.sage.sample.models.ContentId
import net.sigmabeta.sage.sample.models.contentIdOrNull

/**
 * One addressable screen in the app shell: the owning top-level tab, the screen on top of that
 * tab's stack, and the content ID when that screen is Content detail.
 *
 * The browser uses this as its URL model. A top-level route has `screen == tab`; a pushed route
 * names a non-top-level [screen]. Keeping the owning tab in the route is what makes a deep link
 * to a pushed screen reload into the same tab instead of always landing on Home.
 */
data class SampleRoute(
    val tab: SampleDestination,
    val screen: SampleDestination,
    val contentId: ContentId? = null,
) {
    init {
        require(tab.isTopLevel) { "A route's tab must be top-level: ${tab.destName}" }
        require(contentId == null || screen == SampleDestination.CONTENT_DETAIL) {
            "Only content-detail routes carry a content id: ${screen.destName}"
        }
        if (screen.isTopLevel) {
            require(screen == tab && contentId == null) {
                "A top-level route must be its own tab: ${tab.destName}/${screen.destName}"
            }
        }
    }

    /** True when the route names the tab's root rather than a pushed screen. */
    val isRoot: Boolean get() = screen == tab

    companion object {
        /** The app's default address: Home at its root. */
        val HOME = SampleRoute(SampleDestination.HOME, SampleDestination.HOME)
    }
}

/**
 * The pure route ↔ URL codec shared by the browser bridge and its tests.
 *
 * Canonical URLs:
 * - `/` → Home
 * - `/home`, `/browse`, `/search` → a tab root
 * - `/browse/list-items` → List Items pushed on Browse
 * - `/home/content-detail/home:big_image_1:5` → Content detail on Home
 * - `/settings`, `/content-detail/{id}` → the same screens on Home (the canonical form adds the
 *   `home` prefix on load)
 *
 * The content ID sits in the final path segment. Content IDs are restricted to `A-Za-z0-9_.:-`,
 * so they need no percent-encoding; an invalid segment decodes to the not-found state (a null
 * content ID) rather than failing the whole route.
 */
object SampleRouteUrl {
    /** Encodes [route] as an absolute path. */
    fun encode(route: SampleRoute): String = buildString {
        append('/').append(route.tab.destName)
        if (!route.isRoot) {
            append('/').append(route.screen.destName)
            route.contentId?.let { append('/').append(it.value) }
        }
    }

    /**
     * Decodes a path (query string and fragment are ignored), or null when it does not name an
     * app destination. The caller decides how to canonicalise an unknown path.
     */
    fun decode(path: String): SampleRoute? {
        val withoutQuery = path.substringBefore('?').substringBefore('#')
        val segments = withoutQuery.split('/').filter { it.isNotEmpty() }
        if (segments.isEmpty()) return SampleRoute.HOME

        val tab = topLevelDestination(segments[0])
        if (tab != null) {
            if (segments.size == 1) return SampleRoute(tab, tab)
            val screen = destinationByName(segments[1]) ?: return null
            if (screen.isTopLevel) return null
            return routeFor(tab, screen, segments, screenIndex = 1)
        }

        // No tab prefix: a pushed screen on Home. This makes short links such as `/settings` and
        // `/content-detail/{id}` work; the bridge canonicalises them to `/home/...` on load.
        val screen = destinationByName(segments[0]) ?: return null
        if (screen.isTopLevel) return null
        return routeFor(SampleDestination.HOME, screen, segments, screenIndex = 0)
    }

    private fun routeFor(
        tab: SampleDestination,
        screen: SampleDestination,
        segments: List<String>,
        screenIndex: Int,
    ): SampleRoute? {
        val idIndex = screenIndex + 1
        if (screen != SampleDestination.CONTENT_DETAIL) {
            return if (segments.size == idIndex) SampleRoute(tab, screen) else null
        }
        return when (segments.size) {
            idIndex -> SampleRoute(tab, screen)
            idIndex + 1 -> SampleRoute(tab, screen, contentIdOrNull(segments[idIndex]))
            else -> null
        }
    }

    private fun destinationByName(segment: String): SampleDestination? =
        SampleDestination.entries.firstOrNull { it.destName == segment }

    private fun topLevelDestination(segment: String): SampleDestination? =
        SampleDestination.topLevel.firstOrNull { it.destName == segment }
}
