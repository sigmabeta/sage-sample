package net.sigmabeta.sage.sample.jvm.ui.navigation

import androidx.compose.runtime.staticCompositionLocalOf
import net.sigmabeta.sage.components.TitleBarModel
import net.sigmabeta.sage.list.ColumnType
import net.sigmabeta.sage.list.WidthClass

/**
 * Which leading affordance the shared top bar shows.
 *
 * [MENU] opens Settings; [BACK] pops the current screen. The choice comes from the active screen's
 * SAGE title state, not from the destination alone.
 */
enum class SampleTopBarLeading { MENU, BACK }

/** Everything the shared top bar renders: the active screen's title and its leading affordance. */
data class SampleTopBarState(
    val title: String,
    val leading: SampleTopBarLeading,
)

/**
 * Derives the top bar from the active screen's [TitleBarModel].
 *
 * The screen owns the title while it is rendered; [destinationLabel] and [isTopLevel] are only the
 * fallback used before the screen reports (and while a screen reports a null title). SAGE defaults
 * `shouldShowBack` to `true`, which matches "below the top level ⇒ back": a screen that wants the
 * hamburger states `shouldShowBack = false` explicitly.
 */
fun topBarState(
    titleBar: TitleBarModel?,
    destinationLabel: String,
    isTopLevel: Boolean,
): SampleTopBarState = SampleTopBarState(
    title = titleBar?.title ?: destinationLabel,
    leading = if (titleBar?.shouldShowBack ?: !isTopLevel) {
        SampleTopBarLeading.BACK
    } else {
        SampleTopBarLeading.MENU
    },
)

/**
 * Maps a window width in dp onto SAGE's [WidthClass] breakpoints: 600 dp and 840 dp.
 *
 * Shared by the navigation suite (bottom bar below 600 dp, rail at or above it) and by layout
 * selection, so both agree on what "compact" means.
 */
fun widthClassFromDp(widthDp: Int): WidthClass = when {
    widthDp < 600 -> WidthClass.COMPACT
    widthDp < 840 -> WidthClass.MEDIUM
    else -> WidthClass.EXPANDED
}

/**
 * How the app renders a screen's items.
 *
 * [List] is SAGE's single-column `ListScreen`, the presentation every current screen keeps. [Grid]
 * is SAGE's `GridScreen`, including its staggered variant.
 */
sealed interface SampleScreenLayout {
    data object List : SampleScreenLayout

    data class Grid(
        val columns: Int,
        val staggered: Boolean,
        val allowHorizScroller: Boolean,
    ) : SampleScreenLayout
}

/**
 * Chooses between the list and grid presentations from the screen's SAGE [ColumnType] and the
 * window's [WidthClass].
 *
 * At most one column always means [SampleScreenLayout.List], so a screen that does not opt into
 * columns keeps exactly its existing presentation. That includes zero columns: SAGE's
 * `numberOfColumns` is integer division, so a wide `Regular`/`Staggered` descriptor on a narrow
 * window yields 0, which is the same single-column case rather than an error.
 */
fun screenLayoutFor(columnType: ColumnType, widthClass: WidthClass): SampleScreenLayout {
    val columns = columnType.numberOfColumns(widthClass)

    return if (columns <= 1) {
        SampleScreenLayout.List
    } else {
        SampleScreenLayout.Grid(
            columns = columns,
            staggered = columnType is ColumnType.Staggered,
            allowHorizScroller = (columnType as? ColumnType.Staggered)?.allowHorizScroller ?: false,
        )
    }
}

/**
 * The active screen's report of its SAGE title state.
 *
 * The screen calls this whenever [net.sigmabeta.sage.list.ListStateActual.title] changes;
 * [SampleNavHost] holds the value and derives the shared top bar from it.
 */
fun interface SampleTitleBarSink {
    fun onTitleBar(titleBar: TitleBarModel)
}

/** Provided by [SampleNavHost] around the current screen; there is no sensible default. */
val LocalSampleTitleBarSink = staticCompositionLocalOf<SampleTitleBarSink> {
    error("No SampleTitleBarSink provided — render screens inside SampleNavHost.")
}

/** The current window's SAGE [WidthClass]; provided once by [SampleNavHost]. */
val LocalSampleWidthClass = staticCompositionLocalOf<WidthClass> {
    error("No WidthClass provided — render screens inside SampleNavHost.")
}
