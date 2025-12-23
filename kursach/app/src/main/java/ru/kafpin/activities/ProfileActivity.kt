package ru.kafpin.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.kafpin.R
import ru.kafpin.databinding.ActivityProfileBinding
import ru.kafpin.ui.profile.EditProfileDialog
import ru.kafpin.viewmodels.ProfileViewModel
import ru.kafpin.viewmodels.ProfileViewModelFactory

class ProfileActivity : BaseActivity<ActivityProfileBinding>() {

    private val viewModel: ProfileViewModel by viewModels {
        ProfileViewModelFactory.getInstance(this)
    }

    override fun inflateBinding(): ActivityProfileBinding {
        return ActivityProfileBinding.inflate(layoutInflater)
    }

    override fun setupUI() {
        setupToolbarButtons(
            showBackButton = true,
            showLogoutButton = true
        )
        setToolbarTitle("Мой профиль")

        setupObservers()
        setupClickListeners()
        setupNetworkObserver()

        loadProfile()
    }

    private fun setupNetworkObserver() {
        lifecycleScope.launch {
            networkMonitor.isOnline.collect { isOnline ->
                updateButtonsVisibility(isOnline)
            }
        }
    }

    private fun updateButtonsVisibility(isOnline: Boolean) {
        with(binding) {
            btnEditProfile.isVisible = isOnline
            btnChangeLogin.isVisible = isOnline
            btnChangePassword.isVisible = isOnline
        }
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.profile.collect { profile ->
                profile?.let {
                    bindProfileDetails(it)
                    binding.contentLayout.isVisible = true
                    binding.errorLayout.isVisible = false
                    binding.progressBar.isVisible = false
                }
            }
        }

        lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                if (isLoading) {
                    binding.contentLayout.isVisible = false
                    binding.errorLayout.isVisible = false
                    binding.progressBar.isVisible = true
                }
            }
        }

        lifecycleScope.launch {
            viewModel.errorMessage.collect { error ->
                error?.let {
                    showErrorState(it)
                    viewModel.clearError()
                }
            }
        }

        lifecycleScope.launch {
            viewModel.isOnline.collect { isOnline ->
                updateButtonsVisibility(isOnline)
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnEditProfile.setOnClickListener {
            showEditProfileDialog()
        }

        binding.btnChangeLogin.setOnClickListener {
            showChangeLoginDialog()
        }

        binding.btnChangePassword.setOnClickListener {
            showChangePasswordDialog()
        }

        binding.btnRetry.setOnClickListener {
            loadProfile()
        }
    }

    private fun loadProfile() {
        lifecycleScope.launch {
            viewModel.loadProfile()
        }
    }

    private fun bindProfileDetails(profile: ru.kafpin.data.models.ProfileWithDetails) {
        with(binding) {
            tvFullName.text = profile.fullName
            tvBirthday.text = profile.profile.birthday
            tvEducation.text = profile.profile.education
            tvProfession.text = profile.profile.profession ?: "Не указано"
            tvEducationalInst.text = profile.profile.educationalInst ?: "Не указано"
            tvPassport.text = "${profile.profile.passportSeries} ${profile.profile.passportNumber}"
            tvIssuedBy.text = profile.profile.issuedByWhom
            tvDateIssue.text = profile.profile.dateIssue ?: "Не указано"
            tvAddress.text = profile.address
            tvPhone.text = profile.profile.phone
            tvEmail.text = profile.profile.mail
            tvLogin.text = profile.profile.login
        }
    }

    private fun showEditProfileDialog() {
        viewModel.profile.value?.let { profile ->
            val dialog = EditProfileDialog.newInstance(
                surname = profile.profile.surname,
                name = profile.profile.name,
                patronymic = profile.profile.patronymic,
                birthday = profile.profile.birthday,
                education = profile.profile.education,
                profession = profile.profile.profession,
                educationalInst = profile.profile.educationalInst,
                city = profile.profile.city,
                street = profile.profile.street,
                house = profile.profile.house,
                buildingHouse = profile.profile.buildingHouse,
                flat = profile.profile.flat,
                passportSeries = profile.profile.passportSeries,
                passportNumber = profile.profile.passportNumber,
                issuedByWhom = profile.profile.issuedByWhom,
                dateIssue = profile.profile.dateIssue,
                phone = profile.profile.phone,
                mail = profile.profile.mail
            )

            dialog.setOnProfileUpdatedListener { profileData ->
                lifecycleScope.launch {
                    val success = viewModel.updateProfile(
                        surname = profileData.surname,
                        name = profileData.name,
                        patronymic = profileData.patronymic,
                        birthday = profileData.birthday,
                        education = profileData.education,
                        profession = profileData.profession,
                        educationalInst = profileData.educationalInst,
                        city = profileData.city,
                        street = profileData.street,
                        house = profileData.house,
                        buildingHouse = profileData.buildingHouse,
                        flat = profileData.flat,
                        passportSeries = profileData.passportSeries,
                        passportNumber = profileData.passportNumber,
                        issuedByWhom = profileData.issuedByWhom,
                        dateIssue = profileData.dateIssue,
                        phone = profileData.phone,
                        mail = profileData.mail
                    )

                    if (success) {
                        Toast.makeText(
                            this@ProfileActivity,
                            "Профиль обновлён",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            this@ProfileActivity,
                            "Ошибка обновления",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }

            dialog.show(supportFragmentManager, "EditProfileDialog")
        }
    }

    private fun showErrorState(message: String) {
        binding.errorText.text = message
        binding.errorLayout.isVisible = true
        binding.contentLayout.isVisible = false
        binding.progressBar.isVisible = false
    }

    private fun lockUIForLogout() {
        // Блокируем кнопки в контенте
        binding.btnEditProfile.isEnabled = false
        binding.btnChangeLogin.isEnabled = false
        binding.btnChangePassword.isEnabled = false
        binding.btnRetry.isEnabled = false

        // Блокируем кнопки в тулбаре
        btnBack.isEnabled = false
        btnLogout.isEnabled = false

        // Меняем заголовок
        setToolbarTitle("Выход через 5 сек...")
    }

    private fun unlockUI() {
        binding.btnEditProfile.isEnabled = true
        binding.btnChangeLogin.isEnabled = true
        binding.btnChangePassword.isEnabled = true
        binding.btnRetry.isEnabled = true

        btnBack.isEnabled = true
        btnLogout.isEnabled = true

        setToolbarTitle("Мой профиль")
    }

    private fun scheduleAutoLogout(message: String) {
        Toast.makeText(
            this,
            "$message Выход через 5 секунд...",
            Toast.LENGTH_LONG
        ).show()

        lockUIForLogout()

        lifecycleScope.launch {
            delay(5000) // 5 секунд

            performLogout()
        }
    }

    private fun showChangeLoginDialog() {
        val builder = AlertDialog.Builder(this)
        val view = layoutInflater.inflate(R.layout.dialog_change_login, null)
        builder.setView(view)
        builder.setTitle("Изменить логин")
        builder.setPositiveButton("Изменить") { dialog, _ ->
            val etCurrentPassword = view.findViewById<android.widget.EditText>(R.id.etCurrentPassword)
            val etNewLogin = view.findViewById<android.widget.EditText>(R.id.etNewLogin)

            val currentPassword = etCurrentPassword.text.toString()
            val newLogin = etNewLogin.text.toString()

            if (currentPassword.isEmpty()) {
                Toast.makeText(this, "Введите текущий пароль", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }
            if (newLogin.isEmpty()) {
                Toast.makeText(this, "Введите новый логин", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }

            lifecycleScope.launch {
                val success = viewModel.changeLogin(currentPassword, newLogin)
                if (success) {

                    scheduleAutoLogout("Логин изменён.")
                } else {
                    Toast.makeText(
                        this@ProfileActivity,
                        "Ошибка изменения логина",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("Отмена", null)
        builder.create().show()
    }

    private fun showChangePasswordDialog() {
        val builder = AlertDialog.Builder(this)
        val view = layoutInflater.inflate(R.layout.dialog_change_password, null)
        builder.setView(view)
        builder.setTitle("Изменить пароль")
        builder.setPositiveButton("Изменить") { dialog, _ ->
            val etCurrentPassword = view.findViewById<android.widget.EditText>(R.id.etCurrentPassword)
            val etNewPassword = view.findViewById<android.widget.EditText>(R.id.etNewPassword)
            val etConfirmPassword = view.findViewById<android.widget.EditText>(R.id.etConfirmPassword)

            val currentPassword = etCurrentPassword.text.toString()
            val newPassword = etNewPassword.text.toString()
            val confirmPassword = etConfirmPassword.text.toString()

            if (currentPassword.isEmpty()) {
                Toast.makeText(this, "Введите текущий пароль", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }
            if (newPassword.isEmpty()) {
                Toast.makeText(this, "Введите новый пароль", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }
            if (newPassword.length < 6) {
                Toast.makeText(this, "Пароль должен быть не менее 6 символов", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }
            if (newPassword != confirmPassword) {
                Toast.makeText(this, "Пароли не совпадают", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }

            lifecycleScope.launch {
                val success = viewModel.changePassword(currentPassword, newPassword)
                if (success) {
                    scheduleAutoLogout("Пароль изменён.")
                } else {
                    Toast.makeText(
                        this@ProfileActivity,
                        "Ошибка изменения пароля",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("Отмена", null)
        builder.create().show()
    }

    companion object {
        fun start(context: android.content.Context) {
            val intent = Intent(context, ProfileActivity::class.java)
            context.startActivity(intent)
        }
    }
}