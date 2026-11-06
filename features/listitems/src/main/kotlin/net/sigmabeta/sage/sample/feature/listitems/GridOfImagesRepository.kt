package net.sigmabeta.sage.sample.feature.listitems

import kotlinx.coroutines.flow.Flow
import net.sigmabeta.sage.appcomm.LCE

/**
 * The data seam Grid of Images loads through.
 *
 * It streams SAGE [LCE] values so the view model can publish loading, content and error states
 * without knowing where the data came from. Tests inject their own fake implementation.
 */
fun interface GridOfImagesRepository {
    fun load(): Flow<LCE<List<SampleItem>>>
}

/** How many generated images the demo grid renders. */
const val GRID_OF_IMAGES_ITEM_COUNT = 50

/** The operation name Grid of Images' LCE values carry through loading and error states. */
const val GRID_OF_IMAGES_OPERATION_NAME = "grid-of-images"
