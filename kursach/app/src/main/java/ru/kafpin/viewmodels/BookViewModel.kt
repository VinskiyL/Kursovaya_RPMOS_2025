package ru.kafpin.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ru.kafpin.api.models.Book
import ru.kafpin.repositories.BookRepository
import ru.kafpin.utils.NetworkMonitor

class BookViewModel(
    private val repository: BookRepository,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    private val TAG = "BookViewModel"

    // StateFlow
    private val _allBooks = MutableStateFlow<List<Book>>(emptyList())
    private val _currentBooks = MutableStateFlow<List<Book>>(emptyList())
    val currentBooks: StateFlow<List<Book>> = _currentBooks.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // StateFlow для пагинации
    private val _hasNextPage = MutableStateFlow(false)
    val hasNextPage: StateFlow<Boolean> = _hasNextPage.asStateFlow()

    private val _hasPreviousPage = MutableStateFlow(false)
    val hasPreviousPage: StateFlow<Boolean> = _hasPreviousPage.asStateFlow()

    private val _pageInfo = MutableStateFlow("Страница 0 из 0")
    val pageInfo: StateFlow<String> = _pageInfo.asStateFlow()

    // StateFlow для состояния сети
    val isOnline: StateFlow<Boolean> = networkMonitor.isOnline

    private var currentPage = 0
    private val pageSize = 10

    init {
        Log.d(TAG, "BookViewModel initialized")
        loadAllBooks()
        setupAutoSync()
    }

    private fun setupAutoSync() {
        viewModelScope.launch {
            networkMonitor.isOnline.collect { isOnline ->
                if (isOnline) {
                    Log.d(TAG, "Network is available - checking if we should sync...")
                    autoSyncIfNeeded()
                } else {
                    Log.d(TAG, "Network is unavailable")
                }
            }
        }
    }

    private var lastSyncTime = 0L
    private val SYNC_COOLDOWN = 5 * 60 * 1000 // 5 минут

    private fun autoSyncIfNeeded() {
        if (_isLoading.value) {
            Log.d(TAG, "autoSyncIfNeeded: Already loading, skipping")
            return
        }

        if (_allBooks.value.isEmpty()) {
            Log.d(TAG, "autoSyncIfNeeded: No local data, skipping auto-sync")
            return
        }

        val now = System.currentTimeMillis()
        val timeSinceLastSync = now - lastSyncTime

        if (timeSinceLastSync < SYNC_COOLDOWN) {
            Log.d(TAG, "autoSyncIfNeeded: Sync cooldown (${timeSinceLastSync/1000}s), skipping")
            return
        }

        Log.d(TAG, "autoSyncIfNeeded: Starting auto-sync...")
        lastSyncTime = now

        // Используем refresh, но не показываем ошибки пользователю
        viewModelScope.launch {
            try {
                val books = repository.getAllBooks()
                _allBooks.value = books
                showPage(currentPage)
                Log.d(TAG, "autoSyncIfNeeded: Sync successful")
            } catch (e: Exception) {
                Log.e(TAG, "autoSyncIfNeeded: Sync failed silently", e)
                // Не показываем ошибку пользователю - это фоновая синхронизация
            }
        }
    }

    fun loadAllBooks() {
        if (_isLoading.value) {
            Log.d(TAG, "loadAllBooks: Already loading, skipping")
            return
        }

        Log.d(TAG, "loadAllBooks: Starting load...")
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                Log.d(TAG, "loadAllBooks: Calling repository...")
                val books = repository.getAllBooks()
                Log.d(TAG, "loadAllBooks: Success! Received ${books.size} books")
                _allBooks.value = books
                showPage(0)
            } catch (e: Exception) {
                Log.e(TAG, "loadAllBooks: Error - ${e.message}", e)
                _errorMessage.value = "Ошибка загрузки: ${e.message}"
                if (_allBooks.value.isEmpty()) {
                    _currentBooks.value = emptyList()
                }
            } finally {
                _isLoading.value = false
                Log.d(TAG, "loadAllBooks: Finished loading")
            }
        }
    }

    fun refresh() {
        Log.d(TAG, "refresh: Manual refresh called")

        _errorMessage.value = null
        lastSyncTime = System.currentTimeMillis()

        viewModelScope.launch {
            try {
                Log.d(TAG, "refresh: Starting refresh...")
                val books = repository.getAllBooks()
                Log.d(TAG, "refresh: Success! Received ${books.size} books")
                _allBooks.value = books
                showPage(currentPage)
            } catch (e: Exception) {
                Log.e(TAG, "refresh: Error - ${e.message}", e)
                _errorMessage.value = "Ошибка обновления: ${e.message}"
            } finally {
                _isLoading.value = false
                Log.d(TAG, "refresh: Finished refresh")
            }
        }
    }

    fun showPage(page: Int) {
        val allBooks = _allBooks.value
        Log.d(TAG, "showPage: page=$page, totalBooks=${allBooks.size}")

        // ЗАЩИТА: если книг нет - показываем пустой список
        if (allBooks.isEmpty()) {
            _currentBooks.value = emptyList()
            currentPage = 0
            updatePaginationProperties()
            return
        }

        // ВЫЧИСЛЯЕМ БЕЗОПАСНУЮ СТРАНИЦУ
        val totalPages = maxOf(1, (allBooks.size + pageSize - 1) / pageSize)
        val safePage = page.coerceIn(0, totalPages - 1) // ← ГЛАВНОЕ ИСПРАВЛЕНИЕ!

        val start = safePage * pageSize
        val end = minOf(start + pageSize, allBooks.size)

        // ДОПОЛНИТЕЛЬНАЯ ПРОВЕРКА
        if (start >= 0 && start < allBooks.size) {
            _currentBooks.value = allBooks.subList(start, end)
            currentPage = safePage
            updatePaginationProperties()
            Log.d(TAG, "showPage: Showing page $safePage (books ${start}-${end})")
        } else {
            Log.e(TAG, "showPage: Invalid page range")
            // ФALLBACK: показываем первую страницу
            _currentBooks.value = allBooks.subList(0, minOf(pageSize, allBooks.size))
            currentPage = 0
            updatePaginationProperties()
        }
    }

    private fun updatePaginationProperties() {
        val allBooks = _allBooks.value
        _hasNextPage.value = (currentPage + 1) * pageSize < allBooks.size
        _hasPreviousPage.value = currentPage > 0

        val totalPages = if (allBooks.isEmpty()) 0 else (allBooks.size + pageSize - 1) / pageSize
        _pageInfo.value = "Страница ${currentPage + 1} из $totalPages"
    }

    fun nextPage() = showPage(currentPage + 1)
    fun previousPage() = showPage(currentPage - 1)

    val totalBooksCount: Int
        get() = _allBooks.value.size

    override fun onCleared() {
        super.onCleared()
        networkMonitor.stop()
    }
}