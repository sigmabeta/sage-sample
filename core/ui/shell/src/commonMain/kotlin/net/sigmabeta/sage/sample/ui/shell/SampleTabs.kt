package net.sigmabeta.sage.sample.ui.shell

import net.sigmabeta.sage.sample.models.ContentArtworkSize
import net.sigmabeta.sage.sample.models.ContentId
import net.sigmabeta.sage.sample.nav.SampleDestination
import net.sigmabeta.sage.sample.nav.activeScreenFor
import net.sigmabeta.sage.sample.nav.backActionFor
import net.sigmabeta.sage.sample.ui.components.api.BASE_FADE_IN_MILLIS
import net.sigmabeta.sage.sample.ui.components.api.BASE_FADE_OUT_MILLIS
import net.sigmabeta.sage.sample.ui.components.api.LocalAnimationScale
import net.sigmabeta.sage.sample.ui.components.api.LocalScreenTransitionScope
import net.sigmabeta.sage.sample.ui.components.api.scaledMillis
import net.sigmabeta.sage.sample.ui.components.api.text

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
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
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import cafe.adriel.voyager.core.stack.StackEvent
import cafe.adriel.voyager.transitions.ScreenTransition
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
            title = destination.label.text(),
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
 * The selected top-level tab's content, animated on switch: the outgoing tab slides down and
 * fades out while the incoming one slides in from the left (no fade).
 *
 * Voyager's `CurrentTab` swaps tabs with no transition. This keeps its saveable-state contract —
 * each tab's `rememberSaveable` state and retained inner navigator are keyed by the tab — and uses
 * the shape real SAGE apps use for a tab switch, deliberately distinct from the horizontal slide a
 * push uses inside a tab. The duration follows the animation-scale setting.
 */
@Composable
fun SampleCurrentTab() {
    val tabNavigator = LocalTabNavigator.current
    val animationScale = LocalAnimationScale.current
    val durationMillis = remember(animationScale) {
        scaledMillis(TAB_SWITCH_BASE_MILLIS, animationScale)
    }
    val maxSlidePx = with(LocalDensity.current) { TAB_SWITCH_MAX_SLIDE.roundToPx() }

    AnimatedContent(
        targetState = tabNavigator.current,
        contentKey = { it.key },
        transitionSpec = {
            val enter = slideInHorizontally(
                animationSpec = tween(durationMillis = durationMillis),
                initialOffsetX = { fullWidth -> -fullWidth },
            )
            val exit = slideOutVertically(
                animationSpec = tween(durationMillis = durationMillis),
                targetOffsetY = { fullHeight -> minOf(fullHeight, maxSlidePx) },
            ) + fadeOut(
                animationSpec = tween(durationMillis = durationMillis),
            )
            enter togetherWith exit
        },
        label = "TabSwitch",
    ) { tab ->
        tabNavigator.saveableState("currentTab", tab) {
            tab.Content()
        }
    }
}

// Top-level tab switches: the outgoing tab slides down and fades out while the incoming tab slides
// in from the left. Deliberately distinct from the horizontal slide used for push/pop within a
// tab's hierarchy.
private const val TAB_SWITCH_BASE_MILLIS = 210

// Cap how far the outgoing tab travels downward, so on tall screens it fades out rather than
// sliding the full height.
private val TAB_SWITCH_MAX_SLIDE = 400.dp

/**
 * Hosts one tab's inner back stack. Registering [navigator] together with its tab lets the shell
 * pop the active tab from outside this tab's composition. The registration can lag the selected
 * tab by one frame while Voyager swaps tabs, so the shell checks the registered key before trusting
 * it (see [activeScreenFor]).
 *
 * `onBackPressed` routes platform Back through the shell's shared handler ([LocalSampleBackHandler])
 * and returns `false` to decline Voyager's own pop. Each screen is wrapped by the active
 * [LocalSampleScreenContainer]; Android passes screens straight through, desktop adds the
 * per-screen view-model store that Voyager supplies only on Android.
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

        val screens = LocalSampleScreenContainer.current

        // One scale for the whole move: the fade specs stretch linearly, and the spring's natural
        // period is stretched with them (see screenSlideSpec), so slowing the transition down keeps
        // its shape instead of ending in a fade tail behind a slide that already settled.
        val animationScale = LocalAnimationScale.current
        val fadeInSpec = remember(animationScale) {
            tween<Float>(scaledMillis(BASE_FADE_IN_MILLIS, animationScale))
        }
        val fadeOutSpec = remember(animationScale) {
            tween<Float>(scaledMillis(BASE_FADE_OUT_MILLIS, animationScale))
        }
        val slideSpec = remember(animationScale) { screenSlideSpec(animationScale) }

        // Voyager's SlideTransition moves the screens but leaves them fully opaque, so a push reads
        // as one page sliding over another. Adding a fade to the same offsets makes the content
        // that has no shared element dissolve while the artwork that does travels: the overlap
        // stops looking like two stacked pages.
        ScreenTransition(
            navigator = navigator,
            transition = {
                val (initialOffset, targetOffset) = when (navigator.lastEvent) {
                    StackEvent.Pop -> ({ size: Int -> -size }) to ({ size: Int -> size })
                    else -> ({ size: Int -> size }) to ({ size: Int -> -size })
                }

                (
                    slideInHorizontally(slideSpec, initialOffset) +
                        fadeIn(fadeInSpec)
                    ) togetherWith (
                    slideOutHorizontally(slideSpec, targetOffset) +
                        fadeOut(fadeOutSpec)
                    )
            },
        ) { screen ->
            // `this` is the screen's slot in the navigator's AnimatedContent: the scope the shared
            // elements below use to know which transition they belong to. Both the outgoing and
            // the incoming screen get their own, which is what lets an image morph between them.
            CompositionLocalProvider(LocalScreenTransitionScope provides this) {
                screens.Content(screen) {
                    screen.Content()
                }
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
 * Only the parameterized Content detail destination consumes [contentId] and [artworkSize]; for
 * every other destination they are ignored.
 */
internal fun screenFor(
    destination: SampleDestination,
    contentId: ContentId? = null,
    artworkSize: ContentArtworkSize? = null,
): SampleScreen = when (destination) {
    SampleDestination.HOME -> HomeScreen
    SampleDestination.BROWSE -> BrowseScreen
    SampleDestination.LIST_ITEMS -> ListItemsScreen
    SampleDestination.LIST_ITEMS_WITH_IMAGES -> ListItemsWithImagesScreen
    SampleDestination.GRID_OF_IMAGES -> GridOfImagesScreen
    SampleDestination.CONTENT_DETAIL -> ContentDetailScreen(contentId, artworkSize)
    SampleDestination.SEARCH -> SearchScreen
    SampleDestination.SETTINGS -> SettingsScreen
}

/**
 * The slide's spring, at Voyager's default stiffness when [scale] is one.
 *
 * A spring has no duration, so the scale is applied to what shapes its timing: its period is
 * proportional to `1 / sqrt(stiffness)`, hence the squared divisor.
 */
private fun screenSlideSpec(scale: Float): FiniteAnimationSpec<IntOffset> = spring(
    stiffness = Spring.StiffnessMediumLow / (scale * scale),
    visibilityThreshold = IntOffset.VisibilityThreshold,
)
