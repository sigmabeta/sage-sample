package net.sigmabeta.sage.sample.feature.browse

import net.sigmabeta.sage.appcomm.SageAction
import net.sigmabeta.sage.components.SingleTextListModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BrowseStateTest {
    private val strings = FakeStringProvider()

    @Test
    fun `browse lists only its three destination rows`() {
        val rows = BrowseState.toListItems(strings)

        assertEquals(3, rows.size)
        assertTrue(rows.all { it is SingleTextListModel })
    }

    @Test
    fun `the list items row opens its destination`() {
        val openRow = BrowseState.toListItems(strings)[0] as SingleTextListModel

        assertEquals("str-BROWSE_OPEN_LIST_ITEMS", openRow.name)
        assertTrue(openRow.clickAction is BrowseAction.OpenListItems)
        assertEquals(BrowseState.OPEN_LIST_ITEMS_DATA_ID, openRow.dataId)
    }

    @Test
    fun `the list items with images row opens its destination`() {
        val openRow = BrowseState.toListItems(strings)[1] as SingleTextListModel

        assertEquals("str-BROWSE_OPEN_LIST_ITEMS_WITH_IMAGES", openRow.name)
        assertTrue(openRow.clickAction is BrowseAction.OpenListItemsWithImages)
        assertEquals(BrowseState.OPEN_LIST_ITEMS_WITH_IMAGES_DATA_ID, openRow.dataId)
    }

    @Test
    fun `the grid of images row opens its destination`() {
        val openRow = BrowseState.toListItems(strings)[2] as SingleTextListModel

        assertEquals("str-BROWSE_OPEN_GRID_OF_IMAGES", openRow.name)
        assertTrue(openRow.clickAction is BrowseAction.OpenGridOfImages)
        assertEquals(BrowseState.OPEN_GRID_OF_IMAGES_DATA_ID, openRow.dataId)
    }

    @Test
    fun `no row is an inactive no-op`() {
        val actions = BrowseState.toListItems(strings)
            .filterIsInstance<SingleTextListModel>()
            .map { it.clickAction }

        assertTrue(actions.none { it is SageAction.Noop })
    }
}
