package net.sigmabeta.sage.sample.jvm

import net.sigmabeta.sage.sample.feature.browse.BrowseStringId
import net.sigmabeta.sage.sample.feature.listitems.ListItemsStringId
import net.sigmabeta.sage.sample.feature.search.SearchStringId
import net.sigmabeta.sage.sample.feature.settings.SettingsStringId
import net.sigmabeta.sage.sample.feature.welcome.WelcomeStringId
import net.sigmabeta.sage.ui.SageStringId
import net.sigmabeta.sage.ui.StringProvider

/**
 * Desktop implementation of SAGE's [StringProvider] seam.
 *
 * Feature modules stay platform-agnostic by emitting a feature-owned [SageStringId]; this is the
 * one place the desktop app turns those ids into text. The Android app's equivalent mapping
 * lives in `SampleBindings` and points at `R.string`.
 *
 * A table rather than a `when` keeps the id → text mapping readable and makes an unmapped id a
 * single, obvious failure point.
 */
object SampleStringProvider : StringProvider {
    private val strings: Map<SageStringId, String> = mapOf(
        WelcomeStringId.APP_NAME to SampleStrings.APP_NAME,
        WelcomeStringId.WELCOME_TITLE to SampleStrings.WELCOME_TITLE,
        WelcomeStringId.WELCOME_ABOUT_HEADER to SampleStrings.WELCOME_ABOUT_HEADER,
        WelcomeStringId.WELCOME_ABOUT_BODY_COMPOSITE_BUILD to SampleStrings.WELCOME_ABOUT_BODY_COMPOSITE_BUILD,
        WelcomeStringId.WELCOME_ABOUT_BODY_LIST_STATE to SampleStrings.WELCOME_ABOUT_BODY_LIST_STATE,
        WelcomeStringId.WELCOME_OPEN_BROWSE to SampleStrings.WELCOME_OPEN_BROWSE,
        BrowseStringId.BROWSE_TITLE to SampleStrings.NAV_BROWSE,
        BrowseStringId.BROWSE_EMPTY to SampleStrings.BROWSE_EMPTY,
        BrowseStringId.BROWSE_OPEN_LIST_ITEMS to SampleStrings.NAV_LIST_ITEMS,
        BrowseStringId.BROWSE_OPEN_LIST_ITEMS_WITH_IMAGES to SampleStrings.NAV_LIST_ITEMS_WITH_IMAGES,
        BrowseStringId.BROWSE_DEMO_ITEM_LISTS to SampleStrings.BROWSE_DEMO_ITEM_LISTS,
        BrowseStringId.BROWSE_DEMO_ITEM_METRO to SampleStrings.BROWSE_DEMO_ITEM_METRO,
        BrowseStringId.BROWSE_DEMO_ITEM_VOYAGER to SampleStrings.BROWSE_DEMO_ITEM_VOYAGER,
        ListItemsStringId.LIST_ITEMS_TITLE to SampleStrings.NAV_LIST_ITEMS,
        ListItemsStringId.LIST_ITEMS_ITEM_CLICKED to SampleStrings.LIST_ITEMS_ITEM_CLICKED,
        ListItemsStringId.LIST_ITEMS_WITH_IMAGES_TITLE to SampleStrings.NAV_LIST_ITEMS_WITH_IMAGES,
        ListItemsStringId.LIST_ITEMS_WITH_IMAGES_ITEM_CLICKED to SampleStrings.LIST_ITEMS_ITEM_CLICKED,
        SearchStringId.SEARCH_TITLE to SampleStrings.NAV_SEARCH,
        SearchStringId.SEARCH_TODO to SampleStrings.SEARCH_TODO,
        SettingsStringId.SETTINGS_TITLE to SampleStrings.NAV_SETTINGS,
        SettingsStringId.SETTINGS_SECTION_APPEARANCE to SampleStrings.SETTINGS_SECTION_APPEARANCE,
        SettingsStringId.SETTINGS_LABEL_THEME to SampleStrings.SETTINGS_LABEL_THEME,
        SettingsStringId.SETTINGS_THEME_LIGHT to SampleStrings.SETTINGS_THEME_LIGHT,
        SettingsStringId.SETTINGS_THEME_DARK to SampleStrings.SETTINGS_THEME_DARK,
        SettingsStringId.SETTINGS_THEME_SYSTEM to SampleStrings.SETTINGS_THEME_SYSTEM,
        SettingsStringId.SETTINGS_SECTION_SIMULATION to SampleStrings.SETTINGS_SECTION_SIMULATION,
        SettingsStringId.SETTINGS_LABEL_SIMULATE_DELAY to SampleStrings.SETTINGS_LABEL_SIMULATE_DELAY,
        SettingsStringId.SETTINGS_DELAY_NONE to SampleStrings.SETTINGS_DELAY_NONE,
        SettingsStringId.SETTINGS_DELAY_ONE_SECOND to SampleStrings.SETTINGS_DELAY_ONE_SECOND,
        SettingsStringId.SETTINGS_DELAY_TWO_SECONDS to SampleStrings.SETTINGS_DELAY_TWO_SECONDS,
        SettingsStringId.SETTINGS_DELAY_FIVE_SECONDS to SampleStrings.SETTINGS_DELAY_FIVE_SECONDS,
        SettingsStringId.SETTINGS_SECTION_ABOUT to SampleStrings.SETTINGS_SECTION_ABOUT,
        SettingsStringId.SETTINGS_LABEL_APP_VERSION to SampleStrings.SETTINGS_LABEL_APP_VERSION,
        SettingsStringId.SETTINGS_LABEL_BUILD_DATE to SampleStrings.SETTINGS_LABEL_BUILD_DATE,
    )

    override fun getString(string: SageStringId): String =
        strings[string] ?: error("No desktop string mapping for $string")

    override fun getStringOneArg(string: SageStringId, arg: String): String =
        getString(string).format(arg)

    override fun getStringOneInt(string: SageStringId, arg: Int): String =
        getString(string).format(arg)

    override fun getStringTwoArgs(string: SageStringId, first: String, second: String): String =
        getString(string).format(first, second)
}
