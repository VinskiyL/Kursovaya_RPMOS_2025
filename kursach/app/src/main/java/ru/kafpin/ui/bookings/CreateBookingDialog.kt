package ru.kafpin.ui.bookings

import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import ru.kafpin.databinding.DialogCreateBookingBinding
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar

class CreateBookingDialog : DialogFragment() {

    private lateinit var binding: DialogCreateBookingBinding

    private var bookId: Long = -1L
    private var bookTitle: String = ""
    private var quantityRemaining: Int = 0

    private val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    private var selectedDateIssue: LocalDate? = null
    private var selectedDateReturn: LocalDate? = null

    private var onBookingCreated: ((bookId: Long, quantity: Int, dateIssue: LocalDate, dateReturn: LocalDate) -> Unit)? = null

    companion object {
        private const val ARG_BOOK_ID = "book_id"
        private const val ARG_BOOK_TITLE = "book_title"
        private const val ARG_QUANTITY_REMAINING = "quantity_remaining"

        fun newInstance(bookId: Long, bookTitle: String, quantityRemaining: Int): CreateBookingDialog {
            return CreateBookingDialog().apply {
                arguments = Bundle().apply {
                    putLong(ARG_BOOK_ID, bookId)
                    putString(ARG_BOOK_TITLE, bookTitle)
                    putInt(ARG_QUANTITY_REMAINING, quantityRemaining)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bookId = requireArguments().getLong(ARG_BOOK_ID)
        bookTitle = requireArguments().getString(ARG_BOOK_TITLE) ?: ""
        quantityRemaining = requireArguments().getInt(ARG_QUANTITY_REMAINING)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = DialogCreateBookingBinding.inflate(layoutInflater)

        setupQuantityField()
        setupDatePickers()
        setupButtons()

        return androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setView(binding.root)
            .setTitle("Бронирование: $bookTitle")
            .create()
    }

    private fun setupQuantityField() {
        if (quantityRemaining <= 0) {
            Toast.makeText(requireContext(), "Книга недоступна", Toast.LENGTH_SHORT).show()
            dismiss()
            return
        }

        val maxAllowed = minOf(quantityRemaining, 5)
        binding.etQuantity.hint = "Введите от 1 до $maxAllowed"

        val defaultQuantity = maxAllowed.toString()
        binding.etQuantity.setText(defaultQuantity)

        binding.etQuantity.requestFocus()
        binding.etQuantity.selectAll()
    }

    private fun setupDatePickers() {
        val today = LocalDate.now()
        val maxIssueDate = today.plusDays(5)

        binding.btnSelectIssueDate.setOnClickListener {
            showDatePicker(
                minDate = today,
                maxDate = maxIssueDate
            ) { date ->
                selectedDateIssue = date
                selectedDateReturn = null
                binding.tvIssueDate.text = date.format(dateFormatter)
                binding.tvReturnDate.text = "Не выбрана"
            }
        }

        binding.btnSelectReturnDate.setOnClickListener {
            if (selectedDateIssue == null) {
                Toast.makeText(requireContext(), "Сначала выберите дату выдачи", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val minReturnDate = selectedDateIssue!!.plusDays(1)
            val maxReturnDate = selectedDateIssue!!.plusMonths(1)

            showDatePicker(
                minDate = minReturnDate,
                maxDate = maxReturnDate
            ) { date ->
                selectedDateReturn = date
                binding.tvReturnDate.text = date.format(dateFormatter)
            }
        }
    }

    private fun showDatePicker(
        minDate: LocalDate? = null,
        maxDate: LocalDate? = null,
        onDateSelected: (LocalDate) -> Unit
    ) {
        val calendar = Calendar.getInstance()
        val datePicker = DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                val selectedDate = LocalDate.of(year, month + 1, day)
                onDateSelected(selectedDate)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        minDate?.let {
            calendar.timeInMillis = it.atStartOfDay()
                .atZone(java.time.ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
            datePicker.datePicker.minDate = calendar.timeInMillis
        }

        maxDate?.let {
            calendar.timeInMillis = it.atStartOfDay()
                .atZone(java.time.ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
            datePicker.datePicker.maxDate = calendar.timeInMillis
        }

        datePicker.show()
    }

    private fun setupButtons() {
        binding.btnCreate.setOnClickListener {
            if (validateInput()) {
                val quantity = binding.etQuantity.text.toString().toInt()
                onBookingCreated?.invoke(bookId, quantity, selectedDateIssue!!, selectedDateReturn!!)
                dismiss()
            }
        }

        binding.btnCancel.setOnClickListener {
            dismiss()
        }
    }

    private fun validateInput(): Boolean {
        val quantityText = binding.etQuantity.text.toString().trim()
        if (quantityText.isEmpty()) {
            Toast.makeText(requireContext(), "Введите количество", Toast.LENGTH_SHORT).show()
            binding.etQuantity.requestFocus()
            return false
        }

        val quantity: Int
        try {
            quantity = quantityText.toInt()
        } catch (e: NumberFormatException) {
            Toast.makeText(requireContext(), "Введите корректное число", Toast.LENGTH_SHORT).show()
            binding.etQuantity.requestFocus()
            binding.etQuantity.selectAll()
            return false
        }

        val maxAllowed = minOf(quantityRemaining, 5)
        if (quantity < 1 || quantity > maxAllowed) {
            Toast.makeText(requireContext(), "Количество должно быть от 1 до $maxAllowed", Toast.LENGTH_SHORT).show()
            binding.etQuantity.requestFocus()
            binding.etQuantity.selectAll()
            return false
        }

        if (selectedDateIssue == null) {
            Toast.makeText(requireContext(), "Выберите дату выдачи", Toast.LENGTH_SHORT).show()
            binding.btnSelectIssueDate.requestFocus()
            return false
        }

        if (selectedDateReturn == null) {
            Toast.makeText(requireContext(), "Выберите дату возврата", Toast.LENGTH_SHORT).show()
            binding.btnSelectReturnDate.requestFocus()
            return false
        }

        if (selectedDateReturn!!.isBefore(selectedDateIssue!!)) {
            Toast.makeText(requireContext(), "Дата возврата не может быть раньше даты выдачи", Toast.LENGTH_SHORT).show()
            binding.btnSelectReturnDate.requestFocus()
            return false
        }

        val today = LocalDate.now()
        val maxIssueDate = today.plusDays(5)

        if (selectedDateIssue!!.isAfter(maxIssueDate)) {
            Toast.makeText(requireContext(), "Дата выдачи не может быть позже чем через 5 дней", Toast.LENGTH_SHORT).show()
            binding.btnSelectIssueDate.requestFocus()
            return false
        }

        if (selectedDateReturn!!.isAfter(selectedDateIssue!!.plusMonths(1))) {
            Toast.makeText(requireContext(), "Дата возврата не может быть позже чем через месяц от даты выдачи", Toast.LENGTH_SHORT).show()
            binding.btnSelectReturnDate.requestFocus()
            return false
        }

        return true
    }

    fun setOnBookingCreatedListener(listener: (bookId: Long, quantity: Int, dateIssue: LocalDate, dateReturn: LocalDate) -> Unit) {
        onBookingCreated = listener
    }
}