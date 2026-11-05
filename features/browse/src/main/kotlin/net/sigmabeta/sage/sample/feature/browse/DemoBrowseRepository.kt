package net.sigmabeta.sage.sample.feature.browse

import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import net.sigmabeta.sage.appcomm.LCE
import net.sigmabeta.sage.di.AppScope

/**
 * The sample's stand-in datasource for Browse.
 *
 * It emits SAGE's real asynchronous shape — a loading value, a short delay, then content or an
 * error — so the demo exercises the same path a network repository would. Metro binds it to
 * [BrowseRepository] once for the application graph; the sample has no real backend.
 */
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class DemoBrowseRepository : BrowseRepository {
    override fun load(outcome: BrowseOutcome): Flow<LCE<List<BrowseItem>>> = flow {
        emit(LCE.Loading(BROWSE_OPERATION_NAME))
        delay(DEMO_DELAY_MILLIS)

        emit(
            when (outcome) {
                BrowseOutcome.SUCCESS -> LCE.Content(DEMO_ITEMS)

                BrowseOutcome.EMPTY -> LCE.Content(emptyList())

                BrowseOutcome.ERROR -> LCE.Error(
                    operationName = BROWSE_OPERATION_NAME,
                    error = IllegalStateException("Demo Browse load failed."),
                )
            },
        )
    }

    private companion object {
        const val DEMO_DELAY_MILLIS = 500L

        val DEMO_ITEMS = listOf(
            BrowseItem(id = 1L, name = "SAGE lists"),
            BrowseItem(id = 2L, name = "Metro view models"),
            BrowseItem(id = 3L, name = "Voyager navigation"),
        )
    }
}
