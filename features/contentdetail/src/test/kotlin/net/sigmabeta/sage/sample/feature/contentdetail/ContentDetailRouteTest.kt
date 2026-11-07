package net.sigmabeta.sage.sample.feature.contentdetail

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The feature-owned route contract between Content detail and the app.
 *
 * `SageEvent.NavigateTo` carries only a string, so the typed [ContentId] travels inside the route
 * and the app decodes it back through [contentDetailIdFromRoute]. These tests are the single
 * definition of that encoding.
 */
class ContentDetailRouteTest {
    @Test
    fun `a content id encodes into a content detail route`() {
        assertEquals(
            "content-detail/list-items:7",
            contentDetailRoute(ContentId("list-items:7")),
        )
    }

    @Test
    fun `a content detail route decodes to its content id`() {
        assertEquals(
            ContentId("home:big_image_1:5"),
            contentDetailIdFromRoute("content-detail/home:big_image_1:5"),
        )
    }

    @Test
    fun `namespaced ids round trip unchanged`() {
        listOf(
            "home:big_image_1:5",
            "search:list:-123456:3",
            "list-items:7",
            "list-items-with-images:25",
            "grid-of-images:1",
            "content-detail:related:4",
            "browse:featured",
        ).forEach { value ->
            val id = ContentId(value)
            assertEquals(id, contentDetailIdFromRoute(contentDetailRoute(id)))
        }
    }

    @Test
    fun `the same numeric item routes apart in every content namespace`() {
        // Every surface numbers its generated items from 1, so the namespaces are what keep a
        // tap on item 7 in one list from opening item 7 from another.
        val ids = listOf(
            "home:big_image_1:7",
            "search:list:-123456:7",
            "search:grid:-123456:7",
            "grid-of-images:7",
            "content-detail:related:7",
            "content-detail:item:7",
            "list-items:7",
            "list-items-with-images:7",
        ).map(::ContentId)

        ids.forEach { assertTrue("$it must name a known surface", isKnownContentId(it)) }
        assertEquals(ids.size, ids.map { contentDetailRoute(it) }.toSet().size)
    }

    @Test
    fun `the default content id is a known, encodable id`() {
        assertTrue(isKnownContentId(DEFAULT_CONTENT_ID))
        assertEquals(
            "content-detail/browse:featured",
            contentDetailRoute(DEFAULT_CONTENT_ID),
        )
    }

    @Test
    fun `a missing id decodes to null`() {
        assertNull(contentDetailIdFromRoute("content-detail"))
        assertNull(contentDetailIdFromRoute("content-detail/"))
    }

    @Test
    fun `an invalid or multi-segment id decodes to null`() {
        assertNull(contentDetailIdFromRoute("content-detail/has space"))
        assertNull(contentDetailIdFromRoute("content-detail/a/b"))
        assertNull(contentDetailIdFromRoute("content-detail/!!"))
        assertNull(contentDetailIdFromRoute("content-detailish/1"))
    }

    @Test
    fun `a route outside the content detail prefix decodes to null`() {
        assertNull(contentDetailIdFromRoute("list-items:7"))
        assertNull(contentDetailIdFromRoute("browse"))
        assertNull(contentDetailIdFromRoute(""))
    }

    @Test
    fun `a content id rejects characters outside the contract`() {
        listOf("", "has space", "a/b", "naïve", "tab\t").forEach { value ->
            assertThrows(IllegalArgumentException::class.java) { ContentId(value) }
        }
    }

    @Test
    fun `content ids accept the contract's character set`() {
        val id = ContentId("A-Z_a-z.0-9:namespace")

        assertEquals("A-Z_a-z.0-9:namespace", id.value)
        assertNotNull(contentIdOrNull(id.value))
    }

    @Test
    fun `the safe parser returns null instead of throwing for invalid values`() {
        assertNull(contentIdOrNull(""))
        assertNull(contentIdOrNull("not valid"))
        assertEquals(ContentId("list-items:7"), contentIdOrNull("list-items:7"))
    }
}
