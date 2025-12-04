package ru.kafpin.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ru.kafpin.data.LibraryDatabase
import ru.kafpin.data.RepositoryProvider
import ru.kafpin.repositories.BookRepository

class BookDetailsViewModel(context: Context, private val bookId: Long) : ViewModel() {
    private val TAG = "BookDetailsViewModel"

    private val database = LibraryDatabase.getInstance(context)

    private val bookRepository = BookRepository(context)
    private val bookDetailsRepository = RepositoryProvider.getBookDetailsRepository(database)

    private val _bookDetails = MutableStateFlow<ru.kafpin.data.models.BookWithDetails?>(null)
    val bookDetails: StateFlow<ru.kafpin.data.models.BookWithDetails?> = _bookDetails.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // ДОБАВЛЯЕМ для toast сообщений
    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    init {
        Log.d(TAG, "Initializing for bookId: $bookId")
        loadBookDetailsWithFlow()
    }

    private fun loadBookDetailsWithFlow() {
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            bookDetailsRepository.getBookWithDetailsFlow(bookId)
                .catch { e ->
                    Log.e(TAG, "Flow error", e)
                    _errorMessage.value = "Ошибка загрузки: ${e.message}"
                    _isLoading.value = false
                }
                .collect { bookWithDetails ->
                    _bookDetails.value = bookWithDetails
                    _isLoading.value = false

                    if (bookWithDetails == null) {
                        _errorMessage.value = "Книга не найдена"
                    } else {
                        // ПРОВЕРЯЕМ НУЖНО ЛИ АВТООБНОВИТЬ
                        val fifteenMinutes = 15 * 60 * 1000L
                        val needRefresh = System.currentTimeMillis() - bookWithDetails.book.lastSynced > fifteenMinutes

                        if (needRefresh) {
                            // ОБНОВЛЯЕМ В ФОНЕ (не показываем toast)
                            try {
                                bookRepository.syncSingleBook(bookId)
                                Log.d(TAG, "🔄 Auto-refresh book $bookId")
                            } catch (e: Exception) {
                                Log.e(TAG, "Auto-refresh error", e)
                            }
                        }
                    }
                }
        }
    }

    // РУЧНОЕ ОБНОВЛЕНИЕ (как в списке книг)
    fun refreshBook() {
        if (_isLoading.value) return

        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val success = bookRepository.syncSingleBook(bookId)

                if (success) {
                    _toastMessage.value = "✅ Книга обновлена"
                    Log.d(TAG, "✅ Manual refresh successful")
                } else {
                    _toastMessage.value = "❌ Не удалось обновить"
                    Log.w(TAG, "⚠️ Manual refresh failed")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Manual refresh error", e)
                _errorMessage.value = "Ошибка обновления: ${e.message}"
                _toastMessage.value = "⚠️ Ошибка обновления"
            } finally {
                // Даём время toast показаться
                delay(500)
                _isLoading.value = false
            }
        }
    }

    fun retry() {
        loadBookDetailsWithFlow()
    }

    fun clearToast() {
        _toastMessage.value = null
    }
}