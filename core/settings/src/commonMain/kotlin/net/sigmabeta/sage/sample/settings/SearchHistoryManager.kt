package net.sigmabeta.sage.sample.settings

import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.sigmabeta.sage.di.AppScope
import net.sigmabeta.sage.storage.common.Storage

/**
 * The app's recent-search list.
 *
 * Reads are observable state so the Search screen can render history while the field is blank;
 * [record], [remove] and [clear] write through to persistent [Storage], so the list survives an
 * app restart on both Android and desktop.
 */
interface SearchHistoryManager {
    /** Recorded queries, newest first, with duplicates and blank entries removed. */
    val history: StateFlow<List<String>>

    /** Moves [query] to the front of the list, dropping any earlier copy. */
    fun record(query: String)

    /** Drops [query] from the list. */
    fun remove(query: String)

    /** Drops every recorded query. */
    fun clear()
}

/** The one Storage key the recent-search list lives under. */
internal const val SEARCH_HISTORY_STORAGE_KEY = "search.history"

/** How many recent queries are kept; older entries fall off the end. */
internal const val SEARCH_HISTORY_MAX_ENTRIES = 20

/**
 * [SearchHistoryManager] over SAGE's key-value [Storage] seam.
 *
 * The list is held in memory once loaded and every mutation is read-modify-write through one
 * mutex, so two quick writes cannot lose an entry even though [Storage.saveString] itself is
 * asynchronous. Loading happens once, on the app scope, from the persisted string; a second
 * manager instance over the same storage (an app restart) therefore sees the same history.
 *
 * Entries are stored one per line. Queries are single-line by construction, but backslashes,
 * newlines and carriage returns are still escaped so a pasted multi-line query cannot corrupt the
 * list; malformed or blank entries are dropped on read.
 */
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class StorageSearchHistoryManager(
    private val storage: Storage,
    private val scope: CoroutineScope,
) : SearchHistoryManager {
    private val mutex = Mutex()
    private val loaded = CompletableDeferred<Unit>()
    private val mutableHistory = MutableStateFlow<List<String>>(emptyList())

    override val history: StateFlow<List<String>> = mutableHistory.asStateFlow()

    init {
        scope.launch {
            mutex.withLock {
                mutableHistory.value =
                    decodeSearchHistory(storage.savedStringFlow(SEARCH_HISTORY_STORAGE_KEY).first())
                loaded.complete(Unit)
            }
        }
    }

    override fun record(query: String) {
        val normalized = query.trim()
        if (normalized.isEmpty()) {
            return
        }

        mutate { current ->
            (listOf(normalized) + current.filterNot { it == normalized })
                .take(SEARCH_HISTORY_MAX_ENTRIES)
        }
    }

    override fun remove(query: String) {
        val normalized = query.trim()
        mutate { current -> current.filterNot { it == normalized } }
    }

    override fun clear() {
        mutate { emptyList() }
    }

    private fun mutate(transform: (List<String>) -> List<String>) {
        scope.launch {
            loaded.await()
            mutex.withLock {
                val updated = transform(mutableHistory.value)
                if (updated != mutableHistory.value) {
                    mutableHistory.value = updated
                    storage.saveString(SEARCH_HISTORY_STORAGE_KEY, encodeSearchHistory(updated))
                }
            }
        }
    }
}

/** Encodes one query per line, escaping the separators a query could theoretically contain. */
internal fun encodeSearchHistory(entries: List<String>): String = entries.joinToString("\n") { entry ->
    buildString {
        entry.forEach { char ->
            when (char) {
                '\\' -> append("\\\\")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                else -> append(char)
            }
        }
    }
}

/** Decodes [encodeSearchHistory]'s output, ignoring malformed, blank and duplicate entries. */
internal fun decodeSearchHistory(raw: String?): List<String> = raw.orEmpty()
    .split('\n')
    .map(::unescapeSearchEntry)
    .map(String::trim)
    .filter(String::isNotEmpty)
    .distinct()
    .take(SEARCH_HISTORY_MAX_ENTRIES)

private fun unescapeSearchEntry(encoded: String): String = buildString {
    var escaped = false
    encoded.forEach { char ->
        when {
            escaped -> {
                when (char) {
                    'n' -> append('\n')

                    'r' -> append('\r')

                    '\\' -> append('\\')

                    // Unknown escape: keep both characters rather than dropping one.
                    else -> append('\\').append(char)
                }
                escaped = false
            }

            char == '\\' -> escaped = true

            else -> append(char)
        }
    }
    // A trailing lone backslash is malformed but still recoverable.
    if (escaped) {
        append('\\')
    }
}
