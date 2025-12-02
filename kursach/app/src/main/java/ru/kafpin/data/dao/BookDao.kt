package ru.kafpin.data.dao

import androidx.room.*
import ru.kafpin.data.models.BookEntity

@Dao
interface BookDao {

    // Базовые операции CRUD
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooks(books: List<BookEntity>)

    @Query("SELECT * FROM books")
    suspend fun getAllBooks(): List<BookEntity>

    @Query("SELECT * FROM books WHERE id = :bookId")
    suspend fun getBookById(bookId: Long): BookEntity?

    @Query("DELETE FROM books")
    suspend fun clearAllBooks()

    // Пагинация (для офлайн-режима)
    @Query("SELECT * FROM books LIMIT :limit OFFSET :offset")
    suspend fun getBooksPaginated(limit: Int, offset: Int): List<BookEntity>

    // Получить общее количество для пагинации
    @Query("SELECT COUNT(*) FROM books")
    suspend fun getBooksCount(): Int

    // BookDao.kt
    @Query("SELECT * FROM books WHERE LOWER(title) LIKE '%' || LOWER(:query) || '%'")
    suspend fun searchBooksByTitle(query: String): List<BookEntity>

    @Query("""
    SELECT DISTINCT b.* FROM books b
    JOIN bookauthorcrossref ba ON b.id = ba.bookId
    JOIN authors a ON ba.authorId = a.id
    WHERE LOWER(a.surname) LIKE '%' || LOWER(:query) || '%' 
       OR LOWER(a.name) LIKE '%' || LOWER(:query) || '%'
""")
    suspend fun searchBooksByAuthor(query: String): List<BookEntity>

    @Query("""
    SELECT DISTINCT b.* FROM books b
    JOIN bookgenrecrossref bg ON b.id = bg.bookId
    JOIN genres g ON bg.genreId = g.id
    WHERE LOWER(g.name) LIKE '%' || LOWER(:query) || '%'
""")
    suspend fun searchBooksByGenre(query: String): List<BookEntity>
}