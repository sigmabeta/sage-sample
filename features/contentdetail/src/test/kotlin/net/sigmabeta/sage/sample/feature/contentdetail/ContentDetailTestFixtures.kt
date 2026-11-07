package net.sigmabeta.sage.sample.feature.contentdetail

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import net.sigmabeta.sage.analytics.Analytics
import net.sigmabeta.sage.analytics.AnalyticsScreenId
import net.sigmabeta.sage.appcomm.LCE
import net.sigmabeta.sage.appcomm.SageAction
import net.sigmabeta.sage.appcomm.SageEvent
import net.sigmabeta.sage.ui.Icon
import net.sigmabeta.sage.ui.SageStringId
import net.sigmabeta.sage.ui.StringProvider

internal class FakeStringProvider : StringProvider {
    override fun getString(string: SageStringId): String = "str-$string"

    override fun getStringOneArg(string: SageStringId, arg: String): String =
        "${getString(string)}:$arg"

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
 * Each requested ID gets its own replayed [MutableSharedFlow], so the test decides exactly when a
 * value for a given address reaches the view model, and a stale ID's stream can be driven after
 * the view model has moved on. No real delays, no background threads.
 */
internal class FakeContentDetailRepository : ContentDetailRepository {
    private val streams = mutableMapOf<ContentId, MutableSharedFlow<LCE<ContentDetail?>>>()

    /** Every ID the view model asked for, in request order. */
    val requestedIds = mutableListOf<ContentId>()

    override fun load(contentId: ContentId): Flow<LCE<ContentDetail?>> {
        requestedIds += contentId
        return streamFor(contentId)
    }

    suspend fun emit(contentId: ContentId, value: LCE<ContentDetail?>) {
        streamFor(contentId).emit(value)
    }

    private fun streamFor(contentId: ContentId): MutableSharedFlow<LCE<ContentDetail?>> =
        streams.getOrPut(contentId) {
            MutableSharedFlow(replay = 1, extraBufferCapacity = 1)
        }
}

/** A small content detail fixture with stable, easy-to-assert values. */
internal val CONTENT_DETAIL_FIXTURE = ContentDetail(
    id = ContentId("list-items:1"),
    title = "Fugiat lorem ipsum",
    details = listOf(
        ContentDetailField(label = "Alpha label", value = "One"),
        ContentDetailField(label = "Beta label", value = "Two"),
    ),
    actions = listOf(
        ContentAction(label = "First action", icon = Icon.Calendar),
        ContentAction(label = "Second action", icon = Icon.Tag),
    ),
    related = listOf(
        RelatedContent(id = 1L, name = "Alpha"),
        RelatedContent(id = 2L, name = "Beta"),
    ),
    items = listOf(
        ContentItem(id = 1L, name = "One", caption = "Ann"),
        ContentItem(id = 2L, name = "Two", caption = "Bob"),
    ),
)
