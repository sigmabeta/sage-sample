package net.sigmabeta.sage.sample.nav

import net.sigmabeta.sage.sample.models.ContentId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The native deep-link contract: one custom scheme whose authority is the route's first segment,
 * decoded by the same route codec the browser uses.
 */
class SampleDeepLinkTest {
    @Test
    fun `the scheme decodes tab roots`() {
        assertEquals(SampleRoute.HOME, SampleDeepLink.routeFor("sagesample://home"))
        assertEquals(
            SampleRoute(SampleDestination.BROWSE, SampleDestination.BROWSE),
            SampleDeepLink.routeFor("sagesample://browse"),
        )
        assertEquals(
            SampleRoute(SampleDestination.SEARCH, SampleDestination.SEARCH),
            SampleDeepLink.routeFor("sagesample://search"),
        )
    }

    @Test
    fun `pushed screens carry their owning tab`() {
        assertEquals(
            SampleRoute(SampleDestination.BROWSE, SampleDestination.LIST_ITEMS),
            SampleDeepLink.routeFor("sagesample://browse/list-items"),
        )
        assertEquals(
            SampleRoute(
                SampleDestination.HOME,
                SampleDestination.CONTENT_DETAIL,
                ContentId("list-items:7"),
            ),
            SampleDeepLink.routeFor("sagesample://home/content-detail/list-items:7"),
        )
    }

    @Test
    fun `short links land on home`() {
        assertEquals(
            SampleRoute(SampleDestination.HOME, SampleDestination.SETTINGS),
            SampleDeepLink.routeFor("sagesample://settings"),
        )
        assertEquals(
            SampleRoute(
                SampleDestination.HOME,
                SampleDestination.CONTENT_DETAIL,
                ContentId("browse:featured"),
            ),
            SampleDeepLink.routeFor("sagesample://content-detail/browse:featured"),
        )
    }

    @Test
    fun `a scheme link without a path is home`() {
        assertEquals(SampleRoute.HOME, SampleDeepLink.routeFor("sagesample://"))
    }

    @Test
    fun `bare paths are desktop launch arguments`() {
        assertEquals(
            SampleRoute(SampleDestination.BROWSE, SampleDestination.LIST_ITEMS),
            SampleDeepLink.routeFor("/browse/list-items"),
        )
        assertEquals(
            SampleRoute(SampleDestination.HOME, SampleDestination.SETTINGS),
            SampleDeepLink.routeFor("settings"),
        )
    }

    @Test
    fun `query strings and fragments are ignored`() {
        assertEquals(
            SampleRoute(SampleDestination.BROWSE, SampleDestination.BROWSE),
            SampleDeepLink.routeFor("sagesample://browse?campaign=123"),
        )
        assertEquals(
            SampleRoute(SampleDestination.HOME, SampleDestination.SETTINGS),
            SampleDeepLink.routeFor("sagesample://settings#about"),
        )
    }

    @Test
    fun `other schemes and unknown destinations are ignored`() {
        assertNull(SampleDeepLink.routeFor("https://example.com/browse/list-items"))
        assertNull(SampleDeepLink.routeFor("otherapp://browse"))
        assertNull(SampleDeepLink.routeFor("sagesample://nope"))
        assertNull(SampleDeepLink.routeFor("sagesample://browse/search"))
        assertNull(SampleDeepLink.routeFor(""))
    }

    @Test
    fun `every route round trips through a scheme link`() {
        listOf(
            "/home",
            "/browse",
            "/search",
            "/settings",
            "/list-items",
            "/browse/grid-of-images",
            "/content-detail/browse:featured",
            "/home/content-detail/search:list:-123456:3",
        ).forEach { path ->
            val route = requireNotNull(SampleRouteUrl.decode(path)) { path }
            assertEquals(route, SampleDeepLink.routeFor("sagesample://" + path.removePrefix("/")))
        }
    }
}
