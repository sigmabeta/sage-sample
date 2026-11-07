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
import net.sigmabeta.sage.sample.feature.contentdetail.ContentDetailStringId
import net.sigmabeta.sage.sample.feature.home.HomeStringId
import net.sigmabeta.sage.sample.feature.listitems.ListItemsStringId
import net.sigmabeta.sage.sample.feature.search.SearchStringId
import net.sigmabeta.sage.sample.feature.settings.SettingsStringId
import net.sigmabeta.sage.sample.storage.PreferencesStorage
import net.sigmabeta.sage.sample.storage.settingsDataStore
import net.sigmabeta.sage.sample.ui.components.api.ComponentStringId
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
                is HomeStringId -> homeResource(id)
                is BrowseStringId -> browseResource(id)
                is ContentDetailStringId -> contentDetailResource(id)
                is ListItemsStringId -> listItemsResource(id)
                is SearchStringId -> searchResource(id)
                is SettingsStringId -> settingsResource(id)
                is ComponentStringId -> componentResource(id)
                else -> error("No R.string mapping for $id")
            }
        }

    private fun homeResource(id: HomeStringId): Int = when (id) {
        HomeStringId.HOME_TITLE -> R.string.home_title
        HomeStringId.HOME_SECTION_BIG_IMAGE -> R.string.home_section_big_image
        HomeStringId.HOME_SECTION_GRID_IMAGES -> R.string.home_section_grid_images
        HomeStringId.HOME_ITEM_CLICKED -> R.string.home_item_clicked
        HomeStringId.HOME_EMPTY -> R.string.home_empty
        HomeStringId.HOME_ALL_ERROR -> R.string.home_all_error
    }

    private fun browseResource(id: BrowseStringId): Int = when (id) {
        BrowseStringId.BROWSE_TITLE -> R.string.nav_browse
        BrowseStringId.BROWSE_OPEN_LIST_ITEMS -> R.string.nav_list_items
        BrowseStringId.BROWSE_OPEN_LIST_ITEMS_WITH_IMAGES -> R.string.nav_list_items_with_images
        BrowseStringId.BROWSE_OPEN_GRID_OF_IMAGES -> R.string.nav_grid_of_images
        BrowseStringId.BROWSE_OPEN_CONTENT_DETAIL -> R.string.nav_content_detail
    }

    private fun contentDetailResource(id: ContentDetailStringId): Int = when (id) {
        ContentDetailStringId.CONTENT_DETAIL_TITLE -> R.string.nav_content_detail
        ContentDetailStringId.CONTENT_DETAIL_SECTION_DETAILS -> R.string.content_detail_section_details
        ContentDetailStringId.CONTENT_DETAIL_SECTION_RELATED -> R.string.content_detail_section_related
        ContentDetailStringId.CONTENT_DETAIL_SECTION_ITEMS -> R.string.content_detail_section_items
        ContentDetailStringId.CONTENT_DETAIL_ITEM_CLICKED -> R.string.content_detail_item_clicked
    }

    private fun listItemsResource(id: ListItemsStringId): Int = when (id) {
        ListItemsStringId.LIST_ITEMS_TITLE -> R.string.nav_list_items
        ListItemsStringId.LIST_ITEMS_ITEM_CLICKED -> R.string.list_items_item_clicked
        ListItemsStringId.LIST_ITEMS_WITH_IMAGES_TITLE -> R.string.nav_list_items_with_images
        ListItemsStringId.LIST_ITEMS_WITH_IMAGES_ITEM_CLICKED -> R.string.list_items_item_clicked
        ListItemsStringId.GRID_OF_IMAGES_TITLE -> R.string.nav_grid_of_images
        ListItemsStringId.GRID_OF_IMAGES_ITEM_CLICKED -> R.string.list_items_item_clicked
    }

    private fun searchResource(id: SearchStringId): Int = when (id) {
        SearchStringId.SEARCH_TITLE -> R.string.nav_search
        SearchStringId.SEARCH_HINT -> R.string.search_hint
        SearchStringId.SEARCH_CLEAR_DESCRIPTION -> R.string.search_clear_description
        SearchStringId.SEARCH_BACK_DESCRIPTION -> R.string.search_back_description
        SearchStringId.SEARCH_PROMPT -> R.string.search_prompt
        SearchStringId.SEARCH_NO_RESULTS -> R.string.search_no_results
        SearchStringId.SEARCH_RESULT_CLICKED -> R.string.search_result_clicked
        SearchStringId.SEARCH_SECTION_LIST -> R.string.search_section_list
        SearchStringId.SEARCH_SECTION_GRID -> R.string.search_section_grid
    }

    private fun settingsResource(id: SettingsStringId): Int = when (id) {
        SettingsStringId.SETTINGS_TITLE -> R.string.nav_settings
        SettingsStringId.SETTINGS_SECTION_APPEARANCE -> R.string.settings_section_appearance
        SettingsStringId.SETTINGS_LABEL_THEME -> R.string.settings_label_theme
        SettingsStringId.SETTINGS_THEME_LIGHT -> R.string.settings_theme_light
        SettingsStringId.SETTINGS_THEME_DARK -> R.string.settings_theme_dark
        SettingsStringId.SETTINGS_THEME_SYSTEM -> R.string.settings_theme_system
        SettingsStringId.SETTINGS_LABEL_FONT -> R.string.settings_label_font
        SettingsStringId.SETTINGS_FONT_STANDARD -> R.string.settings_font_standard
        SettingsStringId.SETTINGS_FONT_INTER -> R.string.settings_font_inter
        SettingsStringId.SETTINGS_SECTION_SIMULATION -> R.string.settings_section_simulation
        SettingsStringId.SETTINGS_LABEL_SIMULATE_DELAY -> R.string.settings_label_simulate_delay
        SettingsStringId.SETTINGS_DELAY_NONE -> R.string.settings_delay_none
        SettingsStringId.SETTINGS_DELAY_ONE_SECOND -> R.string.settings_delay_one_second
        SettingsStringId.SETTINGS_DELAY_TWO_SECONDS -> R.string.settings_delay_two_seconds
        SettingsStringId.SETTINGS_DELAY_FIVE_SECONDS -> R.string.settings_delay_five_seconds
        SettingsStringId.SETTINGS_LABEL_DEMO_OUTCOME -> R.string.settings_label_demo_outcome
        SettingsStringId.SETTINGS_DEMO_OUTCOME_CONTENT -> R.string.settings_demo_outcome_content
        SettingsStringId.SETTINGS_DEMO_OUTCOME_EMPTY -> R.string.settings_demo_outcome_empty
        SettingsStringId.SETTINGS_DEMO_OUTCOME_ERROR -> R.string.settings_demo_outcome_error
        SettingsStringId.SETTINGS_LABEL_HOME_SHOWS_SAME_CONTENT -> R.string.settings_label_home_shows_same_content
        SettingsStringId.SETTINGS_SECTION_ABOUT -> R.string.settings_section_about
        SettingsStringId.SETTINGS_LABEL_APP_VERSION -> R.string.settings_label_app_version
        SettingsStringId.SETTINGS_LABEL_BUILD_DATE -> R.string.settings_label_build_date
    }

    /** Accessibility labels owned by the shared component renderers. */
    private fun componentResource(id: ComponentStringId): Int = when (id) {
        ComponentStringId.ACCY_OCL_CHECKBOX -> R.string.cd_component_checkbox
        ComponentStringId.ACCY_OCL_DROPDOWN -> R.string.cd_component_dropdown
        ComponentStringId.ACCY_OCL_RATING -> R.string.cd_component_rating
        ComponentStringId.ACCY_OCL_SINGLE_LINE -> R.string.cd_component_single_line
        ComponentStringId.ACCY_OCL_VALUE -> R.string.cd_component_value
        ComponentStringId.ACCY_ST_DESC_CHECKED -> R.string.state_component_checked
        ComponentStringId.ACCY_ST_DESC_LOADING -> R.string.state_component_loading
        ComponentStringId.ACCY_ST_DESC_UNCHECKED -> R.string.state_component_unchecked
    }

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
