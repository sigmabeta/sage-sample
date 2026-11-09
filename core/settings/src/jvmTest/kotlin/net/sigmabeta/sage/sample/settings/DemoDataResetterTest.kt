package net.sigmabeta.sage.sample.settings

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import net.sigmabeta.sage.storage.common.Storage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The app-default demo reset: clearing history and restoring the demo preferences must survive an
 * app restart, so each assertion re-reads the state through a new manager over the same storage.
 */
class DemoDataResetterTest {
    private val scope = CoroutineScope(Dispatchers.Unconfined)
    private val storage = FakeResetterStorage()

    @Test
    fun `reset clears persisted searches and restores the demo defaults`() = runBlocking {
        val history = historyManager()
        val settings = settingsManager()
        history.record("chrono")
        settings.setDemoOutcome(DemoOutcome.ERROR)
        settings.setHomeShowsSameContent(false)

        DefaultDemoDataResetter(
            searchHistoryManager = history,
            settingsManager = settings,
        ).reset()

        assertEquals(emptyList<String>(), historyManager().history.value)
        val reloaded = settingsManager()
        assertEquals(DemoOutcome.CONTENT, reloaded.demoOutcome.value)
        assertTrue(reloaded.homeShowsSameContent.value)
    }

    private fun historyManager() = StorageSearchHistoryManager(storage = storage, scope = scope)

    private fun settingsManager() = StorageSampleSettingsManager(storage = storage, scope = scope)
}

/** In-memory [Storage] mirroring SAGE's key-value semantics with reactive flows. */
private class FakeResetterStorage : Storage {
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
