package net.sigmabeta.sage.sample.feature.settings

import kotlinx.collections.immutable.toImmutableList
import net.sigmabeta.sage.appcomm.SageAction
import net.sigmabeta.sage.components.DropdownSettingListModel
import net.sigmabeta.sage.components.LabelValueListModel
import net.sigmabeta.sage.components.ListModel
import net.sigmabeta.sage.components.SectionHeaderListModel
import net.sigmabeta.sage.components.TitleBarModel
import net.sigmabeta.sage.list.ListState
import net.sigmabeta.sage.sample.settings.SimulateDelay
import net.sigmabeta.sage.sample.settings.ThemeMode
import net.sigmabeta.sage.ui.StringProvider

/**
 * Settings' content, declared as a SAGE [ListState].
 *
 * The two dropdowns are closed sets: their option order mirrors the enum's `entries`, so the
 * picked index maps straight back to an enum value. At most one dropdown is expanded at a time,
 * matching the list's single interaction focus.
 */
data class SettingsState(
    val themeMode: ThemeMode = ThemeMode.DEFAULT,
    val simulateDelay: SimulateDelay = SimulateDelay.DEFAULT,
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
        sectionHeader(stringProvider, SettingsStringId.SETTINGS_SECTION_SIMULATION),
        simulateDelayDropdown(stringProvider),
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

    private fun ThemeMode.labelId(): SettingsStringId = when (this) {
        ThemeMode.LIGHT -> SettingsStringId.SETTINGS_THEME_LIGHT
        ThemeMode.DARK -> SettingsStringId.SETTINGS_THEME_DARK
        ThemeMode.SYSTEM -> SettingsStringId.SETTINGS_THEME_SYSTEM
    }

    private fun SimulateDelay.labelId(): SettingsStringId = when (this) {
        SimulateDelay.NONE -> SettingsStringId.SETTINGS_DELAY_NONE
        SimulateDelay.ONE_SECOND -> SettingsStringId.SETTINGS_DELAY_ONE_SECOND
        SimulateDelay.TWO_SECONDS -> SettingsStringId.SETTINGS_DELAY_TWO_SECONDS
        SimulateDelay.FIVE_SECONDS -> SettingsStringId.SETTINGS_DELAY_FIVE_SECONDS
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
