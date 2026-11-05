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
    fun `loading content maps to loading rows`() {
        val items = BrowseState(LCE.Loading("browse")).toListItems(strings)

        assertTrue(items.first() is LoadingItemListModel)
        assertTrue(items.none { it is SingleTextListModel })
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

        val contentRows = state.toListItems(strings).filterIsInstance<SingleTextListModel>()

        assertEquals(listOf(11L, 22L), contentRows.map { it.dataId })
        assertEquals(listOf("Alpha", "Beta"), contentRows.map { it.name })
    }

    @Test
    fun `empty content maps to an empty state`() {
        val items = BrowseState(LCE.Content(emptyList())).toListItems(strings)

        val emptyState = items.first() as EmptyStateListModel
        assertEquals("str-BROWSE_EMPTY", emptyState.explanation)
    }

    @Test
    fun `error maps to an error state`() {
        val items = BrowseState(
            LCE.Error("browse", IllegalStateException("boom")),
        ).toListItems(strings)

        val errorState = items.first() as ErrorStateListModel
        assertEquals("browse", errorState.failedOperationName)
        assertEquals("boom", errorState.error.message)
    }
}
