package net.sigmabeta.sage.sample.feature.listitems

import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import net.sigmabeta.sage.appcomm.LCE
import net.sigmabeta.sage.di.AppScope

/**
 * The sample's generated datasource for Grid of Images.
 *
 * It reuses List Items' seeded generator but asks for [GRID_OF_IMAGES_ITEM_COUNT] rows, so the
 * grid is a larger set of the same deterministic demo content. The artificial latency comes from
 * the shared `runInBackground` seam, which applies the duration selected on the Settings screen;
 * this repository does not add a delay of its own.
 *
 * Metro binds it to [GridOfImagesRepository] once for the application graph; the sample has no
 * real backend.
 */
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class DemoGridOfImagesRepository : GridOfImagesRepository {
    override fun load(): Flow<LCE<List<SampleItem>>> = flow {
        emit(LCE.Content(demoSampleItems(GRID_OF_IMAGES_ITEM_COUNT)))
    }
}
