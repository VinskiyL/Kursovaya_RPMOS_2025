package ru.kafpin.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import ru.kafpin.api.models.Author

interface AuthorService {
    @GET("authors")
    suspend fun getAllAuthors(): Response<List<Author>>

    @GET("authors/{id}")
    suspend fun getAuthorById(@Path("id") id: Long): Response<Author>
}