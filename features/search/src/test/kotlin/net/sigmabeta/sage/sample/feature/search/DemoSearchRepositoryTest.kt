package net.sigmabeta.sage.sample.feature.search
import net.sigmabeta.sage.sample.models.SearchResult

import kotlinx.coroutines.flow.single
import kotlinx.coroutines.test.runTest
import net.sigmabeta.sage.appcomm.LCE
import net.sigmabeta.sage.components.EmptyStateListModel
import net.sigmabeta.sage.components.ErrorStateListModel
import net.sigmabeta.sage.components.SectionHeaderListModel
import net.sigmabeta.sage.sample.settings.DemoOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The generated search datasource honors the Settings demo outcome at call time, and the two
 * terminal outcomes reach Search's real screen states.
 */
class DemoSearchRepositoryTest {
    private val settings = FakeSampleSettingsManager()
    private val repository = DemoSearchRepository(settings)

    @Test
    fun `content outcome returns the generated list and grid results`() = runTest {
        settings.setDemoOutcome(DemoOutcome.CONTENT)

        assertEquals(
            LCE.Content(searchResultsFor("chrono", SearchResultKind.LIST)),
            repository.searchList("chrono").single(),
        )
        assertEquals(
            LCE.Content(searchResultsFor("chrono", SearchResultKind.GRID)),
            repository.searchGrid("chrono").single(),
        )
    }

    @Test
    fun `empty outcome returns successful empty results for both sections`() = runTest {
        settings.setDemoOutcome(DemoOutcome.EMPTY)

        assertEquals(
            LCE.Content(emptyList<SearchResult>()),
            repository.searchList("chrono").single(),
        )
        assertEquals(
            LCE.Content(emptyList<SearchResult>()),
            repository.searchGrid("chrono").single(),
        )
    }

    @Test
    fun `error outcome fails both sections under their own operation names`() = runTest {
        settings.setDemoOutcome(DemoOutcome.ERROR)

        val list = repository.searchList("chrono").single()
        val grid = repository.searchGrid("chrono").single()

        assertTrue(list is LCE.Error)
        assertTrue(grid is LCE.Error)
        assertEquals(SEARCH_LIST_OPERATION_NAME, (list as LCE.Error).operationName)
        assertEquals(SEARCH_GRID_OPERATION_NAME, (grid as LCE.Error).operationName)
    }

    @Test
    fun `changing the outcome between calls changes the next result without rebuilding`() = runTest {
        settings.setDemoOutcome(DemoOutcome.CONTENT)
        assertTrue(repository.searchList("chrono").single() is LCE.Content)

        settings.setDemoOutcome(DemoOutcome.ERROR)

        assertTrue(repository.searchList("chrono").single() is LCE.Error)
    }

    @Test
    fun `empty outcome makes a settled query show no results`() = runTest {
        settings.setDemoOutcome(DemoOutcome.EMPTY)
        val constraint = "chrono"

        val state = SearchState(
            query = constraint,
            submittedQuery = constraint,
            listResults = repository.searchList(constraint).single(),
            gridResults = repository.searchGrid(constraint).single(),
        )

        val empty = state.toListItems(FakeStringProvider())
            .filterIsInstance<EmptyStateListModel>()
            .single()
        assertEquals("str-SEARCH_NO_RESULTS:chrono", empty.explanation)
    }

    @Test
    fun `error outcome keeps both section headers and shows one error each`() = runTest {
        settings.setDemoOutcome(DemoOutcome.ERROR)
        val constraint = "chrono"

        val rows = SearchState(
            query = constraint,
            submittedQuery = constraint,
            listResults = repository.searchList(constraint).single(),
            gridResults = repository.searchGrid(constraint).single(),
        ).toListItems(FakeStringProvider())

        assertEquals(
            listOf("str-SEARCH_SECTION_LIST", "str-SEARCH_SECTION_GRID"),
            rows.filterIsInstance<SectionHeaderListModel>().map { it.title },
        )
        assertEquals(
            setOf(SEARCH_LIST_OPERATION_NAME, SEARCH_GRID_OPERATION_NAME),
            rows.filterIsInstance<ErrorStateListModel>()
                .map { it.failedOperationName }
                .toSet(),
        )
        assertEquals(2, rows.filterIsInstance<ErrorStateListModel>().size)
    }
}
