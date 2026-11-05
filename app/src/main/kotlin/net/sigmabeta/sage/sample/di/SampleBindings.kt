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
import net.sigmabeta.sage.sample.R
import net.sigmabeta.sage.sample.feature.browse.BrowseStringId
import net.sigmabeta.sage.sample.feature.listitems.ListItemsStringId
import net.sigmabeta.sage.sample.feature.search.SearchStringId
import net.sigmabeta.sage.sample.feature.settings.SettingsStringId
import net.sigmabeta.sage.sample.feature.welcome.WelcomeStringId
import net.sigmabeta.sage.sample.storage.PreferencesStorage
import net.sigmabeta.sage.sample.storage.settingsDataStore
import net.sigmabeta.sage.storage.common.Storage
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
                is ListItemsStringId -> listItemsResource(id)
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
        BrowseStringId.BROWSE_EMPTY -> R.string.browse_empty
        BrowseStringId.BROWSE_OPEN_LIST_ITEMS -> R.string.nav_list_items
        BrowseStringId.BROWSE_DEMO_ITEM_LISTS -> R.string.browse_demo_item_lists
        BrowseStringId.BROWSE_DEMO_ITEM_METRO -> R.string.browse_demo_item_metro
        BrowseStringId.BROWSE_DEMO_ITEM_VOYAGER -> R.string.browse_demo_item_voyager
    }

    private fun listItemsResource(id: ListItemsStringId): Int = when (id) {
        ListItemsStringId.LIST_ITEMS_TITLE -> R.string.nav_list_items
        ListItemsStringId.LIST_ITEMS_ITEM_CLICKED -> R.string.list_items_item_clicked
    }

    private fun searchResource(id: SearchStringId): Int = when (id) {
        SearchStringId.SEARCH_TITLE -> R.string.nav_search
        SearchStringId.SEARCH_TODO -> R.string.search_todo
    }

    private fun settingsResource(id: SettingsStringId): Int = when (id) {
        SettingsStringId.SETTINGS_TITLE -> R.string.nav_settings
        SettingsStringId.SETTINGS_SECTION_APPEARANCE -> R.string.settings_section_appearance
        SettingsStringId.SETTINGS_LABEL_THEME -> R.string.settings_label_theme
        SettingsStringId.SETTINGS_THEME_LIGHT -> R.string.settings_theme_light
        SettingsStringId.SETTINGS_THEME_DARK -> R.string.settings_theme_dark
        SettingsStringId.SETTINGS_THEME_SYSTEM -> R.string.settings_theme_system
        SettingsStringId.SETTINGS_SECTION_SIMULATION -> R.string.settings_section_simulation
        SettingsStringId.SETTINGS_LABEL_SIMULATE_DELAY -> R.string.settings_label_simulate_delay
        SettingsStringId.SETTINGS_DELAY_NONE -> R.string.settings_delay_none
        SettingsStringId.SETTINGS_DELAY_ONE_SECOND -> R.string.settings_delay_one_second
        SettingsStringId.SETTINGS_DELAY_TWO_SECONDS -> R.string.settings_delay_two_seconds
        SettingsStringId.SETTINGS_DELAY_FIVE_SECONDS -> R.string.settings_delay_five_seconds
        SettingsStringId.SETTINGS_SECTION_ABOUT -> R.string.settings_section_about
        SettingsStringId.SETTINGS_LABEL_APP_VERSION -> R.string.settings_label_app_version
        SettingsStringId.SETTINGS_LABEL_BUILD_DATE -> R.string.settings_label_build_date
    }

    @Provides
    @SingleIn(AppScope::class)
    fun provideHatchet(): Hatchet = AndroidHatchet()

    @Provides
    @SingleIn(AppScope::class)
    fun provideAnalytics(hatchet: Hatchet): Analytics = NoopAnalytics(hatchet)

    /**
     * SAGE's key-value storage seam. The settings manager persists the theme and simulated-delay
     * choices through it; Android implements it with Preferences DataStore.
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
