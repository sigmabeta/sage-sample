package net.sigmabeta.sage.sample.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey

/**
 * One User's favorite: [contentId] is a Content detail id (see `ContentId`), kept as text so this
 * module needs no dependency on the models. A User favorites an id at most once, and a deleted User
 * takes their favorites with them. [createdAt] is epoch milliseconds.
 */
@Entity(
    tableName = "favorites",
    primaryKeys = ["user_id", "content_id"],
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class FavoriteEntity(
    @ColumnInfo(name = "user_id")
    val userId: String,
    @ColumnInfo(name = "content_id")
    val contentId: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
)
