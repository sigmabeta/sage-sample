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
import net.sigmabeta.sage.appcomm.SageEvent
import net.sigmabeta.sage.components.ListModel
import net.sigmabeta.sage.list.ListStateActual
import net.sigmabeta.sage.sample.feature.browse.BrowseViewModel
import net.sigmabeta.sage.sample.feature.contentdetail.ContentDetailViewModel
import net.sigmabeta.sage.sample.feature.home.HomeViewModel
import net.sigmabeta.sage.sample.feature.listitems.GridOfImagesViewModel
import net.sigmabeta.sage.sample.feature.listitems.ListItemsViewModel
import net.sigmabeta.sage.sample.feature.listitems.ListItemsWithImagesViewModel
import net.sigmabeta.sage.sample.feature.search.SearchStringId
import net.sigmabeta.sage.sample.feature.search.SearchViewModel
import net.sigmabeta.sage.sample.feature.settings.SettingsViewModel
import net.sigmabeta.sage.sample.ui.components.api.Content
import net.sigmabeta.sage.sample.ui.components.api.SearchContent
import net.sigmabeta.sage.sample.ui.components.api.text
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
        SampleListScreen(metroViewModel<HomeViewModel>())
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
        val viewModel: SearchViewModel = metroViewModel()
        val query by viewModel.query.collectAsState()
        val navEffects = LocalSampleNavEffects.current
        val chromeController = LocalSampleChromeController.current

        // Chipbox's search chrome: the in-screen field replaces the app top bar. The next
        // destination resets the controller, so there is nothing to restore on disposal.
        LaunchedEffect(Unit) {
            chromeController.set(SampleScreenChrome(showTopBar = false))
        }

        SampleListScreen(viewModel) { state ->
            SearchContent(
                listItems = state.listItems,
                query = query,
                hint = SearchStringId.SEARCH_HINT.text(),
                clearContentDescription = SearchStringId.SEARCH_CLEAR_DESCRIPTION.text(),
                backContentDescription = SearchStringId.SEARCH_BACK_DESCRIPTION.text(),
                onQueryChange = { viewModel.sendAction(SageAction.SearchQueryEntered(it)) },
                onClear = { viewModel.sendAction(SageAction.SearchClearClicked) },
                onBack = {
                    navEffects.onEvent(SageEvent.NavigateBack(source = "SearchScreen"))
                },
                actionSink = viewModel,
            )
        }
    }
}

data object ListItemsScreen : SampleScreen {
    override val destination = SampleDestination.LIST_ITEMS

    @Composable
    override fun Content() {
        SampleListScreen(metroViewModel<ListItemsViewModel>())
    }
}

data object ListItemsWithImagesScreen : SampleScreen {
    override val destination = SampleDestination.LIST_ITEMS_WITH_IMAGES

    @Composable
    override fun Content() {
        SampleListScreen(metroViewModel<ListItemsWithImagesViewModel>())
    }
}

data object GridOfImagesScreen : SampleScreen {
    override val destination = SampleDestination.GRID_OF_IMAGES

    @Composable
    override fun Content() {
        SampleListScreen(metroViewModel<GridOfImagesViewModel>())
    }
}

data object ContentDetailScreen : SampleScreen {
    override val destination = SampleDestination.CONTENT_DETAIL

    @Composable
    override fun Content() {
        SampleListScreen(metroViewModel<ContentDetailViewModel>())
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
 * Renders a [SampleListViewModel] with the shared
 * [net.sigmabeta.sage.sample.ui.components.api.Content] renderer.
 *
 * Every destination goes through this, so the only per-screen decisions are which view model it
 * resolves, what state it produces, and whether it replaces the shared list presentation
 * (Search overlays its field on the grid).
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
private fun SampleListScreen(
    viewModel: SampleListViewModel<*>,
    content: (@Composable (ListStateActual) -> Unit)? = null,
) {
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

    if (content != null) {
        content(state)
        return
    }

    val itemContent: @Composable (
        model: ListModel,
        sink: ActionSink,
        debug: Boolean,
        modifier: Modifier,
        padding: PaddingValues,
    ) -> Unit = { model, sink, debug, itemModifier, padding ->
        model.Content(sink = sink, debug = debug, mod = itemModifier, pad = padding)
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
