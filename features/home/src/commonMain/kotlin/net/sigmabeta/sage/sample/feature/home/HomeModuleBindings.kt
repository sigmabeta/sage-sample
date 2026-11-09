package net.sigmabeta.sage.sample.feature.home

import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Multibinds
import net.sigmabeta.sage.di.AppScope
import net.sigmabeta.sage.sample.feature.home.modules.BigImageModuleDefinition
import net.sigmabeta.sage.sample.feature.home.modules.GridImagesModuleDefinition

/**
 * Contributes the Home row definitions as a shared set.
 *
 * Adding a row is one `@Binds @IntoSet` line here plus the definition class; the factory and the
 * view model never learn about concrete module types. The set itself is unscoped, and the
 * factory is injected per screen, so popping and re-opening Home rebuilds the plan.
 */
@ContributesTo(AppScope::class)
interface HomeModuleBindings {

    @Multibinds
    fun homeModuleDefinitions(): Set<HomeModuleDefinition>

    @Binds
    @IntoSet
    fun bindBigImageDefinition(impl: BigImageModuleDefinition): HomeModuleDefinition

    @Binds
    @IntoSet
    fun bindGridImagesDefinition(impl: GridImagesModuleDefinition): HomeModuleDefinition
}
