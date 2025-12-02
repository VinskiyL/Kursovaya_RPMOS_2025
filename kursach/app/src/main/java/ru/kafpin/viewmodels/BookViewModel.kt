package ru.kafpin.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ru.kafpin.data.LibraryDatabase
import ru.kafpin.data.RepositoryProvider
import ru.kafpin.repositories.BookRepository

class BookViewModel(
    private val bookRepository: BookRepository,
    private val bookDetailsRepository: ru.kafpin.repositories.BookDetailsRepository,
    private val networkMonitor: ru.kafpin.utils.NetworkMonitor
) : ViewModel() {

    private val TAG = "BookViewModel"

    private val _allBooks = MutableStateFlow<List<ru.kafpin.data.models.BookWithDetails>>(emptyList())
    val allBooks: StateFlow<List<ru.kafpin.data.models.BookWithDetails>> = _allBooks.asStateFlow()

    private val _currentPageBooks = MutableStateFlow<List<ru.kafpin.data.models.BookWithDetails>>(emptyList())
    val currentPageBooks: StateFlow<List<ru.kafpin.data.models.BookWithDetails>> = _currentPageBooks.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isOnline = MutableStateFlow(false)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

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

        viewModelScope.launch {
            try {
                val books = bookDetailsRepository.getAllBooksWithDetails()
                if (books.isEmpty() && networkMonitor.isOnline.value) {
                    Log.d(TAG, "📱 No books found, performing initial sync...")
                    val syncSuccess = bookRepository.syncBooks()

                    if (syncSuccess) {
                        val freshBooks = bookDetailsRepository.getAllBooksWithDetails()
                        _allBooks.value = freshBooks
                        showPage(0)
                        Log.d(TAG, "✅ Initial sync successful, loaded ${freshBooks.size} books")
                    } else {
                        Log.w(TAG, "⚠️ Initial sync failed")
                        _errorMessage.value = "Не удалось загрузить данные"
                    }
                } else {
                    Log.d(TAG, "📚 Found ${books.size} books, no sync needed")
                    _allBooks.value = books
                    showPage(0)
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error during initial load", e)
                _errorMessage.value = "Ошибка инициализации: ${e.message}"
            }
        }

        viewModelScope.launch {
            networkMonitor.isOnline.collect { online ->
                _isOnline.value = online
                Log.d(TAG, "Network status changed: ${if (online) "ONLINE" else "OFFLINE"}")
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
                        val freshBooks = bookDetailsRepository.getAllBooksWithDetails()
                        _allBooks.value = freshBooks
                        showPage(_paginationInfo.value.currentPage)
                        Log.d(TAG, "✅ Updated books list with details (${freshBooks.size} books)")
                    } else {
                        Log.w(TAG, "⚠️ Sync failed")
                        _errorMessage.value = "Не удалось обновиться"
                    }
                } else {
                    Log.d(TAG, "📴 Offline mode - reloading local books")
                    _errorMessage.value = "Офлайн режим - данные могут быть устаревшими"
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Refresh error", e)
                _errorMessage.value = "Ошибка обновления: ${e.message}"
            } finally {
                _isLoading.value = false
                Log.d(TAG, "refresh completed, isLoading = false")
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
            _allBooks.value
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