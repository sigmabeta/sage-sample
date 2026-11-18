package net.sigmabeta.sage.sample.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface FavoriteDao {
    /** Favorites [favorite]; one that is already stored is left as it was. An unknown User aborts. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun add(favorite: FavoriteEntity)

    /** Un-favorites; an id that was not a favorite is a no-op. */
    @Query("DELETE FROM favorites WHERE user_id = :userId AND content_id = :contentId")
    suspend fun remove(userId: String, contentId: String)

    /** Every favorite [userId] holds, oldest first. */
    @Query("SELECT * FROM favorites WHERE user_id = :userId ORDER BY created_at, content_id")
    suspend fun listForUser(userId: String): List<FavoriteEntity>
}
