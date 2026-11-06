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
 * Deterministic placeholder-image generator for `@Preview`s.
 *
 * Seeds a [Random] from the source object's hash so the same input always produces the same
 * artwork, then draws a per-row gradient into a Compose [ImageBitmap]. Multiplatform graphics
 * keeps this in commonMain, so the Android and desktop apps share one implementation.
 */
object BitmapGenerator {
    fun generateBitmap(
        sourceInfo: Any,
        squareImage: Boolean = true,
    ): ImageBitmap {
        val spec = specFor(sourceInfo, squareImage)

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

internal fun specFor(sourceInfo: Any, squareImage: Boolean): BitmapSpec {
    val rng = Random(sourceInfo.hashCode())
    val width = rng.nextInt(RNG_LIMIT) + RNG_LIMIT
    val height = if (squareImage) width else rng.nextInt(RNG_LIMIT) + RNG_LIMIT

    return BitmapSpec(
        width = width,
        height = height,
        colorTopLeft = Color(rng.nextInt()),
        colorTopRight = Color(rng.nextInt()),
        colorBottomLeft = Color(rng.nextInt()),
        colorBottomRight = Color(rng.nextInt()),
    )
}
