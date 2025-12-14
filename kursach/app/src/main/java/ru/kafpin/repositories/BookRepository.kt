package ru.kafpin.repositories

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import retrofit2.Response
import ru.kafpin.api.ApiClient
import ru.kafpin.api.models.*
import ru.kafpin.data.LibraryDatabase
import ru.kafpin.data.RepositoryProvider
import ru.kafpin.data.mappers.*

class BookRepository(context: Context) {
    private val TAG = "BookRepository"

    private val database = LibraryDatabase.getInstance(context)
    private val apiService = ApiClient.apiService
    private val networkMonitor = (context.applicationContext as ru.kafpin.MyApplication).networkMonitor

    private val authRepository = RepositoryProvider.getAuthRepository(database, context)

    private var isTokenRefreshInProgress = false
    private var lastTokenRefreshTime: Long = 0
    private val TOKEN_REFRESH_COOLDOWN = 30_000L // 30 секунд

    // ==================== ОБЩИЕ МЕТОДЫ ====================

    suspend fun syncBooks(): Boolean {
        Log.d(TAG, "🔄 syncBooks() called")

        if (!networkMonitor.isOnline.value) {
            Log.d(TAG, "🔄 No internet connection, skipping sync")
            return false
        }

        return try {
            if (!authRepository.hasValidTokenForApi()) {
                Log.w(TAG, "⚠️ Нет валидного токена для синхронизации")
                return false
            }

            val booksSuccess = syncBooksOnly()
            val authorsSuccess = syncAuthorsOnly()
            val genresSuccess = syncGenresOnly()
            val relationsSuccess = syncRelationsOnly()

            Log.d(TAG, "🔄 Sync results - Books: $booksSuccess, Authors: $authorsSuccess, Genres: $genresSuccess, Relations: $relationsSuccess")

            booksSuccess && authorsSuccess && genresSuccess && relationsSuccess
        } catch (e: Exception) {
            Log.e(TAG, "🔄 Error during full sync", e)
            false
        }
    }

    // ==================== БЕЗОПАСНОЕ ОБНОВЛЕНИЕ ТОКЕНА ====================

    private suspend fun safeRefreshToken(): Boolean {
        val now = System.currentTimeMillis()

        if (isTokenRefreshInProgress) {
            Log.d(TAG, "🔄 Уже обновляем токен, пропускаем...")
            return false
        }

        if (now - lastTokenRefreshTime < TOKEN_REFRESH_COOLDOWN) {
            Log.d(TAG, "🔄 Слишком частые попытки обновления, пропускаем...")
            return false
        }

        isTokenRefreshInProgress = true
        lastTokenRefreshTime = now

        return try {
            val result = authRepository.refreshTokenIfNeeded()
            Log.d(TAG, "🔄 Результат обновления токена: $result")
            result
        } finally {
            isTokenRefreshInProgress = false
        }
    }

    // ==================== ОБРАБОТКА ИСТЕЧЕНИЯ ТОКЕНА ====================

    private suspend fun <T> handleTokenExpiry(response: Response<T>, retryAction: suspend () -> Response<T>): T? {
        if (response.code() == 403) {
            Log.w(TAG, "⏰ Токен истёк (403), пробуем обновить...")

            if (safeRefreshToken()) {
                Log.d(TAG, "🔄 Токен обновлён, повторяем запрос...")
                val newResponse = retryAction()

                if (newResponse.isSuccessful) {
                    return newResponse.body()
                } else {
                    Log.e(TAG, "❌ Повторный запрос не удался: ${newResponse.code()}")
                    throw Exception("Не удалось выполнить запрос после обновления токена: ${newResponse.code()}")
                }
            } else {
                Log.e(TAG, "❌ Не удалось обновить токен")
                throw Exception("Не удалось обновить токен. Возможно, сессия истекла.")
            }
        }

        return null
    }

    // ==================== ЗАГРУЗКА КНИГ С СЕРВЕРА ====================

    private suspend fun getRemoteBooks(): List<Book> {
        Log.d(TAG, "🌐 getRemoteBooks() called")

        val token = authRepository.getValidAccessToken()?.let { "Bearer $it" }
        Log.d(TAG, "📎 Токен для запроса книг: ${token?.take(20)}...")

        val response = apiService.getAllBooks(token)

        val handledResponse = handleTokenExpiry(response) {
            val newToken = authRepository.getValidAccessToken()?.let { "Bearer $it" }
            apiService.getAllBooks(newToken)
        }

        if (handledResponse != null) {
            return handledResponse
        }

        if (response.isSuccessful) {
            val books = response.body() ?: emptyList()
            Log.d(TAG, "🌐 Получено ${books.size} книг с API")
            return books
        } else {
            Log.e(TAG, "🌐 Ошибка сервера: ${response.code()}")
            throw Exception("Ошибка сервера: ${response.code()}")
        }
    }

    // ==================== ЗАГРУЗКА АВТОРОВ С СЕРВЕРА ====================

    private suspend fun getRemoteAuthor(): List<Author> {
        Log.d(TAG, "🌐 getRemoteAuthor() called")

        val token = authRepository.getValidAccessToken()?.let { "Bearer $it" }
        Log.d(TAG, "📎 Токен для запроса авторов: ${token?.take(20)}...")

        val response = apiService.getAllAuthors(token)

        val handledResponse = handleTokenExpiry(response) {
            val newToken = authRepository.getValidAccessToken()?.let { "Bearer $it" }
            apiService.getAllAuthors(newToken)
        }

        if (handledResponse != null) {
            return handledResponse
        }

        if (response.isSuccessful) {
            val authors = response.body() ?: emptyList()
            Log.d(TAG, "🌐 Получено ${authors.size} авторов с API")
            return authors
        } else {
            Log.e(TAG, "🌐 Ошибка сервера: ${response.code()}")
            throw Exception("Ошибка сервера при загрузке авторов: ${response.code()}")
        }
    }

    // ==================== ЗАГРУЗКА СВЯЗЕЙ АВТОР-КНИГА ====================

    private suspend fun getRemoteAuthorBook(): List<AuthorBook> {
        Log.d(TAG, "🌐 getRemoteAuthorBook() called")

        val token = authRepository.getValidAccessToken()?.let { "Bearer $it" }
        Log.d(TAG, "📎 Токен для запроса связей автор-книга: ${token?.take(20)}...")

        val response = apiService.getAllAuthorBooks(token)

        val handledResponse = handleTokenExpiry(response) {
            val newToken = authRepository.getValidAccessToken()?.let { "Bearer $it" }
            apiService.getAllAuthorBooks(newToken)
        }

        if (handledResponse != null) {
            return handledResponse
        }

        if (response.isSuccessful) {
            val authorBooks = response.body() ?: emptyList()
            Log.d(TAG, "🌐 Получено ${authorBooks.size} связей автор-книга с API")
            return authorBooks
        } else {
            Log.e(TAG, "🌐 Ошибка сервера: ${response.code()}")
            throw Exception("Ошибка сервера при загрузке связей автор-книга: ${response.code()}")
        }
    }

    // ==================== ЗАГРУЗКА ЖАНРОВ С СЕРВЕРА ====================

    private suspend fun getRemoteGenres(): List<Genre> {
        Log.d(TAG, "🌐 getRemoteGenres() called")

        val token = authRepository.getValidAccessToken()?.let { "Bearer $it" }
        Log.d(TAG, "📎 Токен для запроса жанров: ${token?.take(20)}...")

        val response = apiService.getAllGenres(token)

        val handledResponse = handleTokenExpiry(response) {
            val newToken = authRepository.getValidAccessToken()?.let { "Bearer $it" }
            apiService.getAllGenres(newToken)
        }

        if (handledResponse != null) {
            return handledResponse
        }

        if (response.isSuccessful) {
            val genres = response.body() ?: emptyList()
            Log.d(TAG, "🌐 Получено ${genres.size} жанров с API")
            return genres
        } else {
            Log.e(TAG, "🌐 Ошибка сервера: ${response.code()}")
            throw Exception("Ошибка сервера при загрузке жанров: ${response.code()}")
        }
    }

    // ==================== ЗАГРУЗКА СВЯЗЕЙ КНИГА-ЖАНР ====================

    private suspend fun getRemoteBookGenres(): List<BookGenre> {
        Log.d(TAG, "🌐 getRemoteBookGenres() called")

        val token = authRepository.getValidAccessToken()?.let { "Bearer $it" }
        Log.d(TAG, "📎 Токен для запроса связей книга-жанр: ${token?.take(20)}...")

        val response = apiService.getAllBookGenres(token)

        val handledResponse = handleTokenExpiry(response) {
            val newToken = authRepository.getValidAccessToken()?.let { "Bearer $it" }
            apiService.getAllBookGenres(newToken)
        }

        if (handledResponse != null) {
            return handledResponse
        }

        if (response.isSuccessful) {
            val bookGenres = response.body() ?: emptyList()
            Log.d(TAG, "🌐 Получено ${bookGenres.size} связей книга-жанр с API")
            return bookGenres
        } else {
            Log.e(TAG, "🌐 Ошибка сервера: ${response.code()}")
            throw Exception("Ошибка сервера при загрузке связей книга-жанр: ${response.code()}")
        }
    }

    // ==================== ОТДЕЛЬНЫЕ МЕТОДЫ СИНХРОНИЗАЦИИ ====================

    private suspend fun syncBooksOnly(): Boolean {
        return try {
            Log.d(TAG, "📚 Синхронизация книг...")
            val remoteBooks = getRemoteBooks()

            if (remoteBooks.isEmpty()) {
                Log.w(TAG, "📚 Книги не получены с API")
                return false
            }

            saveBooksToLocal(remoteBooks)
            Log.d(TAG, "✅ Книги синхронизированы: ${remoteBooks.size} книг")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Ошибка синхронизации книг", e)
            false
        }
    }

    private suspend fun syncAuthorsOnly(): Boolean {
        return try {
            Log.d(TAG, "👤 Синхронизация авторов...")
            val remoteAuthor = getRemoteAuthor()

            if (remoteAuthor.isEmpty()) {
                Log.w(TAG, "📚 Авторы не получены с API")
                return false
            }

            saveAuthorsToLocal(remoteAuthor)
            Log.d(TAG, "✅ Авторы синхронизированы: ${remoteAuthor.size} авторов")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Ошибка синхронизации авторов", e)
            false
        }
    }

    private suspend fun syncGenresOnly(): Boolean {
        return try {
            Log.d(TAG, "👤 Синхронизация жанров...")
            val remoteGenres = getRemoteGenres()

            if (remoteGenres.isEmpty()) {
                Log.w(TAG, "📚 Жанры не получены с API")
                return false
            }

            saveGenresToLocal(remoteGenres)
            Log.d(TAG, "✅ Жанры синхронизированы: ${remoteGenres.size} жанров")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Ошибка синхронизации жанров", e)
            false
        }
    }

    private suspend fun syncRelationsOnly(): Boolean {
        return try {
            Log.d(TAG, "🔗 Синхронизация связей...")

            val remoteAuthorBookResponse = getRemoteAuthorBook()
            if (remoteAuthorBookResponse.isEmpty()) {
                Log.w(TAG, "📚 Связи автор-книга не получены с API")
                return false
            }

            saveAuthorBooksToLocal(remoteAuthorBookResponse)
            Log.d(TAG, "✅ Связи автор-книга синхронизированы: ${remoteAuthorBookResponse.size} связей")

            val remoteBookGenresResponse = getRemoteBookGenres()
            if (remoteBookGenresResponse.isEmpty()) {
                Log.w(TAG, "📚 Связи книга-жанр не получены с API")
                return false
            }

            saveBookGenresToLocal(remoteBookGenresResponse)
            Log.d(TAG, "✅ Связи книга-жанр синхронизированы: ${remoteBookGenresResponse.size} связей")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Ошибка синхронизации связей", e)
            false
        }
    }

    // ==================== СОХРАНЕНИЕ В БАЗУ ДАННЫХ ====================

    private suspend fun saveBooksToLocal(books: List<Book>) {
        try {
            database.withTransaction {
                Log.d(TAG, "💾 Сохранение книг в локальную БД: ${books.size} шт")

                val serverIds = books.map { it.id }
                val localIds = database.bookDao().getAllBookIds()
                val idsToDelete = localIds.filter { it !in serverIds }

                if (idsToDelete.isNotEmpty()) {
                    Log.d(TAG, "🗑️ Удаление книг: $idsToDelete")
                    database.bookDao().deleteBooksByIds(idsToDelete)
                }

                val entities = books.map { it.toBookEntity() }
                database.bookDao().insertBooks(entities)
                Log.d(TAG, "✅ Сохранено ${entities.size} книг")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Ошибка сохранения книг в локальную БД", e)
            throw e
        }
    }

    private suspend fun saveAuthorsToLocal(authors: List<Author>) {
        try {
            database.withTransaction {
                Log.d(TAG, "💾 Сохранение авторов в локальную БД: ${authors.size} шт")

                val serverIds = authors.map { it.id }
                val localIds = database.authorDao().getAllAuthorIds()
                val idsToDelete = localIds.filter { it !in serverIds }

                if (idsToDelete.isNotEmpty()) {
                    Log.d(TAG, "🗑️ Удаление авторов: $idsToDelete")
                    database.authorDao().deleteAuthorsByIds(idsToDelete)
                }

                val entities = authors.map { it.toAuthorEntity() }
                database.authorDao().insertAuthors(entities)
                Log.d(TAG, "✅ Сохранено ${entities.size} авторов")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Ошибка сохранения авторов в локальную БД", e)
            throw e
        }
    }

    private suspend fun saveGenresToLocal(genres: List<Genre>) {
        try {
            database.withTransaction {
                Log.d(TAG, "💾 Сохранение жанров в локальную БД: ${genres.size} шт")

                val serverIds = genres.map { it.id }
                val localIds = database.genreDao().getAllGenreIds()
                val idsToDelete = localIds.filter { it !in serverIds }

                if (idsToDelete.isNotEmpty()) {
                    Log.d(TAG, "🗑️ Удаление жанров: $idsToDelete")
                    database.genreDao().deleteGenresByIds(idsToDelete)
                }

                val entities = genres.map { it.toGenreEntity() }
                database.genreDao().insertGenres(entities)
                Log.d(TAG, "✅ Сохранено ${entities.size} жанров")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Ошибка сохранения жанров в локальную БД", e)
            throw e
        }
    }

    private suspend fun saveAuthorBooksToLocal(relations: List<AuthorBook>) {
        try {
            database.withTransaction {
                Log.d(TAG, "💾 Сохранение связей автор-книга: ${relations.size} шт")

                val serverIds = relations.map { it.id }
                val localIds = database.bookAuthorDao().getAllRelationIds()
                val idsToDelete = localIds.filter { it !in serverIds }

                if (idsToDelete.isNotEmpty()) {
                    Log.d(TAG, "🗑️ Удаление связей автор-книга: ${idsToDelete.size} шт")
                    database.bookAuthorDao().deleteRelationsByIds(idsToDelete)
                }

                val entities = relations.map { it.toBookAuthorCrossRef() }
                database.bookAuthorDao().insertBookAuthorRelations(entities)
                Log.d(TAG, "✅ Сохранено ${entities.size} связей автор-книга")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Ошибка сохранения связей автор-книга в локальную БД", e)
            throw e
        }
    }

    private suspend fun saveBookGenresToLocal(relations: List<BookGenre>) {
        try {
            database.withTransaction {
                Log.d(TAG, "💾 Сохранение связей книга-жанр: ${relations.size} шт")

                val serverIds = relations.map { it.id }
                val localIds = database.bookGenreDao().getAllRelationIds()
                val idsToDelete = localIds.filter { it !in serverIds }

                if (idsToDelete.isNotEmpty()) {
                    Log.d(TAG, "🗑️ Удаление связей книга-жанр: ${idsToDelete.size} шт")
                    database.bookGenreDao().deleteRelationsByIds(idsToDelete)
                }

                val entities = relations.map { it.toBookGenreCrossRef() }
                database.bookGenreDao().insertBookGenreRelations(entities)
                Log.d(TAG, "✅ Сохранено ${entities.size} связей книга-жанр")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Ошибка сохранения связей книга-жанр в локальную БД", e)
            throw e
        }
    }

    suspend fun syncSingleBook(bookId: Long): Boolean {
        Log.d(TAG, "🔄 syncSingleBook($bookId) called")

        if (!networkMonitor.isOnline.value) {
            Log.d(TAG, "📡 Нет интернета для синхронизации книги")
            return false
        }

        return try {
            val token = authRepository.getValidAccessToken()?.let { "Bearer $it" }
            val response = apiService.getBookById(bookId, token)

            if (response.code() == 403 && safeRefreshToken()) {
                val newToken = authRepository.getValidAccessToken()?.let { "Bearer $it" }
                val newResponse = apiService.getBookById(bookId, newToken)
                return processBookResponse(bookId, newResponse)
            }

            processBookResponse(bookId, response)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Ошибка синхронизации книги $bookId", e)
            false
        }
    }

    private suspend fun processBookResponse(bookId: Long, response: Response<Book>): Boolean {
        return when {
            response.isSuccessful && response.body() != null -> {
                val remoteBook = response.body()!!
                val bookEntity = remoteBook.toBookEntity()
                database.bookDao().insertBooks(listOf(bookEntity))
                Log.d(TAG, "✅ Книга синхронизирована: $bookId")
                true
            }
            response.code() == 404 -> {
                database.bookDao().deleteBooksByIds(listOf(bookId))
                Log.d(TAG, "🗑️ Книга $bookId удалена с сервера, удалена из локальной БД")
                true
            }
            else -> {
                Log.e(TAG, "❌ Ошибка синхронизации книги $bookId: ${response.code()}")
                false
            }
        }
    }
}