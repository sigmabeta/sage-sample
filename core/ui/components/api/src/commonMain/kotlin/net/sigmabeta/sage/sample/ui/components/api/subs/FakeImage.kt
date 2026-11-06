package net.sigmabeta.sage.sample.ui.components.api.subs

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import net.sigmabeta.sage.images.SourceInfo
import net.sigmabeta.sage.sample.bitmaps.GeneratedBitmap

/**
 * Inspection-only stand-in for a real Coil-loaded image: the shared deterministic generated
 * gradient (`:core:bitmaps`), so `@Preview` renders meaningful artwork instead of the
 * loading/error placeholder Coil falls back to when it can't fetch in a preview.
 *
 * Reached via [CrossfadeImage]'s `forceGenBitmap` branch (defaulting to
 * `LocalInspectionMode.current`).
 */
@Composable
internal fun FakeImage(sourceInfo: SourceInfo, modifier: Modifier) {
    GeneratedBitmap(
        sourceInfo = sourceInfo.info.toString(),
        modifier = modifier,
    )
}
