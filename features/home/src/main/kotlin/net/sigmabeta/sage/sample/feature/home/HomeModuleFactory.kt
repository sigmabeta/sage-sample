package net.sigmabeta.sage.sample.feature.home

import dev.zacsweers.metro.Inject
import kotlin.random.Random

/**
 * Builds the module plan for one Home screen from the graph's registered definitions.
 *
 * Stable mode ("Home shows same content", now the default) renders one instance of every
 * [HomeModuleDefinition] in priority order, so every visit produces the same rows with the same
 * ids and titles. Randomized mode keeps the demo's 2–10 instance plan: every definition is
 * represented, the remaining slots repeat definitions at random, and the order is shuffled.
 *
 * Every plan slot gets its own instance number, priority, and data-id range, so repeated module
 * types cannot collide in SAGE's list de-duping.
 */
@Inject
class HomeModuleFactory(definitions: Set<HomeModuleDefinition>) {
    // Tie-break on typeId so a priority collision still yields a deterministic stable plan.
    private val definitions = definitions.sortedWith(compareBy({ it.priority }, { it.typeId }))

    /**
     * Honors the "Home shows same content" preference: true renders the stable plan, false draws
     * a fresh randomized plan.
     */
    fun create(homeShowsSameContent: Boolean): List<HomeModule> =
        if (homeShowsSameContent) stablePlan() else create(Random.Default)

    /** One instance of every registered definition, ordered by [HomeModuleDefinition.priority]. */
    private fun stablePlan(): List<HomeModule> = definitions.mapIndexed { index, definition ->
        definition.create(
            instanceNumber = index + 1,
            dataIdBase = index * HOME_MODULE_DATA_ID_STRIDE,
        )
    }

    /** A fresh randomized plan; pass a seeded [random] for a reproducible one. */
    fun create(random: Random = Random.Default): List<HomeModule> {
        if (definitions.isEmpty()) return emptyList()

        // Every registered definition is always represented, so the count can never fall below
        // the number of definitions even if more rows are added later.
        val minCount = maxOf(MIN_HOME_MODULE_COUNT, definitions.size)
        val maxCount = maxOf(MAX_HOME_MODULE_COUNT, definitions.size)
        val count = if (maxCount == minCount) minCount else random.nextInt(minCount, maxCount + 1)

        val types = definitions.toMutableList()
        repeat(count - types.size) { types += definitions.random(random) }
        types.shuffle(random)

        return types.mapIndexed { index, definition ->
            definition.create(
                instanceNumber = index + 1,
                dataIdBase = index * HOME_MODULE_DATA_ID_STRIDE,
            )
        }
    }
}

/** Inclusive bounds for one randomized Home plan's module count. */
internal const val MIN_HOME_MODULE_COUNT = 2
internal const val MAX_HOME_MODULE_COUNT = 10

/**
 * Keeps each instance's item ids in their own range. Demo item ids are small; the stride leaves
 * room for far larger catalogs without two modules sharing an id.
 */
internal const val HOME_MODULE_DATA_ID_STRIDE = 10_000_000L
