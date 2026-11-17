package net.sigmabeta.sage.sample.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface UserDao {
    /** Stores a new User. A taken username or id aborts with a constraint failure, never replaces. */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(user: UserEntity)

    @Query("SELECT * FROM users WHERE username = :username")
    suspend fun findByUsername(username: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun findById(id: String): UserEntity?
}
