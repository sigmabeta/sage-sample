package net.sigmabeta.sage.sample.feature.settings
import net.sigmabeta.sage.sample.strings.api.SampleStringId

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.sigmabeta.sage.appcomm.SageEvent
import net.sigmabeta.sage.components.DropdownSettingListModel
import net.sigmabeta.sage.components.LabelValueListModel
import net.sigmabeta.sage.components.ListModel
import net.sigmabeta.sage.components.LoadingItemListModel
import net.sigmabeta.sage.components.NameCaptionCheckboxListModel
import net.sigmabeta.sage.components.NameCaptionListModel
import net.sigmabeta.sage.coroutines.SageDispatchers
import net.sigmabeta.sage.sample.settings.AnimationScale
import net.sigmabeta.sage.sample.settings.DemoDataResetter
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
            initialAnimationScale = AnimationScale.ONE_X,
            initialDemoOutcome = DemoOutcome.ERROR,
            initialHomeShowsSameContent = true,
        )
        val viewModel = createViewModel(settings = settings, appInfo = testAppInfo(versionName = "9.9.9"))

        val items = viewModel.uiStateActual.value.listItems

        assertEquals(ThemeMode.DARK.ordinal, themeDropdown(items).selectedPosition)
        assertEquals(FontChoice.INTER.ordinal, fontDropdown(items).selectedPosition)
        assertEquals(SimulateDelay.ONE_SECOND.ordinal, delayDropdown(items).selectedPosition)
        assertEquals(AnimationScale.ONE_X.ordinal, animationScaleDropdown(items).selectedPosition)
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
    fun `selecting an animation scale writes through and collapses the dropdown`() = runTest(dispatcher) {
        val settings = FakeSettingsManager()
        val viewModel = createViewModel(settings = settings)

        viewModel.sendAction(SettingsAction.DropdownExpandClicked(ANIMATION_SCALE_SETTING_ID))
        advanceUntilIdle()
        assertTrue(animationScaleDropdown(viewModel.uiStateActual.value.listItems).expanded)

        viewModel.sendAction(SettingsAction.AnimationScaleSelected(AnimationScale.FIVE_X))
        advanceUntilIdle()

        assertEquals(listOf(AnimationScale.FIVE_X), settings.setAnimationScaleCalls)
        assertEquals(
            AnimationScale.FIVE_X.ordinal,
            animationScaleDropdown(viewModel.uiStateActual.value.listItems).selectedPosition,
        )
        assertFalse(animationScaleDropdown(viewModel.uiStateActual.value.listItems).expanded)
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

    @Test
    fun `resetting demo data runs the resetter and reports success`() = runTest(dispatcher) {
        val resetter = FakeDemoDataResetter()
        val viewModel = createViewModel(resetter = resetter)
        val events = mutableListOf<SageEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiEvents.collect { events += it }
        }

        viewModel.sendAction(SettingsAction.ResetDemoDataClicked)
        advanceUntilIdle()

        assertEquals(1, resetter.resetCalls)
        val snackbar = events.filterIsInstance<SageEvent.ShowSnackbar>().single()
        assertEquals(SampleStringId.SETTINGS_RESET_DEMO_DATA_SUCCESS.toString(), snackbar.message)
        assertEquals(RESET_ROW_ACTION, resetDemoDataRow(viewModel.uiStateActual.value.listItems).clickAction)
        assertTrue(viewModel.uiStateActual.value.listItems.none { it is LoadingItemListModel })
    }

    @Test
    fun `a failed reset reports failure and leaves the row actionable`() = runTest(dispatcher) {
        val resetter = FakeDemoDataResetter(failure = IllegalStateException("storage unavailable"))
        val viewModel = createViewModel(resetter = resetter)
        val events = mutableListOf<SageEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiEvents.collect { events += it }
        }

        viewModel.sendAction(SettingsAction.ResetDemoDataClicked)
        advanceUntilIdle()

        assertEquals(1, resetter.resetCalls)
        val snackbar = events.filterIsInstance<SageEvent.ShowSnackbar>().single()
        assertEquals(SampleStringId.SETTINGS_RESET_DEMO_DATA_FAILURE.toString(), snackbar.message)
        assertEquals(RESET_ROW_ACTION, resetDemoDataRow(viewModel.uiStateActual.value.listItems).clickAction)
        assertTrue(viewModel.uiStateActual.value.listItems.none { it is LoadingItemListModel })
    }

    @Test
    fun `a second reset while one is running is ignored`() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        val resetter = FakeDemoDataResetter(gate = gate)
        val viewModel = createViewModel(resetter = resetter)
        val events = mutableListOf<SageEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiEvents.collect { events += it }
        }

        viewModel.sendAction(SettingsAction.ResetDemoDataClicked)
        advanceUntilIdle()
        assertTrue(viewModel.uiStateActual.value.listItems.any { it is LoadingItemListModel })

        viewModel.sendAction(SettingsAction.ResetDemoDataClicked)
        advanceUntilIdle()
        assertEquals(1, resetter.resetCalls)

        gate.complete(Unit)
        advanceUntilIdle()

        assertEquals(1, resetter.resetCalls)
        assertEquals(1, events.filterIsInstance<SageEvent.ShowSnackbar>().size)
        assertTrue(viewModel.uiStateActual.value.listItems.none { it is LoadingItemListModel })
    }

    private fun createViewModel(
        settings: FakeSettingsManager = FakeSettingsManager(),
        appInfo: net.sigmabeta.sage.appinfo.AppInfo = testAppInfo(),
        resetter: DemoDataResetter = FakeDemoDataResetter(),
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
        demoDataResetter = resetter,
        appInfo = appInfo,
    )

    private fun themeDropdown(items: List<ListModel>): DropdownSettingListModel =
        items.filterIsInstance<DropdownSettingListModel>()
            .single { it.name == SampleStringId.SETTINGS_LABEL_THEME.toString() }

    private fun fontDropdown(items: List<ListModel>): DropdownSettingListModel =
        items.filterIsInstance<DropdownSettingListModel>()
            .single { it.name == SampleStringId.SETTINGS_LABEL_FONT.toString() }

    private fun delayDropdown(items: List<ListModel>): DropdownSettingListModel =
        items.filterIsInstance<DropdownSettingListModel>()
            .single { it.name == SampleStringId.SETTINGS_LABEL_SIMULATE_DELAY.toString() }

    private fun animationScaleDropdown(items: List<ListModel>): DropdownSettingListModel =
        items.filterIsInstance<DropdownSettingListModel>()
            .single { it.name == SampleStringId.SETTINGS_LABEL_ANIMATION_SCALE.toString() }

    private fun demoOutcomeDropdown(items: List<ListModel>): DropdownSettingListModel =
        items.filterIsInstance<DropdownSettingListModel>()
            .single { it.name == SampleStringId.SETTINGS_LABEL_DEMO_OUTCOME.toString() }

    private fun homeShowsSameContentToggle(items: List<ListModel>): NameCaptionCheckboxListModel =
        items.filterIsInstance<NameCaptionCheckboxListModel>()
            .single {
                it.name == SampleStringId.SETTINGS_LABEL_HOME_SHOWS_SAME_CONTENT.toString()
            }

    private fun resetDemoDataRow(items: List<ListModel>): NameCaptionListModel =
        items.filterIsInstance<NameCaptionListModel>()
            .single { it.name == SampleStringId.SETTINGS_LABEL_RESET_DEMO_DATA.toString() }

    private companion object {
        val THEME_SETTING_ID = SampleStringId.SETTINGS_LABEL_THEME.name
        val FONT_SETTING_ID = SampleStringId.SETTINGS_LABEL_FONT.name
        val DELAY_SETTING_ID = SampleStringId.SETTINGS_LABEL_SIMULATE_DELAY.name
        val ANIMATION_SCALE_SETTING_ID = SampleStringId.SETTINGS_LABEL_ANIMATION_SCALE.name
        val DEMO_OUTCOME_SETTING_ID = SampleStringId.SETTINGS_LABEL_DEMO_OUTCOME.name
        val RESET_ROW_ACTION = SettingsAction.ResetDemoDataClicked
    }
}
