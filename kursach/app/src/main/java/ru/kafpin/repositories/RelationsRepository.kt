package ru.kafpin.repositories

import android.content.Context
import ru.kafpin.api.ApiClient
import ru.kafpin.data.LibraryDatabase
import ru.kafpin.data.mappers.toBookAuthorCrossRef
import ru.kafpin.data.mappers.toBookGenreCrossRef
import ru.kafpin.utils.NetworkMonitor

class RelationsRepository(context: Context) {
    private val database = LibraryDatabase.getInstance(context)
    private val apiService = ApiClient.apiService
    private val networkMonitor: NetworkMonitor

    init {
        val appContext = context.applicationContext
        networkMonitor = if (appContext is ru.kafpin.MyApplication) {
            appContext.networkMonitor
        } else {
            NetworkMonitor(context).apply { start() }
        }
    }

    suspend fun getAuthorsForBook(bookId: Long): List<Long> {
        return try {
            database.bookAuthorDao().getAuthorIdsForBook(bookId)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getGenresForBook(bookId: Long): List<Long> {
        return try {
            database.bookGenreDao().getGenreIdsForBook(bookId)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getBooksForAuthor(authorId: Long): List<Long> {
        return try {
            database.bookAuthorDao().getBookIdsForAuthor(authorId)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getBooksForGenre(genreId: Long): List<Long> {
        return try {
            database.bookGenreDao().getBookIdsForGenre(genreId)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private suspend fun getRemoteAuthorBooks(): List<ru.kafpin.api.models.AuthorBook> {
        val response = apiService.getAllAuthorBooks()
        if (response.isSuccessful) {
            return response.body() ?: emptyList()
        } else {
            throw Exception("Ошибка сервера: ${response.code()}")
        }
    }

    private suspend fun getRemoteBookGenres(): List<ru.kafpin.api.models.BookGenre> {
        val response = apiService.getAllBookGenres()
        if (response.isSuccessful) {
            return response.body() ?: emptyList()
        } else {
            throw Exception("Ошибка сервера: ${response.code()}")
        }
    }

    private suspend fun saveAuthorBooksToLocal(relations: List<ru.kafpin.api.models.AuthorBook>) {
        try {
            val entities = relations.map { it.toBookAuthorCrossRef() }
            database.bookAuthorDao().insertBookAuthorRelations(entities)
        } catch (e: Exception) {}
    }

    private suspend fun saveBookGenresToLocal(relations: List<ru.kafpin.api.models.BookGenre>) {
        try {
            val entities = relations.map { it.toBookGenreCrossRef() }
            database.bookGenreDao().insertBookGenreRelations(entities)
        } catch (e: Exception) {}
    }

    suspend fun syncRelations(): Boolean {
        if (!networkMonitor.isOnline.value) return false

        return try {
            val authorBooks = getRemoteAuthorBooks()
            val bookGenres = getRemoteBookGenres()

            saveAuthorBooksToLocal(authorBooks)
            saveBookGenresToLocal(bookGenres)
            true
        } catch (e: Exception) {
            false
        }
    }
}