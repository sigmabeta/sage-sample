package net.sigmabeta.sage.sample.feature.listitems
import net.sigmabeta.sage.sample.models.SampleItem

import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import net.sigmabeta.sage.appcomm.LCE
import net.sigmabeta.sage.di.AppScope
import net.sigmabeta.sage.sample.settings.DemoOutcome
import net.sigmabeta.sage.sample.settings.SampleSettingsManager
import net.sigmabeta.sage.sample.settings.demoOutcomeFailure
import net.sigmabeta.sage.sample.settings.select

/**
 * The sample's generated datasource for Grid of Images.
 *
 * It reuses List Items' seeded generator over the same deterministic set, but asks for
 * [GRID_OF_IMAGES_ITEM_COUNT] cells so the grid shows more of the demo content. [load] serves an
 * `[offset, offset + limit)` window, so the grid fetches one page at a time. The artificial latency
 * comes from the shared `runInBackground` seam, which applies the duration selected on the Settings
 * screen; this repository does not add a delay of its own.
 *
 * The Settings demo outcome is read when a load is requested, not cached at construction:
 * [DemoOutcome.CONTENT] returns the generated cells, [DemoOutcome.EMPTY] returns a successful
 * empty result, and [DemoOutcome.ERROR] fails the load. Because the value is read per call,
 * changing it affects the next load without an app restart.
 *
 * Metro binds it to [GridOfImagesRepository] once for the application graph; the sample has no
 * real backend.
 */
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class DemoGridOfImagesRepository(
    private val settingsManager: SampleSettingsManager,
) : GridOfImagesRepository {
    override fun load(offset: Int, limit: Int): Flow<LCE<List<SampleItem>>> {
        val outcome = settingsManager.demoOutcome.value

        return flow {
            emit(
                outcome.select(
                    content = {
                        LCE.Content(
                            demoSampleItemsWindow(
                                offset = offset,
                                limit = limit,
                                total = GRID_OF_IMAGES_ITEM_COUNT,
                            ),
                        )
                    },
                    empty = { LCE.Content(emptyList()) },
                    error = {
                        LCE.Error(
                            operationName = GRID_OF_IMAGES_OPERATION_NAME,
                            error = demoOutcomeFailure(),
                        )
                    },
                ),
            )
        }
    }
}
