package ru.kafpin.ui.orders

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.kafpin.R
import ru.kafpin.data.models.OrderWithDetails
import ru.kafpin.databinding.ItemOrderBinding

class OrdersAdapter(
    private val onItemClick: (OrderWithDetails) -> Unit,
    private val onDeleteClick: (OrderWithDetails) -> Unit,
    private val isOnline: Boolean
) : ListAdapter<OrderWithDetails, OrdersAdapter.ViewHolder>(OrderDiffCallback()) {

    inner class ViewHolder(
        private val binding: ItemOrderBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(order: OrderWithDetails) {
            with(binding) {
                // ID
                tvOrderId.text = order.displayId

                // Статус с цветом
                tvStatus.text = order.statusText
                val statusColor = when (order.order.status) {
                    ru.kafpin.data.models.OrderStatus.LOCAL_PENDING -> R.color.status_order_pending
                    ru.kafpin.data.models.OrderStatus.SERVER_PENDING -> R.color.status_order_sent
                    ru.kafpin.data.models.OrderStatus.CONFIRMED -> R.color.status_order_confirmed
                }
                tvStatus.setBackgroundColor(
                    ContextCompat.getColor(itemView.context, statusColor)
                )

                // Информация о книге
                tvBookTitle.text = order.order.title
                tvAuthor.text = order.authorFull
                tvQuantity.text = "${order.order.quantity} шт."
                tvYear.text = order.order.datePublication?.let { "Год: $it" } ?: ""
                tvDate.text = order.formattedDate

                // Клик на всю карточку
                root.setOnClickListener {
                    onItemClick(order)
                }

                // Долгое нажатие для удаления
                root.setOnLongClickListener {
                    if (order.canDelete(isOnline)) {
                        onDeleteClick(order)
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
        val binding = ItemOrderBinding.inflate(inflater, parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class OrderDiffCallback : DiffUtil.ItemCallback<OrderWithDetails>() {
    override fun areItemsTheSame(oldItem: OrderWithDetails, newItem: OrderWithDetails): Boolean {
        return oldItem.order.localId == newItem.order.localId
    }

    override fun areContentsTheSame(oldItem: OrderWithDetails, newItem: OrderWithDetails): Boolean {
        return oldItem == newItem
    }
}