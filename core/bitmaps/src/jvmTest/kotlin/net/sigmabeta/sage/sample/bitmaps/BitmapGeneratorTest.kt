package net.sigmabeta.sage.sample.bitmaps

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class BitmapGeneratorTest {
    @Test
    fun sameSourceProducesIdenticalArtwork() {
        assertEquals(specFor("same", squareImage = true), specFor("same", squareImage = true))
    }

    @Test
    fun differentSourcesProduceDifferentArtwork() {
        assertNotEquals(specFor("first", squareImage = true), specFor("second", squareImage = true))
    }

    @Test
    fun squareImageIsSquare() {
        val spec = specFor("square", squareImage = true)
        assertEquals(spec.width, spec.height)
    }

    @Test
    fun nonSquareImageMayHaveADifferentHeight() {
        val specs = (0 until 8).map { specFor("wide-$it", squareImage = false) }
        assertTrue(specs.any { it.width != it.height })
    }
}
