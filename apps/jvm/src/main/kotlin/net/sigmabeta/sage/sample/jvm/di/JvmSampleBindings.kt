package net.sigmabeta.sage.sample.jvm.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.Dispatchers
import net.sigmabeta.sage.analytics.Analytics
import net.sigmabeta.sage.analytics.NoopAnalytics
import net.sigmabeta.sage.coroutines.SageDispatchers
import net.sigmabeta.sage.di.AppScope
import net.sigmabeta.sage.list.DelayManager
import net.sigmabeta.sage.logging.BluntHatchet
import net.sigmabeta.sage.logging.Hatchet
import net.sigmabeta.sage.sample.jvm.SampleStringProvider
import net.sigmabeta.sage.ui.StringProvider

/**
 * Desktop implementations of SAGE's platform seams.
 *
 * The Android app binds `AndroidStringProvider`, `AndroidHatchet` and the Android dispatcher
 * module; the desktop target has no `Context`, so it supplies JVM equivalents here. Feature
 * modules still contribute their own bindings (view models, `DemoBrowseRepository`, …).
 */
@BindingContainer
@ContributesTo(AppScope::class)
object JvmSampleBindings {
    @Provides
    @SingleIn(AppScope::class)
    fun provideStringProvider(): StringProvider = SampleStringProvider

    /** Silent logger: the sample has no logcat, and desktop analytics are no-ops. */
    @Provides
    @SingleIn(AppScope::class)
    fun provideHatchet(): Hatchet = BluntHatchet()

    @Provides
    @SingleIn(AppScope::class)
    fun provideAnalytics(hatchet: Hatchet): Analytics = NoopAnalytics(hatchet)

    @Provides
    @SingleIn(AppScope::class)
    fun provideDelayManager(): DelayManager = object : DelayManager {
        override fun shouldDelay() = false
    }

    /**
     * SAGE's coroutine seam. `main` is the AWT event queue on desktop, which is why the module
     * depends on `kotlinx-coroutines-swing`.
     */
    @Provides
    @SingleIn(AppScope::class)
    fun provideDispatchers(): SageDispatchers = SageDispatchers(
        computation = Dispatchers.Default,
        disk = Dispatchers.IO,
        network = Dispatchers.IO,
        main = Dispatchers.Main,
    )
}
