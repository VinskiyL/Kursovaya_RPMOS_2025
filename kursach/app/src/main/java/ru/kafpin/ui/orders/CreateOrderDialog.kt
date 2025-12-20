package ru.kafpin.ui.orders

import android.app.Dialog
import android.os.Bundle
import android.text.InputType
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import ru.kafpin.databinding.DialogCreateOrderBinding

class CreateOrderDialog : DialogFragment() {

    private lateinit var binding: DialogCreateOrderBinding

    private var isEditMode = false
    private var orderId: Long = -1L

    private var onOrderCreated: ((OrderData) -> Unit)? = null
    private var onOrderUpdated: ((Long, OrderData) -> Unit)? = null

    data class OrderData(
        val title: String,
        val authorSurname: String,
        val authorName: String?,
        val authorPatronymic: String?,
        val quantity: Int,
        val year: String?
    )

    companion object {
        private const val ARG_ORDER_ID = "order_id"
        private const val ARG_TITLE = "title"
        private const val ARG_AUTHOR_SURNAME = "author_surname"
        private const val ARG_AUTHOR_NAME = "author_name"
        private const val ARG_AUTHOR_PATRONYMIC = "author_patronymic"
        private const val ARG_QUANTITY = "quantity"
        private const val ARG_YEAR = "year"

        fun newInstance(): CreateOrderDialog {
            return CreateOrderDialog()
        }

        fun editInstance(
            orderId: Long,
            title: String,
            authorSurname: String,
            authorName: String?,
            authorPatronymic: String?,
            quantity: Int,
            year: String?
        ): CreateOrderDialog {
            return CreateOrderDialog().apply {
                arguments = Bundle().apply {
                    putLong(ARG_ORDER_ID, orderId)
                    putString(ARG_TITLE, title)
                    putString(ARG_AUTHOR_SURNAME, authorSurname)
                    putString(ARG_AUTHOR_NAME, authorName)
                    putString(ARG_AUTHOR_PATRONYMIC, authorPatronymic)
                    putInt(ARG_QUANTITY, quantity)
                    putString(ARG_YEAR, year)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            orderId = it.getLong(ARG_ORDER_ID, -1L)
            if (orderId != -1L) {
                isEditMode = true
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = DialogCreateOrderBinding.inflate(layoutInflater)

        setupFields()
        setupValidation()

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .setTitle(if (isEditMode) "Редактировать заказ" else "Новый заказ")
            .setPositiveButton(if (isEditMode) "Сохранить" else "Создать", null)
            .setNegativeButton("Отмена") { dialog, _ -> dialog.dismiss() }
            .create()

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                if (validateInput()) {
                    val orderData = getOrderData()
                    if (isEditMode) {
                        onOrderUpdated?.invoke(orderId, orderData)
                    } else {
                        onOrderCreated?.invoke(orderData)
                    }
                    dialog.dismiss()
                }
            }
        }

        return dialog
    }

    private fun setupFields() {
        if (isEditMode) {
            arguments?.let {
                binding.etTitle.setText(it.getString(ARG_TITLE, ""))
                binding.etAuthorSurname.setText(it.getString(ARG_AUTHOR_SURNAME, ""))
                binding.etAuthorName.setText(it.getString(ARG_AUTHOR_NAME))
                binding.etAuthorPatronymic.setText(it.getString(ARG_AUTHOR_PATRONYMIC))
                binding.etQuantity.setText(it.getInt(ARG_QUANTITY, 1).toString())
                binding.etYear.setText(it.getString(ARG_YEAR))
            }
        }

        // Ограничение ввода года только цифрами
        binding.etYear.inputType = InputType.TYPE_CLASS_NUMBER

        // Фокус на первом поле
        if (!isEditMode) {
            binding.etTitle.requestFocus()
        }
    }

    private fun setupValidation() {
        // Реальная валидация делается в validateInput()
        // Здесь можно добавить подсказки при вводе
        binding.etYear.doAfterTextChanged { text ->
            text?.toString()?.let { year ->
                if (year.isNotEmpty() && year.length > 4) {
                    binding.etYear.setText(year.take(4))
                    binding.etYear.setSelection(4)
                }
            }
        }
    }

    private fun getOrderData(): OrderData {
        return OrderData(
            title = binding.etTitle.text.toString().trim(),
            authorSurname = binding.etAuthorSurname.text.toString().trim(),
            authorName = binding.etAuthorName.text.toString().trim().takeIf { it.isNotEmpty() },
            authorPatronymic = binding.etAuthorPatronymic.text.toString().trim().takeIf { it.isNotEmpty() },
            quantity = binding.etQuantity.text.toString().toIntOrNull() ?: 1,
            year = binding.etYear.text.toString().trim().takeIf { it.isNotEmpty() }
        )
    }

    private fun validateInput(): Boolean {
        val title = binding.etTitle.text.toString().trim()
        val authorSurname = binding.etAuthorSurname.text.toString().trim()
        val quantityText = binding.etQuantity.text.toString().trim()
        val year = binding.etYear.text.toString().trim()

        if (title.isEmpty()) {
            showError("Введите название книги")
            binding.etTitle.requestFocus()
            return false
        }

        if (title.length > 200) {
            showError("Название слишком длинное (макс. 200 символов)")
            binding.etTitle.requestFocus()
            return false
        }

        if (authorSurname.isEmpty()) {
            showError("Введите фамилию автора")
            binding.etAuthorSurname.requestFocus()
            return false
        }

        if (authorSurname.length > 100) {
            showError("Фамилия слишком длинная (макс. 100 символов)")
            binding.etAuthorSurname.requestFocus()
            return false
        }

        binding.etAuthorName.text.toString().trim().let { name ->
            if (name.isNotEmpty() && name.length > 100) {
                showError("Имя слишком длинное (макс. 100 символов)")
                binding.etAuthorName.requestFocus()
                return false
            }
        }

        binding.etAuthorPatronymic.text.toString().trim().let { patronymic ->
            if (patronymic.isNotEmpty() && patronymic.length > 100) {
                showError("Отчество слишком длинное (макс. 100 символов)")
                binding.etAuthorPatronymic.requestFocus()
                return false
            }
        }

        val quantity = quantityText.toIntOrNull()
        if (quantity == null) {
            showError("Введите количество книг")
            binding.etQuantity.requestFocus()
            return false
        }

        if (quantity < 1 || quantity > 5) {
            showError("Количество должно быть от 1 до 5")
            binding.etQuantity.requestFocus()
            binding.etQuantity.selectAll()
            return false
        }

        if (year.isNotEmpty()) {
            if (!year.matches(Regex("\\d{4}"))) {
                showError("Год должен содержать 4 цифры")
                binding.etYear.requestFocus()
                binding.etYear.selectAll()
                return false
            }

            val yearNum = year.toInt()
            if (yearNum < 1000 || yearNum > 2100) {
                showError("Введите корректный год (1000-2100)")
                binding.etYear.requestFocus()
                binding.etYear.selectAll()
                return false
            }
        }

        return true
    }

    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    fun setOnOrderCreatedListener(listener: (OrderData) -> Unit) {
        onOrderCreated = listener
    }

    fun setOnOrderUpdatedListener(listener: (Long, OrderData) -> Unit) {
        onOrderUpdated = listener
    }
}