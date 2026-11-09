package net.sigmabeta.sage.sample.nav

import net.sigmabeta.sage.sample.models.ContentId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The browser URL contract: every app destination has one canonical path, and short links without
 * a tab prefix land on Home.
 */
class SampleRouteUrlTest {
    @Test
    fun `tab roots encode to one segment`() {
        assertEquals("/home", SampleRouteUrl.encode(SampleRoute.HOME))
        assertEquals(
            "/browse",
            SampleRouteUrl.encode(
                SampleRoute(SampleDestination.BROWSE, SampleDestination.BROWSE),
            ),
        )
    }

    @Test
    fun `pushed screens carry their owning tab`() {
        assertEquals(
            "/browse/list-items",
            SampleRouteUrl.encode(
                SampleRoute(SampleDestination.BROWSE, SampleDestination.LIST_ITEMS),
            ),
        )
        assertEquals(
            "/home/content-detail/list-items:7",
            SampleRouteUrl.encode(
                SampleRoute(
                    SampleDestination.HOME,
                    SampleDestination.CONTENT_DETAIL,
                    ContentId("list-items:7"),
                ),
            ),
        )
    }

    @Test
    fun `the root decodes to home`() {
        assertEquals(SampleRoute.HOME, SampleRouteUrl.decode("/"))
        assertEquals(SampleRoute.HOME, SampleRouteUrl.decode(""))
        assertNull(SampleRouteUrl.decode("/nope"))
    }

    @Test
    fun `tab roots decode`() {
        assertEquals(SampleRoute.HOME, SampleRouteUrl.decode("/home"))
        assertEquals(
            SampleRoute(SampleDestination.SEARCH, SampleDestination.SEARCH),
            SampleRouteUrl.decode("/search"),
        )
    }

    @Test
    fun `tab-prefixed pushed screens decode`() {
        assertEquals(
            SampleRoute(SampleDestination.BROWSE, SampleDestination.LIST_ITEMS),
            SampleRouteUrl.decode("/browse/list-items"),
        )
        assertEquals(
            SampleRoute(
                SampleDestination.HOME,
                SampleDestination.CONTENT_DETAIL,
                ContentId("home:big_image_1:5"),
            ),
            SampleRouteUrl.decode("/home/content-detail/home:big_image_1:5"),
        )
    }

    @Test
    fun `tab-less pushed screens land on home`() {
        assertEquals(
            SampleRoute(SampleDestination.HOME, SampleDestination.SETTINGS),
            SampleRouteUrl.decode("/settings"),
        )
        assertEquals(
            SampleRoute(
                SampleDestination.HOME,
                SampleDestination.CONTENT_DETAIL,
                ContentId("list-items:7"),
            ),
            SampleRouteUrl.decode("/content-detail/list-items:7"),
        )
    }

    @Test
    fun `a content-detail route without a valid id is the not-found state`() {
        assertEquals(
            SampleRoute(SampleDestination.HOME, SampleDestination.CONTENT_DETAIL),
            SampleRouteUrl.decode("/content-detail"),
        )
        assertEquals(
            SampleRoute(SampleDestination.HOME, SampleDestination.CONTENT_DETAIL),
            SampleRouteUrl.decode("/content-detail/bad id"),
        )
    }

    @Test
    fun `query strings and fragments are ignored`() {
        assertEquals(
            SampleRoute(SampleDestination.BROWSE, SampleDestination.BROWSE),
            SampleRouteUrl.decode("/browse?api=http://localhost:8080"),
        )
        assertEquals(
            SampleRoute(SampleDestination.HOME, SampleDestination.SETTINGS),
            SampleRouteUrl.decode("/settings#about"),
        )
    }

    @Test
    fun `malformed paths are rejected`() {
        assertNull(SampleRouteUrl.decode("/browse/search"))
        assertNull(SampleRouteUrl.decode("/browse/list-items/extra"))
        assertNull(SampleRouteUrl.decode("/content-detail/a/b"))
        assertNull(SampleRouteUrl.decode("/home/nope"))
    }

    @Test
    fun `every decoded route round trips through its canonical URL`() {
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
            assertEquals(route, SampleRouteUrl.decode(SampleRouteUrl.encode(route)))
        }
    }
}
