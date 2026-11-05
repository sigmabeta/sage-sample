package net.sigmabeta.sage.sample.feature.listitems

import net.sigmabeta.sage.appcomm.LCE
import net.sigmabeta.sage.components.ErrorStateListModel
import net.sigmabeta.sage.components.LoadingItemListModel
import net.sigmabeta.sage.components.NameCaptionListModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ListItemsStateTest {
    private val strings = FakeStringProvider()

    @Test
    fun `loading content maps to loading rows`() {
        val items = ListItemsState(LCE.Loading(LIST_ITEMS_OPERATION_NAME)).toListItems(strings)

        assertTrue(items.any { it is LoadingItemListModel })
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
    fun `error maps to an error state`() {
        val items = ListItemsState(
            LCE.Error(LIST_ITEMS_OPERATION_NAME, IllegalStateException("boom")),
        ).toListItems(strings)

        val errorState = items.filterIsInstance<ErrorStateListModel>().single()
        assertEquals(LIST_ITEMS_OPERATION_NAME, errorState.failedOperationName)
        assertEquals("boom", errorState.error.message)
    }
}
