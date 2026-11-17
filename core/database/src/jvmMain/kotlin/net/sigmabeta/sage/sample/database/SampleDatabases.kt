package net.sigmabeta.sage.sample.database

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

/** JVM builders for [SampleDatabase], both over the bundled SQLite driver. */
object SampleDatabases {
    /** A private database that lives until it is closed; for tests. */
    fun inMemory(): SampleDatabase =
        Room.inMemoryDatabaseBuilder<SampleDatabase>()
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
}
