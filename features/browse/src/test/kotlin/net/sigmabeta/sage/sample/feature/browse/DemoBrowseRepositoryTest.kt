package net.sigmabeta.sage.sample.feature.browse

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import net.sigmabeta.sage.appcomm.LCE
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The demo repository stands in for a network source, but its user-visible row names are still
 * product text: they come through [net.sigmabeta.sage.ui.StringProvider], never hard-coded here.
 */
class DemoBrowseRepositoryTest {
    @Test
    fun `demo content names come from the string provider`() = runTest {
        val repository = DemoBrowseRepository(FakeStringProvider())

        val content = repository.load(BrowseOutcome.CONTENT).toList().last() as LCE.Content

        assertEquals(
            listOf(
                "str-BROWSE_DEMO_ITEM_LISTS",
                "str-BROWSE_DEMO_ITEM_METRO",
                "str-BROWSE_DEMO_ITEM_VOYAGER",
            ),
            content.data.map { it.name },
        )
    }

    @Test
    fun `every demo outcome streams loading before its terminal value`() = runTest {
        BrowseOutcome.entries.forEach { outcome ->
            val emissions = DemoBrowseRepository(FakeStringProvider()).load(outcome).toList()

            assertEquals("loading first for $outcome", 2, emissions.size)
            assertTrue("first value is loading for $outcome", emissions.first() is LCE.Loading)
            assertTrue("second value is terminal for $outcome", emissions.last() !is LCE.Loading)
        }
    }
}
