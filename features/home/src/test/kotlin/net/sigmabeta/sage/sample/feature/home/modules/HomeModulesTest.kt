package net.sigmabeta.sage.sample.feature.home.modules

import kotlin.random.Random
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import net.sigmabeta.sage.appcomm.LCE
import net.sigmabeta.sage.components.ErrorStateListModel
import net.sigmabeta.sage.components.GridImageListModel
import net.sigmabeta.sage.components.SectionHeaderListModel
import net.sigmabeta.sage.sample.feature.home.DemoHomeRepository
import net.sigmabeta.sage.sample.feature.home.FakeHomeRepository
import net.sigmabeta.sage.sample.feature.home.FakeStringProvider
import net.sigmabeta.sage.sample.feature.home.HomeAction
import net.sigmabeta.sage.sample.feature.home.HomeItem
import net.sigmabeta.sage.sample.feature.home.HomeModuleFactory
import net.sigmabeta.sage.sample.feature.home.HomeModuleSection
import net.sigmabeta.sage.sample.feature.home.HomeRepository
import net.sigmabeta.sage.sample.feature.home.HomeSectionState
import net.sigmabeta.sage.sample.feature.home.HomeState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeModulesTest {
    private val strings = FakeStringProvider()

    @Test
    fun `daily index maps every day into range and rejects an empty catalog`() {
        assertNull(dailyIndex(epochDay = 0, itemCount = 0))
        assertNull(dailyIndex(epochDay = 3, itemCount = -1))

        assertEquals(0, dailyIndex(epochDay = 0, itemCount = 3))
        assertEquals(2, dailyIndex(epochDay = 2, itemCount = 3))
        // The last day before rollover, the rollover itself, and a wrap further ahead.
        assertEquals(2, dailyIndex(epochDay = 2, itemCount = 3))
        assertEquals(0, dailyIndex(epochDay = 3, itemCount = 3))
        assertEquals(1, dailyIndex(epochDay = 7, itemCount = 3))
        // A day before the epoch still lands in range.
        assertEquals(2, dailyIndex(epochDay = -1, itemCount = 3))
    }

    @Test
    fun `big image emits loading then one numbered hero for the daily pick`() = runTest {
        val item = HomeItem(id = 7L, title = "Chosen")
        val repository = FakeHomeRepository(items = listOf(item))
        val module = bigImage(
            repository = repository,
            instanceNumber = 3,
            dataIdBase = 20_000_000L,
        )

        val emissions = module.state().toList()

        assertEquals(listOf(LCE.Loading("home.big_image_3.load")), emissions.dropLast(1))
        val section = (emissions.last() as LCE.Content<HomeModuleSection>).data
        assertEquals("str-HOME_SECTION_BIG_IMAGE:3", section.title)

        val hero = section.items.single() as GridImageListModel
        assertEquals("Chosen", hero.name)
        assertEquals(20_000_000L + 7L, hero.dataId)
        assertEquals("generated://home/big_image_3/7?width=528&height=704", hero.sourceInfo.info)
        assertEquals(HomeAction.ItemClicked("Chosen"), hero.clickAction)
        assertEquals(0.75f, hero.aspectRatio, 0f)
        assertEquals(400f, hero.maxWidthDp!!, 0f)

        assertEquals(listOf("big_image_3"), repository.itemSeeds.distinct())
    }

    @Test
    fun `big image hides itself for an empty catalog`() = runTest {
        val module = bigImage(FakeHomeRepository())

        assertEquals(
            listOf(LCE.Loading("home.big_image_1.load"), LCE.Uninitialized),
            module.state().toList(),
        )
    }

    @Test
    fun `big image reports a repository failure as an error`() = runTest {
        val module = bigImage(
            repository = FakeHomeRepository(countFailure = IllegalStateException("boom")),
        )

        val failure = module.state().toList().last()
        assertTrue(failure is LCE.Error)
        assertEquals("home.big_image_1.load", (failure as LCE.Error).operationName)
        assertEquals("boom", failure.error.message)
    }

    @Test
    fun `grid images emits loading then the bounded item list`() = runTest {
        val items = (1L..10L).map { HomeItem(id = it, title = "Item $it") }
        val repository = FakeHomeRepository(items = items)
        val module = gridImages(
            repository = repository,
            instanceNumber = 2,
            dataIdBase = 10_000_000L,
        )

        val emissions = module.state().toList()

        assertEquals(listOf(LCE.Loading("home.grid_images_2.load")), emissions.dropLast(1))
        val section = (emissions.last() as LCE.Content<HomeModuleSection>).data
        assertEquals("str-HOME_SECTION_GRID_IMAGES:2", section.title)

        val cells = section.items.filterIsInstance<GridImageListModel>()
        assertEquals(items.map { 10_000_000L + it.id }, cells.map { it.dataId })
        assertEquals(items.map { it.title }, cells.map { it.name })
        assertEquals(
            "generated://home/grid_images_2/1?width=264&height=352",
            cells.first().sourceInfo.info,
        )
        assertEquals(HomeAction.ItemClicked("Item 1"), cells.first().clickAction)
        assertTrue(cells.all { it.aspectRatio == 0.75f })

        assertEquals(listOf("grid_images_2"), repository.gridSeeds)
    }

    @Test
    fun `grid images hides itself below the minimum item count`() = runTest {
        val items = (1L..2L).map { HomeItem(id = it, title = "Item $it") }
        val module = gridImages(FakeHomeRepository(items = items))

        assertEquals(
            listOf(LCE.Loading("home.grid_images_1.load"), LCE.Uninitialized),
            module.state().toList(),
        )
    }

    @Test
    fun `grid images maps a repository error emission to its own operation`() = runTest {
        val module = gridImages(
            repository = FakeHomeRepository(
                items = (1L..10L).map { HomeItem(it, "Item $it") },
                gridResponse = { _, _ -> LCE.Error("repo", IllegalStateException("boom")) },
            ),
        )

        val failure = module.state().toList().last()
        assertTrue(failure is LCE.Error)
        assertEquals("home.grid_images_1.load", (failure as LCE.Error).operationName)
        assertEquals("boom", failure.error.message)
    }

    @Test
    fun `grid images catches a repository that throws while streaming`() = runTest {
        val module = gridImages(
            repository = FakeHomeRepository(gridFailure = IllegalStateException("exploded")),
        )

        val failure = module.state().toList().last()
        assertTrue(failure is LCE.Error)
        assertEquals("home.grid_images_1.load", (failure as LCE.Error).operationName)
        assertEquals("exploded", failure.error.message)
    }

    @Test
    fun `a randomized plan renders without duplicate list ids`() = runTest {
        val items = (1L..18L).map { HomeItem(id = it, title = "Item $it") }
        val modules = HomeModuleFactory(
            repository = FakeHomeRepository(items = items),
            stringProvider = strings,
        ).create(Random(seed = 7))

        val sections = modules.map { module ->
            val section = (module.state().toList().last() as LCE.Content<HomeModuleSection>).data
            HomeSectionState(module.id, module.priority, LCE.Content(section))
        }
        val rows = HomeState(sections.toImmutableList()).toActual(strings).listItems

        assertTrue(rows.none { it is ErrorStateListModel })
        assertEquals(rows.size, rows.map { it.dataId }.toSet().size)

        val headers = rows.filterIsInstance<SectionHeaderListModel>().map { it.title }
        assertEquals(headers.size, headers.toSet().size)
    }

    @Test
    fun `content is generated from each module instance's seed`() = runTest {
        val repository = DemoHomeRepository()
        val firstBig = bigImage(repository, instanceNumber = 1)
        val secondBig = bigImage(repository, instanceNumber = 2)
        val firstGrid = gridImages(repository, instanceNumber = 1)
        val secondGrid = gridImages(repository, instanceNumber = 2)

        suspend fun heroName(module: BigImageModule): String {
            val section = (module.state().toList().last() as LCE.Content<HomeModuleSection>).data
            return (section.items.single() as GridImageListModel).name
        }

        suspend fun cellNames(module: GridImagesModule): List<String> {
            val section = (module.state().toList().last() as LCE.Content<HomeModuleSection>).data
            return section.items.filterIsInstance<GridImageListModel>().map { it.name }
        }

        assertNotEquals(heroName(firstBig), heroName(secondBig))
        assertNotEquals(cellNames(firstGrid), cellNames(secondGrid))
        // A second visit to the same instance id reproduces the same content.
        assertEquals(heroName(firstBig), heroName(bigImage(repository, instanceNumber = 1)))
        assertEquals(cellNames(firstGrid), cellNames(gridImages(repository, instanceNumber = 1)))
    }

    private fun bigImage(
        repository: HomeRepository,
        instanceNumber: Int = 1,
        dataIdBase: Long = 0L,
    ) = BigImageModule(
        id = "big_image_$instanceNumber",
        priority = instanceNumber - 1,
        instanceNumber = instanceNumber,
        dataIdBase = dataIdBase,
        repository = repository,
        stringProvider = strings,
    )

    private fun gridImages(
        repository: HomeRepository,
        instanceNumber: Int = 1,
        dataIdBase: Long = 0L,
    ) = GridImagesModule(
        id = "grid_images_$instanceNumber",
        priority = instanceNumber - 1,
        instanceNumber = instanceNumber,
        dataIdBase = dataIdBase,
        repository = repository,
        stringProvider = strings,
    )
}
