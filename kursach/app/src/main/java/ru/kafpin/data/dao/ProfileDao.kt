package ru.kafpin.data.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import ru.kafpin.data.models.ProfileEntity

@Dao
interface ProfileDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: ProfileEntity)

    @Update
    suspend fun update(profile: ProfileEntity)

    @Delete
    suspend fun delete(profile: ProfileEntity)

    @Query("DELETE FROM profiles WHERE userId = :userId")
    suspend fun deleteByUserId(userId: Long)

    @Query("SELECT * FROM profiles WHERE userId = :userId")
    suspend fun findByUserId(userId: Long): ProfileEntity?

    @Query("SELECT * FROM profiles WHERE userId = :userId")
    fun getByUserIdFlow(userId: Long): Flow<ProfileEntity?>

    @Query("DELETE FROM profiles")
    suspend fun deleteAll()
}