package net.sigmabeta.sage.sample.feature.componentlibrary

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.sigmabeta.sage.coroutines.SageDispatchers
import net.sigmabeta.sage.list.ColumnType
import net.sigmabeta.sage.sample.settings.SampleDelayManager
import net.sigmabeta.sage.ui.componentlibrary.ComponentLibraryAction
import net.sigmabeta.sage.ui.componentlibrary.ComponentLibraryLayout
import net.sigmabeta.sage.ui.componentlibrary.StyleToken
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ComponentLibraryViewModelTest {
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
    fun `a layout action changes the config and the reported column type`() = runTest(dispatcher) {
        val viewModel = createViewModel()

        viewModel.sendAction(ComponentLibraryAction.SetLayout(ComponentLibraryLayout.GRID))
        advanceUntilIdle()

        assertEquals(ColumnType.Regular(160), viewModel.uiStateActual.value.columnType)
    }

    @Test
    fun `a style action reaches the exposed style`() = runTest(dispatcher) {
        val viewModel = createViewModel()

        viewModel.sendAction(ComponentLibraryAction.SetStyle(StyleToken.HERO_CORNER, 24))
        advanceUntilIdle()

        assertEquals(24, viewModel.style.value.heroCornerRadiusDp)
    }

    @Test
    fun `toggling a dropdown expands it in the rendered rows`() = runTest(dispatcher) {
        val viewModel = createViewModel()
        val before = viewModel.uiStateActual.value.listItems

        viewModel.sendAction(ComponentLibraryAction.ToggleDropdown("cl-layout"))
        advanceUntilIdle()

        assertFalse("the expanded dropdown changes the rows", before == viewModel.uiStateActual.value.listItems)
    }

    private fun createViewModel() = ComponentLibraryViewModel(
        stringProvider = FakeStringProvider,
        analytics = FakeAnalytics,
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
