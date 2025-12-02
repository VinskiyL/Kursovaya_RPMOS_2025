package ru.kafpin.repositories

import android.content.Context
import android.util.Log
import ru.kafpin.data.LibraryDatabase
import ru.kafpin.data.mappers.toBook

class SearchRepository(context: Context) {
    private val TAG = "SearchRepository"
    private val database = LibraryDatabase.getInstance(context)

    suspend fun searchBooks(query: String): List<ru.kafpin.api.models.Book> {
        return try {
            // Сначала ищем по названию
            val byTitle = database.bookDao().searchBooksByTitle(query).map { it.toBook() }

            if (byTitle.isNotEmpty()) {
                Log.d(TAG, "Found ${byTitle.size} books by title")
                return byTitle
            }

            // Если не нашли по названию, ищем по автору
            val byAuthor = database.bookDao().searchBooksByAuthor(query).map { it.toBook() }
            if (byAuthor.isNotEmpty()) {
                Log.d(TAG, "Found ${byAuthor.size} books by author")
                return byAuthor
            }

            // Если не нашли по автору, ищем по жанру
            val byGenre = database.bookDao().searchBooksByGenre(query).map { it.toBook() }
            if (byGenre.isNotEmpty()) {
                Log.d(TAG, "Found ${byGenre.size} books by genre")
                return byGenre
            }

            emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Search error", e)
            emptyList()
        }
    }
}