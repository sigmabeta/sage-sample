package net.sigmabeta.sage.sample.nav

import net.sigmabeta.sage.components.TitleBarModel
import net.sigmabeta.sage.list.WidthClass

/**
 * The shell chrome a screen can request.
 *
 * The sample only has a top bar to hide; the nav suite is always present. [Default] is all bars
 * visible, which is what the navigation host resets to on every destination change.
 */
data class SampleScreenChrome(
    val showTopBar: Boolean = true,
) {
    companion object {
        val Default = SampleScreenChrome()
    }
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
 * The screen the shell treats as active: the registered top of [currentTabKey]'s stack when the
 * registration belongs to that tab, otherwise the tab's own root.
 *
 * The tab registration is installed by a `DisposableEffect`, so for the frame after a tab switch it
 * can still name the outgoing tab's navigator. Honouring that would briefly render the old screen's
 * title, leading affordance and route key in the new tab, so an old registration is ignored.
 *
 * Generic in the screen type so the pure policy (and its test) stays independent of Voyager.
 */
fun <Screen> activeScreenFor(
    currentTabKey: String,
    currentTabRoot: Screen,
    registeredTabKey: String?,
    registeredScreen: Screen?,
): Screen = if (registeredTabKey == currentTabKey) {
    registeredScreen ?: currentTabRoot
} else {
    currentTabRoot
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
