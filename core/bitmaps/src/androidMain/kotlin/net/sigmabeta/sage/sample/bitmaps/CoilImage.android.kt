package net.sigmabeta.sage.sample.bitmaps

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import coil3.Image
import coil3.asImage

internal actual fun ImageBitmap.asCoilImage(): Image = asAndroidBitmap().asImage()
