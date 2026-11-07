package net.sigmabeta.sage.sample.jvm.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The app half of the feature → app navigation contract: a feature emits a route string, and the
 * app resolves it through [SampleDestination.fromRoute].
 */
class SampleDestinationTest {
    @Test
    fun `top-level destinations are Home, Browse and Search in display order`() {
        assertEquals(
            listOf(
                SampleDestination.HOME,
                SampleDestination.BROWSE,
                SampleDestination.SEARCH,
            ),
            SampleDestination.topLevel,
        )
    }

    @Test
    fun `fromRoute maps the browse route to the browse destination`() {
        assertEquals(SampleDestination.BROWSE, SampleDestination.fromRoute("browse"))
    }

    @Test
    fun `fromRoute maps the list items route to the list items destination`() {
        assertEquals(SampleDestination.LIST_ITEMS, SampleDestination.fromRoute("list-items"))
    }

    @Test
    fun `fromRoute maps the list items with images route to its destination`() {
        assertEquals(
            SampleDestination.LIST_ITEMS_WITH_IMAGES,
            SampleDestination.fromRoute("list-items-with-images"),
        )
    }

    @Test
    fun `fromRoute maps the grid of images route to its destination`() {
        assertEquals(
            SampleDestination.GRID_OF_IMAGES,
            SampleDestination.fromRoute("grid-of-images"),
        )
    }

    @Test
    fun `fromRoute maps the content detail route to its destination`() {
        assertEquals(
            SampleDestination.CONTENT_DETAIL,
            SampleDestination.fromRoute("content-detail"),
        )
    }

    @Test
    fun `fromRoute returns null for an unknown route`() {
        assertNull(SampleDestination.fromRoute("no-such-destination"))
    }
}
