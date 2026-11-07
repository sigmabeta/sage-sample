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
import net.sigmabeta.sage.sample.settings.demoOutcomeFailure
import net.sigmabeta.sage.sample.settings.select

/**
 * The sample's generated datasource for List Items and List Items with Images.
 *
 * Every load rebuilds the same seeded rows through [demoSampleItems], so the rows are random in
 * content but identical on every visit — the screen looks the same after popping and re-pushing.
 * [load] serves an `[offset, offset + limit)` window of the fixed [LIST_ITEMS_ITEM_COUNT] set, so
 * paging screens fetch one page at a time while the non-paginated List Items screen asks for the
 * whole set. The artificial latency comes from the shared `runInBackground` seam, which applies
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
                                total = LIST_ITEMS_ITEM_COUNT,
                            ),
                        )
                    },
                    empty = { LCE.Content(emptyList()) },
                    error = {
                        LCE.Error(
                            operationName = LIST_ITEMS_OPERATION_NAME,
                            error = demoOutcomeFailure(),
                        )
                    },
                ),
            )
        }
    }
}
