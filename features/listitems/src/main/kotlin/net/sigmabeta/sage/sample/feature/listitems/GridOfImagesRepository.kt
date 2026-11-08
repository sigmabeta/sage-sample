package net.sigmabeta.sage.sample.feature.listitems
import net.sigmabeta.sage.sample.models.SampleItem

import kotlinx.coroutines.flow.Flow
import net.sigmabeta.sage.appcomm.LCE

/**
 * The data seam Grid of Images loads through.
 *
 * It streams SAGE [LCE] values so the view model can publish loading, content and error states
 * without knowing where the data came from. [load] asks for a window of the catalogue: the grid
 * fetches one page at a time with [offset]/[limit]. Tests inject their own fake implementation.
 */
interface GridOfImagesRepository {
    /** Loads the `[offset, offset + limit)` window of the generated catalogue. */
    fun load(offset: Int, limit: Int): Flow<LCE<List<SampleItem>>>

    /** Loads the whole generated set, for code that does not page. */
    fun load(): Flow<LCE<List<SampleItem>>> = load(offset = 0, limit = GRID_OF_IMAGES_ITEM_COUNT)
}

/** How many generated images the demo grid renders. */
const val GRID_OF_IMAGES_ITEM_COUNT = 50

/** The operation name Grid of Images' LCE values carry through loading and error states. */
const val GRID_OF_IMAGES_OPERATION_NAME = "grid-of-images"
