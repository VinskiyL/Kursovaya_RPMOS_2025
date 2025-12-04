package ru.kafpin.data.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import ru.kafpin.data.models.BookGenreCrossRef

@Dao
interface BookGenreDao {
    @Query("SELECT * FROM bookgenrecrossref WHERE bookId = :bookId")
    suspend fun getGenresForBook(bookId: Long): List<BookGenreCrossRef>

    @Query("SELECT * FROM bookgenrecrossref WHERE genreId = :genreId")
    suspend fun getBooksForGenre(genreId: Long): List<BookGenreCrossRef>

    @Query("SELECT * FROM bookgenrecrossref")
    suspend fun getAllRelations(): List<BookGenreCrossRef>

    @Query("DELETE FROM bookgenrecrossref")
    suspend fun clearAllRelations()

    @Query("SELECT bookId FROM bookgenrecrossref WHERE genreId = :genreId")
    suspend fun getBookIdsForGenre(genreId: Long): List<Long>

    @Query("SELECT genreId FROM bookgenrecrossref WHERE bookId = :bookId")
    suspend fun getGenreIdsForBook(bookId: Long): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookGenreRelations(relations: List<BookGenreCrossRef>)

    @Query("SELECT * FROM bookgenrecrossref WHERE bookId IN (:bookIds)")
    suspend fun getGenreRelationsForBooks(bookIds: List<Long>): List<BookGenreCrossRef>

    @Query("UPDATE bookgenrecrossref SET lastSynced = :timestamp WHERE id = :relationId")
    suspend fun updateRelationLastSynced(relationId: Long, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT id FROM bookgenrecrossref")
    suspend fun getAllRelationIds(): List<Long>

    @Query("DELETE FROM bookgenrecrossref WHERE id IN (:ids)")
    suspend fun deleteRelationsByIds(ids: List<Long>)

    // НОВЫЕ FLOW МЕТОДЫ:
    @Query("SELECT * FROM bookgenrecrossref")
    fun getAllGenreRelationsFlow(): Flow<List<BookGenreCrossRef>>

    @Query("SELECT genreId FROM bookgenrecrossref WHERE bookId = :bookId")
    fun getGenreIdsForBookFlow(bookId: Long): Flow<List<Long>>

    @Query("SELECT * FROM bookgenrecrossref WHERE bookId = :bookId")
    fun getGenresForBookFlow(bookId: Long): Flow<List<BookGenreCrossRef>>

    @Query("SELECT * FROM bookgenrecrossref WHERE genreId = :genreId")
    fun getBooksForGenreFlow(genreId: Long): Flow<List<BookGenreCrossRef>>

    @Query("SELECT * FROM bookgenrecrossref")
    fun getAllRelationsFlow(): Flow<List<BookGenreCrossRef>>

    @Query("SELECT * FROM bookgenrecrossref WHERE bookId IN (:bookIds)")
    fun getGenreRelationsForBooksFlow(bookIds: List<Long>): Flow<List<BookGenreCrossRef>>
}