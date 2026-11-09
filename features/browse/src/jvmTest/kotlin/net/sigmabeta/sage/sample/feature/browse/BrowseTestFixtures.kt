package net.sigmabeta.sage.sample.feature.browse

import net.sigmabeta.sage.analytics.Analytics
import net.sigmabeta.sage.analytics.AnalyticsScreenId
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
