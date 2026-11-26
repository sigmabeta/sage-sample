package net.sigmabeta.sage.sample.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One stored User: a stable id that user-scoped data keys on, a unique username, and the stored
 * password verifier.
 *
 * The verifier columns are opaque here. `:core:identity` derives them and owns their encoding,
 * so this table only records which KDF and how many iterations produced the hash; that lets the
 * parameters be raised later without invalidating existing rows.
 */
@Entity(
    tableName = "users",
    indices = [Index(value = ["username"], unique = true)],
)
data class UserEntity(
    @PrimaryKey
    val id: String,
    val username: String,
    @ColumnInfo(name = "password_hash")
    val passwordHash: String,
    val salt: String,
    @ColumnInfo(name = "kdf_algorithm")
    val kdfAlgorithm: String,
    @ColumnInfo(name = "kdf_iterations")
    val kdfIterations: Int,
)
