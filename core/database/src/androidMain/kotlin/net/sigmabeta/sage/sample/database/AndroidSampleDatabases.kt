package net.sigmabeta.sage.sample.database

import android.content.Context
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

/**
 * The Android builder for [SampleDatabase], the counterpart of the JVM's `SampleDatabases`: Room's
 * Android builder needs a [Context] to place the file, so it lives in `androidMain`.
 *
 * It uses the same bundled SQLite driver and IO query context as the JVM builders, and has no
 * destructive migration fallback either.
 */
object AndroidSampleDatabases {
    /** The file name under the app's database directory. */
    const val DEFAULT_NAME = "sage-sample.db"

    /** A durable database at `context.getDatabasePath(name)`, created on first open. */
    fun file(context: Context, name: String = DEFAULT_NAME): SampleDatabase {
        val appContext = context.applicationContext
        return Room.databaseBuilder<SampleDatabase>(
            context = appContext,
            name = appContext.getDatabasePath(name).absolutePath,
        )
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
    }
}
