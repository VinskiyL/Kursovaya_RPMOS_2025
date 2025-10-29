package ru.kafpin.api
import ru.kafpin.api.models.Book
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface BookService {
    @GET("books") // Получаем ВСЕ книги
    suspend fun getAllBooks(): Response<List<Book>>

    @GET("books/{id}")
    suspend fun getBookById(@Path("id") id: Long): Response<Book>
}