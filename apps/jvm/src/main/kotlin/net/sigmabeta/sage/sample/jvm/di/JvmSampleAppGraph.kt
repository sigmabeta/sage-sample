package net.sigmabeta.sage.sample.jvm.di

import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metrox.viewmodel.ViewModelGraph
import net.sigmabeta.sage.di.AppScope

/**
 * The desktop app's dependency graph — the JVM counterpart of the Android `SampleAppGraph`.
 *
 * Extending [ViewModelGraph] gives the graph Metro's ViewModel multibinding map, which every
 * feature populates with `@ContributesIntoMap` + `@ViewModelKey`. Screens resolve their own view
 * model with `metroViewModel<...>()`; this interface needs no accessor per screen.
 *
 * There is no `Context` binding on the desktop side: Android-only seams are replaced by
 * [JvmSampleBindings].
 */
@SingleIn(AppScope::class)
@DependencyGraph(AppScope::class)
interface JvmSampleAppGraph : ViewModelGraph {
    @DependencyGraph.Factory
    fun interface Factory {
        fun create(): JvmSampleAppGraph
    }
}
