package ru.kafpin.repositories

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import ru.kafpin.utils.NetworkMonitor

class SmartSyncService(context: Context) {
    private val TAG = "SmartSyncService"

    private val bookRepository = BookRepository(context)
    private val authorRepository = AuthorRepository(context)
    private val genreRepository = GenreRepository(context)
    private val relationsRepository = RelationsRepository(context)
    private val networkMonitor: NetworkMonitor

    init {
        val appContext = context.applicationContext
        networkMonitor = if (appContext is ru.kafpin.MyApplication) {
            appContext.networkMonitor
        } else {
            NetworkMonitor(context).apply { start() }
        }
        Log.d(TAG, "SmartSyncService initialized")
    }

    /**
     * Умная синхронизация всех данных
     * - Сначала основные сущности (книги, авторы, жанры)
     * - Потом связи между ними
     * - Только если есть сеть
     */
    suspend fun syncAllData(): SyncResult {
        Log.d(TAG, "syncAllData: Starting full sync...")

        if (!networkMonitor.isOnline.value) {
            Log.d(TAG, "syncAllData: No network available")
            return SyncResult.NoNetwork
        }

        return try {
            // Параллельно синхронизируем основные сущности
            val booksDeferred = CoroutineScope(Dispatchers.IO).async { bookRepository.syncBooks() }
            val authorsDeferred = CoroutineScope(Dispatchers.IO).async { authorRepository.syncAuthors() }
            val genresDeferred = CoroutineScope(Dispatchers.IO).async { genreRepository.syncGenres() }

            val booksSuccess = booksDeferred.await()
            val authorsSuccess = authorsDeferred.await()
            val genresSuccess = genresDeferred.await()

            Log.d(TAG, "syncAllData: Basic sync results - Books: $booksSuccess, Authors: $authorsSuccess, Genres: $genresSuccess")

            // Только если основные сущности загрузились, синхронизируем связи
            if (booksSuccess && authorsSuccess && genresSuccess) {
                Log.d(TAG, "syncAllData: Syncing relations...")
                val relationsSuccess = relationsRepository.syncRelations()

                if (relationsSuccess) {
                    Log.d(TAG, "syncAllData: Full sync completed successfully")
                    SyncResult.Success
                } else {
                    Log.w(TAG, "syncAllData: Relations sync failed")
                    SyncResult.PartialSuccess("Данные загружены, но связи не обновлены")
                }
            } else {
                Log.w(TAG, "syncAllData: Some basic syncs failed")
                SyncResult.PartialSuccess("Не все данные обновлены")
            }

        } catch (e: Exception) {
            Log.e(TAG, "syncAllData: Sync failed with error", e)
            SyncResult.Error(e.message ?: "Неизвестная ошибка")
        }
    }

    /**
     * Фоновая синхронизация при наличии сети
     * - Не блокирует UI
     * - Логирует ошибки, но не выбрасывает исключения
     */
    fun backgroundSync() {
        CoroutineScope(Dispatchers.IO).launch {
            if (networkMonitor.isOnline.value) {
                Log.d(TAG, "backgroundSync: Starting background sync...")
                try {
                    syncAllData()
                    Log.d(TAG, "backgroundSync: Completed successfully")
                } catch (e: Exception) {
                    Log.e(TAG, "backgroundSync: Failed silently", e)
                }
            } else {
                Log.d(TAG, "backgroundSync: No network, skipping")
            }
        }
    }

    /**
     * Проверка необходимости синхронизации
     * - Если локальных данных нет
     * - Если давно не синхронизировались
     */
    suspend fun syncIfNeeded() {
        Log.d(TAG, "syncIfNeeded: Checking if sync is needed...")

        val hasLocalBooks = bookRepository.getLocalBooks().isNotEmpty()
        val hasLocalAuthors = authorRepository.getLocalAuthors().isNotEmpty()
        val hasLocalGenres = genreRepository.getLocalGenres().isNotEmpty()

        Log.d(TAG, "syncIfNeeded: Local data - Books: $hasLocalBooks, Authors: $hasLocalAuthors, Genres: $hasLocalGenres")

        // Если каких-то данных нет, пробуем синхронизировать
        if (!hasLocalBooks || !hasLocalAuthors || !hasLocalGenres) {
            Log.d(TAG, "syncIfNeeded: Missing some local data, starting sync...")
            backgroundSync()
        } else {
            Log.d(TAG, "syncIfNeeded: All local data exists, sync not needed")
        }
    }

    sealed class SyncResult {
        object Success : SyncResult()
        data class PartialSuccess(val message: String) : SyncResult()
        object NoNetwork : SyncResult()
        data class Error(val message: String) : SyncResult()
    }
}