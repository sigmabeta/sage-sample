package net.sigmabeta.sage.sample.settings.screenshot
import net.sigmabeta.sage.sample.strings.api.SampleStringId

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import net.sigmabeta.sage.list.WidthClass
import net.sigmabeta.sage.sample.feature.settings.SettingsState
import net.sigmabeta.sage.sample.settings.DemoOutcome
import net.sigmabeta.sage.sample.settings.FontChoice
import net.sigmabeta.sage.sample.settings.SimulateDelay
import net.sigmabeta.sage.sample.settings.ThemeMode
import net.sigmabeta.sage.sample.ui.previews.DevicePreviews
import net.sigmabeta.sage.sample.ui.previews.ListScreenPreview
import net.sigmabeta.sage.sample.ui.previews.previewWidthClass

@DevicePreviews
@Composable
internal fun Settings(
    darkTheme: Boolean = isSystemInDarkTheme(),
    syntheticWidthClass: WidthClass = previewWidthClass(),
) {
    ListScreenPreview(
        screenState = settingsState(),
        darkTheme = darkTheme,
        syntheticWidthClass = syntheticWidthClass,
    )
}

@DevicePreviews
@Composable
internal fun SettingsExpandedDropdown(
    darkTheme: Boolean = isSystemInDarkTheme(),
    syntheticWidthClass: WidthClass = previewWidthClass(),
) {
    ListScreenPreview(
        screenState = settingsState(expandedDropdownId = SampleStringId.SETTINGS_LABEL_THEME.name),
        darkTheme = darkTheme,
        syntheticWidthClass = syntheticWidthClass,
    )
}

/**
 * A settled Settings screen with fixed About values. The version/build strings are normally
 * supplied by the app's build config, so the fixture pins them rather than reading the real build.
 */
private fun settingsState(
    expandedDropdownId: String? = null,
): SettingsState = SettingsState(
    themeMode = ThemeMode.SYSTEM,
    fontChoice = FontChoice.STANDARD,
    simulateDelay = SimulateDelay.NONE,
    demoOutcome = DemoOutcome.CONTENT,
    homeShowsSameContent = true,
    appVersion = "0.1.0",
    buildDate = "Nov 7, 2026",
    expandedDropdownId = expandedDropdownId,
)
