package net.sigmabeta.sage.sample.feature.listitems

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import net.sigmabeta.sage.appcomm.LCE
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * SAGE's `ListScreen` keys its `LazyColumn` by `ListModel.dataId`.
 *
 * Both List Items screens therefore have to emit the same stable, unique identity per row: the
 * repository's item id, never the row's position. These tests use the real demo repository so the
 * contract holds for the content the screens actually render.
 */
class ListItemsListIdentityTest {
    private val strings = FakeStringProvider()

    @Test
    fun `both screens use the repository ids as unique lazy list keys`() = runTest {
        val items = demoItems()
        val expectedIds = items.map { it.id }

        val textRowIds = ListItemsState(LCE.Content(items))
            .toListItems(strings)
            .map { it.dataId }
        val imageRowIds = ListItemsWithImagesState(LCE.Content(items))
            .toListItems(strings)
            .map { it.dataId }
        val gridRowIds = GridOfImagesState(LCE.Content(items))
            .toListItems(strings)
            .map { it.dataId }

        assertEquals(expectedIds, textRowIds)
        assertEquals(expectedIds, imageRowIds)
        assertEquals(expectedIds, gridRowIds)
        assertEquals(expectedIds.size, expectedIds.distinct().size)
    }

    @Test
    fun `the lazy list keys are unchanged when a screen reloads the same items`() = runTest {
        val items = demoItems()

        assertEquals(
            ListItemsState(LCE.Content(items)).toListItems(strings).map { it.dataId },
            ListItemsState(LCE.Content(items)).toListItems(strings).map { it.dataId },
        )
        assertEquals(
            ListItemsWithImagesState(LCE.Content(items)).toListItems(strings).map { it.dataId },
            ListItemsWithImagesState(LCE.Content(items)).toListItems(strings).map { it.dataId },
        )
        assertEquals(
            GridOfImagesState(LCE.Content(items)).toListItems(strings).map { it.dataId },
            GridOfImagesState(LCE.Content(items)).toListItems(strings).map { it.dataId },
        )
    }

    private suspend fun demoItems(): List<SampleItem> {
        val emission = DemoSampleItemRepository(FakeSampleSettingsManager()).load().toList().single()
        return (emission as LCE.Content).data
    }
}
