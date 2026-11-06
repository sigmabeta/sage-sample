package net.sigmabeta.sage.sample.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import net.sigmabeta.sage.sample.bitmaps.GeneratedBitmap
import net.sigmabeta.sage.sample.bitmaps.GeneratedImageFetcher
import net.sigmabeta.sage.sample.bitmaps.generatedImageLoader

/**
 * Shows the deterministic placeholder art from `:core:bitmaps`. Preview-only: no runtime screen
 * renders generated bitmaps.
 */
@Preview
@Composable
private fun GeneratedBitmapPreview() {
    Surface {
        Row(
            modifier = Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf("Sage", "Sample", "Preview").forEach { source ->
                GeneratedBitmap(
                    sourceInfo = source,
                    width = 96,
                    height = 96,
                    modifier = Modifier.size(96.dp),
                )
            }
        }
    }
}

/**
 * The same artwork, requested through Coil. Each URL is the seed, so `generated://Sage` always
 * renders the same image.
 */
@Preview
@Composable
private fun GeneratedBitmapRequestPreview() {
    val context = LocalPlatformContext.current
    val loader = remember(context) { generatedImageLoader(context) }
    Surface {
        Row(
            modifier = Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf("Sage", "Sample", "Preview").forEach { seed ->
                AsyncImage(
                    model = GeneratedImageFetcher.urlFor(seed, width = 96, height = 96),
                    imageLoader = loader,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(96.dp),
                )
            }
        }
    }
}
