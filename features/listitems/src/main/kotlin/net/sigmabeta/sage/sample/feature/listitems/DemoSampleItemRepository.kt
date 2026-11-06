package net.sigmabeta.sage.sample.feature.listitems

import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import net.sigmabeta.sage.appcomm.LCE
import net.sigmabeta.sage.di.AppScope

/**
 * The sample's generated datasource for List Items.
 *
 * Every load rebuilds the same seeded rows through [demoSampleItems], so the 25 rows are random
 * in content but identical on every visit — the screen looks the same after popping and
 * re-pushing. The artificial latency comes from the shared `runInBackground` seam, which applies
 * the duration selected on the Settings screen; this repository does not add a delay of its own.
 *
 * Metro binds it to [SampleItemRepository] once for the application graph; the sample has no real
 * backend.
 */
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class DemoSampleItemRepository : SampleItemRepository {
    override fun load(): Flow<LCE<List<SampleItem>>> = flow {
        emit(LCE.Content(demoSampleItems(ITEM_COUNT)))
    }

    private companion object {
        const val ITEM_COUNT = 25
    }
}
