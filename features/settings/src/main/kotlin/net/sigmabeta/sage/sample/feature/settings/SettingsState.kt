package net.sigmabeta.sage.sample.feature.settings

import kotlinx.collections.immutable.toImmutableList
import net.sigmabeta.sage.appcomm.SageAction
import net.sigmabeta.sage.components.CheckableListModel
import net.sigmabeta.sage.components.DropdownSettingListModel
import net.sigmabeta.sage.components.LabelValueListModel
import net.sigmabeta.sage.components.ListModel
import net.sigmabeta.sage.components.SectionHeaderListModel
import net.sigmabeta.sage.components.TitleBarModel
import net.sigmabeta.sage.list.ListState
import net.sigmabeta.sage.sample.settings.DEFAULT_HOME_SHOWS_SAME_CONTENT
import net.sigmabeta.sage.sample.settings.DemoOutcome
import net.sigmabeta.sage.sample.settings.FontChoice
import net.sigmabeta.sage.sample.settings.SimulateDelay
import net.sigmabeta.sage.sample.settings.ThemeMode
import net.sigmabeta.sage.ui.StringProvider

/**
 * Settings' content, declared as a SAGE [ListState].
 *
 * The dropdowns are closed sets: their option order mirrors the enum's `entries`, so the picked
 * index maps straight back to an enum value. At most one dropdown is expanded at a time, matching
 * the list's single interaction focus.
 */
data class SettingsState(
    val themeMode: ThemeMode = ThemeMode.DEFAULT,
    val fontChoice: FontChoice = FontChoice.DEFAULT,
    val simulateDelay: SimulateDelay = SimulateDelay.DEFAULT,
    val demoOutcome: DemoOutcome = DemoOutcome.DEFAULT,
    val homeShowsSameContent: Boolean = DEFAULT_HOME_SHOWS_SAME_CONTENT,
    val appVersion: String? = null,
    val buildDate: String? = null,
    /** `settingId` of the expanded dropdown, or null when all are collapsed. */
    val expandedDropdownId: String? = null,
) : ListState() {
    override fun title(stringProvider: StringProvider) = TitleBarModel(
        title = stringProvider.getString(SettingsStringId.SETTINGS_TITLE),
        shouldShowBack = true,
    )

    override fun toListItems(stringProvider: StringProvider): List<ListModel> = listOf(
        sectionHeader(stringProvider, SettingsStringId.SETTINGS_SECTION_APPEARANCE),
        themeDropdown(stringProvider),
        fontDropdown(stringProvider),
        sectionHeader(stringProvider, SettingsStringId.SETTINGS_SECTION_SIMULATION),
        simulateDelayDropdown(stringProvider),
        demoOutcomeDropdown(stringProvider),
        homeShowsSameContentToggle(stringProvider),
        sectionHeader(stringProvider, SettingsStringId.SETTINGS_SECTION_ABOUT),
        labelValue(
            stringProvider,
            SettingsStringId.SETTINGS_LABEL_APP_VERSION,
            appVersion,
        ),
        labelValue(
            stringProvider,
            SettingsStringId.SETTINGS_LABEL_BUILD_DATE,
            buildDate,
        ),
    )

    private fun themeDropdown(stringProvider: StringProvider): ListModel {
        val settingId = SettingsStringId.SETTINGS_LABEL_THEME.name
        return DropdownSettingListModel.ofLabels(
            settingId = settingId,
            name = stringProvider.getString(SettingsStringId.SETTINGS_LABEL_THEME),
            selectedPosition = themeMode.ordinal,
            labels = ThemeMode.entries
                .map { stringProvider.getString(it.labelId()) }
                .toImmutableList(),
            expanded = expandedDropdownId == settingId,
            onExpandClicked = SettingsAction.DropdownExpandClicked(settingId),
            onNewOptionSelected = { index ->
                SettingsAction.ThemeModeSelected(ThemeMode.entries[index])
            },
        )
    }

    private fun fontDropdown(stringProvider: StringProvider): ListModel {
        val settingId = SettingsStringId.SETTINGS_LABEL_FONT.name
        return DropdownSettingListModel.ofLabels(
            settingId = settingId,
            name = stringProvider.getString(SettingsStringId.SETTINGS_LABEL_FONT),
            selectedPosition = fontChoice.ordinal,
            labels = FontChoice.entries
                .map { stringProvider.getString(it.labelId()) }
                .toImmutableList(),
            expanded = expandedDropdownId == settingId,
            onExpandClicked = SettingsAction.DropdownExpandClicked(settingId),
            onNewOptionSelected = { index ->
                SettingsAction.FontChoiceSelected(FontChoice.entries[index])
            },
        )
    }

    private fun simulateDelayDropdown(stringProvider: StringProvider): ListModel {
        val settingId = SettingsStringId.SETTINGS_LABEL_SIMULATE_DELAY.name
        return DropdownSettingListModel.ofLabels(
            settingId = settingId,
            name = stringProvider.getString(SettingsStringId.SETTINGS_LABEL_SIMULATE_DELAY),
            selectedPosition = simulateDelay.ordinal,
            labels = SimulateDelay.entries
                .map { stringProvider.getString(it.labelId()) }
                .toImmutableList(),
            expanded = expandedDropdownId == settingId,
            onExpandClicked = SettingsAction.DropdownExpandClicked(settingId),
            onNewOptionSelected = { index ->
                SettingsAction.SimulateDelaySelected(SimulateDelay.entries[index])
            },
        )
    }

    private fun demoOutcomeDropdown(stringProvider: StringProvider): ListModel {
        val settingId = SettingsStringId.SETTINGS_LABEL_DEMO_OUTCOME.name
        return DropdownSettingListModel.ofLabels(
            settingId = settingId,
            name = stringProvider.getString(SettingsStringId.SETTINGS_LABEL_DEMO_OUTCOME),
            selectedPosition = demoOutcome.ordinal,
            labels = DemoOutcome.entries
                .map { stringProvider.getString(it.labelId()) }
                .toImmutableList(),
            expanded = expandedDropdownId == settingId,
            onExpandClicked = SettingsAction.DropdownExpandClicked(settingId),
            onNewOptionSelected = { index ->
                SettingsAction.DemoOutcomeSelected(DemoOutcome.entries[index])
            },
        )
    }

    private fun homeShowsSameContentToggle(stringProvider: StringProvider): ListModel {
        val settingId = SettingsStringId.SETTINGS_LABEL_HOME_SHOWS_SAME_CONTENT.name
        return CheckableListModel(
            settingId = settingId,
            name = stringProvider.getString(SettingsStringId.SETTINGS_LABEL_HOME_SHOWS_SAME_CONTENT),
            checked = homeShowsSameContent,
            clickAction = SettingsAction.HomeShowsSameContentToggled,
        )
    }

    private fun ThemeMode.labelId(): SettingsStringId = when (this) {
        ThemeMode.LIGHT -> SettingsStringId.SETTINGS_THEME_LIGHT
        ThemeMode.DARK -> SettingsStringId.SETTINGS_THEME_DARK
        ThemeMode.SYSTEM -> SettingsStringId.SETTINGS_THEME_SYSTEM
    }

    private fun FontChoice.labelId(): SettingsStringId = when (this) {
        FontChoice.STANDARD -> SettingsStringId.SETTINGS_FONT_STANDARD
        FontChoice.INTER -> SettingsStringId.SETTINGS_FONT_INTER
    }

    private fun SimulateDelay.labelId(): SettingsStringId = when (this) {
        SimulateDelay.NONE -> SettingsStringId.SETTINGS_DELAY_NONE
        SimulateDelay.ONE_SECOND -> SettingsStringId.SETTINGS_DELAY_ONE_SECOND
        SimulateDelay.TWO_SECONDS -> SettingsStringId.SETTINGS_DELAY_TWO_SECONDS
        SimulateDelay.FIVE_SECONDS -> SettingsStringId.SETTINGS_DELAY_FIVE_SECONDS
    }

    private fun DemoOutcome.labelId(): SettingsStringId = when (this) {
        DemoOutcome.CONTENT -> SettingsStringId.SETTINGS_DEMO_OUTCOME_CONTENT
        DemoOutcome.EMPTY -> SettingsStringId.SETTINGS_DEMO_OUTCOME_EMPTY
        DemoOutcome.ERROR -> SettingsStringId.SETTINGS_DEMO_OUTCOME_ERROR
    }

    private fun sectionHeader(
        stringProvider: StringProvider,
        id: SettingsStringId,
    ): ListModel = SectionHeaderListModel(title = stringProvider.getString(id))

    // Display-only rows: SageAction.Noop keeps them non-interactive in the app renderer.
    private fun labelValue(
        stringProvider: StringProvider,
        labelId: SettingsStringId,
        value: String?,
    ): ListModel = LabelValueListModel(
        label = stringProvider.getString(labelId),
        value = value,
        clickAction = SageAction.Noop,
    )
}
