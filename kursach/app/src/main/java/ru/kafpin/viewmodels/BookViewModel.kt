package ru.kafpin.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.LiveData
import kotlinx.coroutines.launch
import ru.kafpin.api.models.Book
import ru.kafpin.repositories.BookRepository

class BookViewModel : ViewModel() {
    private val repository = BookRepository()

    private val _allBooks = MutableLiveData<List<Book>>()
    private val _currentBooks = MutableLiveData<List<Book>>()
    val currentBooks: LiveData<List<Book>> = _currentBooks

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>(null)
    val errorMessage: LiveData<String?> = _errorMessage

    private var currentPage = 0
    private val pageSize = 10

    init {
        loadAllBooks()
    }

    fun loadAllBooks() {
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                val books = repository.getAllBooks()
                _allBooks.value = books
                showPage(0)

                if (books.isEmpty()) {
                    _errorMessage.value = "Книги не найдены"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Ошибка загрузки: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun showPage(page: Int) {
        val allBooks = _allBooks.value ?: return
        val start = page * pageSize
        val end = minOf(start + pageSize, allBooks.size)

        if (start < allBooks.size) {
            _currentBooks.value = allBooks.subList(start, end)
            currentPage = page
        }
    }

    fun nextPage() = showPage(currentPage + 1)
    fun previousPage() = showPage(currentPage - 1)

    fun refresh() {
        loadAllBooks()
    }

    val hasNextPage: Boolean
        get() {
            val allBooks = _allBooks.value ?: return false
            return (currentPage + 1) * pageSize < allBooks.size
        }

    val hasPreviousPage: Boolean
        get() = currentPage > 0

    val pageInfo: String
        get() {
            val allBooks = _allBooks.value ?: return "Страница 0 из 0"
            val totalPages = if (allBooks.isEmpty()) 0 else (allBooks.size + pageSize - 1) / pageSize
            return "Страница ${currentPage + 1} из $totalPages"
        }

    val totalBooksCount: Int
        get() = _allBooks.value?.size ?: 0
}