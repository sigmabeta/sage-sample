package net.sigmabeta.sage.sample.jvm

import net.sigmabeta.sage.sample.feature.browse.BrowseStringId
import net.sigmabeta.sage.sample.feature.contentdetail.ContentDetailStringId
import net.sigmabeta.sage.sample.feature.home.HomeStringId
import net.sigmabeta.sage.sample.feature.listitems.ListItemsStringId
import net.sigmabeta.sage.sample.feature.search.SearchStringId
import net.sigmabeta.sage.sample.feature.settings.SettingsStringId
import net.sigmabeta.sage.sample.ui.components.api.ComponentStringId
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
        HomeStringId.HOME_TITLE to SampleStrings.HOME_TITLE,
        HomeStringId.HOME_SECTION_BIG_IMAGE to SampleStrings.HOME_SECTION_BIG_IMAGE,
        HomeStringId.HOME_SECTION_GRID_IMAGES to SampleStrings.HOME_SECTION_GRID_IMAGES,
        HomeStringId.HOME_ITEM_CLICKED to SampleStrings.HOME_ITEM_CLICKED,
        BrowseStringId.BROWSE_TITLE to SampleStrings.NAV_BROWSE,
        BrowseStringId.BROWSE_OPEN_LIST_ITEMS to SampleStrings.NAV_LIST_ITEMS,
        BrowseStringId.BROWSE_OPEN_LIST_ITEMS_WITH_IMAGES to SampleStrings.NAV_LIST_ITEMS_WITH_IMAGES,
        BrowseStringId.BROWSE_OPEN_GRID_OF_IMAGES to SampleStrings.NAV_GRID_OF_IMAGES,
        BrowseStringId.BROWSE_OPEN_CONTENT_DETAIL to SampleStrings.NAV_CONTENT_DETAIL,
        ContentDetailStringId.CONTENT_DETAIL_TITLE to SampleStrings.NAV_CONTENT_DETAIL,
        ContentDetailStringId.CONTENT_DETAIL_SECTION_DETAILS to
            SampleStrings.CONTENT_DETAIL_SECTION_DETAILS,
        ContentDetailStringId.CONTENT_DETAIL_SECTION_RELATED to
            SampleStrings.CONTENT_DETAIL_SECTION_RELATED,
        ContentDetailStringId.CONTENT_DETAIL_SECTION_ITEMS to
            SampleStrings.CONTENT_DETAIL_SECTION_ITEMS,
        ContentDetailStringId.CONTENT_DETAIL_ITEM_CLICKED to
            SampleStrings.CONTENT_DETAIL_ITEM_CLICKED,
        ContentDetailStringId.CONTENT_DETAIL_NOT_FOUND to SampleStrings.CONTENT_DETAIL_NOT_FOUND,
        ListItemsStringId.LIST_ITEMS_TITLE to SampleStrings.NAV_LIST_ITEMS,
        ListItemsStringId.LIST_ITEMS_ITEM_CLICKED to SampleStrings.LIST_ITEMS_ITEM_CLICKED,
        ListItemsStringId.LIST_ITEMS_WITH_IMAGES_TITLE to SampleStrings.NAV_LIST_ITEMS_WITH_IMAGES,
        ListItemsStringId.LIST_ITEMS_WITH_IMAGES_ITEM_CLICKED to SampleStrings.LIST_ITEMS_ITEM_CLICKED,
        ListItemsStringId.GRID_OF_IMAGES_TITLE to SampleStrings.NAV_GRID_OF_IMAGES,
        ListItemsStringId.GRID_OF_IMAGES_ITEM_CLICKED to SampleStrings.LIST_ITEMS_ITEM_CLICKED,
        SearchStringId.SEARCH_TITLE to SampleStrings.NAV_SEARCH,
        SearchStringId.SEARCH_HINT to SampleStrings.SEARCH_HINT,
        SearchStringId.SEARCH_CLEAR_DESCRIPTION to SampleStrings.SEARCH_CLEAR_DESCRIPTION,
        SearchStringId.SEARCH_BACK_DESCRIPTION to SampleStrings.SEARCH_BACK_DESCRIPTION,
        SearchStringId.SEARCH_PROMPT to SampleStrings.SEARCH_PROMPT,
        SearchStringId.SEARCH_NO_RESULTS to SampleStrings.SEARCH_NO_RESULTS,
        SearchStringId.SEARCH_RESULT_CLICKED to SampleStrings.SEARCH_RESULT_CLICKED,
        SearchStringId.SEARCH_SECTION_LIST to SampleStrings.SEARCH_SECTION_LIST,
        SearchStringId.SEARCH_SECTION_GRID to SampleStrings.SEARCH_SECTION_GRID,
        SettingsStringId.SETTINGS_TITLE to SampleStrings.NAV_SETTINGS,
        SettingsStringId.SETTINGS_SECTION_APPEARANCE to SampleStrings.SETTINGS_SECTION_APPEARANCE,
        SettingsStringId.SETTINGS_LABEL_THEME to SampleStrings.SETTINGS_LABEL_THEME,
        SettingsStringId.SETTINGS_THEME_LIGHT to SampleStrings.SETTINGS_THEME_LIGHT,
        SettingsStringId.SETTINGS_THEME_DARK to SampleStrings.SETTINGS_THEME_DARK,
        SettingsStringId.SETTINGS_THEME_SYSTEM to SampleStrings.SETTINGS_THEME_SYSTEM,
        SettingsStringId.SETTINGS_LABEL_FONT to SampleStrings.SETTINGS_LABEL_FONT,
        SettingsStringId.SETTINGS_FONT_STANDARD to SampleStrings.SETTINGS_FONT_STANDARD,
        SettingsStringId.SETTINGS_FONT_INTER to SampleStrings.SETTINGS_FONT_INTER,
        SettingsStringId.SETTINGS_SECTION_SIMULATION to SampleStrings.SETTINGS_SECTION_SIMULATION,
        SettingsStringId.SETTINGS_LABEL_SIMULATE_DELAY to SampleStrings.SETTINGS_LABEL_SIMULATE_DELAY,
        SettingsStringId.SETTINGS_DELAY_NONE to SampleStrings.SETTINGS_DELAY_NONE,
        SettingsStringId.SETTINGS_DELAY_ONE_SECOND to SampleStrings.SETTINGS_DELAY_ONE_SECOND,
        SettingsStringId.SETTINGS_DELAY_TWO_SECONDS to SampleStrings.SETTINGS_DELAY_TWO_SECONDS,
        SettingsStringId.SETTINGS_DELAY_FIVE_SECONDS to SampleStrings.SETTINGS_DELAY_FIVE_SECONDS,
        SettingsStringId.SETTINGS_LABEL_DEMO_OUTCOME to SampleStrings.SETTINGS_LABEL_DEMO_OUTCOME,
        SettingsStringId.SETTINGS_DEMO_OUTCOME_CONTENT to
            SampleStrings.SETTINGS_DEMO_OUTCOME_CONTENT,
        SettingsStringId.SETTINGS_DEMO_OUTCOME_EMPTY to SampleStrings.SETTINGS_DEMO_OUTCOME_EMPTY,
        SettingsStringId.SETTINGS_DEMO_OUTCOME_ERROR to SampleStrings.SETTINGS_DEMO_OUTCOME_ERROR,
        SettingsStringId.SETTINGS_LABEL_HOME_SHOWS_SAME_CONTENT to
            SampleStrings.SETTINGS_LABEL_HOME_SHOWS_SAME_CONTENT,
        SettingsStringId.SETTINGS_SECTION_ABOUT to SampleStrings.SETTINGS_SECTION_ABOUT,
        SettingsStringId.SETTINGS_LABEL_APP_VERSION to SampleStrings.SETTINGS_LABEL_APP_VERSION,
        SettingsStringId.SETTINGS_LABEL_BUILD_DATE to SampleStrings.SETTINGS_LABEL_BUILD_DATE,
        ComponentStringId.ACCY_OCL_CHECKBOX to SampleStrings.CD_COMPONENT_CHECKBOX,
        ComponentStringId.ACCY_OCL_DROPDOWN to SampleStrings.CD_COMPONENT_DROPDOWN,
        ComponentStringId.ACCY_OCL_RATING to SampleStrings.CD_COMPONENT_RATING,
        ComponentStringId.ACCY_OCL_SINGLE_LINE to SampleStrings.CD_COMPONENT_SINGLE_LINE,
        ComponentStringId.ACCY_OCL_VALUE to SampleStrings.CD_COMPONENT_VALUE,
        ComponentStringId.ACCY_ST_DESC_CHECKED to SampleStrings.STATE_COMPONENT_CHECKED,
        ComponentStringId.ACCY_ST_DESC_LOADING to SampleStrings.STATE_COMPONENT_LOADING,
        ComponentStringId.ACCY_ST_DESC_UNCHECKED to SampleStrings.STATE_COMPONENT_UNCHECKED,
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
