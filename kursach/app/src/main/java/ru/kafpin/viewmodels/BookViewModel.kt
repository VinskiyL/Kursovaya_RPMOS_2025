package ru.kafpin.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.kafpin.repositories.BookRepository

class BookViewModel(
    private val bookRepository: BookRepository,
    private val bookDetailsRepository: ru.kafpin.repositories.BookDetailsRepository,
    private val networkMonitor: ru.kafpin.utils.NetworkMonitor
) : ViewModel() {

    private val TAG = "BookViewModel"

    // ==================== НОВЫЕ FLOW ====================

    // Главный Flow для автообновления данных
    val allBooksWithDetails: StateFlow<List<ru.kafpin.data.models.BookWithDetails>> =
        bookDetailsRepository.getAllBooksWithDetailsFlow()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    // ==================== СУЩЕСТВУЮЩИЕ StateFlow ====================

    private val _currentPageBooks = MutableStateFlow<List<ru.kafpin.data.models.BookWithDetails>>(emptyList())
    val currentPageBooks: StateFlow<List<ru.kafpin.data.models.BookWithDetails>> = _currentPageBooks.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isOnline = MutableStateFlow(false)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _searchQuery = MutableStateFlow("")

    private val _searchResults = MutableStateFlow<List<ru.kafpin.data.models.BookWithDetails>>(emptyList())
    val searchResults: StateFlow<List<ru.kafpin.data.models.BookWithDetails>> = _searchResults.asStateFlow()

    private val _paginationInfo = MutableStateFlow(PaginationInfo())
    val paginationInfo: StateFlow<PaginationInfo> = _paginationInfo.asStateFlow()

    data class PaginationInfo(
        val currentPage: Int = 0,
        val totalPages: Int = 0,
        val hasNextPage: Boolean = false,
        val hasPreviousPage: Boolean = false,
        val pageInfoText: String = "Страница 1 из 1"
    )

    // ==================== INIT ====================

    init {
        Log.d(TAG, "BookViewModel initialized")

        // 1. Подписываемся на сеть
        viewModelScope.launch {
            networkMonitor.isOnline.collect { online ->
                _isOnline.value = online
                Log.d(TAG, "Network status changed: ${if (online) "ONLINE" else "OFFLINE"}")
            }
        }

        viewModelScope.launch {
            allBooksWithDetails.collect { books ->
                Log.d(TAG, "📚 Flow обновил данные: ${books.size} книг")

                if (_currentPageBooks.value.isEmpty() && books.isNotEmpty()) {
                    showPage(0)
                }

                if (_isLoading.value && books.isNotEmpty()) {
                    _isLoading.value = false
                }
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            delay(500) // Даём время Flow загрузить данные

            val currentBooks = allBooksWithDetails.value
            val isOnline = _isOnline.value

            if (currentBooks.isEmpty() && isOnline) {
                Log.d(TAG, "📱 БД пустая, делаем первоначальную синхронизацию...")
                try {
                    val syncSuccess = bookRepository.syncBooks()
                    if (!syncSuccess) {
                        Log.w(TAG, "⚠️ Initial sync failed")
                        withContext(Dispatchers.Main) {
                            _errorMessage.value = "Не удалось загрузить данные"
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Initial sync error", e)
                }
            }
        }
    }

    // ==================== ОСНОВНЫЕ МЕТОДЫ ====================

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    fun refresh() {
        Log.d(TAG, "refresh() called, isLoading=${_isLoading.value}, isOnline=${_isOnline.value}")

        if (_isLoading.value) {
            Log.d(TAG, "Already loading, skipping")
            return
        }

        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                if (_isOnline.value) {
                    Log.d(TAG, "🔄 Online refresh - syncing with server...")
                    val success = bookRepository.syncBooks()

                    if (success) {
                        Log.d(TAG, "✅ Sync successful")
                    } else {
                        Log.w(TAG, "⚠️ Sync failed")
                        _errorMessage.value = "Не удалось обновиться"
                    }
                } else {
                    Log.d(TAG, "📴 Offline mode - reloading local books")
                    _errorMessage.value = "Офлайн режим - данные могут быть устаревшими"
                }
            }catch (e: kotlinx.coroutines.CancellationException) {
                Log.d(TAG, "Refresh cancelled")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Refresh error", e)
                _errorMessage.value = "Ошибка обновления: ${e.message}"
            } finally {
                _isLoading.value = false
                Log.d(TAG, "refresh completed")
            }
        }
    }

    // ==================== ПОИСК ====================

    fun performSearch(query: String) {
        _searchQuery.value = query

        if (query.isBlank()) {
            _searchResults.value = emptyList()
            showPage(0)
            return
        }

        viewModelScope.launch {
            try {
                val results = bookDetailsRepository.searchBooksWithDetails(query)
                _searchResults.value = results

                if (results.isNotEmpty()) {
                    showPage(0)
                    Log.d(TAG, "🔍 Found ${results.size} books for query: '$query'")
                } else {
                    _currentPageBooks.value = emptyList()
                    updatePaginationInfo(0, 0)
                    Log.d(TAG, "🔍 No books found for query: '$query'")
                }
            } catch (e: Exception) {
                Log.e(TAG, "🔍 Search error", e)
            }
        }
    }

    // ==================== ПАГИНАЦИЯ ====================

    private val pageSize = 10

    private fun showPage(page: Int) {
        val booksToShow = if (_searchQuery.value.isBlank()) {
            allBooksWithDetails.value
        } else {
            _searchResults.value
        }

        Log.d(TAG, "showPage($page) called, booksToShow size: ${booksToShow.size}")

        if (booksToShow.isEmpty()) {
            Log.d(TAG, "No books to show")
            _currentPageBooks.value = emptyList()
            updatePaginationInfo(0, 0)
            return
        }

        val totalPages = maxOf(1, (booksToShow.size + pageSize - 1) / pageSize)
        val safePage = page.coerceIn(0, totalPages - 1)

        val start = safePage * pageSize
        val end = minOf(start + pageSize, booksToShow.size)

        Log.d(TAG, "Showing page $safePage/$totalPages, items $start-$end")

        _currentPageBooks.value = booksToShow.subList(start, end)
        updatePaginationInfo(safePage, totalPages)
    }

    private fun updatePaginationInfo(currentPage: Int, totalPages: Int) {
        _paginationInfo.value = PaginationInfo(
            currentPage = currentPage,
            totalPages = totalPages,
            hasNextPage = (currentPage + 1) < totalPages,
            hasPreviousPage = currentPage > 0,
            pageInfoText = "Страница ${currentPage + 1} из $totalPages"
        )
    }

    fun nextPage() {
        val current = _paginationInfo.value.currentPage
        if (_paginationInfo.value.hasNextPage) {
            showPage(current + 1)
        }
    }

    fun previousPage() {
        val current = _paginationInfo.value.currentPage
        if (_paginationInfo.value.hasPreviousPage) {
            showPage(current - 1)
        }
    }

    // ==================== CLEANUP ====================

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "BookViewModel cleared")
    }
}