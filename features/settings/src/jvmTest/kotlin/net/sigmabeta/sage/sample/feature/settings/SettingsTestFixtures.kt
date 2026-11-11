package net.sigmabeta.sage.sample.feature.settings

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.sigmabeta.sage.analytics.Analytics
import net.sigmabeta.sage.analytics.AnalyticsScreenId
import net.sigmabeta.sage.appcomm.SageAction
import net.sigmabeta.sage.appcomm.SageEvent
import net.sigmabeta.sage.appinfo.AppInfo
import net.sigmabeta.sage.sample.settings.DEFAULT_HOME_SHOWS_SAME_CONTENT
import net.sigmabeta.sage.sample.settings.AnimationScale
import net.sigmabeta.sage.sample.settings.DemoDataResetter
import net.sigmabeta.sage.sample.settings.DemoOutcome
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
    initialAnimationScale: AnimationScale = AnimationScale.DEFAULT,
    initialDemoOutcome: DemoOutcome = DemoOutcome.DEFAULT,
    initialHomeShowsSameContent: Boolean = DEFAULT_HOME_SHOWS_SAME_CONTENT,
) : SampleSettingsManager {
    private val theme = MutableStateFlow(initialThemeMode)
    private val font = MutableStateFlow(initialFontChoice)
    private val delay = MutableStateFlow(initialSimulateDelay)
    private val animation = MutableStateFlow(initialAnimationScale)
    private val demo = MutableStateFlow(initialDemoOutcome)
    private val homeSameContent = MutableStateFlow(initialHomeShowsSameContent)

    val setThemeModeCalls: MutableList<ThemeMode> = mutableListOf()
    val setFontChoiceCalls: MutableList<FontChoice> = mutableListOf()
    val setSimulateDelayCalls: MutableList<SimulateDelay> = mutableListOf()
    val setAnimationScaleCalls: MutableList<AnimationScale> = mutableListOf()
    val setDemoOutcomeCalls: MutableList<DemoOutcome> = mutableListOf()
    val setHomeShowsSameContentCalls: MutableList<Boolean> = mutableListOf()

    override val themeMode: StateFlow<ThemeMode> = theme.asStateFlow()
    override val fontChoice: StateFlow<FontChoice> = font.asStateFlow()
    override val simulateDelay: StateFlow<SimulateDelay> = delay.asStateFlow()
    override val animationScale: StateFlow<AnimationScale> = animation.asStateFlow()
    override val demoOutcome: StateFlow<DemoOutcome> = demo.asStateFlow()
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

    override fun setAnimationScale(scale: AnimationScale) {
        setAnimationScaleCalls += scale
        animation.value = scale
    }

    override fun setDemoOutcome(outcome: DemoOutcome) {
        setDemoOutcomeCalls += outcome
        demo.value = outcome
    }

    override fun setHomeShowsSameContent(showSameContent: Boolean) {
        setHomeShowsSameContentCalls += showSameContent
        homeSameContent.value = showSameContent
    }
}

/**
 * A controllable [DemoDataResetter]: counts calls and can hold the reset open or make it fail.
 *
 * [gate] lets a test keep the reset in flight while it dispatches a second one; [failure] is
 * thrown after the gate opens.
 */
internal class FakeDemoDataResetter(
    private val gate: CompletableDeferred<Unit>? = null,
    private val failure: Throwable? = null,
) : DemoDataResetter {
    var resetCalls: Int = 0
        private set

    override suspend fun reset() {
        resetCalls += 1
        gate?.await()
        failure?.let { throw it }
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
