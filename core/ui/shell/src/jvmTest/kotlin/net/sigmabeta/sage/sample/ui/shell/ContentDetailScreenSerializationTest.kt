package net.sigmabeta.sage.sample.ui.shell

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import net.sigmabeta.sage.sample.models.ArtworkCorner
import net.sigmabeta.sage.sample.models.ContentArtworkSize
import net.sigmabeta.sage.sample.models.ContentId
import net.sigmabeta.sage.sample.nav.SampleDestination
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Voyager writes each [cafe.adriel.voyager.core.screen.Screen] into the Activity's saved state as a
 * `java.io.Serializable`. A Content detail screen carrying [ContentId] / [ContentArtworkSize]
 * fields crashed API 30 with `NotSerializableException` when the activity stopped; the screen now
 * stores their wire strings, and this round-trip pins that.
 */
class ContentDetailScreenSerializationTest {
    @Test
    fun `a content detail screen survives java serialization`() {
        val screen = screenFor(
            destination = SampleDestination.CONTENT_DETAIL,
            contentId = ContentId("grid-of-images:7"),
            artworkSize = ContentArtworkSize(256, 256, ArtworkCorner.Rounded(16)),
        ) as ContentDetailScreen

        val restored = roundTrip(screen)

        assertEquals(screen, restored)
        assertEquals(SampleDestination.CONTENT_DETAIL, restored.destination)
        assertEquals(ContentId("grid-of-images:7"), restored.contentId)
        assertEquals(ContentArtworkSize(256, 256, ArtworkCorner.Rounded(16)), restored.artworkSize)
        assertEquals(screen.key, restored.key)
    }

    @Test
    fun `a not-found content detail screen also survives`() {
        val restored = roundTrip(
            screenFor(destination = SampleDestination.CONTENT_DETAIL) as ContentDetailScreen,
        )

        assertEquals(null, restored.contentId)
        assertEquals(null, restored.artworkSize)
    }

    private fun roundTrip(screen: ContentDetailScreen): ContentDetailScreen {
        val bytes = ByteArrayOutputStream().use { buffer ->
            ObjectOutputStream(buffer).use { it.writeObject(screen) }
            buffer.toByteArray()
        }
        return ObjectInputStream(ByteArrayInputStream(bytes)).use { it.readObject() } as ContentDetailScreen
    }
}
