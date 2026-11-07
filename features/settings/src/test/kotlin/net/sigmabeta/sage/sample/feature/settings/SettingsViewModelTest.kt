package net.sigmabeta.sage.sample.feature.settings

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.sigmabeta.sage.components.CheckableListModel
import net.sigmabeta.sage.components.DropdownSettingListModel
import net.sigmabeta.sage.components.LabelValueListModel
import net.sigmabeta.sage.components.ListModel
import net.sigmabeta.sage.coroutines.SageDispatchers
import net.sigmabeta.sage.sample.settings.DemoOutcome
import net.sigmabeta.sage.sample.settings.FontChoice
import net.sigmabeta.sage.sample.settings.SampleDelayManager
import net.sigmabeta.sage.sample.settings.SimulateDelay
import net.sigmabeta.sage.sample.settings.ThemeMode
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
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
    fun `initial state mirrors the persisted values and app info`() = runTest(dispatcher) {
        val settings = FakeSettingsManager(
            initialThemeMode = ThemeMode.DARK,
            initialFontChoice = FontChoice.INTER,
            initialSimulateDelay = SimulateDelay.ONE_SECOND,
            initialDemoOutcome = DemoOutcome.ERROR,
            initialHomeShowsSameContent = true,
        )
        val viewModel = createViewModel(settings = settings, appInfo = testAppInfo(versionName = "9.9.9"))

        val items = viewModel.uiStateActual.value.listItems

        assertEquals(ThemeMode.DARK.ordinal, themeDropdown(items).selectedPosition)
        assertEquals(FontChoice.INTER.ordinal, fontDropdown(items).selectedPosition)
        assertEquals(SimulateDelay.ONE_SECOND.ordinal, delayDropdown(items).selectedPosition)
        assertEquals(DemoOutcome.ERROR.ordinal, demoOutcomeDropdown(items).selectedPosition)
        assertEquals(true, homeShowsSameContentToggle(items).checked)
        assertEquals("9.9.9", items.filterIsInstance<LabelValueListModel>()[0].value)
    }

    @Test
    fun `selecting a theme writes through and collapses the dropdown`() = runTest(dispatcher) {
        val settings = FakeSettingsManager()
        val viewModel = createViewModel(settings = settings)

        viewModel.sendAction(SettingsAction.DropdownExpandClicked(THEME_SETTING_ID))
        advanceUntilIdle()
        assertTrue(themeDropdown(viewModel.uiStateActual.value.listItems).expanded)

        viewModel.sendAction(SettingsAction.ThemeModeSelected(ThemeMode.LIGHT))
        advanceUntilIdle()

        assertEquals(listOf(ThemeMode.LIGHT), settings.setThemeModeCalls)
        assertEquals(ThemeMode.LIGHT.ordinal, themeDropdown(viewModel.uiStateActual.value.listItems).selectedPosition)
        assertFalse(themeDropdown(viewModel.uiStateActual.value.listItems).expanded)
    }

    @Test
    fun `selecting a font writes through and collapses the dropdown`() = runTest(dispatcher) {
        val settings = FakeSettingsManager()
        val viewModel = createViewModel(settings = settings)

        viewModel.sendAction(SettingsAction.DropdownExpandClicked(FONT_SETTING_ID))
        advanceUntilIdle()
        assertTrue(fontDropdown(viewModel.uiStateActual.value.listItems).expanded)

        viewModel.sendAction(SettingsAction.FontChoiceSelected(FontChoice.INTER))
        advanceUntilIdle()

        assertEquals(listOf(FontChoice.INTER), settings.setFontChoiceCalls)
        assertEquals(FontChoice.INTER.ordinal, fontDropdown(viewModel.uiStateActual.value.listItems).selectedPosition)
        assertFalse(fontDropdown(viewModel.uiStateActual.value.listItems).expanded)
    }

    @Test
    fun `selecting a delay writes through and collapses the dropdown`() = runTest(dispatcher) {
        val settings = FakeSettingsManager()
        val viewModel = createViewModel(settings = settings)

        viewModel.sendAction(SettingsAction.DropdownExpandClicked(DELAY_SETTING_ID))
        advanceUntilIdle()

        viewModel.sendAction(SettingsAction.SimulateDelaySelected(SimulateDelay.FIVE_SECONDS))
        advanceUntilIdle()

        assertEquals(listOf(SimulateDelay.FIVE_SECONDS), settings.setSimulateDelayCalls)
        assertEquals(SimulateDelay.FIVE_SECONDS.ordinal, delayDropdown(viewModel.uiStateActual.value.listItems).selectedPosition)
        assertFalse(delayDropdown(viewModel.uiStateActual.value.listItems).expanded)
    }

    @Test
    fun `selecting a demo outcome writes through and collapses the dropdown`() = runTest(dispatcher) {
        val settings = FakeSettingsManager()
        val viewModel = createViewModel(settings = settings)

        viewModel.sendAction(SettingsAction.DropdownExpandClicked(DEMO_OUTCOME_SETTING_ID))
        advanceUntilIdle()
        assertTrue(demoOutcomeDropdown(viewModel.uiStateActual.value.listItems).expanded)

        viewModel.sendAction(SettingsAction.DemoOutcomeSelected(DemoOutcome.EMPTY))
        advanceUntilIdle()

        assertEquals(listOf(DemoOutcome.EMPTY), settings.setDemoOutcomeCalls)
        assertEquals(
            DemoOutcome.EMPTY.ordinal,
            demoOutcomeDropdown(viewModel.uiStateActual.value.listItems).selectedPosition,
        )
        assertFalse(demoOutcomeDropdown(viewModel.uiStateActual.value.listItems).expanded)
    }

    @Test
    fun `toggling home shows same content writes through`() = runTest(dispatcher) {
        val settings = FakeSettingsManager(initialHomeShowsSameContent = false)
        val viewModel = createViewModel(settings = settings)

        assertFalse(homeShowsSameContentToggle(viewModel.uiStateActual.value.listItems).checked!!)

        viewModel.sendAction(SettingsAction.HomeShowsSameContentToggled)
        advanceUntilIdle()

        assertEquals(listOf(true), settings.setHomeShowsSameContentCalls)
        assertTrue(homeShowsSameContentToggle(viewModel.uiStateActual.value.listItems).checked!!)
    }

    @Test
    fun `expanding another dropdown replaces the open one and re-tapping closes it`() = runTest(dispatcher) {
        val viewModel = createViewModel(settings = FakeSettingsManager())

        viewModel.sendAction(SettingsAction.DropdownExpandClicked(THEME_SETTING_ID))
        advanceUntilIdle()
        viewModel.sendAction(SettingsAction.DropdownExpandClicked(DELAY_SETTING_ID))
        advanceUntilIdle()

        assertFalse(themeDropdown(viewModel.uiStateActual.value.listItems).expanded)
        assertTrue(delayDropdown(viewModel.uiStateActual.value.listItems).expanded)

        viewModel.sendAction(SettingsAction.DropdownExpandClicked(DELAY_SETTING_ID))
        advanceUntilIdle()

        assertFalse(delayDropdown(viewModel.uiStateActual.value.listItems).expanded)
    }

    private fun createViewModel(
        settings: FakeSettingsManager,
        appInfo: net.sigmabeta.sage.appinfo.AppInfo = testAppInfo(),
    ) = SettingsViewModel(
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
        settingsManager = settings,
        appInfo = appInfo,
    )

    private fun themeDropdown(items: List<ListModel>): DropdownSettingListModel =
        items.filterIsInstance<DropdownSettingListModel>()
            .single { it.name == SettingsStringId.SETTINGS_LABEL_THEME.toString() }

    private fun fontDropdown(items: List<ListModel>): DropdownSettingListModel =
        items.filterIsInstance<DropdownSettingListModel>()
            .single { it.name == SettingsStringId.SETTINGS_LABEL_FONT.toString() }

    private fun delayDropdown(items: List<ListModel>): DropdownSettingListModel =
        items.filterIsInstance<DropdownSettingListModel>()
            .single { it.name == SettingsStringId.SETTINGS_LABEL_SIMULATE_DELAY.toString() }

    private fun demoOutcomeDropdown(items: List<ListModel>): DropdownSettingListModel =
        items.filterIsInstance<DropdownSettingListModel>()
            .single { it.name == SettingsStringId.SETTINGS_LABEL_DEMO_OUTCOME.toString() }

    private fun homeShowsSameContentToggle(items: List<ListModel>): CheckableListModel =
        items.filterIsInstance<CheckableListModel>()
            .single {
                it.name == SettingsStringId.SETTINGS_LABEL_HOME_SHOWS_SAME_CONTENT.toString()
            }

    private companion object {
        val THEME_SETTING_ID = SettingsStringId.SETTINGS_LABEL_THEME.name
        val FONT_SETTING_ID = SettingsStringId.SETTINGS_LABEL_FONT.name
        val DELAY_SETTING_ID = SettingsStringId.SETTINGS_LABEL_SIMULATE_DELAY.name
        val DEMO_OUTCOME_SETTING_ID = SettingsStringId.SETTINGS_LABEL_DEMO_OUTCOME.name
    }
}
