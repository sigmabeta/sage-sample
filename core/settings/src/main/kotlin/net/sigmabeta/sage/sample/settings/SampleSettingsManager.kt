package net.sigmabeta.sage.sample.settings

import kotlinx.coroutines.flow.StateFlow

/**
 * The app's persisted preferences, observable as state.
 *
 * [StateFlow] rather than a plain `Flow` because both consumers read the current value without a
 * collector: the composition root applies the theme, and [SettingsDelayManager] reads the delay
 * when a screen kicks off a background load.
 */
interface SampleSettingsManager {
    val themeMode: StateFlow<ThemeMode>
    val simulateDelay: StateFlow<SimulateDelay>

    fun setThemeMode(mode: ThemeMode)
    fun setSimulateDelay(delay: SimulateDelay)
}
