package ru.kafpin.ui.profile

import android.app.Dialog
import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import ru.kafpin.databinding.DialogEditProfileBinding

/**
 * ПОЛНЫЙ диалог для редактирования профиля.
 * Все поля, вся валидация по аналогии с CreateOrderDialog.
 */
class EditProfileDialog : DialogFragment() {

    private lateinit var binding: DialogEditProfileBinding

    data class ProfileData(
        val surname: String,
        val name: String,
        val patronymic: String?,
        val birthday: String,
        val education: String,
        val profession: String?,
        val educationalInst: String?,
        val city: String,
        val street: String,
        val house: Int,
        val buildingHouse: String?,
        val flat: Int?,
        val passportSeries: Int,
        val passportNumber: Int,
        val issuedByWhom: String,
        val dateIssue: String,
        val phone: String,
        val mail: String
    )

    companion object {
        private const val ARG_SURNAME = "surname"
        private const val ARG_NAME = "name"
        private const val ARG_PATRONYMIC = "patronymic"
        private const val ARG_BIRTHDAY = "birthday"
        private const val ARG_EDUCATION = "education"
        private const val ARG_PROFESSION = "profession"
        private const val ARG_EDUCATIONAL_INST = "educational_inst"
        private const val ARG_CITY = "city"
        private const val ARG_STREET = "street"
        private const val ARG_HOUSE = "house"
        private const val ARG_BUILDING_HOUSE = "building_house"
        private const val ARG_FLAT = "flat"
        private const val ARG_PASSPORT_SERIES = "passport_series"
        private const val ARG_PASSPORT_NUMBER = "passport_number"
        private const val ARG_ISSUED_BY_WHOM = "issued_by_whom"
        private const val ARG_DATE_ISSUE = "date_issue"
        private const val ARG_PHONE = "phone"
        private const val ARG_MAIL = "mail"

        fun newInstance(
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
        ): EditProfileDialog {
            return EditProfileDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_SURNAME, surname)
                    putString(ARG_NAME, name)
                    putString(ARG_PATRONYMIC, patronymic)
                    putString(ARG_BIRTHDAY, birthday)
                    putString(ARG_EDUCATION, education)
                    putString(ARG_PROFESSION, profession)
                    putString(ARG_EDUCATIONAL_INST, educationalInst)
                    putString(ARG_CITY, city)
                    putString(ARG_STREET, street)
                    putInt(ARG_HOUSE, house)
                    putString(ARG_BUILDING_HOUSE, buildingHouse)
                    putInt(ARG_FLAT, flat ?: -1)
                    putInt(ARG_PASSPORT_SERIES, passportSeries)
                    putInt(ARG_PASSPORT_NUMBER, passportNumber)
                    putString(ARG_ISSUED_BY_WHOM, issuedByWhom)
                    putString(ARG_DATE_ISSUE, dateIssue)
                    putString(ARG_PHONE, phone)
                    putString(ARG_MAIL, mail)
                }
            }
        }
    }

    private var onProfileUpdated: ((ProfileData) -> Unit)? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = DialogEditProfileBinding.inflate(layoutInflater)

        setupFields()
        setupValidation()

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .setTitle("Редактировать профиль")
            .setPositiveButton("Сохранить", null)
            .setNegativeButton("Отмена") { dialog, _ -> dialog.dismiss() }
            .create()

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                if (validateInput()) {
                    val profileData = getProfileData()
                    onProfileUpdated?.invoke(profileData)
                    dialog.dismiss()
                }
            }
        }

        return dialog
    }

    private fun setupFields() {
        arguments?.let { args ->
            with(binding) {
                etSurname.setText(args.getString(ARG_SURNAME, ""))
                etName.setText(args.getString(ARG_NAME, ""))
                etPatronymic.setText(args.getString(ARG_PATRONYMIC))
                etBirthday.setText(args.getString(ARG_BIRTHDAY, ""))
                etEducation.setText(args.getString(ARG_EDUCATION, ""))
                etProfession.setText(args.getString(ARG_PROFESSION))
                etEducationalInst.setText(args.getString(ARG_EDUCATIONAL_INST))
                etCity.setText(args.getString(ARG_CITY, ""))
                etStreet.setText(args.getString(ARG_STREET, ""))
                etHouse.setText(args.getInt(ARG_HOUSE).toString())
                etBuildingHouse.setText(args.getString(ARG_BUILDING_HOUSE))
                val flat = args.getInt(ARG_FLAT)
                if (flat != -1) etFlat.setText(flat.toString())
                etPassportSeries.setText(args.getInt(ARG_PASSPORT_SERIES).toString())
                etPassportNumber.setText(args.getInt(ARG_PASSPORT_NUMBER).toString())
                etIssuedByWhom.setText(args.getString(ARG_ISSUED_BY_WHOM, ""))
                etDateIssue.setText(args.getString(ARG_DATE_ISSUE, ""))
                etPhone.setText(args.getString(ARG_PHONE, ""))
                etMail.setText(args.getString(ARG_MAIL, ""))
            }
        }

        // Фокус на первом поле (как в CreateOrderDialog)
        binding.etSurname.requestFocus()
    }

    private fun setupValidation() {
        // Ограничение ввода для числовых полей (как year в заказе)
        binding.etPassportSeries.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val text = binding.etPassportSeries.text?.toString() ?: ""
                if (text.isNotEmpty() && text.length > 4) {
                    binding.etPassportSeries.setText(text.take(4))
                    binding.etPassportSeries.setSelection(4)
                }
            }
        }

        binding.etPassportNumber.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val text = binding.etPassportNumber.text?.toString() ?: ""
                if (text.isNotEmpty() && text.length > 6) {
                    binding.etPassportNumber.setText(text.take(6))
                    binding.etPassportNumber.setSelection(6)
                }
            }
        }
    }

    private fun getProfileData(): ProfileData {
        return ProfileData(
            surname = binding.etSurname.text.toString().trim(),
            name = binding.etName.text.toString().trim(),
            patronymic = binding.etPatronymic.text.toString().trim().takeIf { it.isNotEmpty() },
            birthday = binding.etBirthday.text.toString().trim(),
            education = binding.etEducation.text.toString().trim(),
            profession = binding.etProfession.text.toString().trim().takeIf { it.isNotEmpty() },
            educationalInst = binding.etEducationalInst.text.toString().trim().takeIf { it.isNotEmpty() },
            city = binding.etCity.text.toString().trim(),
            street = binding.etStreet.text.toString().trim(),
            house = binding.etHouse.text.toString().toIntOrNull() ?: 1,
            buildingHouse = binding.etBuildingHouse.text.toString().trim().takeIf { it.isNotEmpty() },
            flat = binding.etFlat.text.toString().toIntOrNull(),
            passportSeries = binding.etPassportSeries.text.toString().toIntOrNull() ?: 0,
            passportNumber = binding.etPassportNumber.text.toString().toIntOrNull() ?: 0,
            issuedByWhom = binding.etIssuedByWhom.text.toString().trim(),
            dateIssue = binding.etDateIssue.text.toString().trim(),
            phone = binding.etPhone.text.toString().trim(),
            mail = binding.etMail.text.toString().trim()
        )
    }

    private fun validateInput(): Boolean {
        // 1. Проверка обязательных полей
        val surname = binding.etSurname.text.toString().trim()
        if (surname.isEmpty()) {
            showError("Введите фамилию")
            binding.etSurname.requestFocus()
            return false
        }
        if (surname.length > 50) {
            showError("Фамилия слишком длинная (макс. 50 символов)")
            binding.etSurname.requestFocus()
            return false
        }

        val name = binding.etName.text.toString().trim()
        if (name.isEmpty()) {
            showError("Введите имя")
            binding.etName.requestFocus()
            return false
        }
        if (name.length > 50) {
            showError("Имя слишком длинное (макс. 50 символов)")
            binding.etName.requestFocus()
            return false
        }

        val patronymic = binding.etPatronymic.text.toString().trim()
        if (patronymic.isNotEmpty() && patronymic.length > 50) {
            showError("Отчество слишком длинное (макс. 50 символов)")
            binding.etPatronymic.requestFocus()
            return false
        }

        // 2. Проверка даты рождения
        val birthday = binding.etBirthday.text.toString().trim()
        if (birthday.isEmpty()) {
            showError("Введите дату рождения")
            binding.etBirthday.requestFocus()
            return false
        }
        if (!birthday.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) {
            showError("Дата рождения должна быть в формате ГГГГ-ММ-ДД")
            binding.etBirthday.requestFocus()
            return false
        }

        // 3. Проверка образования
        val education = binding.etEducation.text.toString().trim()
        if (education.isEmpty()) {
            showError("Введите образование")
            binding.etEducation.requestFocus()
            return false
        }
        if (education.length > 100) {
            showError("Образование слишком длинное (макс. 100 символов)")
            binding.etEducation.requestFocus()
            return false
        }

        // 4. Проверка профессии (необязательно)
        val profession = binding.etProfession.text.toString().trim()
        if (profession.isNotEmpty() && profession.length > 100) {
            showError("Профессия слишком длинная (макс. 100 символов)")
            binding.etProfession.requestFocus()
            return false
        }

        // 5. Проверка учебного заведения (необязательно)
        val educationalInst = binding.etEducationalInst.text.toString().trim()
        if (educationalInst.isNotEmpty() && educationalInst.length > 200) {
            showError("Учебное заведение слишком длинное (макс. 200 символов)")
            binding.etEducationalInst.requestFocus()
            return false
        }

        // 6. Проверка адреса
        val city = binding.etCity.text.toString().trim()
        if (city.isEmpty()) {
            showError("Введите город")
            binding.etCity.requestFocus()
            return false
        }

        val street = binding.etStreet.text.toString().trim()
        if (street.isEmpty()) {
            showError("Введите улицу")
            binding.etStreet.requestFocus()
            return false
        }

        val house = binding.etHouse.text.toString().toIntOrNull()
        if (house == null || house < 1 || house > 1000) {
            showError("Номер дома должен быть от 1 до 1000")
            binding.etHouse.requestFocus()
            binding.etHouse.selectAll()
            return false
        }

        val buildingHouse = binding.etBuildingHouse.text.toString().trim()
        if (buildingHouse.isNotEmpty() && buildingHouse.length > 10) {
            showError("Корпус слишком длинный (макс. 10 символов)")
            binding.etBuildingHouse.requestFocus()
            return false
        }

        val flat = binding.etFlat.text.toString().toIntOrNull()
        if (flat != null && (flat < 1 || flat > 1000)) {
            showError("Номер квартиры должен быть от 1 до 1000")
            binding.etFlat.requestFocus()
            binding.etFlat.selectAll()
            return false
        }

        // 7. Проверка паспорта
        val passportSeries = binding.etPassportSeries.text.toString().toIntOrNull()
        if (passportSeries == null || passportSeries < 1000 || passportSeries > 9999) {
            showError("Серия паспорта должна быть 4 цифры (1000-9999)")
            binding.etPassportSeries.requestFocus()
            binding.etPassportSeries.selectAll()
            return false
        }

        val passportNumber = binding.etPassportNumber.text.toString().toIntOrNull()
        if (passportNumber == null || passportNumber < 100000 || passportNumber > 999999) {
            showError("Номер паспорта должен быть 6 цифр (100000-999999)")
            binding.etPassportNumber.requestFocus()
            binding.etPassportNumber.selectAll()
            return false
        }

        // 8. Проверка "Кем выдан"
        val issuedByWhom = binding.etIssuedByWhom.text.toString().trim()
        if (issuedByWhom.isEmpty()) {
            showError("Введите кем выдан паспорт")
            binding.etIssuedByWhom.requestFocus()
            return false
        }
        if (issuedByWhom.length > 200) {
            showError("Поле 'Кем выдан' слишком длинное (макс. 200 символов)")
            binding.etIssuedByWhom.requestFocus()
            return false
        }

        // 9. Проверка даты выдачи
        val dateIssue = binding.etDateIssue.text.toString().trim()
        if (dateIssue.isEmpty()) {
            showError("Введите дату выдачи паспорта")
            binding.etDateIssue.requestFocus()
            return false
        }
        if (!dateIssue.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) {
            showError("Дата выдачи должна быть в формате ГГГГ-ММ-ДД")
            binding.etDateIssue.requestFocus()
            return false
        }

        // 10. Проверка телефона
        val phone = binding.etPhone.text.toString().trim()
        if (phone.isEmpty()) {
            showError("Введите телефон")
            binding.etPhone.requestFocus()
            return false
        }
        if (!phone.matches(Regex("^\\+?[0-9]{10,15}$"))) {
            showError("Неверный формат телефона")
            binding.etPhone.requestFocus()
            return false
        }

        // 11. Проверка email
        val email = binding.etMail.text.toString().trim()
        if (email.isEmpty()) {
            showError("Введите email")
            binding.etMail.requestFocus()
            return false
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError("Неверный формат email")
            binding.etMail.requestFocus()
            return false
        }

        return true
    }

    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    fun setOnProfileUpdatedListener(listener: (ProfileData) -> Unit) {
        onProfileUpdated = listener
    }
}