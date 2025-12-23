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
import ru.kafpin.data.models.OrderWithDetails
import ru.kafpin.utils.NotificationHelper

class OrderViewModel(private val context: Context) : ViewModel() {
    private val TAG = "OrderViewModel"

    private val database = LibraryDatabase.getInstance(context)
    private val authRepository = RepositoryProvider.getAuthRepository(database, context)
    private val orderRepository = RepositoryProvider.getOrderRepository(
        database = database,
        authRepository = authRepository,
        context = context
    )

    private val networkMonitor = (context.applicationContext as ru.kafpin.MyApplication).networkMonitor

    private val _orders = MutableStateFlow<List<OrderWithDetails>>(emptyList())
    val orders: StateFlow<List<OrderWithDetails>> = _orders.asStateFlow()

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

        loadOrders()
        setupSearch()
        observeSyncing()
        observeSyncErrors()
        observeOrderStatusChanges()
    }

    private fun observeOrderStatusChanges() {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId() ?: -1
            orderRepository.getOrdersByUserFlow(userId).collect { orders ->
                _orders.value = orders
                Log.d(TAG, "Обновление списка заказов: ${orders.size} шт")
            }
        }
    }

    private fun observeSyncErrors() {
        viewModelScope.launch {
            orderRepository.syncErrors.collect { errors ->
                _syncErrors.value = errors
                if (errors.isNotEmpty()) {
                    Log.d(TAG, "Получены ошибки синхронизации: ${errors.size}")
                }
            }
        }
    }

    fun clearSyncError(orderId: Long) {
        orderRepository.removeSyncError(orderId)
    }

    fun clearAllSyncErrors() {
        orderRepository.clearSyncErrors()
    }

    private fun observeSyncing() {
        viewModelScope.launch {
            orderRepository.isSyncing.collect { syncing ->
                _isSyncing.value = syncing
            }
        }
    }

    fun canPerformAction(): Boolean {
        return !isLoading.value && !isSyncing.value
    }

    private fun loadOrders() {
        viewModelScope.launch {
            try {
                val userId = authRepository.getCurrentUserId()
                if (userId == null) {
                    _errorMessage.value = "Пользователь не авторизован"
                    _orders.value = emptyList()
                    return@launch
                }

                // Flow автоматически обновляет список
                orderRepository.getOrdersByUserFlow(userId)
                    .catch { e ->
                        Log.e(TAG, "Ошибка в потоке заказов", e)
                        _errorMessage.value = "Ошибка загрузки: ${e.message}"
                    }
                    .collect { ordersWithDetails ->
                        _orders.value = ordersWithDetails
                    }
            } catch (e: Exception) {
                _errorMessage.value = "Ошибка загрузки заказов: ${e.message}"
                Log.e(TAG, "Ошибка в loadOrders()", e)
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
        if (query.isBlank()) {
            loadOrders()
        }
    }

    suspend fun createOrder(
        title: String,
        authorSurname: String,
        authorName: String?,
        authorPatronymic: String?,
        quantity: Int,
        year: String?
    ): Result<Long> {
        return try {
            if (!canPerformAction()) {
                return Result.failure(Exception("Дождитесь завершения текущей операции"))
            }

            _isLoading.value = true

            val result = orderRepository.createLocalOrder(
                title = title,
                authorSurname = authorSurname,
                authorName = authorName,
                authorPatronymic = authorPatronymic,
                quantity = quantity,
                datePublication = year
            )

            if (result.isSuccess) {
                NotificationHelper.showOrderCreatedNotification(
                    context = context,
                    bookTitle = title,
                    orderId = result.getOrNull() ?: 0L
                )
            }

            result

        } catch (e: Exception) {
            _errorMessage.value = "Ошибка создания: ${e.message}"
            Log.e(TAG, "Ошибка создания заказа", e)
            Result.failure(e)
        } finally {
            _isLoading.value = false
        }
    }

    suspend fun syncOrders(): Boolean {
        return try {
            if (!canPerformAction()) {
                _errorMessage.value = "Дождитесь завершения синхронизации"
                return false
            }

            _isLoading.value = true
            _syncStatus.value = SyncStatus.SYNCING

            clearAllSyncErrors()

            val results = orderRepository.syncPendingOrders()

            val hasSuccess = results.any { it is ru.kafpin.repositories.SyncResult.Success }
            val errors = results.filterIsInstance<ru.kafpin.repositories.SyncResult.Error>()

            if (errors.isEmpty() && hasSuccess) {
                _syncStatus.value = SyncStatus.SUCCESS
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
            Log.e(TAG, "Ошибка в syncOrders()", e)
            false
        } finally {
            _isLoading.value = false
        }
    }

    suspend fun deleteOrder(localId: Long): Boolean {
        return try {
            if (!canPerformAction()) {
                _errorMessage.value = "Дождитесь завершения текущей операции"
                return false
            }

            val order = orderRepository.getOrderWithDetails(localId)
            if (order == null) {
                _errorMessage.value = "Заказ не найден"
                return false
            }

            if (!order.canDelete(networkMonitor.isOnline.value)) {
                _errorMessage.value = "Этот заказ нельзя удалить"
                return false
            }

            when (order.order.status) {
                OrderStatus.LOCAL_PENDING -> {
                    orderRepository.deleteLocalOrder(localId)
                    NotificationHelper.showOrderDeletedNotification(
                        context = context,
                        orderId = localId,
                        bookTitle = order.order.title,
                        adminDeleted = false
                    )
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
                    syncOrders()

                    NotificationHelper.showOrderDeletedNotification(
                        context = context,
                        orderId = localId,
                        bookTitle = order.order.title,
                        adminDeleted = false
                    )
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

    suspend fun canCreateNewOrder(): Boolean {
        return try {
            orderRepository.canCreateNewOrder()
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка проверки лимита", e)
            false
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}