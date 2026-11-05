package net.sigmabeta.sage.sample.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import dev.zacsweers.metrox.viewmodel.metroViewModel
import net.sigmabeta.sage.sample.feature.browse.BrowseViewModel
import net.sigmabeta.sage.sample.feature.search.SearchViewModel
import net.sigmabeta.sage.sample.feature.settings.SettingsViewModel
import net.sigmabeta.sage.sample.feature.welcome.WelcomeViewModel
import net.sigmabeta.sage.sample.ui.ListItemContent
import net.sigmabeta.sage.sample.viewmodel.SampleListViewModel
import net.sigmabeta.sage.ui.list.ListScreen

/**
 * A Voyager [Screen] that knows which app destination it renders. The scaffold reads
 * [destination] to pick the top-bar affordance and the selected navigation item.
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
 */
@Composable
private fun SampleListScreen(viewModel: SampleListViewModel<*>) {
    val state by viewModel.uiStateActual.collectAsStateWithLifecycle()

    ListScreen(
        state = state,
        actionSink = viewModel,
        showDebug = false,
        sideMargin = SCREEN_SIDE_MARGIN_DP.dp,
        modifier = Modifier.fillMaxSize(),
        itemContent = { model, sink, _, itemModifier, padding ->
            ListItemContent(model, sink, itemModifier, padding)
        },
    )
}

private const val SCREEN_SIDE_MARGIN_DP = 16
