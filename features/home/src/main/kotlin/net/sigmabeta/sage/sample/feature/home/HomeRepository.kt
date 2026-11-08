package net.sigmabeta.sage.sample.feature.home

import net.sigmabeta.sage.sample.models.HomeItem

import kotlinx.coroutines.flow.Flow
import net.sigmabeta.sage.appcomm.LCE

/**
 * The Home feature's datasource seam.
 *
 * Every call takes the module instance's [seed] — its stable id — so two instances of the same
 * module generate different content, while the same instance always reproduces the same items.
 * The two modules ask for different shapes: [BigImageModule] needs a count plus a single indexed
 * pick (a daily, stable hero), while [GridImagesModule] needs a bounded list. Both stay cheap:
 * neither call loads the whole catalog to filter it in memory, which is the rule real Home
 * modules follow. The sample's [DemoHomeRepository] generates deterministic content; tests can
 * substitute their own data without touching either module.
 */
interface HomeRepository {
    /** Total number of items available to the daily pick for [seed]. */
    fun itemCount(seed: String): Int

    /** The item at [index] for [seed], or null when that catalog is shorter. */
    fun itemAt(seed: String, index: Int): HomeItem?

    /** The first [limit] items for [seed], streamed with the grid's loading state. */
    fun gridImages(seed: String, limit: Int): Flow<LCE<List<HomeItem>>>
}
