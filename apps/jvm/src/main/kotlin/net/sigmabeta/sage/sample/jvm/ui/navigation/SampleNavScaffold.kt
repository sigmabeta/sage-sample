package net.sigmabeta.sage.sample.jvm.ui.navigation

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import net.sigmabeta.sage.list.WidthClass
import net.sigmabeta.sage.ui.vector

/**
 * The app's navigation chrome: an animated shared top bar, plus the top-level destinations as a
 * bottom bar on [WidthClass.COMPACT] windows and a rail on anything wider.
 *
 * [chrome] comes from the active screen's [SampleChromeController]. Search hides the top bar while
 * it is visible (its overlay search field replaces it); [SampleNavHost] resets the chrome on every
 * destination change. The Scaffold uses zero content insets, matching Chipbox: the top and bottom
 * bars supply their own system-bar insets, and a screen that hides the top bar handles the status
 * bar itself (as Search does). The width class is computed once by [SampleNavHost] from the window
 * size (see [widthClassFromDp]); using SAGE's [WidthClass] keeps the nav suite and list/grid layout
 * on the same breakpoints. The rail is a sibling of the [Scaffold] rather than part of it, so it
 * spans the full window height next to the content instead of sitting below the top bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SampleNavScaffold(
    destination: SampleDestination,
    widthClass: WidthClass,
    chrome: SampleScreenChrome,
    topBar: SampleTopBarState,
    snackbarHostState: SnackbarHostState,
    onSelectDestination: (SampleDestination) -> Unit,
    onOpenSettings: () -> Unit,
    onBack: () -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    val useRail = widthClass != WidthClass.COMPACT

    Row(modifier = Modifier.fillMaxSize()) {
        if (useRail) {
            NavigationRail(modifier = Modifier.safeDrawingPadding()) {
                SampleDestination.topLevel.forEach { item ->
                    NavigationRailItem(
                        selected = item == destination,
                        onClick = { onSelectDestination(item) },
                        icon = { DestinationIcon(item) },
                        label = { Text(item.label) },
                    )
                }
            }
        }

        Scaffold(
            // Take the space left over beside the rail (or the whole window when there is none).
            modifier = Modifier.weight(1f).fillMaxHeight(),
            topBar = {
                AnimatedVisibility(
                    visible = chrome.showTopBar,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    SampleTopBar(
                        state = topBar,
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
                                selected = item == destination,
                                onClick = { onSelectDestination(item) },
                                icon = { DestinationIcon(item) },
                                label = { Text(item.label) },
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
