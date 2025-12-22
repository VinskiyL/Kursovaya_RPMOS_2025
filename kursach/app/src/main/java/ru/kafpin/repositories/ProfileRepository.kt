package ru.kafpin.repositories

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import retrofit2.Response
import ru.kafpin.api.ApiClient
import ru.kafpin.api.models.*
import ru.kafpin.data.dao.ProfileDao
import ru.kafpin.data.models.ProfileEntity
import ru.kafpin.data.models.ProfileWithDetails

class ProfileRepository(
    private val profileDao: ProfileDao,
    private val authRepository: AuthRepository,
    private val context: Context
) {
    private val TAG = "ProfileRepository"
    private val apiService = ApiClient.apiService

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: Flow<Boolean> = _isSyncing

    // ==================== ЗАГРУЗКА С СЕРВЕРА ====================

    /**
     * Загрузить профиль с сервера и сохранить локально
     * (вызывается при логине)
     */
    suspend fun fetchProfileFromServer(): Result<ProfileEntity> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Загрузка профиля с сервера...")

                val token = authRepository.getValidAccessToken()?.let { "Bearer $it" }
                if (token == null) {
                    return@withContext Result.failure(Exception("Требуется авторизация"))
                }

                val response = apiService.getMyProfile(token)

                if (!response.isSuccessful) {
                    val errorMsg = "Ошибка загрузки: ${response.code()}"
                    Log.e(TAG, errorMsg)
                    return@withContext Result.failure(Exception(errorMsg))
                }

                val profileResponse = response.body()!!
                val userId = authRepository.getCurrentUserId() ?: -1L

                // Конвертируем в локальную модель
                val profileEntity = profileResponse.toProfileEntity(userId)

                // Сохраняем в БД (REPLACE - всегда одна запись на пользователя)
                profileDao.insert(profileEntity)

                // Получаем имя для логирования (как в OrderRepository)
                val fullName = "${profileEntity.surname} ${profileEntity.name} ${profileEntity.patronymic ?: ""}"
                Log.d(TAG, "✅ Профиль загружен и сохранён: $fullName")
                Result.success(profileEntity)

            } catch (e: Exception) {
                Log.e(TAG, "Ошибка загрузки профиля", e)
                Result.failure(e)
            }
        }
    }

    // ==================== ПОЛУЧЕНИЕ ИЗ БД ====================

    /**
     * Получить профиль из локальной БД
     */
    suspend fun getProfile(userId: Long): ProfileEntity? {
        return withContext(Dispatchers.IO) {
            profileDao.findByUserId(userId)
        }
    }

    /**
     * Flow профиля для UI
     */
    fun getProfileFlow(userId: Long): Flow<ProfileWithDetails?> {
        return profileDao.getByUserIdFlow(userId).map { entity ->
            entity?.let { ProfileWithDetails(it) }
        }
    }

    // ==================== ОБНОВЛЕНИЕ ПРОФИЛЯ ====================

    /**
     * Обновить профиль (только онлайн)
     */
    suspend fun updateProfile(profile: ProfileEntity): Result<ProfileEntity> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Обновление профиля на сервере...")

                val token = authRepository.getValidAccessToken()?.let { "Bearer $it" }
                if (token == null) {
                    return@withContext Result.failure(Exception("Требуется авторизация"))
                }

                // Создаём запрос для сервера
                val request = ReaderUpdateRequest.fromProfileEntity(profile)
                val response = apiService.updateProfile(request, token)

                if (!response.isSuccessful) {
                    val errorBody = response.errorBody()?.string() ?: "Неизвестная ошибка"
                    Log.e(TAG, "Ошибка обновления: ${response.code()}, $errorBody")
                    return@withContext Result.failure(Exception("Ошибка сервера: ${response.code()}"))
                }

                // Обновляем локальную БД
                val updatedProfile = profile.copy(lastSyncedAt = System.currentTimeMillis())
                profileDao.insert(updatedProfile)

                Log.d(TAG, "✅ Профиль обновлён")
                Result.success(updatedProfile)

            } catch (e: Exception) {
                Log.e(TAG, "Ошибка обновления профиля", e)
                Result.failure(e)
            }
        }
    }

    // ==================== ИЗМЕНЕНИЕ ЛОГИНА ====================

    suspend fun changeLogin(currentPassword: String, newLogin: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val token = authRepository.getValidAccessToken()?.let { "Bearer $it" }
                if (token == null) {
                    return@withContext Result.failure(Exception("Требуется авторизация"))
                }

                val request = ChangeLoginRequest(currentPassword, newLogin)
                val response = apiService.changeLogin(request, token)

                if (!response.isSuccessful) {
                    val errorBody = response.errorBody()?.string() ?: "Неизвестная ошибка"
                    Log.e(TAG, "Ошибка смены логина: ${response.code()}, $errorBody")

                    // Парсим сообщение об ошибке
                    val errorMessage = when {
                        errorBody.contains("Неверный пароль") -> "Неверный пароль"
                        errorBody.contains("Логин уже занят") -> "Логин уже занят"
                        else -> "Ошибка: ${response.code()}"
                    }

                    return@withContext Result.failure(Exception(errorMessage))
                }

                // Логин изменён успешно - обновляем локальные данные
                val userId = authRepository.getCurrentUserId() ?: return@withContext Result.success(Unit)
                val profile = profileDao.findByUserId(userId)
                profile?.let {
                    val updated = it.copy(login = newLogin, lastSyncedAt = System.currentTimeMillis())
                    profileDao.insert(updated)
                }

                Log.d(TAG, "✅ Логин изменён на: $newLogin")
                Result.success(Unit)

            } catch (e: Exception) {
                Log.e(TAG, "Ошибка смены логина", e)
                Result.failure(e)
            }
        }
    }

    // ==================== ИЗМЕНЕНИЕ ПАРОЛЯ ====================

    suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val token = authRepository.getValidAccessToken()?.let { "Bearer $it" }
                if (token == null) {
                    return@withContext Result.failure(Exception("Требуется авторизация"))
                }

                val request = ChangePasswordRequest(currentPassword, newPassword)
                val response = apiService.changePassword(request, token)

                if (!response.isSuccessful) {
                    val errorBody = response.errorBody()?.string() ?: "Неизвестная ошибка"
                    Log.e(TAG, "Ошибка смены пароля: ${response.code()}, $errorBody")

                    val errorMessage = if (errorBody.contains("Неверный пароль")) {
                        "Неверный текущий пароль"
                    } else {
                        "Ошибка: ${response.code()}"
                    }

                    return@withContext Result.failure(Exception(errorMessage))
                }

                Log.d(TAG, "✅ Пароль изменён")
                Result.success(Unit)

            } catch (e: Exception) {
                Log.e(TAG, "Ошибка смены пароля", e)
                Result.failure(e)
            }
        }
    }
    // ==================== УДАЛЕНИЕ ====================

    /**
     * Удалить профиль при выходе
     */
    suspend fun deleteProfile(userId: Long) {
        withContext(Dispatchers.IO) {
            profileDao.deleteByUserId(userId)
            Log.d(TAG, "🗑️ Профиль удалён для userId: $userId")
        }
    }
}