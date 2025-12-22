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

    @POST("orders")
    suspend fun createOrder(
        @Body request: OrderCreateRequest,
        @Header("Authorization") token: String? = null
    ): Response<OrderResponse>

    @GET("orders/my")
    suspend fun getMyOrders(
        @Header("Authorization") token: String? = null
    ): Response<List<OrderResponse>>

    @DELETE("orders/{id}")
    suspend fun deleteOrder(
        @Path("id") id: Long,
        @Header("Authorization") token: String? = null
    ): Response<Void>

    @PUT("orders/{id}")
    suspend fun updateOrder(
        @Path("id") id: Long,
        @Body request: OrderCreateRequest,
        @Header("Authorization") token: String? = null
    ): Response<OrderResponse>

    @GET("readers/me")
    suspend fun getMyProfile(
        @Header("Authorization") token: String? = null
    ): Response<ReaderProfileResponse>

    // Обновить профиль (аналог PUT /api/orders/{id})
    @PUT("readers/me")
    suspend fun updateProfile(
        @Body request: ReaderUpdateRequest,
        @Header("Authorization") token: String? = null
    ): Response<ReaderProfileResponse>

    // Изменить логин
    @PUT("readers/me/login")
    suspend fun changeLogin(
        @Body request: ChangeLoginRequest,
        @Header("Authorization") token: String? = null
    ): Response<Void>

    // Изменить пароль
    @PUT("readers/me/password")
    suspend fun changePassword(
        @Body request: ChangePasswordRequest,
        @Header("Authorization") token: String? = null
    ): Response<Void>

    @GET("comments")
    suspend fun getAllComments(
        @Header("Authorization") token: String? = null
    ): Response<List<CommentResponse>>

    @POST("comments")
    suspend fun createComment(
        @Body request: CommentCreateRequest,
        @Header("Authorization") token: String? = null
    ): Response<CommentResponse>

    @PUT("comments/{id}")
    suspend fun updateComment(
        @Path("id") id: Long,
        @Body request: CommentUpdateRequest,
        @Header("Authorization") token: String? = null
    ): Response<CommentResponse>

    @DELETE("comments/{id}")
    suspend fun deleteComment(
        @Path("id") id: Long,
        @Header("Authorization") token: String? = null
    ): Response<Void>
}