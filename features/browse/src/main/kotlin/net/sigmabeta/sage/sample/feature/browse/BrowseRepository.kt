package net.sigmabeta.sage.sample.feature.browse

import kotlinx.coroutines.flow.Flow
import net.sigmabeta.sage.appcomm.LCE

/**
 * The data seam Browse loads through.
 *
 * It streams SAGE [LCE] values so the view model can publish loading, content and error states
 * without knowing where the data came from. Tests inject their own fake implementation.
 */
fun interface BrowseRepository {
    fun load(): Flow<LCE<List<BrowseItem>>>
}

/** The operation name Browse's LCE values carry through loading and error states. */
const val BROWSE_OPERATION_NAME = "browse"
