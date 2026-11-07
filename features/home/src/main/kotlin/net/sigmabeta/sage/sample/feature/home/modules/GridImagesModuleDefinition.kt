package net.sigmabeta.sage.sample.feature.home.modules

import dev.zacsweers.metro.Inject
import net.sigmabeta.sage.sample.feature.home.HomeModule
import net.sigmabeta.sage.sample.feature.home.HomeModuleDefinition
import net.sigmabeta.sage.sample.feature.home.HomeRepository
import net.sigmabeta.sage.ui.StringProvider

/**
 * Registers the bounded image-row implementation with the Home module set.
 *
 * The definition holds the graph dependencies; each Home plan creates its own [GridImagesModule]
 * with a plan-unique instance number and data-id range.
 */
@Inject
class GridImagesModuleDefinition(
    private val repository: HomeRepository,
    private val stringProvider: StringProvider,
) : HomeModuleDefinition {
    override val typeId = "grid_images"

    override val priority = 1

    override fun create(instanceNumber: Int, dataIdBase: Long): HomeModule = GridImagesModule(
        id = "${typeId}_$instanceNumber",
        priority = instanceNumber - 1,
        instanceNumber = instanceNumber,
        dataIdBase = dataIdBase,
        repository = repository,
        stringProvider = stringProvider,
    )
}
