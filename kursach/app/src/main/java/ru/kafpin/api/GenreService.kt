package ru.kafpin.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import ru.kafpin.api.models.Genre

interface GenreService {
    @GET("genres")
    suspend fun getAllAuthors(): Response<List<Genre>>

    @GET("genres/{id}")
    suspend fun getGenreById(@Path("id") id: Long): Response<Genre>
}