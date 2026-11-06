package net.sigmabeta.sage.sample.bitmaps

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class BitmapGeneratorTest {
    @Test
    fun sameSourceProducesIdenticalArtwork() {
        assertEquals(specFor("same"), specFor("same"))
    }

    @Test
    fun differentSourcesProduceDifferentArtwork() {
        assertNotEquals(specFor("first"), specFor("second"))
    }

    @Test
    fun defaultSizeIsASeededSquare() {
        val spec = specFor("square")
        assertEquals(spec.width, spec.height)
    }

    @Test
    fun explicitDimensionsAreUsed() {
        val spec = specFor("sized", width = 300, height = 200)
        assertEquals(300, spec.width)
        assertEquals(200, spec.height)
    }

    @Test
    fun oneDimensionMakesASquare() {
        val widthOnly = specFor("width-only", width = 300)
        assertEquals(300, widthOnly.width)
        assertEquals(300, widthOnly.height)

        val heightOnly = specFor("height-only", height = 200)
        assertEquals(200, heightOnly.width)
        assertEquals(200, heightOnly.height)
    }

    @Test
    fun dimensionsDoNotChangeTheColours() {
        val small = specFor("same-seed", width = 100, height = 100)
        val large = specFor("same-seed", width = 300, height = 200)
        assertEquals(small.colorTopLeft, large.colorTopLeft)
        assertEquals(small.colorTopRight, large.colorTopRight)
        assertEquals(small.colorBottomLeft, large.colorBottomLeft)
        assertEquals(small.colorBottomRight, large.colorBottomRight)
    }

    @Test
    fun nonPositiveDimensionsAreRejected() {
        assertFailsWith<IllegalArgumentException> { specFor("bad", width = 0) }
        assertFailsWith<IllegalArgumentException> { specFor("bad", height = -1) }
        assertTrue(
            assertFailsWith<IllegalArgumentException> {
                specFor("bad", width = 100, height = 0)
            }.message.orEmpty().contains("height"),
        )
    }
}
