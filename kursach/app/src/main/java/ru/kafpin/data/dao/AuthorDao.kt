package ru.kafpin.data.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import ru.kafpin.data.models.AuthorEntity

@Dao
interface AuthorDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuthors(authors: List<AuthorEntity>)

    @Query("SELECT * FROM authors")
    suspend fun getAllAuthors(): List<AuthorEntity>

    @Query("SELECT * FROM authors WHERE id = :authorId")
    suspend fun getAuthorById(authorId: Long): AuthorEntity?

    @Query("DELETE FROM authors")
    suspend fun clearAllAuthors()

    @Query("SELECT * FROM authors WHERE LOWER(surname) LIKE '%' || LOWER(:query) || '%' OR LOWER(name) LIKE '%' || LOWER(:query) || '%'")
    suspend fun searchAuthors(query: String): List<AuthorEntity>

    @Query("SELECT * FROM authors WHERE id IN (:authorIds)")
    suspend fun getAuthorsByIds(authorIds: List<Long>): List<AuthorEntity>

    @Query("SELECT id FROM authors")
    suspend fun getAllAuthorIds(): List<Long>

    @Query("DELETE FROM authors WHERE id IN (:ids)")
    suspend fun deleteAuthorsByIds(ids: List<Long>)

    @Query("SELECT * FROM authors")
    fun getAllAuthorsFlow(): Flow<List<AuthorEntity>>

    @Query("SELECT * FROM authors WHERE id IN (:authorIds)")
    fun getAuthorsByIdsFlow(authorIds: List<Long>): Flow<List<AuthorEntity>>
}