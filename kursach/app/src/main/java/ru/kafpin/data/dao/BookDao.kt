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

    // Поиск и фильтрация (для офлайн-режима)
    @Query("SELECT * FROM books WHERE title LIKE '%' || :query || '%' OR authorsMark LIKE '%' || :query || '%'")
    suspend fun searchBooks(query: String): List<BookEntity>

    // Пагинация (для офлайн-режима)
    @Query("SELECT * FROM books LIMIT :limit OFFSET :offset")
    suspend fun getBooksPaginated(limit: Int, offset: Int): List<BookEntity>

    // Получить общее количество для пагинации
    @Query("SELECT COUNT(*) FROM books")
    suspend fun getBooksCount(): Int
}