package net.sigmabeta.sage.sample.database

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * Opens a file written by schema version 1 (built from `schemas/.../1.json`, the committed
 * contract) with the current database class, so the registered migration really runs.
 */
class MigrationTest {
    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun `a version 1 file keeps its users and sessions and gains working favorites`() = runTest {
        val path = folder.root.resolve("v1.db").absolutePath
        writeVersion1File(path)

        val migrated = SampleDatabases.file(path)
        try {
            assertEquals("alice", migrated.userDao().findById("user-alice")?.username)
            assertEquals(
                listOf("token-phone"),
                migrated.sessionDao().listForUser("user-alice").map { it.tokenHash },
            )

            assertEquals(emptyList<FavoriteEntity>(), migrated.favoriteDao().listForUser("user-alice"))
            val favorite = FavoriteEntity(userId = "user-alice", contentId = "list-items:7", createdAt = 10L)
            migrated.favoriteDao().add(favorite)
            assertEquals(listOf(favorite), migrated.favoriteDao().listForUser("user-alice"))
        } finally {
            migrated.close()
        }

        // The migrated file reopens as version 2 without migrating again.
        val reopened = SampleDatabases.file(path)
        try {
            assertEquals(1, reopened.favoriteDao().listForUser("user-alice").size)
        } finally {
            reopened.close()
        }
    }

    /** The version 1 schema verbatim, with its identity hash and `user_version = 1`. */
    private fun writeVersion1File(path: String) {
        val connection = BundledSQLiteDriver().open(path)
        try {
            connection.execSQL(
                "CREATE TABLE IF NOT EXISTS `users` (`id` TEXT NOT NULL, `username` TEXT NOT NULL, " +
                    "`password_hash` TEXT NOT NULL, `salt` TEXT NOT NULL, `kdf_algorithm` TEXT NOT NULL, " +
                    "`kdf_iterations` INTEGER NOT NULL, PRIMARY KEY(`id`))",
            )
            connection.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_users_username` ON `users` (`username`)")
            connection.execSQL(
                "CREATE TABLE IF NOT EXISTS `sessions` (`token_hash` TEXT NOT NULL, `user_id` TEXT NOT NULL, " +
                    "`created_at` INTEGER NOT NULL, `expires_at` INTEGER NOT NULL, PRIMARY KEY(`token_hash`), " +
                    "FOREIGN KEY(`user_id`) REFERENCES `users`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            )
            connection.execSQL("CREATE INDEX IF NOT EXISTS `index_sessions_user_id` ON `sessions` (`user_id`)")
            connection.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
            connection.execSQL(
                "INSERT OR REPLACE INTO room_master_table (id,identity_hash) " +
                    "VALUES(42, '$VERSION_1_IDENTITY_HASH')",
            )
            connection.execSQL(
                "INSERT INTO users VALUES ('user-alice', 'alice', 'aGFzaA==', 'c2FsdA==', 'PBKDF2WithHmacSHA256', 210000)",
            )
            connection.execSQL("INSERT INTO sessions VALUES ('token-phone', 'user-alice', 1000, 5000)")
            connection.execSQL("PRAGMA user_version = 1")
        } finally {
            connection.close()
        }
    }

    private companion object {
        /** `identityHash` in `schemas/.../1.json`. */
        const val VERSION_1_IDENTITY_HASH = "c0043f241d5d86986e20492198142b0e"
    }
}
