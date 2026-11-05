package net.sigmabeta.sage.sample.di

import android.app.Application
import android.content.Context
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import net.sigmabeta.sage.di.AppScope
import net.sigmabeta.sage.logging.Hatchet
import net.sigmabeta.sage.sample.feature.welcome.WelcomeViewModelBrain
import net.sigmabeta.sage.ui.StringProvider

/**
 * The application-wide dependency graph.
 *
 * SAGE no longer ships Hilt: `@DependencyGraph(AppScope::class)` is the entry point, and every
 * sage module + app module contributes bindings to this same scope with
 * `@ContributesTo(AppScope::class)` / `@BindingContainer`. Depend only on the accessors an entry
 * point actually reads; everything else is resolved transitively.
 */
@SingleIn(AppScope::class)
@DependencyGraph(AppScope::class)
interface SampleAppGraph {
    val hatchet: Hatchet
    val stringProvider: StringProvider
    val welcomeBrain: WelcomeViewModelBrain

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
