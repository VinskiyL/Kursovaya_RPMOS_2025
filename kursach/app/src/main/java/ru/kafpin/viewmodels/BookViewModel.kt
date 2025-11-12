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

    private var currentPage = 0
    private val pageSize = 10

    // Флаг чтобы не синхронизировать слишком часто
    private var lastSyncTime = 0L
    private val SYNC_COOLDOWN = 5 * 60 * 1000 // 5 минут

    init {
        Log.d(TAG, "BookViewModel initialized")
        loadAllBooks()
        setupAutoSync()
    }

    /**
     * Настраивает автоматическую синхронизацию при появлении сети
     */
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

    /**
     * Автоматически синхронизирует если прошло достаточно времени
     */
    private fun autoSyncIfNeeded() {
        val now = System.currentTimeMillis()
        val timeSinceLastSync = now - lastSyncTime

        if (timeSinceLastSync > SYNC_COOLDOWN) {
            Log.d(TAG, "Auto-syncing... (last sync was ${timeSinceLastSync/1000}s ago)")
            syncWithServer()
        } else {
            Log.d(TAG, "Skipping auto-sync (last sync was ${timeSinceLastSync/1000}s ago)")
        }
    }

    // СУЩЕСТВУЮЩИЕ МЕТОДЫ (без изменений)
    fun loadAllBooks() {
        if (_isLoading.value) return

        Log.d(TAG, "loadAllBooks: Starting load...")
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                Log.d(TAG, "loadAllBooks: Calling repository...")
                val books = repository.getAllBooks()
                Log.d(TAG, "loadAllBooks: Success! Received ${books.size} books")
                _allBooks.value = books
                lastSyncTime = System.currentTimeMillis()
                showPage(0)
            } catch (e: Exception) {
                Log.e(TAG, "loadAllBooks: Error - ${e.message}", e)
                _errorMessage.value = "Ошибка загрузки: ${e.message}"
                if (_allBooks.value.isEmpty()) {
                    _currentBooks.value = emptyList()
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refresh() {
        Log.d(TAG, "refresh: Manual refresh called")

        // Сразу сбрасываем ошибку
        _errorMessage.value = null

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
                // ВАЖНО: всегда завершаем загрузку, даже при ошибке
                _isLoading.value = false
                Log.d(TAG, "refresh: Finished refresh")
            }
        }
    }

    /**
     * Синхронизация с сервером (теперь используется автоматически)
     */
    private fun syncWithServer() {
        if (_isLoading.value) return

        Log.d(TAG, "syncWithServer: Starting sync...")
        _isLoading.value = true

        viewModelScope.launch {
            try {
                Log.d(TAG, "syncWithServer: Calling repository sync...")
                val books = repository.syncWithServer()
                Log.d(TAG, "syncWithServer: Sync success! Received ${books.size} books")

                _allBooks.value = books
                lastSyncTime = System.currentTimeMillis()
                showPage(currentPage)

            } catch (e: Exception) {
                Log.e(TAG, "syncWithServer: Error - ${e.message}", e)
                // НЕ показываем ошибку пользователю - это фоновая синхронизация
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ОСТАЛЬНЫЕ МЕТОДЫ БЕЗ ИЗМЕНЕНИЙ
    fun showPage(page: Int) {
        val allBooks = _allBooks.value
        Log.d(TAG, "showPage: page=$page, totalBooks=${allBooks.size}")

        val safePage = when {
            page < 0 -> 0
            page * pageSize >= allBooks.size -> maxOf(0, (allBooks.size - 1) / pageSize)
            else -> page
        }

        val start = safePage * pageSize
        val end = minOf(start + pageSize, allBooks.size)

        if (start < allBooks.size) {
            _currentBooks.value = allBooks.subList(start, end)
            currentPage = safePage
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