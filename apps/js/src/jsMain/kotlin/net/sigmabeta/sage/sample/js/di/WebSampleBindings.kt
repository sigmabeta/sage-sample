package net.sigmabeta.sage.sample.js.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import io.ktor.client.HttpClient
import io.ktor.client.engine.js.Js
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.browser.window
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json
import net.sigmabeta.sage.analytics.Analytics
import net.sigmabeta.sage.analytics.NoopAnalytics
import net.sigmabeta.sage.appinfo.AppInfo
import net.sigmabeta.sage.coroutines.SageDispatchers
import net.sigmabeta.sage.di.AppScope
import net.sigmabeta.sage.logging.BasicHatchet
import net.sigmabeta.sage.logging.Hatchet
import net.sigmabeta.sage.sample.js.WebBuildInfo
import net.sigmabeta.sage.sample.js.navigation.BrowserHistoryNavigation
import net.sigmabeta.sage.sample.js.storage.LocalStorageStorage
import net.sigmabeta.sage.sample.repository.remote.SampleApiClient
import net.sigmabeta.sage.sample.strings.real.SampleStringProvider
import net.sigmabeta.sage.sample.ui.shell.SampleBrowserNavigation
import net.sigmabeta.sage.storage.common.Storage
import net.sigmabeta.sage.ui.StringProvider

/**
 * Browser implementations of SAGE's platform seams.
 *
 * The twin of `JvmSampleBindings`/`SampleBindings`: the graph, features and repository interfaces
 * are shared, and this container supplies what only a browser can. The remote repository
 * implementations in `:core:repository:remote` are discovered from this module's dependencies, so
 * there is no per-repository provider here.
 */
@BindingContainer
@ContributesTo(AppScope::class)
object WebSampleBindings {
    @Provides
    @SingleIn(AppScope::class)
    fun provideStringProvider(): StringProvider = SampleStringProvider

    @Provides
    @SingleIn(AppScope::class)
    fun provideHatchet(): Hatchet = BasicHatchet()

    @Provides
    @SingleIn(AppScope::class)
    fun provideAnalytics(hatchet: Hatchet): Analytics = NoopAnalytics(hatchet)

    /** Version info shown in Settings' About section. */
    @Provides
    @SingleIn(AppScope::class)
    fun provideAppInfo(): AppInfo = AppInfo(
        isDebug = false,
        versionName = WebBuildInfo.VERSION_NAME,
        versionCode = WebBuildInfo.VERSION_CODE,
        buildTimeMs = WebBuildInfo.BUILD_TIME_MS,
        buildBranch = WebBuildInfo.BUILD_BRANCH,
    )

    /** Settings persist in `window.localStorage`, so they survive reloads and sync across tabs. */
    @Provides
    @SingleIn(AppScope::class)
    fun provideStorage(): Storage = LocalStorageStorage()

    /**
     * The browser has no blocking IO pool: computation, "disk" and network all run on the JS event
     * loop's Default dispatcher, and `main` is the browser main dispatcher.
     */
    @Provides
    @SingleIn(AppScope::class)
    fun provideDispatchers(): SageDispatchers = SageDispatchers(
        computation = Dispatchers.Default,
        disk = Dispatchers.Default,
        network = Dispatchers.Default,
        main = Dispatchers.Main,
    )

    @Provides
    @SingleIn(AppScope::class)
    fun provideCoroutineScope(dispatchers: SageDispatchers): CoroutineScope =
        CoroutineScope(SupervisorJob() + dispatchers.computation)

    @Provides
    @SingleIn(AppScope::class)
    fun provideHttpClient(): HttpClient = HttpClient(Js) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    /**
     * The API lives on the page's origin by default. `?api=` overrides it for a webpack dev server
     * running on a different port from `apps:server`.
     */
    @Provides
    @SingleIn(AppScope::class)
    fun provideApiClient(httpClient: HttpClient): SampleApiClient =
        SampleApiClient(httpClient, apiBaseUrl())

    @Provides
    @SingleIn(AppScope::class)
    fun provideBrowserNavigation(): SampleBrowserNavigation = BrowserHistoryNavigation()

    private fun apiBaseUrl(): String = window.location.search
        .removePrefix("?")
        .split('&')
        .firstOrNull { it.startsWith("api=") }
        ?.substringAfter("api=")
        ?.takeIf { it.isNotBlank() }
        ?: window.location.origin
}
