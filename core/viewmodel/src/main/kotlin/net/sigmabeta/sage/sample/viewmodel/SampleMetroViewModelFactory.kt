package net.sigmabeta.sage.sample.viewmodel

import androidx.lifecycle.ViewModel
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metrox.viewmodel.MetroViewModelFactory
import kotlin.reflect.KClass
import net.sigmabeta.sage.di.AppScope

/**
 * The `ViewModelProvider.Factory` behind `metroViewModel<T>()`.
 *
 * Metro fills [viewModelProviders] from the graph's ViewModel multibinding map, which every
 * feature populates with `@ContributesIntoMap` + `@ViewModelKey`. Contributing the factory as a
 * `MetroViewModelFactory` binding is what lets an app graph extending `ViewModelGraph` resolve
 * `metroViewModelFactory` — Metro does not synthesize it. Both apps share this binding, so neither
 * carries a factory of its own.
 */
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class SampleMetroViewModelFactory(
    override val viewModelProviders: Map<KClass<out ViewModel>, () -> ViewModel>,
) : MetroViewModelFactory()
