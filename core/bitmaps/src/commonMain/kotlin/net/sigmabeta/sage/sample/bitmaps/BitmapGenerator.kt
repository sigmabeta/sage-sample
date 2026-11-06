package net.sigmabeta.sage.sample.bitmaps

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.lerp
import kotlin.random.Random

/**
 * Deterministic placeholder-image generator for `@Preview`s and `generated://` Coil requests.
 *
 * Seeds a [Random] from the source object's hash so the same input always produces the same
 * artwork, then draws a per-row gradient into a Compose [ImageBitmap]. Callers that need a
 * specific output size pass [width]/[height]; otherwise the seeded size is a small square.
 * Multiplatform graphics keeps this in commonMain, so the Android and desktop apps share one
 * implementation.
 */
object BitmapGenerator {
    fun generateBitmap(
        sourceInfo: Any,
        width: Int? = null,
        height: Int? = null,
    ): ImageBitmap {
        val spec = specFor(sourceInfo, width, height)

        val bitmap = ImageBitmap(spec.width, spec.height)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply { isAntiAlias = false }

        repeat(spec.height) { row ->
            val heightFraction = if (spec.height > 1) row.toFloat() / (spec.height - 1) else 0f
            val colorLeft = lerp(spec.colorTopLeft, spec.colorBottomLeft, heightFraction)
            val colorRight = lerp(spec.colorTopRight, spec.colorBottomRight, heightFraction)

            paint.shader = LinearGradientShader(
                from = Offset(0f, 0f),
                to = Offset(spec.width.toFloat(), 1f),
                colors = listOf(colorLeft, colorRight),
                tileMode = TileMode.Mirror,
            )
            canvas.drawLine(
                Offset(0f, row.toFloat()),
                Offset(spec.width.toFloat(), row.toFloat()),
                paint,
            )
        }

        return bitmap
    }
}

private const val RNG_LIMIT = 16

/** The seeded geometry and corner colours that [BitmapGenerator] renders. */
internal data class BitmapSpec(
    val width: Int,
    val height: Int,
    val colorTopLeft: Color,
    val colorTopRight: Color,
    val colorBottomLeft: Color,
    val colorBottomRight: Color,
)

internal fun specFor(sourceInfo: Any, width: Int? = null, height: Int? = null): BitmapSpec {
    require(width == null || width > 0) { "width must be positive: $width" }
    require(height == null || height > 0) { "height must be positive: $height" }

    val rng = Random(sourceInfo.hashCode())
    val seeded = rng.nextInt(RNG_LIMIT) + RNG_LIMIT
    val resolvedWidth = width ?: height ?: seeded
    val resolvedHeight = height ?: width ?: seeded

    return BitmapSpec(
        width = resolvedWidth,
        height = resolvedHeight,
        colorTopLeft = Color(rng.nextInt()),
        colorTopRight = Color(rng.nextInt()),
        colorBottomLeft = Color(rng.nextInt()),
        colorBottomRight = Color(rng.nextInt()),
    )
}
