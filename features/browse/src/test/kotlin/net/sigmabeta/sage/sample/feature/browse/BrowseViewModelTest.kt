package net.sigmabeta.sage.sample.feature.browse

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.sigmabeta.sage.appcomm.SageEvent
import net.sigmabeta.sage.components.LoadingItemListModel
import net.sigmabeta.sage.components.SingleTextListModel
import net.sigmabeta.sage.coroutines.SageDispatchers
import net.sigmabeta.sage.sample.settings.SampleDelayManager
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BrowseViewModelTest {
    private lateinit var dispatcher: TestDispatcher

    @Before
    fun setUp() {
        dispatcher = StandardTestDispatcher()
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `the screen renders its destinations without a loading state`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        val rows = viewModel.uiStateActual.value.listItems

        assertEquals(4, rows.size)
        assertTrue(rows.all { it is SingleTextListModel })
        assertTrue(rows.none { it is LoadingItemListModel })
    }

    @Test
    fun `each destination row emits its navigation route`() = runTest {
        val viewModel = createViewModel()
        val events = mutableListOf<SageEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiEvents.collect { event -> events += event }
        }
        advanceUntilIdle()

        viewModel.sendAction(BrowseAction.OpenListItems)
        viewModel.sendAction(BrowseAction.OpenListItemsWithImages)
        viewModel.sendAction(BrowseAction.OpenGridOfImages)
        viewModel.sendAction(BrowseAction.OpenContentDetail)
        advanceUntilIdle()

        val navigation = events.filterIsInstance<SageEvent.NavigateTo>()
        assertEquals(
            listOf(
                "list-items",
                "list-items-with-images",
                "grid-of-images",
                "content-detail/browse:featured",
            ),
            navigation.map { it.destination },
        )
        assertEquals("content-detail/browse:featured", navigation.last().destination)
        assertTrue(navigation.all { it.source == "BrowseViewModel" })
    }

    private fun createViewModel() = BrowseViewModel(
        stringProvider = FakeStringProvider(),
        analytics = FakeAnalytics(),
        dispatchers = SageDispatchers(
            computation = dispatcher,
            disk = dispatcher,
            network = dispatcher,
            main = dispatcher,
        ),
        delayManager = object : SampleDelayManager {
            override fun delayMillis() = 0L
        },
    )
}
