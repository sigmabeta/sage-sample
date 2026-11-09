package net.sigmabeta.sage.sample.ui.components.api

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import net.sigmabeta.sage.appcomm.ActionSink
import net.sigmabeta.sage.components.ListModel

/**
 * Search's app-owned rendering: the screen draws its own field and adaptive grid.
 *
 * A single adaptive grid draws every [ListModel] through [Content]; the [SearchField] is overlaid
 * on top with a status-bar scrim, rather than sitting in the layout above the list. Row items and
 * section headers span the grid, while grid-image cells keep one lane each.
 */
@Composable
@Suppress("LongParameterList", "LongMethod")
fun SearchContent(
    listItems: ImmutableList<ListModel>,
    query: String,
    hint: String,
    clearContentDescription: String,
    backContentDescription: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    onBack: () -> Unit,
    actionSink: ActionSink,
    showDebug: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        val belowSearchBar: Dp = 96.dp
        val topInsets = WindowInsets.statusBars

        val contentPadding = WindowInsets(top = belowSearchBar)
            .add(topInsets)
            .add(WindowInsets(left = 16.dp, right = 16.dp))
            .add(WindowInsets.navigationBars)
            .asPaddingValues()

        val gridState = rememberLazyGridState()
        LaunchedEffect(query) { gridState.animateScrollToItem(0) }

        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Adaptive(160.dp),
            contentPadding = contentPadding,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(
                items = listItems,
                key = { it.dataId },
                contentType = { it.layoutId() },
                span = {
                    if (it.columns < 1) {
                        GridItemSpan(maxLineSpan)
                    } else {
                        GridItemSpan(it.columns)
                    }
                },
            ) {
                it.Content(
                    sink = actionSink,
                    debug = showDebug,
                    mod = Modifier.animateItem(),
                    pad = PaddingValues(),
                )
            }
        }

        val topPaddingForSearchBar = topInsets.asPaddingValues().calculateTopPadding()

        SearchField(
            text = query,
            hint = hint,
            clearContentDescription = clearContentDescription,
            backContentDescription = backContentDescription,
            onValueChange = onQueryChange,
            onClear = onClear,
            onBack = onBack,
            modifier = Modifier.padding(top = topPaddingForSearchBar + 16.dp),
        )

        // Scrim for the Android status bar, so the search bar reads cleanly under it.
        Spacer(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(topPaddingForSearchBar.times(2))
                .background(
                    brush = Brush.verticalGradient(
                        colors = scrimColors(isSystemInDarkTheme()),
                    ),
                ),
        )
    }
}

@Suppress("MagicNumber")
private fun scrimColors(isDarkTheme: Boolean) = if (isDarkTheme) {
    listOf(
        Color(0, 0, 0, 160),
        Color(0, 0, 0, 64),
        Color(0, 0, 0, 0),
    )
} else {
    listOf(
        Color(255, 255, 255, 255),
        Color(255, 255, 255, 192),
        Color(255, 255, 255, 0),
    )
}
