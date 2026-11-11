package net.sigmabeta.sage.sample.ui.components.api

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.sigmabeta.sage.images.SourceInfo
import net.sigmabeta.sage.sample.models.ArtworkCorner

/**
 * Base time a row's label takes to leave, and to return, before [LocalAnimationScale] stretches it.
 */
private const val LABEL_BASE_MILLIS = 100

/**
 * The label half of a row's shared-element transition.
 *
 * A row's artwork travels as a shared element while its label cannot: the label isn't part of the
 * matched element, so left alone it would sit still while the picture flies away, and pop back into
 * place before the picture lands. This sequences it — out first, so the screen transition can wait
 * for it; back only once the artwork has arrived.
 *
 * It applies to the cell whose artwork is actually matched, not to every row on the screen: on a
 * return, only the row the artwork lands in holds its label back.
 */
@Stable
class ImageLabelChoreography internal constructor(
    /** The shared-element modifier for this row's artwork. */
    val imageModifier: Modifier,
    private val animatable: Animatable<Float, *>,
    private val scope: CoroutineScope,
    private val fadeMillis: Int,
) {
    private var dispatching = false

    /** Label opacity. Read it inside a `graphicsLayer`, so fading never recomposes the row. */
    val alpha: Float get() = animatable.value

    /**
     * Fades the label out and only then runs [navigate], so the push transition starts on a row
     * that is already just artwork.
     */
    fun click(navigate: () -> Unit) {
        if (dispatching) return
        dispatching = true

        scope.launch {
            animatable.animateTo(targetValue = 0f, animationSpec = tween(fadeMillis))
            navigate()
        }
    }
}

/**
 * Remembers the label choreography for [sourceInfo]'s row.
 *
 * The label is held back exactly while its artwork is matched to a peer on the other screen, and
 * fades back in when that match ends — i.e. when the artwork has landed.
 */
@Composable
fun rememberImageLabelChoreography(
    sourceInfo: SourceInfo,
    corner: ArtworkCorner? = null,
    arrivingFrom: ArtworkCorner? = null,
): ImageLabelChoreography {
    val sharedImage = rememberSharedImageElement(sourceInfo, corner, arrivingFrom)
    val labelAlpha = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    val animationScale = LocalAnimationScale.current
    val fadeMillis = remember(animationScale) {
        scaledMillis(LABEL_BASE_MILLIS, animationScale)
    }

    LaunchedEffect(sharedImage.isMatched, fadeMillis) {
        if (sharedImage.isMatched) {
            // This row's artwork is the one in flight, so its label belongs to the arrival.
            labelAlpha.snapTo(0f)
        } else if (labelAlpha.value < 1f) {
            labelAlpha.animateTo(targetValue = 1f, animationSpec = tween(fadeMillis))
        }
    }

    return remember(sharedImage.modifier, labelAlpha, fadeMillis) {
        ImageLabelChoreography(
            imageModifier = sharedImage.modifier,
            animatable = labelAlpha,
            scope = scope,
            fadeMillis = fadeMillis,
        )
    }
}
