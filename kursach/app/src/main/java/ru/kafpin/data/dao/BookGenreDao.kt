package ru.kafpin.data.dao

import androidx.room.*
import ru.kafpin.data.models.BookGenreCrossRef

@Dao
interface BookGenreDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookGenreRelations(relations: List<BookGenreCrossRef>)

    @Query("SELECT genreId FROM book_genres WHERE bookId = :bookId")
    suspend fun getGenreIdsForBook(bookId: Long): List<Long>

    @Query("SELECT bookId FROM book_genres WHERE genreId = :genreId")
    suspend fun getBookIdsForGenre(genreId: Long): List<Long>

    @Query("DELETE FROM book_genres WHERE bookId = :bookId")
    suspend fun deleteRelationsForBook(bookId: Long)

    @Query("DELETE FROM book_genres")
    suspend fun clearAllRelations()
}