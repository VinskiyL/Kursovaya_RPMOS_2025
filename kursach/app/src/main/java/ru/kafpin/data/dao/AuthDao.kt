package ru.kafpin.data.dao

import androidx.room.*
import ru.kafpin.data.models.AuthSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AuthDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: AuthSessionEntity)

    @Query("SELECT * FROM auth_sessions WHERE userId = :userId ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatestSession(userId: Long): AuthSessionEntity?

    @Query("SELECT * FROM auth_sessions WHERE accessExpiresAt > :currentTime ORDER BY createdAt DESC LIMIT 1")
    suspend fun getActiveSession(currentTime: Long = System.currentTimeMillis()): AuthSessionEntity?

    @Query("SELECT * FROM auth_sessions WHERE accessExpiresAt > :currentTime ORDER BY createdAt DESC LIMIT 1")
    fun getActiveSessionFlow(currentTime: Long = System.currentTimeMillis()): Flow<AuthSessionEntity?>

    @Query("DELETE FROM auth_sessions WHERE userId = :userId")
    suspend fun deleteSessionsForUser(userId: Long)

    @Query("DELETE FROM auth_sessions")
    suspend fun clearAllSessions()

}