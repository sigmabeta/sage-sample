package net.sigmabeta.sage.sample.settings

import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import net.sigmabeta.sage.di.AppScope

/**
 * Restores the sample's demo data to its launch defaults.
 *
 * The reset spans several stores (clearing the recent-search history and restoring two demo
 * preferences), so Settings runs it through this seam: the caller can move it off the main thread
 * and tests can inject success or failure without touching storage.
 */
interface DemoDataResetter {
    /** Clears recent searches and restores [DemoOutcome.DEFAULT] plus the Home-content default. */
    suspend fun reset()
}

/**
 * [DemoDataResetter] over the app-scoped managers.
 *
 * These managers persist asynchronously, so this only has to sequence the calls; running the
 * resetter off the main thread is the caller's job.
 */
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class DefaultDemoDataResetter(
    private val searchHistoryManager: SearchHistoryManager,
    private val settingsManager: SampleSettingsManager,
) : DemoDataResetter {
    override suspend fun reset() {
        searchHistoryManager.clear()
        settingsManager.setDemoOutcome(DemoOutcome.DEFAULT)
        settingsManager.setHomeShowsSameContent(DEFAULT_HOME_SHOWS_SAME_CONTENT)
    }
}
