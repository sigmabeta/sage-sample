package net.sigmabeta.sage.sample.settings

import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import net.sigmabeta.sage.di.AppScope

/**
 * Applies the persisted [SimulateDelay] setting to every background load.
 *
 * The value is read when a load starts, so a change on the Settings screen affects the next load
 * rather than interrupting one already in flight.
 */
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class SettingsDelayManager(
    private val settings: SampleSettingsManager,
) : SampleDelayManager {
    override fun delayMillis(): Long = settings.simulateDelay.value.millis
}
