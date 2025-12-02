package ru.kafpin.data.dao

import androidx.room.*
import ru.kafpin.data.models.BookAuthorCrossRef

@Dao
interface BookAuthorDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookAuthorRelations(relations: List<BookAuthorCrossRef>)

    @Query("SELECT * FROM bookauthorcrossref WHERE bookId = :bookId")
    suspend fun getAuthorsForBook(bookId: Long): List<BookAuthorCrossRef>

    @Query("SELECT * FROM bookauthorcrossref WHERE authorId = :authorId")
    suspend fun getBooksForAuthor(authorId: Long): List<BookAuthorCrossRef>

    @Query("SELECT * FROM bookauthorcrossref")
    suspend fun getAllRelations(): List<BookAuthorCrossRef>

    @Query("DELETE FROM bookauthorcrossref")
    suspend fun clearAllRelations()

    @Query("SELECT authorId FROM bookauthorcrossref WHERE bookId = :bookId")
    suspend fun getAuthorIdsForBook(bookId: Long): List<Long>

    @Query("SELECT bookId FROM bookauthorcrossref WHERE authorId = :authorId")
    suspend fun getBookIdsForAuthor(authorId: Long): List<Long>
}