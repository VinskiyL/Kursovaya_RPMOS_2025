package ru.kafpin.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ru.kafpin.repositories.BookRepository
import ru.kafpin.repositories.SmartSyncService
import ru.kafpin.utils.NetworkMonitor

class BookViewModel(
    private val repository: BookRepository,
    private val networkMonitor: NetworkMonitor,
    context: Context
) : ViewModel() {

    private val TAG = "BookViewModel"

    // ==================== STATE FLOWS ====================

    private val _allBooks = MutableStateFlow<List<ru.kafpin.api.models.Book>>(emptyList())
    val allBooks: StateFlow<List<ru.kafpin.api.models.Book>> = _allBooks.asStateFlow()

    private val _currentPageBooks = MutableStateFlow<List<ru.kafpin.api.models.Book>>(emptyList())
    val currentPageBooks: StateFlow<List<ru.kafpin.api.models.Book>> = _currentPageBooks.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isOnline = MutableStateFlow(false)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    // Поиск
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<ru.kafpin.api.models.Book>>(emptyList())
    val searchResults: StateFlow<List<ru.kafpin.api.models.Book>> = _searchResults.asStateFlow()

    // Пагинация
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
            Log.d(TAG, "🚀 Starting SmartSyncService from ViewModel")
            val smartSync = SmartSyncService(context)
            smartSync.syncIfNeeded()
        }

        networkMonitor.start()

        viewModelScope.launch {
            networkMonitor.isOnline.collect { online ->
                _isOnline.value = online
                Log.d(TAG, "Network status changed: ${if (online) "ONLINE" else "OFFLINE"}")

                if (online && _allBooks.value.isNotEmpty()) {
                    backgroundSync()
                }
            }
        }

        loadBooks()
    }

    // ==================== ОСНОВНЫЕ МЕТОДЫ ====================

    fun loadBooks() {
        if (_isLoading.value) return

        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                val books = repository.getBooks()
                _allBooks.value = books
                showPage(0)
            } catch (e: Exception) {
                Log.e(TAG, "loadBooks: Error", e)
                _errorMessage.value = "Ошибка загрузки: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

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
        _errorMessage.value = null  // Очищаем предыдущие ошибки

        viewModelScope.launch {
            try {
                if (_isOnline.value) {
                    // Есть интернет - пробуем синхронизировать
                    Log.d(TAG, "🔄 Online refresh - syncing with server...")
                    val success = repository.syncBooks()

                    if (success) {
                        Log.d(TAG, "✅ Sync successful")
                        val freshBooks = repository.getLocalBooks()
                        _allBooks.value = freshBooks
                        showPage(_paginationInfo.value.currentPage)
                        // Не устанавливаем ошибку при успехе
                    } else {
                        // Синхронизация не удалась
                        Log.w(TAG, "⚠️ Sync failed")
                        val localBooks = repository.getLocalBooks()
                        _allBooks.value = localBooks
                        showPage(_paginationInfo.value.currentPage)
                        _errorMessage.value = "Не удалось обновиться"
                    }
                } else {
                    // Нет интернета - просто показываем локальные книги
                    Log.d(TAG, "📴 Offline mode - showing local books")
                    val localBooks = repository.getLocalBooks()
                    _allBooks.value = localBooks
                    showPage(_paginationInfo.value.currentPage)
                    _errorMessage.value = "Офлайн режим - данные могут быть устаревшими"
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Refresh error", e)
                // При любой ошибке показываем локальные книги
                try {
                    val localBooks = repository.getLocalBooks()
                    _allBooks.value = localBooks
                    showPage(_paginationInfo.value.currentPage)
                } catch (dbError: Exception) {
                    // Если даже локальные не загрузились
                    _allBooks.value = emptyList()
                    showPage(0)
                }
                _errorMessage.value = "Ошибка: ${e.message}"
            } finally {
                _isLoading.value = false
                Log.d(TAG, "refresh completed, isLoading = false")
            }
        }
    }

    private fun backgroundSync() {
        viewModelScope.launch {
            try {
                repository.syncBooks()
                val books = repository.getLocalBooks()
                _allBooks.value = books
                showPage(_paginationInfo.value.currentPage)
            } catch (e: Exception) {
                // Фоновая ошибка - не показываем
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
                val results = repository.searchBooks(query)
                _searchResults.value = results

                // Показываем первую страницу результатов
                if (results.isNotEmpty()) {
                    showPage(0, results)
                } else {
                    _currentPageBooks.value = emptyList()
                    updatePaginationInfo(0, 0)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Search error", e)
            }
        }
    }

    // ==================== ПАГИНАЦИЯ ====================

    private val pageSize = 10

    private fun showPage(page: Int, booksList: List<ru.kafpin.api.models.Book>? = null) {
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
        networkMonitor.stop()
    }
}