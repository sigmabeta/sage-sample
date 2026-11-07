package net.sigmabeta.sage.sample.jvm.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideTransition
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.sigmabeta.sage.appcomm.SageEvent
import net.sigmabeta.sage.components.TitleBarModel
import net.sigmabeta.sage.list.WidthClass
import net.sigmabeta.sage.sample.jvm.ui.WithPerScreenViewModelStore
import kotlin.math.roundToInt

/**
 * Owns the app's single Voyager back stack and hands the current screen to [SampleNavScaffold].
 *
 * Unlike Android, the desktop target has no system back gesture: the top bar's back arrow calls
 * [Navigator.pop] directly, and tab selection resets the stack through [showTopLevel].
 *
 * This is also where feature navigation intents land: [LocalSampleNavEffects] translates a
 * one-shot [SageEvent] through [navActionFor] into the same back-stack operations the scaffold's
 * own controls use — a top-level route resets the stack, any other route is pushed. `ShowSnackbar`
 * is rendered by the scaffold's host instead. The chrome is hoisted here too: [LocalSampleTitleBarSink]
 * collects the active screen's SAGE title state into [topBarState], with the destination label and
 * top-level flag as the pre-report fallback, and [LocalSampleWidthClass] is computed once from the
 * window width.
 */
@Composable
fun SampleNavHost() {
    Navigator(screen = HomeScreen) { navigator ->
        val current = navigator.lastItem as SampleScreen
        val snackbarHostState = remember { SnackbarHostState() }
        val coroutineScope = rememberCoroutineScope()
        val navEffects = remember(navigator, snackbarHostState, coroutineScope) {
            SampleNavEffects { event ->
                handleEffect(event, navigator, snackbarHostState, coroutineScope)
            }
        }
        val chromeController = remember { SampleChromeController() }

        // Chipbox's chrome contract: every destination starts with the default shell chrome, and
        // a screen may hide a bar from its own LaunchedEffect. Resetting here (before the screen
        // composes) means Search does not have to restore the top bar when it leaves.
        LaunchedEffect(current) {
            chromeController.set(SampleScreenChrome.Default)
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
            destinationLabel = current.destination.label,
            isTopLevel = current.destination.isTopLevel,
        )

        CompositionLocalProvider(
            LocalSampleChromeController provides chromeController,
            LocalSampleNavEffects provides navEffects,
            LocalSampleTitleBarSink provides titleBarSink,
            LocalSampleWidthClass provides widthClass,
        ) {
            SampleNavScaffold(
                destination = current.destination,
                widthClass = widthClass,
                chrome = chromeController.state,
                topBar = topBar,
                snackbarHostState = snackbarHostState,
                onSelectDestination = { destination -> navigator.showTopLevel(destination) },
                onOpenSettings = { navigator.pushDestination(SampleDestination.SETTINGS) },
                onBack = { navigator.pop() },
            ) { innerPadding ->
                SlideTransition(
                    navigator = navigator,
                    modifier = Modifier.padding(innerPadding).fillMaxSize(),
                ) { screen ->
                    // Voyager only provides per-screen ViewModelStores on Android, so the desktop
                    // app wraps each screen itself; see WithPerScreenViewModelStore.
                    WithPerScreenViewModelStore(screen) {
                        screen.Content()
                    }
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
 * Applies a feature's one-shot effect: a snackbar is rendered by the scaffold's host, and
 * everything else goes through [navActionFor] to the back stack.
 */
private fun handleEffect(
    event: SageEvent,
    navigator: Navigator,
    snackbarHostState: SnackbarHostState,
    scope: CoroutineScope,
) {
    if (event !is SageEvent.ShowSnackbar) {
        navigator.handleEvent(event)
        return
    }

    scope.launch {
        val result = snackbarHostState.showSnackbar(
            message = event.message,
            actionLabel = event.actionDetails?.clickActionLabel,
            withDismissAction = event.withDismissAction,
        )
        if (result == SnackbarResult.ActionPerformed) {
            event.actionDetails?.let { details ->
                details.actionSink.sendAction(details.clickAction)
            }
        }
    }
}

/**
 * Turning a feature event into navigation is app policy; the feature only names a route.
 *
 * [navActionFor] does the translation — a top-level route resets the stack, any other route is
 * pushed — and this applies the result to Voyager. Unknown routes and event types are ignored
 * rather than crashing a screen.
 */
private fun Navigator.handleEvent(event: SageEvent) {
    when (val action = navActionFor(event)) {
        is SampleNavAction.ShowTopLevel -> showTopLevel(action.destination)
        is SampleNavAction.Push -> pushDestination(action.destination)
        SampleNavAction.Pop -> pop()
        null -> Unit
    }
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

/**
 * Pushes a non-top-level destination once. Features reach Settings the same way the hamburger
 * does, without replacing the screen they came from.
 */
private fun Navigator.pushDestination(destination: SampleDestination) {
    val target = screenFor(destination)
    if (lastItem != target) {
        push(target)
    }
}

private fun screenFor(destination: SampleDestination): SampleScreen = when (destination) {
    SampleDestination.HOME -> HomeScreen
    SampleDestination.BROWSE -> BrowseScreen
    SampleDestination.LIST_ITEMS -> ListItemsScreen
    SampleDestination.LIST_ITEMS_WITH_IMAGES -> ListItemsWithImagesScreen
    SampleDestination.GRID_OF_IMAGES -> GridOfImagesScreen
    SampleDestination.SEARCH -> SearchScreen
    SampleDestination.SETTINGS -> SettingsScreen
}
