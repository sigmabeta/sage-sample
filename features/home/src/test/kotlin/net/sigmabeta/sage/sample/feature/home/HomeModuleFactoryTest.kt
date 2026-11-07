package net.sigmabeta.sage.sample.feature.home

import kotlin.random.Random
import net.sigmabeta.sage.sample.feature.home.modules.BigImageModule
import net.sigmabeta.sage.sample.feature.home.modules.GridImagesModule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The plan contract: every Home screen gets 2–10 module instances, both types are represented,
 * and every instance has the unique keys the section slots and list de-duping rely on.
 */
class HomeModuleFactoryTest {
    private val repository = FakeHomeRepository(
        items = (1L..18L).map { HomeItem(id = it, title = "Item $it") },
    )
    private val factory = HomeModuleFactory(repository, FakeStringProvider())

    @Test
    fun `every plan is within bounds and contains both module types`() {
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

    @Test
    fun `same-content mode uses the hardcoded plan seed`() {
        fun plan(modules: List<HomeModule>): List<String> = modules.map {
            "${it::class.simpleName}:${it.id}:${it.priority}"
        }

        assertEquals(
            plan(factory.create(Random(HOME_MODULE_STATIC_SEED))),
            plan(factory.create(homeShowsSameContent = true)),
        )
    }
}
