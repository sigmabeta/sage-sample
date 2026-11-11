package net.sigmabeta.sage.sample.ui.components.api

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import net.sigmabeta.sage.appcomm.ActionSink
import net.sigmabeta.sage.components.HorizontalScrollerListModel
import net.sigmabeta.sage.components.ListModel
import net.sigmabeta.sage.components.LoadingItemListModel
import net.sigmabeta.sage.components.SectionListModel

@Composable
@Suppress("MagicNumber")
fun SectionListItem(
    model: SectionListModel,
    actionSink: ActionSink,
    showDebug: Boolean,
    modifier: Modifier,
    padding: PaddingValues,
) {
    val maxWidth = model.maxContentWidthDp
    if (maxWidth != null) {
        // Cap the content to maxWidth and centre it horizontally in the full-width slot. Inset the
        // slot by the screen side margin so a backgroundContainer panel doesn't bleed to the screen
        // edges; the inner items then sit flush to the panel (zeroed `padding`) so they keep the
        // same on-screen position instead of being double-inset.
        Box(
            modifier = modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(padding),
            contentAlignment = Alignment.TopCenter,
        ) {
            SectionContentColumn(
                model = model,
                actionSink = actionSink,
                showDebug = showDebug,
                padding = PaddingValues(),
                modifier = Modifier.widthIn(max = maxWidth.dp),
            )
        }
    } else {
        SectionContentColumn(
            model = model,
            actionSink = actionSink,
            showDebug = showDebug,
            padding = padding,
            modifier = modifier,
        )
    }
}

@Composable
private fun SectionContentColumn(
    model: SectionListModel,
    actionSink: ActionSink,
    showDebug: Boolean,
    padding: PaddingValues,
    modifier: Modifier,
) {
    // Opt-in rounded `surfaceContainer` panel, matching the Now Playing setlist/info panes.
    val panelModifier = if (model.backgroundContainer) {
        Modifier
            .clip(RoundedCornerShape(PANEL_CORNER_RADIUS))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(PANEL_CONTENT_PADDING)
    } else {
        Modifier
    }
    // A section swaps its placeholders for real rows when its load lands, and nothing else in this
    // path animates that: the section is a single lazy item (so `animateItem()` only sees the
    // section, which keeps its key) and these children aren't lazy items at all. Cross-fade the two
    // phases so the block doesn't pop.
    val animationScale = LocalAnimationScale.current
    AnimatedContent(
        targetState = model.sectionItems,
        // Keyed by phase, not by content: a re-emitted list with the same phase (an `active`
        // row, a pagination flag, a page appended below the existing rows) must not re-trigger it.
        contentKey = { it.isPlaceholderPhase() },
        transitionSpec = {
            fadeIn(tween(scaledMillis(BASE_FADE_IN_MILLIS, animationScale))) togetherWith
                fadeOut(tween(scaledMillis(BASE_FADE_OUT_MILLIS, animationScale)))
        },
        label = "SectionContent.phase",
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .then(panelModifier),
    ) { sectionItems ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            sectionItems.forEach {
                it.Content(
                    sink = actionSink,
                    debug = showDebug,
                    mod = Modifier,
                    pad = padding
                )
            }
        }
    }
}

/**
 * Whether the section is showing nothing but placeholders, i.e. its load hasn't produced content
 * yet. [SectionContentColumn] uses this as the phase to cross-fade.
 *
 * It asks whether *everything* is a placeholder rather than whether anything is, because a loaded
 * section carries placeholders too: the page loaders at either end of a paginated window.
 */
private fun ImmutableList<ListModel>.isPlaceholderPhase(): Boolean =
    isNotEmpty() && all { it.isPlaceholder() }

private fun ListModel.isPlaceholder(): Boolean = when (this) {
    is LoadingItemListModel -> true

    // A horizontally scrolling section is flattened into its parent in the grid path, but the list
    // path keeps the scroller, so its children decide the phase.
    is HorizontalScrollerListModel ->
        scrollingItems.isNotEmpty() && scrollingItems.all { it is LoadingItemListModel }

    else -> false
}

private val PANEL_CORNER_RADIUS = 16.dp
private val PANEL_CONTENT_PADDING = 8.dp
