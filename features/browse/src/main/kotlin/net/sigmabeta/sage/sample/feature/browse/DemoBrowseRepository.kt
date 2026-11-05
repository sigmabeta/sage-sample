package net.sigmabeta.sage.sample.feature.browse

import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import net.sigmabeta.sage.appcomm.LCE
import net.sigmabeta.sage.di.AppScope
import net.sigmabeta.sage.ui.StringProvider

/**
 * The sample's stand-in datasource for Browse.
 *
 * It emits SAGE's real asynchronous shape — a loading value, a short delay, then content or an
 * error — so the demo exercises the same path a network repository would. Metro binds it to
 * [BrowseRepository] once for the application graph; the sample has no real backend.
 *
 * The demo row names are user-visible text, so they come through [stringProvider] like every other
 * feature string instead of being hard-coded here.
 */
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class DemoBrowseRepository(private val stringProvider: StringProvider) : BrowseRepository {
    override fun load(outcome: BrowseOutcome): Flow<LCE<List<BrowseItem>>> = flow {
        emit(LCE.Loading(BROWSE_OPERATION_NAME))
        delay(DEMO_DELAY_MILLIS)

        emit(
            when (outcome) {
                BrowseOutcome.CONTENT -> LCE.Content(demoItems())

                BrowseOutcome.EMPTY -> LCE.Content(emptyList())

                BrowseOutcome.ERROR -> LCE.Error(
                    operationName = BROWSE_OPERATION_NAME,
                    // Diagnostic only: ErrorStateListModel renders the state helper's errorString,
                    // never this exception's message.
                    error = IllegalStateException("Demo Browse load failed."),
                )
            },
        )
    }

    private fun demoItems(): List<BrowseItem> = listOf(
        BrowseItem(1L, stringProvider.getString(BrowseStringId.BROWSE_DEMO_ITEM_LISTS)),
        BrowseItem(2L, stringProvider.getString(BrowseStringId.BROWSE_DEMO_ITEM_METRO)),
        BrowseItem(3L, stringProvider.getString(BrowseStringId.BROWSE_DEMO_ITEM_VOYAGER)),
    )

    private companion object {
        const val DEMO_DELAY_MILLIS = 500L
    }
}
