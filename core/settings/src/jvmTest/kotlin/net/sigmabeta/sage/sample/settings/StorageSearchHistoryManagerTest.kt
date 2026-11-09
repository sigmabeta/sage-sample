package net.sigmabeta.sage.sample.settings

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import net.sigmabeta.sage.storage.common.Storage
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The Storage-backed recent-search list: writes go through one Storage key, so a manager built
 * after an app restart reads the same entries back.
 */
class StorageSearchHistoryManagerTest {
    private val scope = CoroutineScope(Dispatchers.Unconfined)
    private val storage = FakeHistoryStorage()

    @Test
    fun `a recorded query is readable by a new manager over the same storage`() {
        manager().record("chrono")

        assertEquals(listOf("chrono"), manager().history.value)
    }

    @Test
    fun `recording an existing query moves it to the front without duplicating`() {
        val history = manager()
        history.record("chrono")
        history.record("ff7")
        history.record("chrono")

        assertEquals(listOf("chrono", "ff7"), history.history.value)
        assertEquals(listOf("chrono", "ff7"), manager().history.value)
    }

    @Test
    fun `recording keeps only the newest entries`() {
        val history = manager()
        repeat(SEARCH_HISTORY_MAX_ENTRIES + 5) { index ->
            history.record("query-$index")
        }

        val entries = history.history.value
        assertEquals(SEARCH_HISTORY_MAX_ENTRIES, entries.size)
        assertEquals("query-${SEARCH_HISTORY_MAX_ENTRIES + 4}", entries.first())
        assertEquals("query-5", entries.last())
    }

    @Test
    fun `removing an entry updates the observable list and the stored value`() {
        val history = manager()
        history.record("chrono")
        history.record("ff7")

        history.remove("chrono")

        assertEquals(listOf("ff7"), history.history.value)
        assertEquals(listOf("ff7"), manager().history.value)
    }

    @Test
    fun `clearing drops every entry from the observable list and storage`() {
        val history = manager()
        history.record("chrono")
        history.record("ff7")

        history.clear()

        assertEquals(emptyList<String>(), history.history.value)
        assertEquals(emptyList<String>(), manager().history.value)
    }

    @Test
    fun `blank queries are ignored`() {
        val history = manager()
        history.record("   ")
        history.record("")

        assertEquals(emptyList<String>(), history.history.value)
        assertEquals(emptyList<String>(), manager().history.value)
    }

    @Test
    fun `a query is trimmed before it is recorded`() {
        val history = manager()

        history.record("  chrono  ")

        assertEquals(listOf("chrono"), history.history.value)
    }

    @Test
    fun `malformed stored entries are ignored and duplicates collapse`() {
        storage.saveString(SEARCH_HISTORY_STORAGE_KEY, "  \n chrono \n\nchrono\nff7\n")

        assertEquals(listOf("chrono", "ff7"), manager().history.value)
    }

    @Test
    fun `multi-line and backslash queries round-trip through storage`() {
        val history = manager()

        history.record("a\nb")
        history.record("c\\d")

        assertEquals(listOf("c\\d", "a\nb"), history.history.value)
        assertEquals(listOf("c\\d", "a\nb"), manager().history.value)
    }

    private fun manager(): StorageSearchHistoryManager =
        StorageSearchHistoryManager(storage = storage, scope = scope)
}

/** In-memory [Storage] mirroring SAGE's key-value semantics with reactive flows. */
private class FakeHistoryStorage : Storage {
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
