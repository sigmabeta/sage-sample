package net.sigmabeta.sage.sample.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The app half of the feature → app navigation contract: a feature emits a route string, and the
 * app resolves it through [SampleDestination.fromRoute].
 */
class SampleDestinationTest {
    @Test
    fun `fromRoute maps the browse route to the browse destination`() {
        assertEquals(SampleDestination.BROWSE, SampleDestination.fromRoute("browse"))
    }

    @Test
    fun `fromRoute maps the list items route to the list items destination`() {
        assertEquals(SampleDestination.LIST_ITEMS, SampleDestination.fromRoute("list-items"))
    }

    @Test
    fun `fromRoute returns null for an unknown route`() {
        assertNull(SampleDestination.fromRoute("no-such-destination"))
    }
}
