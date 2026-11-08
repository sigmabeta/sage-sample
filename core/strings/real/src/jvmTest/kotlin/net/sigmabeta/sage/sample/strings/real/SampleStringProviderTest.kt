package net.sigmabeta.sage.sample.strings.real

import net.sigmabeta.sage.sample.strings.api.SampleStringId
import net.sigmabeta.sage.ui.SageStringId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The shared provider is the single id → text table; these tests pin its completeness and the
 * Android-style formatter it exposes to view models (which cannot use `stringResource`).
 */
class SampleStringProviderTest {
    @Test
    fun `every id has text`() {
        SampleStringId.entries.forEach { id ->
            assertTrue("no text for $id", SampleStringProvider.getString(id).isNotEmpty())
        }
    }

    @Test
    fun `formats a positional int`() {
        assertEquals(
            "Big image 3",
            SampleStringProvider.getStringOneInt(SampleStringId.HOME_SECTION_BIG_IMAGE, 3),
        )
    }

    @Test
    fun `formats a positional string`() {
        assertEquals(
            "No results for \u201cx\u201d.",
            SampleStringProvider.getStringOneArg(SampleStringId.SEARCH_NO_RESULTS, "x"),
        )
    }

    @Test
    fun `formats a plain specifier`() {
        assertEquals(
            "Open item",
            SampleStringProvider.getStringOneArg(SampleStringId.ACCY_OCL_SINGLE_LINE, "item"),
        )
    }

    @Test(expected = IllegalStateException::class)
    fun `an unmapped id fails loudly`() {
        SampleStringProvider.getString(UnknownStringId)
    }

    private data object UnknownStringId : SageStringId
}
