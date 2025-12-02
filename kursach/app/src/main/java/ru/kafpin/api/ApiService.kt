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
    suspend fun getBookById(@Path("id") id: Long): Response<Book>

    @GET("authors")
    suspend fun getAllAuthors(): Response<List<Author>>

    @GET("authors/{id}")
    suspend fun getAuthorById(@Path("id") id: Long): Response<Author>

    @GET("genres")
    suspend fun getAllGenres(): Response<List<Genre>>

    @GET("genres/{id}")
    suspend fun getGenresById(@Path("id") id: Long): Response<Genre>

    @GET("authors-books")
    suspend fun getAllAuthorBooks(): Response<List<AuthorBook>>

    @GET("authors-books/{id}")
    suspend fun getAuthorBooksById(@Path("id") id: Long): Response<AuthorBook>

    @GET("books-genres")
    suspend fun getAllBookGenres(): Response<List<BookGenre>>

    @GET("books-genres/{id}")
    suspend fun getBookGenresById(@Path("id") id: Long): Response<BookGenre>
}