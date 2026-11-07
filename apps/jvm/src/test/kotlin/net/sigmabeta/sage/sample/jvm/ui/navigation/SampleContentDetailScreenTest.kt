package net.sigmabeta.sage.sample.jvm.ui.navigation

import net.sigmabeta.sage.sample.feature.contentdetail.ContentId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * Content detail is parameterized: the screen key embeds the full content ID, so Voyager's
 * per-screen ViewModelStore (and therefore the view model) is distinct per ID.
 */
class SampleContentDetailScreenTest {
    @Test
    fun `each content id has its own screen key`() {
        val first = ContentId("list-items:1")
        val second = ContentId("list-items:2")

        assertNotEquals(
            ContentDetailScreen(first).key,
            ContentDetailScreen(second).key,
        )
        assertEquals(
            ContentDetailScreen(first).key,
            ContentDetailScreen(first).key,
        )
    }

    @Test
    fun `a missing id has a distinct not-found key`() {
        assertNotEquals(
            ContentDetailScreen(null).key,
            ContentDetailScreen(ContentId("list-items:1")).key,
        )
        assertEquals(ContentDetailScreen(null), ContentDetailScreen(null))
    }

    @Test
    fun `every id keeps the content detail destination`() {
        assertEquals(
            SampleDestination.CONTENT_DETAIL,
            ContentDetailScreen(ContentId("browse:featured")).destination,
        )
        assertEquals(
            SampleDestination.CONTENT_DETAIL,
            ContentDetailScreen(null).destination,
        )
    }
}
