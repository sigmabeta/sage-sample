package net.sigmabeta.sage.sample.di

import android.content.Context
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import net.sigmabeta.sage.analytics.Analytics
import net.sigmabeta.sage.analytics.NoopAnalytics
import net.sigmabeta.sage.android.logging.AndroidHatchet
import net.sigmabeta.sage.appinfo.AppInfo
import net.sigmabeta.sage.coroutines.SageDispatchers
import net.sigmabeta.sage.di.AppScope
import net.sigmabeta.sage.logging.Hatchet
import net.sigmabeta.sage.sample.BuildConfig
import net.sigmabeta.sage.sample.storage.PreferencesStorage
import net.sigmabeta.sage.sample.storage.settingsDataStore
import net.sigmabeta.sage.sample.strings.real.SampleStringProvider
import net.sigmabeta.sage.storage.common.Storage
import net.sigmabeta.sage.ui.StringProvider

/**
 * App-side implementations of SAGE's platform seams.
 *
 * `@BindingContainer` + `@ContributesTo(AppScope::class)` is the Metro replacement for Hilt's
 * `@Module` + `@InstallIn(SingletonComponent::class)`. Anything contributed here is visible to
 * [SampleAppGraph] without listing it in the graph.
 */
@BindingContainer
@ContributesTo(AppScope::class)
object SampleBindings {
    /**
     * Text is owned by `:core:strings:real`, not by the app: the same provider serves Android and
     * desktop, and is installed at the composition root as well as consumed by view models.
     */
    @Provides
    @SingleIn(AppScope::class)
    fun provideStringProvider(): StringProvider = SampleStringProvider

    @Provides
    @SingleIn(AppScope::class)
    fun provideHatchet(): Hatchet = AndroidHatchet()

    @Provides
    @SingleIn(AppScope::class)
    fun provideAnalytics(hatchet: Hatchet): Analytics = NoopAnalytics(hatchet)

    /**
     * SAGE's key-value storage seam. The settings manager persists the theme, font and
     * simulated-delay choices through it; Android implements it with Preferences DataStore.
     */
    @Provides
    @SingleIn(AppScope::class)
    fun provideStorage(
        context: Context,
        scope: CoroutineScope,
        dispatchers: SageDispatchers,
    ): Storage = PreferencesStorage(
        dataStore = context.settingsDataStore,
        scope = scope,
        dispatchers = dispatchers,
    )

    /** Version info shown in Settings' About section. */
    @Provides
    @SingleIn(AppScope::class)
    fun provideAppInfo(): AppInfo = AppInfo(
        isDebug = BuildConfig.DEBUG,
        versionName = BuildConfig.VERSION_NAME,
        versionCode = BuildConfig.VERSION_CODE,
        buildTimeMs = BuildConfig.BUILD_TIME_MS,
        buildBranch = "local",
    )
}
