package net.sigmabeta.sage.sample.settings

import kotlinx.coroutines.flow.StateFlow

/**
 * The app's persisted preferences, observable as state.
 *
 * [StateFlow] rather than a plain `Flow` because consumers read the current value without a
 * collector: the composition roots apply the theme and font, and [SettingsDelayManager] reads the
 * delay when a screen kicks off a background load.
 */
interface SampleSettingsManager {
    val themeMode: StateFlow<ThemeMode>
    val fontChoice: StateFlow<FontChoice>
    val simulateDelay: StateFlow<SimulateDelay>

    /**
     * When true, Home builds its module plan from a fixed seed, so every visit shows the same
     * rows in the same order. Off gives a fresh random plan per visit.
     */
    val homeShowsSameContent: StateFlow<Boolean>

    fun setThemeMode(mode: ThemeMode)
    fun setFontChoice(choice: FontChoice)
    fun setSimulateDelay(delay: SimulateDelay)
    fun setHomeShowsSameContent(showSameContent: Boolean)
}

/** Default for [SampleSettingsManager.homeShowsSameContent]: randomize each visit. */
const val DEFAULT_HOME_SHOWS_SAME_CONTENT = false
