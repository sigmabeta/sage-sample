package net.sigmabeta.sage.sample.bitmaps

import androidx.compose.ui.graphics.ImageBitmap
import coil3.Image

/**
 * The browser never routes generated art through Coil: the shared renderers force the
 * [GeneratedBitmap] path (`LocalForceFakeImages`), which draws the bitmap directly. A request that
 * still reaches this seam is a wiring bug, so fail loudly instead of returning a blank image.
 */
internal actual fun ImageBitmap.asCoilImage(): Image =
    error("Generated images render directly on web; Coil fetching is not used")
