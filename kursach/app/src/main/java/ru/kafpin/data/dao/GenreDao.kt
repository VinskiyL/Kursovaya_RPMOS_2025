package ru.kafpin.data.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import ru.kafpin.data.models.GenreEntity

@Dao
interface GenreDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGenres(genres: List<GenreEntity>)

    @Query("SELECT * FROM genres")
    suspend fun getAllGenres(): List<GenreEntity>

    @Query("SELECT * FROM genres WHERE id = :genreId")
    suspend fun getGenreById(genreId: Long): GenreEntity?

    @Query("DELETE FROM genres")
    suspend fun clearAllGenres()

    @Query("SELECT * FROM genres WHERE LOWER(name) LIKE '%' || LOWER(:query) || '%'")
    suspend fun searchGenres(query: String): List<GenreEntity>

    @Query("SELECT * FROM genres WHERE id IN (:genreIds)")
    suspend fun getGenresByIds(genreIds: List<Long>): List<GenreEntity>

    @Query("SELECT id FROM genres")
    suspend fun getAllGenreIds(): List<Long>

    @Query("DELETE FROM genres WHERE id IN (:ids)")
    suspend fun deleteGenresByIds(ids: List<Long>)

    @Query("SELECT * FROM genres")
    fun getAllGenresFlow(): Flow<List<GenreEntity>>

    @Query("SELECT * FROM genres WHERE id IN (:genreIds)")
    fun getGenresByIdsFlow(genreIds: List<Long>): Flow<List<GenreEntity>>
}