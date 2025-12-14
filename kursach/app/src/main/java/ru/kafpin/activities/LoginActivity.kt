package ru.kafpin.activities

import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import ru.kafpin.databinding.ActivityLoginBinding
import ru.kafpin.viewmodels.LoginViewModel
import ru.kafpin.viewmodels.LoginViewModelFactory

class LoginActivity : BaseActivity<ActivityLoginBinding>() {

    private val viewModel: LoginViewModel by viewModels {
        LoginViewModelFactory(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun inflateBinding(): ActivityLoginBinding {
        return ActivityLoginBinding.inflate(layoutInflater)
    }

    override fun setupUI() {
        Log.d(TAG, "setupUI()")

        setToolbarTitle("Вход в систему")
        setupToolbarButtons(
            showBackButton = false,
            showLogoutButton = false
        )

        setupLoginForm()
        observeViewModel()
        checkAuthentication()
    }

    private fun checkAuthentication() {
        lifecycleScope.launch {
            Log.d(TAG, "🔍 Проверяем аутентификацию...")

            val hasValidSession = viewModel.checkExistingSession()
            Log.d(TAG, "hasValidSession: $hasValidSession")

            if (!hasValidSession) {
                Log.d(TAG, "❌ Нет валидной сессии, показываем форму входа")
                return@launch
            }
            checkTokenStatusAndNavigate()
        }
    }

    private suspend fun checkTokenStatusAndNavigate() {
        val isOnline = networkMonitor.isOnline.value
        Log.d(TAG, "📶 Статус сети: ${if (isOnline) "онлайн" else "офлайн"}")

        if (isOnline) {
            val canMakeApiCalls = viewModel.checkTokenValidity()
            Log.d(TAG, "Можно делать API запросы: $canMakeApiCalls")

            if (!canMakeApiCalls) {
                Log.w(TAG, "🔄 Пробуем обновить токен...")
                val refreshSuccess = viewModel.tryRefreshToken()

                if (!refreshSuccess) {
                    Log.e(TAG, "❌ Не удалось обновить токен, разлогиниваем")
                    viewModel.performLogout()
                    showWarning("Сессия истекла. Войдите заново.")
                    return
                } else {
                    Log.d(TAG, "✅ Токен обновлён")
                }
            }
        } else {
            showWarning("Нет подключения к сети. Работаем в офлайн-режиме.")
        }

        navigateToMain()
    }

    private fun setupLoginForm() {
        binding.btnLogin.setOnClickListener {
            performLogin()
        }

        binding.etPassword.setOnEditorActionListener { _, _, _ ->
            performLogin()
            true
        }
    }

    private fun performLogin() {
        val login = binding.etLogin.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        if (login.isEmpty() || password.isEmpty()) {
            showError("Заполните все поля")
            return
        }

        viewModel.login = login
        viewModel.password = password
        viewModel.performLogin()
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                viewModel.loginState.collect { state ->
                    when (state) {
                        is LoginViewModel.LoginState.Idle -> {
                            showLoading(false)
                            clearMessages()
                        }
                        is LoginViewModel.LoginState.Loading -> {
                            showLoading(true)
                            clearMessages()
                        }
                        is LoginViewModel.LoginState.Success -> {
                            showLoading(false)
                            showSuccess(state.message)

                            binding.root.postDelayed({
                                navigateToMain()
                            }, 1500)
                        }
                        is LoginViewModel.LoginState.Error -> {
                            showLoading(false)
                            showError(state.message)
                        }
                    }
                }
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.isVisible = isLoading
        binding.btnLogin.isEnabled = !isLoading
        binding.btnLogin.text = if (isLoading) "Вход..." else "Войти"
    }

    private fun showError(message: String) {
        binding.tvError.text = message
        binding.tvError.isVisible = true
        binding.tvSuccess.isVisible = false
    }

    private fun showSuccess(message: String) {
        binding.tvSuccess.text = message
        binding.tvSuccess.isVisible = true
        binding.tvError.isVisible = false
    }

    private fun showWarning(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun clearMessages() {
        binding.tvError.isVisible = false
        binding.tvSuccess.isVisible = false
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            context.startActivity(intent)
        }
    }
}