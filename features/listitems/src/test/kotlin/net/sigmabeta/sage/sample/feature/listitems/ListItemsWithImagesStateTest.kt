package net.sigmabeta.sage.sample.feature.listitems

import net.sigmabeta.sage.appcomm.LCE
import net.sigmabeta.sage.components.ErrorStateListModel
import net.sigmabeta.sage.components.ImageNameCaptionListModel
import net.sigmabeta.sage.components.LoadingItemListModel
import net.sigmabeta.sage.ui.Icon
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ListItemsWithImagesStateTest {
    private val strings = FakeStringProvider()

    private val items = listOf(
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
    )

    @Test
    fun `loading content maps to loading rows`() {
        val rows = ListItemsWithImagesState(
            LCE.Loading(LIST_ITEMS_WITH_IMAGES_OPERATION_NAME),
        ).toListItems(strings)

        assertTrue(rows.any { it is LoadingItemListModel })
        assertTrue(rows.none { it is ImageNameCaptionListModel })
    }

    @Test
    fun `content maps to image name caption rows with stable ids and generated urls`() {
        val rows = ListItemsWithImagesState(LCE.Content(items))
            .toListItems(strings)
            .filterIsInstance<ImageNameCaptionListModel>()

        assertEquals(listOf(11L, 22L), rows.map { it.dataId })
        assertEquals(listOf("Sleve McDichael", "Exie Clayton"), rows.map { it.name })
        assertEquals(
            listOf("Fermentum pellentesque iaculis", "Lorem ipsum dolor"),
            rows.map { it.caption },
        )
        assertEquals(
            listOf(
                "generated://list-items-with-images/11?width=128&height=128",
                "generated://list-items-with-images/22?width=128&height=128",
            ),
            rows.map { it.sourceInfo.info },
        )
        rows.forEach { row -> assertEquals(Icon.Person, row.imagePlaceholder) }
        assertEquals(
            listOf(11L, 22L),
            rows.map {
                (it.clickAction as ListItemsWithImagesAction.ItemClicked).item.id
            },
        )
    }

    @Test
    fun `the same item always maps to the same image source`() {
        assertEquals(imageSourceFor(items[0]), imageSourceFor(items[0]))
    }

    @Test
    fun `different items map to different image sources`() {
        assertNotEquals(imageSourceFor(items[0]), imageSourceFor(items[1]))
    }

    @Test
    fun `error maps to an error state`() {
        val rows = ListItemsWithImagesState(
            LCE.Error(
                LIST_ITEMS_WITH_IMAGES_OPERATION_NAME,
                IllegalStateException("boom"),
            ),
        ).toListItems(strings)

        val errorState = rows.filterIsInstance<ErrorStateListModel>().single()
        assertEquals(LIST_ITEMS_WITH_IMAGES_OPERATION_NAME, errorState.failedOperationName)
        assertEquals("boom", errorState.error.message)
    }
}
