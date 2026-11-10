package net.sigmabeta.sage.sample.jvm.di

import dev.zacsweers.metro.createGraphFactory
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * Runtime check that the desktop Metro graph builds and resolves the entry points the composition
 * root reads before any screen exists.
 *
 * Compile-time Metro validation catches missing bindings, but this also exercises graph creation
 * itself (which is lazy and otherwise only hit when the window opens).
 */
class JvmSampleAppGraphTest {
    @Test
    fun `the desktop graph resolves its ViewModel factory and storage`() {
        val graph = createGraphFactory<JvmSampleAppGraph.Factory>().create()

        assertNotNull(graph.metroViewModelFactory)
        assertNotNull(graph.storage)
    }
}
