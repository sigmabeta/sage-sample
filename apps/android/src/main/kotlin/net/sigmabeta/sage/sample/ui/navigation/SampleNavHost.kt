package net.sigmabeta.sage.sample.ui.navigation

import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.LocalLifecycleOwner
import cafe.adriel.voyager.navigator.tab.CurrentTab
import cafe.adriel.voyager.navigator.tab.TabNavigator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.sigmabeta.sage.appcomm.SageEvent
import net.sigmabeta.sage.components.TitleBarModel
import net.sigmabeta.sage.list.WidthClass
import net.sigmabeta.sage.sample.feature.contentdetail.ContentId
import kotlin.math.roundToInt

/**
 * The app shell: a Voyager [TabNavigator] whose top-level tabs are Home, Browse and Search.
 *
 * Each tab owns its retained inner [cafe.adriel.voyager.navigator.Navigator] (see [SampleTab]), so
 * switching away leaves that tab's root view model and UI state in place. Selecting a tab pops the
 * outgoing tab to its root,
 * discarding any pushed screen; re-tapping the current tab does the same. Feature navigation lands
 * through [LocalSampleNavEffects]: a top-level route selects a tab, a pushed route stays on the
 * active tab, and `NavigateBack` runs the same [backActionFor] policy as the top-bar arrow and
 * system Back. `ShowSnackbar` is rendered by the scaffold's host instead.
 *
 * [onOuterBack] is the policy's `Outer` branch: Android finishes the activity, desktop has nothing
 * above the shell. Chrome, title reporting and width class are hoisted here once for every tab and
 * pushed screen.
 */
@Composable
fun SampleNavHost(onOuterBack: () -> Unit = {}) {
    TabNavigator(tab = HomeTab) { tabNavigator ->
        SampleTabbedShell(tabNavigator = tabNavigator, onOuterBack = onOuterBack)
    }
}

@Composable
private fun SampleTabbedShell(tabNavigator: TabNavigator, onOuterBack: () -> Unit) {
    val activeTabNavigator = remember { SampleActiveTabNavigator() }
    val shellNavigation = rememberShellNavigation(tabNavigator, activeTabNavigator, onOuterBack)
    val activeNavigator = activeTabNavigator.navigator

    // `activeScreen` is the top of the active tab's stack; before the first tab registers its
    // Navigator (one frame), fall back to the tab's own root so chrome never reads a null screen.
    val currentTab = tabNavigator.current as SampleTab
    val activeScreen = (activeNavigator?.lastItem as? SampleScreen) ?: currentTab.root

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val chromeController = remember { SampleChromeController() }

    // A new tab or pushed screen starts with the default shell chrome; a screen may hide the top
    // bar from its own LaunchedEffect (Search does). The same route key re-expands the scrolling
    // top bar, so a push/pop within the active tab resets it just like a tab switch.
    val activeRouteKey = routeKeyFor(currentTab.key, activeScreen.key)
    LaunchedEffect(activeRouteKey) {
        chromeController.set(SampleScreenChrome.Default)
    }

    var titleBar by remember(activeScreen) { mutableStateOf<TitleBarModel?>(null) }
    val titleBarSink = remember(activeScreen) {
        SampleTitleBarSink { reported -> titleBar = reported }
    }
    val widthClass = currentWidthClass()
    val navEffects = remember(activeNavigator, shellNavigation, snackbarHostState) {
        SampleNavEffects { event ->
            handleEffect(event, shellNavigation, snackbarHostState, coroutineScope)
        }
    }

    val topBar = topBarState(
        titleBar = titleBar,
        destinationLabel = stringResource(activeScreen.destination.labelRes),
        isTopLevel = activeScreen.destination.isTopLevel,
    )

    CompositionLocalProvider(
        LocalSampleChromeController provides chromeController,
        LocalSampleNavEffects provides navEffects,
        LocalSampleTitleBarSink provides titleBarSink,
        LocalSampleWidthClass provides widthClass,
        LocalSampleActiveTabNavigator provides activeTabNavigator,
        LocalSampleBackHandler provides shellNavigation,
        // The activity/process lifecycle: backgrounding a still-composed screen pauses it and
        // returning to the foreground resumes it.
        LocalScreenLifecycleOwner provides LocalLifecycleOwner.current,
    ) {
        // Voyager handles Back while the active tab can pop; at a tab root this outer handler is
        // the one that receives it, so Search-root Back can still reach the shared policy.
        SampleSystemBack(
            enabled = activeNavigator?.canPop != true,
            onBack = shellNavigation::back,
        )

        SampleNavScaffold(
            selectedDestination = currentTab.destination,
            widthClass = widthClass,
            chrome = chromeController.state,
            topBar = topBar,
            activeRouteKey = activeRouteKey,
            snackbarHostState = snackbarHostState,
            onSelectDestination = { destination ->
                tabFor(destination)?.let(shellNavigation::selectTab)
            },
            onOpenSettings = { shellNavigation.push(SampleDestination.SETTINGS) },
            onBack = shellNavigation::back,
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                CurrentTab()
            }
        }
    }
}

@Composable
private fun rememberShellNavigation(
    tabNavigator: TabNavigator,
    activeTabNavigator: SampleActiveTabNavigator,
    onOuterBack: () -> Unit,
): SampleShellNavigation {
    // The host may pass a fresh lambda each recomposition; the navigation object outlives that and
    // still needs the latest outer callback when Back reaches the bottom of the shell.
    val outerBack = rememberUpdatedState(onOuterBack)
    return remember(tabNavigator, activeTabNavigator) {
        SampleShellNavigation(tabNavigator, activeTabNavigator) { outerBack.value() }
    }
}

/**
 * The shell's navigation operations, shared by tab taps, feature events and Back.
 *
 * All three mutate the same two navigators, so keeping them together is what stops the bottom bar,
 * the top-bar arrow and system Back from drifting apart.
 */
private class SampleShellNavigation(
    private val tabNavigator: TabNavigator,
    private val activeTabNavigator: SampleActiveTabNavigator,
    private val onOuterBack: () -> Unit,
) : SampleBackHandler {

    override fun back() {
        val action = backActionFor(
            tab = (tabNavigator.current as SampleTab).destination,
            canPop = activeTabNavigator.navigator?.canPop == true,
        )
        when (action) {
            SampleBackAction.Pop -> activeTabNavigator.navigator?.pop()
            is SampleBackAction.ShowTab -> tabFor(action.destination)?.let(::selectTab)
            SampleBackAction.Outer -> onOuterBack()
        }
    }

    /**
     * Selecting a tab first clears the outgoing tab's pushed screens, then switches. Re-tapping
     * the current tab only runs the clear, so it returns that tab to its root in place.
     */
    fun selectTab(target: SampleTab) {
        activeTabNavigator.navigator?.popAll()
        if (tabNavigator.current.key != target.key) {
            tabNavigator.current = target
        }
    }

    /**
     * Pushes onto the active tab; a pushed screen belongs to the tab that was visible.
     *
     * [contentId] is the decoded Content detail ID, `null` for its not-found state.
     */
    fun push(destination: SampleDestination, contentId: ContentId? = null) {
        val navigator = activeTabNavigator.navigator ?: return
        val target = screenFor(destination, contentId)
        if (navigator.lastItem != target) {
            navigator.push(target)
        }
    }
}

/**
 * Applies a feature's one-shot effect: a snackbar is rendered by the scaffold's host, and every
 * other event goes through [navActionFor] to the shell's navigation operations.
 */
private fun handleEffect(
    event: SageEvent,
    shellNavigation: SampleShellNavigation,
    snackbarHostState: SnackbarHostState,
    scope: CoroutineScope,
) {
    if (event !is SageEvent.ShowSnackbar) {
        when (val action = navActionFor(event)) {
            is SampleNavAction.ShowTopLevel -> tabFor(action.destination)?.let(shellNavigation::selectTab)

            is SampleNavAction.Push ->
                shellNavigation.push(action.destination, action.contentId)

            SampleNavAction.Pop -> shellNavigation.back()

            null -> Unit
        }
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

/** Reads the window width in dp and maps it onto SAGE's [WidthClass]. */
@Composable
private fun currentWidthClass(): WidthClass {
    val windowWidth = LocalWindowInfo.current.containerSize.width
    val density = LocalDensity.current
    val widthDp = with(density) { windowWidth.toDp().value }.roundToInt()
    return widthClassFromDp(widthDp)
}
