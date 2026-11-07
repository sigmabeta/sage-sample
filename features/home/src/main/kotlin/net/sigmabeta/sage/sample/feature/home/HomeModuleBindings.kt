package net.sigmabeta.sage.sample.feature.home

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import net.sigmabeta.sage.di.AppScope
import net.sigmabeta.sage.sample.settings.SampleSettingsManager

/**
 * Supplies one Home screen with its randomized module list.
 *
 * The provider is intentionally unscoped: each [HomeViewModel] gets a fresh plan, so popping and
 * re-opening Home produces a new order and count. [HomeModuleFactory] owns the construction so
 * the view model only ever sees the `List<HomeModule>` contract. "Home shows same content" turns
 * that fresh plan into the factory's fixed-seed plan.
 */
@BindingContainer
@ContributesTo(AppScope::class)
object HomeModuleBindings {

    @Provides
    fun provideHomeModules(
        factory: HomeModuleFactory,
        settingsManager: SampleSettingsManager,
    ): List<HomeModule> = factory.create(
        homeShowsSameContent = settingsManager.homeShowsSameContent.value,
    )
}
