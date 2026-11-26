package net.sigmabeta.sage.sample.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One stored Session: a client's proof that it is signed in as [userId].
 *
 * Only a hash of the client's token is stored, so a leaked database does not hand out live
 * Sessions; `:core:identity` produces the hash. A User may hold many rows (one per device or
 * browser). Times are epoch milliseconds.
 */
@Entity(
    tableName = "sessions",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["user_id"])],
)
data class SessionEntity(
    @PrimaryKey
    @ColumnInfo(name = "token_hash")
    val tokenHash: String,
    @ColumnInfo(name = "user_id")
    val userId: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "expires_at")
    val expiresAt: Long,
)
