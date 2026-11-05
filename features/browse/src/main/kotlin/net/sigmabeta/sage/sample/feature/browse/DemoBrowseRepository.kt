package net.sigmabeta.sage.sample.feature.browse

import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import net.sigmabeta.sage.appcomm.LCE
import net.sigmabeta.sage.di.AppScope
import net.sigmabeta.sage.ui.StringProvider

/**
 * The sample's stand-in datasource for Browse.
 *
 * It exposes SAGE's real asynchronous shape — an [LCE] stream the view model collects — so the
 * screen exercises the same path a network repository would. The artificial latency comes from
 * the shared `runInBackground` seam, which applies the duration selected on the Settings screen;
 * this repository does not add a delay of its own. Metro binds it to [BrowseRepository] once for
 * the application graph; the sample has no real backend.
 *
 * The demo row names are user-visible text, so they come through [stringProvider] like every other
 * feature string instead of being hard-coded here.
 */
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class DemoBrowseRepository(private val stringProvider: StringProvider) : BrowseRepository {
    override fun load(): Flow<LCE<List<BrowseItem>>> = flow {
        emit(LCE.Content(demoItems()))
    }

    private fun demoItems(): List<BrowseItem> = listOf(
        BrowseItem(1L, stringProvider.getString(BrowseStringId.BROWSE_DEMO_ITEM_LISTS)),
        BrowseItem(2L, stringProvider.getString(BrowseStringId.BROWSE_DEMO_ITEM_METRO)),
        BrowseItem(3L, stringProvider.getString(BrowseStringId.BROWSE_DEMO_ITEM_VOYAGER)),
    )
}
