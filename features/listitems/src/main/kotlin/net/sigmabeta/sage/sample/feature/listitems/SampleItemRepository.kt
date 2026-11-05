package net.sigmabeta.sage.sample.feature.listitems

import kotlinx.coroutines.flow.Flow
import net.sigmabeta.sage.appcomm.LCE

/**
 * The data seam List Items loads through.
 *
 * It streams SAGE [LCE] values so the view model can publish loading, content and error states
 * without knowing where the data came from. Tests inject their own fake implementation.
 */
fun interface SampleItemRepository {
    fun load(): Flow<LCE<List<SampleItem>>>
}

/** The operation name List Items' LCE values carry through loading and error states. */
const val LIST_ITEMS_OPERATION_NAME = "list-items"
