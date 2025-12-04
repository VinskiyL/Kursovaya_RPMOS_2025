package ru.kafpin.data.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import ru.kafpin.data.models.BookAuthorCrossRef

@Dao
interface BookAuthorDao {
    @Query("SELECT * FROM bookauthorcrossref WHERE bookId = :bookId")
    suspend fun getAuthorsForBook(bookId: Long): List<BookAuthorCrossRef>

    @Query("SELECT * FROM bookauthorcrossref WHERE authorId = :authorId")
    suspend fun getBooksForAuthor(authorId: Long): List<BookAuthorCrossRef>

    @Query("SELECT * FROM bookauthorcrossref")
    suspend fun getAllRelations(): List<BookAuthorCrossRef>

    @Query("DELETE FROM bookauthorcrossref")
    suspend fun clearAllRelations()

    @Query("SELECT bookId FROM bookauthorcrossref WHERE authorId = :authorId")
    suspend fun getBookIdsForAuthor(authorId: Long): List<Long>

    @Query("SELECT authorId FROM bookauthorcrossref WHERE bookId = :bookId")
    suspend fun getAuthorIdsForBook(bookId: Long): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookAuthorRelations(relations: List<BookAuthorCrossRef>)

    @Query("SELECT * FROM bookauthorcrossref WHERE bookId IN (:bookIds)")
    suspend fun getAuthorRelationsForBooks(bookIds: List<Long>): List<BookAuthorCrossRef>

    @Query("UPDATE bookauthorcrossref SET lastSynced = :timestamp WHERE id = :relationId")
    suspend fun updateRelationLastSynced(relationId: Long, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT id FROM bookauthorcrossref")
    suspend fun getAllRelationIds(): List<Long>

    @Query("DELETE FROM bookauthorcrossref WHERE id IN (:ids)")
    suspend fun deleteRelationsByIds(ids: List<Long>)

    // FLOW МЕТОДЫ:
    @Query("SELECT * FROM bookauthorcrossref")
    fun getAllAuthorRelationsFlow(): Flow<List<BookAuthorCrossRef>>

    @Query("SELECT bookId FROM bookauthorcrossref WHERE authorId = :authorId")
    fun getBookIdsForAuthorFlow(authorId: Long): Flow<List<Long>>

    @Query("SELECT authorId FROM bookauthorcrossref WHERE bookId = :bookId")
    fun getAuthorIdsForBookFlow(bookId: Long): Flow<List<Long>>

    @Query("SELECT * FROM bookauthorcrossref WHERE bookId = :bookId")
    fun getAuthorsForBookFlow(bookId: Long): Flow<List<BookAuthorCrossRef>>

    @Query("SELECT * FROM bookauthorcrossref WHERE authorId = :authorId")
    fun getBooksForAuthorFlow(authorId: Long): Flow<List<BookAuthorCrossRef>>

    @Query("SELECT * FROM bookauthorcrossref")
    fun getAllRelationsFlow(): Flow<List<BookAuthorCrossRef>>

    @Query("SELECT * FROM bookauthorcrossref WHERE bookId IN (:bookIds)")
    fun getAuthorRelationsForBooksFlow(bookIds: List<Long>): Flow<List<BookAuthorCrossRef>>
}