package net.sigmabeta.sage.sample.jvm.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.sigmabeta.sage.sample.bitmaps.GeneratedBitmap

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
                GeneratedBitmap(sourceInfo = source, modifier = Modifier.size(96.dp))
            }
        }
    }
}
