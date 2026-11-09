package net.sigmabeta.sage.sample.feature.listitems
import net.sigmabeta.sage.sample.models.SampleItem
import net.sigmabeta.sage.sample.repository.LIST_ITEMS_OPERATION_NAME

import net.sigmabeta.sage.appcomm.LCE
import net.sigmabeta.sage.components.EmptyStateListModel
import net.sigmabeta.sage.components.ErrorStateListModel
import net.sigmabeta.sage.components.LoadingItemListModel
import net.sigmabeta.sage.components.LoadingType
import net.sigmabeta.sage.components.NameCaptionListModel
import net.sigmabeta.sage.ui.Icon
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ListItemsStateTest {
    private val strings = FakeStringProvider()

    @Test
    fun `loading content maps to loading rows`() {
        val items = ListItemsState(LCE.Loading(LIST_ITEMS_OPERATION_NAME)).toListItems(strings)

        val loadingRows = items.filterIsInstance<LoadingItemListModel>()
        assertTrue(loadingRows.isNotEmpty())
        assertTrue(loadingRows.all { it.loadingType == LoadingType.TEXT_CAPTION })
        assertTrue(loadingRows.all { it.loadOperationName == LIST_ITEMS_OPERATION_NAME })
        assertTrue(items.none { it is NameCaptionListModel })
    }

    @Test
    fun `content maps to name caption rows with stable ids`() {
        val state = ListItemsState(
            LCE.Content(
                listOf(
                    SampleItem(
                        id = 11L,
                        name = "Sleve McDichael",
                        caption = "Fermentum pellentesque iaculis",
                    ),
                    SampleItem(
                        id = 22L,
                        name = "Exie Clayton",
                        caption = "Lorem ipsum dolor",
                    ),
                ),
            ),
        )

        val rows = state.toListItems(strings).filterIsInstance<NameCaptionListModel>()

        assertEquals(listOf(11L, 22L), rows.map { it.dataId })
        assertEquals(listOf("Sleve McDichael", "Exie Clayton"), rows.map { it.name })
        assertEquals(
            listOf("Fermentum pellentesque iaculis", "Lorem ipsum dolor"),
            rows.map { it.caption },
        )
        assertEquals(
            listOf(11L, 22L),
            rows.map { (it.clickAction as ListItemsAction.ItemClicked).item.id },
        )
    }

    @Test
    fun `empty content maps to one localized empty state`() {
        val rows = ListItemsState(LCE.Content(emptyList())).toListItems(strings)

        val emptyState = rows.filterIsInstance<EmptyStateListModel>().single()
        assertEquals("str-LIST_ITEMS_EMPTY", emptyState.explanation)
        assertEquals(Icon.Person, emptyState.icon)
        assertFalse(emptyState.showCrossOut)
        assertTrue(rows.none { it is NameCaptionListModel })
        assertEquals(1, rows.size)
    }

    @Test
    fun `error maps to one localized error state under the list items operation`() {
        val items = ListItemsState(
            LCE.Error(LIST_ITEMS_OPERATION_NAME, IllegalStateException("boom")),
        ).toListItems(strings)

        val errorState = items.filterIsInstance<ErrorStateListModel>().single()
        assertEquals(LIST_ITEMS_OPERATION_NAME, errorState.failedOperationName)
        assertEquals("str-LIST_ITEMS_ERROR", errorState.errorString)
        assertEquals("boom", errorState.error.message)
        assertTrue(items.none { it is NameCaptionListModel })
        assertEquals(1, items.size)
    }

    @Test
    fun `uninitialized content shows loading rather than a blank list`() {
        val rows = ListItemsState(LCE.Uninitialized).toListItems(strings)

        assertTrue(rows.any { it is LoadingItemListModel })
    }

    @Test
    fun `every terminal state renders at least one row`() {
        val states = listOf(
            ListItemsState(LCE.Loading(LIST_ITEMS_OPERATION_NAME)),
            ListItemsState(LCE.Content(emptyList())),
            ListItemsState(LCE.Error(LIST_ITEMS_OPERATION_NAME, IllegalStateException("boom"))),
            ListItemsState(LCE.Uninitialized),
        )

        states.forEach { state ->
            val rows = state.toListItems(strings)
            assertTrue("$state rendered no rows", rows.isNotEmpty())
            assertEquals(rows.size, rows.map { it.dataId }.distinct().size)
        }
    }

    @Test
    fun `empty and error keep the screen title and back`() {
        listOf(
            ListItemsState(LCE.Content(emptyList())),
            ListItemsState(LCE.Error(LIST_ITEMS_OPERATION_NAME, IllegalStateException("boom"))),
        ).forEach { state ->
            val title = state.title(strings)
            assertEquals("str-LIST_ITEMS_TITLE", title.title)
            assertTrue(title.shouldShowBack)
        }
    }
}
