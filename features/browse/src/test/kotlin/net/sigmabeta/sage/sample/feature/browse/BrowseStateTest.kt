package net.sigmabeta.sage.sample.feature.browse

import net.sigmabeta.sage.appcomm.LCE
import net.sigmabeta.sage.components.EmptyStateListModel
import net.sigmabeta.sage.components.ErrorStateListModel
import net.sigmabeta.sage.components.LoadingItemListModel
import net.sigmabeta.sage.components.SingleTextListModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BrowseStateTest {
    private val strings = FakeStringProvider()

    @Test
    fun `the list items row is always present and opens the destination`() {
        val items = BrowseState(LCE.Loading("browse")).toListItems(strings)

        val openRow = items.first() as SingleTextListModel
        assertEquals("str-BROWSE_OPEN_LIST_ITEMS", openRow.name)
        assertTrue(openRow.clickAction is BrowseAction.OpenListItems)
        assertEquals(BrowseState.OPEN_LIST_ITEMS_DATA_ID, openRow.dataId)
    }

    @Test
    fun `loading content maps to loading rows`() {
        val items = BrowseState(LCE.Loading("browse")).toListItems(strings)

        assertTrue(items.any { it is LoadingItemListModel })
        assertTrue(
            items.none {
                it is SingleTextListModel && it.dataId != BrowseState.OPEN_LIST_ITEMS_DATA_ID
            },
        )
    }

    @Test
    fun `content maps to single text rows with stable ids`() {
        val state = BrowseState(
            LCE.Content(
                listOf(
                    BrowseItem(id = 11L, name = "Alpha"),
                    BrowseItem(id = 22L, name = "Beta"),
                ),
            ),
        )

        val rows = state.toListItems(strings)
            .filterIsInstance<SingleTextListModel>()
            .filter { it.dataId != BrowseState.OPEN_LIST_ITEMS_DATA_ID }

        assertEquals(listOf(11L, 22L), rows.map { it.dataId })
        assertEquals(listOf("Alpha", "Beta"), rows.map { it.name })
    }

    @Test
    fun `empty content maps to an empty state`() {
        val items = BrowseState(LCE.Content(emptyList())).toListItems(strings)

        val emptyState = items.filterIsInstance<EmptyStateListModel>().single()
        assertEquals("str-BROWSE_EMPTY", emptyState.explanation)
    }

    @Test
    fun `error maps to an error state`() {
        val items = BrowseState(
            LCE.Error("browse", IllegalStateException("boom")),
        ).toListItems(strings)

        val errorState = items.filterIsInstance<ErrorStateListModel>().single()
        assertEquals("browse", errorState.failedOperationName)
        assertEquals("boom", errorState.error.message)
    }
}
