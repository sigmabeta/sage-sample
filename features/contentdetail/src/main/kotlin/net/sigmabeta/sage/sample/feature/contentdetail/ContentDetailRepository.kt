package net.sigmabeta.sage.sample.feature.contentdetail

import kotlinx.coroutines.flow.Flow
import net.sigmabeta.sage.appcomm.LCE

/**
 * The data seam Content detail loads through.
 *
 * It streams SAGE [LCE] values so the view model can publish loading, content and error states
 * without knowing where the data came from. Tests inject their own fake implementation.
 */
fun interface ContentDetailRepository {
    fun load(): Flow<LCE<ContentDetail>>
}

/** The operation name Content detail's LCE values carry through loading and error states. */
const val CONTENT_DETAIL_OPERATION_NAME = "content-detail"
