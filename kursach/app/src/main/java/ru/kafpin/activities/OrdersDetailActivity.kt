package ru.kafpin.activities

import android.content.Intent
import android.text.InputType
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import ru.kafpin.R
import ru.kafpin.databinding.ActivityOrdersDetailBinding
import ru.kafpin.ui.orders.CreateOrderDialog
import ru.kafpin.viewmodels.OrderDetailsViewModel
import ru.kafpin.viewmodels.OrderDetailsViewModelFactory

class OrdersDetailActivity : BaseActivity<ActivityOrdersDetailBinding>() {

    private val orderId: Long by lazy {
        intent.getLongExtra(EXTRA_ORDER_ID, -1L)
    }

    private val viewModel: OrderDetailsViewModel by viewModels {
        OrderDetailsViewModelFactory.getInstance(this)
    }

    companion object {
        const val EXTRA_ORDER_ID = "order_id"

        fun start(context: android.content.Context, orderId: Long) {
            val intent = android.content.Intent(context, OrdersDetailActivity::class.java)
            intent.putExtra(EXTRA_ORDER_ID, orderId)
            context.startActivity(intent)
        }
    }

    override fun inflateBinding(): ActivityOrdersDetailBinding {
        return ActivityOrdersDetailBinding.inflate(layoutInflater)
    }

    override fun setupUI() {
        if (orderId == -1L) {
            showErrorState("Неверный ID заказа")
            return
        }

        setupToolbarButtons(
            showBackButton = true,
            showLogoutButton = true
        )
        setToolbarTitle("Детали заказа")

        setupObservers()
        setupClickListeners()
        loadOrderDetails()
        setupNetworkObserver()
    }

    private fun setupNetworkObserver() {
        lifecycleScope.launch {
            networkMonitor.isOnline.collect { isOnline ->
                updateButtonsVisibility(isOnline)
            }
        }
    }

    private fun updateButtonsVisibility(isOnline: Boolean) {
        viewModel.orderDetails.value?.let { order ->
            binding.btnEdit.isVisible = order.canEdit(isOnline)
            binding.btnDelete.isVisible = order.canDelete(isOnline)
        }
    }


    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.orderDetails.collect { order ->
                order?.let {
                    bindOrderDetails(it)
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
            showEditDialog()
        }

        binding.btnDelete.setOnClickListener {
            showDeleteDialog()
        }

        // Кнопка повтора в errorLayout
        binding.btnRetry.setOnClickListener {
            loadOrderDetails()
        }
    }

    private fun loadOrderDetails() {
        lifecycleScope.launch {
            viewModel.loadOrderDetails(orderId)
        }
    }

    private fun bindOrderDetails(order: ru.kafpin.data.models.OrderWithDetails) {
        with(binding) {
            // ID заказа
            tvOrderId.text = order.displayId

            // Информация о заказе
            tvBookTitle.text = order.order.title
            tvAuthor.text = order.authorFull
            tvQuantity.text = "${order.order.quantity} шт."
            tvYear.text = order.order.datePublication ?: "Не указан"
            tvCreatedAt.text = order.formattedDate

            // Статус
            tvStatus.text = order.statusText
            val statusColor = when (order.order.status) {
                ru.kafpin.data.models.OrderStatus.LOCAL_PENDING -> R.color.status_order_pending
                ru.kafpin.data.models.OrderStatus.SERVER_PENDING -> R.color.status_order_sent
                ru.kafpin.data.models.OrderStatus.CONFIRMED -> R.color.status_order_confirmed
            }
            tvStatus.setBackgroundColor(
                androidx.core.content.ContextCompat.getColor(this@OrdersDetailActivity, statusColor)
            )
        }

        updateButtonsVisibility(networkMonitor.isOnline.value)
    }

    private fun showEditDialog() {
        viewModel.orderDetails.value?.let { order ->
            val dialog = CreateOrderDialog.editInstance(
                orderId = order.order.localId,
                title = order.order.title,
                authorSurname = order.order.authorSurname,
                authorName = order.order.authorName,
                authorPatronymic = order.order.authorPatronymic,
                quantity = order.order.quantity,
                year = order.order.datePublication
            )

            dialog.setOnOrderUpdatedListener { localId, orderData ->
                lifecycleScope.launch {
                    val success = viewModel.updateOrder(
                        localId = localId,
                        title = orderData.title,
                        authorSurname = orderData.authorSurname,
                        authorName = orderData.authorName,
                        authorPatronymic = orderData.authorPatronymic,
                        quantity = orderData.quantity,
                        year = orderData.year
                    )

                    if (success) {
                        Toast.makeText(this@OrdersDetailActivity,
                            "Заказ обновлён", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@OrdersDetailActivity,
                            "Ошибка обновления", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            dialog.show(supportFragmentManager, "EditOrderDialog")
        }
    }

    private fun showDeleteDialog() {
        viewModel.orderDetails.value?.let { order ->
            AlertDialog.Builder(this)
                .setTitle("Удаление заказа")
                .setMessage("Удалить заказ ${order.displayId} на книгу \"${order.order.title}\"?")
                .setPositiveButton("Удалить") { _, _ ->
                    lifecycleScope.launch {
                        val success = viewModel.deleteOrder(order.order.localId)
                        if (success) {
                            Toast.makeText(this@OrdersDetailActivity, "Заказ удалён", Toast.LENGTH_SHORT).show()
                            finish()
                        } else {
                            Toast.makeText(this@OrdersDetailActivity, "Ошибка удаления", Toast.LENGTH_SHORT).show()
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