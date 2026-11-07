package net.sigmabeta.sage.sample.jvm.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import cafe.adriel.voyager.transitions.SlideTransition
import net.sigmabeta.sage.sample.jvm.ui.WithPerScreenViewModelStore
import net.sigmabeta.sage.sample.feature.contentdetail.ContentId
import net.sigmabeta.sage.ui.vector

/**
 * A top-level destination as a Voyager [Tab].
 *
 * Each tab owns an inner [Navigator] whose root is [root] — a long-lived `data object`, so the
 * screen key is stable and Voyager keeps that screen's view model and saveable state while its tab
 * is off screen. [key] is likewise stable, which is what `TabNavigator` uses to save and restore the
 * tab's state.
 */
sealed class SampleTab(
    val destination: SampleDestination,
    val root: SampleScreen,
    private val index: UShort,
) : Tab {
    override val key: ScreenKey = "${destination.destName}Tab"

    override val options: TabOptions
        @Composable get() = TabOptions(
            index = index,
            title = destination.label,
            icon = rememberVectorPainter(destination.icon.vector()),
        )

    @Composable
    override fun Content() {
        SampleTabContent(this)
    }
}

data object HomeTab : SampleTab(SampleDestination.HOME, HomeScreen, 0u)

data object BrowseTab : SampleTab(SampleDestination.BROWSE, BrowseScreen, 1u)

data object SearchTab : SampleTab(SampleDestination.SEARCH, SearchScreen, 2u)

/** The top-level tabs in bottom-bar / rail display order. */
val sampleTabs: List<SampleTab> = listOf(HomeTab, BrowseTab, SearchTab)

/** The tab for a top-level destination, or `null` when the destination is pushed onto a tab. */
fun tabFor(destination: SampleDestination): SampleTab? =
    sampleTabs.firstOrNull { it.destination == destination }

/**
 * Hosts one tab's inner back stack. Registering [navigator] together with its tab lets the shell
 * pop the active tab from outside this tab's composition. The registration can lag the selected
 * tab by one frame while Voyager swaps tabs, so the shell checks the registered key before trusting
 * it (see [activeScreenFor]).
 *
 * `onBackPressed` routes platform Back through the shell's shared handler
 * ([LocalSampleBackHandler]) and returns `false` to decline Voyager's own pop. Voyager's desktop
 * Back dispatcher is a no-op; the tab content is still wrapped in [WithPerScreenViewModelStore]
 * because Voyager only supplies per-screen stores on Android.
 */
@Composable
private fun SampleTabContent(tab: SampleTab) {
    val activeTabNavigator = LocalSampleActiveTabNavigator.current
    val backHandler = LocalSampleBackHandler.current

    Navigator(
        screen = tab.root,
        onBackPressed = {
            backHandler.back()
            false
        },
    ) { navigator ->
        DisposableEffect(tab.key, navigator) {
            activeTabNavigator.register(tab.key, navigator)
            onDispose { activeTabNavigator.unregister(navigator) }
        }

        SlideTransition(navigator) { screen ->
            WithPerScreenViewModelStore(screen) {
                screen.Content()
            }
        }
    }
}

/**
 * Holds the active tab's inner [Navigator] so the shell can pop a tab's pushed screens from outside
 * that tab's composition (the top-bar arrow, platform Back, and feature navigation events).
 *
 * [tabKey] names the tab that registered [navigator]. The registration is a composition side effect,
 * so it can still point at the outgoing tab for one frame after a switch; anything that acts on the
 * current tab must go through [navigatorFor] rather than reading [navigator] directly.
 */
@Stable
class SampleActiveTabNavigator {
    var tabKey: String? by mutableStateOf(null)
        private set

    var navigator: Navigator? by mutableStateOf(null)
        private set

    /** Registers [navigator] as [tabKey]'s stack, replacing any previous registration. */
    fun register(tabKey: String, navigator: Navigator) {
        this.tabKey = tabKey
        this.navigator = navigator
    }

    /** Clears the registration only if [navigator] is still the registered one. */
    fun unregister(navigator: Navigator) {
        if (this.navigator === navigator) {
            this.navigator = null
            this.tabKey = null
        }
    }

    /** The navigator registered for [tabKey], or null while another (outgoing) tab owns it. */
    fun navigatorFor(tabKey: String): Navigator? = navigator.takeIf { this.tabKey == tabKey }
}

/** Provided by `SampleNavHost` around the tab navigator; there is no sensible default. */
val LocalSampleActiveTabNavigator = staticCompositionLocalOf<SampleActiveTabNavigator> {
    error("No SampleActiveTabNavigator provided — render tabs inside SampleNavHost.")
}

/**
 * One Back request. The shell provides the implementation so every affordance — top-bar arrow,
 * platform Back, and `SageEvent.NavigateBack` — runs the same [backActionFor] policy.
 */
fun interface SampleBackHandler {
    fun back()
}

/** Provided by `SampleNavHost` around every screen; there is no sensible default. */
val LocalSampleBackHandler = staticCompositionLocalOf<SampleBackHandler> {
    error("No SampleBackHandler provided — render screens inside SampleNavHost.")
}

/**
 * The [SampleScreen] for a destination the app can reach (top-level roots and pushed screens).
 *
 * Only the parameterized Content detail destination consumes [contentId]; for every other
 * destination it is ignored.
 */
internal fun screenFor(
    destination: SampleDestination,
    contentId: ContentId? = null,
): SampleScreen = when (destination) {
    SampleDestination.HOME -> HomeScreen
    SampleDestination.BROWSE -> BrowseScreen
    SampleDestination.LIST_ITEMS -> ListItemsScreen
    SampleDestination.LIST_ITEMS_WITH_IMAGES -> ListItemsWithImagesScreen
    SampleDestination.GRID_OF_IMAGES -> GridOfImagesScreen
    SampleDestination.CONTENT_DETAIL -> ContentDetailScreen(contentId)
    SampleDestination.SEARCH -> SearchScreen
    SampleDestination.SETTINGS -> SettingsScreen
}
