package ru.kafpin.repositories

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import retrofit2.Response
import ru.kafpin.api.ApiClient
import ru.kafpin.api.models.OrderCreateRequest
import ru.kafpin.data.dao.OrderDao
import ru.kafpin.data.models.OrderEntity
import ru.kafpin.data.models.OrderStatus
import ru.kafpin.data.models.OrderWithDetails
import ru.kafpin.utils.NotificationHelper
import java.time.LocalDate

/**
 * Репозиторий для заказов. Аналог BookingRepository с упрощениями:
 * - Нет дат выдачи/возврата
 * - Нет связи с существующими книгами
 * - 3 статуса вместо 4
 * - Лимит 5 не-подтверждённых заказов
 */
class OrderRepository(
    private val orderDao: OrderDao,
    private val authRepository: AuthRepository,
    private val context: Context
) {
    private val TAG = "OrderRepository"
    private val apiService = ApiClient.apiService

    private var isTokenRefreshInProgress = false
    private var lastTokenRefreshTime: Long = 0
    private val TOKEN_REFRESH_COOLDOWN = 30_000L

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: Flow<Boolean> = _isSyncing

    private val _syncErrors = MutableStateFlow<List<SyncResult.Error>>(emptyList())
    val syncErrors: Flow<List<SyncResult.Error>> = _syncErrors

    // ==================== БЕЗОПАСНОЕ ОБНОВЛЕНИЕ ТОКЕНА ====================

    private suspend fun safeRefreshToken(): Boolean {
        val now = System.currentTimeMillis()

        if (isTokenRefreshInProgress) {
            Log.d(TAG, "Уже обновляем токен, пропускаем...")
            return false
        }

        if (now - lastTokenRefreshTime < TOKEN_REFRESH_COOLDOWN) {
            Log.d(TAG, "Слишком частые попытки, пропускаем...")
            return false
        }

        isTokenRefreshInProgress = true
        lastTokenRefreshTime = now

        return try {
            val result = authRepository.refreshTokenIfNeeded()
            Log.d(TAG, "Результат обновления токена: $result")
            result
        } finally {
            isTokenRefreshInProgress = false
        }
    }

    // ==================== ОБРАБОТКА ИСТЕЧЕНИЯ ТОКЕНА ====================

    private suspend fun <T> handleTokenExpiry(
        response: Response<T>,
        retryAction: suspend () -> Response<T>
    ): T? {
        if (response.code() == 403) {
            Log.w(TAG, "Токен истёк (403), пробуем обновить...")

            if (safeRefreshToken()) {
                Log.d(TAG, "Токен обновлён, повторяем запрос...")
                val newResponse = retryAction()

                if (newResponse.isSuccessful) {
                    return newResponse.body()
                } else {
                    Log.e(TAG, "Повторный запрос не удался: ${newResponse.code()}")
                    throw Exception("Не удалось выполнить запрос после обновления токена: ${newResponse.code()}")
                }
            } else {
                Log.e(TAG, "Не удалось обновить токен")
                throw Exception("Не удалось обновить токен. Возможно, сессия истекла.")
            }
        }

        return null
    }

    // ==================== ЛОКАЛЬНЫЕ ОПЕРАЦИИ ====================

    /**
     * Создание локального заказа с проверкой лимита 5
     */
    suspend fun createLocalOrder(
        title: String,
        authorSurname: String,
        authorName: String?,
        authorPatronymic: String?,
        quantity: Int,
        datePublication: String?
    ): Result<Long> {
        return withContext(Dispatchers.IO) {
            try {
                val userId = authRepository.getCurrentUserId()
                    ?: return@withContext Result.failure(Exception("Пользователь не авторизован"))

                // Проверка лимита 5 не-подтверждённых заказов
                val activeCount = orderDao.countActiveByUser(userId)
                if (activeCount >= 5) {
                    return@withContext Result.failure(Exception("Нельзя создать более 5 заказов. Удалите старые."))
                }

                // Проверка количества
                if (quantity < 1 || quantity > 5) {
                    return@withContext Result.failure(Exception("Количество должно быть от 1 до 5"))
                }

                // Проверка года (только цифры если указан)
                datePublication?.let { year ->
                    if (!year.matches(Regex("\\d{4}"))) {
                        return@withContext Result.failure(Exception("Год должен содержать 4 цифры"))
                    }
                }

                val order = OrderEntity(
                    title = title,
                    authorSurname = authorSurname,
                    authorName = authorName,
                    authorPatronymic = authorPatronymic,
                    quantity = quantity,
                    datePublication = datePublication,
                    userId = userId,
                    status = OrderStatus.LOCAL_PENDING
                )

                val localId = orderDao.insert(order)
                Result.success(localId)

            } catch (e: Exception) {
                Log.e(TAG, "Ошибка создания локального заказа", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Обновление локального заказа
     */
    suspend fun updateLocalOrder(
        localId: Long,
        title: String,
        authorSurname: String,
        authorName: String?,
        authorPatronymic: String?,
        quantity: Int,
        datePublication: String?
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val order = orderDao.findById(localId)
                if (order == null) {
                    return@withContext Result.failure(Exception("Заказ не найден"))
                }

                // Проверка прав на редактирование
                if (order.status == OrderStatus.CONFIRMED) {
                    return@withContext Result.failure(Exception("Подтверждённый заказ нельзя редактировать"))
                }

                // Проверка количества
                if (quantity < 1 || quantity > 5) {
                    return@withContext Result.failure(Exception("Количество должно быть от 1 до 5"))
                }

                // Проверка года
                datePublication?.let { year ->
                    if (!year.matches(Regex("\\d{4}"))) {
                        return@withContext Result.failure(Exception("Год должен содержать 4 цифры"))
                    }
                }

                val updated = order.copy(
                    title = title,
                    authorSurname = authorSurname,
                    authorName = authorName,
                    authorPatronymic = authorPatronymic,
                    quantity = quantity,
                    datePublication = datePublication,
                    lastUpdated = System.currentTimeMillis()
                )

                orderDao.update(updated)
                Result.success(Unit)

            } catch (e: Exception) {
                Log.e(TAG, "Ошибка обновления заказа", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Удаление локального заказа
     */
    suspend fun deleteLocalOrder(localId: Long) {
        withContext(Dispatchers.IO) {
            orderDao.deleteById(localId)
        }
    }

    /**
     * Пометить заказ на удаление (для синхронизации)
     */
    suspend fun markForDeletion(localId: Long) {
        withContext(Dispatchers.IO) {
            val order = orderDao.findById(localId)
            if (order != null) {
                val updated = order.copy(
                    markedForDeletion = true,
                    lastUpdated = System.currentTimeMillis()
                )
                orderDao.update(updated)
            }
        }
    }

    /**
     * Проверка можно ли создать новый заказ (лимит 5)
     */
    suspend fun canCreateNewOrder(): Boolean {
        return withContext(Dispatchers.IO) {
            val userId = authRepository.getCurrentUserId() ?: return@withContext false
            val activeCount = orderDao.countActiveByUser(userId)
            activeCount < 5
        }
    }

    // ==================== ПОИСК И ПОЛУЧЕНИЕ ====================

    /**
     * Flow заказов пользователя для UI
     */
    fun getOrdersByUserFlow(userId: Long): Flow<List<OrderWithDetails>> {
        return orderDao.getByUserIdFlow(userId).map { orders ->
            orders.map { OrderWithDetails(it) }
        }
    }

    /**
     * Получение заказа с деталями
     */
    suspend fun getOrderWithDetails(localId: Long): OrderWithDetails? {
        return withContext(Dispatchers.IO) {
            val order = orderDao.findById(localId)
            order?.let { OrderWithDetails(it) }
        }
    }

    /**
     * Очистка старых LOCAL_PENDING заказов (например, старше 2 дней)
     */
    suspend fun cleanupOldPendingOrders() {
        withContext(Dispatchers.IO) {
            try {
                val twoDaysAgo = System.currentTimeMillis() - 2 * 24 * 60 * 60 * 1000L
                val oldPending = orderDao.getOldPending(twoDaysAgo)

                if (oldPending.isNotEmpty()) {
                    Log.d(TAG, "Удаляем старые LOCAL_PENDING: ${oldPending.size} шт")

                    oldPending.forEach { order ->
                        orderDao.deleteById(order.localId)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка очистки старых заказов", e)
            }
        }
    }

    // ==================== СИНХРОНИЗАЦИЯ ====================

    /**
     * Синхронизация заказов (аналогично броням, но проще)
     */
    suspend fun syncPendingOrders(): List<SyncResult> {
        if (_isSyncing.value) {
            Log.d(TAG, "Уже синхронизируемся, пропускаем")
            return emptyList()
        }

        return try {
            _isSyncing.value = true

            if (!authRepository.hasValidTokenForApi()) {
                Log.w(TAG, "Нет валидного токена для синхронизации")
                val error = SyncResult.Error(
                    bookingId = -1L,
                    bookTitle = "",
                    errorType = SyncErrorType.AUTH_ERROR,
                    message = "Требуется авторизация"
                )
                _syncErrors.value = listOf(error)
                return listOf(error)
            }

            Log.d(TAG, "Есть валидный токен для синхронизации")
            val results = mutableListOf<SyncResult>()

            // 1. Получить заказы с сервера
            val statusResults = getRemoteOrders()
            results.addAll(statusResults)

            val hasAuthError = results.any { it is SyncResult.Error && it.errorType == SyncErrorType.AUTH_ERROR }

            if (!hasAuthError) {
                // 2. Отправить удаления
                results.addAll(syncPendingDeletions())
                // 3. Отправить создания
                results.addAll(syncPendingCreations())
                // 4. Очистка старых
                cleanupOldPendingOrders()
            }

            val errors = results.filterIsInstance<SyncResult.Error>()
            if (errors.isNotEmpty()) {
                _syncErrors.value = errors
            }

            results

        } catch (e: Exception) {
            Log.e(TAG, "Ошибка синхронизации заказов", e)
            val error = SyncResult.Error(
                bookingId = -1L,
                bookTitle = "",
                errorType = SyncErrorType.NETWORK_ERROR,
                message = "Сетевая ошибка: ${e.message}"
            )
            _syncErrors.value = listOf(error)
            listOf(error)
        } finally {
            _isSyncing.value = false
        }
    }

    /**
     * Получение заказов с сервера (этап 1)
     */
    private suspend fun getRemoteOrders(): List<SyncResult> {
        val results = mutableListOf<SyncResult>()

        return try {
            Log.d(TAG, "Получение заказов с сервера...")

            val token = authRepository.getValidAccessToken()?.let { "Bearer $it" }
            Log.d(TAG, "Токен для запроса заказов: ${token?.take(20)}...")

            val response = apiService.getMyOrders(token)

            val handledResponse = handleTokenExpiry(response) {
                val newToken = authRepository.getValidAccessToken()?.let { "Bearer $it" }
                apiService.getMyOrders(newToken)
            }

            if (handledResponse != null) {
                processServerOrders(handledResponse)
                results.add(SyncResult.Success)
                return results
            }

            if (response.isSuccessful) {
                val serverOrders = response.body()!!
                processServerOrders(serverOrders)
                results.add(SyncResult.Success)
            } else if (response.code() == 403) {
                Log.w(TAG, "403 - Токен невалиден")
                val error = SyncResult.Error(
                    bookingId = -1L,
                    bookTitle = "",
                    errorType = SyncErrorType.AUTH_ERROR,
                    message = "Требуется повторная авторизация"
                )
                results.add(error)
            } else {
                Log.w(TAG, "Не удалось получить заказы с сервера: ${response.code()}")
                val error = SyncResult.Error(
                    bookingId = -1L,
                    bookTitle = "",
                    errorType = SyncErrorType.SERVER_ERROR,
                    message = "Ошибка сервера: ${response.code()}"
                )
                results.add(error)
            }

            results

        } catch (e: Exception) {
            Log.e(TAG, "Ошибка получения заказов с сервера", e)
            val error = SyncResult.Error(
                bookingId = -1L,
                bookTitle = "",
                errorType = SyncErrorType.NETWORK_ERROR,
                message = "Ошибка сети при загрузке статусов"
            )
            listOf(error)
        }
    }

    /**
     * Обработка заказов с сервера
     */
    private suspend fun processServerOrders(serverOrders: List<ru.kafpin.api.models.OrderResponse>) {
        Log.d(TAG, "Получено заказов с сервера: ${serverOrders.size}")

        val userId = authRepository.getCurrentUserId() ?: return
        val allLocalOrders = orderDao.getByUserIdFlow(userId).first()
        val localOrdersWithServerId = allLocalOrders.filter { it.serverId != null }

        // ID заказов которые есть на сервере
        val serverIdsFromServer = serverOrders.map { it.id }

        // Локальные заказы которых нет на сервере → админ удалил → удаляем локально
        val localIdsToDelete = localOrdersWithServerId
            .filter { local -> local.serverId !in serverIdsFromServer }
            .map { it.localId }

        if (localIdsToDelete.isNotEmpty()) {
            Log.d(TAG, "Удаляем локальные заказы которых нет на сервере: ${localIdsToDelete.size} шт")
            localIdsToDelete.forEach { localId ->
                val orderToDelete = orderDao.findById(localId)
                orderToDelete?.let {
                    // Уведомление о удалении админом
                    NotificationHelper.showOrderDeletedNotification(
                        context = context,
                        orderId = it.localId,
                        bookTitle = it.title,
                        adminDeleted = true
                    )
                }
                orderDao.deleteById(localId)
            }
        }

        // Обновление/добавление заказов с сервера
        serverOrders.forEach { serverOrder ->
            val localOrder = orderDao.findByServerId(serverOrder.id)

            if (localOrder != null) {
                // Заказ уже есть локально - обновляем статус
                val newStatus = serverOrder.toStatus()
                if (localOrder.status != newStatus) {
                    val oldStatus = localOrder.status

                    // Уведомление о изменении статуса
                    if (oldStatus == OrderStatus.SERVER_PENDING && newStatus == OrderStatus.CONFIRMED) {
                        NotificationHelper.showOrderConfirmedNotification(
                            context = context,
                            orderId = localOrder.localId,
                            bookTitle = localOrder.title
                        )
                    }

                    orderDao.update(localOrder.copy(
                        status = newStatus,
                        lastUpdated = System.currentTimeMillis()
                    ))
                    Log.d(TAG, "Обновлён статус заказа ${serverOrder.id}: $oldStatus -> $newStatus")
                }
            } else {
                // Новый заказ с сервера (например, созданный в веб-интерфейсе)
                NotificationHelper.showOrderCreatedNotification(
                    context = context,
                    bookTitle = serverOrder.title,
                    orderId = serverOrder.id
                )

                val order = OrderEntity(
                    serverId = serverOrder.id,
                    title = serverOrder.title,
                    authorSurname = serverOrder.authorSurname,
                    authorName = serverOrder.authorName,
                    authorPatronymic = serverOrder.authorPatronymic,
                    quantity = serverOrder.quantity,
                    datePublication = serverOrder.datePublication,
                    userId = serverOrder.readerId,
                    status = serverOrder.toStatus()
                )
                orderDao.insert(order)
                Log.d(TAG, "Загружен заказ с сервера: ${serverOrder.id}")
            }
        }
    }

    /**
     * Синхронизация удалений (этап 2)
     */
    private suspend fun syncPendingDeletions(): List<SyncResult> {
        val results = mutableListOf<SyncResult>()

        try {
            val ordersToDelete = orderDao.getMarkedForDeletion()
            Log.d(TAG, "Найдено заказов для удаления: ${ordersToDelete.size}")

            ordersToDelete.forEach { order ->
                try {
                    if (order.serverId != null) {
                        val token = authRepository.getValidAccessToken()?.let { "Bearer $it" }
                        val response = apiService.deleteOrder(order.serverId, token)

                        val handledResponse = handleTokenExpiry(response) {
                            val newToken = authRepository.getValidAccessToken()?.let { "Bearer $it" }
                            apiService.deleteOrder(order.serverId, newToken)
                        }

                        if (handledResponse != null || response.isSuccessful) {
                            orderDao.deleteById(order.localId)
                            Log.d(TAG, "Заказ удалён с сервера: ${order.serverId}")
                            results.add(SyncResult.Success)
                        } else {
                            Log.w(TAG, "Не удалось удалить заказ с сервера: ${order.serverId}")
                        }
                    } else {
                        // Локальный заказ без serverId - просто удаляем
                        orderDao.deleteById(order.localId)
                        Log.d(TAG, "Локальный заказ удалён: ${order.localId}")
                        results.add(SyncResult.Success)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Ошибка удаления заказа ${order.localId}", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка синхронизации удалений", e)
        }

        return results
    }

    /**
     * Синхронизация созданий (этап 3)
     */
    private suspend fun syncPendingCreations(): List<SyncResult> {
        val results = mutableListOf<SyncResult>()

        try {
            val pendingOrders = orderDao.getPendingForSync()
            Log.d(TAG, "LOCAL_PENDING для отправки: ${pendingOrders.size}")

            pendingOrders.forEach { order ->
                try {
                    val request = OrderCreateRequest(
                        title = order.title,
                        authorSurname = order.authorSurname,
                        authorName = order.authorName,
                        authorPatronymic = order.authorPatronymic,
                        quantity = order.quantity,
                        datePublication = order.datePublication
                    )

                    val token = authRepository.getValidAccessToken()?.let { "Bearer $it" }
                    val response = apiService.createOrder(request, token)  // ← РЕАЛЬНЫЙ ВЫЗОВ!

                    val handledResponse = handleTokenExpiry(response) {
                        val newToken = authRepository.getValidAccessToken()?.let { "Bearer $it" }
                        apiService.createOrder(request, newToken)
                    }

                    if (handledResponse != null) {
                        // Успешно создано на сервере
                        orderDao.update(order.copy(
                            serverId = handledResponse.id,
                            status = handledResponse.toStatus(), // SERVER_PENDING
                            lastUpdated = System.currentTimeMillis()
                        ))

                        // Уведомление о отправке
                        NotificationHelper.showOrderSentNotification(
                            context = context,
                            orderId = order.localId,
                            bookTitle = order.title
                        )

                        Log.d(TAG, "Заказ синхронизирован: ${handledResponse.id}")
                        results.add(SyncResult.Success)
                        return@forEach
                    }

                    if (response.isSuccessful) {
                        val serverOrder = response.body()!!
                        orderDao.update(order.copy(
                            serverId = serverOrder.id,
                            status = serverOrder.toStatus(),
                            lastUpdated = System.currentTimeMillis()
                        ))

                        NotificationHelper.showOrderSentNotification(
                            context = context,
                            orderId = order.localId,
                            bookTitle = order.title
                        )

                        Log.d(TAG, "Заказ синхронизирован: ${serverOrder.id}")
                        results.add(SyncResult.Success)

                    } else if (response.code() == 400) {
                        // Ошибка валидации или лимит 5 на сервере
                        val errorBody = response.errorBody()?.string() ?: ""
                        Log.w(TAG, "Ошибка создания заказа: ${response.code()}, $errorBody")

                        val error = SyncResult.Error(
                            bookingId = order.localId,
                            bookTitle = order.title,
                            errorType = if (errorBody.contains("5 заказов"))
                                SyncErrorType.DUPLICATE_BOOKING
                            else
                                SyncErrorType.SERVER_ERROR,
                            message = "Ошибка создания заказа: $errorBody"
                        )
                        results.add(error)

                    } else {
                        Log.w(TAG, "Ошибка создания заказа: ${response.code()}")
                        val error = SyncResult.Error(
                            bookingId = order.localId,
                            bookTitle = order.title,
                            errorType = SyncErrorType.SERVER_ERROR,
                            message = "Ошибка сервера (${response.code()})"
                        )
                        results.add(error)
                    }

                } catch (e: Exception) {
                    Log.e(TAG, "Ошибка синхронизации заказа ${order.localId}", e)
                    val error = SyncResult.Error(
                        bookingId = order.localId,
                        bookTitle = order.title,
                        errorType = SyncErrorType.NETWORK_ERROR,
                        message = "Сетевая ошибка: ${e.message}"
                    )
                    results.add(error)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка синхронизации созданий", e)
            val error = SyncResult.Error(
                bookingId = -1L,
                bookTitle = "",
                errorType = SyncErrorType.NETWORK_ERROR,
                message = "Ошибка сети: ${e.message}"
            )
            results.add(error)
        }

        return results
    }

    // ==================== УТИЛИТЫ ====================

    fun clearSyncErrors() {
        _syncErrors.value = emptyList()
    }

    fun removeSyncError(orderId: Long) {
        val currentErrors = _syncErrors.value
        _syncErrors.value = currentErrors.filter { it.bookingId != orderId }
    }
}