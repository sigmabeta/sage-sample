package net.sigmabeta.sage.sample.feature.componentlibrary

import net.sigmabeta.sage.analytics.Analytics
import net.sigmabeta.sage.analytics.AnalyticsScreenId
import net.sigmabeta.sage.appcomm.SageAction
import net.sigmabeta.sage.appcomm.SageEvent
import net.sigmabeta.sage.ui.SageStringId
import net.sigmabeta.sage.ui.StringProvider

/** Renders every id as its name so tests can identify rows without a string table. */
internal object FakeStringProvider : StringProvider {
    override fun getString(string: SageStringId): String = string.toString()
    override fun getStringOneArg(string: SageStringId, arg: String): String = string.toString()
    override fun getStringOneInt(string: SageStringId, arg: Int): String = string.toString()
    override fun getStringTwoArgs(string: SageStringId, first: String, second: String): String =
        string.toString()
}

internal object FakeAnalytics : Analytics {
    override fun logScreenView(action: SageAction, screen: AnalyticsScreenId) = Unit
    override fun logAction(action: SageAction, fromScreen: AnalyticsScreenId) = Unit
    override fun logEvent(event: SageEvent) = Unit
    override fun logAutoRefresh() = Unit
    override fun logError(failedOperationName: String, errorString: String, error: Throwable) = Unit
}
