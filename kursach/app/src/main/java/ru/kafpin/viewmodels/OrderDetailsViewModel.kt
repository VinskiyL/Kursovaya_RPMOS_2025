package ru.kafpin.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ru.kafpin.data.LibraryDatabase
import ru.kafpin.data.RepositoryProvider
import ru.kafpin.data.models.OrderStatus

class OrderDetailsViewModel(context: Context) : ViewModel() {
    private val TAG = "OrderDetailsViewModel"

    private val database = LibraryDatabase.getInstance(context)
    private val authRepository = RepositoryProvider.getAuthRepository(database, context)
    private val orderRepository = RepositoryProvider.getOrderRepository(
        database = database,
        authRepository = authRepository,
        context = context
    )

    private val networkMonitor = (context.applicationContext as ru.kafpin.MyApplication).networkMonitor

    private val _orderDetails = MutableStateFlow<ru.kafpin.data.models.OrderWithDetails?>(null)
    val orderDetails: StateFlow<ru.kafpin.data.models.OrderWithDetails?> = _orderDetails.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isOnline = MutableStateFlow(false)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    init {
        viewModelScope.launch {
            networkMonitor.isOnline.collect { online ->
                _isOnline.value = online
                Log.d(TAG, "Сеть: ${if (online) "ONLINE" else "OFFLINE"}")
            }
        }
    }

    suspend fun loadOrderDetails(orderId: Long) {
        _isLoading.value = true
        _errorMessage.value = null

        try {
            val order = orderRepository.getOrderWithDetails(orderId)
            _orderDetails.value = order
            if (order == null) {
                _errorMessage.value = "Заказ не найден"
            }
        } catch (e: Exception) {
            _errorMessage.value = "Ошибка загрузки: ${e.message}"
            Log.e(TAG, "Ошибка загрузки заказа", e)
        } finally {
            _isLoading.value = false
        }
    }

    suspend fun updateOrder(
        localId: Long,
        title: String,
        authorSurname: String,
        authorName: String?,
        authorPatronymic: String?,
        quantity: Int,
        year: String?
    ): Boolean {
        return try {
            val order = orderRepository.getOrderWithDetails(localId)
            if (order == null) {
                _errorMessage.value = "Заказ не найден"
                return false
            }

            if (!order.canEdit(_isOnline.value)) {
                _errorMessage.value = "Этот заказ нельзя редактировать"
                return false
            }

            // Для SERVER_PENDING нужен интернет
            if (order.order.status == OrderStatus.SERVER_PENDING && !_isOnline.value) {
                _errorMessage.value = "Нет интернета. Редактирование невозможно."
                return false
            }

            val success = orderRepository.updateLocalOrder(
                localId = localId,
                title = title,
                authorSurname = authorSurname,
                authorName = authorName,
                authorPatronymic = authorPatronymic,
                quantity = quantity,
                datePublication = year
            ).isSuccess

            if (success) {
                loadOrderDetails(localId)  // Перезагружаем данные
            }

            success

        } catch (e: Exception) {
            _errorMessage.value = "Ошибка обновления: ${e.message}"
            Log.e(TAG, "Ошибка обновления заказа", e)
            false
        }
    }

    suspend fun deleteOrder(localId: Long): Boolean {
        return try {
            val order = orderRepository.getOrderWithDetails(localId)
            if (order == null) {
                _errorMessage.value = "Заказ не найден"
                return false
            }

            if (!order.canDelete(_isOnline.value)) {
                _errorMessage.value = "Этот заказ нельзя удалить"
                return false
            }

            when (order.order.status) {
                OrderStatus.LOCAL_PENDING -> {
                    orderRepository.deleteLocalOrder(localId)
                    true
                }
                OrderStatus.SERVER_PENDING -> {
                    if (!_isOnline.value) {
                        _errorMessage.value = "Нет интернета. Удаление невозможно."
                        return false
                    }

                    if (!authRepository.hasValidTokenForApi()) {
                        _errorMessage.value = "Требуется авторизация для удаления"
                        return false
                    }

                    orderRepository.markForDeletion(localId)
                    orderRepository.syncPendingOrders()
                    true
                }
                OrderStatus.CONFIRMED -> {
                    _errorMessage.value = "Подтверждённый заказ нельзя удалить"
                    false
                }
            }
        } catch (e: Exception) {
            _errorMessage.value = "Ошибка удаления: ${e.message}"
            Log.e(TAG, "Ошибка удаления заказа", e)
            false
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}