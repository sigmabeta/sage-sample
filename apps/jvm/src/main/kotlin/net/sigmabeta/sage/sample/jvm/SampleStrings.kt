package net.sigmabeta.sage.sample.jvm

/**
 * The desktop app's user-visible text.
 *
 * The Android app owns the same strings in `app/src/main/res/values/strings.xml`; the desktop
 * target has no Android resources, so it keeps its own copy and maps feature `SageStringId`s to
 * these values in [SampleStringProvider].
 */
object SampleStrings {
    const val APP_NAME = "SAGE Sample"
    const val WELCOME_TITLE = "SAGE Sample"
    const val WELCOME_ABOUT_HEADER = "About"
    const val WELCOME_ABOUT_BODY_COMPOSITE_BUILD =
        "SAGE is consumed as a Gradle composite build from the sage/ submodule."
    const val WELCOME_ABOUT_BODY_LIST_STATE =
        "Each feature is a pure JVM module that produces a ListState rendered by SAGE's ListScreen."
    const val WELCOME_OPEN_BROWSE = "Open Browse"

    const val NAV_HOME = "Home"
    const val NAV_BROWSE = "Browse"
    const val NAV_SEARCH = "Search"
    const val NAV_LIST_ITEMS = "List Items"
    const val NAV_LIST_ITEMS_WITH_IMAGES = "List Items with Images"
    const val LIST_ITEMS_ITEM_CLICKED = "Clicked %s"
    const val NAV_SETTINGS = "Settings"
    const val CD_OPEN_SETTINGS = "Open settings"
    const val CD_NAVIGATE_BACK = "Back"

    const val BROWSE_EMPTY = "Nothing to browse yet."
    const val BROWSE_DEMO_ITEM_LISTS = "SAGE lists"
    const val BROWSE_DEMO_ITEM_METRO = "Metro view models"
    const val BROWSE_DEMO_ITEM_VOYAGER = "Voyager navigation"

    const val SEARCH_TODO = "TODO: search content"

    const val SETTINGS_SECTION_APPEARANCE = "Appearance"
    const val SETTINGS_LABEL_THEME = "Theme"
    const val SETTINGS_THEME_LIGHT = "Light"
    const val SETTINGS_THEME_DARK = "Dark"
    const val SETTINGS_THEME_SYSTEM = "System"

    const val SETTINGS_SECTION_SIMULATION = "Simulation"
    const val SETTINGS_LABEL_SIMULATE_DELAY = "Simulate delay"
    const val SETTINGS_DELAY_NONE = "None"
    const val SETTINGS_DELAY_ONE_SECOND = "1 second"
    const val SETTINGS_DELAY_TWO_SECONDS = "2 seconds"
    const val SETTINGS_DELAY_FIVE_SECONDS = "5 seconds"

    const val SETTINGS_SECTION_ABOUT = "About"
    const val SETTINGS_LABEL_APP_VERSION = "App version"
    const val SETTINGS_LABEL_BUILD_DATE = "Build date"
}
