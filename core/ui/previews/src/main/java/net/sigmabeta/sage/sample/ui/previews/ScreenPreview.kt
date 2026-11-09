package net.sigmabeta.sage.sample.ui.previews

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import net.sigmabeta.sage.appcomm.ActionSink
import net.sigmabeta.sage.components.ListModel
import net.sigmabeta.sage.components.TitleBarModel
import net.sigmabeta.sage.list.ColumnType
import net.sigmabeta.sage.list.ListState
import net.sigmabeta.sage.list.ListStateActual
import net.sigmabeta.sage.list.WidthClass
import net.sigmabeta.sage.sample.strings.real.SampleStringProvider
import net.sigmabeta.sage.sample.ui.components.api.Content
import net.sigmabeta.sage.sample.ui.components.api.LocalSampleStringProvider
import net.sigmabeta.sage.sample.ui.theme.api.SampleTheme
import net.sigmabeta.sage.ui.StringProvider
import net.sigmabeta.sage.ui.list.GridScreen
import net.sigmabeta.sage.ui.list.ListScreen

/**
 * Renders a screen's [ListState] through the real SAGE list pipeline, exactly as the app's
 * navigation entry would, but with no ViewModels or DI so it is Paparazzi- and `@Preview`-safe.
 *
 * Text comes from the shared [SampleStringProvider] the apps use, so a golden pins real copy. The
 * sample's full chrome (nav bars, scrolling top bar) is app-owned and ViewModel-bound, so the
 * preview uses a lightweight VM-free [PreviewChrome]: a Material3 top bar driven by the rendered
 * title. The nav suite is intentionally omitted — these goldens regression-test screen content,
 * not the shell.
 */
@Composable
fun ListScreenPreview(
    screenState: ListState,
    darkTheme: Boolean,
    syntheticWidthClass: WidthClass,
) {
    val actionSink = ActionSink { }
    val state = screenState.toActual(SampleStringProvider)

    PreviewTheme(darkTheme = darkTheme) {
        PreviewChrome(titleBarModel = state.title) { innerPadding ->
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
                    .fillMaxSize(),
            ) {
                ListContent(state, syntheticWidthClass, actionSink)
            }
        }
    }
}

/**
 * For screens that own their presentation and don't render through the generic list pipeline
 * (Search draws its own field over an adaptive grid). Provides the same theme,
 * `LocalInspectionMode` and string provider as [ListScreenPreview], with no top bar, and hands the
 * [content] a [StringProvider] so it can turn a screen state into list items itself.
 */
@Composable
fun ScreenPreview(
    darkTheme: Boolean,
    content: @Composable (StringProvider) -> Unit,
) {
    PreviewTheme(darkTheme = darkTheme) {
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .fillMaxSize(),
        ) {
            content(SampleStringProvider)
        }
    }
}

@Composable
private fun PreviewTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit,
) {
    SampleTheme(darkTheme = darkTheme, fontFamily = FontFamily.Default) {
        CompositionLocalProvider(
            // Coil can't fetch under Paparazzi; inspection mode routes image surfaces to the shared
            // generated-bitmap stand-in instead of a loading/error placeholder.
            LocalInspectionMode provides true,
            LocalSampleStringProvider provides SampleStringProvider,
            content = content,
        )
    }
}

/**
 * Picks [GridScreen]/[ListScreen] the same way the app's `SampleListScreen` does: at most one
 * column keeps the single-column list presentation, anything wider uses the grid.
 */
@Composable
private fun ListContent(
    state: ListStateActual,
    displayWidthClass: WidthClass,
    actionSink: ActionSink,
) {
    val columnType = state.columnType
    val numColumns = columnType.numberOfColumns(displayWidthClass)

    val itemContent: @Composable (
        model: ListModel,
        sink: ActionSink,
        debug: Boolean,
        modifier: Modifier,
        padding: PaddingValues,
    ) -> Unit = { model, sink, debug, mod, pad -> model.Content(sink, debug, mod, pad) }

    if (numColumns > 1) {
        val staggered = columnType is ColumnType.Staggered
        GridScreen(
            state = state,
            actionSink = actionSink,
            showDebug = false,
            numberOfColumns = numColumns,
            staggered = staggered,
            allowHorizScroller = (columnType as? ColumnType.Staggered)?.allowHorizScroller ?: false,
            sideMargin = SCREEN_SIDE_MARGIN,
            modifier = Modifier,
            itemContent = itemContent,
        )
    } else {
        ListScreen(
            state = state,
            actionSink = actionSink,
            showDebug = false,
            sideMargin = SCREEN_SIDE_MARGIN,
            modifier = Modifier,
            itemContent = itemContent,
        )
    }
}

/**
 * VM-free stand-in for the app's `SampleNavScaffold`: a Material3 top bar showing the screen's
 * title. The bottom bar / rail and the player-like chrome are intentionally omitted because they
 * are shell concerns, not what these screenshots regression-test.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PreviewChrome(
    titleBarModel: TitleBarModel,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(text = titleBarModel.title.orEmpty()) })
        },
        content = { innerPadding -> content(innerPadding) },
    )
}

/**
 * Mirrors the app's private width-class derivation; used as the `@Preview` default so the IDE
 * gutter renders a sensible layout.
 */
@Composable
fun previewWidthClass(): WidthClass {
    val widthDp = LocalConfiguration.current.screenWidthDp
    return when {
        widthDp < WIDTH_BREAKPOINT_MEDIUM -> WidthClass.COMPACT
        widthDp < WIDTH_BREAKPOINT_EXPANDED -> WidthClass.MEDIUM
        else -> WidthClass.EXPANDED
    }
}

private val SCREEN_SIDE_MARGIN = 16.dp

private const val WIDTH_BREAKPOINT_MEDIUM = 600
private const val WIDTH_BREAKPOINT_EXPANDED = 840
