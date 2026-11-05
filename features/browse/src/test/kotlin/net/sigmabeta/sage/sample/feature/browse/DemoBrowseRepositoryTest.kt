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

        val content = repository.load().toList().single() as LCE.Content

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
    fun `the repository emits terminal content without adding its own delay`() = runTest {
        val emissions = DemoBrowseRepository(FakeStringProvider()).load().toList()

        assertEquals(1, emissions.size)
        assertTrue(emissions.single() is LCE.Content)
    }
}
