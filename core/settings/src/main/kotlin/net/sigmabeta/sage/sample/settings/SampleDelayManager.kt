package net.sigmabeta.sage.sample.settings

import net.sigmabeta.sage.list.DelayManager

/**
 * SAGE's debug-delay seam, extended with the configured duration.
 *
 * SAGE's [DelayManager] only answers whether to delay and uses a fixed five seconds. The sample's
 * Settings screen lets the user choose one, two or five seconds, so screens ask this type for the
 * millis instead.
 */
interface SampleDelayManager : DelayManager {
    override fun shouldDelay(): Boolean = delayMillis() > 0L

    fun delayMillis(): Long
}
