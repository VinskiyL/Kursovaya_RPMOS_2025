package ru.kafpin.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ru.kafpin.data.LibraryDatabase
import ru.kafpin.data.RepositoryProvider

class ProfileViewModel(context: Context) : ViewModel() {
    private val TAG = "ProfileViewModel"

    private val database = LibraryDatabase.getInstance(context)
    private val authRepository = RepositoryProvider.getAuthRepository(database, context)
    private val profileRepository = RepositoryProvider.getProfileRepository(
        database = database,
        authRepository = authRepository,
        context = context
    )

    private val networkMonitor = (context.applicationContext as ru.kafpin.MyApplication).networkMonitor

    private val _profile = MutableStateFlow<ru.kafpin.data.models.ProfileWithDetails?>(null)
    val profile: StateFlow<ru.kafpin.data.models.ProfileWithDetails?> = _profile.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isOnline = MutableStateFlow(false)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    init {
        // Подписка на сеть
        viewModelScope.launch {
            networkMonitor.isOnline.collect { online ->
                _isOnline.value = online
                Log.d(TAG, "Сеть: ${if (online) "ONLINE" else "OFFLINE"}")
            }
        }

        // ПОДПИСКА НА FLOW ПРОФИЛЯ
        viewModelScope.launch {
            try {
                val userId = authRepository.getCurrentUserId()
                if (userId == null) {
                    _errorMessage.value = "Пользователь не авторизован"
                    return@launch
                }

                // Подписываемся на Flow (автообновление UI)
                profileRepository.getProfileFlow(userId).collect { profileWithDetails ->
                    _profile.value = profileWithDetails

                    // 🔥 ИСПРАВЛЕНИЕ 1: Убрана автоматическая загрузка с сервера
                    // Теперь загрузка с сервера происходит только при явном вызове loadProfile()
                    // или если профиля совсем нет в БД
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка в Flow профиля", e)
            }
        }
    }

    // 🔥 ИСПРАВЛЕНИЕ 2: Новый метод loadProfile() по аналогии с OrderDetailsViewModel
    suspend fun loadProfile() {
        _isLoading.value = true
        _errorMessage.value = null

        try {
            val userId = authRepository.getCurrentUserId()
            if (userId == null) {
                _errorMessage.value = "Пользователь не авторизован"
                _isLoading.value = false
                return
            }

            // 1. Проверяем, есть ли профиль в локалке (Flow уже обновил _profile.value)
            val localProfile = _profile.value

            // 2. Если в локалке нет профиля И есть интернет → грузим с сервера
            if (localProfile == null && _isOnline.value) {
                Log.d(TAG, "Профиля нет в локальной БД, загружаем с сервера...")
                fetchProfileFromServer()
            }

            // 3. Если в локалке нет профиля И нет интернета → ошибка
            else if (localProfile == null && !_isOnline.value) {
                _errorMessage.value = "Нет подключения к интернету. Профиль не загружен."
            }

            // 4. Если в локалке есть профиль → всё ок
            else {
                Log.d(TAG, "Профиль загружен из локальной БД")
            }

        } catch (e: Exception) {
            _errorMessage.value = "Ошибка загрузки профиля: ${e.message}"
            Log.e(TAG, "Ошибка в loadProfile()", e)
        } finally {
            _isLoading.value = false
        }
    }

    // 🔥 ИСПРАВЛЕНИЕ 3: Переименован для ясности
    suspend fun fetchProfileFromServer() {
        _isLoading.value = true
        _errorMessage.value = null

        try {
            val result = profileRepository.fetchProfileFromServer()
            if (result.isFailure) {
                _errorMessage.value = result.exceptionOrNull()?.message ?: "Ошибка загрузки профиля"
            }
        } catch (e: Exception) {
            _errorMessage.value = "Ошибка: ${e.message}"
            Log.e(TAG, "Ошибка загрузки профиля с сервера", e)
        } finally {
            _isLoading.value = false
        }
    }

    // ==================== ОБНОВЛЕНИЕ ПРОФИЛЯ ====================
    suspend fun updateProfile(
        surname: String,
        name: String,
        patronymic: String?,
        birthday: String,
        education: String,
        profession: String?,
        educationalInst: String?,
        city: String,
        street: String,
        house: Int,
        buildingHouse: String?,
        flat: Int?,
        passportSeries: Int,
        passportNumber: Int,
        issuedByWhom: String,
        dateIssue: String,
        phone: String,
        mail: String
    ): Boolean {
        return try {
            if (!_isOnline.value) {
                _errorMessage.value = "Нет интернета. Редактирование невозможно."
                return false
            }

            val currentProfile = _profile.value?.profile
            if (currentProfile == null) {
                _errorMessage.value = "Профиль не загружен"
                return false
            }

            if (!authRepository.hasValidTokenForApi()) {
                _errorMessage.value = "Требуется авторизация"
                return false
            }

            val updatedProfile = currentProfile.copy(
                surname = surname,
                name = name,
                patronymic = patronymic,
                birthday = birthday,
                education = education,
                profession = profession,
                educationalInst = educationalInst,
                city = city,
                street = street,
                house = house,
                buildingHouse = buildingHouse,
                flat = flat,
                passportSeries = passportSeries,
                passportNumber = passportNumber,
                issuedByWhom = issuedByWhom,
                dateIssue = dateIssue,
                phone = phone,
                mail = mail
            )

            val result = profileRepository.updateProfile(updatedProfile)
            if (result.isSuccess) {
                true
            } else {
                _errorMessage.value = result.exceptionOrNull()?.message
                false
            }

        } catch (e: Exception) {
            _errorMessage.value = "Ошибка обновления: ${e.message}"
            Log.e(TAG, "Ошибка обновления профиля", e)
            false
        }
    }

    // ==================== ИЗМЕНЕНИЕ ЛОГИНА ====================
    suspend fun changeLogin(currentPassword: String, newLogin: String): Boolean {
        return try {
            if (!_isOnline.value) {
                _errorMessage.value = "Нет интернета. Изменение логина невозможно."
                return false
            }

            if (!authRepository.hasValidTokenForApi()) {
                _errorMessage.value = "Требуется авторизация"
                return false
            }

            val result = profileRepository.changeLogin(currentPassword, newLogin)
            if (result.isSuccess) {
                true
            } else {
                _errorMessage.value = result.exceptionOrNull()?.message
                false
            }

        } catch (e: Exception) {
            _errorMessage.value = "Ошибка смены логина: ${e.message}"
            Log.e(TAG, "Ошибка смены логина", e)
            false
        }
    }

    // ==================== ИЗМЕНЕНИЕ ПАРОЛЯ ====================
    suspend fun changePassword(currentPassword: String, newPassword: String): Boolean {
        return try {
            if (!_isOnline.value) {
                _errorMessage.value = "Нет интернета. Изменение пароля невозможно."
                return false
            }

            if (!authRepository.hasValidTokenForApi()) {
                _errorMessage.value = "Требуется авторизация"
                return false
            }

            val result = profileRepository.changePassword(currentPassword, newPassword)
            result.isSuccess

        } catch (e: Exception) {
            _errorMessage.value = "Ошибка смены пароля: ${e.message}"
            Log.e(TAG, "Ошибка смены пароля", e)
            false
        }
    }

    // ==================== УТИЛИТЫ ====================
    fun clearError() {
        _errorMessage.value = null
    }

    fun canEdit(): Boolean {
        return _isOnline.value
    }
}