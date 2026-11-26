package net.sigmabeta.sage.sample.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SessionDao {
    /** Stores a new Session. A reused token hash or an unknown User aborts, never replaces. */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(session: SessionEntity)

    @Query("SELECT * FROM sessions WHERE token_hash = :tokenHash")
    suspend fun findByTokenHash(tokenHash: String): SessionEntity?

    /** Every Session [userId] holds, oldest first. */
    @Query("SELECT * FROM sessions WHERE user_id = :userId ORDER BY created_at, token_hash")
    suspend fun listForUser(userId: String): List<SessionEntity>

    /** Ends one Session (Sign out); a hash that matches nothing is a no-op. */
    @Query("DELETE FROM sessions WHERE token_hash = :tokenHash")
    suspend fun deleteByTokenHash(tokenHash: String)
}
