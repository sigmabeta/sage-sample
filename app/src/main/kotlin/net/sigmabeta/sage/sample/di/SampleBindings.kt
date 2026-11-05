package net.sigmabeta.sage.sample.di

import android.content.Context
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import net.sigmabeta.sage.analytics.Analytics
import net.sigmabeta.sage.analytics.NoopAnalytics
import net.sigmabeta.sage.android.logging.AndroidHatchet
import net.sigmabeta.sage.di.AppScope
import net.sigmabeta.sage.list.DelayManager
import net.sigmabeta.sage.logging.Hatchet
import net.sigmabeta.sage.sample.R
import net.sigmabeta.sage.sample.feature.browse.BrowseStringId
import net.sigmabeta.sage.sample.feature.search.SearchStringId
import net.sigmabeta.sage.sample.feature.settings.SettingsStringId
import net.sigmabeta.sage.sample.feature.welcome.WelcomeStringId
import net.sigmabeta.sage.ui.SageStringId
import net.sigmabeta.sage.ui.StringProvider
import net.sigmabeta.sage.ui.strings.AndroidStringProvider

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
     * SAGE features read user-facing text through [StringProvider] and a module-owned
     * [SageStringId] enum, so the feature modules stay platform-agnostic. The app is the only
     * place that knows about `R.string`, and it owns one mapping per feature.
     */
    @Provides
    @SingleIn(AppScope::class)
    fun provideStringProvider(context: Context): StringProvider =
        AndroidStringProvider(context.resources) { id ->
            when (id) {
                is WelcomeStringId -> welcomeResource(id)
                is BrowseStringId -> browseResource(id)
                is SearchStringId -> searchResource(id)
                is SettingsStringId -> settingsResource(id)
                else -> error("No R.string mapping for $id")
            }
        }

    private fun welcomeResource(id: WelcomeStringId): Int = when (id) {
        WelcomeStringId.APP_NAME -> R.string.app_name
        WelcomeStringId.WELCOME_TITLE -> R.string.welcome_title
        WelcomeStringId.WELCOME_ABOUT_HEADER -> R.string.welcome_about_header
        WelcomeStringId.WELCOME_ABOUT_BODY_COMPOSITE_BUILD -> R.string.welcome_about_body_composite_build
        WelcomeStringId.WELCOME_ABOUT_BODY_LIST_STATE -> R.string.welcome_about_body_list_state
        WelcomeStringId.WELCOME_OPEN_BROWSE -> R.string.welcome_open_browse
    }

    private fun browseResource(id: BrowseStringId): Int = when (id) {
        BrowseStringId.BROWSE_TITLE -> R.string.nav_browse
        BrowseStringId.BROWSE_TODO -> R.string.browse_todo
    }

    private fun searchResource(id: SearchStringId): Int = when (id) {
        SearchStringId.SEARCH_TITLE -> R.string.nav_search
        SearchStringId.SEARCH_TODO -> R.string.search_todo
    }

    private fun settingsResource(id: SettingsStringId): Int = when (id) {
        SettingsStringId.SETTINGS_TITLE -> R.string.nav_settings
        SettingsStringId.SETTINGS_TODO -> R.string.settings_todo
    }

    @Provides
    @SingleIn(AppScope::class)
    fun provideHatchet(): Hatchet = AndroidHatchet()

    @Provides
    @SingleIn(AppScope::class)
    fun provideAnalytics(hatchet: Hatchet): Analytics = NoopAnalytics(hatchet)

    /**
     * SAGE's debug-delay seam. Each view model builds its own `SageScheduler` around
     * `viewModelScope`; this is the delay policy it consults.
     */
    @Provides
    @SingleIn(AppScope::class)
    fun provideDelayManager(): DelayManager = object : DelayManager {
        override fun shouldDelay() = false
    }
}
