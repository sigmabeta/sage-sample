package net.sigmabeta.sage.sample.bitmaps

import androidx.compose.ui.graphics.ImageBitmap
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.Uri
import coil3.decode.DataSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.ImageFetchResult
import coil3.request.Options

/**
 * Serves deterministic placeholder art for `generated://` URLs.
 *
 * The URL's scheme, authority and path form the seed, so `generated://cover/42` always produces
 * the same colours. Optional `?width=` and `?height=` query parameters pick the output size
 * without changing the artwork; omit both for the generator's seeded size, or pass one to get a
 * square image. Returning an [ImageFetchResult] hands Coil a ready-made image and skips the
 * decoding pipeline.
 *
 * Register [Factory] on an [ImageLoader] (see [generatedImageLoader]) and request a URL built by
 * [urlFor].
 */
class GeneratedImageFetcher(
    private val seed: String,
    private val width: Int? = null,
    private val height: Int? = null,
) : Fetcher {
    override suspend fun fetch(): FetchResult = ImageFetchResult(
        image = BitmapGenerator.generateBitmap(seed, width, height).asCoilImage(),
        isSampled = false,
        dataSource = DataSource.MEMORY,
    )

    class Factory : Fetcher.Factory<Uri> {
        override fun create(
            data: Uri,
            options: Options,
            imageLoader: ImageLoader,
        ): Fetcher? = if (data.scheme.equals(SCHEME, ignoreCase = true)) {
            GeneratedImageFetcher(
                seed = seedFor(data),
                width = data.query.intParameter("width"),
                height = data.query.intParameter("height"),
            )
        } else {
            null
        }
    }

    companion object {
        const val SCHEME = "generated"

        /**
         * Builds a request URL seeded by [seed] and rendered at [width] × [height].
         *
         * Passing only one dimension yields a square image; passing neither keeps the generator's
         * seeded size.
         */
        fun urlFor(
            seed: String,
            width: Int? = null,
            height: Int? = null,
        ): String {
            val query = listOfNotNull(
                width?.let { "width=$it" },
                height?.let { "height=$it" },
            ).joinToString("&")
            return buildString {
                append(SCHEME).append("://").append(seed)
                if (query.isNotEmpty()) append('?').append(query)
            }
        }
    }
}

/** An [ImageLoader] that can serve generated images alongside Coil's default components. */
fun generatedImageLoader(context: PlatformContext): ImageLoader =
    ImageLoader.Builder(context)
        .components { add(GeneratedImageFetcher.Factory()) }
        .build()

/**
 * The seed is the URL without its query or fragment, so presentation-only parameters such as
 * `width`/`height` don't change the artwork.
 */
internal fun seedFor(data: Uri): String =
    data.newBuilder()
        .query(null)
        .fragment(null)
        .build()
        .toString()

/** Reads a positive integer query parameter, ignoring malformed or non-positive values. */
internal fun String?.intParameter(name: String): Int? =
    this?.split('&')
        ?.firstNotNullOfOrNull { part ->
            val key = part.substringBefore('=', missingDelimiterValue = "")
            if (key != name) return@firstNotNullOfOrNull null
            part.substringAfter('=', missingDelimiterValue = "")
                .toIntOrNull()
                ?.takeIf { it > 0 }
        }

/** Converts the Compose bitmap into the platform image Coil's [ImageFetchResult] expects. */
internal expect fun ImageBitmap.asCoilImage(): coil3.Image
