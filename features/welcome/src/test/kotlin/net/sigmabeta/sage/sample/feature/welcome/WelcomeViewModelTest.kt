package net.sigmabeta.sage.sample.feature.welcome

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.sigmabeta.sage.analytics.Analytics
import net.sigmabeta.sage.analytics.AnalyticsScreenId
import net.sigmabeta.sage.appcomm.SageAction
import net.sigmabeta.sage.appcomm.SageEvent
import net.sigmabeta.sage.components.SingleTextListModel
import net.sigmabeta.sage.coroutines.SageDispatchers
import net.sigmabeta.sage.list.DelayManager
import net.sigmabeta.sage.ui.SageStringId
import net.sigmabeta.sage.ui.StringProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * The navigation contract a Welcome row shares with the app: the row carries a typed feature
 * action, and handling it emits exactly one one-shot [SageEvent.NavigateTo] whose destination is
 * the route the app maps with `SampleDestination.fromRoute`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WelcomeViewModelTest {
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
    fun `open browse action emits exactly one navigate-to event for browse`() = runTest(dispatcher) {
        val viewModel = createViewModel()
        val events = mutableListOf<SageEvent>()
        val collector = launch { viewModel.uiEvents.collect { events += it } }

        viewModel.sendAction(WelcomeAction.OpenBrowse)
        advanceUntilIdle()

        assertEquals(
            listOf(SageEvent.NavigateTo(destination = "browse", source = "WelcomeViewModel")),
            events,
        )

        collector.cancel()
    }

    @Test
    fun `welcome offers an actionable browse row with a stable id`() {
        val row = WelcomeState.toListItems(FakeStringProvider)
            .filterIsInstance<SingleTextListModel>()
            .single { it.clickAction is WelcomeAction.OpenBrowse }

        assertEquals(WelcomeStringId.WELCOME_OPEN_BROWSE.toString(), row.name)
        assertEquals(3L, row.dataId)
    }

    private fun createViewModel() = WelcomeViewModel(
        stringProvider = FakeStringProvider,
        analytics = FakeAnalytics,
        dispatchers = SageDispatchers(
            computation = dispatcher,
            disk = dispatcher,
            network = dispatcher,
            main = dispatcher,
        ),
        delayManager = object : DelayManager {
            override fun shouldDelay() = false
        },
    )
}

/** Renders every id as its name so tests can identify rows without Android resources. */
private object FakeStringProvider : StringProvider {
    override fun getString(string: SageStringId): String = string.toString()
    override fun getStringOneArg(string: SageStringId, arg: String): String = string.toString()
    override fun getStringOneInt(string: SageStringId, arg: Int): String = string.toString()
    override fun getStringTwoArgs(string: SageStringId, first: String, second: String): String =
        string.toString()
}

private object FakeAnalytics : Analytics {
    override fun logScreenView(action: SageAction, screen: AnalyticsScreenId) = Unit
    override fun logAction(action: SageAction, fromScreen: AnalyticsScreenId) = Unit
    override fun logEvent(event: SageEvent) = Unit
    override fun logAutoRefresh() = Unit
    override fun logError(failedOperationName: String, errorString: String, error: Throwable) = Unit
}
