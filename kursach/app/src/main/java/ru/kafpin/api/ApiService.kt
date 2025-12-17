package ru.kafpin.api

import retrofit2.Response
import retrofit2.http.*
import ru.kafpin.api.models.*

interface ApiService {
    @GET("books")
    suspend fun getAllBooks(
        @Header("Authorization") token: String? = null
    ): Response<List<Book>>

    @GET("books/{id}")
    suspend fun getBookById(
        @Path("id") bookId: Long,
        @Header("Authorization") token: String? = null
    ): Response<Book>

    @GET("authors")
    suspend fun getAllAuthors(
        @Header("Authorization") token: String? = null
    ): Response<List<Author>>

    @GET("genres")
    suspend fun getAllGenres(
        @Header("Authorization") token: String? = null
    ): Response<List<Genre>>

    @GET("authors-books")
    suspend fun getAllAuthorBooks(
        @Header("Authorization") token: String? = null
    ): Response<List<AuthorBook>>

    @GET("books-genres")
    suspend fun getAllBookGenres(
        @Header("Authorization") token: String? = null
    ): Response<List<BookGenre>>

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @POST("auth/refresh")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): Response<AuthResponse>

    @POST("auth/logout")
    suspend fun logout(): Response<Void>

    @GET("auth/me")
    suspend fun getCurrentUser(@Header("Authorization") token: String): Response<UserResponse>

    @POST("bookings")
    suspend fun createBooking(@Body request: BookingCreateRequest,
                              @Header("Authorization") token: String? = null
    ): Response<BookingResponse>

    @GET("bookings/my")
    suspend fun getMyBookings(@Header("Authorization") token: String? = null): Response<List<BookingResponse>>

    @DELETE("bookings/{id}")
    suspend fun deleteBooking(@Path("id") id: Long,
                              @Header("Authorization") token: String? = null
    ): Response<Void>

    @PATCH("bookings/{id}/quantity")
    suspend fun updateBookingQuantity(
        @Path("id") id: Long,
        @Body request: BookingUpdateRequest,
        @Header("Authorization") authHeader: String
    ): Response<BookingResponse>
}