package net.sigmabeta.sage.sample.di

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
 * `MetroViewModelFactory` binding is what lets `SampleAppGraph : ViewModelGraph` resolve
 * `metroViewModelFactory` — Metro does not synthesize it. Replaces Hilt's `hiltViewModel()`.
 */
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class SampleMetroViewModelFactory(
    override val viewModelProviders: Map<KClass<out ViewModel>, () -> ViewModel>,
) : MetroViewModelFactory()
