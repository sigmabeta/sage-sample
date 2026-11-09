package net.sigmabeta.sage.sample.feature.search

import org.junit.Assert.assertEquals
import net.sigmabeta.sage.sample.repository.MAX_SEARCH_RESULT_COUNT
import net.sigmabeta.sage.sample.repository.MIN_SEARCH_RESULT_COUNT
import net.sigmabeta.sage.sample.repository.searchSeedFor
import net.sigmabeta.sage.sample.repository.SearchResultKind
import net.sigmabeta.sage.sample.repository.demo.DemoContent
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The generated results are the screen's content, so their contract is worth pinning: the query
 * plus the result kind is the seed, normalization does not change the seed, and short queries do
 * not search. Each kind has its own RNG stream so each repository query can choose its own count.
 */
class SearchResultGeneratorTest {
    @Test
    fun `the same query and kind always generate the same results`() {
        SearchResultKind.entries.forEach { kind ->
            assertEquals(
                DemoContent.searchResults("chrono trigger", kind),
                DemoContent.searchResults("chrono trigger", kind),
            )
        }
    }

    @Test
    fun `different queries generate different results`() {
        SearchResultKind.entries.forEach { kind ->
            assertNotEquals(
                DemoContent.searchResults("chrono", kind),
                DemoContent.searchResults("final fantasy", kind),
            )
        }
    }

    @Test
    fun `the list and grid queries use independent seeds`() {
        assertNotEquals(
            searchSeedFor("chrono", SearchResultKind.LIST),
            searchSeedFor("chrono", SearchResultKind.GRID),
        )
    }

    @Test
    fun `case and surrounding whitespace do not change the seed`() {
        SearchResultKind.entries.forEach { kind ->
            assertEquals(
                DemoContent.searchResults("Chrono Trigger", kind),
                DemoContent.searchResults("  chrono trigger  ", kind),
            )
        }
    }

    @Test
    fun `queries shorter than the minimum generate no results`() {
        SearchResultKind.entries.forEach { kind ->
            assertTrue(DemoContent.searchResults("", kind).isEmpty())
            assertTrue(DemoContent.searchResults("  ", kind).isEmpty())
            assertTrue(DemoContent.searchResults("ab", kind).isEmpty())
        }
    }

    @Test
    fun `each query returns between one and twenty results with unique ids and non-blank text`() {
        listOf("mitsuda", "chrono", "final fantasy", "xenoblade", "abc").forEach { query ->
            SearchResultKind.entries.forEach { kind ->
                val results = DemoContent.searchResults(query, kind)

                assertTrue(
                    "$query/$kind returned ${results.size} results",
                    results.size in MIN_SEARCH_RESULT_COUNT..MAX_SEARCH_RESULT_COUNT,
                )
                assertEquals(results.indices.map { it + 1L }, results.map { it.id })
                assertTrue(results.all { it.name.isNotBlank() })
                assertTrue(results.all { it.caption.isNotBlank() })
            }
        }
    }

    @Test
    fun `minimum-length queries are searchable`() {
        SearchResultKind.entries.forEach { kind ->
            assertTrue(DemoContent.searchResults("abc", kind).isNotEmpty())
        }
    }
}
