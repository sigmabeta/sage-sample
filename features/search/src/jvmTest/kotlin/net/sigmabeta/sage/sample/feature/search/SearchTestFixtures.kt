package net.sigmabeta.sage.sample.feature.search
import net.sigmabeta.sage.sample.models.SearchResult
import net.sigmabeta.sage.sample.repository.SearchResultKind
import net.sigmabeta.sage.sample.repository.SearchRepository
import net.sigmabeta.sage.sample.repository.demo.DemoContent

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import net.sigmabeta.sage.analytics.Analytics
import net.sigmabeta.sage.analytics.AnalyticsScreenId
import net.sigmabeta.sage.appcomm.LCE
import net.sigmabeta.sage.appcomm.SageAction
import net.sigmabeta.sage.appcomm.SageEvent
import net.sigmabeta.sage.sample.settings.DEFAULT_HOME_SHOWS_SAME_CONTENT
import net.sigmabeta.sage.sample.settings.DemoOutcome
import net.sigmabeta.sage.sample.settings.FontChoice
import net.sigmabeta.sage.sample.settings.SampleSettingsManager
import net.sigmabeta.sage.sample.settings.SearchHistoryManager
import net.sigmabeta.sage.sample.settings.SimulateDelay
import net.sigmabeta.sage.sample.settings.ThemeMode
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
 * Settings seam used by repository tests that read a preference at call time.
 *
 * Only the values those tests care about are mutable; the rest mirror the manager's defaults so
 * the sample's preferences have one fake vocabulary in this module.
 */
internal class FakeSampleSettingsManager(
    initialDemoOutcome: DemoOutcome = DemoOutcome.DEFAULT,
) : SampleSettingsManager {
    private val demoOutcomeState = MutableStateFlow(initialDemoOutcome)
    private val themeModeState = MutableStateFlow(ThemeMode.DEFAULT)
    private val fontChoiceState = MutableStateFlow(FontChoice.DEFAULT)
    private val simulateDelayState = MutableStateFlow(SimulateDelay.DEFAULT)
    private val homeShowsSameContentState = MutableStateFlow(DEFAULT_HOME_SHOWS_SAME_CONTENT)

    override val demoOutcome: StateFlow<DemoOutcome> = demoOutcomeState.asStateFlow()
    override val themeMode: StateFlow<ThemeMode> = themeModeState.asStateFlow()
    override val fontChoice: StateFlow<FontChoice> = fontChoiceState.asStateFlow()
    override val simulateDelay: StateFlow<SimulateDelay> = simulateDelayState.asStateFlow()
    override val homeShowsSameContent: StateFlow<Boolean> = homeShowsSameContentState.asStateFlow()

    override fun setDemoOutcome(outcome: DemoOutcome) {
        demoOutcomeState.value = outcome
    }

    override fun setThemeMode(mode: ThemeMode) {
        themeModeState.value = mode
    }

    override fun setFontChoice(choice: FontChoice) {
        fontChoiceState.value = choice
    }

    override fun setSimulateDelay(delay: SimulateDelay) {
        simulateDelayState.value = delay
    }

    override fun setHomeShowsSameContent(showSameContent: Boolean) {
        homeShowsSameContentState.value = showSameContent
    }
}

/**
 * Recent-search seam used by the view model tests.
 *
 * Records what the view model actually did, so the linger/cancellation tests assert on persisted
 * effects rather than on the timer mechanism.
 */
internal class FakeSearchHistoryManager(initialHistory: List<String> = emptyList()) : SearchHistoryManager {
    private val state = MutableStateFlow(initialHistory)

    override val history: StateFlow<List<String>> = state.asStateFlow()

    val recorded = mutableListOf<String>()
    val removed = mutableListOf<String>()
    var clearCount = 0
        private set

    override fun record(query: String) {
        recorded += query
        state.value = listOf(query) + state.value.filterNot { it == query }
    }

    override fun remove(query: String) {
        removed += query
        state.value = state.value.filterNot { it == query }
    }

    override fun clear() {
        clearCount++
        state.value = emptyList()
    }
}

/**
 * Search datasource seam used by the view model tests.
 *
 * By default it answers with the real seeded generators, so the view model test also exercises
 * the two-query-to-results path. The separate query lists record what actually reached each
 * repository method, which is how the debounce and minimum-length tests observe submissions.
 *
 * [listFlow] / [gridFlow] replace the default immediate answer when a test needs control over
 * timing (delays) or cancellation (a `finally` block), while still recording the submitted
 * query.
 */
internal class FakeSearchRepository(
    private val listResponse: (String) -> LCE<List<SearchResult>> = { query ->
        LCE.Content(DemoContent.searchResults(query, SearchResultKind.LIST))
    },
    private val gridResponse: (String) -> LCE<List<SearchResult>> = { query ->
        LCE.Content(DemoContent.searchResults(query, SearchResultKind.GRID))
    },
    private val listFlow: ((String) -> Flow<LCE<List<SearchResult>>>)? = null,
    private val gridFlow: ((String) -> Flow<LCE<List<SearchResult>>>)? = null,
) : SearchRepository {
    val listQueries = mutableListOf<String>()
    val gridQueries = mutableListOf<String>()

    override fun searchList(query: String): Flow<LCE<List<SearchResult>>> {
        listQueries += query
        return listFlow?.invoke(query) ?: flow { emit(listResponse(query)) }
    }

    override fun searchGrid(query: String): Flow<LCE<List<SearchResult>>> {
        gridQueries += query
        return gridFlow?.invoke(query) ?: flow { emit(gridResponse(query)) }
    }
}
