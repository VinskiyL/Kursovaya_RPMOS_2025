package ru.kafpin.data.dao

import androidx.room.*
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
}