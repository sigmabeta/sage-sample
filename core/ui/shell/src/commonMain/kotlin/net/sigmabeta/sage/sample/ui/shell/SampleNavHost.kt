package net.sigmabeta.sage.sample.ui.shell

import net.sigmabeta.sage.sample.models.ContentArtworkSize
import net.sigmabeta.sage.sample.models.ContentId
import net.sigmabeta.sage.sample.nav.SampleBackAction
import net.sigmabeta.sage.sample.nav.SampleDestination
import net.sigmabeta.sage.sample.nav.SampleNavAction
import net.sigmabeta.sage.sample.nav.SampleRoute
import net.sigmabeta.sage.sample.nav.SampleScreenChrome
import net.sigmabeta.sage.sample.nav.activeScreenFor
import net.sigmabeta.sage.sample.nav.backActionFor
import net.sigmabeta.sage.sample.nav.navActionFor
import net.sigmabeta.sage.sample.nav.routeKeyFor
import net.sigmabeta.sage.sample.nav.topBarState
import net.sigmabeta.sage.sample.nav.widthClassFromDp
import net.sigmabeta.sage.sample.ui.components.api.LocalSharedTransitionScope
import net.sigmabeta.sage.sample.ui.components.api.text

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import cafe.adriel.voyager.navigator.tab.TabNavigator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.sigmabeta.sage.appcomm.SageEvent
import net.sigmabeta.sage.components.TitleBarModel
import net.sigmabeta.sage.list.WidthClass
import kotlin.math.roundToInt

/**
 * The app shell: a Voyager [TabNavigator] whose top-level tabs are Home, Browse and Search.
 *
 * Each tab owns its retained inner [cafe.adriel.voyager.navigator.Navigator] (see [SampleTab]), so
 * switching away leaves that tab's root view model and UI state in place. Visiting a top-level
 * screen slides the old tab down and out while the new one slides in (see [SampleCurrentTab]).
 * A tab left through a switch keeps its stack on screen for that exit and reopens at its root
 * the next time it is shown; re-tapping the current tab returns it to its root in place. Feature
 * navigation lands through [LocalSampleNavEffects]: a top-level route selects a tab, a pushed
 * route stays on the active tab, and `NavigateBack` runs the same [backActionFor] policy as the
 * top-bar arrow and platform Back. `ShowSnackbar` is rendered by the scaffold's host instead.
 *
 * [onOuterBack] is the policy's `Outer` branch: the host decides what lies above the shell (the
 * Android activity finishes; desktop has nothing). Chrome, title reporting and width class are
 * hoisted here once for every tab and pushed screen.
 */
@Composable
fun SampleNavHost(onOuterBack: () -> Unit = {}) {
    TabNavigator(tab = HomeTab) { tabNavigator ->
        SampleTabbedShell(tabNavigator = tabNavigator, onOuterBack = onOuterBack)
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun SampleTabbedShell(tabNavigator: TabNavigator, onOuterBack: () -> Unit) {
    val activeTabNavigator = rememberSaveable(saver = SampleActiveTabNavigator.Saver) {
        SampleActiveTabNavigator()
    }
    val shellNavigation = rememberShellNavigation(tabNavigator, activeTabNavigator, onOuterBack)

    // `activeScreen` is the top of the current tab's stack. Before the first tab registers its
    // Navigator, and for the frame in which a tab switch has not yet re-registered, fall back to
    // the current tab's own root so chrome and the route key never read the outgoing tab's screen.
    val currentTab = tabNavigator.current as SampleTab
    val activeNavigator = activeTabNavigator.navigatorFor(currentTab.key)
    val activeScreen = activeScreenFor(
        currentTabKey = currentTab.key,
        currentTabRoot = currentTab.root,
        registeredTabKey = activeTabNavigator.tabKey,
        registeredScreen = activeNavigator?.lastItem as? SampleScreen,
    )

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

    // Browser history bridge (null on Android/desktop). The shell reports the active route after
    // every navigation, and a deep link / popstate arrives through [SampleShellNavigation].
    val browserNavigation = LocalSampleBrowserNavigation.current
    LaunchedEffect(browserNavigation, shellNavigation) {
        if (browserNavigation != null) {
            shellNavigation.attachBrowser(browserNavigation)
            browserNavigation.onShellReady(shellNavigation)
        }
    }
    DisposableEffect(browserNavigation) {
        onDispose { browserNavigation?.onShellDisposed() }
    }
    // A native deep link (an Android intent or a desktop launch argument) is a one-shot route:
    // apply it through the same external-navigation path as a browser URL, then consume it so a
    // recreated host cannot replay it.
    val deepLinkSource = LocalSampleDeepLinkSource.current
    LaunchedEffect(deepLinkSource, shellNavigation) {
        deepLinkSource?.pending?.collect { route ->
            if (route != null) {
                shellNavigation.navigateTo(route)
                deepLinkSource.consume()
            }
        }
    }
    LaunchedEffect(activeRouteKey, browserNavigation) {
        browserNavigation?.onRouteChanged(
            route = SampleRoute(
                tab = currentTab.destination,
                screen = activeScreen.destination,
                contentId = (activeScreen as? ContentDetailScreen)?.contentId,
            ),
            push = !shellNavigation.consumeReplaceReport(),
        )
    }
    // An external route that switches tabs registers its inner navigator one frame later; apply
    // the pending screen once that registration lands.
    val pendingExternalRoute = shellNavigation.pendingExternalRoute
    LaunchedEffect(pendingExternalRoute, activeNavigator, currentTab.key) {
        if (pendingExternalRoute != null && currentTab.destination == pendingExternalRoute.tab) {
            shellNavigation.applyPendingExternalRoute()
        }
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
        destinationLabel = activeScreen.destination.label.text(),
        isTopLevel = activeScreen.destination.isTopLevel,
    )

    // One stable reference for the platform Back seams, so the pointer handler is not restarted by
    // ordinary recomposition.
    val onBack = remember(shellNavigation) { shellNavigation::back }
    val platformBack = LocalSamplePlatformBack.current

    CompositionLocalProvider(
        LocalSampleChromeController provides chromeController,
        LocalSampleNavEffects provides navEffects,
        LocalSampleTitleBarSink provides titleBarSink,
        LocalSampleWidthClass provides widthClass,
        LocalSampleActiveTabNavigator provides activeTabNavigator,
        LocalSampleBackHandler provides shellNavigation,
    ) {
        // The platform Back input is the same policy handler the top-bar arrow and
        // `SageEvent.NavigateBack` use. `enabled` lets Android stay out of the way while Voyager
        // pops a pushed screen itself.
        SampleSystemBack(
            enabled = activeNavigator?.canPop != true,
            onBack = onBack,
        )

        Box(modifier = platformBack.wrap(Modifier.fillMaxSize(), onBack)) {
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
                onBack = onBack,
            ) { innerPadding ->
                Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                    // One shared-element host above every tab's navigator: a push inside a tab
                    // animates the images that both screens show between the two layouts, and the
                    // overlay has to outlive both screens to draw the element in flight.
                    SharedTransitionLayout(modifier = Modifier.fillMaxSize()) {
                        CompositionLocalProvider(LocalSharedTransitionScope provides this) {
                            SampleCurrentTab()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun rememberShellNavigation(
    tabNavigator: TabNavigator,
    activeTabNavigator: SampleActiveTabNavigator,
    onOuterBack: () -> Unit,
): DefaultSampleShellNavigation {
    // The host may pass a fresh lambda each recomposition; the navigation object outlives that and
    // still needs the latest outer callback when Back reaches the bottom of the shell.
    val outerBack = rememberUpdatedState(onOuterBack)
    return remember(tabNavigator, activeTabNavigator) {
        DefaultSampleShellNavigation(tabNavigator, activeTabNavigator) { outerBack.value() }
    }
}

/**
 * The shell's navigation operations, shared by tab taps, feature events, Back and external routes.
 *
 * All of them mutate the same two navigators, so keeping them together is what stops the bottom
 * bar, the top-bar arrow, platform Back and a deep link from drifting apart.
 */
private class DefaultSampleShellNavigation(
    private val tabNavigator: TabNavigator,
    private val activeTabNavigator: SampleActiveTabNavigator,
    private val onOuterBack: () -> Unit,
) : SampleBackHandler,
    SampleShellNavigation {

    /**
     * The external route waiting for its tab's inner navigator to register. Set by [navigateTo];
     * cleared by [applyPendingExternalRoute]. Shared by browser history and native deep links.
     */
    var pendingExternalRoute: SampleRoute? by mutableStateOf(null)
        private set

    override val isExternalNavigationPending: Boolean
        get() = pendingExternalRoute != null

    /**
     * True when the next route report should replace the current browser entry instead of pushing
     * one. Set by an in-app Back; consumed and reset by the shell's report effect.
     */
    private var replaceNextReport: Boolean by mutableStateOf(false)

    private var browserNavigation: SampleBrowserNavigation? = null

    /** Links the host bridge once it is ready. */
    fun attachBrowser(navigation: SampleBrowserNavigation) {
        browserNavigation = navigation
    }

    /** Consumes the replace flag; the shell calls this once per route report. */
    fun consumeReplaceReport(): Boolean {
        val replace = replaceNextReport
        replaceNextReport = false
        return replace
    }

    override fun navigateTo(route: SampleRoute) {
        pendingExternalRoute = route
        val target = tabFor(route.tab) ?: return
        val current = tabNavigator.current
        if (current.key != target.key) {
            activeTabNavigator.resetToRootOnNextEntry(current.key)
            tabNavigator.current = target
            return
        }
        // Same tab: the navigator is already registered, so apply now instead of waiting for the
        // shell's pending-route effect.
        applyPendingExternalRoute()
    }

    /** Applies the pending external route once its tab's navigator is available. */
    fun applyPendingExternalRoute() {
        val route = pendingExternalRoute ?: return
        val target = tabFor(route.tab) ?: return
        val navigator = activeTabNavigator.navigatorFor(target.key) ?: return
        navigator.popAll()
        if (!route.isRoot) {
            navigator.push(screenFor(route.screen, route.contentId))
        }
        pendingExternalRoute = null
    }

    override fun back() {
        // With an app entry behind the current URL, the browser owns Back: history.back() fires
        // popstate, which drives this shell through navigateTo.
        if (pendingExternalRoute == null && browserNavigation?.onBackRequested() == true) return

        val currentTab = tabNavigator.current as SampleTab
        val navigator = activeTabNavigator.navigatorFor(currentTab.key)
        val action = backActionFor(
            tab = currentTab.destination,
            canPop = navigator?.canPop == true,
        )
        when (action) {
            SampleBackAction.Pop -> {
                replaceNextReport = true
                navigator?.pop()
            }

            is SampleBackAction.ShowTab -> {
                replaceNextReport = true
                tabFor(action.destination)?.let(::selectTab)
            }

            SampleBackAction.Outer -> onOuterBack()
        }
    }

    /**
     * Switches tabs without touching the outgoing tab's stack, so its exit animation keeps
     * showing the screen the user was on; the outgoing tab is marked to reopen at its root the
     * next time it is composed. Re-tapping the current tab instead pops it to its root in place,
     * because that change is the whole point of the tap.
     */
    fun selectTab(target: SampleTab) {
        val current = tabNavigator.current as SampleTab
        if (current.key == target.key) {
            activeTabNavigator.navigator?.popAll()
            return
        }
        activeTabNavigator.resetToRootOnNextEntry(current.key)
        tabNavigator.current = target
    }

    /**
     * Pushes onto the active tab; a pushed screen belongs to the tab that was visible.
     *
     * [contentId] is the decoded Content detail ID, `null` for its not-found state. [artworkSize]
     * is the size the tapped row rendered its own artwork at, when the route carried it.
     */
    fun push(
        destination: SampleDestination,
        contentId: ContentId? = null,
        artworkSize: ContentArtworkSize? = null,
    ) {
        val currentTab = tabNavigator.current as SampleTab
        val navigator = activeTabNavigator.navigatorFor(currentTab.key) ?: return
        val target = screenFor(destination, contentId, artworkSize)
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
    shellNavigation: DefaultSampleShellNavigation,
    snackbarHostState: SnackbarHostState,
    scope: CoroutineScope,
) {
    if (event !is SageEvent.ShowSnackbar) {
        when (val action = navActionFor(event)) {
            is SampleNavAction.ShowTopLevel -> tabFor(action.destination)?.let(shellNavigation::selectTab)

            is SampleNavAction.Push ->
                shellNavigation.push(
                    destination = action.destination,
                    contentId = action.contentId,
                    artworkSize = action.artworkSize,
                )

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
