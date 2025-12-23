package ru.kafpin.activities

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import ru.kafpin.api.models.RegistrationRequest
import ru.kafpin.databinding.ActivityRegisterBinding
import ru.kafpin.viewmodels.RegisterViewModel
import ru.kafpin.viewmodels.RegisterViewModelFactory
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class RegisterActivity : BaseActivity<ActivityRegisterBinding>() {

    private val viewModel: RegisterViewModel by viewModels {
        RegisterViewModelFactory(this)
    }

    private val educationOptions = listOf(
        "Нет образования",
        "Начальное общее (9 классов)",
        "Среднее общее (11 классов)",
        "Среднее профессиональное (колледж/техникум)",
        "Неоконченное высшее",
        "Высшее (бакалавриат)",
        "Высшее (специалитет)",
        "Высшее (магистратура)",
        "Учёная степень (кандидат/доктор наук)"
    )

    private var selectedBirthday: Calendar? = null
    private var selectedDateIssue: Calendar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun inflateBinding(): ActivityRegisterBinding {
        return ActivityRegisterBinding.inflate(layoutInflater)
    }

    override fun setupUI() {
        Log.d(TAG, "setupUI()")

        setToolbarTitle("Регистрация")
        setupToolbarButtons(
            showBackButton = true,
            showLogoutButton = false
        )

        setupEducationSpinner()
        setupDateButtons()
        setupRegisterForm()
        observeViewModel()
    }

    private fun setupEducationSpinner() {
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            educationOptions
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerEducation.adapter = adapter
    }

    private fun setupDateButtons() {
        binding.btnBirthday.setOnClickListener {
            showDatePickerDialog("birthday")
        }

        binding.etBirthday.setOnClickListener {
            showDatePickerDialog("birthday")
        }

        binding.btnDateIssue.setOnClickListener {
            showDatePickerDialog("dateIssue")
        }

        binding.etDateIssue.setOnClickListener {
            showDatePickerDialog("dateIssue")
        }
    }

    private fun showDatePickerDialog(type: String) {
        val calendar = if (type == "birthday") selectedBirthday else selectedDateIssue
        val initialYear = calendar?.get(Calendar.YEAR) ?: 2000
        val initialMonth = calendar?.get(Calendar.MONTH) ?: 0
        val initialDay = calendar?.get(Calendar.DAY_OF_MONTH) ?: 1

        val datePicker = DatePickerDialog(
            this,
            { _, year, month, day ->
                val selectedCalendar = Calendar.getInstance()
                selectedCalendar.set(year, month, day)

                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val dateString = dateFormat.format(selectedCalendar.time)

                if (type == "birthday") {
                    selectedBirthday = selectedCalendar
                    binding.etBirthday.setText(dateString)
                } else {
                    selectedDateIssue = selectedCalendar
                    binding.etDateIssue.setText(dateString)
                }
            },
            initialYear,
            initialMonth,
            initialDay
        )

        datePicker.show()
    }

    private fun setupRegisterForm() {
        binding.btnRegister.setOnClickListener {
            performRegistration()
        }

        binding.etConfirmPassword.setOnEditorActionListener { _, _, _ ->
            performRegistration()
            true
        }
    }

    private fun performRegistration() {
        clearFieldErrors()

        if (!validateForm()) {
            return
        }

        val request = createRegistrationRequest()
        viewModel.register(request)
    }

    private fun validateForm(): Boolean {
        var isValid = true

        if (binding.etSurname.text.toString().trim().isEmpty()) {
            binding.tilSurname.error = "Введите фамилию"
            isValid = false
        }

        if (binding.etName.text.toString().trim().isEmpty()) {
            binding.tilName.error = "Введите имя"
            isValid = false
        }

        val birthdayText = binding.etBirthday.text.toString().trim()
        if (birthdayText.isEmpty()) {
            binding.etBirthday.error = "Выберите дату рождения"
            isValid = false
        } else {
            selectedBirthday?.let { birthdayCalendar ->
                val today = Calendar.getInstance()

                // Проверка, что дата не в будущем
                if (birthdayCalendar.after(today)) {
                    binding.etBirthday.error = "Дата рождения не может быть в будущем"
                    isValid = false
                }

                // Проверка минимального возраста (14 лет)
                val minAgeCalendar = Calendar.getInstance().apply {
                    add(Calendar.YEAR, -14)
                }
                if (birthdayCalendar.after(minAgeCalendar)) {
                    binding.etBirthday.error = "Минимальный возраст - 18 лет"
                    isValid = false
                }

                // Проверка максимального возраста (120 лет)
                val maxAgeCalendar = Calendar.getInstance().apply {
                    add(Calendar.YEAR, -120)
                }
                if (birthdayCalendar.before(maxAgeCalendar)) {
                    binding.etBirthday.error = "Проверьте дату рождения"
                    isValid = false
                }
            } ?: run {
                // Если selectedBirthday не установлен, но поле заполнено
                try {
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    dateFormat.parse(birthdayText)
                    // Парсинг успешен, но нет Calendar объекта
                    // Можно создать Calendar здесь для проверок
                } catch (e: Exception) {
                    binding.etBirthday.error = "Неверный формат даты"
                    isValid = false
                }
            }
        }

        if (binding.spinnerEducation.selectedItemPosition == -1) {
            Toast.makeText(this, "Выберите образование", Toast.LENGTH_SHORT).show()
            isValid = false
        }

        if (binding.etCity.text.toString().trim().isEmpty()) {
            binding.tilCity.error = "Введите город"
            isValid = false
        }

        if (binding.etStreet.text.toString().trim().isEmpty()) {
            binding.tilStreet.error = "Введите улицу"
            isValid = false
        }

        val house = binding.etHouse.text.toString().trim().toIntOrNull()
        if (house == null || house < 1 || house > 1000) {
            binding.tilHouse.error = "Дом должен быть от 1 до 1000"
            isValid = false
        }

        val passportSeries = binding.etPassportSeries.text.toString().trim()
        if (passportSeries.isEmpty()) {
            binding.tilPassportSeries.error = "Введите серию паспорта"
            isValid = false
        } else if (passportSeries.length != 4 || passportSeries.toIntOrNull() == null) {
            binding.tilPassportSeries.error = "Серия должна быть 4 цифры"
            isValid = false
        }

        val passportNumber = binding.etPassportNumber.text.toString().trim()
        if (passportNumber.isEmpty()) {
            binding.tilPassportNumber.error = "Введите номер паспорта"
            isValid = false
        } else if (passportNumber.length != 6 || passportNumber.toIntOrNull() == null) {
            binding.tilPassportNumber.error = "Номер должен быть 6 цифр"
            isValid = false
        }

        if (binding.etIssuedByWhom.text.toString().trim().isEmpty()) {
            binding.tilIssuedByWhom.error = "Введите кем выдан"
            isValid = false
        }

        if (binding.etDateIssue.text.toString().trim().isEmpty()) {
            binding.etDateIssue.error = "Выберите дату выдачи"
            isValid = false
        }

        val phone = binding.etPhone.text.toString().trim()
        if (phone.isEmpty()) {
            binding.tilPhone.error = "Введите телефон"
            isValid = false
        } else if (!phone.matches(Regex("^\\+?[0-9]{10,15}$"))) {
            binding.tilPhone.error = "Неверный формат телефона"
            isValid = false
        }

        val email = binding.etMail.text.toString().trim()
        if (email.isEmpty()) {
            binding.tilMail.error = "Введите email"
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilMail.error = "Неверный формат email"
            isValid = false
        }

        val login = binding.etLogin.text.toString().trim()
        if (login.isEmpty()) {
            binding.tilLogin.error = "Введите логин"
            isValid = false
        }

        val password = binding.etPassword.text.toString()
        if (password.isEmpty()) {
            binding.tilPassword.error = "Введите пароль"
            isValid = false
        } else if (password.length < 6) {
            binding.tilPassword.error = "Пароль должен быть не менее 6 символов"
            isValid = false
        }

        val confirmPassword = binding.etConfirmPassword.text.toString()
        if (confirmPassword.isEmpty()) {
            binding.tilConfirmPassword.error = "Подтвердите пароль"
            isValid = false
        } else if (password != confirmPassword) {
            binding.tilConfirmPassword.error = "Пароли не совпадают"
            isValid = false
        }

        return isValid
    }

    private fun clearFieldErrors() {
        binding.tilSurname.error = null
        binding.tilName.error = null
        binding.etBirthday.error = null
        binding.tilCity.error = null
        binding.tilStreet.error = null
        binding.tilHouse.error = null
        binding.tilPassportSeries.error = null
        binding.tilPassportNumber.error = null
        binding.tilIssuedByWhom.error = null
        binding.etDateIssue.error = null
        binding.tilPhone.error = null
        binding.tilMail.error = null
        binding.tilLogin.error = null
        binding.tilPassword.error = null
        binding.tilConfirmPassword.error = null
    }

    private fun createRegistrationRequest(): RegistrationRequest {
        return RegistrationRequest(
            surname = binding.etSurname.text.toString().trim(),
            name = binding.etName.text.toString().trim(),
            patronymic = binding.etPatronymic.text.toString().trim().takeIf { it.isNotEmpty() },
            birthday = binding.etBirthday.text.toString().trim(),
            education = binding.spinnerEducation.selectedItem.toString(),
            profession = binding.etProfession.text.toString().trim().takeIf { it.isNotEmpty() },
            educationalInst = binding.etEducationalInst.text.toString().trim().takeIf { it.isNotEmpty() },
            city = binding.etCity.text.toString().trim(),
            street = binding.etStreet.text.toString().trim(),
            house = binding.etHouse.text.toString().toInt(),
            buildingHouse = binding.etBuildingHouse.text.toString().trim().takeIf { it.isNotEmpty() },
            flat = binding.etFlat.text.toString().toIntOrNull(),
            passportSeries = binding.etPassportSeries.text.toString().toInt(),
            passportNumber = binding.etPassportNumber.text.toString().toInt(),
            issuedByWhom = binding.etIssuedByWhom.text.toString().trim(),
            dateIssue = binding.etDateIssue.text.toString().trim(),
            phone = binding.etPhone.text.toString().trim(),
            login = binding.etLogin.text.toString().trim(),
            password = binding.etPassword.text.toString(),
            confirmPassword = binding.etConfirmPassword.text.toString(),
            mail = binding.etMail.text.toString().trim()
        )
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                viewModel.registerState.collect { state ->
                    when (state) {
                        is RegisterViewModel.RegisterState.Idle -> {
                            showLoading(false)
                            clearMessages()
                        }
                        is RegisterViewModel.RegisterState.Loading -> {
                            showLoading(true)
                            clearMessages()
                        }
                        is RegisterViewModel.RegisterState.Success -> {
                            showLoading(false)
                            showSuccess(state.message)

                            binding.root.postDelayed({
                                navigateToLogin(state.user.login)
                            }, 1500)
                        }
                        is RegisterViewModel.RegisterState.Error -> {
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
        binding.btnRegister.isEnabled = !isLoading
        binding.btnRegister.text = if (isLoading) "Регистрация..." else "Зарегистрироваться"
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

    private fun clearMessages() {
        binding.tvError.isVisible = false
        binding.tvSuccess.isVisible = false
    }

    private fun navigateToLogin(login: String) {
        LoginActivity.startWithLogin(this, login)
        finish()
    }

    companion object {
        private const val TAG = "RegisterActivity"

        fun start(context: AppCompatActivity) {
            val intent = android.content.Intent(context, RegisterActivity::class.java)
            context.startActivity(intent)
        }
    }
}