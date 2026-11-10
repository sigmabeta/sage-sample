package net.sigmabeta.sage.sample.ui.components.api

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import net.sigmabeta.sage.sample.ui.components.api.previews.BigImageConstants
import net.sigmabeta.sage.sample.ui.components.api.previews.CoverArtConstants
import net.sigmabeta.sage.sample.ui.components.api.previews.NotifConstants
import net.sigmabeta.sage.sample.ui.components.api.previews.SquareConstants
import net.sigmabeta.sage.sample.ui.components.api.previews.WideItemConstants
import net.sigmabeta.sage.sample.ui.components.api.subs.ElevatedPill
import net.sigmabeta.sage.sample.ui.components.api.subs.ElevatedRoundRect
import net.sigmabeta.sage.sample.ui.components.api.subs.Flasher
import net.sigmabeta.sage.sample.ui.components.api.utils.nextPercentageFloat
import net.sigmabeta.sage.components.LoadingType
import kotlin.random.Random

@Composable
@Suppress("MagicNumber")
fun LoadingItem(
    seed: Long,
    loadingType: LoadingType,
    modifier: Modifier,
    padding: PaddingValues,
) {
    val randomizer = Random(seed)
    val randomDelay = randomizer.nextInt(200)

    // A wide item is a fixed-height row rather than a ratio-sized card, so it gets its own shape:
    // sized by the same ratio rule it scales with the column and then collapses when the row it
    // stands in for arrives.
    if (loadingType == LoadingType.WIDE_ITEM) {
        WideItemLoadingItem(randomizer, randomDelay, modifier, padding)
        return
    }

    val (width, ratio) = when (loadingType) {
        LoadingType.PAGE -> PAGE_MIN_WIDTH.dp to PAGE_ASPECT_RATIO

        LoadingType.SQUARE -> SquareConstants.MIN_WIDTH to SquareConstants.ASPECT_RATIO

        // Same minimum width as SQUARE, but the cover-art 3:4 ratio so games' loading
        // placeholders don't pop from square → portrait when the data arrives.
        LoadingType.COVER -> SquareConstants.MIN_WIDTH to CoverArtConstants.ASPECT_RATIO

        LoadingType.NOTIF -> NotifConstants.MIN_WIDTH to NotifConstants.ASPECT_RATIO

        LoadingType.BIG_IMAGE -> BigImageConstants.MIN_WIDTH to BigImageConstants.ASPECT_RATIO

        else -> return
    }

    ElevatedRoundRect(
        modifier = modifier
            .padding(paddingValues = padding)
            // Section children are stacked by the section renderer with no arrangement of its
            // own, so each renderer supplies its vertical rhythm — this one mirrors the 4 dp the
            // loaded rows use (WideItem, the grid cells' margins), which keeps a block of loading
            // cards from reading as one solid slab. The list screens only ever pass horizontal
            // margins here.
            .padding(vertical = LOADING_ITEM_VERTICAL_SPACING)
            .defaultMinSize(minWidth = width)
            .aspectRatio(ratio),
    ) {
        Flasher(startDelay = randomDelay)
    }
}

/**
 * The wide item's placeholder: a 64 dp row with a square thumbnail block and a two-line text
 * placeholder, mirroring [WideItem] so the block keeps its height when the real rows arrive.
 */
@Composable
@Suppress("MagicNumber")
private fun WideItemLoadingItem(
    randomizer: Random,
    randomDelay: Int,
    modifier: Modifier,
    padding: PaddingValues,
) {
    Row(
        modifier = modifier
            .padding(paddingValues = padding)
            .padding(vertical = LOADING_ITEM_VERTICAL_SPACING)
            .defaultMinSize(minWidth = WideItemConstants.MIN_WIDTH)
            .height(WIDE_ITEM_LOADING_HEIGHT)
            .shadow(elevation = 4.dp, shape = WideItemShape)
            .background(MaterialTheme.colorScheme.surfaceContainer),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .aspectRatio(1.0f),
        ) {
            Flasher(startDelay = randomDelay)
        }

        // One bar for the row's one text field. Its width is random per row and drives the row's
        // width, because the real item is content-sized too: a `WideItem` wraps its title rather
        // than filling the line, and only `defaultMinSize` floors it.
        ElevatedPill(
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .height(14.dp)
                .width(
                    randomizer.nextInt(
                        WIDE_ITEM_TEXT_MIN_WIDTH_DP,
                        WIDE_ITEM_TEXT_MAX_WIDTH_DP,
                    ).dp,
                ),
        ) {
            Flasher(startDelay = randomDelay)
        }
    }
}

private val WIDE_ITEM_LOADING_HEIGHT = 64.dp

/** The range the placeholder title bar (and so the row) varies over. */
private const val WIDE_ITEM_TEXT_MIN_WIDTH_DP = 80
private const val WIDE_ITEM_TEXT_MAX_WIDTH_DP = 240

private val LOADING_ITEM_VERTICAL_SPACING = 4.dp

private const val PAGE_MIN_WIDTH = 300
private const val PAGE_ASPECT_RATIO = 0.77272725f
