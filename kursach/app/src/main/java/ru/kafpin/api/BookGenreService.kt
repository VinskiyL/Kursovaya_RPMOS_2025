package ru.kafpin.api

import retrofit2.Response
import retrofit2.http.GET
import ru.kafpin.api.models.BookGenre

interface BookGenreService {
    @GET("books-genres")
    suspend fun getAllBooksGenres(): Response<List<BookGenre>>
}