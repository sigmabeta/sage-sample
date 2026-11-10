package net.sigmabeta.sage.sample.ui.components.api

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import net.sigmabeta.sage.images.SourceInfo
/**
 * Shared-element plumbing for the sample's image renderers.
 *
 * The shell hosts one [SharedTransitionLayout] above every tab's navigator and publishes it as
 * [LocalSharedTransitionScope]; each screen's transition slot publishes its own
 * [AnimatedVisibilityScope] as [LocalScreenTransitionScope]. A renderer then calls
 * [Modifier.sharedImageElement] with its image request, and the same image appearing in both the
 * outgoing and the incoming screen morphs between the two instead of sliding with the rest of the
 * page.
 *
 * Both halves are optional: outside the shell — previews, screenshot fixtures, isolated render
 * tests — no scope is provided and the modifier is the receiver unchanged.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedTransitionScope = staticCompositionLocalOf<SharedTransitionScope?> { null }

/** Provided per Voyager screen by the tab host; `null` outside the shell. */
val LocalScreenTransitionScope = staticCompositionLocalOf<AnimatedVisibilityScope?> { null }

/**
 * The artwork the user came from, published by a destination that was opened from an image row.
 *
 * It is the *row's* image request, not this screen's: a renderer that would otherwise show an icon
 * placeholder can draw the row's bitmap instead, and win the frame the screen first appears. Purely
 * a cache hint — a renderer with nothing cached falls back to its normal placeholder.
 */
val LocalArtworkHandoff = staticCompositionLocalOf<String?> { null }

/**
 * Marks this image as a shared element identified by [sourceInfo].
 *
 * The key is the request's `generated://` URL without its size query. The seed lives in the URL's
 * path, so a row's square thumbnail and the full-width hero it opens — the same artwork at two
 * resolutions — resolve to the same key and animate into one another.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.sharedImageElement(sourceInfo: SourceInfo): Modifier {
    val sharedScope = LocalSharedTransitionScope.current ?: return this
    val animatedVisibilityScope = LocalScreenTransitionScope.current ?: return this
    val key = sourceInfo.imageSeedKey() ?: return this

    return with(sharedScope) {
        sharedElement(
            sharedContentState = rememberSharedContentState(key = key),
            animatedVisibilityScope = animatedVisibilityScope,
        )
    }
}

/**
 * The frame the artwork travels in: a [sharedBounds] pair with the same [sourceInfo] identity as
 * [sharedImageElement].
 *
 * Apply it to the *container* — the row's circle, the grid cell, the hero's rounded rect — and
 * leave [sharedImageElement] on the image inside it. The bounds carry the container's geometry, so
 * the two shapes morph into each other and the artwork keeps filling them; [clipShape] is what
 * rounds the corners while the element is drawn in the overlay, since the container's own clipping
 * lives inside the modifier chain and is not inherited by it.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.sharedImageBounds(
    sourceInfo: SourceInfo,
    clipShape: Shape = RectangleShape,
): Modifier {
    val sharedScope = LocalSharedTransitionScope.current ?: return this
    val animatedVisibilityScope = LocalScreenTransitionScope.current ?: return this
    val key = sourceInfo.imageSeedKey() ?: return this

    return with(sharedScope) {
        sharedBounds(
            sharedContentState = rememberSharedContentState(key = "$key#bounds"),
            animatedVisibilityScope = animatedVisibilityScope,
            enter = fadeIn(),
            exit = fadeOut(),
            clipInOverlayDuringTransition = OverlayClip(clipShape),
        )
    }
}

/** The size-independent identity of a generated image request; `null` for a non-string source. */
private fun SourceInfo.imageSeedKey(): String? =
    (info as? String)?.substringBefore('?')?.takeIf { it.startsWith("generated://") }
