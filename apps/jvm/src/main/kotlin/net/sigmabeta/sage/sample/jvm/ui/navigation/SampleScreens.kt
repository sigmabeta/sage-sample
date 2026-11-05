package net.sigmabeta.sage.sample.jvm.ui.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import dev.zacsweers.metrox.viewmodel.metroViewModel
import net.sigmabeta.sage.appcomm.ActionSink
import net.sigmabeta.sage.appcomm.SageAction
import net.sigmabeta.sage.components.ListModel
import net.sigmabeta.sage.sample.feature.browse.BrowseViewModel
import net.sigmabeta.sage.sample.feature.search.SearchViewModel
import net.sigmabeta.sage.sample.feature.settings.SettingsViewModel
import net.sigmabeta.sage.sample.feature.welcome.WelcomeViewModel
import net.sigmabeta.sage.sample.jvm.ui.ListItemContent
import net.sigmabeta.sage.sample.viewmodel.SampleListViewModel
import net.sigmabeta.sage.ui.list.GridScreen
import net.sigmabeta.sage.ui.list.ListScreen

/**
 * A Voyager [Screen] that knows which app destination it renders. The scaffold reads
 * [destination] to select the navigation item and to supply the top bar's pre-report fallback
 * (destination label and top-level flag); the top bar itself is driven by the screen's SAGE title
 * state.
 */
sealed interface SampleScreen : Screen {
    val destination: SampleDestination
}

data object HomeScreen : SampleScreen {
    override val destination = SampleDestination.HOME

    @Composable
    override fun Content() {
        SampleListScreen(metroViewModel<WelcomeViewModel>())
    }
}

data object BrowseScreen : SampleScreen {
    override val destination = SampleDestination.BROWSE

    @Composable
    override fun Content() {
        SampleListScreen(metroViewModel<BrowseViewModel>())
    }
}

data object SearchScreen : SampleScreen {
    override val destination = SampleDestination.SEARCH

    @Composable
    override fun Content() {
        SampleListScreen(metroViewModel<SearchViewModel>())
    }
}

data object SettingsScreen : SampleScreen {
    override val destination = SampleDestination.SETTINGS

    @Composable
    override fun Content() {
        SampleListScreen(metroViewModel<SettingsViewModel>())
    }
}

/**
 * Renders a [SampleListViewModel] with the app's [ListItemContent].
 *
 * Every destination goes through this, so the only per-screen decisions are which view model it
 * resolves and, in the view model, what state it produces.
 *
 * The shared path owns three cross-cutting behaviours:
 * - **Chrome:** the screen's [net.sigmabeta.sage.list.ListStateActual.title] is reported through
 *   [LocalSampleTitleBarSink], so a dynamic title updates the shared top bar.
 * - **Layout:** [screenLayoutFor] picks SAGE's `ListScreen` or `GridScreen` (staggered when the
 *   state asks for it) from the state's [net.sigmabeta.sage.list.ColumnType] and the window's
 *   [net.sigmabeta.sage.list.WidthClass]. A state that fits at most one column keeps the existing
 *   list presentation.
 * - **Lifecycle:** exactly one foreground pair is sent here — [SageAction.Resume] on entering
 *   composition and [SageAction.Pause] on leaving. SAGE's own title-change `Resume` is coalesced
 *   in [SampleListViewModel.sendAction], so feature side effects run once per foreground entry.
 *
 * `collectAsState` is enough on desktop: the view model lives in the screen's own store (see
 * `WithPerScreenViewModelStore`), so there is no separate Android lifecycle to pause collection.
 */
@Composable
private fun SampleListScreen(viewModel: SampleListViewModel<*>) {
    val state by viewModel.uiStateActual.collectAsState()
    val navEffects = LocalSampleNavEffects.current
    val titleBarSink = LocalSampleTitleBarSink.current
    val widthClass = LocalSampleWidthClass.current

    // One-shot effects are forwarded, never stored: the flow has replay = 0, so recomposition or
    // popping and re-pushing this screen cannot replay an old navigation.
    LaunchedEffect(viewModel) {
        viewModel.uiEvents.collect { event -> navEffects.onEvent(event) }
    }

    LaunchedEffect(state.title) {
        titleBarSink.onTitleBar(state.title)
    }

    DisposableEffect(viewModel) {
        viewModel.sendAction(SageAction.Resume)
        onDispose { viewModel.sendAction(SageAction.Pause) }
    }

    val itemContent: @Composable (
        model: ListModel,
        sink: ActionSink,
        debug: Boolean,
        modifier: Modifier,
        padding: PaddingValues,
    ) -> Unit = { model, sink, _, itemModifier, padding ->
        ListItemContent(model, sink, itemModifier, padding)
    }

    val sideMargin = SCREEN_SIDE_MARGIN_DP.dp
    val screenModifier = Modifier.fillMaxSize()

    when (val layout = screenLayoutFor(state.columnType, widthClass)) {
        SampleScreenLayout.List -> ListScreen(
            state = state,
            actionSink = viewModel,
            showDebug = false,
            sideMargin = sideMargin,
            modifier = screenModifier,
            itemContent = itemContent,
        )

        is SampleScreenLayout.Grid -> GridScreen(
            state = state,
            actionSink = viewModel,
            showDebug = false,
            numberOfColumns = layout.columns,
            staggered = layout.staggered,
            allowHorizScroller = layout.allowHorizScroller,
            sideMargin = sideMargin,
            modifier = screenModifier,
            itemContent = itemContent,
        )
    }
}

private const val SCREEN_SIDE_MARGIN_DP = 16
