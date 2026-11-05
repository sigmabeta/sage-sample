package net.sigmabeta.sage.sample.settings

import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import net.sigmabeta.sage.di.AppScope
import net.sigmabeta.sage.storage.common.Storage

/**
 * [SampleSettingsManager] over SAGE's key-value [Storage] seam.
 *
 * Each preference is one string key. The storage flow is mapped through the enum's parser and
 * shared eagerly, so a missing or unrecognised value falls back to the enum's default instead of
 * failing the screen.
 */
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class StorageSampleSettingsManager(
    private val storage: Storage,
    scope: CoroutineScope,
) : SampleSettingsManager {
    override val themeMode: StateFlow<ThemeMode> = storage
        .savedStringFlow(KEY_THEME_MODE)
        .map { ThemeMode.fromStorageValue(it) }
        .stateIn(scope, SharingStarted.Eagerly, ThemeMode.DEFAULT)

    override val simulateDelay: StateFlow<SimulateDelay> = storage
        .savedStringFlow(KEY_SIMULATE_DELAY)
        .map { SimulateDelay.fromStorageValue(it) }
        .stateIn(scope, SharingStarted.Eagerly, SimulateDelay.DEFAULT)

    override fun setThemeMode(mode: ThemeMode) = storage.saveString(KEY_THEME_MODE, mode.name)

    override fun setSimulateDelay(delay: SimulateDelay) =
        storage.saveString(KEY_SIMULATE_DELAY, delay.name)

    private companion object {
        const val KEY_THEME_MODE = "setting.appearance.theme"
        const val KEY_SIMULATE_DELAY = "setting.debug.simulate_delay"
    }
}
