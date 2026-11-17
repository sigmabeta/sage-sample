package net.sigmabeta.sage.sample.database

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

/**
 * JVM builders for [SampleDatabase], both over the bundled SQLite driver.
 *
 * Neither enables a destructive migration fallback: opening a file written by a different schema
 * version without a registered `Migration` fails instead of dropping the stored Users.
 */
object SampleDatabases {
    /** A private database that lives until it is closed; for tests. */
    fun inMemory(): SampleDatabase = Room.inMemoryDatabaseBuilder<SampleDatabase>().configured()

    /** A durable database at [path], created on first open. */
    fun file(path: String): SampleDatabase = Room.databaseBuilder<SampleDatabase>(name = path).configured()

    private fun RoomDatabase.Builder<SampleDatabase>.configured(): SampleDatabase =
        setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
}
