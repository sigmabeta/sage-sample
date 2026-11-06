package net.sigmabeta.sage.sample.ui.components.api.subs

/**
 * Android's hardware renderer already caches each cell's `RenderNode`, so an extra offscreen
 * compositing layer per visible cell costs GPU memory without a matching win. Off here; on for
 * the JVM (Skiko) target, where the cached layer removes per-frame re-rasterization.
 */
internal actual val gridCellOffscreenLayerEnabled: Boolean = false
