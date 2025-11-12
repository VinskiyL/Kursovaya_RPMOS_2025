import android.util.Log
import ru.kafpin.api.models.Book
import ru.kafpin.repositories.BookDataSource

class RemoteBookDataSource : BookDataSource {
    private val TAG = "RemoteBookDataSource"

    override suspend fun getAllBooks(): List<Book> {
        Log.d(TAG, "Calling API...")
        val response = ru.kafpin.api.ApiClient.bookService.getAllBooks()
        Log.d(TAG, "API response code: ${response.code()}")

        if (response.isSuccessful) {
            val books = response.body() ?: emptyList()
            Log.d(TAG, "API success! Books received: ${books.size}")
            return books
        } else {
            Log.e(TAG, "API error: ${response.code()} - ${response.message()}")
            throw Exception("Ошибка сервера: ${response.code()}")
        }
    }
}