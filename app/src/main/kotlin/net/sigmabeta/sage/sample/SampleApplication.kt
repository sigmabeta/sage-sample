package net.sigmabeta.sage.sample

import android.app.Application
import coil3.SingletonImageLoader
import dev.zacsweers.metro.createGraphFactory
import net.sigmabeta.sage.sample.bitmaps.generatedImageLoader
import net.sigmabeta.sage.sample.di.SampleAppGraph

class SampleApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Register before any composable resolves the singleton, so AsyncImage requests for
        // `generated://` URLs reach :core:bitmaps' fetcher instead of the default loader.
        SingletonImageLoader.setSafe { context -> generatedImageLoader(context) }
    }

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
