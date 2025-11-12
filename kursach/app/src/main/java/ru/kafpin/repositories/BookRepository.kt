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

    // СУЩЕСТВУЮЩИЙ МЕТОД (оставляем как есть)
    override suspend fun getAllBooks(): List<Book> {
        Log.d(TAG, "=== getAllBooks() called ===")

        try {
            Log.d(TAG, "1. Loading from API...")
            val remoteBooks = remoteDataSource.getAllBooks()
            Log.d(TAG, "API success! Books: ${remoteBooks.size}")

            // 2. СОХРАНЯЕМ В БД
            try {
                Log.d(TAG, "2. Saving to database...")
                val entities = remoteBooks.map { it.toBookEntity() }
                database.bookDao().insertBooks(entities)
                Log.d(TAG, "Database saved! Books: ${entities.size}")
            } catch (dbError: Exception) {
                Log.e(TAG, "Database error (but continuing): ${dbError.message}")
            }

            return remoteBooks

        } catch (apiError: Exception) {
            Log.e(TAG, "3. API failed: ${apiError.message}")

            // 4. ПРОБУЕМ БД КАК ЗАПАСНОЙ ВАРИАНТ
            try {
                Log.d(TAG, "4. Trying database as backup...")
                val localBooks = database.bookDao().getAllBooks().map { it.toBook() }

                if (localBooks.isNotEmpty()) {
                    Log.d(TAG, "Database backup success! Books: ${localBooks.size}")
                    return localBooks
                } else {
                    Log.d(TAG, "Database is empty")
                    throw Exception("Нет данных: ${apiError.message}")
                }
            } catch (dbError: Exception) {
                Log.e(TAG, "5. Database also failed: ${dbError.message}")
                throw Exception("Ошибка загрузки: ${apiError.message}")
            }
        }
    }

    /**
     * НОВЫЙ МЕТОД: Принудительная синхронизация с сервером
     * Обновляет БД даже если там уже есть данные
     * Используется когда сеть появляется во время работы приложения
     */
    suspend fun syncWithServer(): List<Book> {
        Log.d(TAG, "=== FORCED SYNC with server ===")

        try {
            Log.d(TAG, "Loading fresh data from API...")
            val remoteBooks = remoteDataSource.getAllBooks()
            Log.d(TAG, "API sync success! Books: ${remoteBooks.size}")

            // ОБНОВЛЯЕМ БД (даже если там уже есть данные)
            try {
                Log.d(TAG, "Updating database with fresh data...")
                val entities = remoteBooks.map { it.toBookEntity() }
                database.bookDao().insertBooks(entities) // REPLACE заменит старые данные
                Log.d(TAG, "Database updated! Books: ${entities.size}")
            } catch (dbError: Exception) {
                Log.e(TAG, "Database update error: ${dbError.message}")
                // НЕ ВЫБРАСЫВАЕМ ОШИБКУ - ВАЖНО ЧТО API РАБОТАЕТ
            }

            return remoteBooks

        } catch (apiError: Exception) {
            Log.e(TAG, "Sync failed: ${apiError.message}")
            throw Exception("Ошибка синхронизации: ${apiError.message}")
        }
    }
}