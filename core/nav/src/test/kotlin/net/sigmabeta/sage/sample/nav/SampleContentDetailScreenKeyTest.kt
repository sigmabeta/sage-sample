package net.sigmabeta.sage.sample.nav

import net.sigmabeta.sage.sample.models.ContentId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * Content detail is parameterized: the screen key embeds the full content ID, so Voyager's
 * per-screen ViewModelStore (and therefore the view model) is distinct per ID.
 */
class SampleContentDetailScreenKeyTest {
    @Test
    fun `each content id has its own screen key`() {
        val first = ContentId("list-items:1")
        val second = ContentId("list-items:2")

        assertNotEquals(
            contentDetailScreenKey(first),
            contentDetailScreenKey(second),
        )
        assertEquals(
            contentDetailScreenKey(first),
            contentDetailScreenKey(first),
        )
    }

    @Test
    fun `a missing id has a distinct not-found key`() {
        assertNotEquals(
            contentDetailScreenKey(null),
            contentDetailScreenKey(ContentId("list-items:1")),
        )
        assertEquals(contentDetailScreenKey(null), contentDetailScreenKey(null))
    }
}
