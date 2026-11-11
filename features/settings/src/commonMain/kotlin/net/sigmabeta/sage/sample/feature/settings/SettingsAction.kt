package net.sigmabeta.sage.sample.feature.settings

import net.sigmabeta.sage.appcomm.SageAction
import net.sigmabeta.sage.sample.settings.AnimationScale
import net.sigmabeta.sage.sample.settings.DemoOutcome
import net.sigmabeta.sage.sample.settings.FontChoice
import net.sigmabeta.sage.sample.settings.SimulateDelay
import net.sigmabeta.sage.sample.settings.ThemeMode

/**
 * Settings' feature-specific actions.
 *
 * Dropdown expansion and selection are separate: the row that opens a dropdown dispatches
 * [DropdownExpandClicked], while the expanded option row carries the typed selection action.
 */
sealed class SettingsAction : SageAction() {
    data class DropdownExpandClicked(val settingId: String) : SettingsAction()
    data class ThemeModeSelected(val mode: ThemeMode) : SettingsAction()
    data class FontChoiceSelected(val choice: FontChoice) : SettingsAction()
    data class SimulateDelaySelected(val delay: SimulateDelay) : SettingsAction()
    data class AnimationScaleSelected(val scale: AnimationScale) : SettingsAction()
    data class DemoOutcomeSelected(val outcome: DemoOutcome) : SettingsAction()
    data object HomeShowsSameContentToggled : SettingsAction()
    data object ResetDemoDataClicked : SettingsAction()
}
