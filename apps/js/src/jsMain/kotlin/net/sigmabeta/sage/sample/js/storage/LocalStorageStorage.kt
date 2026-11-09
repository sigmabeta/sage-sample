package net.sigmabeta.sage.sample.js.storage

import kotlinx.browser.window
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.sigmabeta.sage.storage.common.Storage
import org.w3c.dom.StorageEvent
import org.w3c.dom.events.Event

/**
 * Browser-backed [Storage] over `window.localStorage`.
 *
 * localStorage does not push change notifications, so this keeps a per-key [MutableStateFlow]
 * cache that mirrors it: writes update both, and the `storage` event (fired in *other* tabs of the
 * same origin) updates the cache so settings stay live across tabs.
 *
 * One singleton binding means every consumer of a key shares one flow.
 */
class LocalStorageStorage : Storage {
    private val stringFlows = mutableMapOf<String, MutableStateFlow<String?>>()
    private val intFlows = mutableMapOf<String, MutableStateFlow<Int?>>()

    init {
        window.addEventListener("storage", { event ->
            val storageEvent = event.unsafeCast<StorageEvent>()
            // StorageEvent also fires for sessionStorage; this app only uses localStorage.
            val area = storageEvent.storageArea
            if (area != null && area != window.localStorage) return@addEventListener

            val key = storageEvent.key
            if (key == null) {
                // A null key means the whole store was cleared (for example DevTools).
                stringFlows.forEach { (k, flow) -> flow.value = window.localStorage.getItem(k) }
                intFlows.forEach { (k, flow) ->
                    flow.value = window.localStorage.getItem(k)?.toIntOrNull()
                }
                return@addEventListener
            }
            stringFlows[key]?.value = storageEvent.newValue
            intFlows[key]?.value = storageEvent.newValue?.toIntOrNull()
        })
    }

    override fun saveString(key: String, value: String) {
        window.localStorage.setItem(key, value)
        stringFlow(key).value = value
    }

    override fun savedStringFlow(key: String): Flow<String?> = stringFlow(key).asStateFlow()

    override fun saveInt(key: String, value: Int) {
        window.localStorage.setItem(key, value.toString())
        intFlow(key).value = value
        stringFlows[key]?.value = value.toString()
    }

    override fun savedIntFlow(key: String): Flow<Int?> = intFlow(key).asStateFlow()

    private fun stringFlow(key: String): MutableStateFlow<String?> =
        stringFlows.getOrPut(key) { MutableStateFlow(window.localStorage.getItem(key)) }

    private fun intFlow(key: String): MutableStateFlow<Int?> =
        intFlows.getOrPut(key) { MutableStateFlow(window.localStorage.getItem(key)?.toIntOrNull()) }
}
