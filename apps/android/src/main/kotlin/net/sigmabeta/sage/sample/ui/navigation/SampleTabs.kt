package net.sigmabeta.sage.sample.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import cafe.adriel.voyager.transitions.SlideTransition
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
            title = stringResource(destination.labelRes),
            icon = rememberVectorPainter(destination.icon.vector()),
        )

    @Composable
    override fun Content() {
        SampleTabContent(root)
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
 * Hosts one tab's inner back stack. Registering [navigator] lets the shell pop the active tab from
 * outside this tab's composition; Voyager unmounts the outgoing tab before mounting the incoming
 * one, so the registration always names the visible tab.
 *
 * `onBackPressed` routes Android system Back through the shell's shared handler ([LocalSampleBackHandler])
 * and returns `false` to decline Voyager's own pop.
 */
@Composable
private fun SampleTabContent(root: SampleScreen) {
    val activeTabNavigator = LocalSampleActiveTabNavigator.current
    val backHandler = LocalSampleBackHandler.current

    Navigator(
        screen = root,
        onBackPressed = {
            backHandler.back()
            false
        },
    ) { navigator ->
        DisposableEffect(navigator) {
            activeTabNavigator.navigator = navigator
            onDispose {
                if (activeTabNavigator.navigator === navigator) {
                    activeTabNavigator.navigator = null
                }
            }
        }

        SlideTransition(navigator) { screen ->
            screen.Content()
        }
    }
}

/**
 * Holds the active tab's inner [Navigator] so the shell can pop a tab's pushed screens from outside
 * that tab's composition (the top-bar arrow, system Back, and feature navigation events).
 */
@Stable
class SampleActiveTabNavigator {
    var navigator: Navigator? by mutableStateOf(null)
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
