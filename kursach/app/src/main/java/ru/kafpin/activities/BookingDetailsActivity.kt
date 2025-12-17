package ru.kafpin.activities

import android.text.InputType
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import ru.kafpin.R
import ru.kafpin.databinding.ActivityBookingDetailsBinding
import ru.kafpin.viewmodels.BookingDetailsViewModel
import ru.kafpin.viewmodels.BookingDetailsViewModelFactory

class BookingDetailsActivity : BaseActivity<ActivityBookingDetailsBinding>() {

    private val bookingId: Long by lazy {
        intent.getLongExtra(EXTRA_BOOKING_ID, -1L)
    }

    private val viewModel: BookingDetailsViewModel by viewModels {
        BookingDetailsViewModelFactory.getInstance(this)
    }

    companion object {
        const val EXTRA_BOOKING_ID = "booking_id"

        fun start(context: android.content.Context, bookingId: Long) {
            val intent = android.content.Intent(context, BookingDetailsActivity::class.java)
            intent.putExtra(EXTRA_BOOKING_ID, bookingId)
            context.startActivity(intent)
        }
    }

    override fun inflateBinding(): ActivityBookingDetailsBinding {
        return ActivityBookingDetailsBinding.inflate(layoutInflater)
    }

    override fun setupUI() {
        if (bookingId == -1L) {
            showErrorState("Неверный ID брони")
            return
        }

        setupToolbarButtons(
            showBackButton = true,
            showLogoutButton = true
        )
        setToolbarTitle("Детали брони")

        setupObservers()
        setupClickListeners()
        loadBookingDetails()
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.bookingDetails.collect { booking ->
                booking?.let {
                    bindBookingDetails(it)
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
    }

    private fun setupClickListeners() {
        binding.btnEdit.setOnClickListener {
            showEditQuantityDialog()
        }

        binding.btnDelete.setOnClickListener {
            showDeleteDialog()
        }
    }

    private fun loadBookingDetails() {
        lifecycleScope.launch {
            viewModel.loadBookingDetails(bookingId)
        }
    }

    private fun bindBookingDetails(booking: ru.kafpin.data.models.BookingWithDetails) {
        with(binding) {
            // ID брони
            tvBookingId.text = booking.displayId

            // Информация о книге
            tvBookTitle.text = booking.booking.bookTitle
            tvAuthors.text = booking.booking.bookAuthors
            tvGenres.text = booking.booking.bookGenres

            // Даты
            tvDates.text = booking.formattedDates
            tvIssueDate.text = booking.booking.dateIssue
            tvReturnDate.text = booking.booking.dateReturn

            // Количество
            tvQuantity.text = "${booking.booking.quantity} шт."

            // Статус
            tvStatus.text = booking.statusText
            val statusColor = when (booking.booking.status) {
                ru.kafpin.data.models.BookingStatus.PENDING -> R.color.status_pending
                ru.kafpin.data.models.BookingStatus.CONFIRMED -> R.color.status_confirmed
                ru.kafpin.data.models.BookingStatus.ISSUED -> R.color.status_issued
                ru.kafpin.data.models.BookingStatus.RETURNED -> R.color.status_returned
            }
            tvStatus.setBackgroundColor(
                androidx.core.content.ContextCompat.getColor(this@BookingDetailsActivity, statusColor)
            )

            // Кнопки редактирования/удаления
            btnEdit.isVisible = booking.canEdit
            btnDelete.isVisible = booking.canDelete

            // Предупреждение если просрочено
            if (booking.isOverdue) {
                tvOverdueWarning.isVisible = true
                tvOverdueWarning.text = "⚠️ Просрочено!"
            } else {
                booking.daysRemaining?.let { days ->
                    tvOverdueWarning.isVisible = true
                    tvOverdueWarning.text = "Осталось дней: $days"
                } ?: run {
                    tvOverdueWarning.isVisible = false
                }
            }
        }
    }

    private fun showEditQuantityDialog() {
        viewModel.bookingDetails.value?.let { booking ->
            val input = EditText(this)
            input.inputType = InputType.TYPE_CLASS_NUMBER
            input.setText(booking.booking.quantity.toString())

            AlertDialog.Builder(this)
                .setTitle("Изменение количества")
                .setMessage("Введите новое количество (от 1 до 5)")
                .setView(input)
                .setPositiveButton("Сохранить") { _, _ ->
                    val newQuantity = input.text.toString().toIntOrNull()
                    if (newQuantity != null && newQuantity in 1..5) {
                        if (newQuantity <= booking.booking.availableCopies) {
                            lifecycleScope.launch {
                                val success = viewModel.updateBookingQuantity(
                                    booking.booking.localId,
                                    newQuantity
                                )
                                if (success) {
                                    Toast.makeText(this@BookingDetailsActivity,
                                        "Количество обновлено", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(this@BookingDetailsActivity,
                                        "Ошибка обновления", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            Toast.makeText(this@BookingDetailsActivity,
                                "Нельзя заказать больше ${booking.booking.availableCopies} книг",
                                Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this@BookingDetailsActivity,
                            "Введите число от 1 до 5", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Отмена", null)
                .show()
        }
    }

    private fun showDeleteDialog() {
        viewModel.bookingDetails.value?.let { booking ->
            AlertDialog.Builder(this)
                .setTitle("Удаление брони")
                .setMessage("Удалить бронь ${booking.displayId} на книгу \"${booking.booking.bookTitle}\"?")
                .setPositiveButton("Удалить") { _, _ ->
                    lifecycleScope.launch {
                        val success = viewModel.deleteBooking(booking.booking.localId)
                        if (success) {
                            Toast.makeText(this@BookingDetailsActivity, "Бронь удалена", Toast.LENGTH_SHORT).show()
                            finish()
                        } else {
                            Toast.makeText(this@BookingDetailsActivity, "Ошибка удаления", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                .setNegativeButton("Отмена", null)
                .show()
        }
    }

    private fun showErrorState(message: String) {
        binding.errorText.text = message
        binding.errorLayout.isVisible = true
        binding.contentLayout.isVisible = false
        binding.progressBar.isVisible = false
    }
}