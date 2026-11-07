package net.sigmabeta.sage.sample.feature.listitems

import kotlinx.coroutines.flow.Flow
import net.sigmabeta.sage.appcomm.LCE

/**
 * The data seam List Items loads through.
 *
 * It streams SAGE [LCE] values so the view model can publish loading, content and error states
 * without knowing where the data came from. [load] asks for a window of the catalogue: a paginated
 * screen fetches one page at a time with [offset]/[limit], while a non-paginated screen calls
 * [load] without arguments for the whole generated set. Tests inject their own fake implementation.
 */
interface SampleItemRepository {
    /** Loads the `[offset, offset + limit)` window of the generated catalogue. */
    fun load(offset: Int, limit: Int): Flow<LCE<List<SampleItem>>>

    /** Loads the whole generated set, for the non-paginated List Items screen. */
    fun load(): Flow<LCE<List<SampleItem>>> = load(offset = 0, limit = LIST_ITEMS_ITEM_COUNT)
}

/** The operation name List Items' LCE values carry through loading and error states. */
const val LIST_ITEMS_OPERATION_NAME = "list-items"

/** The size of the generated List Items catalogue, and the non-paginated screen's page limit. */
const val LIST_ITEMS_ITEM_COUNT = 25
