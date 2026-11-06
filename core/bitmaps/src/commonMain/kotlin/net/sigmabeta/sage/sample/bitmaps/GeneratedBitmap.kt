package net.sigmabeta.sage.sample.bitmaps

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale

/**
 * Renders [sourceInfo] as a deterministic generated image.
 *
 * Intended for `@Preview`s (and other inspection-mode renders) in place of a real image loader,
 * so a preview shows meaningful artwork instead of a loading or error placeholder.
 */
@Composable
fun GeneratedBitmap(
    sourceInfo: Any,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    val bitmap = remember(sourceInfo) { BitmapGenerator.generateBitmap(sourceInfo) }
    Image(
        painter = BitmapPainter(bitmap, filterQuality = FilterQuality.None),
        contentDescription = null,
        contentScale = contentScale,
        modifier = modifier,
    )
}
