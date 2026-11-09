package net.sigmabeta.sage.sample.feature.settings
import net.sigmabeta.sage.sample.strings.api.SampleStringId

import kotlinx.collections.immutable.toImmutableList
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
    /** Progress of the "Reset demo data" action; the row shows inline progress while loading. */
    val resetStatus: LCE<Unit> = LCE.Uninitialized,
    val appVersion: String? = null,
    val buildDate: String? = null,
    /** `settingId` of the expanded dropdown, or null when all are collapsed. */
    val expandedDropdownId: String? = null,
) : ListState() {
    override fun title(stringProvider: StringProvider) = TitleBarModel(
        title = stringProvider.getString(SampleStringId.SETTINGS_TITLE),
        shouldShowBack = true,
    )

    override fun toListItems(stringProvider: StringProvider): List<ListModel> = listOf(
        sectionHeader(stringProvider, SampleStringId.SETTINGS_SECTION_APPEARANCE),
        themeDropdown(stringProvider),
        fontDropdown(stringProvider),
        sectionHeader(stringProvider, SampleStringId.SETTINGS_SECTION_SIMULATION),
        simulateDelayDropdown(stringProvider),
        demoOutcomeDropdown(stringProvider),
        homeShowsSameContentToggle(stringProvider),
        resetDemoDataRow(stringProvider),
        sectionHeader(stringProvider, SampleStringId.SETTINGS_SECTION_ABOUT),
        labelValue(
            stringProvider,
            SampleStringId.SETTINGS_LABEL_APP_VERSION,
            appVersion,
        ),
        labelValue(
            stringProvider,
            SampleStringId.SETTINGS_LABEL_BUILD_DATE,
            buildDate,
        ),
    )

    private fun themeDropdown(stringProvider: StringProvider): ListModel {
        val settingId = SampleStringId.SETTINGS_LABEL_THEME.name
        return DropdownSettingListModel.ofLabels(
            settingId = settingId,
            name = stringProvider.getString(SampleStringId.SETTINGS_LABEL_THEME),
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
        val settingId = SampleStringId.SETTINGS_LABEL_FONT.name
        return DropdownSettingListModel.ofLabels(
            settingId = settingId,
            name = stringProvider.getString(SampleStringId.SETTINGS_LABEL_FONT),
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
        val settingId = SampleStringId.SETTINGS_LABEL_SIMULATE_DELAY.name
        return DropdownSettingListModel.ofLabels(
            settingId = settingId,
            name = stringProvider.getString(SampleStringId.SETTINGS_LABEL_SIMULATE_DELAY),
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
        val settingId = SampleStringId.SETTINGS_LABEL_DEMO_OUTCOME.name
        return DropdownSettingListModel.ofLabels(
            settingId = settingId,
            name = stringProvider.getString(SampleStringId.SETTINGS_LABEL_DEMO_OUTCOME),
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
        val settingId = SampleStringId.SETTINGS_LABEL_HOME_SHOWS_SAME_CONTENT.name
        return NameCaptionCheckboxListModel(
            settingId = settingId,
            name = stringProvider.getString(SampleStringId.SETTINGS_LABEL_HOME_SHOWS_SAME_CONTENT),
            caption = stringProvider.getString(
                SampleStringId.SETTINGS_CAPTION_HOME_SHOWS_SAME_CONTENT,
            ),
            checked = homeShowsSameContent,
            clickAction = SettingsAction.HomeShowsSameContentToggled,
        )
    }

    private fun resetDemoDataRow(stringProvider: StringProvider): ListModel =
        when (resetStatus) {
            is LCE.Loading -> LoadingItemListModel(
                loadingType = LoadingType.TEXT_CAPTION,
                loadOperationName = resetStatus.operationName,
                loadPositionOffset = 0,
            )

            else -> NameCaptionListModel(
                dataId = SampleStringId.SETTINGS_LABEL_RESET_DEMO_DATA.name.hashCode().toLong(),
                name = stringProvider.getString(SampleStringId.SETTINGS_LABEL_RESET_DEMO_DATA),
                caption = stringProvider.getString(SampleStringId.SETTINGS_CAPTION_RESET_DEMO_DATA),
                clickAction = SettingsAction.ResetDemoDataClicked,
            )
        }

    private fun ThemeMode.labelId(): SampleStringId = when (this) {
        ThemeMode.LIGHT -> SampleStringId.SETTINGS_THEME_LIGHT
        ThemeMode.DARK -> SampleStringId.SETTINGS_THEME_DARK
        ThemeMode.SYSTEM -> SampleStringId.SETTINGS_THEME_SYSTEM
    }

    private fun FontChoice.labelId(): SampleStringId = when (this) {
        FontChoice.STANDARD -> SampleStringId.SETTINGS_FONT_STANDARD
        FontChoice.INTER -> SampleStringId.SETTINGS_FONT_INTER
    }

    private fun SimulateDelay.labelId(): SampleStringId = when (this) {
        SimulateDelay.NONE -> SampleStringId.SETTINGS_DELAY_NONE
        SimulateDelay.ONE_SECOND -> SampleStringId.SETTINGS_DELAY_ONE_SECOND
        SimulateDelay.TWO_SECONDS -> SampleStringId.SETTINGS_DELAY_TWO_SECONDS
        SimulateDelay.FIVE_SECONDS -> SampleStringId.SETTINGS_DELAY_FIVE_SECONDS
    }

    private fun DemoOutcome.labelId(): SampleStringId = when (this) {
        DemoOutcome.CONTENT -> SampleStringId.SETTINGS_DEMO_OUTCOME_CONTENT
        DemoOutcome.EMPTY -> SampleStringId.SETTINGS_DEMO_OUTCOME_EMPTY
        DemoOutcome.ERROR -> SampleStringId.SETTINGS_DEMO_OUTCOME_ERROR
    }

    private fun sectionHeader(
        stringProvider: StringProvider,
        id: SampleStringId,
    ): ListModel = SectionHeaderListModel(title = stringProvider.getString(id))

    // Display-only rows: SageAction.Noop keeps them non-interactive in the app renderer.
    private fun labelValue(
        stringProvider: StringProvider,
        labelId: SampleStringId,
        value: String?,
    ): ListModel = LabelValueListModel(
        label = stringProvider.getString(labelId),
        value = value,
        clickAction = SageAction.Noop,
    )
}
