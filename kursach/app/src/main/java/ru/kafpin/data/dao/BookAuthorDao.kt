package ru.kafpin.data.dao

import androidx.room.*
import ru.kafpin.data.models.BookAuthorCrossRef

@Dao
interface BookAuthorDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookAuthorRelations(relations: List<BookAuthorCrossRef>)

    @Query("SELECT authorId FROM book_authors WHERE bookId = :bookId")
    suspend fun getAuthorIdsForBook(bookId: Long): List<Long>

    @Query("SELECT bookId FROM book_authors WHERE authorId = :authorId")
    suspend fun getBookIdsForAuthor(authorId: Long): List<Long>

    @Query("DELETE FROM book_authors WHERE bookId = :bookId")
    suspend fun deleteRelationsForBook(bookId: Long)

    @Query("DELETE FROM book_authors")
    suspend fun clearAllRelations()
}