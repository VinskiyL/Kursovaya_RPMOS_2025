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
import ru.kafpin.api.models.BookingCreateRequest
import ru.kafpin.api.models.BookingUpdateRequest
import ru.kafpin.data.dao.BookingDao
import ru.kafpin.data.models.BookingEntity
import ru.kafpin.data.models.BookingStatus
import ru.kafpin.data.models.BookingWithDetails
import ru.kafpin.utils.NotificationHelper
import java.time.LocalDate
import java.time.format.DateTimeFormatter

sealed class SyncResult {
    object Success : SyncResult()
    data class Error(
        val bookingId: Long,
        val bookTitle: String,
        val errorType: SyncErrorType,
        val message: String
    ) : SyncResult()
}

enum class SyncErrorType {
    DUPLICATE_BOOKING,    // 400 - уже есть активная бронь
    INSUFFICIENT_BOOKS,   // 409 - не хватает книг
    NETWORK_ERROR,
    SERVER_ERROR,
    AUTH_ERROR
}

class BookingRepository(
    private val bookingDao: BookingDao,
    private val authRepository: AuthRepository,
    private val context: Context
) {
    private val TAG = "BookingRepository"
    private val apiService = ApiClient.apiService
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    private var isTokenRefreshInProgress = false
    private var lastTokenRefreshTime: Long = 0
    private val TOKEN_REFRESH_COOLDOWN = 30_000L // 30 секунд

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: Flow<Boolean> = _isSyncing

    private val _syncErrors = MutableStateFlow<List<SyncResult.Error>>(emptyList())
    val syncErrors: Flow<List<SyncResult.Error>> = _syncErrors

    // ==================== БЕЗОПАСНОЕ ОБНОВЛЕНИЕ ТОКЕНА ====================

    private suspend fun safeRefreshToken(): Boolean {
        val now = System.currentTimeMillis()

        if (isTokenRefreshInProgress) {
            Log.d(TAG, "🔄 Уже обновляем токен, пропускаем...")
            return false
        }

        if (now - lastTokenRefreshTime < TOKEN_REFRESH_COOLDOWN) {
            Log.d(TAG, "🔄 Слишком частые попытки обновления, пропускаем...")
            return false
        }

        isTokenRefreshInProgress = true
        lastTokenRefreshTime = now

        return try {
            val result = authRepository.refreshTokenIfNeeded()
            Log.d(TAG, "🔄 Результат обновления токена: $result")
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
            Log.w(TAG, "⏰ Токен истёк (403), пробуем обновить...")

            if (safeRefreshToken()) {
                Log.d(TAG, "🔄 Токен обновлён, повторяем запрос...")
                val newResponse = retryAction()

                if (newResponse.isSuccessful) {
                    return newResponse.body()
                } else {
                    Log.e(TAG, "❌ Повторный запрос не удался: ${newResponse.code()}")
                    throw Exception("Не удалось выполнить запрос после обновления токена: ${newResponse.code()}")
                }
            } else {
                Log.e(TAG, "❌ Не удалось обновить токен")
                throw Exception("Не удалось обновить токен. Возможно, сессия истекла.")
            }
        }

        return null
    }

    // ==================== ЛОКАЛЬНЫЕ ОПЕРАЦИИ ====================

    suspend fun createLocalBooking(
        bookId: Long,
        bookTitle: String,
        bookAuthors: String,
        bookGenres: String,
        availableCopies: Int,
        userId: Long,
        quantity: Int,
        dateIssue: LocalDate,
        dateReturn: LocalDate
    ): Long {
        return withContext(Dispatchers.IO) {
            val booking = BookingEntity(
                bookId = bookId,
                bookTitle = bookTitle,
                bookAuthors = bookAuthors,
                bookGenres = bookGenres,
                availableCopies = availableCopies,
                userId = userId,
                quantity = quantity,
                dateIssue = dateIssue.format(dateFormatter),
                dateReturn = dateReturn.format(dateFormatter),
                status = BookingStatus.PENDING
            )

            bookingDao.insert(booking)
        }
    }

    suspend fun updateLocalQuantity(bookingId: Long, newQuantity: Int): Boolean {
        return withContext(Dispatchers.IO) {
            val booking = bookingDao.findById(bookingId)
            if (booking != null && booking.status in listOf(BookingStatus.PENDING, BookingStatus.CONFIRMED)) {
                val updated = booking.copy(
                    quantity = newQuantity,
                    lastUpdated = System.currentTimeMillis()
                )
                bookingDao.update(updated)
                true
            } else {
                false
            }
        }
    }

    suspend fun deleteLocalBooking(bookingId: Long) {
        withContext(Dispatchers.IO) {
            bookingDao.deleteById(bookingId)
        }
    }

    suspend fun markForDeletion(bookingId: Long) {
        withContext(Dispatchers.IO) {
            val booking = bookingDao.findById(bookingId)
            if (booking != null) {
                val updated = booking.copy(
                    markedForDeletion = true,
                    lastUpdated = System.currentTimeMillis()
                )
                bookingDao.update(updated)
            }
        }
    }

    // ==================== ПОИСК И ПОЛУЧЕНИЕ ====================

    fun getBookingsByUserFlow(userId: Long): Flow<List<BookingWithDetails>> {
        return bookingDao.getByUserIdFlow(userId).map { bookings ->
            bookings.map { BookingWithDetails(it) }
        }
    }

    suspend fun searchBookings(query: String): List<BookingWithDetails> {
        return withContext(Dispatchers.IO) {
            bookingDao.searchByBookTitle(query)
                .map { BookingWithDetails(it) }
        }
    }

    suspend fun getBookingWithDetails(localId: Long): BookingWithDetails? {
        return withContext(Dispatchers.IO) {
            val booking = bookingDao.findById(localId)
            booking?.let { BookingWithDetails(it) }
        }
    }

    suspend fun cleanupOldPendingBookings() {
        withContext(Dispatchers.IO) {
            try {
                val twoDaysAgo = System.currentTimeMillis() - 2 * 24 * 60 * 60 * 1000L
                val oldPending = bookingDao.getOldPending(twoDaysAgo)

                if (oldPending.isNotEmpty()) {
                    Log.d(TAG, "🧹 Удаляем старые PENDING: ${oldPending.size} шт")

                    oldPending.forEach { booking ->
                        try {
                            withContext(Dispatchers.Main) {
                                NotificationHelper.showPendingBookingExpiredNotification(
                                    context = this@BookingRepository.context,
                                    bookTitle = booking.bookTitle,
                                    bookingId = booking.localId
                                )
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Ошибка показа уведомления для брони ${booking.localId}", e)
                        }

                        bookingDao.deleteById(booking.localId)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка очистки старых броней", e)
            }
        }
    }

    // ==================== СИНХРОНИЗАЦИЯ ====================

    suspend fun syncPendingBookings(): List<SyncResult> {
        if (_isSyncing.value) {
            Log.d(TAG, "🔄 Уже синхронизируемся, пропускаем")
            return emptyList()
        }

        return try {
            _isSyncing.value = true

            if (!authRepository.hasValidTokenForApi()) {
                Log.w(TAG, "⚠️ Нет валидного токена для синхронизации броней")
                val error = SyncResult.Error(
                    bookingId = -1L,
                    bookTitle = "",
                    errorType = SyncErrorType.AUTH_ERROR,
                    message = "Требуется авторизация"
                )
                _syncErrors.value = listOf(error)
                return listOf(error)
            }

            Log.d(TAG, "✅ Есть валидный токен для синхронизации броней")

            val results = mutableListOf<SyncResult>()

            val statusResults = getRemoteBookings()
            results.addAll(statusResults)

            val hasAuthError = results.any {
                it is SyncResult.Error && it.errorType == SyncErrorType.AUTH_ERROR
            }

            if (!hasAuthError) {
                results.addAll(syncPendingDeletions())
                results.addAll(syncPendingCreations())
                cleanupOldPendingBookings()
            }

            val errors = results.filterIsInstance<SyncResult.Error>()
            if (errors.isNotEmpty()) {
                _syncErrors.value = errors
            }

            results
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка синхронизации броней", e)
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
    private suspend fun getRemoteBookings(): List<SyncResult> {
        val results = mutableListOf<SyncResult>()

        return try {
            Log.d(TAG, "🌐 Получение броней с сервера...")

            val token = authRepository.getValidAccessToken()?.let { "Bearer $it" }
            Log.d(TAG, "📎 Токен для запроса броней: ${token?.take(20)}...")

            val response = apiService.getMyBookings(token)

            val handledResponse = handleTokenExpiry(response) {
                val newToken = authRepository.getValidAccessToken()?.let { "Bearer $it" }
                apiService.getMyBookings(newToken)
            }

            if (handledResponse != null) {
                processServerBookings(handledResponse)
                results.add(SyncResult.Success)
                return results
            }

            if (response.isSuccessful) {
                val serverBookings = response.body()!!
                processServerBookings(serverBookings)
                results.add(SyncResult.Success)
            } else if (response.code() == 403) {
                Log.w(TAG, "🔐 403 - Токен невалиден")
                val error = SyncResult.Error(
                    bookingId = -1L,
                    bookTitle = "",
                    errorType = SyncErrorType.AUTH_ERROR,
                    message = "Требуется повторная авторизация"
                )
                results.add(error)
            } else {
                Log.w(TAG, "⚠️ Не удалось получить брони с сервера: ${response.code()}")
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
            Log.e(TAG, "Ошибка получения броней с сервера", e)
            val error = SyncResult.Error(
                bookingId = -1L,
                bookTitle = "",
                errorType = SyncErrorType.NETWORK_ERROR,
                message = "Ошибка сети при загрузке статусов"
            )
            listOf(error)
        }
    }

    private suspend fun processServerBookings(serverBookings: List<ru.kafpin.api.models.BookingResponse>) {
        Log.d(TAG, "📥 Получено броней с сервера: ${serverBookings.size}")

        val userId = authRepository.getCurrentUserId() ?: return
        val allLocalBookings = bookingDao.getByUserIdFlow(userId).first()
        val localBookingsWithServerId = allLocalBookings.filter { it.serverId != null }

        val serverIdsFromServer = serverBookings.map { it.id }
        val localIdsToDelete = localBookingsWithServerId
            .filter { local -> local.serverId !in serverIdsFromServer }
            .map { it.localId }

        if (localIdsToDelete.isNotEmpty()) {
            Log.d(TAG, "🗑️ Удаляем локальные брони которых нет на сервере: ${localIdsToDelete.size} шт")
            localIdsToDelete.forEach { localId ->
                val bookingToDelete = bookingDao.findById(localId)
                bookingToDelete?.let {
                    NotificationHelper.showStatusChangeNotification(
                        context = context,
                        bookingId = it.localId,
                        bookTitle = it.bookTitle,
                        oldStatus = it.status.name,
                        newStatus = "DELETED"
                    )
                }
                bookingDao.deleteById(localId)
            }
        }

        serverBookings.forEach { serverBooking ->
            val localBooking = bookingDao.findByServerId(serverBooking.id)
            if (localBooking != null) {
                val newStatus = serverBooking.toStatus()
                if (localBooking.status != newStatus) {
                    NotificationHelper.showStatusChangeNotification(
                        context = context,
                        bookingId = localBooking.localId,
                        bookTitle = localBooking.bookTitle,
                        oldStatus = localBooking.status.name,
                        newStatus = newStatus.name
                    )
                    bookingDao.update(localBooking.copy(
                        status = newStatus,
                        lastUpdated = System.currentTimeMillis()
                    ))
                    Log.d(TAG, "🔄 Обновлён статус брони ${serverBooking.id}: ${localBooking.status} -> $newStatus")
                }
            } else {
                NotificationHelper.showBookingCreatedNotification(
                    context = context,
                    bookTitle = serverBooking.bookTitle,
                    bookingId = serverBooking.id
                )
                val booking = BookingEntity(
                    serverId = serverBooking.id,
                    bookId = serverBooking.bookId,
                    bookTitle = serverBooking.bookTitle,
                    bookAuthors = "Неизвестно",
                    bookGenres = "Неизвестно",
                    availableCopies = -1,
                    userId = serverBooking.readerId,
                    quantity = serverBooking.quantity,
                    dateIssue = serverBooking.dateIssue,
                    dateReturn = serverBooking.dateReturn,
                    status = serverBooking.toStatus()
                )
                bookingDao.insert(booking)
                Log.d(TAG, "📥 Загружена бронь с сервера: ${serverBooking.id}")
            }
        }
    }

    private suspend fun syncPendingDeletions(): List<SyncResult> {
        val results = mutableListOf<SyncResult>()

        try {
            val bookingsToDelete = bookingDao.getMarkedForDeletion()
            Log.d(TAG, "🗑️ Найдено броней для удаления: ${bookingsToDelete.size}")

            bookingsToDelete.forEach { booking ->
                try {
                    if (booking.serverId != null) {
                        val token = authRepository.getValidAccessToken()?.let { "Bearer $it" }
                        val response = apiService.deleteBooking(booking.serverId, token)

                        val handledResponse = handleTokenExpiry(response) {
                            val newToken = authRepository.getValidAccessToken()?.let { "Bearer $it" }
                            apiService.deleteBooking(booking.serverId, newToken)
                        }

                        if (handledResponse != null || response.isSuccessful) {
                            bookingDao.deleteById(booking.localId)
                            Log.d(TAG, "✅ Бронь удалена с сервера: ${booking.serverId}")
                            results.add(SyncResult.Success)
                        } else {
                            Log.w(TAG, "⚠️ Не удалось удалить бронь с сервера: ${booking.serverId}")
                        }
                    } else {
                        bookingDao.deleteById(booking.localId)
                        Log.d(TAG, "🗑️ Локальная бронь удалена: ${booking.localId}")
                        results.add(SyncResult.Success)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Ошибка удаления брони ${booking.localId}", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка синхронизации удалений", e)
        }

        return results
    }

    private suspend fun syncPendingCreations(): List<SyncResult> {
        val results = mutableListOf<SyncResult>()

        try {
            val pendingBookings = bookingDao.getPendingForSync()
            Log.d(TAG, "📤 PENDING для отправки: ${pendingBookings.size}")

            pendingBookings.forEach { booking ->
                try {
                    val request = BookingCreateRequest(
                        bookId = booking.bookId,
                        quantity = booking.quantity,
                        dateIssue = booking.dateIssue,
                        dateReturn = booking.dateReturn
                    )

                    val token = authRepository.getValidAccessToken()?.let { "Bearer $it" }
                    val response = apiService.createBooking(request, token)

                    val handledResponse = handleTokenExpiry(response) {
                        val newToken = authRepository.getValidAccessToken()?.let { "Bearer $it" }
                        apiService.createBooking(request, newToken)
                    }

                    if (handledResponse != null) {
                        bookingDao.update(booking.copy(
                            serverId = handledResponse.id,
                            status = handledResponse.toStatus(),
                            lastUpdated = System.currentTimeMillis()
                        ))
                        Log.d(TAG, "✅ Бронь синхронизирована: ${handledResponse.id}")
                        results.add(SyncResult.Success)
                        return@forEach
                    }

                    when {
                        response.isSuccessful -> {
                            val serverBooking = response.body()!!
                            bookingDao.update(booking.copy(
                                serverId = serverBooking.id,
                                status = serverBooking.toStatus(),
                                lastUpdated = System.currentTimeMillis()
                            ))
                            Log.d(TAG, "✅ Бронь синхронизирована: ${serverBooking.id}")
                            results.add(SyncResult.Success)
                        }

                        response.code() == 400 &&
                                response.errorBody()?.string()?.contains("уже есть активная бронь") == true -> {
                            Log.w(TAG, "⚠️ Дубликат брони: ${booking.localId}")

                            val error = SyncResult.Error(
                                bookingId = booking.localId,
                                bookTitle = booking.bookTitle,
                                errorType = SyncErrorType.DUPLICATE_BOOKING,
                                message = "Уже есть активная бронь на книгу '${booking.bookTitle}'"
                            )
                            results.add(error)

                            bookingDao.deleteById(booking.localId)
                        }

                        response.code() == 409 ||
                                (response.code() == 400 &&
                                        response.errorBody()?.string()?.contains("книг") == true) -> {
                            Log.w(TAG, "📚 Не хватает книг для брони ${booking.localId}")

                            val error = SyncResult.Error(
                                bookingId = booking.localId,
                                bookTitle = booking.bookTitle,
                                errorType = SyncErrorType.INSUFFICIENT_BOOKS,
                                message = "Не хватает книг '${booking.bookTitle}'. Доступно: ${booking.availableCopies}"
                            )
                            results.add(error)
                        }

                        else -> {
                            Log.w(TAG, "⚠️ Ошибка создания брони: ${response.code()}")
                            val error = SyncResult.Error(
                                bookingId = booking.localId,
                                bookTitle = booking.bookTitle,
                                errorType = SyncErrorType.SERVER_ERROR,
                                message = "Ошибка сервера (${response.code()})"
                            )
                            results.add(error)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Ошибка синхронизации брони ${booking.localId}", e)
                    val error = SyncResult.Error(
                        bookingId = booking.localId,
                        bookTitle = booking.bookTitle,
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



    fun clearSyncErrors() {
        _syncErrors.value = emptyList()
    }

    fun removeSyncError(bookingId: Long) {
        val currentErrors = _syncErrors.value
        _syncErrors.value = currentErrors.filter { it.bookingId != bookingId }
    }

    // ==================== ОБНОВЛЕНИЕ НА СЕРВЕРЕ ====================

    suspend fun updateServerQuantity(serverId: Long, newQuantity: Int): Boolean {
        return try {
            val token = authRepository.getValidAccessToken()?.let { "Bearer $it" } ?: return false
            val request = BookingUpdateRequest(newQuantity)

            val response = apiService.updateBookingQuantity(serverId, request, token)

            val handledResponse = handleTokenExpiry(response) {
                val newToken = authRepository.getValidAccessToken()?.let { "Bearer $it" }
                apiService.updateBookingQuantity(serverId, request, newToken ?: token)
            }

            if (handledResponse != null || response.isSuccessful) {
                val localBooking = bookingDao.findByServerId(serverId)
                if (localBooking != null) {
                    bookingDao.update(localBooking.copy(
                        quantity = newQuantity,
                        lastUpdated = System.currentTimeMillis()
                    ))
                }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка обновления количества на сервере", e)
            false
        }
    }

    // ==================== УТИЛИТЫ ====================

    suspend fun hasExistingBooking(
        bookId: Long,
        userId: Long
    ): Boolean {
        return withContext(Dispatchers.IO) {
            val existing = bookingDao.findByBookAndUser(
                bookId = bookId,
                userId = userId
            )
            existing != null && existing.status in listOf(
                BookingStatus.PENDING,
                BookingStatus.CONFIRMED,
                BookingStatus.ISSUED
            )
        }
    }
}