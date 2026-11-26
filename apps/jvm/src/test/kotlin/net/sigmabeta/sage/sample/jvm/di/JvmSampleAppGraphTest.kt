package net.sigmabeta.sage.sample.jvm.di

import dev.zacsweers.metro.createGraphFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import net.sigmabeta.sage.sample.identity.DemoAccounts
import net.sigmabeta.sage.sample.repository.IdentityState
import net.sigmabeta.sage.sample.repository.SignInOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files
import kotlin.time.Duration.Companion.seconds

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

    @Test
    fun `the desktop graph's identity source starts signed out and signs a Demo account in`() {
        val directory = Files.createTempDirectory("sage-sample-graph").toFile()
        val previous = System.getProperty(SETTINGS_FILE_PROPERTY)
        System.setProperty(SETTINGS_FILE_PROPERTY, File(directory, "settings.properties").path)
        try {
            val identity = createGraphFactory<JvmSampleAppGraph.Factory>().create().identityRepository

            runBlocking {
                withTimeout(60.seconds) {
                    assertEquals(IdentityState.SignedOut, identity.state.first { it != IdentityState.Checking })

                    val outcome = identity.signIn(DemoAccounts.DEMO.username, DemoAccounts.DEMO.password)

                    assertTrue(outcome is SignInOutcome.Accepted)
                    assertTrue(identity.state.value is IdentityState.SignedIn)
                }
            }
            assertTrue(File(directory, "sage-sample.db").exists())
        } finally {
            if (previous == null) System.clearProperty(SETTINGS_FILE_PROPERTY) else System.setProperty(SETTINGS_FILE_PROPERTY, previous)
        }
    }

    private companion object {
        const val SETTINGS_FILE_PROPERTY = "sage.sample.settingsFile"
    }
}
