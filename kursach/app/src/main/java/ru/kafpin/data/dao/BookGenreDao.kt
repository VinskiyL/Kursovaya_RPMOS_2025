package ru.kafpin.data.dao

import androidx.room.*
import ru.kafpin.data.models.BookGenreCrossRef

@Dao
interface BookGenreDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookGenreRelations(relations: List<BookGenreCrossRef>)

    @Query("SELECT * FROM bookgenrecrossref WHERE bookId = :bookId")
    suspend fun getGenresForBook(bookId: Long): List<BookGenreCrossRef>

    @Query("SELECT * FROM bookgenrecrossref WHERE genreId = :genreId")
    suspend fun getBooksForGenre(genreId: Long): List<BookGenreCrossRef>

    @Query("SELECT * FROM bookgenrecrossref")
    suspend fun getAllRelations(): List<BookGenreCrossRef>

    @Query("DELETE FROM bookgenrecrossref")
    suspend fun clearAllRelations()

    @Query("SELECT genreId FROM bookgenrecrossref WHERE bookId = :bookId")
    suspend fun getGenreIdsForBook(bookId: Long): List<Long>

    @Query("SELECT bookId FROM bookgenrecrossref WHERE genreId = :genreId")
    suspend fun getBookIdsForGenre(genreId: Long): List<Long>
}