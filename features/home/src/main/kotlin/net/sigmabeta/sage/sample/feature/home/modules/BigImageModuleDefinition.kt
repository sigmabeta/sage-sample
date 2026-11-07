package net.sigmabeta.sage.sample.feature.home.modules

import dev.zacsweers.metro.Inject
import net.sigmabeta.sage.sample.feature.home.HomeModule
import net.sigmabeta.sage.sample.feature.home.HomeModuleDefinition
import net.sigmabeta.sage.sample.feature.home.HomeRepository
import net.sigmabeta.sage.ui.StringProvider

/**
 * Registers the daily-hero row with the Home module set.
 *
 * The definition holds the graph dependencies; each Home plan creates its own [BigImageModule]
 * with a plan-unique instance number and data-id range.
 */
@Inject
class BigImageModuleDefinition(
    private val repository: HomeRepository,
    private val stringProvider: StringProvider,
) : HomeModuleDefinition {
    override val typeId = "big_image"

    override val priority = 0

    override fun create(instanceNumber: Int, dataIdBase: Long): HomeModule = BigImageModule(
        id = "${typeId}_$instanceNumber",
        priority = instanceNumber - 1,
        instanceNumber = instanceNumber,
        dataIdBase = dataIdBase,
        repository = repository,
        stringProvider = stringProvider,
    )
}
