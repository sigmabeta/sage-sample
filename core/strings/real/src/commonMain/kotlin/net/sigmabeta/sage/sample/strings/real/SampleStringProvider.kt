package net.sigmabeta.sage.sample.strings.real

import net.sigmabeta.sage.sample.strings.api.SampleStringId
import net.sigmabeta.sage.ui.SageStringId
import net.sigmabeta.sage.ui.StringProvider

/**
 * The sample's single, shared [StringProvider].
 *
 * Every value the app can render lives here — feature text, navigation labels, accessibility
 * labels and app chrome. Features emit [SampleStringId]s; the shared renderer suite resolves its
 * labels through `LocalSampleStringProvider`; the two apps bind this provider into their graph and
 * install it at the composition root. Neither app owns a text table any more.
 *
 * The map is a table rather than a `when`, so an unmapped id is one obvious failure point: the
 * unit test in `jvmTest` asserts every enum entry is present.
 */
object SampleStringProvider : StringProvider {
    private val strings: Map<SageStringId, String> = mapOf(
        SampleStringId.APP_NAME to "SAGE Sample",

        SampleStringId.NAV_HOME to "Home",
        SampleStringId.NAV_BROWSE to "Browse",
        SampleStringId.NAV_SEARCH to "Search",
        SampleStringId.NAV_LIST_ITEMS to "List Items",
        SampleStringId.NAV_LIST_ITEMS_WITH_IMAGES to "List Items with Images",
        SampleStringId.NAV_GRID_OF_IMAGES to "Grid of images",
        SampleStringId.NAV_CONTENT_DETAIL to "Content detail",
        SampleStringId.NAV_SETTINGS to "Settings",
        SampleStringId.NAV_OPEN_SETTINGS_DESCRIPTION to "Open settings",
        SampleStringId.NAV_BACK_DESCRIPTION to "Back",

        SampleStringId.ACCY_OCL_CHECKBOX to "Toggle checkbox",
        SampleStringId.ACCY_OCL_DROPDOWN to "Expand dropdown",
        SampleStringId.ACCY_OCL_RATING to "Rate",
        SampleStringId.ACCY_OCL_SINGLE_LINE to "Open %1\$s",
        SampleStringId.ACCY_OCL_VALUE to "Open",
        SampleStringId.ACCY_ST_DESC_CHECKED to "Checked",
        SampleStringId.ACCY_ST_DESC_LOADING to "Loading",
        SampleStringId.ACCY_ST_DESC_UNCHECKED to "Unchecked",

        SampleStringId.HOME_TITLE to "SAGE Sample",
        SampleStringId.HOME_SECTION_BIG_IMAGE to "Big image %1\$d",
        SampleStringId.HOME_SECTION_GRID_IMAGES to "Grid images %1\$d",
        SampleStringId.HOME_EMPTY to
            "No Home content is available. Change the demo outcome in Settings to load content.",
        SampleStringId.HOME_ALL_ERROR to
            "Home content could not be loaded. Change the demo outcome in Settings to try again.",

        SampleStringId.BROWSE_TITLE to "Browse",
        SampleStringId.BROWSE_OPEN_LIST_ITEMS to "List Items",
        SampleStringId.BROWSE_OPEN_LIST_ITEMS_WITH_IMAGES to "List Items with Images",
        SampleStringId.BROWSE_OPEN_GRID_OF_IMAGES to "Grid of images",
        SampleStringId.BROWSE_OPEN_CONTENT_DETAIL to "Content detail",

        SampleStringId.CONTENT_DETAIL_TITLE to "Content detail",
        SampleStringId.CONTENT_DETAIL_SECTION_DETAILS to "Details",
        SampleStringId.CONTENT_DETAIL_SECTION_RELATED to "Related",
        SampleStringId.CONTENT_DETAIL_SECTION_ITEMS to "Items",
        SampleStringId.CONTENT_DETAIL_ITEM_CLICKED to "%1\$s clicked",
        SampleStringId.CONTENT_DETAIL_NOT_FOUND to "This content isn't available.",

        SampleStringId.LIST_ITEMS_TITLE to "List Items",
        SampleStringId.LIST_ITEMS_ITEM_CLICKED to "Clicked %1\$s",
        SampleStringId.LIST_ITEMS_EMPTY to "No list items to show.",
        SampleStringId.LIST_ITEMS_ERROR to "Couldn't load list items.",
        SampleStringId.LIST_ITEMS_WITH_IMAGES_TITLE to "List Items with Images",
        SampleStringId.LIST_ITEMS_WITH_IMAGES_ITEM_CLICKED to "Clicked %1\$s",
        SampleStringId.LIST_ITEMS_WITH_IMAGES_EMPTY to "No items with images to show.",
        SampleStringId.LIST_ITEMS_WITH_IMAGES_ERROR to "Couldn't load items with images.",
        SampleStringId.GRID_OF_IMAGES_TITLE to "Grid of images",
        SampleStringId.GRID_OF_IMAGES_EMPTY to "No images to show.",
        SampleStringId.GRID_OF_IMAGES_ERROR to "Couldn't load images.",

        SampleStringId.SEARCH_TITLE to "Search",
        SampleStringId.SEARCH_HINT to "Search",
        SampleStringId.SEARCH_CLEAR_DESCRIPTION to "Clear search",
        SampleStringId.SEARCH_BACK_DESCRIPTION to "Back",
        SampleStringId.SEARCH_PROMPT to "Type at least 3 characters to search.",
        SampleStringId.SEARCH_NO_RESULTS to "No results for \u201c%1\$s\u201d.",
        SampleStringId.SEARCH_SECTION_LIST to "List results",
        SampleStringId.SEARCH_SECTION_GRID to "Grid results",

        SampleStringId.SETTINGS_TITLE to "Settings",
        SampleStringId.SETTINGS_SECTION_APPEARANCE to "Appearance",
        SampleStringId.SETTINGS_LABEL_THEME to "Theme",
        SampleStringId.SETTINGS_THEME_LIGHT to "Light",
        SampleStringId.SETTINGS_THEME_DARK to "Dark",
        SampleStringId.SETTINGS_THEME_SYSTEM to "System",
        SampleStringId.SETTINGS_LABEL_FONT to "Font",
        SampleStringId.SETTINGS_FONT_STANDARD to "Standard",
        SampleStringId.SETTINGS_FONT_INTER to "Inter",
        SampleStringId.SETTINGS_SECTION_SIMULATION to "Simulation",
        SampleStringId.SETTINGS_LABEL_SIMULATE_DELAY to "Simulate delay",
        SampleStringId.SETTINGS_DELAY_NONE to "None",
        SampleStringId.SETTINGS_DELAY_ONE_SECOND to "1 second",
        SampleStringId.SETTINGS_DELAY_TWO_SECONDS to "2 seconds",
        SampleStringId.SETTINGS_DELAY_FIVE_SECONDS to "5 seconds",
        SampleStringId.SETTINGS_LABEL_ANIMATION_SCALE to "Animation scale",
        SampleStringId.SETTINGS_ANIMATION_SCALE_ONE_X to "1x",
        SampleStringId.SETTINGS_ANIMATION_SCALE_TWO_X to "2x",
        SampleStringId.SETTINGS_ANIMATION_SCALE_THREE_X to "3x",
        SampleStringId.SETTINGS_ANIMATION_SCALE_FIVE_X to "5x",
        SampleStringId.SETTINGS_LABEL_DEMO_OUTCOME to "Demo outcome",
        SampleStringId.SETTINGS_DEMO_OUTCOME_CONTENT to "Content",
        SampleStringId.SETTINGS_DEMO_OUTCOME_EMPTY to "Empty",
        SampleStringId.SETTINGS_DEMO_OUTCOME_ERROR to "Error",
        SampleStringId.SETTINGS_LABEL_HOME_SHOWS_SAME_CONTENT to "Home shows same content",
        SampleStringId.SETTINGS_CAPTION_HOME_SHOWS_SAME_CONTENT to
            "Render Home from one stable plan of modules on every visit.",
        SampleStringId.SETTINGS_LABEL_RESET_DEMO_DATA to "Reset demo data",
        SampleStringId.SETTINGS_CAPTION_RESET_DEMO_DATA to
            "Clear recent searches and restore the default demo outcome and Home content.",
        SampleStringId.SETTINGS_RESET_DEMO_DATA_SUCCESS to "Demo data reset",
        SampleStringId.SETTINGS_RESET_DEMO_DATA_FAILURE to
            "Couldn\u2019t reset demo data. Try again.",
        SampleStringId.SETTINGS_SECTION_ABOUT to "About",
        SampleStringId.SETTINGS_LABEL_APP_VERSION to "App version",
        SampleStringId.SETTINGS_LABEL_BUILD_DATE to "Build date",
    )

    override fun getString(string: SageStringId): String =
        strings[string] ?: error("No sample string mapping for $string")

    override fun getStringOneArg(string: SageStringId, arg: String): String =
        formatSampleString(getString(string), listOf(arg))

    override fun getStringOneInt(string: SageStringId, arg: Int): String =
        formatSampleString(getString(string), listOf(arg.toString()))

    override fun getStringTwoArgs(string: SageStringId, first: String, second: String): String =
        formatSampleString(getString(string), listOf(first, second))

    /** Text for [id], for non-composable call sites such as fixtures and window titles. */
    fun string(id: SampleStringId): String = getString(id)
}

/**
 * Multiplatform stand-in for `String.format` over the Android-style specifiers the sample uses:
 * positional `%1$s` / `%2$s` / `%1$d`, then non-positional `%s` / `%d` filled in order.
 */
internal fun formatSampleString(template: String, args: List<String>): String {
    var result = template
    args.forEachIndexed { index, arg ->
        result = result.replace("%${index + 1}\$s", arg).replace("%${index + 1}\$d", arg)
    }

    var argIndex = 0
    val out = StringBuilder(result.length)
    var i = 0
    while (i < result.length) {
        val isSpecifier = result[i] == '%' &&
            i + 1 < result.length &&
            (result[i + 1] == 's' || result[i + 1] == 'd') &&
            argIndex < args.size
        if (isSpecifier) {
            out.append(args[argIndex])
            argIndex++
            i += 2
        } else {
            out.append(result[i])
            i++
        }
    }
    return out.toString()
}
