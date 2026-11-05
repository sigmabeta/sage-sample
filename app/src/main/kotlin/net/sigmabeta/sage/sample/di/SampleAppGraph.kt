package net.sigmabeta.sage.sample.di

import android.app.Application
import android.content.Context
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metrox.viewmodel.ViewModelGraph
import net.sigmabeta.sage.di.AppScope
import net.sigmabeta.sage.sample.settings.SampleSettingsManager

/**
 * The application-wide dependency graph.
 *
 * SAGE no longer ships Hilt: `@DependencyGraph(AppScope::class)` is the entry point, and every
 * sage module + app module contributes bindings to this same scope with
 * `@ContributesTo(AppScope::class)` / `@BindingContainer`.
 *
 * Extending [ViewModelGraph] adds Metro's ViewModel multibinding map, so each feature contributes
 * its view model (`@ContributesIntoMap` + `@ViewModelKey`) and screens resolve it with
 * `metroViewModel<...>()`. The graph therefore needs no accessor per screen — only
 * `metroViewModelFactory`, which MainActivity installs via `LocalMetroViewModelFactory`.
 */
@SingleIn(AppScope::class)
@DependencyGraph(AppScope::class)
interface SampleAppGraph : ViewModelGraph {
    /**
     * The composition root reads the persisted theme from this app-scoped manager before any
     * screen exists, so it is a graph accessor rather than a per-screen view model.
     */
    val settingsManager: SampleSettingsManager

    /**
     * Contributed modules that take a `Context` (strings, analytics) get the application
     * context from this binding. With a single app-scope graph there is no qualifier to
     * disambiguate — Metro matches `Context` directly.
     */
    @Provides
    @SingleIn(AppScope::class)
    fun provideAppContext(application: Application): Context = application

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(@Provides application: Application): SampleAppGraph
    }
}
