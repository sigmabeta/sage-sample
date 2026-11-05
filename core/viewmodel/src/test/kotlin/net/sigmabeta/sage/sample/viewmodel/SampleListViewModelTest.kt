package net.sigmabeta.sage.sample.viewmodel

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.sigmabeta.sage.analytics.Analytics
import net.sigmabeta.sage.analytics.AnalyticsScreen
import net.sigmabeta.sage.analytics.AnalyticsScreenId
import net.sigmabeta.sage.appcomm.SageAction
import net.sigmabeta.sage.appcomm.SageEvent
import net.sigmabeta.sage.components.ListModel
import net.sigmabeta.sage.components.TitleBarModel
import net.sigmabeta.sage.coroutines.SageDispatchers
import net.sigmabeta.sage.sample.settings.SampleDelayManager
import net.sigmabeta.sage.list.ListState
import net.sigmabeta.sage.ui.SageStringId
import net.sigmabeta.sage.ui.StringProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * The shared Resume/Pause policy.
 *
 * The app screen owns the foreground pair, and SAGE's list/grid screens also emit Resume when the
 * title changes. Coalescing in [SampleListViewModel.sendAction] is what keeps feature side effects
 * to once per foreground entry.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SampleListViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `repeated resume and pause actions only reach handleAction on transitions`() =
        runTest(dispatcher) {
            val viewModel = TestViewModel(dispatcher)

            viewModel.sendAction(SageAction.Resume)
            viewModel.sendAction(SageAction.Resume)
            viewModel.sendAction(SageAction.Pause)
            viewModel.sendAction(SageAction.Pause)
            advanceUntilIdle()

            assertEquals(listOf(SageAction.Resume, SageAction.Pause), viewModel.handled)
            assertEquals(2, viewModel.analytics.loggedActions)
        }

    @Test
    fun `resume after pause is a fresh transition`() = runTest(dispatcher) {
        val viewModel = TestViewModel(dispatcher)

        viewModel.sendAction(SageAction.Resume)
        viewModel.sendAction(SageAction.Pause)
        viewModel.sendAction(SageAction.Resume)
        advanceUntilIdle()

        assertEquals(
            listOf(SageAction.Resume, SageAction.Pause, SageAction.Resume),
            viewModel.handled,
        )
    }

    @Test
    fun `actions other than resume and pause are never coalesced`() = runTest(dispatcher) {
        val viewModel = TestViewModel(dispatcher)

        viewModel.sendAction(SageAction.Noop)
        viewModel.sendAction(SageAction.Noop)
        advanceUntilIdle()

        assertEquals(listOf(SageAction.Noop, SageAction.Noop), viewModel.handled)
        assertEquals(2, viewModel.analytics.loggedActions)
    }
}

private class TestViewModel(
    dispatcher: CoroutineDispatcher,
) : SampleListViewModel<TestState>() {
    override val stringProvider = NoopStringProvider

    // Widened so the test can assert which actions were logged.
    public override val analytics = CountingAnalytics()
    override val dispatchers = SageDispatchers(
        computation = dispatcher,
        disk = dispatcher,
        network = dispatcher,
        main = dispatcher,
    )
    override val delayManager = object : SampleDelayManager {
        override fun delayMillis() = 0L
    }

    val handled = mutableListOf<SageAction>()

    override val screenIdentifier: AnalyticsScreenId = AnalyticsScreen.NONE

    override fun initialState(): TestState = TestState

    override fun handleAction(action: SageAction) {
        handled += action
    }
}

private object TestState : ListState() {
    override fun title(stringProvider: StringProvider): TitleBarModel = TitleBarModel()

    override fun toListItems(stringProvider: StringProvider): List<ListModel> = emptyList()
}

private object NoopStringProvider : StringProvider {
    override fun getString(string: SageStringId): String = string.toString()

    override fun getStringOneArg(string: SageStringId, arg: String): String = string.toString()

    override fun getStringOneInt(string: SageStringId, arg: Int): String = string.toString()

    override fun getStringTwoArgs(string: SageStringId, first: String, second: String): String =
        string.toString()
}

private class CountingAnalytics : Analytics {
    var loggedActions = 0
        private set

    override fun logScreenView(action: SageAction, screen: AnalyticsScreenId) = Unit

    override fun logAction(action: SageAction, fromScreen: AnalyticsScreenId) {
        loggedActions += 1
    }

    override fun logEvent(event: SageEvent) = Unit

    override fun logAutoRefresh() = Unit

    override fun logError(failedOperationName: String, errorString: String, error: Throwable) = Unit
}
