package net.sigmabeta.sage.sample.ui.navigation
import net.sigmabeta.sage.sample.ui.components.api.text
import net.sigmabeta.sage.sample.nav.widthClassFromDp
import net.sigmabeta.sage.sample.nav.SampleTopBarState
import net.sigmabeta.sage.sample.nav.SampleScreenChrome
import net.sigmabeta.sage.sample.nav.SampleDestination

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animate
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import net.sigmabeta.sage.list.WidthClass
import net.sigmabeta.sage.ui.vector

/**
 * The app's navigation chrome: an animated shared top bar, plus the top-level destinations as a
 * bottom bar on [WidthClass.COMPACT] windows and a rail on anything wider.
 *
 * [selectedDestination] is the *active tab*, not the top screen: a pushed screen keeps its owning
 * tab highlighted in both the bottom bar and the rail. [chrome] comes from the active screen's
 * [SampleChromeController]. Search hides the top bar while it is visible (its overlay search field
 * replaces it); [SampleNavHost] resets the chrome on every route change. The Scaffold uses zero
 * content insets, matching Chipbox: the top and bottom bars supply their own system-bar insets, and
 * a screen that hides the top bar handles the status bar itself (as Search does). The width class is
 * computed once by [SampleNavHost] from the window size (see [widthClassFromDp]); using SAGE's
 * [WidthClass] keeps the nav suite and list/grid layout on the same breakpoints. The rail is a
 * sibling of the [Scaffold] rather than part of it, so it spans the full window height next to the
 * content instead of sitting below the top bar.
 *
 * The top bar collapses as the active list or grid scrolls ([TopAppBarDefaults.enterAlwaysScrollBehavior])
 * and re-expands whenever [activeRouteKey] changes — a tab switch, a deep push or a pop. The bottom
 * bar and rail are outside that collapsing area.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SampleNavScaffold(
    selectedDestination: SampleDestination,
    widthClass: WidthClass,
    chrome: SampleScreenChrome,
    topBar: SampleTopBarState,
    activeRouteKey: String,
    snackbarHostState: SnackbarHostState,
    onSelectDestination: (SampleDestination) -> Unit,
    onOpenSettings: () -> Unit,
    onBack: () -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    val useRail = widthClass != WidthClass.COMPACT
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(topAppBarState)

    // Re-extend the bar on any navigation, not just on a tab switch. contentOffset is the scroll
    // accumulator; heightOffset drives the visible height, so it is tweened back to zero with the
    // behavior's own snap spec. Re-keying cancels the coroutine, so a fast follow-up navigation
    // restarts from the current height instead of jumping.
    LaunchedEffect(activeRouteKey) {
        topAppBarState.contentOffset = 0f
        val spec = scrollBehavior.snapAnimationSpec
        if (spec != null) {
            animate(
                initialValue = topAppBarState.heightOffset,
                targetValue = 0f,
                animationSpec = spec,
            ) { value, _ -> topAppBarState.heightOffset = value }
        } else {
            topAppBarState.heightOffset = 0f
        }
    }

    Row(modifier = Modifier.fillMaxSize()) {
        if (useRail) {
            NavigationRail(modifier = Modifier.safeDrawingPadding()) {
                SampleDestination.topLevel.forEach { item ->
                    NavigationRailItem(
                        selected = item == selectedDestination,
                        onClick = { onSelectDestination(item) },
                        icon = { DestinationIcon(item) },
                        label = { Text(item.label.text()) },
                    )
                }
            }
        }

        Scaffold(
            // Take the space left over beside the rail (or the whole window when there is none).
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                AnimatedVisibility(
                    visible = chrome.showTopBar,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    SampleTopBar(
                        state = topBar,
                        scrollBehavior = scrollBehavior,
                        onOpenSettings = onOpenSettings,
                        onBack = onBack,
                    )
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (!useRail) {
                    NavigationBar {
                        SampleDestination.topLevel.forEach { item ->
                            NavigationBarItem(
                                selected = item == selectedDestination,
                                onClick = { onSelectDestination(item) },
                                icon = { DestinationIcon(item) },
                                label = { Text(item.label.text()) },
                            )
                        }
                    }
                }
            },
            content = content,
        )
    }
}

/** The item's label is always rendered next to the icon, so the icon can stay decorative. */
@Composable
private fun DestinationIcon(destination: SampleDestination) {
    Icon(
        imageVector = destination.icon.vector(),
        contentDescription = null,
    )
}
