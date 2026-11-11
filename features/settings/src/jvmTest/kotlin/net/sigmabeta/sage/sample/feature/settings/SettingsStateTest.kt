package net.sigmabeta.sage.sample.feature.settings
import net.sigmabeta.sage.sample.strings.api.SampleStringId

import net.sigmabeta.sage.appcomm.LCE
import net.sigmabeta.sage.appcomm.SageAction
import net.sigmabeta.sage.components.DropdownSettingListModel
import net.sigmabeta.sage.components.LabelValueListModel
import net.sigmabeta.sage.components.ListModel
import net.sigmabeta.sage.components.LoadingItemListModel
import net.sigmabeta.sage.components.LoadingType
import net.sigmabeta.sage.components.NameCaptionCheckboxListModel
import net.sigmabeta.sage.components.NameCaptionListModel
import net.sigmabeta.sage.components.SectionHeaderListModel
import net.sigmabeta.sage.sample.settings.AnimationScale
import net.sigmabeta.sage.sample.settings.DemoOutcome
import net.sigmabeta.sage.sample.settings.FontChoice
import net.sigmabeta.sage.sample.settings.SimulateDelay
import net.sigmabeta.sage.sample.settings.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsStateTest {
    @Test
    fun `sections and rows appear in a stable order`() {
        val items = SettingsState().toListItems(FakeStringProvider)

        assertEquals(
            listOf(
                SectionHeaderListModel(SampleStringId.SETTINGS_SECTION_APPEARANCE.toString()),
                themeDropdown(items),
                fontDropdown(items),
                SectionHeaderListModel(SampleStringId.SETTINGS_SECTION_SIMULATION.toString()),
                delayDropdown(items),
                animationScaleDropdown(items),
                demoOutcomeDropdown(items),
                homeShowsSameContentToggle(items),
                resetDemoDataRow(items),
                SectionHeaderListModel(SampleStringId.SETTINGS_SECTION_ABOUT.toString()),
                appVersionRow(items),
                buildDateRow(items),
            ),
            items,
        )
    }

    @Test
    fun `theme dropdown lists every mode in enum order and selects the current one`() {
        val dropdown = themeDropdown(SettingsState(themeMode = ThemeMode.DARK).toListItems(FakeStringProvider))

        assertEquals(
            listOf(
                SampleStringId.SETTINGS_THEME_LIGHT.toString(),
                SampleStringId.SETTINGS_THEME_DARK.toString(),
                SampleStringId.SETTINGS_THEME_SYSTEM.toString(),
            ),
            dropdown.options.map { it.first },
        )
        assertEquals(ThemeMode.DARK.ordinal, dropdown.selectedPosition)
    }

    @Test
    fun `font dropdown lists every choice in enum order and selects the current one`() {
        val dropdown = fontDropdown(
            SettingsState(fontChoice = FontChoice.INTER).toListItems(FakeStringProvider),
        )

        assertEquals(
            listOf(
                SampleStringId.SETTINGS_FONT_STANDARD.toString(),
                SampleStringId.SETTINGS_FONT_INTER.toString(),
            ),
            dropdown.options.map { it.first },
        )
        assertEquals(FontChoice.INTER.ordinal, dropdown.selectedPosition)
    }

    @Test
    fun `delay dropdown lists every duration in enum order and selects the current one`() {
        val dropdown = delayDropdown(
            SettingsState(simulateDelay = SimulateDelay.TWO_SECONDS).toListItems(FakeStringProvider),
        )

        assertEquals(
            listOf(
                SampleStringId.SETTINGS_DELAY_NONE.toString(),
                SampleStringId.SETTINGS_DELAY_ONE_SECOND.toString(),
                SampleStringId.SETTINGS_DELAY_TWO_SECONDS.toString(),
                SampleStringId.SETTINGS_DELAY_FIVE_SECONDS.toString(),
            ),
            dropdown.options.map { it.first },
        )
        assertEquals(SimulateDelay.TWO_SECONDS.ordinal, dropdown.selectedPosition)
    }

    @Test
    fun `animation scale dropdown lists every multiplier in enum order and selects the current one`() {
        val dropdown = animationScaleDropdown(
            SettingsState(animationScale = AnimationScale.TWO_X).toListItems(FakeStringProvider),
        )

        assertEquals(
            listOf(
                SampleStringId.SETTINGS_ANIMATION_SCALE_ONE_X.toString(),
                SampleStringId.SETTINGS_ANIMATION_SCALE_TWO_X.toString(),
                SampleStringId.SETTINGS_ANIMATION_SCALE_THREE_X.toString(),
                SampleStringId.SETTINGS_ANIMATION_SCALE_FIVE_X.toString(),
            ),
            dropdown.options.map { it.first },
        )
        assertEquals(AnimationScale.TWO_X.ordinal, dropdown.selectedPosition)
    }

    @Test
    fun `demo outcome dropdown lists every outcome in enum order and selects the current one`() {
        val dropdown = demoOutcomeDropdown(
            SettingsState(demoOutcome = DemoOutcome.ERROR).toListItems(FakeStringProvider),
        )

        assertEquals(
            listOf(
                SampleStringId.SETTINGS_DEMO_OUTCOME_CONTENT.toString(),
                SampleStringId.SETTINGS_DEMO_OUTCOME_EMPTY.toString(),
                SampleStringId.SETTINGS_DEMO_OUTCOME_ERROR.toString(),
            ),
            dropdown.options.map { it.first },
        )
        assertEquals(DemoOutcome.ERROR.ordinal, dropdown.selectedPosition)
    }

    @Test
    fun `option rows carry the typed selection action`() {
        val theme = themeDropdown(SettingsState().toListItems(FakeStringProvider))
        val font = fontDropdown(SettingsState().toListItems(FakeStringProvider))
        val delay = delayDropdown(SettingsState().toListItems(FakeStringProvider))
        val animationScale = animationScaleDropdown(SettingsState().toListItems(FakeStringProvider))
        val outcome = demoOutcomeDropdown(SettingsState().toListItems(FakeStringProvider))

        assertEquals(
            ThemeMode.entries.map { SettingsAction.ThemeModeSelected(it) },
            theme.options.map { it.second.clickAction() },
        )
        assertEquals(
            FontChoice.entries.map { SettingsAction.FontChoiceSelected(it) },
            font.options.map { it.second.clickAction() },
        )
        assertEquals(
            SimulateDelay.entries.map { SettingsAction.SimulateDelaySelected(it) },
            delay.options.map { it.second.clickAction() },
        )
        assertEquals(
            AnimationScale.entries.map { SettingsAction.AnimationScaleSelected(it) },
            animationScale.options.map { it.second.clickAction() },
        )
        assertEquals(
            DemoOutcome.entries.map { SettingsAction.DemoOutcomeSelected(it) },
            outcome.options.map { it.second.clickAction() },
        )
    }

    @Test
    fun `only the tracked dropdown is expanded`() {
        val settingId = SampleStringId.SETTINGS_LABEL_THEME.name
        val items = SettingsState(expandedDropdownId = settingId).toListItems(FakeStringProvider)

        assertTrue(themeDropdown(items).expanded)
        assertTrue(!fontDropdown(items).expanded)
        assertTrue(!delayDropdown(items).expanded)
        assertTrue(!animationScaleDropdown(items).expanded)
        assertTrue(!demoOutcomeDropdown(items).expanded)
    }

    @Test
    fun `home shows same content toggle is captioned and carries the toggle action`() {
        val on = homeShowsSameContentToggle(SettingsState().toListItems(FakeStringProvider))
        assertEquals(true, on.checked)
        assertEquals(
            SampleStringId.SETTINGS_LABEL_HOME_SHOWS_SAME_CONTENT.toString(),
            on.name,
        )
        assertEquals(
            SampleStringId.SETTINGS_CAPTION_HOME_SHOWS_SAME_CONTENT.toString(),
            on.caption,
        )
        assertEquals(SettingsAction.HomeShowsSameContentToggled, on.clickAction)

        val off = homeShowsSameContentToggle(
            SettingsState(homeShowsSameContent = false).toListItems(FakeStringProvider),
        )
        assertEquals(false, off.checked)
    }

    @Test
    fun `reset demo data row has a stable id, captioned text and the reset action`() {
        val row = resetDemoDataRow(SettingsState().toListItems(FakeStringProvider))

        assertEquals(
            SampleStringId.SETTINGS_LABEL_RESET_DEMO_DATA.name.hashCode().toLong(),
            row.dataId,
        )
        assertEquals(
            SampleStringId.SETTINGS_LABEL_RESET_DEMO_DATA.toString(),
            row.name,
        )
        assertEquals(
            SampleStringId.SETTINGS_CAPTION_RESET_DEMO_DATA.toString(),
            row.caption,
        )
        assertEquals(SettingsAction.ResetDemoDataClicked, row.clickAction)
    }

    @Test
    fun `a pending reset replaces the action row with inline progress`() {
        val operationName = SampleStringId.SETTINGS_LABEL_RESET_DEMO_DATA.toString()
        val items = SettingsState(resetStatus = LCE.Loading(operationName))
            .toListItems(FakeStringProvider)

        val loading = items.filterIsInstance<LoadingItemListModel>().single()
        assertEquals(LoadingType.TEXT_CAPTION, loading.loadingType)
        assertEquals(operationName, loading.loadOperationName)
        assertTrue(
            items.none {
                it is NameCaptionListModel &&
                    it.name == SampleStringId.SETTINGS_LABEL_RESET_DEMO_DATA.toString()
            },
        )
    }

    @Test
    fun `a finished reset leaves the action row actionable`() {
        listOf(
            LCE.Uninitialized,
            LCE.Content(Unit),
            LCE.Error(SampleStringId.SETTINGS_LABEL_RESET_DEMO_DATA.toString(), IllegalStateException()),
        ).forEach { status ->
            val items = SettingsState(resetStatus = status).toListItems(FakeStringProvider)

            assertEquals(
                SettingsAction.ResetDemoDataClicked,
                resetDemoDataRow(items).clickAction,
            )
            assertTrue(items.none { it is LoadingItemListModel })
        }
    }

    @Test
    fun `about rows render the app version and build date`() {
        val items = SettingsState(
            appVersion = "1.2.3",
            buildDate = "January 1, 2026",
        ).toListItems(FakeStringProvider)

        assertEquals("1.2.3", appVersionRow(items).value)
        assertEquals("January 1, 2026", buildDateRow(items).value)
        assertEquals(SageAction.Noop, appVersionRow(items).clickAction)
        assertEquals(SageAction.Noop, buildDateRow(items).clickAction)
    }

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

    private fun appVersionRow(items: List<ListModel>): LabelValueListModel =
        items.filterIsInstance<LabelValueListModel>()
            .single { it.label == SampleStringId.SETTINGS_LABEL_APP_VERSION.toString() }

    private fun buildDateRow(items: List<ListModel>): LabelValueListModel =
        items.filterIsInstance<LabelValueListModel>()
            .single { it.label == SampleStringId.SETTINGS_LABEL_BUILD_DATE.toString() }

    private fun ListModel.clickAction(): SageAction = when (this) {
        is net.sigmabeta.sage.components.SingleTextListModel -> clickAction
        else -> error("unexpected option model: $this")
    }
}
