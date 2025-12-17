package ru.kafpin.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ru.kafpin.data.LibraryDatabase
import ru.kafpin.data.RepositoryProvider
import ru.kafpin.data.models.BookingWithDetails
import ru.kafpin.repositories.SyncResult

class BookingViewModel(private val context: Context) : ViewModel() {
    private val TAG = "BookingViewModel"

    private val database = LibraryDatabase.getInstance(context)
    private val authRepository = RepositoryProvider.getAuthRepository(database, context)
    private val bookingRepository = RepositoryProvider.getBookingRepository(
        database = database,
        authRepository = authRepository,
        context = context
    )

    private val networkMonitor = (context.applicationContext as ru.kafpin.MyApplication).networkMonitor

    private val _bookings = MutableStateFlow<List<BookingWithDetails>>(emptyList())
    val bookings: StateFlow<List<BookingWithDetails>> = _bookings.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.IDLE)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncErrors = MutableStateFlow<List<ru.kafpin.repositories.SyncResult.Error>>(emptyList())
    val syncErrors: StateFlow<List<ru.kafpin.repositories.SyncResult.Error>> = _syncErrors.asStateFlow()

    private val _isOnline = MutableStateFlow(false)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    sealed class SyncStatus {
        object IDLE : SyncStatus()
        object SYNCING : SyncStatus()
        object SUCCESS : SyncStatus()
        data class ERROR(val message: String) : SyncStatus()
    }

    init {
        viewModelScope.launch {
            networkMonitor.isOnline.collect { online ->
                _isOnline.value = online
                Log.d(TAG, "Сеть: ${if (online) "ONLINE" else "OFFLINE"}")
            }
        }

        loadBookings()
        setupSearch()
        observeSyncing()
        observeSyncErrors()
        observeBookingStatusChanges()
    }

    private fun observeBookingStatusChanges() {
        viewModelScope.launch {
            bookingRepository.getBookingsByUserFlow(authRepository.getCurrentUserId() ?: -1)
                .collect { bookings ->
                    _bookings.value = bookings
                }
        }
    }

    private fun observeSyncErrors() {
        viewModelScope.launch {
            bookingRepository.syncErrors.collect { errors ->
                _syncErrors.value = errors
                if (errors.isNotEmpty()) {
                    Log.d(TAG, "Получены ошибки синхронизации: ${errors.size}")
                }
            }
        }
    }

    fun clearSyncError(bookingId: Long) {
        bookingRepository.removeSyncError(bookingId)
    }

    fun clearAllSyncErrors() {
        bookingRepository.clearSyncErrors()
    }

    private fun observeSyncing() {
        viewModelScope.launch {
            bookingRepository.isSyncing.collect { syncing ->
                _isSyncing.value = syncing
            }
        }
    }

    fun canPerformAction(): Boolean {
        return !isLoading.value && !isSyncing.value
    }

    private fun loadBookings() {
        viewModelScope.launch {
            try {
                val userId = authRepository.getCurrentUserId()
                if (userId == null) {
                    _errorMessage.value = "Пользователь не авторизован"
                    _bookings.value = emptyList()
                    return@launch
                }

                bookingRepository.getBookingsByUserFlow(userId)
                    .catch { e ->
                        Log.e(TAG, "Ошибка в потоке броней", e)
                        _errorMessage.value = "Ошибка загрузки: ${e.message}"
                    }
                    .collect { bookingsWithDetails ->
                        _bookings.value = bookingsWithDetails
                        Log.d(TAG, "Загружено броней: ${bookingsWithDetails.size}")
                    }
            } catch (e: Exception) {
                _errorMessage.value = "Ошибка загрузки броней: ${e.message}"
                Log.e(TAG, "Ошибка в loadBookings()", e)
            }
        }
    }

    private fun setupSearch() {
        viewModelScope.launch {
            searchQuery
                .debounce(300)
                .distinctUntilChanged()
                .collect { query ->
                    onSearchQueryChanged(query)
                }
        }
    }

    private fun onSearchQueryChanged(query: String) {
        viewModelScope.launch {
            try {
                if (query.isBlank()) {
                    loadBookings()
                } else {
                    val results = bookingRepository.searchBookings(query)
                    _bookings.value = results
                    Log.d(TAG, "Найдено по запросу '$query': ${results.size} броней")
                }
            } catch (e: Exception) {
                _errorMessage.value = "Ошибка поиска: ${e.message}"
                Log.e(TAG, "Ошибка поиска", e)
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    suspend fun syncBookings(): Boolean {
        return try {
            _isLoading.value = true
            _syncStatus.value = SyncStatus.SYNCING

            clearAllSyncErrors()

            val results = bookingRepository.syncPendingBookings()

            val hasSuccess = results.any { it is SyncResult.Success }
            val errors = results.filterIsInstance<SyncResult.Error>()

            if (errors.isEmpty() && hasSuccess) {
                _syncStatus.value = SyncStatus.SUCCESS
                loadBookings()
                true
            } else if (errors.isNotEmpty()) {
                _syncStatus.value = SyncStatus.ERROR("Есть ошибки синхронизации")
                false
            } else {
                _syncStatus.value = SyncStatus.ERROR("Не удалось синхронизировать")
                false
            }
        } catch (e: Exception) {
            _errorMessage.value = "Ошибка синхронизации: ${e.message}"
            _syncStatus.value = SyncStatus.ERROR(e.message ?: "Неизвестная ошибка")
            Log.e(TAG, "Ошибка в syncBookings()", e)
            false
        } finally {
            _isLoading.value = false
        }
    }

    suspend fun deleteBooking(localId: Long): Boolean {
        return try {
            if (!canPerformAction()) {
                _errorMessage.value = "Дождитесь завершения текущей операции"
                return false
            }

            val booking = bookingRepository.getBookingWithDetails(localId)
            if (booking == null) {
                _errorMessage.value = "Бронь не найдена"
                return false
            }

            if (!booking.canDelete) {
                _errorMessage.value = "Эту бронь нельзя удалить"
                return false
            }

            when (booking.booking.status) {
                ru.kafpin.data.models.BookingStatus.PENDING -> {
                    bookingRepository.deleteLocalBooking(localId)
                    true
                }
                ru.kafpin.data.models.BookingStatus.CONFIRMED,
                ru.kafpin.data.models.BookingStatus.RETURNED -> {
                    if (!_isOnline.value) {
                        _errorMessage.value = "Нет интернета. Удаление невозможно."
                        return false
                    }

                    if (!authRepository.hasValidTokenForApi()) {
                        _errorMessage.value = "Требуется авторизация для удаления"
                        return false
                    }

                    bookingRepository.markForDeletion(localId)

                    bookingRepository.syncPendingBookings()
                    true
                }
                else -> {
                    _errorMessage.value = "Нельзя удалить бронь со статусом ${booking.statusText}"
                    false
                }
            }
        } catch (e: Exception) {
            _errorMessage.value = "Ошибка удаления: ${e.message}"
            Log.e(TAG, "Ошибка удаления брони", e)
            false
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}