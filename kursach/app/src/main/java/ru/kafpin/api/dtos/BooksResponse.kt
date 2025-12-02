package ru.kafpin.api.dtos
import com.google.gson.annotations.SerializedName
import ru.kafpin.api.models.Book

data class BooksResponse(
    @SerializedName("content") val books: List<Book>
)