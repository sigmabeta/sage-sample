package net.sigmabeta.sage.sample.bitmaps

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.request.Options
import coil3.toUri
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class GeneratedImageFetcherTest {
    private val context = PlatformContext.INSTANCE
    private val options = Options(context)
    private val imageLoader = ImageLoader(context)

    @Test
    fun generatedSchemeIsHandled() {
        assertNotNull(
            GeneratedImageFetcher.Factory()
                .create("generated://cover/42".toUri(), options, imageLoader),
        )
    }

    @Test
    fun otherSchemesAreLeftToTheRestOfTheRegistry() {
        assertNull(
            GeneratedImageFetcher.Factory()
                .create("https://example.com/cover.png".toUri(), options, imageLoader),
        )
    }

    @Test
    fun urlForCarriesDimensionsAsQueryParameters() {
        assertEquals(
            "generated://cover/42?width=300&height=200",
            GeneratedImageFetcher.urlFor("cover/42", width = 300, height = 200),
        )
        assertEquals(
            "generated://cover/42?width=300",
            GeneratedImageFetcher.urlFor("cover/42", width = 300),
        )
        assertEquals(
            "generated://cover/42",
            GeneratedImageFetcher.urlFor("cover/42"),
        )
    }

    @Test
    fun dimensionsAreReadFromTheQuery() {
        val data = "generated://cover/42?width=300&height=200".toUri()
        assertEquals(300, data.query.intParameter("width"))
        assertEquals(200, data.query.intParameter("height"))
    }

    @Test
    fun malformedOrNonPositiveDimensionsAreIgnored() {
        val data = "generated://cover/42?width=abc&height=-5".toUri()
        assertNull(data.query.intParameter("width"))
        assertNull(data.query.intParameter("height"))
        assertNull(data.query.intParameter("missing"))
    }

    @Test
    fun theSeedExcludesTheDimensionQuery() {
        assertEquals(
            "generated://cover/42",
            seedFor("generated://cover/42?width=300&height=200".toUri()),
        )
        assertEquals(
            "generated://cover/42",
            seedFor("generated://cover/42#fragment".toUri()),
        )
    }
}
