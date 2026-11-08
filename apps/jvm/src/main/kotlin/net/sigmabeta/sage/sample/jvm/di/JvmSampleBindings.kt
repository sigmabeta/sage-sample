package net.sigmabeta.sage.sample.jvm.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import net.sigmabeta.sage.analytics.Analytics
import net.sigmabeta.sage.analytics.NoopAnalytics
import net.sigmabeta.sage.appinfo.AppInfo
import net.sigmabeta.sage.coroutines.SageDispatchers
import net.sigmabeta.sage.di.AppScope
import net.sigmabeta.sage.logging.BluntHatchet
import net.sigmabeta.sage.logging.Hatchet
import net.sigmabeta.sage.sample.jvm.JvmBuildInfo
import net.sigmabeta.sage.sample.strings.real.SampleStringProvider
import net.sigmabeta.sage.sample.jvm.storage.JvmStorage
import net.sigmabeta.sage.storage.common.Storage
import net.sigmabeta.sage.ui.StringProvider
import java.io.File

/**
 * Desktop implementations of SAGE's platform seams.
 *
 * The Android app binds `AndroidStringProvider`, `AndroidHatchet` and the Android dispatcher
 * module; the desktop target has no `Context`, so it supplies JVM equivalents here. Feature
 * modules still contribute their own bindings (view models, settings, …).
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

    /** Version info shown in Settings' About section. */
    @Provides
    @SingleIn(AppScope::class)
    fun provideAppInfo(): AppInfo = AppInfo(
        isDebug = true,
        versionName = JvmBuildInfo.versionName,
        versionCode = 1,
        buildTimeMs = JvmBuildInfo.buildTimeMs,
        buildBranch = JvmBuildInfo.BUILD_BRANCH,
    )

    /**
     * SAGE's key-value storage seam. The settings manager persists the theme, font and
     * simulated-delay choices through it; the desktop app keeps them in a properties file under the
     * user's home.
     */
    @Provides
    @SingleIn(AppScope::class)
    fun provideStorage(): Storage = JvmStorage(settingsFile())

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

    /** App-lifetime scope for state shared across screens (the settings manager's flows). */
    @Provides
    @SingleIn(AppScope::class)
    fun provideCoroutineScope(dispatchers: SageDispatchers): CoroutineScope =
        CoroutineScope(dispatchers.computation)

    private fun settingsFile(): File {
        val overridePath = System.getProperty("sage.sample.settingsFile")
        if (overridePath != null) return File(overridePath)

        return File(System.getProperty("user.home"), ".sage-sample/settings.properties")
    }
}
