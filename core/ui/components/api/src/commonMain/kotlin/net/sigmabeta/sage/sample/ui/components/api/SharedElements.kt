package net.sigmabeta.sage.sample.ui.components.api

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import net.sigmabeta.sage.images.SourceInfo
import net.sigmabeta.sage.sample.models.ArtworkCorner
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
 * Both scopes are optional: outside the shell — previews, screenshot fixtures, isolated render
 * tests — neither is provided and [Modifier.sharedImageElement] is the receiver unchanged.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedTransitionScope = staticCompositionLocalOf<SharedTransitionScope?> { null }

/** Provided per Voyager screen by the tab host; `null` outside the shell. */
val LocalScreenTransitionScope = staticCompositionLocalOf<AnimatedVisibilityScope?> { null }

/**
 * A destination's handoff from the row it was opened from: that row's image request, and the corner
 * treatment of the container that showed it.
 *
 * [corner] is what lets the destination animate its own clip out of the shape the user tapped.
 */
data class ArtworkHandoff(
    val url: String,
    val corner: ArtworkCorner? = null,
)

/**
 * The artwork the user came from, published by a destination that was opened from an image row.
 *
 * It is the *row's* image request, not this screen's: a renderer that would otherwise show an icon
 * placeholder can draw the row's bitmap instead, and win the frame the screen first appears. Purely
 * a cache hint — a renderer with nothing cached falls back to its normal placeholder.
 */
val LocalArtworkHandoff = staticCompositionLocalOf<ArtworkHandoff?> { null }

/**
 * One renderer's shared image: the modifier to apply, and whether it is currently matched to a
 * peer on the screen the transition is coming from.
 *
 * [isMatched] is what lets a renderer choreograph the *one* element that is flying rather than
 * everything on screen — a returning grid can hold back the tapped cell's label without blanking
 * the rest.
 */
@Immutable
data class SharedImageElement(
    val modifier: Modifier,
    val isMatched: Boolean,
)

/**
 * Marks this image as a shared element identified by [sourceInfo].
 *
 * The key is the request's `generated://` URL without its size query. The seed lives in the URL's
 * path, so a row's square thumbnail and the full-width hero it opens — the same artwork at two
 * resolutions — resolve to the same key and animate into one another.
 */
@Composable
fun Modifier.sharedImageElement(
    sourceInfo: SourceInfo,
    corner: ArtworkCorner? = null,
    arrivingFrom: ArtworkCorner? = null,
): Modifier = rememberSharedImageElement(sourceInfo, corner, arrivingFrom).modifier

/**
 * [sharedImageElement] as a value, for callers that also need [SharedImageElement.isMatched].
 *
 * [corner] and [arrivingFrom] are the two ends' shapes. When they differ the overlay clip is
 * animated between them — see [rememberCornerClipShape].
 *
 * This is the *only* shared node an image gets. The container it sits in — a row's circle, a grid
 * cell, the hero's rounded rect — stays on its own screen and fades with it, because a container
 * put in the overlay would be drawn *behind* an image that is somewhere else entirely: a bare
 * surface-coloured shape trailing the artwork everywhere the artwork's clip does not reach (and,
 * with no cached bitmap to paint, a lone circle where the artwork has not arrived). What morphs is
 * the artwork itself: its bounds, and the corner shape of its clip.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun rememberSharedImageElement(
    sourceInfo: SourceInfo,
    corner: ArtworkCorner? = null,
    arrivingFrom: ArtworkCorner? = null,
): SharedImageElement {
    val sharedScope = LocalSharedTransitionScope.current
    val animatedVisibilityScope = LocalScreenTransitionScope.current
    val key = sourceInfo.imageSeedKey()

    if (sharedScope == null || animatedVisibilityScope == null || key == null) {
        return SharedImageElement(Modifier, isMatched = false)
    }

    val boundsTransform = rememberScaledBoundsTransform()
    val state = with(sharedScope) { rememberSharedContentState(key = key) }
    val clipShape = rememberCornerClipShape(
        isMatched = state.isMatchFound,
        entering = animatedVisibilityScope.transition.targetState == EnterExitState.Visible,
        from = arrivingFrom,
        to = corner,
    )

    return SharedImageElement(
        modifier = with(sharedScope) {
            Modifier.sharedElement(
                sharedContentState = state,
                animatedVisibilityScope = animatedVisibilityScope,
                boundsTransform = boundsTransform,
                clipInOverlayDuringTransition = OverlayClip(clipShape),
            )
        },
        isMatched = state.isMatchFound,
    )
}

/** The corner radius of a content detail's hero: the shape every destination's artwork returns to. */
const val HERO_ARTWORK_CORNER_RADIUS_DP: Int = 16

/**
 * The hero's corner as a shared-transition shape.
 *
 * It is a constant, so a destination that can be returned to does not need it carried in a route:
 * it starts its clip here and rounds into its own shape.
 */
val HERO_ARTWORK_CORNER: ArtworkCorner = ArtworkCorner.Rounded(HERO_ARTWORK_CORNER_RADIUS_DP)

/**
 * The overlay clip: [end] once the flight is over, rounding out of [start] while the element is
 * matched to its peer.
 *
 * Matching is the signal rather than the screen's phase, because the clip is only ever consulted
 * for the element that is actually in the overlay.
 *
 * [progress] rests at zero — the start of a flight — and only ever runs forwards while matched.
 * The clip is read only while the element is in the overlay, so what it says while nothing is
 * flying is invisible; but the element that flies is normally composed on the frame the flight
 * begins, and whatever it is `remember`ed at is what that first frame draws. Resting at the start
 * means the first frame already has the source shape, and the morph takes it from there.
 */
@Composable
private fun rememberCornerClipShape(
    isMatched: Boolean,
    entering: Boolean,
    from: ArtworkCorner?,
    to: ArtworkCorner?,
): Shape {
    val start = if (entering) from else to
    val end = if (entering) to else from
    if (start == null || end == null || start == end) {
        return remember(to) { (to ?: ArtworkCorner.Rounded(0)).asShape() }
    }

    val progress = remember { Animatable(0f) }
    val animationScale = LocalAnimationScale.current
    LaunchedEffect(isMatched, start, end) {
        if (!isMatched) {
            // Re-arm for the next flight. Nothing reads the shape until an element is in the
            // overlay again, so landing back on the source corners here cannot be seen.
            progress.snapTo(0f)
            return@LaunchedEffect
        }

        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(scaledMillis(BOUNDS_CORNER_BASE_MILLIS, animationScale)),
        )
    }

    return remember(start, end) { CornerMorphShape({ progress.value }, start, end) }
}

/** The corner as a drawable shape; a circle is a percentage so it fits any size. */
private fun ArtworkCorner.asShape(): Shape = when (this) {
    ArtworkCorner.Circle -> RoundedCornerShape(CIRCLE_PERCENT)
    is ArtworkCorner.Rounded -> RoundedCornerShape(radiusDp.dp)
}

/**
 * Rounds from [from] to [to], following [progress] as it is drawn.
 *
 * The starting radius is captured in pixels while the element is still at its source size, not
 * recomputed from the current bounds: a circle is "half the short side", which grows with the
 * bounds, so recomputing it would balloon the corners mid-flight before pulling them back to the
 * target radius.
 */
private class CornerMorphShape(
    private val progress: () -> Float,
    private val from: ArtworkCorner,
    private val to: ArtworkCorner,
) : Shape {
    private var startRadiusPx: Float? = null

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val fraction = progress().coerceIn(0f, 1f)
        val fromPx = if (fraction <= 0f) {
            from.radiusPx(size, density).also { startRadiusPx = it }
        } else {
            startRadiusPx ?: from.radiusPx(size, density)
        }
        val toPx = to.radiusPx(size, density)

        return Outline.Rounded(
            RoundRect(
                rect = Rect(Offset.Zero, size),
                cornerRadius = CornerRadius(fromPx + (toPx - fromPx) * fraction),
            ),
        )
    }
}

private fun ArtworkCorner.radiusPx(size: Size, density: Density): Float = when (this) {
    ArtworkCorner.Circle -> size.minDimension / 2f
    is ArtworkCorner.Rounded -> with(density) { radiusDp.dp.toPx() }
}

/** Base time the corner takes to settle, before [LocalAnimationScale] stretches it. */
private const val BOUNDS_CORNER_BASE_MILLIS = 300
private const val CIRCLE_PERCENT = 50

/**
 * The bounds spring, at Compose's default stiffness when the animation scale is one.
 *
 * Sharing the scale with the screen transition is what keeps the artwork and the pages moving
 * together: a slower slide with an unscaled morph would have the image arrive long before the
 * screens settle.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun rememberScaledBoundsTransform(): BoundsTransform {
    val scale = LocalAnimationScale.current

    return remember(scale) {
        BoundsTransform { _, _ ->
            spring(
                stiffness = Spring.StiffnessMediumLow / (scale * scale),
                visibilityThreshold = Rect.VisibilityThreshold,
            )
        }
    }
}

/** The size-independent identity of a generated image request; `null` for a non-string source. */
private fun SourceInfo.imageSeedKey(): String? =
    (info as? String)?.substringBefore('?')?.takeIf { it.startsWith("generated://") }
