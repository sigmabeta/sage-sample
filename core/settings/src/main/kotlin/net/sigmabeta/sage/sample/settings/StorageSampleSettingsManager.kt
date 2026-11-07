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
 * Each preference is one string key. Enum-backed preferences map through their `fromStorageValue`
 * parser and the boolean flag through [homeShowsSameContentFromStorage]; missing or unrecognised
 * values fall back to the preference's default instead of failing the screen.
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

    override val fontChoice: StateFlow<FontChoice> = storage
        .savedStringFlow(KEY_FONT_CHOICE)
        .map { FontChoice.fromStorageValue(it) }
        .stateIn(scope, SharingStarted.Eagerly, FontChoice.DEFAULT)

    override val simulateDelay: StateFlow<SimulateDelay> = storage
        .savedStringFlow(KEY_SIMULATE_DELAY)
        .map { SimulateDelay.fromStorageValue(it) }
        .stateIn(scope, SharingStarted.Eagerly, SimulateDelay.DEFAULT)

    override val homeShowsSameContent: StateFlow<Boolean> = storage
        .savedStringFlow(KEY_HOME_SHOWS_SAME_CONTENT)
        .map(::homeShowsSameContentFromStorage)
        .stateIn(scope, SharingStarted.Eagerly, DEFAULT_HOME_SHOWS_SAME_CONTENT)

    override fun setThemeMode(mode: ThemeMode) = storage.saveString(KEY_THEME_MODE, mode.name)

    override fun setFontChoice(choice: FontChoice) =
        storage.saveString(KEY_FONT_CHOICE, choice.name)

    override fun setSimulateDelay(delay: SimulateDelay) =
        storage.saveString(KEY_SIMULATE_DELAY, delay.name)

    override fun setHomeShowsSameContent(showSameContent: Boolean) =
        storage.saveString(KEY_HOME_SHOWS_SAME_CONTENT, showSameContent.toString())

    private companion object {
        const val KEY_THEME_MODE = "setting.appearance.theme"
        const val KEY_FONT_CHOICE = "setting.appearance.font"
        const val KEY_SIMULATE_DELAY = "setting.debug.simulate_delay"
        const val KEY_HOME_SHOWS_SAME_CONTENT = "setting.debug.home_shows_same_content"
    }
}

/**
 * Parses the persisted "true"/"false" flag, falling back to the default for missing or
 * unrecognised values.
 */
internal fun homeShowsSameContentFromStorage(value: String?): Boolean =
    value?.toBooleanStrictOrNull() ?: DEFAULT_HOME_SHOWS_SAME_CONTENT
