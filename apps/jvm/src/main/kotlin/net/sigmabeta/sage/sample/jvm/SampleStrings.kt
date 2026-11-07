package net.sigmabeta.sage.sample.jvm

/**
 * The desktop app's user-visible text.
 *
 * The Android app owns the same strings in `apps/android/src/main/res/values/strings.xml`; the desktop
 * target has no Android resources, so it keeps its own copy and maps feature `SageStringId`s to
 * these values in [SampleStringProvider].
 */
object SampleStrings {
    const val APP_NAME = "SAGE Sample"
    const val HOME_TITLE = "SAGE Sample"
    const val HOME_SECTION_BIG_IMAGE = "Big image %d"
    const val HOME_SECTION_GRID_IMAGES = "Grid images %d"
    const val HOME_ITEM_CLICKED = "%s clicked"

    const val NAV_HOME = "Home"
    const val NAV_BROWSE = "Browse"
    const val NAV_SEARCH = "Search"
    const val NAV_LIST_ITEMS = "List Items"
    const val NAV_LIST_ITEMS_WITH_IMAGES = "List Items with Images"
    const val NAV_GRID_OF_IMAGES = "Grid of images"
    const val NAV_CONTENT_DETAIL = "Content detail"
    const val LIST_ITEMS_ITEM_CLICKED = "Clicked %s"
    const val NAV_SETTINGS = "Settings"
    const val CD_OPEN_SETTINGS = "Open settings"
    const val CD_NAVIGATE_BACK = "Back"

    const val CONTENT_DETAIL_SECTION_DETAILS = "Details"
    const val CONTENT_DETAIL_SECTION_RELATED = "Related"
    const val CONTENT_DETAIL_SECTION_ITEMS = "Items"
    const val CONTENT_DETAIL_ITEM_CLICKED = "%s clicked"
    const val CONTENT_DETAIL_NOT_FOUND = "This content isn't available."

    const val SEARCH_HINT = "Search"
    const val SEARCH_CLEAR_DESCRIPTION = "Clear search"
    const val SEARCH_BACK_DESCRIPTION = "Back"
    const val SEARCH_PROMPT = "Type at least 3 characters to search."
    const val SEARCH_NO_RESULTS = "No results for “%s”."
    const val SEARCH_RESULT_CLICKED = "%s clicked"
    const val SEARCH_SECTION_LIST = "List results"
    const val SEARCH_SECTION_GRID = "Grid results"

    const val SETTINGS_SECTION_APPEARANCE = "Appearance"
    const val SETTINGS_LABEL_THEME = "Theme"
    const val SETTINGS_THEME_LIGHT = "Light"
    const val SETTINGS_THEME_DARK = "Dark"
    const val SETTINGS_THEME_SYSTEM = "System"
    const val SETTINGS_LABEL_FONT = "Font"
    const val SETTINGS_FONT_STANDARD = "Standard"
    const val SETTINGS_FONT_INTER = "Inter"

    const val SETTINGS_SECTION_SIMULATION = "Simulation"
    const val SETTINGS_LABEL_SIMULATE_DELAY = "Simulate delay"
    const val SETTINGS_DELAY_NONE = "None"
    const val SETTINGS_DELAY_ONE_SECOND = "1 second"
    const val SETTINGS_DELAY_TWO_SECONDS = "2 seconds"
    const val SETTINGS_DELAY_FIVE_SECONDS = "5 seconds"
    const val SETTINGS_LABEL_HOME_SHOWS_SAME_CONTENT = "Home shows same content"

    const val SETTINGS_SECTION_ABOUT = "About"
    const val SETTINGS_LABEL_APP_VERSION = "App version"
    const val SETTINGS_LABEL_BUILD_DATE = "Build date"

    // Accessibility text for the shared component renderers (:core:ui:components:api).
    const val CD_COMPONENT_CHECKBOX = "Toggle checkbox"
    const val CD_COMPONENT_DROPDOWN = "Expand dropdown"
    const val CD_COMPONENT_RATING = "Rate"
    const val CD_COMPONENT_SINGLE_LINE = "Open %s"
    const val CD_COMPONENT_VALUE = "Open"
    const val STATE_COMPONENT_CHECKED = "Checked"
    const val STATE_COMPONENT_LOADING = "Loading"
    const val STATE_COMPONENT_UNCHECKED = "Unchecked"
}
