package ru.kafpin.repositories

import ru.kafpin.api.ApiClient
import ru.kafpin.api.models.Book
import ru.kafpin.api.models.Author

class BookRepository {

    suspend fun getAllBooks(): List<Book> {
        return try {
            val response = ApiClient.bookService.getAllBooks()
            if (response.isSuccessful) {
                response.body() ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}