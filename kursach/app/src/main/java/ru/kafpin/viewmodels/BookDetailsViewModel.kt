package ru.kafpin.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ru.kafpin.data.LibraryDatabase
import ru.kafpin.data.RepositoryProvider

class BookDetailsViewModel(context: Context, private val bookId: Long) : ViewModel() {
    private val TAG = "BookDetailsViewModel"

    private val database = LibraryDatabase.getInstance(context)
    private val repository = RepositoryProvider.getBookDetailsRepository(database)

    private val _bookDetails = MutableStateFlow<ru.kafpin.data.models.BookWithDetails?>(null)
    val bookDetails: StateFlow<ru.kafpin.data.models.BookWithDetails?> = _bookDetails.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        Log.d(TAG, "Initializing for bookId: $bookId")
        loadBookDetails()
    }

    private fun loadBookDetails() {
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                val details = repository.getBookWithDetails(bookId)
                _bookDetails.value = details

                if (details == null) {
                    _errorMessage.value = "Книга не найдена"
                }

                Log.d(TAG, "Book details loaded: ${details != null}")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading book details", e)
                _errorMessage.value = "Ошибка загрузки: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun retry() {
        loadBookDetails()
    }
}