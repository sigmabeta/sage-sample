package net.sigmabeta.sage.sample.di

import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import net.sigmabeta.sage.coroutines.SageDispatchers
import net.sigmabeta.sage.di.AppScope
import net.sigmabeta.sage.list.DelayManager
import net.sigmabeta.sage.list.SageScheduler

/**
 * The app's [SageScheduler]: SAGE's coroutine scope + dispatchers + debug-delay seam.
 *
 * [Inject] is Metro's annotation (not `javax.inject`); [SampleBindings] binds it to the
 * [SageScheduler] interface the list brains consume.
 */
@SingleIn(AppScope::class)
@Inject
class SchedulerImpl(
    override val dispatchers: SageDispatchers,
    override val coroutineScope: CoroutineScope,
) : SageScheduler {
    override val delayManager: DelayManager = object : DelayManager {
        override fun shouldDelay(): Boolean = false
    }
}
