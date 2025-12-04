package ru.kafpin.repositories

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import ru.kafpin.api.ApiClient
import ru.kafpin.api.models.*
import ru.kafpin.data.LibraryDatabase
import ru.kafpin.data.mappers.*

class BookRepository(context: Context) {
    private val TAG = "BookRepository"

    private val database = LibraryDatabase.getInstance(context)
    private val apiService = ApiClient.apiService
    private val networkMonitor = (context.applicationContext as ru.kafpin.MyApplication).networkMonitor

    // ==================== СИНХРОНИЗАЦИЯ КНИГ ====================

    suspend fun syncBooks(): Boolean {
        Log.d(TAG, "🔄 syncBooks() called")

        if (!networkMonitor.isOnline.value) {
            Log.d(TAG, "🔄 No internet connection, skipping sync")
            return false
        }

        return try {
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

    // ==================== ОТДЕЛЬНЫЕ МЕТОДЫ СИНХРОНИЗАЦИИ ====================

    private suspend fun syncBooksOnly(): Boolean {
        return try {
            Log.d(TAG, "📚 Syncing books...")
            val remoteBooks = getRemoteBooks()

            if (remoteBooks.isEmpty()) {
                Log.w(TAG, "📚 No books received from API")
                return false
            }

            saveBooksToLocal(remoteBooks)
            Log.d(TAG, "✅ Books sync successful: ${remoteBooks.size} books")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Books sync failed", e)
            false
        }
    }

    private suspend fun syncAuthorsOnly(): Boolean {
        return try {
            Log.d(TAG, "👤 Syncing authors...")
            val remoteAuthor = getRemoteAuthor()

            if (remoteAuthor.isEmpty()) {
                Log.w(TAG, "📚 No authors received from API")
                return false
            }

            saveAuthorsToLocal(remoteAuthor)
            Log.d(TAG, "✅ Authors sync successful: ${remoteAuthor.size} books")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Authors sync failed", e)
            false
        }
    }

    private suspend fun syncGenresOnly(): Boolean {
        return try {
            Log.d(TAG, "👤 Syncing genres...")
            val remoteGenres = getRemoteGenres()

            if (remoteGenres.isEmpty()) {
                Log.w(TAG, "📚 No genres received from API")
                return false
            }

            saveGenresToLocal(remoteGenres)
            Log.d(TAG, "✅ Genres sync successful: ${remoteGenres.size} books")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Genres sync failed", e)
            false
        }
    }

    private suspend fun syncRelationsOnly(): Boolean {
        return try {
            Log.d(TAG, "🔗 Syncing relations...")

            val remoteAuthorBookResponse = getRemoteAuthorBook()
            if (remoteAuthorBookResponse.isEmpty()) {
                Log.w(TAG, "📚 No author-book received from API")
                return false
            }

            saveAuthorBooksToLocal(remoteAuthorBookResponse)
            Log.d(TAG, "✅ author-book sync successful: ${remoteAuthorBookResponse.size} books")

            val remoteBookGenresResponse = getRemoteBookGenres()
            if (remoteBookGenresResponse.isEmpty()) {
                Log.w(TAG, "📚 No book-genre received from API")
                return false
            }

            saveBookGenresToLocal(remoteBookGenresResponse)
            Log.d(TAG, "✅ book-genre sync successful: ${remoteAuthorBookResponse.size} books")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Relations sync failed", e)
            false
        }
    }

    // ==================== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ====================

    private suspend fun getRemoteBooks(): List<Book> {
        Log.d(TAG, "🌐 getRemoteBooks() called")
        val response = apiService.getAllBooks()

        if (response.isSuccessful) {
            val books = response.body() ?: emptyList()
            Log.d(TAG, "🌐 Got ${books.size} books from API")
            return books
        } else {
            Log.e(TAG, "🌐 Server error: ${response.code()}")
            throw Exception("Ошибка сервера: ${response.code()}")
        }
    }

    private suspend fun getRemoteAuthor(): List<Author>{
        Log.d(TAG, "🌐 getRemoteAuthor() called")
        val response = apiService.getAllAuthors()

        if (response.isSuccessful) {
            val authors = response.body() ?: emptyList()
            Log.d(TAG, "🌐 Got ${authors.size} authors from API")
            return authors
        } else {
            Log.e(TAG, "🌐 Server error: ${response.code()}")
            throw Exception("Ошибка сервера: ${response.code()}")
        }    }

    private suspend fun getRemoteAuthorBook(): List<AuthorBook>{
        Log.d(TAG, "🌐 getRemoteAuthorBook() called")
        val response = apiService.getAllAuthorBooks()

        if (response.isSuccessful) {
            val authorBooks = response.body() ?: emptyList()
            Log.d(TAG, "🌐 Got ${authorBooks.size} author-book from API")
            return authorBooks
        } else {
            Log.e(TAG, "🌐 Server error: ${response.code()}")
            throw Exception("Ошибка сервера: ${response.code()}")
        }    }

    private suspend fun getRemoteGenres(): List<Genre>{
        Log.d(TAG, "🌐 getRemoteGenres() called")
        val response = apiService.getAllGenres()

        if (response.isSuccessful) {
            val genres = response.body() ?: emptyList()
            Log.d(TAG, "🌐 Got ${genres.size} genres from API")
            return genres
        } else {
            Log.e(TAG, "🌐 Server error: ${response.code()}")
            throw Exception("Ошибка сервера: ${response.code()}")
        }    }

    private suspend fun getRemoteBookGenres(): List<BookGenre>{
        Log.d(TAG, "🌐 getRemoteBookGenres() called")
        val response = apiService.getAllBookGenres()

        if (response.isSuccessful) {
            val bookGenres = response.body() ?: emptyList()
            Log.d(TAG, "🌐 Got ${bookGenres.size} genres from API")
            return bookGenres
        } else {
            Log.e(TAG, "🌐 Server error: ${response.code()}")
            throw Exception("Ошибка сервера: ${response.code()}")
        }    }

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
            Log.d(TAG, "📡 No internet for single book sync")
            return false
        }

        return try {
            val response = apiService.getBookById(bookId)

            if (response.isSuccessful && response.body() != null) {
                val remoteBook = response.body()!!
                val bookEntity = remoteBook.toBookEntity()
                database.bookDao().insertBooks(listOf(bookEntity))
                Log.d(TAG, "✅ Single book sync successful: $bookId")
                true
            } else {
                database.bookDao().deleteBooksByIds(listOf(bookId))
                Log.d(TAG, "🗑️ Book $bookId deleted from server, removed from local DB")
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error syncing book $bookId", e)
            false
        }
    }
}