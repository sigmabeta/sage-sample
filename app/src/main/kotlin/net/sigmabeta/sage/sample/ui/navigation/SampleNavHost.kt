package net.sigmabeta.sage.sample.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideTransition
import net.sigmabeta.sage.appcomm.SageEvent
import net.sigmabeta.sage.components.TitleBarModel
import net.sigmabeta.sage.list.WidthClass
import kotlin.math.roundToInt

/**
 * Owns the app's single Voyager back stack and hands the current screen to [SampleNavScaffold].
 *
 * System back is Voyager's (the navigator pops while it can, and otherwise lets the activity
 * finish); the top bar's back arrow calls [Navigator.pop] directly.
 *
 * This is also where feature navigation intents land: [LocalSampleNavEffects] translates a
 * one-shot [SageEvent] into the same back-stack operations the scaffold's own controls use. The
 * chrome is hoisted here too: [LocalSampleTitleBarSink] collects the active screen's SAGE title
 * state into [topBarState], with the destination label and top-level flag as the pre-report
 * fallback, and [LocalSampleWidthClass] is computed once from the window width.
 */
@Composable
fun SampleNavHost() {
    Navigator(screen = HomeScreen) { navigator ->
        val current = navigator.lastItem as SampleScreen
        val navEffects = remember(navigator) {
            SampleNavEffects { event -> navigator.handleEvent(event) }
        }

        // A new destination starts without a reported title: `remember(current)` drops the previous
        // screen's value so a transition cannot pin a stale title.
        var titleBar by remember(current) { mutableStateOf<TitleBarModel?>(null) }
        val titleBarSink = remember(current) {
            SampleTitleBarSink { reported -> titleBar = reported }
        }
        val widthClass = currentWidthClass()

        val topBar = topBarState(
            titleBar = titleBar,
            destinationLabel = stringResource(current.destination.labelRes),
            isTopLevel = current.destination.isTopLevel,
        )

        CompositionLocalProvider(
            LocalSampleNavEffects provides navEffects,
            LocalSampleTitleBarSink provides titleBarSink,
            LocalSampleWidthClass provides widthClass,
        ) {
            SampleNavScaffold(
                destination = current.destination,
                widthClass = widthClass,
                topBar = topBar,
                onSelectDestination = { destination -> navigator.showTopLevel(destination) },
                onOpenSettings = { navigator.openSettings() },
                onBack = { navigator.pop() },
            ) { innerPadding ->
                SlideTransition(
                    navigator = navigator,
                    modifier = Modifier.padding(innerPadding).fillMaxSize(),
                ) { screen ->
                    screen.Content()
                }
            }
        }
    }
}

/** Reads the window width in dp and maps it onto SAGE's [WidthClass]. */
@Composable
private fun currentWidthClass(): WidthClass {
    val windowWidth = LocalWindowInfo.current.containerSize.width
    val density = LocalDensity.current
    val widthDp = with(density) { windowWidth.toDp().value }.roundToInt()
    return widthClassFromDp(widthDp)
}

/**
 * Turning a feature event into navigation is app policy; the feature only names a route.
 *
 * Routes resolve through [SampleDestination.fromRoute], which matches `RouteDescriptor.destName`.
 * Unknown routes and event types are ignored rather than crashing a screen.
 */
private fun Navigator.handleEvent(event: SageEvent) {
    when (event) {
        is SageEvent.NavigateTo -> navigateToRoute(event.destination)
        is SageEvent.NavigateSingleTopLevel -> navigateToRoute(event.destination)
        is SageEvent.NavigateBack -> pop()
        else -> Unit
    }
}

private fun Navigator.navigateToRoute(route: String) {
    val destination = SampleDestination.fromRoute(route) ?: return
    showTopLevel(destination)
}

/**
 * Switching tabs resets the stack to `[home, target]` (vgls semantics), so back from Browse or
 * Search returns Home instead of leaving the app. Tapping the current tab is a no-op.
 */
private fun Navigator.showTopLevel(destination: SampleDestination) {
    val target = screenFor(destination)
    if (lastItem == target) {
        return
    }

    val screens = if (destination == SampleDestination.HOME) {
        listOf(HomeScreen)
    } else {
        listOf(HomeScreen, target)
    }
    replaceAll(screens)
}

private fun Navigator.openSettings() {
    if (lastItem != SettingsScreen) {
        push(SettingsScreen)
    }
}

private fun screenFor(destination: SampleDestination): SampleScreen = when (destination) {
    SampleDestination.HOME -> HomeScreen
    SampleDestination.BROWSE -> BrowseScreen
    SampleDestination.SEARCH -> SearchScreen
    SampleDestination.SETTINGS -> SettingsScreen
}
