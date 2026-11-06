package net.sigmabeta.sage.sample.ui.components.api.subs

/**
 * Platform gate for the per-cell [androidx.compose.animation.AnimatedContent] +
 * [androidx.compose.animation.Crossfade] wrappers in [CrossfadeImage].
 *
 * The sample has no JavaScript target, so JVM and Android both get the smooth transitions.
 */
internal val crossfadeImagesEnabled: Boolean = true
