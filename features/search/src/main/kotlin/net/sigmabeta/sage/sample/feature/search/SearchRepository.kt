package net.sigmabeta.sage.sample.feature.search
import net.sigmabeta.sage.sample.models.SearchResult

import kotlinx.coroutines.flow.Flow
import net.sigmabeta.sage.appcomm.LCE

/**
 * The search datasource seam.
 *
 * A query runs two independent repository calls: [searchList] supplies the row-shaped results
 * (rendered as `ImageNameCaptionListModel`), and [searchGrid] supplies the grid-image results
 * (rendered as `GridImageListModel`). Keeping them separate means each can have its own random
 * result count, loading state, and error state.
 *
 * The sample has no backend, so [DemoSearchRepository] generates deterministic content. The
 * view model talks to this interface rather than the generator so tests can stand in their own
 * data and, later, a real repository can replace it without touching the screen.
 */
interface SearchRepository {
    fun searchList(query: String): Flow<LCE<List<SearchResult>>>

    fun searchGrid(query: String): Flow<LCE<List<SearchResult>>>
}
