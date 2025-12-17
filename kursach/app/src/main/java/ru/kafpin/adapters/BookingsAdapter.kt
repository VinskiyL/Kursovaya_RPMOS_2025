package ru.kafpin.ui.bookings

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.kafpin.R
import ru.kafpin.data.models.BookingWithDetails
import ru.kafpin.databinding.ItemBookingBinding

class BookingsAdapter(
    private val onItemClick: (BookingWithDetails) -> Unit,
    private val onDeleteClick: (BookingWithDetails) -> Unit
) : ListAdapter<BookingWithDetails, BookingsAdapter.ViewHolder>(BookingDiffCallback()) {

    inner class ViewHolder(
        private val binding: ItemBookingBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(booking: BookingWithDetails) {
            with(binding) {
                // ID
                tvBookingId.text = booking.displayId

                // Статус с цветом
                tvStatus.text = booking.statusText
                val statusColor = when (booking.booking.status) {
                    ru.kafpin.data.models.BookingStatus.PENDING -> R.color.status_pending
                    ru.kafpin.data.models.BookingStatus.CONFIRMED -> R.color.status_confirmed
                    ru.kafpin.data.models.BookingStatus.ISSUED -> R.color.status_issued
                    ru.kafpin.data.models.BookingStatus.RETURNED -> R.color.status_returned
                }
                tvStatus.setBackgroundColor(
                    ContextCompat.getColor(itemView.context, statusColor)
                )

                // Информация о книге
                tvBookTitle.text = booking.booking.bookTitle
                tvAuthors.text = booking.booking.bookAuthors
                tvDates.text = booking.formattedDates

                tvQuantity.text = "${booking.booking.quantity} шт."
                tvGenres.text = booking.booking.bookGenres.takeIf { it.isNotBlank() } ?: "—"

                // Клик на всю карточку
                root.setOnClickListener {
                    onItemClick(booking)
                }

                // Долгое нажатие для удаления
                root.setOnLongClickListener {
                    if (booking.canDelete) {
                        onDeleteClick(booking)
                        true
                    } else {
                        false
                    }
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemBookingBinding.inflate(inflater, parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class BookingDiffCallback : DiffUtil.ItemCallback<BookingWithDetails>() {
    override fun areItemsTheSame(oldItem: BookingWithDetails, newItem: BookingWithDetails): Boolean {
        return oldItem.booking.localId == newItem.booking.localId
    }

    override fun areContentsTheSame(oldItem: BookingWithDetails, newItem: BookingWithDetails): Boolean {
        return oldItem == newItem
    }
}