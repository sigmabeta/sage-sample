package net.sigmabeta.sage.sample.ui.components.api.subs

/**
 * Compose for Web renders through Skiko, so the per-cell offscreen layer is the same scroll win
 * it is on desktop: the rasterized cell is translated instead of re-rasterized every frame.
 */
internal actual val gridCellOffscreenLayerEnabled: Boolean = true
