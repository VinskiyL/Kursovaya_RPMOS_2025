package ru.kafpin.repositories

import android.util.Log
import ru.kafpin.data.LibraryDatabase
import ru.kafpin.data.mappers.toBook
import ru.kafpin.data.mappers.toBookEntity
import ru.kafpin.api.models.Book

/**
 * Источник данных для локальной БД
 */
interface LocalBookDataSource {
    suspend fun getAllBooks(): List<Book>
    suspend fun saveBooks(books: List<Book>)
    suspend fun clearBooks()
    suspend fun getBooksCount(): Int
}

class LocalBookDataSourceImpl(
    private val database: LibraryDatabase
) : LocalBookDataSource {
    private val TAG = "LocalBookDataSource"

    override suspend fun getAllBooks(): List<Book> {
        Log.d(TAG, "Getting books from local DB...")
        val count = database.bookDao().getBooksCount()
        Log.d(TAG, "Total books in DB: $count")

        val entities = database.bookDao().getAllBooks()
        Log.d(TAG, "Retrieved ${entities.size} entities from DB")

        val books = entities.map { it.toBook() }
        Log.d(TAG, "Converted to ${books.size} books")

        return books
    }

    override suspend fun saveBooks(books: List<Book>) {
        Log.d(TAG, "Saving ${books.size} books to DB...")
        val entities = books.map { it.toBookEntity() }
        database.bookDao().insertBooks(entities)
        Log.d(TAG, "Books saved to DB successfully")
    }

    override suspend fun clearBooks() {
        Log.d(TAG, "Clearing all books from DB")
        database.bookDao().clearAllBooks()
    }

    override suspend fun getBooksCount(): Int {
        return database.bookDao().getBooksCount()
    }
}