package net.sigmabeta.sage.sample.feature.settings

import net.sigmabeta.sage.appcomm.SageAction
import net.sigmabeta.sage.components.CheckableListModel
import net.sigmabeta.sage.components.DropdownSettingListModel
import net.sigmabeta.sage.components.LabelValueListModel
import net.sigmabeta.sage.components.ListModel
import net.sigmabeta.sage.components.SectionHeaderListModel
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
                SectionHeaderListModel(SettingsStringId.SETTINGS_SECTION_APPEARANCE.toString()),
                themeDropdown(items),
                fontDropdown(items),
                SectionHeaderListModel(SettingsStringId.SETTINGS_SECTION_SIMULATION.toString()),
                delayDropdown(items),
                homeShowsSameContentToggle(items),
                SectionHeaderListModel(SettingsStringId.SETTINGS_SECTION_ABOUT.toString()),
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
                SettingsStringId.SETTINGS_THEME_LIGHT.toString(),
                SettingsStringId.SETTINGS_THEME_DARK.toString(),
                SettingsStringId.SETTINGS_THEME_SYSTEM.toString(),
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
                SettingsStringId.SETTINGS_FONT_STANDARD.toString(),
                SettingsStringId.SETTINGS_FONT_INTER.toString(),
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
                SettingsStringId.SETTINGS_DELAY_NONE.toString(),
                SettingsStringId.SETTINGS_DELAY_ONE_SECOND.toString(),
                SettingsStringId.SETTINGS_DELAY_TWO_SECONDS.toString(),
                SettingsStringId.SETTINGS_DELAY_FIVE_SECONDS.toString(),
            ),
            dropdown.options.map { it.first },
        )
        assertEquals(SimulateDelay.TWO_SECONDS.ordinal, dropdown.selectedPosition)
    }

    @Test
    fun `option rows carry the typed selection action`() {
        val theme = themeDropdown(SettingsState().toListItems(FakeStringProvider))
        val font = fontDropdown(SettingsState().toListItems(FakeStringProvider))
        val delay = delayDropdown(SettingsState().toListItems(FakeStringProvider))

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
    }

    @Test
    fun `only the tracked dropdown is expanded`() {
        val settingId = SettingsStringId.SETTINGS_LABEL_THEME.name
        val items = SettingsState(expandedDropdownId = settingId).toListItems(FakeStringProvider)

        assertTrue(themeDropdown(items).expanded)
        assertTrue(!fontDropdown(items).expanded)
        assertTrue(!delayDropdown(items).expanded)
    }

    @Test
    fun `home shows same content toggle reflects the setting and carries the toggle action`() {
        val off = homeShowsSameContentToggle(SettingsState().toListItems(FakeStringProvider))
        assertEquals(false, off.checked)
        assertEquals(SettingsAction.HomeShowsSameContentToggled, off.clickAction)

        val on = homeShowsSameContentToggle(
            SettingsState(homeShowsSameContent = true).toListItems(FakeStringProvider),
        )
        assertEquals(true, on.checked)
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
            .single { it.name == SettingsStringId.SETTINGS_LABEL_THEME.toString() }

    private fun fontDropdown(items: List<ListModel>): DropdownSettingListModel =
        items.filterIsInstance<DropdownSettingListModel>()
            .single { it.name == SettingsStringId.SETTINGS_LABEL_FONT.toString() }

    private fun delayDropdown(items: List<ListModel>): DropdownSettingListModel =
        items.filterIsInstance<DropdownSettingListModel>()
            .single { it.name == SettingsStringId.SETTINGS_LABEL_SIMULATE_DELAY.toString() }

    private fun homeShowsSameContentToggle(items: List<ListModel>): CheckableListModel =
        items.filterIsInstance<CheckableListModel>()
            .single {
                it.name == SettingsStringId.SETTINGS_LABEL_HOME_SHOWS_SAME_CONTENT.toString()
            }

    private fun appVersionRow(items: List<ListModel>): LabelValueListModel =
        items.filterIsInstance<LabelValueListModel>()
            .single { it.label == SettingsStringId.SETTINGS_LABEL_APP_VERSION.toString() }

    private fun buildDateRow(items: List<ListModel>): LabelValueListModel =
        items.filterIsInstance<LabelValueListModel>()
            .single { it.label == SettingsStringId.SETTINGS_LABEL_BUILD_DATE.toString() }

    private fun ListModel.clickAction(): SageAction = when (this) {
        is net.sigmabeta.sage.components.SingleTextListModel -> clickAction
        else -> error("unexpected option model: $this")
    }
}
