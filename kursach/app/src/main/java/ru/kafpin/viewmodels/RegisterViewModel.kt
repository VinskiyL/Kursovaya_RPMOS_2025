package ru.kafpin.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import ru.kafpin.api.models.RegistrationRequest
import ru.kafpin.api.models.UserResponse
import ru.kafpin.data.LibraryDatabase
import ru.kafpin.data.RepositoryProvider

class RegisterViewModel(
    private val context: Context
) : ViewModel() {

    private val TAG = "RegisterViewModel"
    private val database = LibraryDatabase.getInstance(context)
    private val authRepository = RepositoryProvider.getAuthRepository(
        database = database,
        context = context
    )

    sealed class RegisterState {
        object Idle : RegisterState()
        object Loading : RegisterState()
        data class Success(val user: UserResponse, val message: String = "Регистрация успешна") : RegisterState()
        data class Error(val message: String) : RegisterState()
    }

    private val _registerState = MutableStateFlow<RegisterState>(RegisterState.Idle)
    val registerState: StateFlow<RegisterState> = _registerState

    fun register(request: RegistrationRequest) {
        Log.d(TAG, "🔄 Начинаем регистрацию для ${request.login}")
        _registerState.value = RegisterState.Loading

        viewModelScope.launch {
            try {
                val result = authRepository.register(request)

                if (result.isSuccess) {
                    val user = result.getOrThrow()
                    Log.d(TAG, "✅ Успех: ${user.login}")
                    _registerState.value = RegisterState.Success(user)
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Неизвестная ошибка"
                    Log.e(TAG, "❌ Ошибка: $error")
                    _registerState.value = RegisterState.Error(error)
                }
            } catch (e: Exception) {
                Log.e(TAG, "💥 Исключение при регистрации", e)
                _registerState.value = RegisterState.Error("Ошибка соединения с сервером")
            }
        }
    }
}