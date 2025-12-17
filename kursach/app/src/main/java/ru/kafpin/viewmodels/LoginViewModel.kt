package ru.kafpin.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import ru.kafpin.data.LibraryDatabase
import ru.kafpin.data.RepositoryProvider

class LoginViewModel(
    private val context: Context
) : ViewModel() {

    private val TAG = "LoginViewModel"
    private val database = LibraryDatabase.getInstance(context)
    private val authRepository = RepositoryProvider.getAuthRepository(
        database = database,
        context = context
    )
    sealed class LoginState {
        object Idle : LoginState()
        object Loading : LoginState()
        data class Success(val message: String) : LoginState()
        data class Error(val message: String) : LoginState()
    }

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState

    var login: String = ""
    var password: String = ""

    suspend fun checkExistingSession(): Boolean {
        return try {
            val hasUser = authRepository.isAuthenticated()
            if (!hasUser) {
                Log.d(TAG, "❌ Пользователя нет в БД")
                return false
            }

            val canContinue = authRepository.canContinueWithoutRelogin()

            if (!canContinue) {
                Log.w(TAG, "⚠️ Сессия скоро истечёт или истекла, требуется перелогин")
                authRepository.forceLogout()
                return false
            }

            Log.d(TAG, "✅ Пользователь в БД, сессия активна")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при проверке сессии", e)
            false
        }
    }

    fun performLogin() {
        if (login.isBlank() || password.isBlank()) {
            _loginState.value = LoginState.Error("Заполните все поля")
            return
        }

        Log.d(TAG, "Логин: $login")
        _loginState.value = LoginState.Loading

        viewModelScope.launch {
            try {
                val result = authRepository.login(login, password)

                if (result.isSuccess) {
                    val user = result.getOrThrow()
                    Log.d(TAG, "Успех: ${user.login}")

                    val sessionInfo = authRepository.getSessionInfo()
                    Log.d(TAG, "Информация о сессии после логина: $sessionInfo")

                    _loginState.value = LoginState.Success("Добро пожаловать!")
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Ошибка"
                    Log.e(TAG, "Ошибка: $error")
                    _loginState.value = LoginState.Error(error)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка соединения", e)
                _loginState.value = LoginState.Error("Нет соединения с сервером")
            }
        }
    }

    suspend fun checkTokenValidity(): Boolean {
        return try {
            val isValid = authRepository.hasValidTokenForApi()
            Log.d(TAG, "Проверка токена для API: $isValid")

            if (!isValid) {
                val sessionInfo = authRepository.getSessionInfo()
                Log.w(TAG, "Токен невалиден для API. Информация: $sessionInfo")
            }

            isValid
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при проверке токена", e)
            false
        }
    }

    suspend fun tryRefreshToken(): Boolean {
        return try {
            authRepository.refreshTokenIfNeeded()
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при обновлении токена", e)
            false
        }
    }
    suspend fun performLogout() {
        try {
            authRepository.forceLogout()
            Log.d(TAG, "✅ Принудительный выход выполнен")
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при принудительном выходе", e)
        }
    }
}