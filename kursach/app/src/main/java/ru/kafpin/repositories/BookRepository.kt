package ru.kafpin.repositories

import RemoteBookDataSource
import android.content.Context
import android.util.Log
import ru.kafpin.api.models.Book
import ru.kafpin.data.LibraryDatabase
import ru.kafpin.data.mappers.toBookEntity
import ru.kafpin.data.mappers.toBook

class BookRepository(
    private val context: Context
) : BookDataSource {

    private val TAG = "BookRepository"
    private val remoteDataSource = RemoteBookDataSource()
    private val database = LibraryDatabase.getInstance(context)

    // НОВЫЙ МЕТОД: Только локальные данные (мгновенно)
    suspend fun getLocalBooks(): List<Book> {
        Log.d(TAG, "=== getLocalBooks() called ===")
        return try {
            val localBooks = database.bookDao().getAllBooks().map { it.toBook() }
            Log.d(TAG, "Local books loaded: ${localBooks.size}")
            localBooks
        } catch (e: Exception) {
            Log.e(TAG, "Error loading local books: ${e.message}")
            emptyList()
        }
    }

    // ПЕРЕДЕЛАННЫЙ МЕТОД: Оффлайн-первая стратегия
    override suspend fun getAllBooks(): List<Book> {
        Log.d(TAG, "=== getAllBooks() called ===")

        // 1. СНАЧАЛА ПОЛУЧАЕМ ЛОКАЛЬНЫЕ ДАННЫЕ (для мгновенного показа)
        val localBooks = getLocalBooks()

        // 2. ПАРАЛЛЕЛЬНО ПЫТАЕМСЯ ОБНОВИТЬСЯ С СЕРВЕРА
        try {
            Log.d(TAG, "1. Trying API for updates...")
            val remoteBooks = remoteDataSource.getAllBooks()
            Log.d(TAG, "API success! Books: ${remoteBooks.size}")

            // 3. ОБНОВЛЯЕМ БАЗУ ДАННЫХ
            try {
                Log.d(TAG, "2. Updating database...")
                val entities = remoteBooks.map { it.toBookEntity() }
                database.bookDao().insertBooks(entities)
                Log.d(TAG, "Database updated! Books: ${entities.size}")
            } catch (dbError: Exception) {
                Log.e(TAG, "Database update error: ${dbError.message}")
            }

            return remoteBooks // Возвращаем свежие данные

        } catch (apiError: Exception) {
            Log.e(TAG, "3. API failed: ${apiError.message}")

            // 4. ЕСЛИ API НЕ ДОСТУПЕН - ВОЗВРАЩАЕМ ЛОКАЛЬНЫЕ ДАННЫЕ
            if (localBooks.isNotEmpty()) {
                Log.d(TAG, "4. Using local books as fallback: ${localBooks.size}")
                return localBooks
            } else {
                Log.e(TAG, "5. No local data available")
                throw Exception("Нет данных: ${apiError.message}")
            }
        }
    }
}