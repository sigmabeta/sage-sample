package net.sigmabeta.sage.sample.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import net.sigmabeta.sage.coroutines.SageDispatchers
import net.sigmabeta.sage.storage.common.Storage

/** The app's single Preferences DataStore; created once per process by the property delegate. */
internal val Context.settingsDataStore by preferencesDataStore(name = "sage_sample_settings")

/**
 * Android implementation of SAGE's key-value [Storage] seam, backed by Preferences DataStore.
 *
 * Writes are dispatched to the disk dispatcher and reads come back as a flow, so the settings
 * manager can expose reactive state without blocking the UI thread.
 */
class PreferencesStorage(
    private val dataStore: DataStore<Preferences>,
    private val scope: CoroutineScope,
    private val dispatchers: SageDispatchers,
) : Storage {
    override fun saveString(key: String, value: String) {
        scope.launch(dispatchers.disk) {
            dataStore.edit { it[stringPreferencesKey(key)] = value }
        }
    }

    override fun savedStringFlow(key: String): Flow<String?> = dataStore.data
        .map { it[stringPreferencesKey(key)] }

    override fun saveInt(key: String, value: Int) {
        scope.launch(dispatchers.disk) {
            dataStore.edit { it[intPreferencesKey(key)] = value }
        }
    }

    override fun savedIntFlow(key: String): Flow<Int?> = dataStore.data
        .map { it[intPreferencesKey(key)] }
}
