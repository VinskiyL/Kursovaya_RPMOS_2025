package ru.kafpin.api

import retrofit2.Response
import retrofit2.http.GET
import ru.kafpin.api.models.AuthorBook

interface AuthorBookService {
    @GET("authors-books")
    suspend fun getAllAuthorsBooks(): Response<List<AuthorBook>>
}