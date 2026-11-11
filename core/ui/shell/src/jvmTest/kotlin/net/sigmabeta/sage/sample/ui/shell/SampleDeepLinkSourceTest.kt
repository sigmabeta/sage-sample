package net.sigmabeta.sage.sample.ui.shell

import net.sigmabeta.sage.sample.nav.SampleDestination
import net.sigmabeta.sage.sample.nav.SampleRoute
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The native deep-link queue: a host offers a decoded route, the shell reads it once, and
 * consuming it makes a recreated host unable to replay the link.
 */
class SampleDeepLinkSourceTest {
    private val browseItems = SampleRoute(
        SampleDestination.BROWSE,
        SampleDestination.LIST_ITEMS,
    )

    @Test
    fun `a source starts with nothing pending`() {
        assertNull(MutableSampleDeepLinkSource().pending.value)
    }

    @Test
    fun `an offered route stays pending until the shell consumes it`() {
        val source = MutableSampleDeepLinkSource()

        source.offer(browseItems)

        assertEquals(browseItems, source.pending.value)
        source.consume()
        assertNull(source.pending.value)
    }

    @Test
    fun `a plain launch leaves a pending link alone`() {
        val source = MutableSampleDeepLinkSource(browseItems)

        source.offer(null)

        assertEquals(browseItems, source.pending.value)
    }

    @Test
    fun `a later link replaces the pending one`() {
        val source = MutableSampleDeepLinkSource(browseItems)
        val search = SampleRoute(SampleDestination.SEARCH, SampleDestination.SEARCH)

        source.offer(search)

        assertEquals(search, source.pending.value)
    }

    @Test
    fun `the same route can be offered again after it was consumed`() {
        val source = MutableSampleDeepLinkSource(browseItems)
        source.consume()

        source.offer(browseItems)

        assertEquals(browseItems, source.pending.value)
    }
}
