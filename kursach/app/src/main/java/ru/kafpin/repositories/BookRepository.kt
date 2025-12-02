package ru.kafpin.repositories

import android.content.Context
import android.util.Log
import kotlinx.coroutines.launch
import ru.kafpin.api.ApiClient
import ru.kafpin.api.models.Book
import ru.kafpin.data.LibraryDatabase
import ru.kafpin.data.mappers.toBook
import ru.kafpin.data.mappers.toBookEntity
import ru.kafpin.utils.NetworkMonitor

class BookRepository(context: Context) {
    private val TAG = "BookRepository"

    private val database = LibraryDatabase.getInstance(context)
    private val apiService = ApiClient.apiService
    private val networkMonitor: NetworkMonitor
    private val searchRepository = SearchRepository(context)

    init {
        Log.d(TAG, "Initializing BookRepository")
        val appContext = context.applicationContext
        networkMonitor = if (appContext is ru.kafpin.MyApplication) {
            Log.d(TAG, "Using shared NetworkMonitor from MyApplication")
            appContext.networkMonitor
        } else {
            Log.d(TAG, "Creating new NetworkMonitor")
            NetworkMonitor(context).apply { start() }
        }
    }

    suspend fun getLocalBooks(): List<Book> {
        Log.d(TAG, "📚 getLocalBooks() called")
        return try {
            val entities = database.bookDao().getAllBooks()
            Log.d(TAG, "📚 Found ${entities.size} books in local DB")

            // Логируем первые 3 книги
            entities.take(3).forEachIndexed { index, entity ->
                Log.d(TAG, "📚 Local book $index: ${entity.title} (ID: ${entity.id})")
            }

            val books = entities.map { it.toBook() }
            Log.d(TAG, "📚 Returning ${books.size} books")
            books
        } catch (e: Exception) {
            Log.e(TAG, "📚 Error getting local books", e)
            emptyList()
        }
    }

    private suspend fun saveBooksToLocal(books: List<Book>) {
        Log.d(TAG, "💾 saveBooksToLocal() called with ${books.size} books")
        try {
            database.bookDao().insertBooks(books.map { it.toBookEntity() })
            Log.d(TAG, "💾 Successfully saved ${books.size} books to DB")

            // Проверяем сохранение
            val count = database.bookDao().getBooksCount()
            Log.d(TAG, "💾 Now have $count books in DB")
        } catch (e: Exception) {
            Log.e(TAG, "💾 Error saving books to local DB", e)
        }
    }

    private suspend fun getRemoteBooks(): List<Book> {
        Log.d(TAG, "🌐 getRemoteBooks() called")
        val response = apiService.getAllBooks()
        if (response.isSuccessful) {
            val books = response.body() ?: emptyList()
            Log.d(TAG, "🌐 Got ${books.size} books from API")

            books.take(3).forEachIndexed { index, book ->
                Log.d(TAG, "🌐 API book $index: ${book.title} (ID: ${book.id})")
            }

            return books
        } else {
            Log.e(TAG, "🌐 Server error: ${response.code()}")
            throw Exception("Ошибка сервера: ${response.code()}")
        }
    }

    suspend fun syncBooks(): Boolean {
        Log.d(TAG, "🔄 syncBooks() called")

        if (!networkMonitor.isOnline.value) {
            Log.d(TAG, "🔄 No internet connection, skipping sync")
            return false
        }

        return try {
            val remoteBooks = getRemoteBooks()

            if (remoteBooks.isEmpty()) {
                Log.w(TAG, "🔄 No books received from API")
                return false
            }

            saveBooksToLocal(remoteBooks)
            Log.d(TAG, "🔄 Sync completed successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "🔄 Error during sync", e)
            false
        }
    }

    suspend fun getBooks(): List<Book> {
        Log.d(TAG, "📖 getBooks() called")

        // 1. Получаем локальные книги
        val localBooks = getLocalBooks()
        Log.d(TAG, "📖 Found ${localBooks.size} local books")

        // 2. Если есть интернет - запускаем фоновую синхронизацию
        if (networkMonitor.isOnline.value) {
            Log.d(TAG, "📖 Online, starting background sync")
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                try {
                    syncBooks()
                } catch (e: Exception) {
                    Log.e(TAG, "📖 Background sync failed", e)
                }
            }
        } else {
            Log.d(TAG, "📖 Offline mode, only local books")
        }

        return localBooks
    }

    suspend fun searchBooks(query: String): List<Book> {
        Log.d(TAG, "🔍 searchBooks() called with query: '$query'")
        return searchRepository.searchBooks(query)
    }

    suspend fun getBooksCount(): Int {
        return try {
            val count = database.bookDao().getBooksCount()
            Log.d(TAG, "📊 Books count in DB: $count")
            count
        } catch (e: Exception) {
            Log.e(TAG, "📊 Error getting books count", e)
            0
        }
    }
}