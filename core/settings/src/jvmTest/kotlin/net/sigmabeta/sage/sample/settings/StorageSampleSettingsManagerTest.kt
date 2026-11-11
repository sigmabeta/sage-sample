package net.sigmabeta.sage.sample.settings

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import net.sigmabeta.sage.storage.common.Storage
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The Storage-backed manager's demo outcome: default when absent, and written/read across
 * manager instances through the same [Storage], which is what survives an app restart.
 */
class StorageSampleSettingsManagerTest {
    private val scope = CoroutineScope(Dispatchers.Unconfined)
    private val storage = FakeStorage()

    @Test
    fun `demo outcome defaults to content when nothing is stored`() {
        assertEquals(DemoOutcome.CONTENT, manager().demoOutcome.value)
    }

    @Test
    fun `setting a demo outcome updates the observable value`() {
        val settings = manager()

        settings.setDemoOutcome(DemoOutcome.EMPTY)

        assertEquals(DemoOutcome.EMPTY, settings.demoOutcome.value)
    }

    @Test
    fun `demo outcome persists through storage for a new manager`() {
        manager().setDemoOutcome(DemoOutcome.ERROR)

        assertEquals(DemoOutcome.ERROR, manager().demoOutcome.value)
    }

    @Test
    fun `animation scale defaults to one time when nothing is stored`() {
        assertEquals(AnimationScale.ONE_X, manager().animationScale.value)
    }

    @Test
    fun `animation scale persists through storage for a new manager`() {
        manager().setAnimationScale(AnimationScale.FIVE_X)

        assertEquals(AnimationScale.FIVE_X, manager().animationScale.value)
    }

    private fun manager(): StorageSampleSettingsManager =
        StorageSampleSettingsManager(storage = storage, scope = scope)
}

/** In-memory [Storage] mirroring SAGE's key-value semantics with reactive flows. */
private class FakeStorage : Storage {
    private val strings = mutableMapOf<String, MutableStateFlow<String?>>()
    private val ints = mutableMapOf<String, MutableStateFlow<Int?>>()

    override fun saveString(key: String, value: String) {
        flowFor(strings, key).value = value
    }

    override fun savedStringFlow(key: String): Flow<String?> = flowFor(strings, key)

    override fun saveInt(key: String, value: Int) {
        flowFor(ints, key).value = value
    }

    override fun savedIntFlow(key: String): Flow<Int?> = flowFor(ints, key)

    private fun <T> flowFor(flows: MutableMap<String, MutableStateFlow<T?>>, key: String) =
        flows.getOrPut(key) { MutableStateFlow(null) }
}
