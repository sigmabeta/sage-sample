package net.sigmabeta.sage.sample

import android.app.Application
import dev.zacsweers.metro.createGraphFactory
import net.sigmabeta.sage.sample.di.SampleAppGraph

class SampleApplication : Application() {
    /**
     * The application-wide Metro graph. Created lazily on first use and shared by every entry
     * point (Activity, Service, ContentProvider) that needs app-scoped bindings.
     *
     * `createGraphFactory` is generated at compile time by the Metro plugin; the factory binds
     * this `Application` into the graph so contributed modules can take a `Context`.
     */
    val appGraph: SampleAppGraph by lazy {
        createGraphFactory<SampleAppGraph.Factory>().create(this)
    }
}
