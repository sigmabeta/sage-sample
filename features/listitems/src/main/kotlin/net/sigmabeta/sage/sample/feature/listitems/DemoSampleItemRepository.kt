package net.sigmabeta.sage.sample.feature.listitems

import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import net.sigmabeta.sage.appcomm.LCE
import net.sigmabeta.sage.di.AppScope
import net.sigmabeta.sage.sample.settings.DemoOutcome
import net.sigmabeta.sage.sample.settings.SampleSettingsManager

/**
 * The sample's generated datasource for List Items and List Items with Images.
 *
 * Every load rebuilds the same seeded rows through [demoSampleItems], so the 25 rows are random
 * in content but identical on every visit — the screen looks the same after popping and
 * re-pushing. The artificial latency comes from the shared `runInBackground` seam, which applies
 * the duration selected on the Settings screen; this repository does not add a delay of its own.
 *
 * The Settings demo outcome is read when a load is requested, not cached at construction:
 * [DemoOutcome.CONTENT] returns the generated rows, [DemoOutcome.EMPTY] returns a successful
 * empty result, and [DemoOutcome.ERROR] fails the load. Because the value is read per call,
 * changing it affects the next load without an app restart.
 *
 * Metro binds it to [SampleItemRepository] once for the application graph; the sample has no real
 * backend.
 */
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class DemoSampleItemRepository(
    private val settingsManager: SampleSettingsManager,
) : SampleItemRepository {
    override fun load(): Flow<LCE<List<SampleItem>>> {
        val outcome = settingsManager.demoOutcome.value

        return flow {
            emit(
                when (outcome) {
                    DemoOutcome.CONTENT -> LCE.Content(demoSampleItems(ITEM_COUNT))

                    DemoOutcome.EMPTY -> LCE.Content(emptyList())

                    DemoOutcome.ERROR -> LCE.Error(
                        operationName = LIST_ITEMS_OPERATION_NAME,
                        error = IllegalStateException("Demo outcome is ERROR"),
                    )
                },
            )
        }
    }

    private companion object {
        const val ITEM_COUNT = 25
    }
}
