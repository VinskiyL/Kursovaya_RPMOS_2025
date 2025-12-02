package ru.kafpin.data.dao

import androidx.room.*
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
}