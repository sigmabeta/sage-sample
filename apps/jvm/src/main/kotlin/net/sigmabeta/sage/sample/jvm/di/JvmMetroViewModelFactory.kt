package net.sigmabeta.sage.sample.jvm.di

import androidx.lifecycle.ViewModel
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metrox.viewmodel.MetroViewModelFactory
import kotlin.reflect.KClass
import net.sigmabeta.sage.di.AppScope

/**
 * The `ViewModelProvider.Factory` behind `metroViewModel<T>()` on the desktop target.
 *
 * Metro fills [viewModelProviders] from the graph's ViewModel multibinding map, which every
 * feature populates with `@ContributesIntoMap` + `@ViewModelKey`. This mirrors the Android
 * `SampleMetroViewModelFactory`; the two apps keep their own copy because `:app` is Android-only.
 */
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class JvmMetroViewModelFactory(
    override val viewModelProviders: Map<KClass<out ViewModel>, () -> ViewModel>,
) : MetroViewModelFactory()
