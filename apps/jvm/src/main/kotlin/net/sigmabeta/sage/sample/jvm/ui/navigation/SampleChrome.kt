package net.sigmabeta.sage.sample.jvm.ui.navigation

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import net.sigmabeta.sage.components.TitleBarModel
import net.sigmabeta.sage.list.ColumnType
import net.sigmabeta.sage.list.WidthClass

/**
 * The shell chrome a screen can request, mirroring Chipbox's `ScreenChrome`.
 *
 * The sample only has a top bar to hide; the nav suite is always present. [Default] is all bars
 * visible, which is what [SampleNavHost] resets to on every destination change.
 */
data class SampleScreenChrome(
    val showTopBar: Boolean = true,
) {
    companion object {
        val Default = SampleScreenChrome()
    }
}

/**
 * Mutable chrome state shared by the active screen and the shell.
 *
 * A screen hides the top bar by setting this in a `LaunchedEffect`; the next destination resets it
 * to [SampleScreenChrome.Default], so the screen does not need to restore it on disposal.
 */
@Stable
class SampleChromeController {
    var state: SampleScreenChrome by mutableStateOf(SampleScreenChrome.Default)
        private set

    fun set(chrome: SampleScreenChrome) {
        state = chrome
    }
}

/** Provided by [SampleNavHost] around every screen; there is no sensible default. */
val LocalSampleChromeController = staticCompositionLocalOf<SampleChromeController> {
    error("No SampleChromeController provided — render screens inside SampleNavHost.")
}

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
 * Identity of the visible route: the active tab plus the top screen of that tab's stack.
 *
 * Chrome resets and the scrolling top bar's re-expand are keyed on this, so a tab switch, a deep
 * push and a pop each look like a navigation even when two of them land on the same destination.
 */
fun routeKeyFor(tabKey: String, screenKey: String): String = "$tabKey/$screenKey"

/**
 * The screen the shell treats as active: the registered top of [currentTab]'s stack when the
 * registration belongs to that tab, otherwise [currentTab]'s own root.
 *
 * The tab registration is installed by a `DisposableEffect`, so for the frame after a tab switch it
 * can still name the outgoing tab's navigator. Honouring that would briefly render the old screen's
 * title, leading affordance and route key in the new tab, so an old registration is ignored.
 */
fun activeScreenFor(
    currentTab: SampleTab,
    registeredTabKey: String?,
    registeredScreen: SampleScreen?,
): SampleScreen =
    if (registeredTabKey == currentTab.key) {
        registeredScreen ?: currentTab.root
    } else {
        currentTab.root
    }

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
