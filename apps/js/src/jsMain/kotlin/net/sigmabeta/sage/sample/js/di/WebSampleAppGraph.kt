package net.sigmabeta.sage.sample.js.di

import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metrox.viewmodel.ViewModelGraph
import net.sigmabeta.sage.di.AppScope
import net.sigmabeta.sage.sample.settings.SampleSettingsManager
import net.sigmabeta.sage.sample.ui.shell.SampleBrowserNavigation
import net.sigmabeta.sage.ui.StringProvider

/**
 * The browser app's dependency graph — the JS twin of `JvmSampleAppGraph` / `SampleAppGraph`.
 *
 * Like the desktop graph it extends [ViewModelGraph], so every feature's `@ContributesIntoMap` view
 * model resolves through `metroViewModel()`. The repository interfaces resolve to the HTTP-backed
 * implementations contributed by `:core:repository:remote`; the demo sources in
 * `:core:repository:demo` are deliberately absent from this graph.
 */
@SingleIn(AppScope::class)
@DependencyGraph(AppScope::class)
interface WebSampleAppGraph : ViewModelGraph {
    /** The composition root reads the persisted theme before any screen exists. */
    val settingsManager: SampleSettingsManager

    /** Installed into `LocalSampleStringProvider`; the shared renderers resolve labels through it. */
    val stringProvider: StringProvider

    /** Mirrors routes into the URL bar and drives the shell from Back/Forward. */
    val browserNavigation: SampleBrowserNavigation

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(): WebSampleAppGraph
    }
}
