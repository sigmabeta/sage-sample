package net.sigmabeta.sage.sample.feature.home

import dev.zacsweers.metro.Inject
import kotlin.random.Random
import net.sigmabeta.sage.sample.feature.home.modules.BigImageModule
import net.sigmabeta.sage.sample.feature.home.modules.GridImagesModule
import net.sigmabeta.sage.ui.StringProvider

/**
 * Builds the randomized plan for one Home screen.
 *
 * Each Home screen renders between [MIN_HOME_MODULE_COUNT] and [MAX_HOME_MODULE_COUNT] instances
 * of the two module types, in random order. Both types are always represented so the demo shows
 * the full module range; the remaining slots are random. Every instance gets its own id,
 * priority, visible number, and data-id range, so duplicate modules cannot collide in SAGE's
 * list de-duping.
 *
 * Construction happens once per view model: the injected `List<HomeModule>` is stable while the
 * screen lives, and opening Home again builds a new plan (the same plan when the setting is on).
 */
@Inject
class HomeModuleFactory(
    private val repository: HomeRepository,
    private val stringProvider: StringProvider,
) {
    fun create(random: Random = Random.Default): List<HomeModule> {
        val count = random.nextInt(MIN_HOME_MODULE_COUNT, MAX_HOME_MODULE_COUNT + 1)
        val types = mutableListOf(HomeModuleType.BIG_IMAGE, HomeModuleType.GRID_IMAGES)
        repeat(count - MIN_HOME_MODULE_COUNT) {
            types += HomeModuleType.entries.random(random)
        }
        types.shuffle(random)

        return types.mapIndexed { index, type ->
            val id = "${type.id}_${index + 1}"
            val instanceNumber = index + 1
            val dataIdBase = index * HOME_MODULE_DATA_ID_STRIDE
            when (type) {
                HomeModuleType.BIG_IMAGE -> BigImageModule(
                    id = id,
                    priority = index,
                    instanceNumber = instanceNumber,
                    dataIdBase = dataIdBase,
                    repository = repository,
                    stringProvider = stringProvider,
                )

                HomeModuleType.GRID_IMAGES -> GridImagesModule(
                    id = id,
                    priority = index,
                    instanceNumber = instanceNumber,
                    dataIdBase = dataIdBase,
                    repository = repository,
                    stringProvider = stringProvider,
                )
            }
        }
    }

    /**
     * Honors the "Home shows same content" preference: true uses [HOME_MODULE_STATIC_SEED], so
     * every visit builds the same plan; false draws a fresh random plan.
     */
    fun create(homeShowsSameContent: Boolean): List<HomeModule> = create(
        if (homeShowsSameContent) Random(HOME_MODULE_STATIC_SEED) else Random.Default,
    )
}

/** The two row implementations Home knows how to instance. */
private enum class HomeModuleType(val id: String) {
    BIG_IMAGE("big_image"),
    GRID_IMAGES("grid_images"),
}

/** Inclusive bounds for one Home screen's module count. */
internal const val MIN_HOME_MODULE_COUNT = 2
internal const val MAX_HOME_MODULE_COUNT = 10

/**
 * Keeps each instance's item ids in their own range. Demo item ids are small; the stride leaves
 * room for far larger catalogs without two modules sharing an id.
 */
internal const val HOME_MODULE_DATA_ID_STRIDE = 10_000_000L

/**
 * The plan seed used when "Home shows same content" is on. It is deliberately fixed and
 * arbitrary: changing it changes the repeated demo layout.
 */
internal const val HOME_MODULE_STATIC_SEED = 123
