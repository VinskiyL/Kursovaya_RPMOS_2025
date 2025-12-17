package ru.kafpin.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ru.kafpin.data.LibraryDatabase
import ru.kafpin.data.RepositoryProvider
import ru.kafpin.data.models.BookingStatus

class BookingDetailsViewModel(context: Context) : ViewModel() {
    private val TAG = "BookingDetailsViewModel"

    private val database = LibraryDatabase.getInstance(context)
    private val authRepository = RepositoryProvider.getAuthRepository(database, context)
    private val bookingRepository = RepositoryProvider.getBookingRepository(
        database = database,
        authRepository = authRepository,
        context = context
    )

    private val networkMonitor = (context.applicationContext as ru.kafpin.MyApplication).networkMonitor

    private val _bookingDetails = MutableStateFlow<ru.kafpin.data.models.BookingWithDetails?>(null)
    val bookingDetails: StateFlow<ru.kafpin.data.models.BookingWithDetails?> = _bookingDetails.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isOnline = MutableStateFlow(false)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    init {
        // Подписываемся на изменения состояния сети
        viewModelScope.launch {
            networkMonitor.isOnline.collect { online ->
                _isOnline.value = online
                Log.d(TAG, "Сеть: ${if (online) "ONLINE" else "OFFLINE"}")
            }
        }
    }

    suspend fun loadBookingDetails(bookingId: Long) {
        _isLoading.value = true
        _errorMessage.value = null

        try {
            val booking = bookingRepository.getBookingWithDetails(bookingId)
            _bookingDetails.value = booking
            if (booking == null) {
                _errorMessage.value = "Бронь не найдена"
            }
        } catch (e: Exception) {
            _errorMessage.value = "Ошибка загрузки: ${e.message}"
            Log.e(TAG, "Ошибка загрузки брони", e)
        } finally {
            _isLoading.value = false
        }
    }

    suspend fun updateBookingQuantity(bookingId: Long, newQuantity: Int): Boolean {
        return try {
            if (!_isOnline.value) {
                _errorMessage.value = "Нет интернета. Редактирование невозможно."
                return false
            }

            val booking = bookingRepository.getBookingWithDetails(bookingId)
            if (booking == null) {
                _errorMessage.value = "Бронь не найдена"
                return false
            }

            if (!booking.canEdit) {
                _errorMessage.value = "Эту бронь нельзя редактировать"
                return false
            }

            if (booking.booking.status == BookingStatus.CONFIRMED) {
                if (!authRepository.hasValidTokenForApi()) {
                    _errorMessage.value = "Требуется авторизация для редактирования"
                    return false
                }
            }

            var serverSuccess = true
            booking.booking.serverId?.let { serverId ->
                serverSuccess = bookingRepository.updateServerQuantity(serverId, newQuantity)
            }

            if (serverSuccess) {
                val localSuccess = bookingRepository.updateLocalQuantity(bookingId, newQuantity)
                if (localSuccess) {
                    loadBookingDetails(bookingId)
                    true
                } else {
                    _errorMessage.value = "Ошибка локального обновления"
                    false
                }
            } else {
                _errorMessage.value = "Ошибка обновления на сервере"
                false
            }
        } catch (e: Exception) {
            _errorMessage.value = "Ошибка обновления: ${e.message}"
            Log.e(TAG, "Ошибка обновления брони", e)
            false
        }
    }

    suspend fun deleteBooking(bookingId: Long): Boolean {
        return try {
            val booking = bookingRepository.getBookingWithDetails(bookingId)
            if (booking == null) {
                _errorMessage.value = "Бронь не найдена"
                return false
            }

            if (!booking.canDelete) {
                _errorMessage.value = "Эту бронь нельзя удалить"
                return false
            }

            when (booking.booking.status) {
                BookingStatus.PENDING -> {
                    bookingRepository.deleteLocalBooking(bookingId)
                    true
                }
                BookingStatus.CONFIRMED,
                BookingStatus.RETURNED -> {
                    if (!_isOnline.value) {
                        _errorMessage.value = "Нет интернета. Удаление невозможно."
                        return false
                    }

                    if (!authRepository.hasValidTokenForApi()) {
                        _errorMessage.value = "Требуется авторизация для удаления"
                        return false
                    }

                    bookingRepository.markForDeletion(bookingId)

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