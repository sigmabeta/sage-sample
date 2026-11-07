package net.sigmabeta.sage.sample.feature.settings

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.sigmabeta.sage.analytics.Analytics
import net.sigmabeta.sage.analytics.AnalyticsScreenId
import net.sigmabeta.sage.appcomm.SageAction
import net.sigmabeta.sage.appcomm.SageEvent
import net.sigmabeta.sage.appinfo.AppInfo
import net.sigmabeta.sage.sample.settings.DEFAULT_HOME_SHOWS_SAME_CONTENT
import net.sigmabeta.sage.sample.settings.FontChoice
import net.sigmabeta.sage.sample.settings.SampleSettingsManager
import net.sigmabeta.sage.sample.settings.SimulateDelay
import net.sigmabeta.sage.sample.settings.ThemeMode
import net.sigmabeta.sage.ui.SageStringId
import net.sigmabeta.sage.ui.StringProvider

/** Renders every id as its name so tests can identify rows without Android resources. */
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

/** In-memory settings with the same defaults as the Storage-backed implementation. */
internal class FakeSettingsManager(
    initialThemeMode: ThemeMode = ThemeMode.DEFAULT,
    initialFontChoice: FontChoice = FontChoice.DEFAULT,
    initialSimulateDelay: SimulateDelay = SimulateDelay.DEFAULT,
    initialHomeShowsSameContent: Boolean = DEFAULT_HOME_SHOWS_SAME_CONTENT,
) : SampleSettingsManager {
    private val theme = MutableStateFlow(initialThemeMode)
    private val font = MutableStateFlow(initialFontChoice)
    private val delay = MutableStateFlow(initialSimulateDelay)
    private val homeSameContent = MutableStateFlow(initialHomeShowsSameContent)

    val setThemeModeCalls: MutableList<ThemeMode> = mutableListOf()
    val setFontChoiceCalls: MutableList<FontChoice> = mutableListOf()
    val setSimulateDelayCalls: MutableList<SimulateDelay> = mutableListOf()
    val setHomeShowsSameContentCalls: MutableList<Boolean> = mutableListOf()

    override val themeMode: StateFlow<ThemeMode> = theme.asStateFlow()
    override val fontChoice: StateFlow<FontChoice> = font.asStateFlow()
    override val simulateDelay: StateFlow<SimulateDelay> = delay.asStateFlow()
    override val homeShowsSameContent: StateFlow<Boolean> = homeSameContent.asStateFlow()

    override fun setThemeMode(mode: ThemeMode) {
        setThemeModeCalls += mode
        theme.value = mode
    }

    override fun setFontChoice(choice: FontChoice) {
        setFontChoiceCalls += choice
        font.value = choice
    }

    override fun setSimulateDelay(delay: SimulateDelay) {
        setSimulateDelayCalls += delay
        this.delay.value = delay
    }

    override fun setHomeShowsSameContent(showSameContent: Boolean) {
        setHomeShowsSameContentCalls += showSameContent
        homeSameContent.value = showSameContent
    }
}

internal fun testAppInfo(
    versionName: String = "0.1.0",
    buildTimeMs: Long? = null,
): AppInfo = AppInfo(
    isDebug = true,
    versionName = versionName,
    versionCode = 1,
    buildTimeMs = buildTimeMs,
    buildBranch = "test",
)
