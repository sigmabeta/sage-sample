package net.sigmabeta.sage.sample.feature.browse

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import net.sigmabeta.sage.analytics.Analytics
import net.sigmabeta.sage.analytics.AnalyticsScreenId
import net.sigmabeta.sage.appcomm.LCE
import net.sigmabeta.sage.appcomm.SageAction
import net.sigmabeta.sage.appcomm.SageEvent
import net.sigmabeta.sage.ui.SageStringId
import net.sigmabeta.sage.ui.StringProvider

internal class FakeStringProvider : StringProvider {
    override fun getString(string: SageStringId): String = "str-$string"

    override fun getStringOneArg(string: SageStringId, arg: String): String =
        error("unused")

    override fun getStringOneInt(string: SageStringId, arg: Int): String =
        error("unused")

    override fun getStringTwoArgs(string: SageStringId, first: String, second: String): String =
        error("unused")
}

internal class FakeAnalytics : Analytics {
    override fun logScreenView(action: SageAction, screen: AnalyticsScreenId) = Unit

    override fun logAction(action: SageAction, fromScreen: AnalyticsScreenId) = Unit

    override fun logEvent(event: SageEvent) = Unit

    override fun logAutoRefresh() = Unit

    override fun logError(
        failedOperationName: String,
        errorString: String,
        error: Throwable,
    ) = Unit
}

/**
 * Repository seam used by the view model tests.
 *
 * Each outcome gets its own replayed stream, so the test decides exactly when a value reaches the
 * view model. No real delays, no background threads.
 */
internal class FakeBrowseRepository : BrowseRepository {
    private val streams = mutableMapOf<BrowseOutcome, MutableSharedFlow<LCE<List<BrowseItem>>>>()

    override fun load(outcome: BrowseOutcome): Flow<LCE<List<BrowseItem>>> = streamFor(outcome)

    suspend fun emit(outcome: BrowseOutcome, value: LCE<List<BrowseItem>>) {
        streamFor(outcome).emit(value)
    }

    private fun streamFor(outcome: BrowseOutcome): MutableSharedFlow<LCE<List<BrowseItem>>> =
        streams.getOrPut(outcome) {
            MutableSharedFlow(replay = 1, extraBufferCapacity = 1)
        }
}
