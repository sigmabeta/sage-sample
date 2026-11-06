package net.sigmabeta.sage.sample.ui.components.api.subs

/**
 * Skiko caches a cell's rasterized contents into an offscreen layer, so scroll translates that
 * layer instead of re-rasterizing the image + scrim + text on every frame. Enabled on desktop.
 */
internal actual val gridCellOffscreenLayerEnabled: Boolean = true
