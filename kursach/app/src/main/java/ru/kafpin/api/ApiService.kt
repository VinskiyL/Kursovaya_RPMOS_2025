package ru.kafpin.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import ru.kafpin.api.models.Author
import ru.kafpin.api.models.AuthorBook
import ru.kafpin.api.models.Book
import ru.kafpin.api.models.BookGenre
import ru.kafpin.api.models.Genre

interface ApiService {
    @GET("books")
    suspend fun getAllBooks(): Response<List<Book>>

    @GET("books/{id}")
    suspend fun getBookById(@Path("id") bookId: Long): Response<Book>

    @GET("authors")
    suspend fun getAllAuthors(): Response<List<Author>>

    @GET("genres")
    suspend fun getAllGenres(): Response<List<Genre>>

    @GET("authors-books")
    suspend fun getAllAuthorBooks(): Response<List<AuthorBook>>

    @GET("books-genres")
    suspend fun getAllBookGenres(): Response<List<BookGenre>>
}