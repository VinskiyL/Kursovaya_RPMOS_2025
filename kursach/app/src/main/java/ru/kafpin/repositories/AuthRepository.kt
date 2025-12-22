package ru.kafpin.repositories

import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import ru.kafpin.api.ApiClient
import ru.kafpin.api.models.*
import ru.kafpin.data.dao.AuthDao
import ru.kafpin.data.dao.BookingDao
import ru.kafpin.data.dao.OrderDao
import ru.kafpin.data.dao.ProfileDao
import ru.kafpin.data.dao.UserDao
import ru.kafpin.data.mappers.toAuthSessionEntity
import ru.kafpin.data.mappers.toUserEntity
import ru.kafpin.data.models.UserEntity
import ru.kafpin.utils.NetworkMonitor

class AuthRepository(
    private val authDao: AuthDao,
    private val userDao: UserDao,
    private val bookingDao: BookingDao,
    private val orderDao: OrderDao,
    private val profileDao: ProfileDao,
    private val networkMonitor: NetworkMonitor
) {
    private val TAG = "AuthRepository"
    private val apiService = ApiClient.apiService

    companion object {
        private const val ACCESS_TOKEN_LIFETIME = 15 * 60 * 1000L
        private const val REFRESH_TOKEN_LIFETIME = 24 * 60 * 60 * 1000L
        private const val REFRESH_THRESHOLD = 5 * 60 * 1000L
        private const val MIN_REFRESH_TOKEN_LIFETIME = 5 * 60 * 1000L
    }

    // ==================== ЛОГИН ====================

    suspend fun login(login: String, password: String): Result<UserEntity> {
        Log.d(TAG, "🔐 Логин: $login")

        return try {
            val response = apiService.login(LoginRequest(login, password))

            if (!response.isSuccessful) {
                val errorMsg = response.errorBody()?.string() ?: "Код ошибки: ${response.code()}"
                Log.e(TAG, "❌ Ошибка логина: $errorMsg")
                return Result.failure(Exception("Неверный логин или пароль"))
            }

            val authResponse = response.body()!!
            Log.d(TAG, "✅ Успешный логин: ${authResponse.user.login}")

            val userEntity = authResponse.user.toUserEntity()
            userDao.insertUser(userEntity)

            val session = authResponse.toAuthSessionEntity(userEntity.id)
            authDao.insertSession(session)

            Log.d(TAG, "💾 Данные сохранены локально")
            Result.success(userEntity)

        } catch (e: Exception) {
            Log.e(TAG, "💥 Ошибка при логине", e)
            Result.failure(Exception("Проверьте подключение к сети"))
        }
    }

    // ==================== ПОЛУЧЕНИЕ ДАННЫХ ====================

    suspend fun getCurrentUserId(): Long? {
        return getCurrentUser()?.id
    }

    suspend fun getCurrentUser(): UserEntity? {
        return authDao.getActiveSession()?.let { session ->
            userDao.getUser(session.userId)
        }
    }

    fun getCurrentUserFlow(): Flow<UserEntity?> {
        return authDao.getActiveSessionFlow()
            .map { session ->
                session?.let { userDao.getUser(it.userId) }
            }
    }

    suspend fun getAccessToken(): String? {
        return authDao.getActiveSession()?.accessToken
    }

    fun getAccessTokenFlow(): Flow<String?> {
        return authDao.getActiveSessionFlow()
            .map { it?.accessToken }
    }

    suspend fun isAuthenticated(): Boolean {
        return userDao.getCurrentUser() != null
    }

    fun isAuthenticatedFlow(): Flow<Boolean> {
        return userDao.getCurrentUserFlow()
            .map { it != null }
    }

    suspend fun hasValidTokenForApi(): Boolean {
        val session = authDao.getActiveSession() ?: return false
        val currentTime = System.currentTimeMillis()

        Log.d(TAG, "🔍 Проверка токена для API: " +
                "accessExpiresAt=${session.accessExpiresAt}, " +
                "refreshExpiresAt=${session.refreshExpiresAt}, " +
                "currentTime=$currentTime")

        // Access token валиден ещё хотя бы 1 минуту?
        if (session.accessExpiresAt > currentTime + 60_000L) {
            Log.d(TAG, "✅ Access token валиден (осталось ${(session.accessExpiresAt - currentTime) / 1000} сек)")
            return true
        }

        // Refresh token жив ещё минимум 5 минут?
        if (session.refreshExpiresAt > currentTime + MIN_REFRESH_TOKEN_LIFETIME) {
            Log.d(TAG, "✅ Можно обновить токен (refresh живёт ещё ${(session.refreshExpiresAt - currentTime) / 1000} сек)")
            return true
        }

        // Access token истёк И refresh token истекает менее чем через 5 минут
        Log.w(TAG, "⏰ Нельзя делать API запросы: " +
                "access истёк=${session.accessExpiresAt <= currentTime}, " +
                "refresh почти истёк=${(session.refreshExpiresAt - currentTime) / 1000} сек")
        return false
    }

    suspend fun getValidAccessToken(): String? {
        val session = authDao.getActiveSession() ?: return null
        val currentTime = System.currentTimeMillis()

        if (session.accessExpiresAt > currentTime + REFRESH_THRESHOLD) {
            Log.d(TAG, "✅ Access token валиден (осталось ${(session.accessExpiresAt - currentTime) / 1000} сек)")
            return session.accessToken
        }

        Log.d(TAG, "🔄 Access token скоро истечёт (осталось ${(session.accessExpiresAt - currentTime) / 1000} сек), обновляем...")

        if (session.refreshExpiresAt > currentTime + MIN_REFRESH_TOKEN_LIFETIME) {
            if (refreshTokenIfNeeded()) {
                return authDao.getActiveSession()?.accessToken
            }
        } else {
            Log.w(TAG, "⚠️ Refresh token скоро истечёт (осталось ${(session.refreshExpiresAt - currentTime) / 1000} сек)")
        }

        Log.e(TAG, "❌ Не удалось получить валидный токен")
        return null
    }

    // ==================== ОБНОВЛЕНИЕ ТОКЕНОВ ====================

    private var isRefreshing = false
    private var lastRefreshAttempt: Long = 0
    private val MIN_REFRESH_INTERVAL = 30_000L

    suspend fun refreshTokenIfNeeded(): Boolean {
        val now = System.currentTimeMillis()
        if (isRefreshing) {
            Log.d(TAG, "🔄 Уже обновляем токен, пропускаем...")
            return false
        }

        if (now - lastRefreshAttempt < MIN_REFRESH_INTERVAL) {
            Log.d(TAG, "🔄 Слишком частая попытка обновления, пропускаем...")
            return false
        }

        val session = authDao.getActiveSession() ?: return false
        val currentTime = now

        val timeUntilAccessExpiry = session.accessExpiresAt - currentTime

        if (timeUntilAccessExpiry > REFRESH_THRESHOLD) {
            Log.d(TAG, "✅ Токен действителен ещё ${timeUntilAccessExpiry / 1000} сек, обновление не требуется")
            return true
        }

        Log.d(TAG, "🔄 Токен скоро истечёт (осталось ${timeUntilAccessExpiry / 1000} сек), пытаемся обновить...")

        if (session.refreshExpiresAt < currentTime) {
            Log.w(TAG, "⏰ Refresh token истёк ${(currentTime - session.refreshExpiresAt) / 1000} сек назад, требуется повторный вход")
            return false
        }

        if (!networkMonitor.isOnline.value) {
            Log.w(TAG, "📴 Нет сети, обновление отложено")
            return timeUntilAccessExpiry > 0
        }

        isRefreshing = true
        lastRefreshAttempt = currentTime

        return try {
            Log.d(TAG, "📡 Отправляем запрос обновления токена на сервер...")

            val response = apiService.refreshToken(
                RefreshTokenRequest(session.refreshToken)
            )

            if (!response.isSuccessful) {
                Log.e(TAG, "❌ Не удалось обновить токен: ${response.code()}, ${response.message()}")

                if (response.code() == 401) {
                    Log.w(TAG, "🔓 Refresh token невалиден, очищаем сессию")
                    authDao.deleteSessionsForUser(session.userId)
                }

                return false
            }

            val authResponse = response.body()!!
            Log.d(TAG, "🔄 Получен новый refreshToken: ${authResponse.refreshToken.take(20)}...")

            val newSession = session.copy(
                accessToken = authResponse.accessToken,
                refreshToken = authResponse.refreshToken,
                accessExpiresAt = currentTime + ACCESS_TOKEN_LIFETIME,
                refreshExpiresAt = currentTime + REFRESH_TOKEN_LIFETIME
            )

            authDao.insertSession(newSession)
            Log.d(TAG, "✅ Токен успешно обновлён. " +
                    "Access до: ${newSession.accessExpiresAt}, " +
                    "Refresh до: ${newSession.refreshExpiresAt}")

            true
        } catch (e: Exception) {
            Log.e(TAG, "💥 Ошибка при обновлении токена", e)
            false
        } finally {
            isRefreshing = false
        }
    }

    // ==================== ВЫХОД ====================

    suspend fun forceLogout() {
        Log.w(TAG, "🚨 ПРИНУДИТЕЛЬНЫЙ ВЫХОД")

        val session = authDao.getActiveSession()
        if (session != null) {
            val userId = session.userId

            if (networkMonitor.isOnline.value) {
                try {
                    apiService.logout()
                    Log.d(TAG, "🌐 Сервер уведомлён о принудительном выходе")
                } catch (e: Exception) {
                    Log.w(TAG, "⚠️ Не удалось уведомить сервер: ${e.message}")
                }
            }
            try {
                bookingDao.deleteAllExceptPendingByUserId(userId)
                Log.d(TAG, "🗑️ Удалены все брони пользователя $userId")
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при удалении броней пользователя", e)
            }

            try {
                orderDao.deleteAllExceptLocalPendingByUserId(userId)
                Log.d(TAG, "🗑️ Удалены заказы пользователя $userId (кроме LOCAL_PENDING)")
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при удалении заказов пользователя", e)
            }

            try {
                profileDao.deleteByUserId(userId)
                Log.d(TAG, "🗑️ Удален профиль пользователя $userId")
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при удалении профиля пользователя", e)
            }

            authDao.deleteSessionsForUser(userId)
            userDao.deleteUser(userId)
            Log.d(TAG, "🧹 Все данные пользователя удалены")
        }
    }

    suspend fun logout(clearUserData: Boolean = false) {
        Log.d(TAG, "🚪 Выход из системы")

        val session = authDao.getActiveSession() ?: return
        val userId = session.userId

        if (networkMonitor.isOnline.value) {
            try {
                apiService.logout()
                Log.d(TAG, "🌐 Сервер уведомлён о выходе")
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ Не удалось уведомить сервер: ${e.message}")
            }
        }

        authDao.deleteSessionsForUser(userId)
        try {
            bookingDao.deleteAllExceptPendingByUserId(userId)
            Log.d(TAG, "🗑️ Удалены все брони пользователя $userId")
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при удалении броней пользователя", e)
        }

        try {
            orderDao.deleteAllExceptLocalPendingByUserId(userId)
            Log.d(TAG, "🗑️ Удалены заказы пользователя $userId (кроме LOCAL_PENDING)")
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при удалении заказов пользователя", e)
        }

        try {
            profileDao.deleteByUserId(userId)
            Log.d(TAG, "🗑️ Удален профиль пользователя $userId")
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при удалении профиля пользователя", e)
        }

        if (clearUserData) {
            userDao.deleteUser(userId)
            Log.d(TAG, "🧹 Локальная сессия и данные пользователя удалены")
        } else {
            Log.d(TAG, "🧹 Локальная сессия удалена (юзер остался для офлайн-режима)")
        }
    }

    suspend fun canContinueWithoutRelogin(): Boolean {
        val session = authDao.getActiveSession() ?: return false
        val currentTime = System.currentTimeMillis()

        Log.d(TAG, "DEBUG: refreshExpiresAt=${session.refreshExpiresAt}, " +
                "currentTime=$currentTime, " +
                "diff=${(session.refreshExpiresAt - currentTime) / 1000} сек, " +
                "required=600 сек")

        return session.refreshExpiresAt > currentTime + 600_000L
    }

    // ==================== УТИЛИТЫ ====================

    suspend fun getSessionInfo(): Map<String, Any> {
        val session = authDao.getActiveSession() ?: return mapOf(
            "status" to "no_session",
            "has_user" to false,
            "is_online" to networkMonitor.isOnline.value
        )

        val currentTime = System.currentTimeMillis()
        val accessSeconds = (session.accessExpiresAt - currentTime) / 1000
        val refreshSeconds = (session.refreshExpiresAt - currentTime) / 1000
        val hasUser = userDao.getUser(session.userId) != null

        return mapOf(
            "user_id" to session.userId,
            "access_expires_in" to "$accessSeconds сек",
            "refresh_expires_in" to "$refreshSeconds сек",
            "access_expires_seconds" to accessSeconds,
            "refresh_expires_seconds" to refreshSeconds,
            "is_online" to networkMonitor.isOnline.value,
            "has_user" to hasUser,
            "can_refresh" to (refreshSeconds > 300),
            "status" to when {
                accessSeconds > 60 -> "access_valid"
                refreshSeconds > 300 -> "can_refresh"
                refreshSeconds > 0 -> "refresh_expiring"
                else -> "expired"
            }
        )
    }

    fun getCurrentUserSync(): UserEntity? {
        return runBlocking {
            userDao.getCurrentUser()
        }
    }
}