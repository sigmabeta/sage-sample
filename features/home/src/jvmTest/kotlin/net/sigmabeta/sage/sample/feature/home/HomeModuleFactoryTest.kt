package net.sigmabeta.sage.sample.feature.home
import net.sigmabeta.sage.sample.models.HomeItem

import kotlin.random.Random
import kotlinx.coroutines.flow.emptyFlow
import net.sigmabeta.sage.appcomm.LCE
import net.sigmabeta.sage.sample.feature.home.modules.BigImageModule
import net.sigmabeta.sage.sample.feature.home.modules.BigImageModuleDefinition
import net.sigmabeta.sage.sample.feature.home.modules.GridImagesModule
import net.sigmabeta.sage.sample.feature.home.modules.GridImagesModuleDefinition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The plan contract. Stable mode renders one instance of every registered definition in priority
 * order; randomized mode keeps the demo's 2–10 instances, both types represented, and the unique
 * keys the section slots and list de-duping rely on.
 */
class HomeModuleFactoryTest {
    private val repository = FakeHomeRepository(
        items = (1L..18L).map { HomeItem(id = it, title = "Item $it") },
    )

    // Deliberately unsorted: the factory, not the set's iteration order, decides the plan order.
    private val factory = HomeModuleFactory(
        setOf(
            GridImagesModuleDefinition(repository, FakeStringProvider()),
            BigImageModuleDefinition(repository, FakeStringProvider()),
        ),
    )

    @Test
    fun `stable mode renders every registered definition once in priority order`() {
        val modules = factory.create(homeShowsSameContent = true)

        assertEquals(2, modules.size)
        assertEquals(
            listOf("big_image_1", "grid_images_2"),
            modules.map { it.id },
        )
        assertEquals(listOf(0, 1), modules.map { it.priority })
        assertTrue(modules[0] is BigImageModule)
        assertTrue(modules[1] is GridImagesModule)
    }

    @Test
    fun `stable mode includes every registered definition`() {
        val factory = HomeModuleFactory(
            setOf(
                GridImagesModuleDefinition(repository, FakeStringProvider()),
                fakeDefinition("extra", priority = 5, flow = emptyFlow<LCE<HomeModuleSection>>()),
                BigImageModuleDefinition(repository, FakeStringProvider()),
            ),
        )

        assertEquals(
            listOf("big_image_1", "grid_images_2", "extra_3"),
            factory.create(homeShowsSameContent = true).map { it.id },
        )
    }

    @Test
    fun `stable mode is deterministic across visits`() {
        fun plan(): List<String> = factory.create(homeShowsSameContent = true).map {
            "${it::class.simpleName}:${it.id}:${it.priority}"
        }

        assertEquals(plan(), plan())
    }

    @Test
    fun `every randomized plan is within bounds and contains both module types`() {
        repeat(50) { seed ->
            val modules = factory.create(Random(seed))

            assertTrue(
                "seed $seed produced ${modules.size} modules",
                modules.size in MIN_HOME_MODULE_COUNT..MAX_HOME_MODULE_COUNT,
            )
            assertTrue(modules.any { it is BigImageModule })
            assertTrue(modules.any { it is GridImagesModule })
            assertTrue(
                modules.filterIsInstance<BigImageModule>().all { it.id.startsWith("big_image_") },
            )
            assertTrue(
                modules.filterIsInstance<GridImagesModule>().all { it.id.startsWith("grid_images_") },
            )
        }
    }

    @Test
    fun `instance ids and priorities are unique and follow the rendered order`() {
        repeat(20) { seed ->
            val modules = factory.create(Random(seed))

            assertEquals(modules.size, modules.map { it.id }.toSet().size)
            assertEquals((0 until modules.size).toList(), modules.map { it.priority })
        }
    }

    @Test
    fun `the same seed produces the same plan`() {
        fun plan(seed: Int): List<String> = factory.create(Random(seed)).map {
            "${it::class.simpleName}:${it.id}:${it.priority}"
        }

        assertEquals(plan(seed = 42), plan(seed = 42))
    }
}
