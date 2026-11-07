package net.sigmabeta.sage.sample.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.launch
import net.sigmabeta.sage.analytics.Analytics
import net.sigmabeta.sage.analytics.AnalyticsScreen
import net.sigmabeta.sage.appcomm.SageAction
import net.sigmabeta.sage.appinfo.AppInfo
import net.sigmabeta.sage.coroutines.SageDispatchers
import net.sigmabeta.sage.di.AppScope
import net.sigmabeta.sage.sample.settings.SampleDelayManager
import net.sigmabeta.sage.sample.settings.SampleSettingsManager
import net.sigmabeta.sage.sample.viewmodel.SampleListViewModel
import net.sigmabeta.sage.ui.StringProvider
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Settings' view model.
 *
 * The selected values live in [SampleSettingsManager] (app-scoped, persisted through SAGE's
 * `Storage` seam); this view model mirrors them into `ListState` and writes selections back. The
 * app version and build date come from SAGE's [AppInfo] seam and are display-only.
 */
@ContributesIntoMap(AppScope::class, binding = binding<ViewModel>())
@ViewModelKey
@Inject
class SettingsViewModel(
    override val stringProvider: StringProvider,
    override val analytics: Analytics,
    override val dispatchers: SageDispatchers,
    override val delayManager: SampleDelayManager,
    private val settingsManager: SampleSettingsManager,
    private val appInfo: AppInfo,
) : SampleListViewModel<SettingsState>() {
    override val screenIdentifier = AnalyticsScreen.SETTINGS

    override fun initialState(): SettingsState = SettingsState(
        appVersion = appInfo.versionName,
        buildDate = formatBuildDate(appInfo.buildTimeMs),
    )

    init {
        sendAction(SageAction.InitNoArgs)

        viewModelScope.launch {
            settingsManager.themeMode.collect { mode ->
                updateState { it.copy(themeMode = mode) }
            }
        }
        viewModelScope.launch {
            settingsManager.fontChoice.collect { choice ->
                updateState { it.copy(fontChoice = choice) }
            }
        }
        viewModelScope.launch {
            settingsManager.simulateDelay.collect { delay ->
                updateState { it.copy(simulateDelay = delay) }
            }
        }
        viewModelScope.launch {
            settingsManager.demoOutcome.collect { outcome ->
                updateState { it.copy(demoOutcome = outcome) }
            }
        }
        viewModelScope.launch {
            settingsManager.homeShowsSameContent.collect { showSameContent ->
                updateState { it.copy(homeShowsSameContent = showSameContent) }
            }
        }
    }

    override fun handleAction(action: SageAction) {
        when (action) {
            is SettingsAction.DropdownExpandClicked -> updateState {
                // One open dropdown at a time: re-tapping the open one closes it; tapping any
                // other replaces it as the single expanded dropdown.
                val next = if (it.expandedDropdownId == action.settingId) null else action.settingId
                it.copy(expandedDropdownId = next)
            }

            is SettingsAction.ThemeModeSelected -> {
                settingsManager.setThemeMode(action.mode)
                collapseDropdown()
            }

            is SettingsAction.FontChoiceSelected -> {
                settingsManager.setFontChoice(action.choice)
                collapseDropdown()
            }

            is SettingsAction.SimulateDelaySelected -> {
                settingsManager.setSimulateDelay(action.delay)
                collapseDropdown()
            }

            is SettingsAction.DemoOutcomeSelected -> {
                settingsManager.setDemoOutcome(action.outcome)
                collapseDropdown()
            }

            SettingsAction.HomeShowsSameContentToggled -> settingsManager.setHomeShowsSameContent(
                !settingsManager.homeShowsSameContent.value,
            )

            else -> Unit
        }
    }

    private fun collapseDropdown() = updateState { it.copy(expandedDropdownId = null) }
}

private val BUILD_DATE_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)

private fun formatBuildDate(epochMs: Long?): String? = epochMs?.let {
    Instant.ofEpochMilli(it)
        .atZone(ZoneId.systemDefault())
        .format(BUILD_DATE_FORMATTER)
}
