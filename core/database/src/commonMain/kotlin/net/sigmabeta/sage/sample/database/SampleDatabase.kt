package net.sigmabeta.sage.sample.database

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor

/**
 * The sample's Room KMP database: Users and their Sessions.
 *
 * Every schema version is exported to `schemas/` and committed. A version bump ships with a
 * `Migration`; there is deliberately no destructive fallback, because these rows are the durable
 * identity a User signs in with.
 */
@Database(
    entities = [UserEntity::class, SessionEntity::class],
    version = 1,
)
@ConstructedBy(SampleDatabaseConstructor::class)
abstract class SampleDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao

    abstract fun sessionDao(): SessionDao
}

/** Room's compiler generates each target's `actual` (KSP), so no hand-written actual exists. */
@Suppress("NO_ACTUAL_FOR_EXPECT", "KotlinNoActualForExpect")
expect object SampleDatabaseConstructor : RoomDatabaseConstructor<SampleDatabase> {
    override fun initialize(): SampleDatabase
}
