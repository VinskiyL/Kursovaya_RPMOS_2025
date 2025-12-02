package ru.kafpin.repositories

import android.content.Context
import android.util.Log
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
        Log.d(TAG, "💾 saveBooksToLocal() called with ${books.size} books")
        try {
            database.bookDao().insertBooks(books.map { it.toBookEntity() })
            Log.d(TAG, "✅ Successfully saved ${books.size} books to DB")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error saving books to local DB", e)
            throw e
        }
    }

    private suspend fun saveAuthorsToLocal(authors: List<Author>) {
        Log.d(TAG, "💾 saveAuthorsToLocal() called with ${authors.size} authors")

        try {
            database.authorDao().insertAuthors(authors.map { it.toAuthorEntity() })
            Log.d(TAG, "✅ Successfully saved ${authors.size} authors to DB")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error saving authors to local DB", e)
            throw e
        }
    }

    private suspend fun saveGenresToLocal(genres: List<Genre>) {
        Log.d(TAG, "💾 saveGenresToLocal() called with ${genres.size} genres")
        try {
            database.genreDao().insertGenres(genres.map { it.toGenreEntity() })
            Log.d(TAG, "✅ Successfully saved ${genres.size} genres to DB")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error saving genres to local DB", e)
            throw e
        }
    }

    private suspend fun saveAuthorBooksToLocal(relations: List<AuthorBook>) {
        Log.d(TAG, "💾 saveAuthorBooksToLocal() called with ${relations.size} relations")
        try {
            database.bookAuthorDao().insertBookAuthorRelations(relations.map { it.toBookAuthorCrossRef() })
            Log.d(TAG, "✅ Successfully saved ${relations.size} author-book relations to DB")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error saving author-book relations to local DB", e)
            throw e
        }
    }

    private suspend fun saveBookGenresToLocal(relations: List<BookGenre>) {
        Log.d(TAG, "💾 saveBookGenresToLocal() called with ${relations.size} relations")
        try {
            database.bookGenreDao().insertBookGenreRelations(relations.map { it.toBookGenreCrossRef() })
            Log.d(TAG, "✅ Successfully saved ${relations.size} book-genre relations to DB")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error saving book-genre relations to local DB", e)
            throw e
        }
    }
}